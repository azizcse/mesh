/*
 * Copyright (c) 2016 Vladimir L. Shabanov <virlof@gmail.com>
 *
 * Licensed under the Underdark License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://underdark.io/LICENSE.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.w3engineers.mesh.queue;

import com.w3engineers.mesh.queue.messages.BTDiscoveryMessage;
import com.w3engineers.mesh.queue.messages.BTMessage;
import com.w3engineers.mesh.queue.messages.BaseMeshMessage;
import com.w3engineers.mesh.queue.messages.InternetMessage;
import com.w3engineers.mesh.queue.messages.WiFiDiscoverMessage;
import com.w3engineers.mesh.queue.messages.WiFiMessage;
import com.w3engineers.mesh.util.MeshLog;

import java.util.ArrayDeque;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link MessageDispatcher} implementation which executes one task at a time
 * using an underlying executor as the actual processor of the task.
 * <p>
 * <p>
 * This implementation differs from an {@link Executor} created by
 * {@link Executors#newSingleThreadExecutor()} in that there is not actual
 * thread handling done by {@link MessageDispatcher} but instead the processing of
 * the tasks is delegated to an underlying executor that can be shared among
 * many {@link Executor}. <p>
 * <p>
 * Tasks submitted on a {@link MessageDispatcher} will be processed sequentially
 * and in the exact same order in which they were submitted, regardless of the
 * number of threads available in the underlying executor.
 *
 * @author muralx (Diego Belfer)
 * https://github.com/muralx/java-fun
 */
public class MessageDispatcher {

    final Lock lock = new ReentrantLock();
    final Condition termination = lock.newCondition();


    final ArrayDeque<BaseMeshMessage> mBaseMeshMessageArrayDeque;

    private BaseMeshMessage mBaseMeshMessage;
    private ExecutorService wifiExecutorService, btExecutorService, internetExecutorService;
    private ExecutorService mDiscoveryExecutorService;
    private ExecutorService wifiReceiverExecutor, btReceiverExecutor, internetReceiverExecutor;


    // TODO: 10/15/2019 we can manage a list to update the client as like of Broadcast Manager 
    private MeshMessageListener messageListener;
    private Executor underlyingExecutor;

    public MessageDispatcher(MeshMessageListener meshMessageListener, Executor messageExecutor) {
        //Maintaining separate send queue for WiFi discovery and message so that they do not
        // interfere each other
        mDiscoveryExecutorService = Executors.newSingleThreadExecutor();
        wifiExecutorService = Executors.newSingleThreadExecutor();
        btExecutorService = Executors.newSingleThreadExecutor();
        internetExecutorService = Executors.newSingleThreadExecutor();

        //For WiFi we expect parallel incoming event processing
        wifiReceiverExecutor = Executors.newSingleThreadExecutor();
        btReceiverExecutor = Executors.newSingleThreadExecutor();
        internetReceiverExecutor = Executors.newSingleThreadExecutor();

        mBaseMeshMessageArrayDeque = new ArrayDeque<>();
        this.messageListener = meshMessageListener;
        this.underlyingExecutor = messageExecutor;
    }

    /*
     * The runnable we submit into the underlyingExecutor, we avoid creating
     * unnecessary runnables since only one will be submitted at a time
     */
    private final Runnable innerRunnable = new Runnable() {

        public void run() {

            try {

                if (mBaseMeshMessage instanceof WiFiDiscoverMessage ||
                        mBaseMeshMessage instanceof BTDiscoveryMessage) {

                    if (mBaseMeshMessage instanceof BTDiscoveryMessage) {
                        DispatcherHelper dispatcherHelper = DispatcherHelper.getDispatcherHelper();
                        synchronized (dispatcherHelper.lock) {
                            dispatcherHelper.mCountBTDiscovering++;
                        }
                    }
                    MeshLog.e("[BLE_PROCESS] Attempt to send Wifi discovery message ");
                    sendAndRetry(wifiExecutorService, mBaseMeshMessage);

                } else if (mBaseMeshMessage instanceof WiFiMessage) {
                    MeshLog.e("[BLE_PROCESS] Attempt to send Wifi message ");

                    sendAndRetry(wifiExecutorService, mBaseMeshMessage);

                } else if (mBaseMeshMessage instanceof BTMessage) {

                    sendAndRetry(btExecutorService, mBaseMeshMessage);

                } else if (mBaseMeshMessage instanceof InternetMessage) {

                    sendAndRetry(internetExecutorService, mBaseMeshMessage);

                }

            } catch (RejectedExecutionException executionException) {
                MeshLog.e("*** MESSAGE SEND REJECTED ***");
            } finally {
                //MeshLog.i("MESSAGE QUEUE UNDER LOCK");
                lock.lock();
                try {
                    mBaseMeshMessage = mBaseMeshMessageArrayDeque.pollFirst();
                    if (mBaseMeshMessage != null) {
                        try {
                            underlyingExecutor.execute(this);
                        } catch (Exception e) {
                            mBaseMeshMessage = null;
                            mBaseMeshMessageArrayDeque.clear();

                        }
                    } else {
                        termination.signalAll();
                    }
                } finally {
                    lock.unlock();
                }
            }
        }
    };

