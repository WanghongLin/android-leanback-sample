/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.app;

import java.util.ArrayList;

import android.animation.TimeAnimator;
import android.animation.TimeAnimator.TimeListener;
import android.graphics.Canvas;
import android.os.Bundle;
import com.example.android.leanback.R;
import android.support.v17.leanback.graphics.ColorOverlayDimmer;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v17.leanback.widget.HorizontalGridView;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

/**
 * An ordered set of rows of leanback widgets.
 */
public class RowsFragment extends BaseRowFragment {

    /**
     * Internal helper class that manages row select animation and apply a default
     * dim to each row.
     */
    final class RowViewHolderExtra implements TimeListener {
        final RowPresenter mRowPresenter;
        final Presenter.ViewHolder mRowViewHolder;

        final TimeAnimator mSelectAnimator = new TimeAnimator();
        final ColorOverlayDimmer mColorDimmer;

        int mSelectAnimatorDurationInUse;
        Interpolator mSelectAnimatorInterpolatorInUse;
        float mSelectLevelAnimStart;
        float mSelectLevelAnimDelta;

        RowViewHolderExtra(ItemBridgeAdapter.ViewHolder ibvh) {
            mRowPresenter = (RowPresenter) ibvh.getPresenter();
            mRowViewHolder = ibvh.getViewHolder();
            mSelectAnimator.setTimeListener(this);
            if (mRowPresenter.getSelectEffectEnabled()
                    && mRowPresenter.isUsingDefaultSelectEffect()) {
                mColorDimmer = ColorOverlayDimmer.createDefault(ibvh.itemView.getContext());
            } else {
                mColorDimmer = null;
            }
        }

        @Override
        public void onTimeUpdate(TimeAnimator animation, long totalTime, long deltaTime) {
            if (mSelectAnimator.isRunning()) {
                updateSelect(totalTime, deltaTime);
            }
        }

        void updateSelect(long totalTime, long deltaTime) {
            float fraction;
            if (totalTime >= mSelectAnimatorDurationInUse) {
                fraction = 1;
                mSelectAnimator.end();
            } else {
                fraction = (float) (totalTime / (double) mSelectAnimatorDurationInUse);
            }
            if (mSelectAnimatorInterpolatorInUse != null) {
                fraction = mSelectAnimatorInterpolatorInUse.getInterpolation(fraction);
            }
            float level =  mSelectLevelAnimStart + fraction * mSelectLevelAnimDelta;
            if (mColorDimmer != null) {
                mColorDimmer.setActiveLevel(level);
            }
            mRowPresenter.setSelectLevel(mRowViewHolder, level);
        }

        void animateSelect(boolean select, boolean immediate) {
            endSelectAnimation();
            final float end = select ? 1 : 0;
            if (immediate) {
                mRowPresenter.setSelectLevel(mRowViewHolder, end);
                if (mColorDimmer != null) {
                    mColorDimmer.setActiveLevel(end);
                }
            } else if (mRowPresenter.getSelectLevel(mRowViewHolder) != end) {
                mSelectAnimatorDurationInUse = mSelectAnimatorDuration;
                mSelectAnimatorInterpolatorInUse = mSelectAnimatorInterpolator;
                mSelectLevelAnimStart = mRowPresenter.getSelectLevel(mRowViewHolder);
                mSelectLevelAnimDelta = end - mSelectLevelAnimStart;
                mSelectAnimator.start();
            }
        }

        void endAnimations() {
            endSelectAnimation();
        }

        void endSelectAnimation() {
            mSelectAnimator.end();
        }

        void drawDimForSelection(Canvas c) {
            if (mColorDimmer != null) {
                mColorDimmer.drawColorOverlay(c, mRowViewHolder.view, false);
            }
        }
    }

    private static final String TAG = "RowsFragment";
    private static final boolean DEBUG = false;

    private ItemBridgeAdapter.ViewHolder mSelectedViewHolder;
    private boolean mExpand = true;
    private boolean mViewsCreated;

