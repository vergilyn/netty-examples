package com.vergilyn.examples.netty.feature.util;

import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

/**
 * for example:
 *  1. delay consumer queue
 *  2. schedule
 *  3. heartbeat, ex. dubbo -> `HeaderExchangeClient`/`HeaderExchangeServer`
 *
 * @author vergilyn
 * @date 2020-05-25
 */
public class WheelTimerMainTest {

    public static void main(String[] args) {

        final Timer timer = new HashedWheelTimer(Executors.defaultThreadFactory(), 5, TimeUnit.SECONDS, 2);

        // task-01
        timer.newTimeout(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                System.out.printf("task-01 exec-time >>>> %s \r\n", LocalTime.now());

                timer.newTimeout(this, 10, TimeUnit.SECONDS); // 结束时候再次注册
            }
        }, 5, TimeUnit.SECONDS);

        // task-02
        timer.newTimeout(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                System.out.printf("task-02 exec-time >>>> %s \r\n", LocalTime.now());

            }
        }, 10, TimeUnit.SECONDS);

        // task-03
        timer.newTimeout(new TimerTask() {
            public void run(Timeout timeout) throws Exception {
                System.out.printf("task-03 exec-time >>>> %s \r\n", LocalTime.now());
            }
        }, 15, TimeUnit.SECONDS);

    }
}
