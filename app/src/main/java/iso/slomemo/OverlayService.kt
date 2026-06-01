package iso.slomemo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingButton: Button? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        // 🟢 Androidのルール上、裏で常駐するために必須の「通知」を出す処理
        val channelId = "overlay_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "常駐カウンター",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("常駐カウンター起動中")
                .setContentText("画面上のボタンからカウントできます")
                .setSmallIcon(android.R.drawable.ic_input_add)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("常駐カウンター起動中")
                .setContentText("画面上のボタンからカウントできます")
                .setSmallIcon(android.R.drawable.ic_input_add)
                .build()
        }

        startForeground(1, notification)

        // 🟢 画面の上にボタンを浮かせる処理
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // ボタンの見た目や設定
        floatingButton = Button(this).apply {
            text = "+"
            textSize = 24f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#AA000000")) // 半透明の黒
        }

        // メモ帳（SharedPreferences）から保存された高さを読み込む（保存がなければデフォルト値は 45）
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val savedHeight = prefs.getInt("counter_height", 45)

        // 設定値（1〜5段階相当の数値）を、スマホの画面に合わせた実際のピクセル数に変換する計算
        // 例: 30dp -> 画面に合わせたピクセル数へ変換
        val density = resources.displayMetrics.density
        val btnHeightPx = (savedHeight * density).toInt()
        val btnWidthPx = (150 / 3) // 幅は一旦今の150を基準に調整用（後ほど使用）

        // 画面のどこに配置するか、重ね合わせのルール設定
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            btnHeightPx,
            btnHeightPx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // キーボードを奪わない設定
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START // 左上を基準にする
            x = 100 // 初期位置X
            y = 100 // 初期位置Y
        }

        // 🟢 ボタンを指でスライドして動かせるようにする処理（ドラッグ対応）
        floatingButton?.setOnTouchListener(object : View.OnTouchListener {
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
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingButton, params) // 位置を更新
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // 指を離した時、移動距離が少なければ「タップ」とみなす
                        val diffX = event.rawX - initialTouchX
                        val diffY = event.rawY - initialTouchY
                        if (java.lang.Math.abs(diffX) < 10 && java.lang.Math.abs(diffY) < 10) {
                            v?.performClick()
                        }
                        return true
                    }
                }
                return false
            }
        })

        // 🟢 ボタンがタップされた時の処理（仮でログを出す状態にしておきます）
        floatingButton?.setOnClickListener {
            android.util.Log.d("SloMemoDebug", "常駐ボタンがタップされました！")
        }

        // 実際に画面へボタンを登録
        windowManager.addView(floatingButton, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        // サービス終了時に画面からボタンを消去する
        if (floatingButton != null) {
            windowManager.removeView(floatingButton)
        }
    }
}