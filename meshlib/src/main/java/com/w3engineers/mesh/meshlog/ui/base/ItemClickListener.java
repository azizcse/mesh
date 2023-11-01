package com.w3engineers.mesh.meshlog.ui.base;

import android.view.View;

/**
 * Item Click Listener
 * @param <T>
 */

public interface ItemClickListener<T> {
    /**
     * Called when a item has been clicked.
     *
     * @param view The view that was clicked.
     * @param item The T type object that was clicked.
     */
    void onItemClick(View view, T item);
}
