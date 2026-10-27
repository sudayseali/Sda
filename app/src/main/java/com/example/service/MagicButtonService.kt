package com.example.service

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
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast

class MagicButtonService : Service() {

    private var windowManager: WindowManager? = null
    private var magicRootView: View? = null
    private var lastTapTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private var pendingSingleTapRunnable: Runnable? = null

    companion object {
        const val ACTION_SHOW = "com.example.service.MagicButton.SHOW"
        const val ACTION_HIDE = "com.example.service.MagicButton.HIDE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showMagicButton()
            ACTION_HIDE -> hideMagicButton()
        }
        return START_NOT_STICKY
    }

    private fun showMagicButton() {
        if (magicRootView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val sizePx = (56 * resources.displayMetrics.density).toInt()
        val layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (16 * resources.displayMetrics.density).toInt()
            y = (36 * resources.displayMetrics.density).toInt()
        }

        val touchView = FrameLayout(this).apply {
            // Semi-transparent indicator dot (5% white or transparent red dot)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(35, 255, 51, 75)) // Very subtle transparent dot
                setStroke((1 * resources.displayMetrics.density).toInt(), Color.argb(40, 255, 255, 255))
            }
            isClickable = true
            isFocusable = false

            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime < 400L) {
                        // Double tap detected -> STOP recording!
                        pendingSingleTapRunnable?.let { handler.removeCallbacks(it) }
                        pendingSingleTapRunnable = null
                        lastTapTime = 0L
                        vibratePulse(double = true)
                        Toast.makeText(applicationContext, "Magic Button: Stopping recording...", Toast.LENGTH_SHORT).show()
                        val stopIntent = Intent(applicationContext, RecordingService::class.java).apply {
                            action = RecordingService.ACTION_STOP
                        }
                        startService(stopIntent)
                        hideMagicButton()
                    } else {
                        // Potential single tap -> Wait 350ms to verify not double-tap
                        lastTapTime = now
                        pendingSingleTapRunnable = Runnable {
                            vibratePulse(double = false)
                            val state = RecordingService.recordingState.value
                            val isPaused = state is RecordingState.Paused
                            val action = if (isPaused) RecordingService.ACTION_RESUME else RecordingService.ACTION_PAUSE
                            val msg = if (isPaused) "Magic Button: Resumed" else "Magic Button: Paused"
                            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                            val toggleIntent = Intent(applicationContext, RecordingService::class.java).apply {
                                this.action = action
                            }
                            startService(toggleIntent)
                        }
                        handler.postDelayed(pendingSingleTapRunnable!!, 350L)
                    }
                    true
                } else {
                    false
                }
            }
        }

        try {
            windowManager?.addView(touchView, layoutParams)
            magicRootView = touchView
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun vibratePulse(double: Boolean) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (double) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 70, 70, 100), -1))
                } else {
                    vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(if (double) 150L else 50L)
            }
        } catch (ignored: Exception) {}
    }

    private fun hideMagicButton() {
        pendingSingleTapRunnable?.let { handler.removeCallbacks(it) }
        pendingSingleTapRunnable = null
        magicRootView?.let {
            try {
                windowManager?.removeView(it)
            } catch (ignored: Exception) {}
        }
        magicRootView = null
        stopSelf()
    }

    override fun onDestroy() {
        hideMagicButton()
        super.onDestroy()
    }
}
