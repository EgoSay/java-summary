package timewheel;

import java.util.TimerTask;
import java.util.concurrent.DelayQueue;

/**
 * @author chenjw
 * @version 1.0
 * @date 2022/7/21 15:17
 */
public class TimeWheel {
    /**
     * 一个槽的时间间隔(时间轮最小刻度)
     */
    private long tickMs;

    /**
     * 时间轮大小(槽的个数)
     */
    private int wheelSize;

    /**
     * 一轮的时间跨度
     */
    private long interval;

    private long currentTime;

    /**
     * 槽
     */
    private TimerTaskList[] buckets;

    /**
     * 上层时间轮
     */
    private volatile TimeWheel overflowWheel;

    /**
     * 一个timer只有一个delay queue
     */
    private DelayQueue<TimerTaskList> delayQueue;

    public TimeWheel(long tickMs, int wheelSize, long currentTime, DelayQueue<TimerTaskList> delayQueue) {
        this.tickMs = tickMs;
        this.wheelSize = wheelSize;
        this.interval = tickMs * wheelSize;
        this.buckets = new TimerTaskList[wheelSize];
        this.currentTime = currentTime - (currentTime % tickMs);
        this.delayQueue = delayQueue;
        for (int i = 0; i < wheelSize; i++) {
            buckets[i] = new TimerTaskList();
        }
    }

    public long getTickMs() {
        return tickMs;
    }

    public void setTickMs(long tickMs) {
        this.tickMs = tickMs;
    }

    public int getWheelSize() {
        return wheelSize;
    }

    public void setWheelSize(int wheelSize) {
        this.wheelSize = wheelSize;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }

    public TimerTaskList[] getBuckets() {
        return buckets;
    }

    public void setBuckets(TimerTaskList[] buckets) {
        this.buckets = buckets;
    }

    private TimeWheel getOverflowWheel() {
        if (overflowWheel == null) {
            synchronized (this) {
                if (overflowWheel == null) {
                    overflowWheel = new TimeWheel(interval, wheelSize, currentTime, delayQueue);
                }
            }
        }
        return overflowWheel;
    }

    public void setOverflowWheel(TimeWheel overflowWheel) {
        this.overflowWheel = overflowWheel;
    }

    public DelayQueue<TimerTaskList> getDelayQueue() {
        return delayQueue;
    }

    public void setDelayQueue(DelayQueue<TimerTaskList> delayQueue) {
        this.delayQueue = delayQueue;
    }

    public boolean add(TimerTaskEntry entry) {
        long expireMs = entry.getExpireMs();
        if (expireMs < tickMs + currentTime) {
            // 到期了
            return false;
        } else if (expireMs < currentTime + interval) {
            // 扔进当前时间轮的某个槽里，只有时间大于某个槽，才会放进去
            // 计算槽位 ID
            long virtualId = (expireMs / tickMs);
            // 计算分层索引
            int index = (int) (virtualId % wheelSize);
            TimerTaskList bucket = buckets[index];
            bucket.addTask(entry);
            // 设置过期时间
            if (bucket.setExpiration(virtualId * tickMs)) {
                delayQueue.offer(bucket);
                return true;
            }
        } else {
            // 当前轮不满足，需要放到上层时间轮
            TimeWheel overflowWheel = getOverflowWheel();
            return overflowWheel.add(entry);
        }
        return false;
    }

    /**
     * 推进指针
     * @param timestamp
     */
    public void advanceLock(long timestamp) {
        if (timestamp > currentTime + tickMs) {
            currentTime = timestamp - (timestamp % tickMs);
            if (overflowWheel != null) {
                this.getOverflowWheel().advanceLock(timestamp);
            }
        }
    }

}


