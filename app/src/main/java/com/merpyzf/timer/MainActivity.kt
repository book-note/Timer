package com.merpyzf.timer

import android.content.*
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.merpyzf.ReadTimerService
import com.merpyzf.Timer
import java.io.File

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var textView: TextView
    private lateinit var btnStart: Button
    private lateinit var btnStartCountdown: Button
    private lateinit var btnPause: Button
    private lateinit var btnResume: Button
    private lateinit var btnStop: Button
    private lateinit var btnPlayMusic: Button
    private lateinit var btnStartMusic: Button
    private lateinit var btnPauseMusic: Button
    private lateinit var btnPlayNext: Button
    private lateinit var btnPlayPre: Button


    private lateinit var musicPlayList: Array<String>
    private lateinit var serviceIntent: Intent
    private lateinit var myServiceBinder: ReadTimerService.MyServiceBinder
    private var currPlayMusicPos = 0

    init {
        val rootPath = Environment.getExternalStorageDirectory().path
        musicPlayList = arrayOf(
            File(rootPath, "ocean.mp3").absolutePath,
            File(rootPath, "rainfall.mp3").absolutePath,
            File(rootPath, "thunder.mp3").absolutePath
        )
    }

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
        btnPlayMusic = findViewById(R.id.btnPlayMusic)
        btnPauseMusic = findViewById(R.id.btnPauseMusic)
        btnPlayNext = findViewById(R.id.btnPlayNextMusic)
        btnPlayPre = findViewById(R.id.btnPlayPreMusic)
        btnStartMusic = findViewById(R.id.btnStartMusic)


        btnStart.setOnClickListener(this)
        btnPause.setOnClickListener(this)
        btnStartCountdown.setOnClickListener(this)
        btnResume.setOnClickListener(this)
        btnStop.setOnClickListener(this)


        btnPlayMusic.setOnClickListener(this)
        btnPauseMusic.setOnClickListener(this)
        btnPlayNext.setOnClickListener(this)
        btnPlayPre.setOnClickListener(this)
        btnStartMusic.setOnClickListener(this)
    }

    private fun startService(context: Context) {
        serviceIntent = Intent(context, ReadTimerService::class.java)
            .apply {
                putExtra("name", "后资本主义时代")
                putExtra("cover", "https://img2.doubanio.com/view/subject/l/public/s33846813.jpg")
            }
        startService(serviceIntent)
        bindService(intent, MyConnection(), Context.BIND_IMPORTANT)
    }

    inner class MyConnection : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            myServiceBinder = service as ReadTimerService.MyServiceBinder
            myServiceBinder.timingListener = object : Timer.OnTimingListener {
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
                    myServiceBinder.startTiming()
                }
                // 开始倒计时
                R.id.btnStartCountdown -> {
                    myServiceBinder.startTiming(isCountDown = true, countDownSeconds = 10000)
                }
                // 暂停计时
                R.id.btnPause -> {
                    myServiceBinder.pauseTiming()
                }
                // 恢复计时
                R.id.btnResume -> {
                    myServiceBinder.resumeTiming()
                }
                // 停止计时
                R.id.btnStop -> {
                    myServiceBinder.stopTiming()
                    stopService(serviceIntent)
                }
                // 播放音乐
                R.id.btnPlayMusic -> {
                    myServiceBinder.playMusic(musicPlayList[currPlayMusicPos % musicPlayList.size])
                }
                // 暂停音乐播放
                R.id.btnPauseMusic -> {
                    myServiceBinder.pauseMusic()
                }
                // 恢复音乐播放
                R.id.btnStartMusic ->{
                    myServiceBinder.resumeMusic()
                }
                // 播放下一首音乐
                R.id.btnPlayNextMusic -> {
                    currPlayMusicPos++
                    myServiceBinder.playMusic(musicPlayList[currPlayMusicPos % musicPlayList.size])
                }
                // 播放前一首音乐
                R.id.btnPlayPreMusic -> {
                    if (currPlayMusicPos != 0){
                        currPlayMusicPos--
                    }
                    myServiceBinder.playMusic(musicPlayList[currPlayMusicPos % musicPlayList.size])
                }
                else->{

                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}