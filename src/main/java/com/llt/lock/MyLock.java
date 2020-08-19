package com.llt.lock;

import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * @author LILONGTAO
 * @date 2020-08-19
 */
public class MyLock implements Lock {

    /**
     * volatile
     * 编译器在用到这个变量时必须每次都小心地重新读取这个变量的值，
     * 而不是使用保存在寄存器里的备份。
     * 在本次线程内，当读取一个变量时，为提高存取速度，编译器优化时有时会先把变量读取到一个寄存器中；以后再取变量值时，就直接从寄存器中取值；
     * 当变量值在本线程里改变时，会同时把变量的新值copy到该寄存器中，以便保持一致
     * 当变量在因别的线程等而改变了值，该寄存器的值不会相应改变，从而造成应用程序读取的值和实际的变量值不一致
     * 当该寄存器在因别的线程等而改变了值，原变量的值不会改变，从而造成应用程序读取的值和实际的变量值不一致
     */
    private volatile int state;

    /**
     * 不安全的类,提供了类似C++手动管理内存的能力
     * 可以操作jvm管理之外的内存
     * 可以提供CAS操作（比较并交换）是CPU指令级的操作
     */
    private static final Unsafe UNSAFE;

    private static final long STATE_OFFSET;
    private static final long FIRST_OFFSET;
    private static final long LAST_OFFSET;
    private static final long NEXT_OFFSET;


    private volatile Node first ;

    private volatile Node last;

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
            FIRST_OFFSET = UNSAFE.objectFieldOffset(MyLock.class.getDeclaredField("first"));
            LAST_OFFSET = UNSAFE.objectFieldOffset(MyLock.class.getDeclaredField("last"));
            NEXT_OFFSET = UNSAFE.objectFieldOffset(Node.class.getDeclaredField("next"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }


    /**
     * 加锁
     */
    @Override
    public void lock() {
        if (!tryLock() ) {
            put(new Node(Thread.currentThread(), null));
            //false 相对时间 0L 无限时间阻塞
            System.out.println("pack");
            acquireQueued(addWaiter());
        }
    }

    private void put(Node node) {
        if (first == null){
            UNSAFE.compareAndSwapObject(this,FIRST_OFFSET,null,node);
        }
        if (last != null) {
            UNSAFE.compareAndSwapObject(last,NEXT_OFFSET,last.next,node);
        }
        UNSAFE.compareAndSwapObject(this,LAST_OFFSET,last,node);
    }
    final boolean acquireQueued(final Node node) {
        boolean failed = true;
        try {
            boolean interrupted = false;
            for (;;) {
                final Node p = first;
                if ( tryLock()) {
                    setHead(node);
                    p.next = null; // help GC
                    failed = false;
                    return interrupted;
                }
                UNSAFE.park(false, 0L);
            }
        } finally {
//            if (failed)
//                cancelAcquire(node);
        }
    }

    private Node addWaiter() {
        Node node = new Node(Thread.currentThread(), null);
        // Try the fast path of enq; backup to full enq on failure
        Node pred = last;
        if (pred != null) {
            if (UNSAFE.compareAndSwapObject(this,LAST_OFFSET,pred,node)) {
                pred.next = node;
                return node;
            }
        }
        enq(node);
        return node;
    }
    private Node enq(final Node node) {
        for (;;) {
            Node t = last;
            if (t == null) {
                if (UNSAFE.compareAndSwapObject(this,FIRST_OFFSET,first,new Node())){
                    last = first;
                }
            } else {
                if (UNSAFE.compareAndSwapObject(this,LAST_OFFSET,last,node)) {
                    t.next = node;
                    return t;
                }
            }
        }
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
            if (hasQueuedPredecessors() && UNSAFE.compareAndSwapInt(this, STATE_OFFSET, 0, 1)) {
                currentThread = current;
                return true;
            }
        } else if (current == currentThread) {
            state = state + 1;
            return true;
        }
        return false;
    }

    public final boolean hasQueuedPredecessors() {
        return first == null || first.thread != Thread.currentThread();
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
                if (first == null) {
                    return;
                }
                Thread thread = first.thread;
                first= first.next;
                UNSAFE.unpark(thread);
                System.out.println("解锁成功");
            }
        }else {
            System.out.println("不是当前线程");
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

    public void setHead(Node node) {
        first = node;
        node.thread = null;
    }

    private static class Node{
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
