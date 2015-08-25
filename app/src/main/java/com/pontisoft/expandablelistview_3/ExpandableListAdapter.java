package com.pontisoft.expandablelistview_3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;

public class ExpandableListAdapter extends SimpleExpandableListAdapter {

    public enum SplitMode { SPL_NONE, SPL_ADD, SPL_DEC };

    private ArrayList<HashMap<String, Object>> currentmaplist = null;
    private List<List<HashMap<String, Object>>> childlist = null;
    private List<HashMap<String, Object>> childobjlist = null;
    private Activity actctx;
    private ExpandableListView mListView;
    private View mDownView; // view which was touched to be swiped
    private int mDownPosition;
    private boolean mItemPressed;
    private boolean mSwiping;
    private boolean mPaused;

    private VelocityTracker mVelocityTracker = null;

    private static final int SWIPE_DURATION = 250;
    private static final int MOVE_DURATION = 150;

    private static final int[] EMPTY_STATE_SET = {};
    private static final int[] GROUP_EXPANDED_STATE_SET =
            {android.R.attr.state_expanded};
    private static final int[][] GROUP_STATE_SETS = {
            EMPTY_STATE_SET, // 0
            GROUP_EXPANDED_STATE_SET // 1
    };
    public ExpandableListAdapter(Context context,
                                            List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
                                            int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
                                            List<? extends List<? extends Map<String, ?>>> childData,
                                            int childLayout, int lastChildLayout, String[] childFrom,
                                            int[] childTo) {
        super(context, groupData, expandedGroupLayout, collapsedGroupLayout, groupFrom,
                groupTo, childData, childLayout, lastChildLayout, childFrom, childTo);
        setEnviron(context, groupData, childData);
    }

    public ExpandableListAdapter(Context context,
                                            List<? extends Map<String, ?>> groupData, int expandedGroupLayout,
                                            int collapsedGroupLayout, String[] groupFrom, int[] groupTo,
                                            List<? extends List<? extends Map<String, ?>>> childData,
                                            int childLayout, String[] childFrom, int[] childTo) {
        super(context, groupData, expandedGroupLayout, collapsedGroupLayout,
                groupFrom, groupTo, childData, childLayout, childFrom, childTo);
        setEnviron(context, groupData, childData);
    }

