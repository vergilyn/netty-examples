package com.vergilyn.examples;

import java.util.concurrent.Semaphore;

import io.netty.util.concurrent.FastThreadLocal;
import io.netty.util.concurrent.FastThreadLocalThread;
import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;

/**
 * <a href="https://mp.weixin.qq.com/s/xJSmSexl8mzdX8ivSRAb6Q">匠心零度 - FastThreadLocal吞吐量居然是ThreadLocal的3倍！！！</a>
 * @author vergilyn
 * @date 2020-01-24
 */
@Slf4j
public class FastThreadLocalTestng {



    /**
     * `fast-thread-local[01]`输出 0~99 <br/>
     * `fast-thread-local[02]`输出 null <br/>
     * {@linkplain FastThreadLocal} 不需要像 {@linkplain ThreadLocal} 那样try-finally，原因查看 {@linkplain io.netty.util.concurrent.FastThreadLocalRunnable#run()} 的构造函数。
     * @see #threadLocal()
     */
    @Test
    public void fastThreadLocal() throws InterruptedException {
        FastThreadLocal<Integer> fastThreadLocal = new FastThreadLocal<>();

        //if (thread instanceof FastThreadLocalThread) 使用FastThreadLocalThread更优，普通线程也可以
        new FastThreadLocalThread(() -> {
            for (int i = 0; i < 100; i++) {
                fastThreadLocal.set(i);
                System.out.println(Thread.currentThread().getName() + "====" + fastThreadLocal.get());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "fast-thread-local[01]").start();


        new FastThreadLocalThread(() -> {
            for (int i = 0; i < 100; i++) {
                System.out.println(Thread.currentThread().getName() + "====" + fastThreadLocal.get());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }, "fast-thread-local[02]").start();

        preventMethodExit();
    }

    /**
     * {@linkplain ThreadLocal} 需要try-finally
     *
     * @see #fastThreadLocal()
     */
    @Test
    public void threadLocal(){
        ThreadLocal<Integer> threadLocal = new ThreadLocal<>();

        new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    threadLocal.set(i);
                    System.out.println(Thread.currentThread().getName() + "====" + threadLocal.get());
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } finally {
                threadLocal.remove();
            }
        }, "thread-local[01]").start();

        new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    System.out.println(Thread.currentThread().getName() + "====" + threadLocal.get());
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } finally {
                threadLocal.remove();
            }
        }, "thread-local[02]").start();

        preventMethodExit();
    }

    /**
     * 阻止测试方法退出
     */
    private void preventMethodExit(){
        try {
            new Semaphore(0).acquire();
        } catch (InterruptedException e) {
            // do nothing
        }

    }
}
