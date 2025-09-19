package com.youtubeapis.stack;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;

import android.util.AttributeSet;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.youtubeapis.R;


public class StackView extends FrameLayout implements GestureDetector.OnGestureListener {
    Scroller scroller;
    StackCallBack adapter;
    GestureDetector gestureDetector;
    int scroll = 0;
    OnItemClickListener onItemClickListener;
    Rect[] childTouchRect;
    private int cardHeight = -1;
    private int maxVisibleCards = 5; // Maximum number of cards visible at once

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public StackView(Context context) {
        super(context);
        initStackView();
    }

    public StackView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initStackView();
    }

    public StackView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initStackView();
    }

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StackView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initStackView();
    }

    private void initStackView() {
        scroller = new Scroller(getContext());
        gestureDetector = new GestureDetector(getContext(), this);
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public StackCallBack getAdapter() {
        return adapter;
    }

    public void setAdapter(StackCallBack adapter) {
        this.adapter = adapter;
        initChildren();
    }

    public void setCardHeight(int height) {
        this.cardHeight = height;
        requestLayout();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (adapter == null)
            return;

        if (getChildCount() != adapter.getCount())
            initChildren();

        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = (cardHeight > 0) ? cardHeight : width;

        childTouchRect = new Rect[getChildCount()];
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).layout(0, 0, width, height);
            childTouchRect[i] = new Rect();
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private void initChildren() {
        if (adapter == null) return;

        removeAllViews();
        for (int i = 0; i < adapter.getCount(); i++) {
            final View card = View.inflate(getContext(), R.layout.stack_card, null);
            TextView title = card.findViewById(R.id.stackTitle);
            title.setText(adapter.getTitle(i));
            ImageView icon = card.findViewById(R.id.stackIcon);
            Drawable drawable = adapter.getIcon(i);
            if (drawable == null) {
                icon.setVisibility(View.GONE);
            } else {
                icon.setImageDrawable(drawable);
            }
            View header = card.findViewById(R.id.stackHeader);
            header.setBackgroundColor(adapter.getHeaderColor(i));
            ViewGroup cardContent = card.findViewById(R.id.stackContent);

            // Add the content view with proper layout params
            View contentView = adapter.getView(i);
            LayoutParams params = new LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
            );
            contentView.setLayoutParams(params);
            cardContent.addView(contentView);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                card.setLayerType(View.LAYER_TYPE_HARDWARE, null);

            addView(card, i, generateDefaultLayoutParams());
            final int finalI = i;
            card.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onItemClickListener != null)
                        onItemClickListener.onItemClick(card, finalI);
                }
            });
        }
        requestLayout();
    }

    private void layoutChildren() {
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int height = getHeight() - getPaddingTop() - getPaddingBottom();
        int cardHeight = (this.cardHeight > 0) ? this.cardHeight : width;

        // Calculate how many cards can be fully visible
        int visibleCardCount = Math.min(maxVisibleCards, getChildCount());

        for (int i = 0; i < getChildCount(); i++) {
            // Calculate position in the stack (0 = top, increasing = lower in stack)
            int stackPosition = Math.min(i, visibleCardCount - 1);

            // Calculate vertical offset - cards lower in the stack are positioned lower
            int yOffset = stackPosition * cardHeight / 4; // Each card is offset by 1/4 of its height

            // Apply scroll to make the stack move
            int y = yOffset - scroll;

            // Calculate scale - cards lower in the stack are smaller
            float scale = 1.0f - stackPosition * 0.05f;
            scale = Math.max(0.8f, scale);

            // Calculate alpha - cards lower in the stack are more transparent
            float alpha = 1.0f - stackPosition * 0.1f;
            alpha = Math.max(0.5f, alpha);

            // Set position, scale and alpha
            ViewHelper.setTranslationX(getChildAt(i), getPaddingLeft());
            ViewHelper.setTranslationY(getChildAt(i), y + getPaddingTop());
            ViewHelper.setScaleX(getChildAt(i), scale);
            ViewHelper.setScaleY(getChildAt(i), scale);
            ViewHelper.setAlpha(getChildAt(i), alpha);

            // Update touch area
            int scaledWidth = (int) (width * scale);
            int scaledHeight = (int) (cardHeight * scale);
            int left = getPaddingLeft() + (width - scaledWidth) / 2;
            int top = y + getPaddingTop();
            childTouchRect[i].set(left, top, left + scaledWidth, top + scaledHeight);

            // Hide cards that are scrolled out of view
            if (y + cardHeight < 0 || y > height) {
                getChildAt(i).setVisibility(View.GONE);
            } else {
                getChildAt(i).setVisibility(View.VISIBLE);
            }
        }
    }

    private int getMaxScroll() {
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int cardHeight = (this.cardHeight > 0) ? this.cardHeight : width;
        return (getChildCount() - 1) * cardHeight;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        layoutChildren();
        super.dispatchDraw(canvas);
        doScrolling();
    }

    @Override
    public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
        if (gestureDetector.onTouchEvent(event)) {
            for (int i = getChildCount() - 1; i >= 0; i--) {
                MotionEvent e = MotionEvent.obtain(event);
                event.setAction(MotionEvent.ACTION_CANCEL);
                e.offsetLocation(-childTouchRect[i].left, -childTouchRect[i].top);
                getChildAt(i).dispatchTouchEvent(e);
            }
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE){
            forceFinished();
        }

        for (int i = getChildCount() - 1; i >= 0; i--){
            if (childTouchRect[i].contains((int) event.getX(), (int) event.getY())) {
                MotionEvent e = MotionEvent.obtain(event);
                e.offsetLocation(-childTouchRect[i].left, -childTouchRect[i].top);
                if (getChildAt(i).dispatchTouchEvent(e))
                    break;
            }
        }

        return true;
    }

    @Override
    public boolean onDown(@NonNull MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent event) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, @NonNull MotionEvent motionEvent2, float v, float v2) {
        scroll = (int) Math.max(0, Math.min(scroll + v2, getMaxScroll()));
        postInvalidate();
        return true;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent motionEvent) {

    }

    void startScrolling(float initialVelocity) {
        scroller.fling(0, scroll, 0, (int) initialVelocity, 0,
                0, Integer.MIN_VALUE, Integer.MAX_VALUE);

        postInvalidate();
    }

    private void doScrolling() {
        if (scroller.isFinished())
            return;

        boolean more = scroller.computeScrollOffset();
        int y = scroller.getCurrY();

        scroll = Math.max(0, Math.min(y, getMaxScroll()));

        if (more)
            postInvalidate();
    }

    boolean isFlinging() {
        return !scroller.isFinished();
    }

    void forceFinished() {
        if (!scroller.isFinished()) {
            scroller.forceFinished(true);
        }
    }

    @Override
    public boolean onFling(MotionEvent motionEvent, @NonNull MotionEvent motionEvent2, float v, float v2) {
        startScrolling(-v2);
        return true;
    }
}