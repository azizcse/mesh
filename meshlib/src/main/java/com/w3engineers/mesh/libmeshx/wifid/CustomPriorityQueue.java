package com.w3engineers.mesh.libmeshx.wifid;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

/**
 * <p>Enqueue items with currently added item to old added item</p>
 * Compare each existed item to protect duplicate
 *
 * @param <T> Generic class type
 */

public class CustomPriorityQueue<T> extends PriorityQueue<T> {

    public CustomPriorityQueue(int initialCapacity, Comparator<? super T> comparator) {
        super(initialCapacity, comparator);
    }

    @Override
    public boolean contains(Object item) {
        Iterator<T> it = iterator();
        if (item == null) return true;
        while (it.hasNext()) {
            T next = it.next();
            if (next instanceof APCredential && item instanceof APCredential) {
                if (((APCredential) next).mSSID.equals(((APCredential) item).mSSID)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean add(T item) {
        boolean isContain = contains(item);
        if (isContain) return false;
        return super.add(item);
    }

}
