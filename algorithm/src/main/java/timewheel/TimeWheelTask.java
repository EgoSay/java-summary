package timewheel;

/**
 * @author chenjw
 * @version 1.0
 * @date 2022/7/21 17:18
 */
public class TimeWheelTask implements Runnable {

    /**
     * 表示当前任务延迟多久后执行 (单位 ms)，比如说延迟3s，则此值为3000
     */
    private long delayMs;
    /**
     * 任务所在的entry
     */
    private TimerTaskEntry timerTaskEntry;

    private String desc;

    private volatile String status;

    public TimeWheelTask(long delayMs, String desc) {
        this.delayMs = delayMs;
        this.desc = desc;
        this.timerTaskEntry = null;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public String getDesc() {
        return desc;
    }

    public TimerTaskEntry getTimerTaskEntry() {
        return timerTaskEntry;
    }

    public void setTimerTaskEntry(TimerTaskEntry entry) {
        // 如果这个 task 已经被一个已存在的TimerTaskEntry持有,先移除一个
        if (timerTaskEntry != null && timerTaskEntry != entry) {
            timerTaskEntry.remove();
        }
        timerTaskEntry = entry;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public void run() {
        // do something
        System.out.println(System.currentTimeMillis() + ", 开始执行任务: " + desc);
        timerTaskEntry.remove();
        timerTaskEntry = null;
        status = "end";
    }

    enum EnumTaskStatus {
        /**
         * 任务状态
         */
        SCHEDULE(1, "调度"),
        PREPARED(2, "待执行"),
        KILL(3, "被杀"),
        CANCELL(4, "取消"),
        FAIL(5, "执行失败"),
        SUCCESS(6, "执行成功"),
        RUNNING(7, "执行中");

        private int code;
        private String desc;

        EnumTaskStatus() {
        }

        EnumTaskStatus(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }
}
