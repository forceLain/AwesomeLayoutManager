package com.forcelain.awesomelayoutmanager;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.PointF;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class AwesomeLayoutManager extends RecyclerView.LayoutManager {

    public enum Orientation {VERTICAL, HORIZONTAL}

    private static final float SCALE_THRESHOLD_PERCENT = 0.66f;
    private static final int TRANSITION_DURATION_MS = 400;
    private static final float ITEM_HEIGHT_PERCENT = 0.75f;
    private RecyclerView recyclerView;
    private int scrollStartPos;
    private SparseArray<View> viewCache = new SparseArray<>();
    private Orientation orientation = Orientation.VERTICAL;
    private int anchorPos;
    private boolean pagination;
    private float scaleThreshold = SCALE_THRESHOLD_PERCENT;
    private float pageHeightFactor = ITEM_HEIGHT_PERCENT;
    private int transitionDuration = TRANSITION_DURATION_MS;
    private int offScreenPages = 0;

    /**
     * @see #setTransitionDuration(int)
     * @return animation's duration in milliseconds
     */
    public int getTransitionDuration() {
        return transitionDuration;
    }

    /**
     * Set the duration of animated transition between vertical and horizontal {@link Orientation}
     * @param transitionDuration animation's duration in milliseconds (400 by default)
     */
    public void setTransitionDuration(int transitionDuration) {
        this.transitionDuration = transitionDuration;
    }

    /**
     * @see #setPagination(boolean)
     * @return true if the AwesomeLayoutManager acts like a ViewPager,
     * false if the AwesomeLayoutManager acts like a ListView
     */
    public boolean isPagination() {
        return pagination;
    }

    /**
     * Set the AwesomeLayoutManager to act like a ViewPager or like a ListView
     * @param pagination true is for ViewPager behavior, false otherwise
     */
    public void setPagination(boolean pagination) {
        this.pagination = pagination;
    }

    /**
     * @see #setScaleFactor(float)
     * @return current scale factor
     */
    public float getScaleFactor() {
        return 1 - scaleThreshold;
    }

    /**
     * Set how much incoming views are scaled
     * @param scaleFactor is [0..1]. 0 - not scaled, 1 - maximum scaled
     */
    public void setScaleFactor(float scaleFactor) {
        this.scaleThreshold = 1 - scaleFactor;
    }

    /**
     * @see #setPageHeightFactor(float)
     * @return current page height factor
     */
    public float getPageHeightFactor() {
        return pageHeightFactor;
    }

    /**
     * Set the maximum page height as a percentage of the RecyclerView's height
     * @param pageHeightFactor in (0, 1]. 0.5 is for half-height, 1 is for full RecyclerView's height
     */
    public void setPageHeightFactor(float pageHeightFactor) {
        this.pageHeightFactor = pageHeightFactor;
    }

    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * Change the orientation immediately (without animation)
     * @param orientation The {@link Orientation} to use
     */
    public void setOrientation(Orientation orientation) {
        View anchorView = getAnchorView();
        anchorPos = anchorView != null ? getPosition(anchorView) : 0;
        if (orientation != null) {
            this.orientation = orientation;
        }
        requestLayout();
    }

    /**
     * Animated expand the page at the given position and change the orientation to Orientation.HORIZONTAL
     * @param pos Adapter's position to open
     */
    public void openItem(int pos) {
        if (orientation == Orientation.VERTICAL) {
            View viewToOpen = null;
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                View view = getChildAt(i);
                int position = getPosition(view);
                if (position == pos) {
                    viewToOpen = view;
                }
            }
            if (viewToOpen != null) {
                openView(viewToOpen);
            }
        }
    }

    /**
     * Animated collapse current page and change the orientation to Orientation.VERTICAL
     */
    public void close() {
        View targetView = getAnchorView();
        final ArrayList<ViewAnimationInfo> animationInfos = new ArrayList<>();
        int childCount = getChildCount();
        int targetPos = getPosition(targetView);
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            int pos = getPosition(view);
            if (pos < targetPos) {
                continue;
            }
            int posDelta = pos - targetPos;
            final ViewAnimationInfo viewAnimationInfo = new ViewAnimationInfo();
            int maxHeight = (int) (getHeight() * pageHeightFactor);
            if (animationInfos.isEmpty()) {
                viewAnimationInfo.startTop = getDecoratedTop(view);
                viewAnimationInfo.startBottom = getDecoratedBottom(view);
                viewAnimationInfo.finishTop = viewAnimationInfo.startTop;
                viewAnimationInfo.finishBottom = viewAnimationInfo.finishTop + Math.min(maxHeight, getDecoratedMeasuredHeight(view));
            } else {
                ViewAnimationInfo prevViewInfo = animationInfos.get(animationInfos.size() - 1);
                viewAnimationInfo.startTop = getHeight() * posDelta;
                viewAnimationInfo.startBottom = viewAnimationInfo.startTop + getHeight();
                viewAnimationInfo.finishTop = prevViewInfo.finishBottom;
                viewAnimationInfo.finishBottom = viewAnimationInfo.finishTop + Math.min(maxHeight, getDecoratedMeasuredHeight(view));
            }
            viewAnimationInfo.view = view;
            animationInfos.add(viewAnimationInfo);
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(transitionDuration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animationProgress = (float) animation.getAnimatedValue();
                for (ViewAnimationInfo animationInfo : animationInfos) {
                    int top = (int) (animationInfo.startTop + animationProgress * (animationInfo.finishTop - animationInfo.startTop));
                    int bottom = (int) (animationInfo.startBottom + animationProgress * (animationInfo.finishBottom - animationInfo.startBottom));
                    layoutDecorated(animationInfo.view, 0, top, getWidth(), bottom);
                    notifyChildState(animationInfo.view, 1 - animationProgress);
                }
                updateViewScale();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setOrientation(Orientation.VERTICAL);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();

    }


    @Override
    public void onAttachedToWindow(final RecyclerView recyclerView) {
        super.onAttachedToWindow(recyclerView);

        this.recyclerView = recyclerView;

        recyclerView.setChildDrawingOrderCallback(new RecyclerView.ChildDrawingOrderCallback() {
            @Override
            public int onGetChildDrawingOrder(int childCount, int i) {
                return childCount - i - 1;
            }
        });

        recyclerView.setOnFlingListener(new RecyclerView.OnFlingListener() {
            @Override
            public boolean onFling(int velocityX, int velocityY) {
                if (!pagination) {
                    return false;
                }
                int position = scrollStartPos;
                int velocity = 0;
                switch (orientation) {
                    case VERTICAL:
                        velocity = velocityY;
                        break;
                    case HORIZONTAL:
                        velocity = velocityX;
                        break;
                }
                position = velocity > 0 ? position + 1 : position - 1;
                position = Math.max(position, 0);
                position = Math.min(position, getItemCount() - 1);
                recyclerView.smoothScrollToPosition(position);
                return true;
            }
        });
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        super.onDetachedFromWindow(view, recycler);
        recyclerView = null;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        fill(recycler);
        anchorPos = -1;
    }

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (!pagination) {
            return;
        }
        if (state == RecyclerView.SCROLL_STATE_DRAGGING) {
            scrollStartPos = getPosition(getAnchorView());
        }
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            switch (orientation) {
                case VERTICAL:
                    checkLayoutVertical();
                    break;
                case HORIZONTAL:
                    checkLayoutHorizontal();
                    break;
            }
        }

    }

    private void checkLayoutHorizontal() {
        View anchorView = getAnchorView();
        int left = getDecoratedLeft(anchorView);
        if (left != 0) {
            recyclerView.smoothScrollBy(left, 0);
        }
    }

    private void checkLayoutVertical() {
        View anchorView = getAnchorView();
        int position = getPosition(anchorView);
        if (position != 0 && position == getItemCount() - 1) {
            int bottom = getDecoratedBottom(anchorView);
            if (bottom != getHeight()) {
                recyclerView.smoothScrollBy(0, bottom);
            }
        } else {
            int decoratedTop = getDecoratedTop(anchorView);
            if (decoratedTop != 0) {
                recyclerView.smoothScrollBy(0, decoratedTop);
            }
        }
    }

    protected void openView(final View targetView) {
        final ArrayList<ViewAnimationInfo> animationInfos = new ArrayList<>();
        int childCount = getChildCount();
        int targetPos = getPosition(targetView);
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            int pos = getPosition(view);
            int posDelta = pos - targetPos;
            final ViewAnimationInfo viewAnimationInfo = new ViewAnimationInfo();
            viewAnimationInfo.startTop = getDecoratedTop(view);
            viewAnimationInfo.startBottom = getDecoratedBottom(view);
            viewAnimationInfo.finishTop = getHeight() * posDelta;
            viewAnimationInfo.finishBottom = viewAnimationInfo.finishTop + getDecoratedMeasuredHeight(targetView);
            viewAnimationInfo.view = view;
            animationInfos.add(viewAnimationInfo);
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(transitionDuration);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float animationProgress = (float) animation.getAnimatedValue();
                for (ViewAnimationInfo animationInfo : animationInfos) {
                    int top = (int) (animationInfo.startTop + animationProgress * (animationInfo.finishTop - animationInfo.startTop));
                    int bottom = (int) (animationInfo.startBottom + animationProgress * (animationInfo.finishBottom - animationInfo.startBottom));
                    layoutDecorated(animationInfo.view, 0, top, getWidth(), bottom);
                    notifyChildState(animationInfo.view, animationProgress);
                }
                updateViewScale();
            }
        });
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setOrientation(Orientation.HORIZONTAL);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });
        animator.start();
    }

    private float map(float x1, float x2, float y1, float y2, float n) {
        return (n - x1) * (y2 - y1) / (x2 - x1) + y1;
    }

    private void fill(RecyclerView.Recycler recycler) {
        offScreenPages = (int) (getHeight() / pageHeightFactor);
        View anchorView = getAnchorView();
        viewCache.clear();
        for (int i = 0, cnt = getChildCount(); i < cnt; i++) {
            View view = getChildAt(i);
            int pos = getPosition(view);
            viewCache.put(pos, view);
        }

        for (int i = 0; i < viewCache.size(); i++) {
            detachView(viewCache.valueAt(i));
        }

        switch (orientation) {

            case VERTICAL:
                fillUp(anchorView, recycler);
                fillDown(anchorView, recycler);
                break;
            case HORIZONTAL:
                fillLeft(anchorView, recycler);
                fillRight(anchorView, recycler);
                break;
        }

        for (int i = 0; i < viewCache.size(); i++) {
            recycler.recycleView(viewCache.valueAt(i));
        }

        updateViewScale();
    }

    private void fillUp(@Nullable View anchorView, RecyclerView.Recycler recycler) {
        int anchorPos;
        int anchorTop = 0;

        if (this.anchorPos >= 0) {
            anchorPos = this.anchorPos;
        } else if (anchorView == null) {
            anchorPos = 0;
        } else {
            anchorPos = getPosition(anchorView);
            anchorTop = getDecoratedTop(anchorView);
        }

        boolean fillUp = true;
        int pos = anchorPos - 1;
        int viewBottom = anchorTop;
        int viewHeight = (int) (getHeight() * pageHeightFactor);
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.AT_MOST);
        while (fillUp && pos >= 0) {
            View view = viewCache.get(pos);
            if (view == null) {
                view = recycler.getViewForPosition(pos);
                addView(view, 0);
                measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                layoutDecorated(view, 0, viewBottom - Math.min(viewHeight, getDecoratedMeasuredHeight(view)), decoratedMeasuredWidth, viewBottom);
            } else {
                attachView(view, 0);
                viewCache.remove(pos);
            }
            notifyChildState(view, 0);
            viewBottom = getDecoratedTop(view);
            fillUp = (viewBottom > 0);
            pos--;
        }
    }

    private void fillDown(@Nullable View anchorView, RecyclerView.Recycler recycler) {
        int anchorPos;
        int anchorTop = 0;

        if (this.anchorPos >= 0) {
            anchorPos = this.anchorPos;
        } else if (anchorView == null) {
            anchorPos = 0;
        } else {
            anchorPos = getPosition(anchorView);
            anchorTop = getDecoratedTop(anchorView);
        }

        int pos = anchorPos;
        boolean fillDown = true;
        int height = getHeight();
        int viewTop = anchorTop;
        int itemCount = getItemCount();
        int viewHeight = (int) (getHeight() * pageHeightFactor);
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(getHeight(), View.MeasureSpec.AT_MOST);

        while (fillDown && pos < itemCount) {
            View view = viewCache.get(pos);
            if (view == null) {
                view = recycler.getViewForPosition(pos);
                addView(view);
                measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                layoutDecorated(view, 0, viewTop, decoratedMeasuredWidth, viewTop + Math.min(viewHeight, getDecoratedMeasuredHeight(view)));
            } else {
                attachView(view);
                viewCache.remove(pos);
            }
            notifyChildState(view, 0);
            viewTop = getDecoratedBottom(view);
            fillDown = viewTop <= height;
            pos++;
        }
    }

    private void fillLeft(@Nullable View anchorView, RecyclerView.Recycler recycler) {
        int anchorPos;
        int anchorLeft = 0;
        if (this.anchorPos >= 0) {
            anchorPos = this.anchorPos;
        } else if (anchorView == null) {
            anchorPos = 0;
        } else {
            anchorPos = getPosition(anchorView);
            anchorLeft = getDecoratedLeft(anchorView);
        }

        int pos = anchorPos - 1;
        int nextViewRight = anchorLeft;
        int width = getWidth();
        boolean fillLeft = canFillLeft(nextViewRight, width);
        int height = getHeight();
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST);
        while (fillLeft && pos >= 0) {
            View view = viewCache.get(pos);
            if (view == null) {
                view = recycler.getViewForPosition(pos);
                addView(view, 0);
                measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
                int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                layoutDecorated(view, nextViewRight - decoratedMeasuredWidth, 0, nextViewRight, decoratedMeasuredHeight);
            } else {
                attachView(view);
                viewCache.remove(pos);
            }
            notifyChildState(view, 1);
            nextViewRight = getDecoratedLeft(view);
            fillLeft = canFillLeft(nextViewRight, width);
            pos--;
        }
    }

    private boolean canFillLeft(int nextViewRight, int width) {
        return nextViewRight > -width * offScreenPages;
    }

    private void fillRight(View anchorView, RecyclerView.Recycler recycler) {
        int anchorPos;
        int anchorLeft = 0;
        if (this.anchorPos >= 0) {
            anchorPos = this.anchorPos;
        } else if (anchorView == null) {
            anchorPos = 0;
        } else {
            anchorPos = getPosition(anchorView);
            anchorLeft = getDecoratedLeft(anchorView);
        }

        int pos = anchorPos;
        boolean fillRight = true;
        int nextViewLeft = anchorLeft;
        int itemCount = getItemCount();
        int width = getWidth();
        int height = getHeight();
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.AT_MOST);

        while (fillRight && pos < itemCount) {
            View view = viewCache.get(pos);
            if (view == null) {
                view = recycler.getViewForPosition(pos);
                addView(view);
                measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
                int decoratedMeasuredHeight = getDecoratedMeasuredHeight(view);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                layoutDecorated(view, nextViewLeft, 0, nextViewLeft + decoratedMeasuredWidth, decoratedMeasuredHeight);
            } else {
                attachView(view);
                viewCache.remove(pos);
            }
            notifyChildState(view, 1);
            nextViewLeft = getDecoratedRight(view);
            fillRight = nextViewLeft < width + width * offScreenPages;
            pos++;
        }
    }

    private void notifyChildState(View view, float progress) {
        RecyclerView.ViewHolder childViewHolder = recyclerView.getChildViewHolder(view);
        if (childViewHolder instanceof AwesomeViewHolder) {
            ((AwesomeViewHolder) childViewHolder).onStateChanged(progress);
        }
    }

    private void updateViewScale() {
        int childCount = getChildCount();
        int height = getHeight();
        int thresholdPerc = (int) (height * scaleThreshold);
        for (int i = 0; i < childCount; i++) {
            float scale = 1f;
            View view = getChildAt(i);
            int viewTop = getDecoratedTop(view);
            if (viewTop >= thresholdPerc) {
                int delta = viewTop - thresholdPerc;
                scale = (height - delta) / (float) height;
                scale = Math.max(scale, 0);
            }
            int pivotY = (int) map(thresholdPerc, height, getHeight() / -2, 0, viewTop);
            view.setPivotX(view.getWidth() / 2);
            view.setPivotY(pivotY);
            view.setScaleX(scale);
            view.setScaleY(scale);
        }
    }

    protected View getAnchorView() {
        int childCount = getChildCount();
        Rect mainRect = new Rect(0, 0, getWidth(), getHeight());
        int maxSquare = 0;
        View anchorView = null;
        for (int i = 0; i < childCount; i++) {
            View view = getChildAt(i);
            int top = getDecoratedTop(view);
            int bottom = getDecoratedBottom(view);
            int left = getDecoratedLeft(view);
            int right = getDecoratedRight(view);
            Rect viewRect = new Rect(left, top, right, bottom);
            boolean intersect = viewRect.intersect(mainRect);
            if (intersect) {
                int square = viewRect.width() * viewRect.height();
                if (square > maxSquare) {
                    maxSquare = square;
                    anchorView = view;
                }
            }
        }
        return anchorView;
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        if (position >= getItemCount()) {
            return;
        }

        LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext()) {
            @Override
            public PointF computeScrollVectorForPosition(int targetPosition) {
                return AwesomeLayoutManager.this.computeScrollVectorForPosition(targetPosition);
            }

            @Override
            protected int getHorizontalSnapPreference() {
                return SNAP_TO_START;
            }

            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
        };
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    private PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0) {
            return null;
        }
        final int firstChildPos = getPosition(getChildAt(0));
        final int direction = targetPosition < firstChildPos ? -1 : 1;
        if (orientation == Orientation.HORIZONTAL) {
            return new PointF(direction, 0);
        } else {
            return new PointF(0, direction);
        }
    }

    @Override
    public boolean canScrollVertically() {
        return orientation == Orientation.VERTICAL;
    }

    @Override
    public boolean canScrollHorizontally() {
        return orientation == Orientation.HORIZONTAL;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int delta = scrollHorizontallyInternal(dx);
        offsetChildrenHorizontal(-delta);
        fill(recycler);
        return delta;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        int delta = scrollVerticallyInternal(dy);
        offsetChildrenVertical(-delta);
        fill(recycler);
        return delta;
    }

    @Override
    public void scrollToPosition(int position) {
        super.scrollToPosition(position);
        anchorPos = position;
        requestLayout();
    }

    private int scrollVerticallyInternal(int dy) {
        int childCount = getChildCount();
        int itemCount = getItemCount();
        if (childCount == 0) {
            return 0;
        }

        final View topView = getChildAt(0);
        final View bottomView = getChildAt(childCount - 1);

        int viewSpan = getDecoratedBottom(bottomView) - getDecoratedTop(topView);
        if (viewSpan <= getHeight()) {
            return 0;
        }

        int delta = 0;
        if (dy < 0) {
            View firstView = getChildAt(0);
            int firstViewAdapterPos = getPosition(firstView);
            if (firstViewAdapterPos > 0) {
                delta = dy;
            } else {
                int viewTop = getDecoratedTop(firstView);
                delta = Math.max(viewTop, dy);
            }
        } else if (dy > 0) {
            View lastView = getChildAt(childCount - 1);
            int lastViewAdapterPos = getPosition(lastView);
            if (lastViewAdapterPos < itemCount - 1) {
                delta = dy;
            } else {
                int viewBottom = getDecoratedBottom(lastView);
                int parentBottom = getHeight();
                delta = Math.min(viewBottom - parentBottom, dy);
            }
        }
        return delta;
    }

    private int scrollHorizontallyInternal(int dx) {
        int childCount = getChildCount();
        int itemCount = getItemCount();
        if (childCount == 0) {
            return 0;
        }
        int delta = 0;
        if (dx < 0) {
            View firstView = getChildAt(0);
            int firstViewAdapterPos = getPosition(firstView);
            if (firstViewAdapterPos > 0) {
                delta = dx;
            } else {
                int viewLeft = getDecoratedLeft(firstView);
                delta = Math.max(viewLeft, dx);
            }
        } else if (dx > 0) {
            View lastView = getChildAt(childCount - 1);
            int lastViewAdapterPos = getPosition(lastView);
            if (lastViewAdapterPos < itemCount - 1) {
                delta = dx;
            } else {
                int viewRight = getDecoratedRight(lastView);
                delta = Math.min(viewRight - getWidth(), dx);
            }
        }
        return delta;
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        Rect decorRect = new Rect();
        calculateItemDecorationsForChild(child, decorRect);
        RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
        widthSpec = updateSpecWithExtra(widthSpec, lp.leftMargin + decorRect.left,
                lp.rightMargin + decorRect.right);
        heightSpec = updateSpecWithExtra(heightSpec, lp.topMargin + decorRect.top,
                lp.bottomMargin + decorRect.bottom);
        child.measure(widthSpec, heightSpec);
    }

    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        final int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            return View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.getSize(spec) - startInset - endInset, mode);
        }
        return spec;
    }


    private static class ViewAnimationInfo {
        int startTop;
        int startBottom;
        int finishTop;
        int finishBottom;
        View view;
    }
}
