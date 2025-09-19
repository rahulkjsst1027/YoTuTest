package com.youtubeapis.stack;

import android.graphics.drawable.Drawable;
import android.view.View;

public interface StackCallBack {
    String getTitle(int position);
    View getView(int position);
    Drawable getIcon(int position);
    int getHeaderColor(int position);

    int getCount();
}