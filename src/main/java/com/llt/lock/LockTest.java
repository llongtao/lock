package com.llt.lock;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.Thread.sleep;

/**
 * @author LILONGTAO
 * @date 2020-08-19
 */
@SuppressWarnings("ALL")
public class LockTest {

    int num = 10000;


    @Test
    public void tstNoLock() throws InterruptedException {
        System.out.println("tstNoLock");

        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    cyclicBarrier.await();
                    for (int j = 0; j < 1000; j++) {
                        num--;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
        sleep(1000);
        System.out.println(num);
    }

    @Test
    public void tstSynchronized() throws InterruptedException {
        System.out.println("tstSynchronized");

        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    cyclicBarrier.await();
                    synchronized (LockTest.class){
                        for (int j = 0; j < 1000; j++) {
                            num--;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
        sleep(1000);
        System.out.println(num);
        assert num==0;
    }

    @Test
    public void tstReentrantLock() throws InterruptedException {
        System.out.println("tstReentrantLock");

        ReentrantLock lock = new ReentrantLock(true);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);

        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    cyclicBarrier.await();
                    lock.lock();
                    for (int j = 0; j < 1000; j++) {
                        num--;
                    }
                    lock.unlock();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
        sleep(1000);
        System.out.println(num);
        assert num==0;
    }

    @Test
    public void tstMyLock() throws InterruptedException {
        System.out.println("tstMyLock");

        Lock lock = new MyLock();

        CyclicBarrier cyclicBarrier = new CyclicBarrier(10);
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            new Thread(() -> {
                try {
                    cyclicBarrier.await();
                    lock.lock();
                    for (int j = 0; j < 1000; j++) {
                        num--;
                    }
                    lock.unlock();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
        //lock.unlock();
        sleep(1000);
        System.out.println(num);
        assert num==0;
    }


}
