package timewheel;

/**
 * 存储任务的容器
 * @author chenjw
 * @version 1.0
 * @date 2022/7/21 15:19
 */
public class TimerTaskEntry implements Comparable<TimerTaskEntry> {

    private TimeWheelTask task;

    /**
     * 任务的过期时间，此处的过期时间设置的过期间隔+系统当前时间（毫秒）
     */
    private long expireMs;

    /**
     * 当前任务属于哪一个列表
     */
    volatile TimerTaskList timedTaskList;

    /**
     * 任务链表指针
     */
    volatile TimerTaskEntry next;
    volatile TimerTaskEntry prev;

    public TimerTaskEntry(TimeWheelTask timeWheelTask, long expireMs) {
        this.task = timeWheelTask;
        this.expireMs = expireMs;
        // 传递进来任务TimerTask，并设置TimerTask的包装类
        if(timeWheelTask != null) {
            task.setTimerTaskEntry(this);
        }
    }

    public TimeWheelTask getTask() {
        return task;
    }

    public void setTask(TimeWheelTask task) {
        this.task = task;
    }

    public long getExpireMs() {
        return expireMs;
    }

    public void setExpireMs(long expireMs) {
        this.expireMs = expireMs;
    }

    public TimerTaskList getTimedTaskList() {
        return timedTaskList;
    }

    public void setTimedTaskList(TimerTaskList timedTaskList) {
        this.timedTaskList = timedTaskList;
    }

    @Override
    public int compareTo(TimerTaskEntry o) {
        return Long.compare(expireMs, o.expireMs);
    }

    void remove() {
        TimerTaskList currentList = timedTaskList;
        while (currentList != null) {
            currentList.remove(this);
            currentList = timedTaskList;
        }
    }

    /**
     * 任务的取消，就是判断任务TimerTask的Entry是否是当前任务
     * @return
     */
    public boolean cancel() {
        return task.getTimerTaskEntry() != this;
    }

}