    public ExpandableListAdapter(Context context,
                                            List<? extends Map<String, ?>> groupData, int groupLayout,
                                            String[] groupFrom, int[] groupTo,
                                            List<? extends List<? extends Map<String, ?>>> childData,
                                            int childLayout, String[] childFrom, int[] childTo) {
        super(context, groupData, groupLayout, groupFrom, groupTo, childData,
                childLayout, childFrom, childTo);
        setEnviron(context, groupData, childData);
    }
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {

        float mDownX, mDownY;
        private int mSwipeSlop = -1;
        private int mSwipingSlop = 0;
        private int mMinFlingVelocity;
        private int mMaxFlingVelocity;

        void finalizeAnimation (final View v, SplitMode split, int splcount) {
            v.setAlpha(1);
            v.setTranslationX(0);
            if (split != SplitMode.SPL_NONE) {
                if (split == SplitMode.SPL_DEC) {
                    execSplit(mListView, v, 0-splcount);
                }
                else {
                    execSplit(mListView, v, splcount);
                }
            } else {
                mSwiping = false;
            }

        };

        @SuppressLint("NewApi")
        @Override
        public boolean onTouch(final View v, MotionEvent event) {

            mListView = (ExpandableListView) actctx.findViewById(R.id.lvExp);
            final int lSplitCount = 1;

            if (mSwipeSlop < 0) {
                mSwipeSlop = ViewConfiguration.get(actctx).getScaledTouchSlop();
                mMinFlingVelocity = ViewConfiguration.get(actctx).getScaledMinimumFlingVelocity() * 16;
                mMaxFlingVelocity = ViewConfiguration.get(actctx).getScaledMaximumFlingVelocity();

            }
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    if (mItemPressed || mPaused) {
                        // Multi-item swipes not handled
                        Log.v("Animate", "ACTION_DOWN: Item Pressed or Scroll pending");
                        return false;
                    }
// Find the child view that was touched (perform a hit test)
                    Rect rect = new Rect();
                    int childCount = mListView.getChildCount();
                    int[] listViewCoords = new int[2];
                    mListView.getLocationOnScreen(listViewCoords);
                    int x = (int) event.getRawX() - listViewCoords[0];
                    int y = (int) event.getRawY() - listViewCoords[1];
                    View child;
                    for (int i = 0; i < childCount; i++) {
                        child = mListView.getChildAt(i);
                        child.getHitRect(rect);
                        if (rect.contains(x, y)) {
                            mDownView = child; // a listview item was touched
                            break;
                        }
                    }

                    if (mDownView != null) {
                        mDownX = event.getRawX();
                        mDownY = event.getRawY();
                        mDownPosition = mListView.getPositionForView(mDownView);
                        if (mDownView.getTag() != null) {
                            if (mVelocityTracker == null) {
                                // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                                mVelocityTracker = VelocityTracker.obtain();
                            } else {
                                // Reset the velocity tracker back to its initial state.
                                mVelocityTracker.clear();
                            }
                            mVelocityTracker.addMovement(event);
                            mItemPressed = true;
                        } else {
                            mDownView = null;
                            mDownPosition = ListView.INVALID_POSITION;
                            return false;
                        }
                    }
                    return false;
                }
                case MotionEvent.ACTION_CANCEL: {
                    mDownView.setAlpha(1);
                    mDownView.setTranslationX(0);
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                    mItemPressed = false;
                    mDownView = null;
                    mSwiping = false;
                    mDownY = 0;
                    mDownX = 0;
                    mDownPosition = ListView.INVALID_POSITION;
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    if (mDownView != null)
                    {
                        if (mVelocityTracker == null || mPaused) {
                            return (false);
                        }
                        mVelocityTracker.addMovement(event);
                        float deltaX = event.getRawX() - mDownX;
                        float deltaY = event.getRawY() - mDownY;
                        float deltaXAbs = Math.abs(deltaX);
                        float deltaYAbs = Math.abs(deltaY);
                        if ((deltaXAbs > mSwipeSlop) && (deltaYAbs < deltaXAbs / 2)) {
                            // Cancel ListView's touch (un-highlighting the item)
                            mSwiping = true;
                            mSwipingSlop = (deltaX > 0 ? mSwipeSlop : -mSwipeSlop);
                            mListView.requestDisallowInterceptTouchEvent(true);
                            MotionEvent cancelEvent = MotionEvent.obtain(event);
                            cancelEvent.setAction(MotionEvent.ACTION_CANCEL |
                                    (event.getActionIndex()
                                            << MotionEvent.ACTION_POINTER_INDEX_SHIFT));
                            mListView.onTouchEvent(cancelEvent);
                            cancelEvent.recycle();
                        }
                        if (mSwiping) {
                            mDownView.setTranslationX((deltaX - mSwipingSlop));
                            mDownView.setAlpha(Math.max(0f, Math.min(1f,
                                    1f - 2f * Math.abs(deltaX) / mDownView.getWidth())));
                            return true;
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    // User let go - figure out whether to animate the view out, or back into place
                    boolean lret = false;
                    if ((mSwiping) && (mDownView != null)) {
                        float deltaX = event.getRawX() - mDownX;
                        float deltaXAbs = Math.abs(deltaX);
                        float fractionCovered = 0;
                        float endX = 0;
                        float endAlpha = 0;
                        SplitMode msplitmode = SplitMode.SPL_NONE;
                        mVelocityTracker.computeCurrentVelocity(1000);
                        float velocityX = mVelocityTracker.getXVelocity();
                        float absVelocityX = Math.abs(velocityX);
                        float absVelocityY = Math.abs(mVelocityTracker.getYVelocity());
                        if (deltaXAbs > (mDownView.getWidth() / 6)) {
                            // Greater than a quarter of the width - animate it out
                            fractionCovered = deltaXAbs / mDownView.getWidth();
                            endX = deltaX < 0 ? -mDownView.getWidth() : mDownView.getWidth();
                            endAlpha = 0;
                            if (deltaX > 0) {
                                msplitmode = SplitMode.SPL_ADD;
                            } else {
                                msplitmode = SplitMode.SPL_DEC;
                            }
                        } else if (mMinFlingVelocity <= absVelocityX && absVelocityX <= mMaxFlingVelocity
                                && absVelocityY < absVelocityX && mSwiping) {
                            // split only if flinging in the same direction as dragging
                            boolean swipe = (velocityX < 0) == (deltaX < 0);
                            if (swipe) {
                                fractionCovered = deltaXAbs / mDownView.getWidth();
                                endX = deltaX < 0 ? -mDownView.getWidth() : mDownView.getWidth();
                                endAlpha = 0;
                                if (deltaX > 0) {
                                    msplitmode = SplitMode.SPL_ADD;
                                } else {
                                    msplitmode = SplitMode.SPL_DEC;
                                }
                            } else {
                                fractionCovered = 1 - (deltaXAbs / mDownView.getWidth());
                                endX = 0;
                                endAlpha = 1;
                                msplitmode = SplitMode.SPL_NONE;
                            }
                        } else {
                            // Not far enough - animate it back
                            fractionCovered = 1 - (deltaXAbs / mDownView.getWidth());
                            endX = 0;
                            endAlpha = 1;
                            msplitmode = SplitMode.SPL_NONE;
                        }
                        if (mDownPosition != ListView.INVALID_POSITION) {
                            // split
                            final View downView = mDownView; // mDownView gets null'd before animation ends
                            final SplitMode splitmode = msplitmode;
                            long duration = (int) ((1 - fractionCovered) * Math.min(500, (mDownView.getWidth() * 1000 / Math.abs(mVelocityTracker.getXVelocity()))));
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                                mDownView.animate().setDuration(duration).alpha(endAlpha).translationX(endX).setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        finalizeAnimation(downView, splitmode, lSplitCount);
                                    }
                                });
                            } else {
                                mDownView.animate().setDuration(duration).alpha(endAlpha).translationX(endX).withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        finalizeAnimation(downView, splitmode, lSplitCount);
                                    }
                                });
                            }
                            mSwiping = false;
                            lret = false;
                        }
                    }
                    mItemPressed = false;
                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                    mDownView = null;
                    mDownX = 0;
                    mDownY = 0;
                    mDownPosition = ListView.INVALID_POSITION;
                    return lret;
                }
                default:
                    Log.v("Animate", String.format("default event:%d", event.getAction()));
            }
            return false;
        }
    };

    void setEnviron (Context context, List<? extends Map<String, ?>> groupData, List<? extends List<? extends Map<String, ?>>> childData) {
        actctx = (Activity) context;
        currentmaplist = (ArrayList<HashMap<String, Object>>) groupData;
        childlist = (ArrayList<List<HashMap<String, Object>>>) childData;
    }

    @Override
    public View getGroupView (int groupPosition,
                              boolean isExpanded,
                              View convertView,
                              ViewGroup parent) {
        View v = super.getGroupView(groupPosition, isExpanded, convertView, parent);
        HashMap<String, Object> omap = currentmaplist.get(groupPosition);
        v.setTag(omap);
//        v.setOnTouchListener(mTouchListener); // not on Item level, perform onTouch on the viewGroup level
        View ind = v.findViewById( R.id.explist_indicator);
        if( ind != null ) {
            ImageView indicator = (ImageView) ind;
            if (getChildrenCount(groupPosition) == 0) {
                indicator.setVisibility(View.INVISIBLE);
            } else {
                indicator.setVisibility(View.VISIBLE);
                int stateSetIndex = (isExpanded ? 1 : 0);
                Drawable drawable = indicator.getDrawable();
                drawable.setState(GROUP_STATE_SETS[stateSetIndex]);
            }
        }
        return v;
    }

    /**
     * performs a simple split calculation, swipe to left -> deduct count, add splitcount, swipe to right --> add count, deduct splitcount
     */
    private void execSplit(ExpandableListView lv, View v, int splcount) {
        HashMap<String, Object> omap = (HashMap<String, Object>)v.getTag();

        if (((splcount < 0) && (Double.parseDouble((String)omap.get("splitcount")) >= Math.abs(splcount))) ||
                ((splcount > 0) && ((Double.parseDouble((String) omap.get("count")) - splcount) >= 0)))
        {
            omap.put("splitcount", String.format(Locale.US, "%d", Integer.parseInt((String) omap.get("splitcount")) + splcount));
            omap.put("count", String.format(Locale.US, "%d", Integer.parseInt((String) omap.get("count")) - splcount));
            childobjlist = null;
            for (int j = 0; j < currentmaplist.size(); j++) {
                if (((String)(currentmaplist.get(j).get("isorder"))).equalsIgnoreCase("true")) {
                    if (((String)(currentmaplist.get(j).get("id"))).equalsIgnoreCase((String)omap.get("id"))) {
                        childobjlist = childlist.get(j);
                        if (childobjlist != null) {
                            if (childobjlist.size() > 0)
                            {
                                for (int i = 0; i < childobjlist.size(); i++) {
                                    HashMap<String, Object> cmap = childobjlist.get(i);
                                    if (splcount != 0) {
                                        cmap.put("splitcount", String.format(Locale.US, "%d", Integer.parseInt((String) cmap.get("splitcount")) + splcount));
                                        cmap.put("count", String.format(Locale.US, "%d", Integer.parseInt((String) cmap.get("count")) - splcount));
                                    }
                                }
                            }
                        }
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }
    }

    /**
     * Enables or disables (pauses or resumes) watching for swipe-to-dismiss gestures.
     *
     * @param enabled Whether or not to watch for gestures.
     */
    public void setEnabled(boolean enabled) {
        mPaused = !enabled;
    }

    /**
     * Returns an {@link AbsListView.OnScrollListener} to be added to the {@link
     * ExpandableListView} using {@link ExpandableListView#setOnScrollListener(AbsListView.OnScrollListener)}.
     * If a scroll listener is already assigned, the caller should still pass scroll changes through
     * to this listener. This will ensure that this {@link View.OnTouchListener} is
     * paused during list view scrolling.</p>
     *
     */
    public AbsListView.OnScrollListener makeScrollListener() {
        return new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
                setEnabled(scrollState != AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        };
    }


    public View.OnTouchListener getTouchListener() {
        return mTouchListener;
    }




}