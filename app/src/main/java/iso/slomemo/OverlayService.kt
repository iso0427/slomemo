package iso.slomemo

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
    private var isAppActive: Boolean = false

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_OVERLAY_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            // アプリ側や外部から停止命令が来たら、Viewを消してサービスを終了する
            if (intent.action == ACTION_STOP_SERVICE) {
                // 🟢 修正：古い端末から最新の端末まで赤線（エラー）が出ない安全なバイブレーションの実装
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_CLICK)
                    } else {
                        android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                    }
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }

                // 確実にアプリ内のスイッチ状態もOFF（false）に書き換えます
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("overlay_running", false).apply()

                containerLayout?.let {
                    if (it.parent != null) {
                        try {
                            windowManager.removeView(it)
                        } catch (e: IllegalArgumentException) {
                            // 既に削除されている場合はスキップ
                        }
                    }
                }
                containerLayout = null

                stopSelf()
                return START_NOT_STICKY
            }

            // 画面の状態に合わせてViewの可視性を切り替えるだけ
            when (intent.action) {
                "ACTION_HIDE_OVERLAY" -> {
                    isAppActive = true
                    containerLayout?.visibility = View.GONE
                }
                "ACTION_SHOW_OVERLAY" -> {
                    isAppActive = false
                    containerLayout?.visibility = View.VISIBLE
                }
            }

            val machineId = intent.getIntExtra("TARGET_MACHINE_ID", -1)
            if (machineId != -1) {
                targetMachineId = machineId
                recreateCounters()
            }
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        // 🟢 修正：通知に関する処理（startForegroundなど）をすべて削除

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
            val savedHeight = prefs.getInt("counter_overlay_height", 60)
            val cellHeightPx = (savedHeight * density).toInt()

            containerLayout = LinearLayout(this@OverlayService).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((5 * density).toInt(), (5 * density).toInt(), (5 * density).toInt(), (5 * density).toInt())

                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#333333"))
                    cornerRadius = 8 * density
                }

                visibility = if (isAppActive) View.GONE else View.VISIBLE
            }

            val dragHandle = TextView(this@OverlayService).apply {
                text = "⁝⁝"
                textSize = 24f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
                val handleWidth = (24 * density).toInt()
                layoutParams = LinearLayout.LayoutParams(handleWidth, cellHeightPx).apply {
                    setMargins((4 * density).toInt(), 0, (4 * density).toInt(), 0)
                }
            }
            containerLayout?.addView(dragHandle)

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

                    // 🟢 修正：単体タップでカウントアップ（+1）
                    setOnClickListener {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

                        // エフェクト（フラッシュ・最大輝度）の既存処理を実行
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

                        // カウントアップ（+1）処理
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

                    // 🟢 修正：長押しでカウントダウン（ただし0未満には下げない）
                    setOnLongClickListener {
                        it.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)

                        CoroutineScope(Dispatchers.Main).launch {
                            val nextCount = withContext(Dispatchers.IO) {
                                val currentValueObj = db.memoDao().getCounterValue(setting.id)
                                val currentCount = currentValueObj?.count ?: 0

                                // 🟢 修正：計算結果が0未満にならないよう、kotlin.math.max を使って0でストップさせます
                                val next = kotlin.math.max(0, currentCount - 1)

                                val newValueObj = CounterValue(counterId = setting.id, count = next)
                                db.memoDao().insertCounterValue(newValueObj)
                                next
                            }
                            text = nextCount.toString()
                        }
                        true
                    }
                }

                val btnParams = LinearLayout.LayoutParams((75 * density).toInt(), cellHeightPx).apply {
                    setMargins((2 * density).toInt(), 0, (2 * density).toInt(), 0)
                }
                containerLayout?.addView(btn, btnParams)
            }

            // 🟢 修正：アイコンを「📄」に変更し、すっきりした見た目に設定
            val openAppBtn = TextView(this@OverlayService).apply {
                text = "📄" // 書類アイコン
                textSize = 18f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER // 文字を中央にぴったり配置
                setPadding(0, 0, 0, 0)

                // TextViewをクリック可能にする設定
                isClickable = true
                isFocusable = true

                // 通常タップ：アプリ（MainActivity）を起動
                setOnClickListener {
                    it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

                    val launchIntent = Intent(this@OverlayService, MainActivity::class.java).apply {
                        action = "ACTION_OPEN_MEMO_FROM_OVERLAY"
                        putExtra("TARGET_MACHINE_ID", targetMachineId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    startActivity(launchIntent)
                }

                // 🟢 追加：長押し（ロングタップ）で常駐カウンターを完全に非表示（終了）にする
                setOnLongClickListener {
                    // 自分自身（OverlayService）の停止アクションを呼び出す
                    val stopIntent = Intent(this@OverlayService, OverlayService::class.java).apply {
                        action = ACTION_STOP_SERVICE
                    }
                    startService(stopIntent)
                    true // イベントを消費して通常のタップが同時に走るのを防ぐ
                }
            }

            // サイズは左側のつまみと同じ幅24dpに据え置き
            val openAppParams = LinearLayout.LayoutParams((24 * density).toInt(), cellHeightPx).apply {
                setMargins((6 * density).toInt(), 0, (4 * density).toInt(), 0)
            }
            containerLayout?.addView(openAppBtn, openAppParams)

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

            // 🟢 修正：通知の重要度を引き上げてサイレントに入らないようにする
            val channelId = "overlay_counter_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = android.app.NotificationChannel(
                    channelId,
                    "常駐カウンター連動",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT // 🟢 サイレントを回避する標準の重要度に変更
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                manager.createNotificationChannel(notificationChannel)
            }

            // 🟢 追加：通知のボタンが押されたときにサービスを止めるためのインテントを用意
            val stopIntent = Intent(this@OverlayService, OverlayService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            val stopPendingIntent = android.app.PendingIntent.getService(
                this@OverlayService,
                0,
                stopIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(this@OverlayService, channelId)
                .setContentTitle("スロメモ常駐カウンター")
                .setContentText("カウンター表示中")
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setOngoing(true)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel, // ×ボタンなどのアイコン
                    "常駐カウンターを非表示", // 通知に表示されるボタンの文字
                    stopPendingIntent // タップ時に実行する処理
                )
                .build()

            startForeground(1001, notification)

            windowManager.addView(containerLayout, params)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        containerLayout?.let {
            if (it.parent != null) {
                try {
                    windowManager.removeView(it)
                } catch (e: IllegalArgumentException) {
                }
            }
        }
        containerLayout = null
    }
}