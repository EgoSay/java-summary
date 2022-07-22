package timewheel;

/**
 * 定时器接口
 * @author chenjw
 * @version 1.0
 * @date 2022/7/21 15:18
 */
public interface Timer {

    /**
     * 添加一个新任务
     *
     * @param task
     */
    void add(TimeWheelTask task);


    /**
     * 推动指针
     *
     * @param timeout
     */
    void advanceClock(long timeout);

    /**
     * 等待执行的任务
     *
     * @return
     */
    int size();

    /**
     * 关闭服务,剩下的无法被执行
     */
    void shutdown();
}
