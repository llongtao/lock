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

    static int num =  10000;



    @Test
    public  void tstNoLock() throws InterruptedException {
        System.out.println(1);

        CyclicBarrier cyclicBarrier = new CyclicBarrier(100);

        for (int i = 0; i <100 ; i++) {
            int finalI = i;
            new Thread(()->{
                try {
                    cyclicBarrier.await();
                    for (int j = 0; j <100 ; j++) {
                        num --;
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
    public  void tstReentrantLock() throws InterruptedException {
        System.out.println(1);

        ReentrantLock lock = new ReentrantLock(true);
        CyclicBarrier cyclicBarrier = new CyclicBarrier(100);

        for (int i = 0; i <100 ; i++) {
            int finalI = i;
            new Thread(()->{
                try {
                    cyclicBarrier.await();
                    lock.lock();
                    //System.out.println(finalI +"加锁成功");
                    for (int j = 0; j <100 ; j++) {
                        num --;
                    }
                    lock.unlock();
                    //System.out.println(finalI +"释放成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
        sleep(1000);
        System.out.println(num);
    }

    @Test
    public  void tstMyLock() throws InterruptedException {
        System.out.println(2);

        Lock lock = new MyLock();

        CyclicBarrier cyclicBarrier = new CyclicBarrier(100);

        for (int i = 0; i <100 ; i++) {
            int finalI = i;
            new Thread(()->{
                try {
                    cyclicBarrier.await();
                    lock.lock();
                    //System.out.println(finalI +"加锁成功");
                    for (int j = 0; j <100 ; j++) {
                        num --;
                    }
                    lock.unlock();
                    //System.out.println(finalI +"释放成功");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
        sleep(2000);
        System.out.println(num);
    }


}
