package com.w3engineers.mesh.queue;

import android.util.Log;

import com.w3engineers.mesh.util.MeshLog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by Azizul Islam on 11/17/20.
 */
public class MeshLibMessageEventQueue extends AbstractExecutorService {
    private static final int RUNNING = 0;
    private static final int SHUTDOWN = 1;
    private static final int TERMINATED = 2;

    final Lock lock = new ReentrantLock();
    final Condition termination = lock.newCondition();

    final Executor underlyingExecutor;
    final ArrayDeque<Runnable> discoveryCommandsQueue;
    final ArrayDeque<Runnable> appMessageCommandQueue;

    volatile int state = RUNNING;
    Runnable currentCommand;

    /*
     * The runnable we submit into the underlyingExecutor, we avoid creating
     * unnecessary runnables since only one will be submitted at a time
     */
    private final Runnable innerRunnable = new Runnable() {

        public void run() {
            /*
             * If state is TERMINATED, skip execution
             */
            if (state == TERMINATED) {
                return;
            }

            //MeshLog.e("Discovery_queue Task started type: " + ((DiscoveryTask) currentCommand).title);

            try {
                currentCommand.run();
            } catch (Exception e) {
                e.printStackTrace();
                MeshLog.e("DiscoveryEventQueue error " + e.getMessage());
            } finally {
                /*if (currentCommand instanceof DiscoveryTask) {
                    MeshLog.e("Discovery_queue", "Task finish generate time : "
                            + ((DiscoveryTask) currentCommand).messageInternalId + " Type: "
                            + ((DiscoveryTask) currentCommand).title);
                }*/

                lock.lock();
                try {
                    currentCommand = discoveryCommandsQueue.pollFirst();
                    if (currentCommand == null) {
                        currentCommand = appMessageCommandQueue.pollFirst();
                        Log.e("Peek_message", "Peek app message");
                    } else {
                        Log.e("Peek_message", "Peek discovery message");
                    }

                    if (currentCommand != null && state < TERMINATED) {
                        try {
                            underlyingExecutor.execute(this);
                        } catch (Exception e) {

                            currentCommand = null;
                            discoveryCommandsQueue.clear();
                            appMessageCommandQueue.clear();
                            transitionToTerminated();
                        }
                    } else {
                        if (state == SHUTDOWN) {
                            transitionToTerminated();
                        }
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    };


    public MeshLibMessageEventQueue(Executor underlyingExecutor) {
        this.underlyingExecutor = underlyingExecutor;
        this.discoveryCommandsQueue = new ArrayDeque<>();
        this.appMessageCommandQueue = new ArrayDeque<>();
    }


    public void addAppMessageInQueue(Runnable runnable) {
        lock.lock();
        try {
            if (state != RUNNING) {
                //throw new IllegalStateException("Executor has been shutdown");
                MeshLog.v("Message Queue is not running :execute(");
                return;
            }
            if (currentCommand == null && discoveryCommandsQueue.isEmpty() && appMessageCommandQueue.isEmpty()) {
                currentCommand = runnable;
                underlyingExecutor.execute(innerRunnable);
            } else {
                appMessageCommandQueue.add(runnable);
            }
        } finally {
            lock.unlock();
        }
    }

    public void execute(Runnable command) {
        lock.lock();
        try {
            if (state != RUNNING) {
                //throw new IllegalStateException("Executor has been shutdown");
                MeshLog.v("Message Queue is not running :execute(");
                return;
            }
            if (currentCommand == null && discoveryCommandsQueue.isEmpty() && appMessageCommandQueue.isEmpty()) {
                currentCommand = command;
                underlyingExecutor.execute(innerRunnable);
            } else {
                discoveryCommandsQueue.add(command);
            }
        } finally {
            lock.unlock();
        }
    }

    public void addDiscoveryTaskInLast(DiscoveryTask task) {
        lock.lock();
        try {
            if (state != RUNNING) {
                //throw new IllegalStateException("Executor has been shutdown");
                MeshLog.v("Message Queue is not running : addTaskInLast(");
                return;
            }
            //MeshLog.e("Discovery_queue", "Task added last title : " + task.title);
            if (currentCommand == null && discoveryCommandsQueue.isEmpty() && appMessageCommandQueue.isEmpty()) {
                currentCommand = task;
                underlyingExecutor.execute(innerRunnable);
            } else {
                discoveryCommandsQueue.addLast(task);
            }
        } finally {
            lock.unlock();
        }
    }


    public void addDiscoveryTaskInFirst(DiscoveryTask task) {
        lock.lock();
        try {
            if (state != RUNNING) {
                //throw new IllegalStateException("Executor has been shutdown");
                MeshLog.v("Message Queue is not running : addTaskInFirst(");
                return;
            }
            MeshLog.e("Discovery_queue", "Task added first title : " + task.title);
            if (currentCommand == null && discoveryCommandsQueue.isEmpty() && appMessageCommandQueue.isEmpty()) {
                currentCommand = task;
                underlyingExecutor.execute(innerRunnable);
            } else {
                discoveryCommandsQueue.addFirst(task);
            }
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        lock.lock();
        try {
            if (state == RUNNING) {
                if (currentCommand == null && discoveryCommandsQueue.isEmpty() && appMessageCommandQueue.isEmpty()) {
                    transitionToTerminated();
                } else {
                    state = SHUTDOWN;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public List<Runnable> shutdownNow() {
        lock.lock();
        try {
            if (state < TERMINATED) {
                transitionToTerminated();
                ArrayList<Runnable> result = new ArrayList<Runnable>(discoveryCommandsQueue);
                discoveryCommandsQueue.clear();
                return result;
            }
            return Collections.<Runnable>emptyList();
        } finally {
            lock.unlock();
        }
    }

    public boolean isShutdown() {
        return state > RUNNING;
    }

    public boolean isTerminated() {
        return state == TERMINATED;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        lock.lock();
        try {
            while (!isTerminated() && nanos > 0) {
                nanos = termination.awaitNanos(nanos);
            }
        } finally {
            lock.unlock();
        }
        return isTerminated();
    }

    /*
     * Lock must me held when calling this method
     */
    private void transitionToTerminated() {
        state = TERMINATED;
        termination.signalAll();
    }
}
