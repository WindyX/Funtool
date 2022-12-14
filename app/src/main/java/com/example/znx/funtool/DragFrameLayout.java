package com.example.znx.funtool;

import android.content.Context;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

public class DragFrameLayout extends FrameLayout {


    private List<View> viewList;
    private ViewDragHelper dragHelper;

    public DragFrameLayout(Context context) {
        this(context, null);
    }

    public DragFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //第二步：创建存放View的集合
        viewList = new ArrayList<>();


        dragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {

            /**
             * 是否捕获childView:
             * 如果viewList包含child，那么捕获childView
             * 如果不包含child，就不捕获childView
             */
            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return viewList.contains(child);
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
            }

            /**
             * 当捕获到child后的处理：
             * 获取child的监听
             */
            @Override
            public void onViewCaptured(View capturedChild, int activePointerId) {
                super.onViewCaptured(capturedChild, activePointerId);
                if (onDragDropListener != null) {
                    onDragDropListener.onDragDrop(true);
                }
            }

            /**
             * 当释放child后的处理：
             * 取消监听，不再处理
             */
            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                super.onViewReleased(releasedChild, xvel, yvel);
                if (onDragDropListener != null) {
                    onDragDropListener.onDragDrop(false);
                }
            }

            /**
             * 当前view的left
             */
            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
//                LogUtil.d("left=" + left + ",dx=" + dx + ",getMeasuredWidth()=" + getMeasuredWidth() + ",childWidth=" + child.getMeasuredWidth());
                //限定left的范围,不让child超过左右边界
                int maxLeft = getMeasuredWidth() - child.getMeasuredWidth();
                if (left < 0) {
                    left = 0;
                } else if (left > maxLeft) {
                    left = maxLeft;
                }
                return left;
            }

            /**
             * 到上边界的距离
             */
            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                //限定top的范围,不让child超过上下边界
                int maxTop = getMeasuredHeight() - child.getMeasuredHeight();
                if (top < 0) {
                    top = 0;
                } else if (top > maxTop) {
                    top = maxTop;
                }
                return top;
            }
        });
    }



    //API>=21
//    public DragRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //当手指抬起或事件取消的时候 就不拦截事件
        int actionMasked = ev.getActionMasked();
        if (actionMasked == MotionEvent.ACTION_CANCEL || actionMasked == MotionEvent.ACTION_UP) {
            return false;
        }
        return dragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        dragHelper.processTouchEvent(event);
        return true;
    }

    public void addDragChildView(View child) {
        viewList.add(child);
    }

    //创建拖动回调
    public interface OnDragDropListener {
        void onDragDrop(boolean captured);
    }

    private OnDragDropListener onDragDropListener;

    public void setOnDragDropListener(OnDragDropListener onDragDropListener) {
        this.onDragDropListener = onDragDropListener;
    }
}

