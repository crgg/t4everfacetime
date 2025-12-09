package com.t4app.videocalltest;

import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

public abstract class SafeClickListener implements View.OnClickListener, PopupMenu.OnMenuItemClickListener {
    private static final long INTERVAL_CLICK = 1000;

    private long lastClickTime = 0;

    private boolean isSafeClick(){
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime > INTERVAL_CLICK){
            lastClickTime = currentTime;
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View view) {
        if (isSafeClick()){
            onSafeClick(view);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (isSafeClick()){
            onSafeMenuItemClick(menuItem);
        }
        return true;
    }

    public void onSafeClick(View v) {}
    public void onSafeMenuItemClick(MenuItem item) {}
}
