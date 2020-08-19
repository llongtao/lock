package com.llt.lock;

import java.util.concurrent.CyclicBarrier;

import static java.lang.Thread.sleep;

/**
 * @author llt
 * @date 2020-08-20 0:04
 */
public class T {

    static int num =  100;
    public static void main(String[] args) throws InterruptedException {
        System.out.println(1);

        CyclicBarrier cyclicBarrier = new CyclicBarrier(100);

        for (int i = 0; i <100 ; i++) {
            int finalI = i;
            new Thread(()->{
                try {
                    cyclicBarrier.await();
                    num = num-1;
                    System.out.println(num);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
        sleep(1000);
        System.out.println(num);
    }
}
