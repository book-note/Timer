package com.merpyzf


import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.*
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.NotificationTarget
import com.merpyzf.model.Book
import com.merpyzf.timer.R

const val ACTION_RESUME_OR_PAUSE_COUNT = "start_count"
const val ACTION_FINISH_READ = "finish_read"
const val TAG = "wk"

class ReadTimerService : Service() {
    private lateinit var book: Book
    private lateinit var timerBinder: TimerBinder
    private lateinit var receiver: BroadcastReceiver
    private lateinit var notification: Notification
    private lateinit var notificationLayout: RemoteViews
    private val channelId = "channelId"
    private val notificationId = 1

    private var screenOffTimeMillis = 0L
    private var screenOnTimeMillis = 0L


    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        receiver = registerScreenBroadcastReceiver(context = this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand")
        intent?.let {
            book = Book(it.getStringExtra("name")!!, it.getStringExtra("cover")!!)
            startForegroundNotification(this, book)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        unregisterReceiver(receiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        val name = intent.getStringExtra("name")!!
        val cover = intent.getStringExtra("cover")!!
        timerBinder = TimerBinder(this, Book(name, cover))
        // init notification status
        updateNotificationTimingStatus(this)
        Log.d(TAG, "onBind")
        return timerBinder
    }

    private fun registerScreenBroadcastReceiver(context: Context): ScreenBroadcastReceiver {
        val filter = IntentFilter()
            .apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SHUTDOWN)
                addAction(ACTION_RESUME_OR_PAUSE_COUNT)
                addAction(ACTION_FINISH_READ)
            }
        val receiver = ScreenBroadcastReceiver()
        context.registerReceiver(receiver, filter)
        return receiver
    }

    inner class TimerBinder(private val context: Service, private var book: Book) : Binder() {
        var timingListener: Timer.OnTimingListener? = null
        private var timer: Timer? = null

        /**
         * start timing
         * @param isCountDown countdown flag
         * @param countDownSeconds countdown seconds
         */
        fun startTiming(isCountDown: Boolean = false, countDownSeconds: Long = 0L) {
            if (this.timer != null && this.timer!!.isRunning()) {
                return
            }
            this.timer = Timer()
            this.timer!!.timingListener = object : Timer.OnTimingListener {
                override fun onStatusChanged(status: Timer.Status, seconds: Long) {
                    Handler(Looper.getMainLooper()).post {
                        timingListener?.onStatusChanged(status, seconds)
                        updateNotificationTiming(context, status.statusName, seconds)
                        Log.d(TAG, "onStatusChanged: ${status.statusName} • ${seconds}s")
                    }
                }

                override fun onTick(status: Timer.Status, seconds: Long) {
                    // todo service 中根据此状态的变更执行相应的操作
                    Handler(Looper.getMainLooper()).post {
                        timingListener?.onTick(status, seconds)
                        updateNotificationTiming(context, status.statusName, seconds)
                        Log.d(TAG, "onStatusChanged: ${status.statusName} • ${seconds}s")
                    }
                }
            }
            this.timer!!.startTiming(isCountDown, countDownSeconds)
            updateNotificationTimingStatus(this@ReadTimerService)
        }

        /**
         * stop timing
         */
        fun stopTiming() {
            this.timer?.stopTiming()
            updateNotificationTimingStatus(this@ReadTimerService)
        }

        /**
         * pause timing
         */
        fun pauseTiming(screenOff: Boolean = false) {
            this.timer?.pauseTiming(screenOff)
            updateNotificationTimingStatus(this@ReadTimerService)
        }

        /**
         * resume timing
         * @param pauseDuration from pause to resume duration
         */
        fun resumeTiming(pauseDuration: Long = 0) {
            this.timer?.resumeTiming(pauseDuration)
            updateNotificationTimingStatus(this@ReadTimerService)
        }

        /**
         * get current timer status
         */
        fun currentStatus(): Timer.Status {
            return timer?.currStatus ?: Timer.Status.PREPARE
        }
    }

