package com.merpyzf

/**
 * Description: 计时器（提供正向计时和倒计时两种计时方式）
 * Date: 4/2/21
 * @author wangke
 *
 */
class Timer : Thread() {
    // 开始时的秒数
    private var startSeconds = 0L

    // 统计计的秒数
    private var totalSeconds = 0L

    // 倒计时的标记
    private var isCountDown = false

    // 倒计时的总秒数
    private var countDownSeconds: Long = 0L

    // 当前计时器的状态
    var currStatus: Status = Status.PREPARE
    var timingListener: OnTimingListener? = null


    override fun run() {
        super.run()
        while (currStatus != Status.STOP) {
            if (currStatus == Status.RUNNING) {
                if (isCountDown) {
                    timingListener?.onTick(currStatus, startSeconds)
                    if (startSeconds == 0L) {
                        currStatus = Status.STOP
                        timingListener?.onStatusChanged(Status.STOP, totalSeconds)
                        break
                    }
                    startSeconds -= 1
                } else {
                    timingListener?.onTick(currStatus, totalSeconds)
                }
                totalSeconds++
                sleep(1 * 1000)
            }
        }
    }

    /**
     * 开始计时
     * @param isCountDown 倒计时标记
     * @param countDownSeconds 倒计时的秒数
     */
    fun startTiming(isCountDown: Boolean = false, countDownSeconds: Long = 0L) {
        if (this.currStatus == Status.PREPARE || this.currStatus == Status.STOP) {
            this.isCountDown = isCountDown
            this.countDownSeconds = countDownSeconds
            this.startSeconds = countDownSeconds
            this.currStatus = Status.RUNNING
            this.totalSeconds = 0
            start()
        }
    }

    /**
     * 停止计时
     */
    fun stopTiming() {
        if (this.currStatus  == Status.RUNNING || this.currStatus == Status.PAUSE) {
            this.currStatus = Status.STOP
            this.timingListener?.onStatusChanged(Status.STOP, totalSeconds)
        }
    }

    /**
     * 暂停计时
     */
    fun pauseTiming(screenOff: Boolean = false) {
        if (this.currStatus == Status.RUNNING) {
            this.currStatus = if (screenOff) {
                Status.PAUSE_CAUSE_SCREEN_OFF
            } else {
                Status.PAUSE
            }
            this.timingListener?.onStatusChanged(this.currStatus, totalSeconds)
        }
    }

    /**
     * 恢复计时
     * @param pauseDuration 从暂停到恢复计时中间的间隔
     */
    fun resumeTiming(pauseDuration: Long = 0) {
        if (this.currStatus == Status.PAUSE || this.currStatus == Status.PAUSE_CAUSE_SCREEN_OFF) {
            if (this.isCountDown) {
                if (totalSeconds + pauseDuration >= countDownSeconds) {
                    this.startSeconds = 0
                    this.currStatus = Status.STOP
                    this.timingListener?.onTick(currStatus, startSeconds)
                    this.timingListener?.onStatusChanged(status = Status.STOP, countDownSeconds)
                } else {
                    this.totalSeconds += pauseDuration
                    this.startSeconds -= pauseDuration
                    this.currStatus = Status.RUNNING
                    this.timingListener?.onTick(currStatus, startSeconds)
                    this.timingListener?.onStatusChanged(
                        status = Status.RUNNING,
                        startSeconds
                    )
                }
            } else {
                this.totalSeconds += pauseDuration
                this.startSeconds += pauseDuration
                this.currStatus = Status.RUNNING
                this.timingListener?.onTick(currStatus, startSeconds)
                this.timingListener?.onStatusChanged(Status.RUNNING, totalSeconds)
            }
        }
    }

    fun isRunning(): Boolean = this.currStatus == Status.RUNNING

    enum class Status(val statusName: String) {
        PREPARE("准备中"),
        PAUSE("暂停"),
        RUNNING("计时中"),
        STOP("停止"),
        PAUSE_CAUSE_SCREEN_OFF("屏幕熄灭暂停暂停计时")
    }

    interface OnTimingListener {
        /**
         * 计时器状态发生变更时调用
         * @param status 当前计时器的状态
         * @param seconds 当前计时的秒数
         */
        fun onStatusChanged(status: Status, seconds: Long)

        /**
         * 嘀嗒嘀嗒
         */
        fun onTick(status: Status, seconds: Long)
    }
}