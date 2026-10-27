package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout

class BrushOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootOverlayView: View? = null

    private var currentPaintColor = Color.parseColor("#FF334B")
    private var isEraser = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showBrushOverlay()
            ACTION_HIDE -> hideBrushOverlay()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showBrushOverlay() {
        if (rootOverlayView != null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        val container = FrameLayout(this)

        // Custom drawing canvas view
        val drawingView = DrawingCanvasView(this)
        container.addView(drawingView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        // Floating brush toolbar at bottom
        val toolbar = createToolbar(drawingView)
        val toolbarParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(32)
        }
        container.addView(toolbar, toolbarParams)

        rootOverlayView = container
        try {
            windowManager?.addView(container, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createToolbar(drawingView: DrawingCanvasView): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EE181A22"))
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.parseColor("#44FFFFFF"))
            }

            // Colors: Red, Cyan, Yellow, White
            val colors = listOf(
                Color.parseColor("#FF334B"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#FFEB3B"),
                Color.WHITE
            )

            for (c in colors) {
                val dot = View(this@BrushOverlayService).apply {
                    val size = dp(22)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginEnd = dp(10)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(c)
                        setStroke(dp(1), Color.WHITE)
                    }
                    setOnClickListener {
                        isEraser = false
                        currentPaintColor = c
                        drawingView.setDrawColor(c)
                    }
                }
                addView(dot)
            }

            // Eraser
            val eraserBtn = ImageView(this@BrushOverlayService).apply {
                val size = dp(28)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(10)
                }
                setImageResource(android.R.drawable.ic_menu_delete)
                setColorFilter(Color.LTGRAY)
                setOnClickListener {
                    isEraser = true
                    drawingView.setEraserMode(true)
                }
            }
            addView(eraserBtn)

            // Clear button
            val clearBtn = ImageView(this@BrushOverlayService).apply {
                val size = dp(28)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(12)
                }
                setImageResource(android.R.drawable.ic_menu_rotate)
                setColorFilter(Color.WHITE)
                setOnClickListener {
                    drawingView.clearCanvas()
                }
            }
            addView(clearBtn)

            // Close brush button
            val closeBtn = ImageView(this@BrushOverlayService).apply {
                val size = dp(28)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.parseColor("#FF334B"))
                setOnClickListener {
                    hideBrushOverlay()
                }
            }
            addView(closeBtn)
        }
    }

    private inner class DrawingCanvasView(context: Context) : View(context) {
        private var drawBitmap: Bitmap? = null
        private var drawCanvas: Canvas? = null
        private val drawPath = Path()
        private val drawPaint = Paint().apply {
            color = currentPaintColor
            isAntiAlias = true
            strokeWidth = dp(5).toFloat()
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        fun setDrawColor(color: Int) {
            drawPaint.xfermode = null
            drawPaint.color = color
            drawPaint.strokeWidth = dp(5).toFloat()
        }

        fun setEraserMode(enabled: Boolean) {
            if (enabled) {
                drawPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                drawPaint.strokeWidth = dp(22).toFloat()
            } else {
                setDrawColor(currentPaintColor)
            }
        }

        fun clearCanvas() {
            drawBitmap?.eraseColor(Color.TRANSPARENT)
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            super.onSizeChanged(w, h, oldw, oldh)
            if (w > 0 && h > 0) {
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                drawBitmap = bmp
                drawCanvas = Canvas(bmp)
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            drawBitmap?.let {
                canvas.drawBitmap(it, 0f, 0f, null)
            }
            canvas.drawPath(drawPath, drawPaint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val touchX = event.x
            val touchY = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    drawPath.moveTo(touchX, touchY)
                }
                MotionEvent.ACTION_MOVE -> {
                    drawPath.lineTo(touchX, touchY)
                }
                MotionEvent.ACTION_UP -> {
                    drawCanvas?.drawPath(drawPath, drawPaint)
                    drawPath.reset()
                }
                else -> return false
            }
            invalidate()
            return true
        }
    }

    private fun hideBrushOverlay() {
        rootOverlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (ignored: Exception) {}
        }
        rootOverlayView = null
        stopSelf()
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideBrushOverlay()
    }

    companion object {
        const val ACTION_SHOW = "com.example.service.action.SHOW_BRUSH"
        const val ACTION_HIDE = "com.example.service.action.HIDE_BRUSH"
    }
}