    private fun startForegroundNotification(context: Service, book: Book) {
        notificationLayout = RemoteViews(packageName, R.layout.notification_large)
        notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_timer)
            .setTicker("${book.name} • 阅读中")
            .setOnlyAlertOnce(true)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_timer))
            .setCustomBigContentView(notificationLayout)
            .setContentTitle("${book.name} • 阅读中")
            .build()

        // 设置书籍封面
        val notificationTarget = NotificationTarget(
            context,
            R.id.ivBookCover,
            notificationLayout,
            notification,
            notificationId
        )
        Glide.with(context)
            .asBitmap()
            .error(R.mipmap.ic_launcher)
            .placeholder(R.mipmap.ic_launcher)
            .apply(RequestOptions.bitmapTransform(RoundedCorners(26)))
            .override(160.px, 200.px) // 此处 如果图像分辨率过大自定义Notification的内容将无法正常显示
            .load(book.cover)
            .into(notificationTarget)
        // 设置书籍名称
        notificationLayout.setTextViewText(R.id.bookName, book.name)

        // 设置点击事件
        val startCountPendingIntent =
            PendingIntent.getBroadcast(context, 1, Intent(ACTION_RESUME_OR_PAUSE_COUNT), 0)
        val finishReadPendingIntent =
            PendingIntent.getBroadcast(context, 1, Intent(ACTION_FINISH_READ), 0)
        notificationLayout.setOnClickPendingIntent(R.id.statusContainer, startCountPendingIntent)
        notificationLayout.setOnClickPendingIntent(R.id.stopContainer, finishReadPendingIntent)

        createNotificationChannel()
        context.startForeground(notificationId, notification)
    }

    private fun updateNotificationTiming(context: Service, status: String, seconds: Long) {
        notificationLayout.setTextViewText(R.id.tvCountInfo, "${seconds.toHms()} • $status")
        context.startForeground(notificationId, notification)
    }

    private fun updateNotificationTimingStatus(context: Service) {
        val status = timerBinder.currentStatus()
        when (status) {
            Timer.Status.PREPARE -> {
                notificationLayout.setViewVisibility(R.id.stopContainer, View.GONE)
                notificationLayout.setViewVisibility(R.id.statusContainer, View.VISIBLE)
                notificationLayout.setTextViewText(R.id.tvNextStatus, "开始")
                notificationLayout.setImageViewResource(R.id.ivStatus, R.drawable.ic_action_play)
            }
            Timer.Status.RUNNING -> {
                notificationLayout.setViewVisibility(R.id.stopContainer, View.VISIBLE)
                notificationLayout.setViewVisibility(R.id.statusContainer, View.VISIBLE)
                notificationLayout.setTextViewText(R.id.tvNextStatus, "暂停")
                notificationLayout.setImageViewResource(R.id.ivStatus, R.drawable.ic_action_pause)
            }
            Timer.Status.PAUSE -> {
                notificationLayout.setTextViewText(R.id.tvNextStatus, "继续")
                notificationLayout.setImageViewResource(R.id.ivStatus, R.drawable.ic_action_play)
            }
            else -> {
                // when the timing stops, hidden status action.
                notificationLayout.setViewVisibility(R.id.statusContainer, View.GONE)
                notificationLayout.setViewVisibility(R.id.stopContainer, View.GONE)
            }
        }
        context.startForeground(notificationId, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "阅读计时"
            val description = "用于展示当前的阅读时间"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance)
            channel.description = description
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun stopTimerService() {
        stopForeground(true)
        stopSelf()
    }

    inner class ScreenBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        if (timerBinder.currentStatus() != Timer.Status.PAUSE_CAUSE_SCREEN_OFF) {
                            return
                        }
                        screenOnTimeMillis = System.currentTimeMillis()
                        Log.i(
                            "wk",
                            "duration: ${(screenOnTimeMillis - screenOffTimeMillis) / 1000}"
                        )
                        timerBinder.resumeTiming((screenOnTimeMillis - screenOffTimeMillis) / 1000)
                        screenOffTimeMillis = 0L
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        if (timerBinder.currentStatus() != Timer.Status.RUNNING) {
                            return
                        }
                        screenOffTimeMillis = System.currentTimeMillis()
                        timerBinder.pauseTiming(true)
                    }
                    Intent.ACTION_SHUTDOWN -> {
                        Log.i("wk", "the device will shutdown，do some save action.")
                        timerBinder.stopTiming()
                        stopTimerService()
                    }
                    ACTION_RESUME_OR_PAUSE_COUNT -> {
                        if (timerBinder.currentStatus() == Timer.Status.PREPARE) {
                            timerBinder.startTiming()
                        } else if (timerBinder.currentStatus() == Timer.Status.RUNNING) {
                            timerBinder.pauseTiming()
                        } else if (timerBinder.currentStatus() == Timer.Status.PAUSE) {
                            timerBinder.resumeTiming()
                        }
                        updateNotificationTimingStatus(this@ReadTimerService)
                    }
                    ACTION_FINISH_READ -> {
                        if (timerBinder.currentStatus() != Timer.Status.PREPARE || timerBinder.currentStatus() != Timer.Status.STOP) {
                            timerBinder.stopTiming()
                            updateNotificationTimingStatus(this@ReadTimerService)
                        }
                    }
                    else -> {

                    }
                }
            }
        }
    }

    val Int.dp: Int
        get() {
            return (this / getResources().getDisplayMetrics().density).toInt()
        }
    val Int.px: Int
        get() {
            return (this * getResources().getDisplayMetrics().density).toInt()
        }

    fun Long.toHms(): String {
        val h = this / 3600
        val m = this % 3600 / 60
        val s = this % 60

        val hStr = if (h < 10) {
            "0$h"
        } else {
            "$h"
        }
        val mStr = if (m < 10) {
            "0$m"
        } else {
            "$m"
        }

        val sStr = if (s < 10) {
            "0$s"
        } else {
            "$s"
        }
        return "$hStr : $mStr : $sStr"
    }
}