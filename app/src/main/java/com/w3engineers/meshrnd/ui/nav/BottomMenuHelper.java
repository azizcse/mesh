package com.w3engineers.meshrnd.ui.nav;

import android.content.Context;
import androidx.annotation.IdRes;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.w3engineers.meshrnd.R;

public class BottomMenuHelper {
    public static void showBadge(Context context, BottomNavigationView bottomNavigationView, @IdRes int itemId, String value) {
        removeBadge(bottomNavigationView, itemId);
        BottomNavigationItemView itemView = bottomNavigationView.findViewById(itemId);
        View badge = LayoutInflater.from(context).inflate(R.layout.layout_user_badge, bottomNavigationView, false);

        TextView text = badge.findViewById(R.id.badge_text_view);
        text.setText(value);
        itemView.addView(badge);
    }

    public static void removeBadge(BottomNavigationView bottomNavigationView, @IdRes int itemId) {
        BottomNavigationItemView itemView = bottomNavigationView.findViewById(itemId);
        if (itemView.getChildCount() == 3) {
            itemView.removeViewAt(2);
        }
    }
}
