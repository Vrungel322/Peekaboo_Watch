package com.skinterface.demo.android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

public class WearMenuActivity extends WearableActivity implements WearableListView.ClickListener {

    public static final String RESULT_EXTRA = "item";

    SSect wholeMenu;
    TextView titleView;
    WearableListView listView;
    Adapter adapter;

    public static void startForResult(int requestCode, Activity context, SSect menu) {
        Intent intent = new Intent(context, WearMenuActivity.class);
        if (menu != null)
            intent.putExtra("menu", menu.fillJson(new JSONObject()).toString());
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_wear_menu);

        wholeMenu = SSect.fromJson(getIntent().getStringExtra("menu"));
        if (wholeMenu == null)
            wholeMenu = SiteNavigator.chooseModelMenu;

        adapter = new Adapter(this, wholeMenu);
        titleView = (TextView) findViewById(R.id.wear_menu_title);
        titleView.setText(wholeMenu.title.data);
        listView = (WearableListView) findViewById(R.id.wear_menu_list);
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
    }

    @Override
    public void onClick(WearableListView.ViewHolder v) {
        int pos = v.getAdapterPosition();
        SSect sect = adapter.mMenu.children[pos];
        if (sect.children == null || sect.children.length == 0) {
            Intent intent = new Intent();
            intent.putExtra(RESULT_EXTRA, sect.fillJson(new JSONObject()).toString());
            setResult(RESULT_OK, intent);
            finish();
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
            if (wholeMenu == SiteNavigator.chooseModelMenu) {
                setResult(RESULT_CANCELED, new Intent());
                finish();
                return;
            }
            wholeMenu = SiteNavigator.chooseModelMenu;
            sect = wholeMenu;
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

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
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
                textView = (TextView) itemView.findViewById(R.id.name);
            }
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ItemViewHolder(mInflater.inflate(R.layout.menu_item, null));
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            TextView view = itemHolder.textView;
            view.setText(mMenu.children[position].title.data);
            holder.itemView.setTag(position);
        }

        @Override
        public int getItemCount() {
            return mMenu.children.length;
        }
    }

}
