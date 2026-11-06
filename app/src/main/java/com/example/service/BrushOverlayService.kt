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
import android.graphics.RectF
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
import android.widget.TextView

class BrushOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var rootOverlayView: View? = null

    private enum class ToolMode { PEN, ARROW, RECTANGLE, CIRCLE, ERASER }

    private var currentToolMode = ToolMode.PEN
    private var currentPaintColor = Color.parseColor("#FF334B")
    private var currentStrokeWidthDp = 6

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

        // Drawing Canvas View
        val drawingView = DrawingCanvasView(this)
        container.addView(
            drawingView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Draggable Floating Toolbar
        val toolbar = createInteractiveToolbar(drawingView)
        val toolbarParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dp(40)
        }
        container.addView(toolbar, toolbarParams)

        makeViewDraggable(toolbar, container)

        rootOverlayView = container
        try {
            windowManager?.addView(container, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeViewDraggable(targetView: View, container: FrameLayout) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        targetView.setOnTouchListener { view, event ->
            val layoutParams = view.layoutParams as FrameLayout.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.leftMargin
                    initialY = layoutParams.topMargin
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    // Only drag if moved significantly to avoid breaking clicks
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        layoutParams.gravity = Gravity.TOP or Gravity.START
                        layoutParams.leftMargin = (initialX + deltaX).coerceIn(0, container.width - view.width)
                        layoutParams.topMargin = (initialY + deltaY).coerceIn(0, container.height - view.height)
                        container.updateViewLayout(view, layoutParams)
                        true
                    } else false
                }
                else -> false
            }
        }
    }

    private fun createInteractiveToolbar(drawingView: DrawingCanvasView): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F512141F"))
                cornerRadius = dp(20).toFloat()
                setStroke(dp(1), Color.parseColor("#40FFFFFF"))
            }

            // Top Row: Tool Selectors (Pen, Arrow, Rectangle, Circle, Eraser)
            val toolsRow = LinearLayout(this@BrushOverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val penBtn = createToolButton("✏️", "Pen", currentToolMode == ToolMode.PEN) {
                currentToolMode = ToolMode.PEN
                drawingView.setMode(ToolMode.PEN)
            }
            val arrowBtn = createToolButton("➡️", "Arrow", false) {
                currentToolMode = ToolMode.ARROW
                drawingView.setMode(ToolMode.ARROW)
            }
            val rectBtn = createToolButton("🔲", "Rect", false) {
                currentToolMode = ToolMode.RECTANGLE
                drawingView.setMode(ToolMode.RECTANGLE)
            }
            val circleBtn = createToolButton("⭕", "Circle", false) {
                currentToolMode = ToolMode.CIRCLE
                drawingView.setMode(ToolMode.CIRCLE)
            }
            val eraserBtn = createToolButton("🧹", "Eraser", false) {
                currentToolMode = ToolMode.ERASER
                drawingView.setMode(ToolMode.ERASER)
            }

            toolsRow.addView(penBtn)
            toolsRow.addView(arrowBtn)
            toolsRow.addView(rectBtn)
            toolsRow.addView(circleBtn)
            toolsRow.addView(eraserBtn)

            // Undo button
            val undoBtn = ImageView(this@BrushOverlayService).apply {
                val size = dp(32)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginStart = dp(6)
                    marginEnd = dp(6)
                }
                setImageResource(android.R.drawable.ic_menu_revert)
                setColorFilter(Color.parseColor("#00E5FF"))
                setOnClickListener {
                    drawingView.undo()
                }
            }
            toolsRow.addView(undoBtn)

            // Clear button
            val clearBtn = ImageView(this@BrushOverlayService).apply {
                val size = dp(32)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    marginEnd = dp(6)
                }
                setImageResource(android.R.drawable.ic_menu_rotate)
                setColorFilter(Color.WHITE)
                setOnClickListener {
                    drawingView.clearCanvas()
                }
            }
            toolsRow.addView(clearBtn)

            // Close button
            val closeBtn = ImageView(this@BrushOverlayService).apply {
                val size = dp(32)
                layoutParams = LinearLayout.LayoutParams(size, size)
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setColorFilter(Color.parseColor("#FF334B"))
                setOnClickListener {
                    hideBrushOverlay()
                }
            }
            toolsRow.addView(closeBtn)

            addView(toolsRow)

            // Bottom Row: Color Palette & Stroke Size
            val bottomRow = LinearLayout(this@BrushOverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, 0)
            }

            // Colors: Red, Cyan, Neon Green, Yellow, Purple, White
            val colors = listOf(
                Color.parseColor("#FF334B"),
                Color.parseColor("#00E5FF"),
                Color.parseColor("#00E676"),
                Color.parseColor("#FFEB3B"),
                Color.parseColor("#A855F7"),
                Color.WHITE
            )

            for (c in colors) {
                val dot = View(this@BrushOverlayService).apply {
                    val size = dp(20)
                    layoutParams = LinearLayout.LayoutParams(size, size).apply {
                        marginEnd = dp(8)
                    }
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(c)
                        setStroke(dp(1), Color.WHITE)
                    }
                    setOnClickListener {
                        currentPaintColor = c
                        if (currentToolMode == ToolMode.ERASER) {
                            currentToolMode = ToolMode.PEN
                            drawingView.setMode(ToolMode.PEN)
                        }
                        drawingView.setColor(c)
                    }
                }
                bottomRow.addView(dot)
            }

            // Stroke Size Selector (Thin 4dp, Med 8dp, Thick 16dp)
            val strokeSizes = listOf(4, 8, 16)
            for (w in strokeSizes) {
                val sizeDot = TextView(this@BrushOverlayService).apply {
                    text = "${w}px"
                    setTextColor(Color.LTGRAY)
                    textSize = 10f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(dp(26), dp(20)).apply {
                        marginEnd = dp(4)
                    }
                    background = GradientDrawable().apply {
                        cornerRadius = dp(6).toFloat()
                        setColor(Color.parseColor("#33FFFFFF"))
                    }
                    setOnClickListener {
                        currentStrokeWidthDp = w
                        drawingView.setStrokeWidth(w)
                    }
                }
                bottomRow.addView(sizeDot)
            }

            addView(bottomRow)
        }
    }

    private fun createToolButton(emoji: String, label: String, isSelected: Boolean, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = emoji
            textSize = 16f
            gravity = Gravity.CENTER
            val size = dp(32)
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = dp(6)
            }
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setColor(if (isSelected) Color.parseColor("#33FF334B") else Color.TRANSPARENT)
                setStroke(dp(1), if (isSelected) Color.parseColor("#FF334B") else Color.TRANSPARENT)
            }
            setOnClickListener {
                onClick()
            }
        }
    }

    private data class DrawItem(
        val shapeType: ToolMode,
        val path: Path? = null,
        val startX: Float = 0f,
        val startY: Float = 0f,
        val endX: Float = 0f,
        val endY: Float = 0f,
        val paint: Paint
    )

    private inner class DrawingCanvasView(context: Context) : View(context) {

        private val historyList = mutableListOf<DrawItem>()
        private var currentPath = Path()

        private var touchStartX = 0f
        private var touchStartY = 0f
        private var tempEndX = 0f
        private var tempEndY = 0f
        private var isDrawingShape = false

        private val activePaint = Paint().apply {
            color = currentPaintColor
            isAntiAlias = true
            strokeWidth = dp(currentStrokeWidthDp).toFloat()
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        fun setMode(mode: ToolMode) {
            currentToolMode = mode
            if (mode == ToolMode.ERASER) {
                activePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                activePaint.strokeWidth = dp(24).toFloat()
            } else {
                activePaint.xfermode = null
                activePaint.color = currentPaintColor
                activePaint.strokeWidth = dp(currentStrokeWidthDp).toFloat()
            }
        }

        fun setColor(color: Int) {
            currentPaintColor = color
            activePaint.xfermode = null
            activePaint.color = color
            activePaint.strokeWidth = dp(currentStrokeWidthDp).toFloat()
        }

        fun setStrokeWidth(widthDp: Int) {
            currentStrokeWidthDp = widthDp
            if (currentToolMode != ToolMode.ERASER) {
                activePaint.strokeWidth = dp(widthDp).toFloat()
            }
        }

        fun undo() {
            if (historyList.isNotEmpty()) {
                historyList.removeAt(historyList.size - 1)
                invalidate()
            }
        }

        fun clearCanvas() {
            historyList.clear()
            currentPath.reset()
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // Render all historical drawn items
            for (item in historyList) {
                renderDrawItem(canvas, item)
            }

            // Render active shape / pen stroke in real-time while dragging
            if (isDrawingShape) {
                when (currentToolMode) {
                    ToolMode.PEN, ToolMode.ERASER -> {
                        canvas.drawPath(currentPath, activePaint)
                    }
                    ToolMode.ARROW -> {
                        drawArrow(canvas, touchStartX, touchStartY, tempEndX, tempEndY, activePaint)
                    }
                    ToolMode.RECTANGLE -> {
                        val rect = RectF(
                            Math.min(touchStartX, tempEndX),
                            Math.min(touchStartY, tempEndY),
                            Math.max(touchStartX, tempEndX),
                            Math.max(touchStartY, tempEndY)
                        )
                        canvas.drawRect(rect, activePaint)
                    }
                    ToolMode.CIRCLE -> {
                        val rect = RectF(
                            Math.min(touchStartX, tempEndX),
                            Math.min(touchStartY, tempEndY),
                            Math.max(touchStartX, tempEndX),
                            Math.max(touchStartY, tempEndY)
                        )
                        canvas.drawOval(rect, activePaint)
                    }
                }
            }
        }

        private fun renderDrawItem(canvas: Canvas, item: DrawItem) {
            when (item.shapeType) {
                ToolMode.PEN, ToolMode.ERASER -> {
                    item.path?.let { canvas.drawPath(it, item.paint) }
                }
                ToolMode.ARROW -> {
                    drawArrow(canvas, item.startX, item.startY, item.endX, item.endY, item.paint)
                }
                ToolMode.RECTANGLE -> {
                    val rect = RectF(
                        Math.min(item.startX, item.endX),
                        Math.min(item.startY, item.endY),
                        Math.max(item.startX, item.endX),
                        Math.max(item.startY, item.endY)
                    )
                    canvas.drawRect(rect, item.paint)
                }
                ToolMode.CIRCLE -> {
                    val rect = RectF(
                        Math.min(item.startX, item.endX),
                        Math.min(item.startY, item.endY),
                        Math.max(item.startX, item.endX),
                        Math.max(item.startY, item.endY)
                    )
                    canvas.drawOval(rect, item.paint)
                }
            }
        }

        private fun drawArrow(canvas: Canvas, fromX: Float, fromY: Float, toX: Float, toY: Float, paint: Paint) {
            canvas.drawLine(fromX, fromY, toX, toY, paint)
            val angle = Math.atan2((toY - fromY).toDouble(), (toX - fromX).toDouble())
            val arrowSize = (paint.strokeWidth * 3.5f).coerceAtLeast(dp(18).toFloat())
            val arrowAngle = Math.toRadians(30.0)

            val x1 = toX - arrowSize * Math.cos(angle - arrowAngle).toFloat()
            val y1 = toY - arrowSize * Math.sin(angle - arrowAngle).toFloat()
            val x2 = toX - arrowSize * Math.cos(angle + arrowAngle).toFloat()
            val y2 = toY - arrowSize * Math.sin(angle + arrowAngle).toFloat()

            val arrowHeadPath = Path().apply {
                moveTo(toX, toY)
                lineTo(x1, y1)
                moveTo(toX, toY)
                lineTo(x2, y2)
            }
            canvas.drawPath(arrowHeadPath, paint)
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val touchX = event.x
            val touchY = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = touchX
                    touchStartY = touchY
                    tempEndX = touchX
                    tempEndY = touchY
                    isDrawingShape = true

                    if (currentToolMode == ToolMode.PEN || currentToolMode == ToolMode.ERASER) {
                        currentPath.reset()
                        currentPath.moveTo(touchX, touchY)
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    tempEndX = touchX
                    tempEndY = touchY
                    if (currentToolMode == ToolMode.PEN || currentToolMode == ToolMode.ERASER) {
                        currentPath.lineTo(touchX, touchY)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    isDrawingShape = false
                    val paintCopy = Paint(activePaint)
                    if (currentToolMode == ToolMode.PEN || currentToolMode == ToolMode.ERASER) {
                        currentPath.lineTo(touchX, touchY)
                        historyList.add(
                            DrawItem(
                                shapeType = currentToolMode,
                                path = Path(currentPath),
                                paint = paintCopy
                            )
                        )
                        currentPath.reset()
                    } else {
                        historyList.add(
                            DrawItem(
                                shapeType = currentToolMode,
                                startX = touchStartX,
                                startY = touchStartY,
                                endX = touchX,
                                endY = touchY,
                                paint = paintCopy
                            )
                        )
                    }
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
