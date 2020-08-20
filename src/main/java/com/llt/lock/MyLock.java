package com.llt.lock;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author LILONGTAO
 * @date 2020-08-19
 */
public class MyLock implements Lock {

    /**
     * volatile 让被他修饰内容具有可见性
     *
     * 可见性，是指线程之间的可见性，一个线程修改的状态对另一个线程是可见的。
     * 也就是一个线程修改的结果。另一个线程马上就能看到。
     * volatile修饰的变量不允许线程内部缓存和重排序，即直接修改内存。
     * 所以对其他线程是可见的。
     * 但是这里需要注意一个问题，volatile只能让被他修饰内容具有可见性，但不能保证它具有原子性。
     * 比如 volatile int a = 0；之后有一个操作 a++；这个变量a具有可见性，但是a++ 依然是一个非原子操作，也就是这个操作同样存在线程安全问题。
     *
     */
    private volatile int state;

    /**
     * 提供CAS操作
     *
     */
    private static final Unsafe UNSAFE;

    private static final long STATE_OFFSET;
    private static final long LAST_OFFSET;

    private volatile Node last = new Node(null, null);
    private volatile Node first = last;


    private Thread currentThread;

    static {
        try {
            Class<?> aClass = Unsafe.class;
            Field theUnsafe = aClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(aClass);
        } catch (Exception e) {
            throw new Error(e);
        }
        try {
            STATE_OFFSET = UNSAFE.objectFieldOffset(MyLock.class.getDeclaredField("state"));
            LAST_OFFSET = UNSAFE.objectFieldOffset(MyLock.class.getDeclaredField("last"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }


    /**
     * 加锁
     */
    @Override
    public void lock() {
        if (!tryLock()) {
            addWaiter();
            if (!tryLock()) {
                //false 相对时间 0L 无限时间阻塞
                UNSAFE.park(false, 0L);
                lock();
            }else {
                System.out.println(Thread.currentThread().getId()+"获取锁");
            }
        }else {
            System.out.println(Thread.currentThread().getId()+"获取锁");
        }
    }

    private Node addWaiter() {
        Node node = new Node(Thread.currentThread(), null);

        Node pred = last;

        while (!UNSAFE.compareAndSwapObject(this, LAST_OFFSET, pred, node)) {
            pred = last;
        }
        pred.next = node;

        return node;
    }

    /**
     * 尝试获取锁
     *
     * @return 是否成功获取
     */
    @Override
    public boolean tryLock() {
        Thread current = Thread.currentThread();
        if (state == 0) {
            if (!hasQueuedPredecessors(current) && UNSAFE.compareAndSwapInt(this, STATE_OFFSET, 0, 1)) {
                currentThread = current;
                return true;
            }
        } else if (current == currentThread) {
            state = state + 1;
            return true;
        }
        return false;
    }

    private boolean hasQueuedPredecessors(Thread current) {
        Node fst = first;
        Node next = fst.next;
        //头结点为当前线程,表示排队到自己了,有夺锁权
        //next为空表示没人排队,有夺锁权
        //next为当前线程表示第一次未抢到锁,自身在等待队列第一个,可能之前抢锁成功的已经释放锁了,有夺锁权
        return !(fst.thread == current || next == null ||   next.thread == current);
    }

    /**
     * 解锁
     */
    @Override
    public void unlock() {
        Thread current = Thread.currentThread();
        if (current == currentThread) {
            state = state - 1;
            if (state == 0) {
                currentThread = null;
                System.out.println(current.getId()+"解锁");

                while (true){
                    Node fst = first;
                    Node next = fst.next;
                    if (next != null) {
                        first = next;
                        Thread thread = next.thread;
                        if (thread == current) {
                            continue;
                        }
                        if (thread != null) {
                            System.out.println("唤醒"+thread.getId());
                            UNSAFE.unpark(thread);
                        }
                    }
                    return;
                }
            }
        }
    }


    /**
     * 尝试获取锁
     *
     * @param time 等待时间
     * @param unit 时间单位
     * @return 是否成功获取
     * @throws InterruptedException 等待过程被打断,抛出此异常
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    /**
     * 获取锁,等待过程可被打断
     * <p>
     * 线程在sleep或wait,join， 此时如果别的进程调用此进程的 interrupt()方法，此线程会被唤醒并被要求处理InterruptedException；
     * <p>
     * 如果此线程在运行中， 则不会收到提醒。但是 此线程的 “打扰标志”会被设置，可以通过isInterrupted()查看并作出处理。
     * lockInterruptibly()和上面的第一种情况是一样的， 线程在请求lock并被阻塞时，如果被interrupt，则“此线程会被唤醒并被要求处理InterruptedException”
     * <p>
     * 1）如果当前线程未被中断，则获取锁。
     * 2）如果该锁没有被另一个线程保持，则获取该锁并立即返回，将锁的保持计数设置为 1。
     * 3）如果当前线程已经保持此锁，则将保持计数加 1，并且该方法立即返回。
     * 4）如果锁被另一个线程保持，则出于线程调度目的，禁用当前线程，并且在发生以下两种情况之一以前，该线程将一直处于休眠状态：
     * (1)锁由当前线程获得；
     * (2)其他某个线程中断当前线程。
     * 5）如果当前线程获得该锁，则将锁保持计数设置为 1.如果当前线程：
     * (1)在进入此方法时已经设置了该线程的中断状态
     * (2)在等待获取锁的同时被中断。
     * 则抛出 InterruptedException，并且清除当前线程的已中断状态。
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {

    }


    /**
     * 生成一个供外部操作该锁实例的对象
     * <p>
     * <p>
     * 为这个{@code Lock}实例返回一个新的{@link Condition}实例
     * 如果此{@code Lock}不支持条件 则@throws UnsupportedOperationException
     * <p>
     * 调用{@link Condition#await（）}将自动释放锁
     */
    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }


    private static class Node {
        Thread thread;
        volatile Node next;

        public Node(Thread thread, Node next) {
            this.thread = thread;
            this.next = next;
        }

        public Node() {
        }
    }
}
