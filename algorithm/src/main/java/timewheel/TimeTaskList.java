package timewheel;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 存储任务的环形链表
 * @author chenjw
 * @version 1.0
 * @date 2022/7/21 15:18
 */
class TimerTaskList implements Delayed {
    /**
     * TimerTaskList 环形链表使用一个虚拟根节点root
     */
    private TimerTaskEntry root;

    /**
     * bucket的过期时间
     */
    private AtomicLong expiration = new AtomicLong(-1L);

    public TimerTaskList() {
        this.root = new TimerTaskEntry(null, -1);
        root.next = root;
        root.prev = root;
    }

    public long getExpiration() {
        return expiration.get();
    }

    /**
     * 设置bucket的过期时间,设置成功返回true
     *
     * @param expirationMs
     * @return
     */
    boolean setExpiration(long expirationMs) {
        return expiration.getAndSet(expirationMs) != expirationMs;
    }

    /**
     * 添加任务到列表中
     * @param entry
     * @return
     */
    public boolean addTask(TimerTaskEntry entry) {
        boolean done = false;
        // 循环的目的是为了保证删除更新成功
        while (!done) {
            // 如果TimerTaskEntry已经在别的list中就先移除
            entry.remove();
            synchronized (this) {
                synchronized (entry) {
                    if (entry.timedTaskList == null) {
                        // 加到链表的末尾
                        entry.timedTaskList = this;
                        TimerTaskEntry tail = root.prev;
                        entry.prev = tail;
                        entry.next = root;
                        tail.next = entry;
                        root.prev = entry;
                        done = true;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 从 TimedTaskList 移除指定的 timerTaskEntry
     *
     * @param entry
     */
    public void remove(TimerTaskEntry entry) {
        synchronized (this) {
            synchronized (entry) {
                if (entry.getTimedTaskList().equals(this)) {
                    entry.next.prev = entry.prev;
                    entry.prev.next = entry.next;
                    entry.next = null;
                    entry.prev = null;
                    entry.timedTaskList = null;
                }
            }
        }
    }

    /**
     * 移除所有
     * @param entryConsumer
     */
    public synchronized void flush(Consumer<TimerTaskEntry> entryConsumer) {
        TimerTaskEntry node = root.next;
        while (!node.equals(root)) {
            remove(node);
            entryConsumer.accept(node);
            node = root.next;
        }
        expiration.set(-1L);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return Math.max(0, unit.convert(expiration.get() - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof TimerTaskList) {
            return Long.compare(expiration.get(), ((TimerTaskList) o).getExpiration());
        }
        return 0;
    }
}