    private void sendAndRetry(ExecutorService executorService, BaseMeshMessage baseMeshMessage) {
        executorService.execute(() -> {
            boolean isSuccess;
            do {

                isSuccess = baseMeshMessage.send() == BaseMeshMessage.MESSAGE_STATUS_SUCCESS;
                MeshLog.i("MESSAGE RETRY COUNT =" + baseMeshMessage.mRetryCount);


            } while (baseMeshMessage.mRetryCount++ < baseMeshMessage.mMaxRetryCount && !isSuccess);

            if (baseMeshMessage instanceof WiFiDiscoverMessage && !isSuccess) {
                messageListener.onWifiHelloMessageSend(((WiFiDiscoverMessage) baseMeshMessage).mIp, isSuccess);
            }
            messageListener.onMessageSend(baseMeshMessage.mInternalId, "",isSuccess);
            /*if (baseMeshMessage instanceof WiFiMessage && baseMeshMessage.messageId != null) {
                //messageListener.onWifiDirectMessageSend(baseMeshMessage.messageId, isSuccess);
            } else {
                messageListener.onMessageSend(baseMeshMessage.mInternalId, isSuccess);
            }*/
        });

    }

    public void shutdown() {
        lock.lock();
        try {
            //Close sender executor
            wifiExecutorService.shutdown();
            btExecutorService.shutdown();
            internetExecutorService.shutdown();

            //Close receiver executor
            wifiReceiverExecutor.shutdown();
            btReceiverExecutor.shutdown();
            internetReceiverExecutor.shutdown();

            //discover executor
            if (mDiscoveryExecutorService != null) {
                mDiscoveryExecutorService.shutdown();
            }

        } finally {
            lock.unlock();
        }
    }

    public int addSendMessage(BaseMeshMessage baseMeshMessage) {
        lock.lock();
        try {
            boolean isDicoveryMessage = baseMeshMessage instanceof WiFiDiscoverMessage;

            MeshLog.e("[BLE_PROCESS] Message queue put :"+isDicoveryMessage+" Msg: "+baseMeshMessage);

            int uuid = Math.abs(UUID.randomUUID().toString().hashCode());
            baseMeshMessage.mInternalId = uuid;
            if (mBaseMeshMessage == null && mBaseMeshMessageArrayDeque.isEmpty()) {
                mBaseMeshMessage = baseMeshMessage;
                underlyingExecutor.execute(innerRunnable);
            } else {
                mBaseMeshMessageArrayDeque.add(baseMeshMessage);
            }
            return uuid;
        } finally {
            lock.unlock();
        }
    }


    public void addReceiveMessage(BaseMeshMessage baseMeshMessage) {
        try {

            if (baseMeshMessage instanceof WiFiDiscoverMessage ||
                    baseMeshMessage instanceof BTDiscoveryMessage) {

                if (mBaseMeshMessage instanceof BTDiscoveryMessage) {
                    DispatcherHelper dispatcherHelper = DispatcherHelper.getDispatcherHelper();
                    synchronized (dispatcherHelper.lock) {
                        dispatcherHelper.mCountBTDiscovering++;
                    }
                }

                mDiscoveryExecutorService.execute(baseMeshMessage::receive);

            } else if (baseMeshMessage instanceof WiFiMessage) {

                mDiscoveryExecutorService.execute(baseMeshMessage::receive);

            } else if (baseMeshMessage instanceof BTMessage) {
                btReceiverExecutor.execute(baseMeshMessage::receive);

            } else if (baseMeshMessage instanceof InternetMessage) {

                internetReceiverExecutor.execute(baseMeshMessage::receive);

            }

        } catch (RejectedExecutionException executionException) {
            MeshLog.e("*** MESSAGE RECEIVE REJECTED ***");
        }
    }
}
