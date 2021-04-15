package com.merpyzf.timer

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.merpyzf.ReadTimerService
import com.merpyzf.Timer

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var textView: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStartCountdown: Button
    private lateinit var btnPause: Button
    private lateinit var btnResume: Button
    private lateinit var btnStop: Button

    private lateinit var timerBinder: ReadTimerService.TimerBinder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initWidgets()
        startService(this)
    }

    private fun initWidgets() {
        textView = findViewById(R.id.textView)
        btnStart = findViewById(R.id.btnStart)
        btnPause = findViewById(R.id.btnPause)
        btnStartCountdown = findViewById(R.id.btnStartCountdown)
        btnResume = findViewById(R.id.btnResume)
        btnStop = findViewById(R.id.btnStop)

        btnStart.setOnClickListener(this)
        btnPause.setOnClickListener(this)
        btnStartCountdown.setOnClickListener(this)
        btnResume.setOnClickListener(this)
        btnStop.setOnClickListener(this)
    }

    private fun startService(context: Context) {
        val intent = Intent(context, ReadTimerService::class.java)
        intent.putExtra("name", "后资本主义时代")
        intent.putExtra("cover", "https://img2.doubanio.com/view/subject/l/public/s33846813.jpg")
        startService(intent)
        bindService(intent, MyConnection(), Context.BIND_IMPORTANT)
    }

    inner class MyConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as ReadTimerService.TimerBinder
            timerBinder.timingListener = object : Timer.OnTimingListener {
                override fun onStatusChanged(status: Timer.Status, seconds: Long) {
                    textView.text = "${status.statusName} • ${seconds}s"
                }

                override fun onTick(status: Timer.Status, seconds: Long) {
                    textView.text = "${status.statusName} • ${seconds}s"
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            // 关闭 Notification
            Log.i("wk", "onServiceDisconnected")
        }
    }

    override fun onClick(v: View?) {
        v?.let {
            when (it.id) {
                // 开始计时
                R.id.btnStart -> {
                    timerBinder.startTiming()
                }
                // 开始倒计时
                R.id.btnStartCountdown -> {
                    timerBinder.startTiming(isCountDown = true, countDownSeconds = 10000)
                }
                // 暂停计时
                R.id.btnPause -> {
                    timerBinder.pauseTiming()
                }
                // 恢复计时
                R.id.btnResume -> {
                    timerBinder.resumeTiming()
                }
                // 停止计时
                R.id.btnStop -> {
                    timerBinder.stopTiming()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}