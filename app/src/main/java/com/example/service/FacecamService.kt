package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import com.example.data.model.FacecamShape
import com.example.data.model.FacecamSize
import com.example.data.repository.SettingsRepository
import kotlin.math.abs

class FacecamService : Service() {

    private var windowManager: WindowManager? = null
    private var facecamRootView: FrameLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var textureView: TextureView? = null
    private val backgroundHandler = Handler(Looper.getMainLooper())

    private var useFrontCamera = true
    private var currentShape = FacecamShape.CIRCLE
    private var currentSize = FacecamSize.MEDIUM

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showFacecam()
            ACTION_HIDE -> hideFacecam()
        }
        return START_NOT_STICKY
    }

    private fun showFacecam() {
        if (facecamRootView != null) return

        val settings = SettingsRepository(this).settingsFlow.value
        currentShape = settings.facecamShape
        currentSize = settings.facecamSize

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = dp(currentSize.sizeDp)
        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 200
        }

        val container = FrameLayout(this)
        applyShapeAndStyle(container)

        val cameraTexture = TextureView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                    openSelectedCamera()
                }
                override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                    closeCamera()
                    return true
                }
                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
            }
        }
        textureView = cameraTexture
        container.addView(cameraTexture)

        // Control Toolbar inside Facecam
        val controlsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(28)
            ).apply {
                gravity = Gravity.TOP
                topMargin = dp(4)
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        }

        // 1. Flip Camera Button (Front / Back)
        val flipBtn = ImageView(this).apply {
            val btnSize = dp(24)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginEnd = dp(4)
            }
            setImageResource(android.R.drawable.ic_popup_sync)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#99000000"))
            }
            contentDescription = "Flip camera"
            setOnClickListener {
                useFrontCamera = !useFrontCamera
                closeCamera()
                openSelectedCamera()
            }
        }
        controlsRow.addView(flipBtn)

        // 2. Shape Switch Button (Circle -> Rounded -> Square)
        val shapeBtn = ImageView(this).apply {
            val btnSize = dp(24)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                marginEnd = dp(4)
            }
            setImageResource(android.R.drawable.ic_menu_crop)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#99000000"))
            }
            contentDescription = "Change shape"
            setOnClickListener {
                currentShape = when (currentShape) {
                    FacecamShape.CIRCLE -> FacecamShape.ROUNDED_RECT
                    FacecamShape.ROUNDED_RECT -> FacecamShape.SQUARE
                    FacecamShape.SQUARE -> FacecamShape.CIRCLE
                }
                SettingsRepository(this@FacecamService).updateFacecamShape(currentShape)
                applyShapeAndStyle(container)
            }
        }
        controlsRow.addView(shapeBtn)

        // Spacer
        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        controlsRow.addView(spacer)

        // 3. Close button on top-right
        val closeBtn = ImageView(this).apply {
            val btnSize = dp(24)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize)
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#99000000"))
            }
            contentDescription = "Close Facecam"
            setOnClickListener { hideFacecam() }
        }
        controlsRow.addView(closeBtn)

        container.addView(controlsRow)

        // Draggable touch handling with double tap to resize
        var lastTouchDown = 0L
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = layoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false

                        val now = System.currentTimeMillis()
                        if (now - lastTouchDown < 350L) {
                            // Double tap to resize
                            cycleSize(container)
                            lastTouchDown = 0L
                            return true
                        }
                        lastTouchDown = now
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 8 || abs(dy) > 8) {
                            isDragging = true
                        }
                        if (isDragging) {
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(container, params)
                            } catch (ignored: Exception) {}
                        }
                        return true
                    }
                }
                return false
            }
        })

        facecamRootView = container
        try {
            windowManager?.addView(container, layoutParams)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding facecam view", e)
        }
    }

    private fun applyShapeAndStyle(container: FrameLayout) {
        val cornerRadiusPx = when (currentShape) {
            FacecamShape.CIRCLE -> dp(currentSize.sizeDp) / 2f
            FacecamShape.ROUNDED_RECT -> dp(20).toFloat()
            FacecamShape.SQUARE -> dp(6).toFloat()
        }

        container.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                if (currentShape == FacecamShape.CIRCLE) {
                    outline.setOval(0, 0, view.width, view.height)
                } else {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
                }
            }
        }
        container.clipToOutline = true
        container.background = GradientDrawable().apply {
            setColor(Color.BLACK)
            if (currentShape == FacecamShape.CIRCLE) {
                shape = GradientDrawable.OVAL
            } else {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = cornerRadiusPx
            }
            setStroke(dp(2), Color.parseColor("#FF334B"))
        }
        container.invalidateOutline()
    }

    private fun cycleSize(container: FrameLayout) {
        currentSize = when (currentSize) {
            FacecamSize.SMALL -> FacecamSize.MEDIUM
            FacecamSize.MEDIUM -> FacecamSize.LARGE
            FacecamSize.LARGE -> FacecamSize.SMALL
        }
        SettingsRepository(this).updateFacecamSize(currentSize)
        val newPx = dp(currentSize.sizeDp)
        layoutParams?.width = newPx
        layoutParams?.height = newPx
        applyShapeAndStyle(container)
        try {
            windowManager?.updateViewLayout(container, layoutParams)
        } catch (ignored: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun openSelectedCamera() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            var targetCameraId: String? = null
            val targetFacing = if (useFrontCamera) CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == targetFacing) {
                    targetCameraId = id
                    break
                }
            }
            val targetId = targetCameraId ?: manager.cameraIdList.firstOrNull() ?: return

            manager.openCamera(targetId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startCameraPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera", e)
        }
    }

    private fun startCameraPreview() {
        val camera = cameraDevice ?: return
        val texture = textureView?.surfaceTexture ?: return

        try {
            val surface = Surface(texture)
            val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
            }

            @Suppress("DEPRECATION")
            camera.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    if (cameraDevice == null) return
                    captureSession = session
                    try {
                        builder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                        session.setRepeatingRequest(builder.build(), null, backgroundHandler)
                    } catch (e: Exception) {
                        Log.e(TAG, "Repeating request failed", e)
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Camera session config failed")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Preview start failed", e)
        }
    }

    private fun closeCamera() {
        try {
            captureSession?.close()
            captureSession = null
        } catch (ignored: Exception) {}
        try {
            cameraDevice?.close()
            cameraDevice = null
        } catch (ignored: Exception) {}
    }

    private fun hideFacecam() {
        closeCamera()
        facecamRootView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (ignored: Exception) {}
        }
        facecamRootView = null
        stopSelf()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFacecam()
    }

    companion object {
        private const val TAG = "FacecamService"
        const val ACTION_SHOW = "com.example.service.action.SHOW_FACECAM"
        const val ACTION_HIDE = "com.example.service.action.HIDE_FACECAM"
    }
}
