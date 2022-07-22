package timewheel;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author chenjw
 * @version 1.0
 * @date 2022/7/21 17:06
 */
public class SystemTimer implements Timer {

    /**
     * 底层时间轮
     */
    private TimeWheel timeWheel;
    /**
     * 一个Timer只有一个延时队列
     */
    private DelayQueue<TimerTaskList> delayQueue = new DelayQueue<>();
    /**
     * 过期任务执行线程
     */
    private ExecutorService workerThreadPool;
    /**
     * 轮询delayQueue获取过期任务线程
     */
    private ExecutorService bossThreadPool;

    private boolean finish;

    /**
     * 一个槽的时间间隔(时间轮最小刻度)
     */
    private long tickMs = 1L;

    /**
     * 时间轮大小(槽的个数)
     */
    private int wheelSize = 20;

    public SystemTimer(long tickMs, int wheelSize) {
        this.timeWheel = new TimeWheel(tickMs, wheelSize, System.currentTimeMillis(), delayQueue);
        this.workerThreadPool = Executors.newFixedThreadPool(10);
        this.bossThreadPool = Executors.newFixedThreadPool(1);
        // 推动时间轮运转
        this.bossThreadPool.submit(() -> {
            while (!finish){
                System.out.println(System.currentTimeMillis() + ", 推动时间轮运转");
                this.advanceClock(500);
            }
        });
    }

    public void addTimerTaskEntry(TimerTaskEntry entry) {
        if (!"1".equals(entry.getTask().getStatus())
                && !entry.cancel() && !timeWheel.add(entry)) {
            System.out.println(entry.getTask().getDesc() + "添加到线程池");
            // 任务已到期，准备执行
            workerThreadPool.submit(entry.getTask());
            entry.getTask().setStatus("1");
        }
    }

    @Override
    public synchronized void add(TimeWheelTask task) {
        // 添加任务开始
        System.out.println("添加任务开始：" + task.getDesc());
        TimerTaskEntry entry = new TimerTaskEntry(task, task.getDelayMs() + System.currentTimeMillis());
        task.setTimerTaskEntry(entry);
        addTimerTaskEntry(entry);
    }

    @Override
    public void advanceClock(long timeout) {
        try {
            TimerTaskList bucket = delayQueue.poll(timeout, TimeUnit.MILLISECONDS);
            if (bucket != null) {
                // 推进时间
                timeWheel.advanceLock(bucket.getExpiration());
                // 执行到期任务
                bucket.flush(this::addTimerTaskEntry);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
        }
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void shutdown() {
        this.finish = true;
        this.workerThreadPool.shutdown();
        this.bossThreadPool.shutdown();

    }
}
