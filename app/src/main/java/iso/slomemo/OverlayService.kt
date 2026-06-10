package iso.slomemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var containerLayout: LinearLayout? = null
    private lateinit var db: AppDatabase

    private var targetMachineId: Int = 0
    // 🟢 追加：アプリ本体（メモ画面等）が今前面にいるかどうかのフラグ
    private var isAppActive: Boolean = false

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_OVERLAY_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            if (intent.action == ACTION_STOP_SERVICE) {
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("overlay_running", false).apply()

                stopSelf()
                return START_NOT_STICKY
            }

            // 🟢 修正：フラグの状態を更新した上で、Viewがあれば即座に可視性を切り替える
            when (intent.action) {
                "ACTION_HIDE_OVERLAY" -> {
                    isAppActive = true
                    containerLayout?.visibility = View.GONE
                    return START_NOT_STICKY
                }
                "ACTION_SHOW_OVERLAY" -> {
                    isAppActive = false
                    containerLayout?.visibility = View.VISIBLE
                    return START_NOT_STICKY
                }
            }

            val machineId = intent.getIntExtra("TARGET_MACHINE_ID", -1)
            if (machineId != -1) {
                targetMachineId = machineId
                recreateCounters()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        val channelId = "overlay_service_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "常駐カウンター", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable
        )

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("常駐カウンター起動中")
                .setContentText("画面上のボタンからカウントできます")
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "カウンターを消去", stopPendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("常駐カウンター起動中")
                .setContentText("画面上のボタンからカウントできます")
                .setSmallIcon(android.R.drawable.ic_input_add)
                .setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "カウンターを消去", stopPendingIntent)
                .build()
        }

        startForeground(1, notification)

        db = androidx.room.Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "memo-db"
        ).fallbackToDestructiveMigration().build()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private fun recreateCounters() {
        containerLayout?.let {
            if (it.parent != null) {
                try {
                    windowManager.removeView(it)
                } catch (e: IllegalArgumentException) {
                }
            }
        }

        val density = resources.displayMetrics.density

        CoroutineScope(Dispatchers.Main).launch {
            val appSetting = withContext(Dispatchers.IO) {
                db.memoDao().getAppSetting()
            }
            val counterSettings = withContext(Dispatchers.IO) {
                db.memoDao().getCounterSettingsByMachineDirect(targetMachineId)
            }

            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val savedHeight = prefs.getInt("counter_overlay_height", 30)
            val cellHeightPx = (savedHeight * density).toInt()

            containerLayout = LinearLayout(this@OverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((5 * density).toInt(), (5 * density).toInt(), (5 * density).toInt(), (5 * density).toInt())

                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#333333"))
                    cornerRadius = 8 * density
                }

                // 🟢 追加：非同期生成が終わった時点のアプリのフォアグラウンド状態を即座に反映する
                visibility = if (isAppActive) View.GONE else View.VISIBLE
            }

            // 左端の移動用つまみ
            val dragHandle = TextView(this@OverlayService).apply {
                text = "⁝⁝"
                textSize = 18f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                val handleWidth = (24 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(handleWidth, cellHeightPx).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
            }
            containerLayout?.addView(dragHandle)

            // 各種カウンターボタンの生成ループ
            counterSettings.forEachIndexed { index, setting ->
                val btn = Button(this@OverlayService).apply {
                    textSize = 24f
                    setTextColor(Color.BLACK)
                    setPadding(0, 0, 0, 0)

                    try {
                        val customTypeface = androidx.core.content.res.ResourcesCompat.getFont(
                            this@OverlayService,
                            R.font.dseg7classic_bold
                        )
                        typeface = customTypeface
                    } catch (e: Exception) {
                        setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    }

                    val btnColor = try {
                        (setting.color as Number).toInt()
                    } catch (e: Exception) {
                        Color.parseColor("#37474F")
                    }

                    background = GradientDrawable().apply {
                        setColor(btnColor)
                        cornerRadius = 6 * density
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        val valueObj = withContext(Dispatchers.IO) {
                            db.memoDao().getCounterValue(setting.id)
                        }
                        text = (valueObj?.count ?: 0).toString()
                    }

                    setOnClickListener {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

                        CoroutineScope(Dispatchers.Main).launch {
                            val currentSetting = withContext(Dispatchers.IO) {
                                db.memoDao().getAppSetting()
                            }

                            val isFlashEnabled = currentSetting?.showFlashEffect ?: true
                            val isMaxBrightnessEnabled = currentSetting?.useMaxBrightness ?: true

                            val params = containerLayout?.layoutParams as? WindowManager.LayoutParams
                            if (isMaxBrightnessEnabled && params != null) {
                                params.screenBrightness = 1.0f
                                windowManager.updateViewLayout(containerLayout, params)
                            }

                            if (isFlashEnabled) {
                                val containerDrawable = containerLayout?.background as? GradientDrawable
                                if (containerDrawable != null) {
                                    containerDrawable.setColor(btnColor)
                                    containerLayout?.invalidate()
                                }
                            }

                            if (isFlashEnabled || isMaxBrightnessEnabled) {
                                it.postDelayed({
                                    if (isFlashEnabled) {
                                        val resetDrawable = containerLayout?.background as? GradientDrawable
                                        resetDrawable?.setColor(Color.parseColor("#333333"))
                                        containerLayout?.invalidate()
                                    }
                                    if (isMaxBrightnessEnabled && params != null) {
                                        params.screenBrightness = -1.0f
                                        windowManager.updateViewLayout(containerLayout, params)
                                    }
                                }, 100)
                            }
                        }

                        CoroutineScope(Dispatchers.Main).launch {
                            val nextCount = withContext(Dispatchers.IO) {
                                val currentValueObj = db.memoDao().getCounterValue(setting.id)
                                val currentCount = currentValueObj?.count ?: 0
                                val next = currentCount + 1
                                val newValueObj = CounterValue(counterId = setting.id, count = next)
                                db.memoDao().insertCounterValue(newValueObj)
                                next
                            }
                            text = nextCount.toString()
                        }
                    }
                }

                val btnParams = LinearLayout.LayoutParams((75 * density).toInt(), cellHeightPx).apply {
                    setMargins((2 * density).toInt(), 0, (2 * density).toInt(), 0)
                }
                containerLayout?.addView(btn, btnParams)
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 100
            }

            @android.annotation.SuppressLint("ClickableViewAccessibility")
            dragHandle.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if (event == null) return false
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val diffX = event.rawX - initialTouchX
                            val diffY = event.rawY - initialTouchY

                            if (java.lang.Math.abs(diffX) > 5 || java.lang.Math.abs(diffY) > 5) {
                                params.x = initialX + diffX.toInt()
                                params.y = initialY + diffY.toInt()
                                windowManager.updateViewLayout(containerLayout, params)
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            windowManager.addView(containerLayout, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        containerLayout?.let {
            windowManager.removeView(it)
        }
    }
}