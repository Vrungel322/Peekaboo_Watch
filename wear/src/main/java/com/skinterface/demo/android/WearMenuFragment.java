package com.skinterface.demo.android;

import android.app.Fragment;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

public class WearMenuFragment extends Fragment implements WearableListView.ClickListener {

    SSect wholeMenu;
    TextView titleView;
    WearableListView listView;
    Adapter adapter;

    public static WearMenuFragment create(SSect menu) {
        Bundle args = new Bundle();
        args.putString("menu", menu.fillJson(new JSONObject()).toString());
        WearMenuFragment fr = new WearMenuFragment();
        fr.setArguments(args);
        return fr;
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        wholeMenu = SSect.fromJson(getArguments().getString("menu"));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        adapter = new Adapter(getActivity(), wholeMenu);
        View view = inflater.inflate(R.layout.fr_wear_menu, container, false);
        titleView = (TextView) view.findViewById(R.id.wear_menu_title);
        titleView.setText(wholeMenu.title.data);
        listView = (WearableListView) view.findViewById(R.id.wear_menu_list);
        listView.setAdapter(adapter);
        listView.setClickListener(this);
        listView.addOnScrollListener(new WearableListView.OnScrollListener() {
            @Override
            public void onAbsoluteScrollChange(int i) {
                // Do only scroll the header up from the base position, not down...
                if (i > 0)
                    titleView.setY(-i);
            }
            @Override
            public void onScroll(int i) {}
            @Override
            public void onScrollStateChanged(int i) {}
            @Override
            public void onCentralPositionChanged(int i) {}
        });
        SwipeDismissLayout dismissLayout = new SwipeDismissLayout(getActivity());
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        dismissLayout.setLayoutParams(lp);
        dismissLayout.addView(view, new ViewGroup.MarginLayoutParams(lp.MATCH_PARENT, lp.MATCH_PARENT));
        dismissLayout.setOnDismissedListener(new SwipeDismissLayout.OnDismissedListener() {
            @Override
            public void onDismissed(SwipeDismissLayout layout) {
                ((WearActivity) getActivity()).exitMenu(null);
            }
        });
        dismissLayout.setOnSwipeProgressChangedListener(new SwipeDismissLayout.OnSwipeProgressChangedListener() {
            @Override
            public void onSwipeProgressChanged(SwipeDismissLayout layout, float progress, float translate) {
                getView().setScrollX(-(int)translate);
            }
            @Override
            public void onSwipeCancelled(SwipeDismissLayout layout) {
                getView().setScrollX(0);
            }
        });
        return dismissLayout;
    }

    // WearableListView click listener
    @Override
    public void onClick(WearableListView.ViewHolder v) {
        int pos = v.getAdapterPosition();
        SSect sect = adapter.mMenu.children[pos];
        if (sect.children == null || sect.children.length == 0) {
            ((WearActivity)getActivity()).exitMenu(sect);
            return;
        }
        adapter.setCurrentMenu(sect);
        titleView.setY(0);
        titleView.setText(sect.title.data);
        listView.scrollToPosition(0);
    }

    @Override
    public void onTopEmptyRegionClick() {
        SSect sect = findParentMenu(wholeMenu, adapter.mMenu);
        if (sect == null) {
            ((WearActivity) getActivity()).exitMenu(null);
            return;
        }
        adapter.setCurrentMenu(sect);
        titleView.setText(sect.title.data);
        listView.scrollToPosition(0);
    }

    protected SSect findParentMenu(SSect parent, SSect action) {
        if (parent.children == null)
            return null;
        for (SSect child : parent.children) {
            if (child == action)
                return parent;
            SSect found = findParentMenu(child, action);
            if (found != null)
                return found;
        }
        return null;
    }

    public static class WearableListItemLayout extends LinearLayout
            implements WearableListView.OnCenterProximityListener {

        private ImageView mCircle;
        private TextView mName;

        private final float mFadedTextAlpha;
        private final int mFadedCircleColor;
        private final int mChosenCircleColor;

        public WearableListItemLayout(Context context) {
            this(context, null);
        }

        public WearableListItemLayout(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public WearableListItemLayout(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);

            mFadedTextAlpha = 0.5f;
            mFadedCircleColor = getResources().getColor(R.color.grey);
            mChosenCircleColor = getResources().getColor(R.color.blue);
        }

        // Get references to the icon and text in the item layout definition
        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            // These are defined in the layout file for list items
            // (see next section)
            mCircle = (ImageView) findViewById(R.id.circle);
            mName = (TextView) findViewById(R.id.name);
        }

        @Override
        public void onCenterPosition(boolean animate) {
            mName.setAlpha(1f);
            Drawable dr = mCircle.getDrawable();
            dr.mutate().setAlpha(255);
            //((GradientDrawable) mCircle.getDrawable()).setColor(mChosenCircleColor);
        }

        @Override
        public void onNonCenterPosition(boolean animate) {
            Drawable dr = mCircle.getDrawable();
            dr.mutate().setAlpha(128);
            //((GradientDrawable) mCircle.getDrawable()).setColor(mFadedCircleColor);
            mName.setAlpha(mFadedTextAlpha);
        }
    }

    private static final class Adapter extends WearableListView.Adapter {
        private SSect mMenu;
        private final Context mContext;
        private final LayoutInflater mInflater;

        // Provide a suitable constructor (depends on the kind of dataset)
        public Adapter(Context context, SSect menu) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mMenu = menu;
        }

        public void setCurrentMenu(SSect menu) {
            mMenu = menu;
            notifyDataSetChanged();
        }

        // Provide a reference to the type of views you're using
        public static class ItemViewHolder extends WearableListView.ViewHolder {
            private TextView textView;
            public ItemViewHolder(View itemView) {
                super(itemView);
                // find the text view within the custom item's layout
                textView = (TextView) itemView.findViewById(R.id.name);
            }
        }

        // Create new views for list items
        // (invoked by the WearableListView's layout manager)
        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            // Inflate our custom layout for list items
            return new ItemViewHolder(mInflater.inflate(R.layout.menu_item, null));
        }

        // Replace the contents of a list item
        // Instead of creating new views, the list tries to recycle existing ones
        // (invoked by the WearableListView's layout manager)
        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder,
                                     int position) {
            // retrieve the text view
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;
            // replace text contents
            view.setText(mMenu.children[position].title.data);
            // replace list item's metadata
            holder.itemView.setTag(position);
        }

        // Return the size of your dataset
        // (invoked by the WearableListView's layout manager)
        @Override
        public int getItemCount() {
            return mMenu.children.length;
        }
    }

}
