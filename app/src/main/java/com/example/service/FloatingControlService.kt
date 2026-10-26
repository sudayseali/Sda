package com.example.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
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
    private var floatingRootView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private var timerTextView: TextView? = null
    private var pauseResumeIcon: ImageView? = null

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
            x = 40
            y = 200
        }

        val container = createFloatingWidgetLayout()
        floatingRootView = container

        // Drag and touch handling
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
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) {
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
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
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

    private fun createFloatingWidgetLayout(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E6181A22"))
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

        // Pause/Resume button
        val pauseBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_media_pause)
            setColorFilter(Color.WHITE)
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnClickListener {
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
        }
        pauseResumeIcon = pauseBtn
        actionContainer.addView(pauseBtn)

        // Stop button
        val stopBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#FF334B"))
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnClickListener {
                val intent = Intent(this@FloatingControlService, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_STOP
                }
                startService(intent)
                hideFloatingView()
            }
        }
        actionContainer.addView(stopBtn)

        // Screenshot button
        val shotBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            setColorFilter(Color.parseColor("#00E5FF"))
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnClickListener {
                val intent = Intent(this@FloatingControlService, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_SCREENSHOT
                }
                startService(intent)
            }
        }
        actionContainer.addView(shotBtn)

        // Open App button
        val appBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_revert)
            setColorFilter(Color.parseColor("#FFAA00"))
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setOnClickListener {
                val intent = Intent(this@FloatingControlService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
        }
        actionContainer.addView(appBtn)

        root.addView(actionContainer)
        return root
    }

    private fun toggleExpanded() {
        val root = floatingRootView as? LinearLayout ?: return
        val actions = root.findViewWithTag<View>(TAG_EXPANDED_ACTIONS) ?: return
        isExpanded = !isExpanded
        actions.visibility = if (isExpanded) View.VISIBLE else View.GONE
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
