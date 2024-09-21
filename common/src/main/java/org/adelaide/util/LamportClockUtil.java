package org.adelaide.util;

import java.util.concurrent.atomic.AtomicInteger;

public class LamportClockUtil {
    private final AtomicInteger time;

    public LamportClockUtil() {
        this.time = new AtomicInteger(0);
    }

    // 递增本地时间，用于处理本地事件
    public int increment() {
        return time.incrementAndGet();
    }

    // 发送消息时，先递增时间
    public int sendEvent() {
        return increment();
    }

    // 接收到消息时，更新时间
    public void receiveEvent(int receivedTime) {
        // 更新本地时间为 max(本地时间, 接收到的时间) + 1
        time.updateAndGet(localTime -> Math.max(localTime, receivedTime) + 1);
    }

    // 获取当前时间
    public int getTime() {
        return time.get();
    }
}