    private OnItemSelectedListener mOnItemSelectedListener;
    private OnItemClickedListener mOnItemClickedListener;

    // Select animation and interpolator are not intended to be
    // exposed at this moment. They might be synced with vertical scroll
    // animation later.
    int mSelectAnimatorDuration;
    Interpolator mSelectAnimatorInterpolator = new DecelerateInterpolator(2);

    private RecyclerView.RecycledViewPool mRecycledViewPool;
    private ArrayList<Presenter> mPresenterMapper;

    /**
     * Sets an item clicked listener on the fragment.
     * OnItemClickedListener will override {@link View.OnClickListener} that
     * item presenter sets during {@link Presenter#onCreateViewHolder(ViewGroup)}.
     * So in general,  developer should choose one of the listeners but not both.
     */
    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mOnItemClickedListener = listener;
        if (mViewsCreated) {
            throw new IllegalStateException(
                    "Item clicked listener must be set before views are created");
        }
    }

    /**
     * Returns the item clicked listener.
     */
    public OnItemClickedListener getOnItemClickedListener() {
        return mOnItemClickedListener;
    }

    /**
     * Set the visibility of titles/hovercard of browse rows.
     */
    public void setExpand(boolean expand) {
        mExpand = expand;
        VerticalGridView listView = getVerticalGridView();
        if (listView != null) {
            final int count = listView.getChildCount();
            if (DEBUG) Log.v(TAG, "setExpand " + expand + " count " + count);
            for (int i = 0; i < count; i++) {
                View view = listView.getChildAt(i);
                ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder) listView.getChildViewHolder(view);
                setRowViewExpanded(vh, mExpand);
            }
        }
    }

    /**
     * Sets an item selection listener.
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mOnItemSelectedListener = listener;
        VerticalGridView listView = getVerticalGridView();
        if (listView != null) {
            final int count = listView.getChildCount();
            for (int i = 0; i < count; i++) {
                View view = listView.getChildAt(i);
                ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder)
                        listView.getChildViewHolder(view);
                setOnItemSelectedListener(vh, mOnItemSelectedListener);
            }
        }
    }

    @Override
    protected void onRowSelected(ViewGroup parent, View view, int position, long id) {
        VerticalGridView listView = getVerticalGridView();
        if (listView == null) {
            return;
        }
        ItemBridgeAdapter.ViewHolder vh = (view == null) ? null :
            (ItemBridgeAdapter.ViewHolder) listView.getChildViewHolder(view);

        if (mSelectedViewHolder != vh) {
            if (DEBUG) Log.v(TAG, "new row selected position " + position + " view " + view);

            if (mSelectedViewHolder != null) {
                setRowViewSelected(mSelectedViewHolder, false, false);
            }
            mSelectedViewHolder = vh;
            if (mSelectedViewHolder != null) {
                setRowViewSelected(mSelectedViewHolder, true, false);
            }
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.lb_rows_fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectAnimatorDuration = getResources().getInteger(R.integer.lb_browse_rows_anim_duration);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (DEBUG) Log.v(TAG, "onViewCreated");
        super.onViewCreated(view, savedInstanceState);
        // Align the top edge of child with id row_content.
        // Need set this for directly using RowsFragment.
        getVerticalGridView().setItemAlignmentViewId(R.id.row_content);
        getVerticalGridView().addItemDecoration(mItemDecoration);

        mRecycledViewPool = null;
        mPresenterMapper = null;
    }

    @Override
    void setItemAlignment() {
        super.setItemAlignment();
        if (getVerticalGridView() != null) {
            getVerticalGridView().setItemAlignmentOffsetWithPadding(true);
        }
    }

    private RecyclerView.ItemDecoration mItemDecoration = new RecyclerView.ItemDecoration() {
        @Override
        public void onDrawOver(Canvas c, RecyclerView parent) {
            final int count = parent.getChildCount();
            for (int i = 0; i < count; i++) {
                ItemBridgeAdapter.ViewHolder ibvh = (ItemBridgeAdapter.ViewHolder)
                        parent.getChildViewHolder(parent.getChildAt(i));
                RowViewHolderExtra extra = (RowViewHolderExtra) ibvh.getExtraObject();
                extra.drawDimForSelection(c);
            }
        }
    };

    private static void setRowViewExpanded(ItemBridgeAdapter.ViewHolder vh, boolean expanded) {
        ((RowPresenter) vh.getPresenter()).setRowViewExpanded(vh.getViewHolder(), expanded);
    }

    private static void setRowViewSelected(ItemBridgeAdapter.ViewHolder vh, boolean selected,
            boolean immediate) {
        RowViewHolderExtra extra = (RowViewHolderExtra) vh.getExtraObject();
        extra.animateSelect(selected, immediate);
        ((RowPresenter) vh.getPresenter()).setRowViewSelected(vh.getViewHolder(), selected);
    }

    private static void setOnItemSelectedListener(ItemBridgeAdapter.ViewHolder vh,
            OnItemSelectedListener listener) {
        ((RowPresenter) vh.getPresenter()).setOnItemSelectedListener(listener);
    }

    private final ItemBridgeAdapter.AdapterListener mBridgeAdapterListener =
            new ItemBridgeAdapter.AdapterListener() {
        @Override
        public void onAddPresenter(Presenter presenter, int type) {
            ((RowPresenter) presenter).setOnItemClickedListener(mOnItemClickedListener);
        }
        @Override
        public void onCreate(ItemBridgeAdapter.ViewHolder vh) {
            VerticalGridView listView = getVerticalGridView();
            if (listView != null && ((RowPresenter) vh.getPresenter()).canDrawOutOfBounds()) {
                listView.setClipChildren(false);
            }
            setupSharedViewPool(vh.getViewHolder());
            mViewsCreated = true;
            vh.setExtraObject(new RowViewHolderExtra(vh));
            // selected state is initialized to false, then driven by grid view onChildSelected
            // events.  When there is rebind, grid view fires onChildSelected event properly.
            // So we don't need do anything special later in onBind or onAttachedToWindow.
            setRowViewSelected(vh, false, true);
        }
        @Override
        public void onAttachedToWindow(ItemBridgeAdapter.ViewHolder vh) {
            if (DEBUG) Log.v(TAG, "onAttachToWindow");
            // All views share the same mExpand value.  When we attach a view to grid view,
            // we should make sure it pick up the latest mExpand value we set early on other
            // attached views.  For no-structure-change update,  the view is rebound to new data,
            // but again it should use the unchanged mExpand value,  so we don't need do any
            // thing in onBind.
            setRowViewExpanded(vh, mExpand);
            setOnItemSelectedListener(vh, mOnItemSelectedListener);
        }
        @Override
        public void onUnbind(ItemBridgeAdapter.ViewHolder vh) {
            RowViewHolderExtra extra = (RowViewHolderExtra) vh.getExtraObject();
            extra.endAnimations();
        }
    };

    private void setupSharedViewPool(Presenter.ViewHolder viewHolder) {
        if (viewHolder instanceof ListRowPresenter.ViewHolder) {
            HorizontalGridView view = ((ListRowPresenter.ViewHolder) viewHolder).getGridView();
            // Recycled view pool is shared between all list rows
            if (mRecycledViewPool == null) {
                mRecycledViewPool = view.getRecycledViewPool();
            } else {
                view.setRecycledViewPool(mRecycledViewPool);
            }

            ItemBridgeAdapter bridgeAdapter =
                    ((ListRowPresenter.ViewHolder) viewHolder).getBridgeAdapter();
            if (mPresenterMapper == null) {
                mPresenterMapper = bridgeAdapter.getPresenterMapper();
            } else {
                bridgeAdapter.setPresenterMapper(mPresenterMapper);
            }
        }
    }

    @Override
    protected void updateAdapter() {
        super.updateAdapter();
        mSelectedViewHolder = null;
        mViewsCreated = false;

        ItemBridgeAdapter adapter = getBridgeAdapter();
        if (adapter != null) {
            adapter.setAdapterListener(mBridgeAdapterListener);
        }
    }

}
