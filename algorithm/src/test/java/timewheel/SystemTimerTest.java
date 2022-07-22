package timewheel;


public class SystemTimerTest {

    //驱动时间轮向前的线程
    public static  SystemTimer timer = new SystemTimer(1000, 5);


    public static void main(String[] args) {
        for(int i = 0;i < 3; i += 1) {
            // 添加任务，每个任务间隔1s
            timer.add(new TimeWheelTask(i * 10000, "testTask-" + i));
        }
    }
}