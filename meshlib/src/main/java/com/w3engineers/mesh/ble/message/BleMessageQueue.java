package com.w3engineers.mesh.ble.message;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class BleMessageQueue<T> extends LinkedBlockingDeque<T> {

    synchronized public boolean insertFirst(T t) {
        while (true) {
            try {
                super.putFirst(t);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    synchronized public boolean insertLast(T t) {
        while (true) {
            try {
                putLast(t);
                return true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public T selectFirst() {
        try {
            return takeFirst();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public T selectFirst(long time) {
        try {
            return pollFirst(time, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
