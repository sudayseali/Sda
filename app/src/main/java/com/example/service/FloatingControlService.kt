package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.MainActivity
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingControlService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var floatingRootView: LinearLayout? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var timerTextView: TextView? = null
    private var pauseResumeIcon: ImageView? = null

    private val autoCollapseHandler = Handler(Looper.getMainLooper())
    private val autoCollapseRunnable = Runnable {
        if (isExpanded) {
            toggleExpanded(forceCollapse = true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showFloatingView()
            ACTION_HIDE -> hideFloatingView()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingView() {
        if (floatingRootView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 30
            y = 220
        }

        val container = createFloatingWidgetLayout()
        floatingRootView = container

        // Drag, snap, and touch handling
        container.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = layoutParams ?: return false
                val rawX = event.rawX
                val rawY = event.rawY

                // Reset auto-collapse timer on user interaction
                resetAutoCollapseTimer()

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = rawX
                        initialTouchY = rawY
                        isDragging = false

                        // If expanded, check if touch hit any child action button
                        if (isExpanded) {
                            val actionContainer = container.findViewWithTag<LinearLayout>(TAG_EXPANDED_ACTIONS)
                            if (actionContainer != null && isTouchInsideView(actionContainer, rawX, rawY)) {
                                // Touch inside action container -> let children handle clicks
                                return false
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (rawX - initialTouchX).toInt()
                        val dy = (rawY - initialTouchY).toInt()
                        if (abs(dx) > 12 || abs(dy) > 12) {
                            isDragging = true
                        }
                        if (isDragging) {
                            val screenBounds = getScreenDimensions()
                            val viewWidth = container.width.coerceAtLeast(dp(120))
                            val viewHeight = container.height.coerceAtLeast(dp(40))

                            params.x = (initialX + dx).coerceIn(0, (screenBounds.first - viewWidth).coerceAtLeast(0))
                            params.y = (initialY + dy).coerceIn(dp(20), (screenBounds.second - viewHeight - dp(40)).coerceAtLeast(0))

                            try {
                                windowManager?.updateViewLayout(container, params)
                            } catch (ignored: Exception) {}
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isDragging) {
                            // Gentle edge snapping
                            val screenBounds = getScreenDimensions()
                            val viewWidth = container.width.coerceAtLeast(dp(120))
                            val midX = screenBounds.first / 2
                            val targetX = if (params.x + (viewWidth / 2) < midX) 16 else (screenBounds.first - viewWidth - 16).coerceAtLeast(0)

                            params.x = targetX
                            try {
                                windowManager?.updateViewLayout(container, params)
                            } catch (ignored: Exception) {}
                        } else {
                            // Simple tap on bubble -> toggle expand/collapse
                            toggleExpanded()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(container, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        observeRecordingStatus()
    }

    private fun isTouchInsideView(view: View, rawX: Float, rawY: Float): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val x = location[0]
        val y = location[1]
        return rawX >= x && rawX <= (x + view.width) && rawY >= y && rawY <= (y + view.height)
    }

    private fun getScreenDimensions(): Pair<Int, Int> {
        val wm = windowManager ?: getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        return Pair(metrics.widthPixels, metrics.heightPixels)
    }

    private fun createFloatingWidgetLayout(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE141724"))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.parseColor("#44FF334B"))
            }
        }

        // Red recording indicator dot
        val dot = View(this).apply {
            val size = dp(10)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(8)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FF334B"))
            }
        }
        root.addView(dot)

        // Timer TextView
        val timer = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        timerTextView = timer
        root.addView(timer)

        // Action buttons container (visible when expanded)
        val actionContainer = LinearLayout(this).apply {
            tag = TAG_EXPANDED_ACTIONS
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            visibility = View.GONE
            setPadding(dp(8), 0, 0, 0)
        }

        // Helper to build action icon buttons with generous 48dp touch target
        fun createActionButton(
            iconRes: Int,
            tintColor: Int,
            contentDesc: String,
            onClick: () -> Unit
        ): ImageView {
            val size = dp(36)
            return ImageView(this).apply {
                setImageResource(iconRes)
                setColorFilter(tintColor)
                contentDescription = contentDesc
                setPadding(dp(7), dp(7), dp(7), dp(7))
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = dp(4)
                }
                setOnClickListener {
                    resetAutoCollapseTimer()
                    onClick()
                }
            }
        }

        // Pause/Resume button
        val pauseBtn = createActionButton(
            iconRes = android.R.drawable.ic_media_pause,
            tintColor = Color.WHITE,
            contentDesc = "Pause or Resume Recording"
        ) {
            val state = RecordingService.recordingState.value
            val intent = Intent(this@FloatingControlService, RecordingService::class.java).apply {
                action = if (state is RecordingState.Paused) {
                    RecordingService.ACTION_RESUME
                } else {
                    RecordingService.ACTION_PAUSE
                }
            }
            startService(intent)
        }
        pauseResumeIcon = pauseBtn
        actionContainer.addView(pauseBtn)

        // Stop button
        val stopBtn = createActionButton(
            iconRes = android.R.drawable.ic_menu_close_clear_cancel,
            tintColor = Color.parseColor("#FF334B"),
            contentDesc = "Stop Recording"
        ) {
            val intent = Intent(this@FloatingControlService, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
            startService(intent)
            hideFloatingView()
        }
        actionContainer.addView(stopBtn)

        // Screenshot button
        val shotBtn = createActionButton(
            iconRes = android.R.drawable.ic_menu_camera,
            tintColor = Color.parseColor("#00E5FF"),
            contentDesc = "Take Screenshot"
        ) {
            val intent = Intent(this@FloatingControlService, RecordingService::class.java).apply {
                action = RecordingService.ACTION_SCREENSHOT
            }
            startService(intent)
        }
        actionContainer.addView(shotBtn)

        // Facecam button
        var isFacecamShowing = false
        val facecamBtn = createActionButton(
            iconRes = android.R.drawable.ic_menu_myplaces,
            tintColor = Color.parseColor("#4CAF50"),
            contentDesc = "Toggle Facecam Window"
        ) {
            isFacecamShowing = !isFacecamShowing
            val intent = Intent(this@FloatingControlService, FacecamService::class.java).apply {
                action = if (isFacecamShowing) FacecamService.ACTION_SHOW else FacecamService.ACTION_HIDE
            }
            startService(intent)
        }
        actionContainer.addView(facecamBtn)

        // Screen Brush / Drawing Tool button
        var isBrushShowing = false
        val brushBtn = createActionButton(
            iconRes = android.R.drawable.ic_menu_edit,
            tintColor = Color.parseColor("#E040FB"),
            contentDesc = "Toggle Drawing Brush Overlay"
        ) {
            isBrushShowing = !isBrushShowing
            val intent = Intent(this@FloatingControlService, BrushOverlayService::class.java).apply {
                action = if (isBrushShowing) BrushOverlayService.ACTION_SHOW else BrushOverlayService.ACTION_HIDE
            }
            startService(intent)
        }
        actionContainer.addView(brushBtn)

        // Open App button
        val appBtn = createActionButton(
            iconRes = android.R.drawable.ic_menu_revert,
            tintColor = Color.parseColor("#FFAA00"),
            contentDesc = "Return to Main Application Screen"
        ) {
            val intent = Intent(this@FloatingControlService, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
        actionContainer.addView(appBtn)

        root.addView(actionContainer)
        return root
    }

    private fun toggleExpanded(forceCollapse: Boolean = false) {
        val root = floatingRootView ?: return
        val actions = root.findViewWithTag<View>(TAG_EXPANDED_ACTIONS) ?: return

        isExpanded = if (forceCollapse) false else !isExpanded
        actions.visibility = if (isExpanded) View.VISIBLE else View.GONE

        if (isExpanded) {
            resetAutoCollapseTimer()
        } else {
            autoCollapseHandler.removeCallbacks(autoCollapseRunnable)
        }

        // Update layout dimensions in WindowManager
        try {
            val params = layoutParams ?: return
            windowManager?.updateViewLayout(root, params)
        } catch (ignored: Exception) {}
    }

    private fun resetAutoCollapseTimer() {
        autoCollapseHandler.removeCallbacks(autoCollapseRunnable)
        if (isExpanded) {
            autoCollapseHandler.postDelayed(autoCollapseRunnable, 5000L) // 5s auto-collapse
        }
    }

    private fun observeRecordingStatus() {
        serviceScope.launch {
            RecordingService.recordingDurationMs.collectLatest { durationMs ->
                timerTextView?.text = TimeUtils.formatDuration(durationMs)
            }
        }

        serviceScope.launch {
            RecordingService.recordingState.collectLatest { state ->
                when (state) {
                    is RecordingState.Paused -> {
                        pauseResumeIcon?.setImageResource(android.R.drawable.ic_media_play)
                    }
                    is RecordingState.Recording -> {
                        pauseResumeIcon?.setImageResource(android.R.drawable.ic_media_pause)
                    }
                    is RecordingState.Idle, is RecordingState.Stopping -> {
                        hideFloatingView()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun hideFloatingView() {
        autoCollapseHandler.removeCallbacks(autoCollapseRunnable)
        floatingRootView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (ignored: Exception) {}
        }
        floatingRootView = null
        stopSelf()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingView()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_SHOW = "com.example.service.action.SHOW_FLOATING"
        const val ACTION_HIDE = "com.example.service.action.HIDE_FLOATING"
        private const val TAG_EXPANDED_ACTIONS = "expanded_actions"
    }
}
