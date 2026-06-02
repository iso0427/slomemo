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

    companion object {
        const val ACTION_STOP_SERVICE = "STOP_OVERLAY_SERVICE"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            // 通知の「カウンターを消去」ボタンが押された場合
            if (intent.action == ACTION_STOP_SERVICE) {
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("overlay_running", false).apply()

                stopSelf()
                return START_NOT_STICKY
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
            val channel = NotificationChannel(channelId, "常駐カウンター", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val flagImmutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or flagImmutable
        )

        // 🟢 タイポを修正した安全な通知ビルド処理
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("常駐カウンター起動中")
                .setContentText("画面上のボタンからカウントできます")
                .setSmallIcon(android.R.drawable.ic_input_add)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "カウンターを消去", stopPendingIntent)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("常駐カウンター起動中")
                .setContentText("画面上のボタンからカウントできます")
                .setSmallIcon(android.R.drawable.ic_input_add)
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

    // 🟢 カウンターを作った時の「本物の設定色」を100%連動して反映させる関数
    private fun recreateCounters() {
        containerLayout?.let {
            windowManager.removeView(it)
        }

        val density = resources.displayMetrics.density
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedHeight = prefs.getInt("counter_height", 45)
        val cellHeightPx = (savedHeight * density).toInt()

        containerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((2 * density).toInt(), (2 * density).toInt(), (2 * density).toInt(), (2 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#333333"))
                cornerRadius = 8 * density
            }
        }

        // 左端の移動用つまみ
        val dragHandle = TextView(this).apply {
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

        CoroutineScope(Dispatchers.Main).launch {
            val counterSettings = withContext(Dispatchers.IO) {
                db.memoDao().getCounterSettingsByMachineDirect(targetMachineId)
            }

            val activeSettings = counterSettings

            activeSettings.forEachIndexed { index, setting ->
                val btn = Button(this@OverlayService).apply {
                    textSize = 24f // デジタルフォント用に見やすく少し大きめ

                    // 🟢 1. 文字色を黒に設定
                    setTextColor(Color.BLACK)

                    // 🟢 2. fontフォルダにある「dseg7classic_bold.ttf」を適用
                    try {
                        val customTypeface = androidx.core.content.res.ResourcesCompat.getFont(
                            this@OverlayService,
                            R.font.dseg7classic_bold
                        )
                        typeface = customTypeface
                    } catch (e: Exception) {
                        setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                    }

                    // 🟢 3. 本体の設定色（Long/Int型）を安全に取得
                    val btnColor = try {
                        (setting.color as Number).toInt()
                    } catch (e: Exception) {
                        Color.parseColor("#37474F") // エラー時のグレー
                    }

                    // 🟢 4. ボタンの背景色として適用（ここで btnColor を使うので、定義より下に書く必要があります）
                    background = GradientDrawable().apply {
                        setColor(btnColor)
                        cornerRadius = 6 * density
                    }

                    // 初期値を非同期で読み込んでセット
                    CoroutineScope(Dispatchers.Main).launch {
                        val valueObj = withContext(Dispatchers.IO) {
                            db.memoDao().getCounterValue(setting.id)
                        }
                        text = (valueObj?.count ?: 0).toString()
                    }

                    // タップ時のカウントアップ処理
                    setOnClickListener {
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

                // ボタンのサイズ・余白設定
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

            // 方法①のアノテーションをここに付与して黄色線を完全に消去
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