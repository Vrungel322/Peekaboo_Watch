package com.skinterface.demo.android;

import android.app.Fragment;
import android.app.FragmentManager;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.wearable.view.CardFragment;
import android.support.wearable.view.CardScrollView;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class SectionsPagerAdapter extends FragmentGridPagerAdapter implements SectionsModel.SectionsListener {

    final SectionsModel model;

    public SectionsPagerAdapter(FragmentManager fm, SectionsModel model) {
        super(fm);
        this.model = model;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        if (observer != null)
            this.model.addSectionsListener(this);
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
        if (observer != null)
            this.model.removeSectionsListener(this);
    }

    @Override
    public void onSectionsChanged(SectionsModel model) {
        notifyDataSetChanged();
    }

    public static class SectFragment extends CardFragment implements View.OnClickListener {
        private boolean expanded;
        public static SectFragment create(CharSequence title, CharSequence text, int iconRes) {
            SectFragment fragment = new SectFragment();
            Bundle args = new Bundle();
            if (title != null)
                args.putCharSequence("CardFragment_title", title);
            if (text != null)
                args.putCharSequence("CardFragment_text", text);
            if(iconRes != 0)
                args.putInt("CardFragment_icon", iconRes);
            fragment.setArguments(args);
            fragment.setCardGravity(Gravity.BOTTOM|Gravity.FILL_HORIZONTAL);
            fragment.setCardMargins(0,0,0,0);
            fragment.setContentPadding(0,0,0,0);
            fragment.setExpansionDirection(EXPAND_DOWN);
            fragment.setExpansionFactor(0.7f);
            return fragment;
        }
        public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
            View view = super.onCreateContentView(inflater, container, saved);
            Bundle args = this.getArguments();
            if (args != null) {
                View title = view.findViewById(android.support.wearable.R.id.title);
                if (!args.containsKey("CardFragment_title") && title != null)
                    title.setVisibility(View.GONE);
            }
            view.setOnClickListener(this);
            return view;
        }

        @Override
        public void onClick(View v) {
            if (expanded) {
                expanded = false;
                scrollToTop();
                setExpansionFactor(0.7f);
            } else {
                expanded = true;
                scrollToTop();
                setExpansionFactor(10.0f);
            }
        }
    }

    @Override
    public Fragment getFragment(int r, int c) {
        SSect sect = model.get(r);
        SectFragment fr = null;
        if (c == 0) {
            String title = "";
            String descr = "";
            if (sect.title != null)
                title = sect.title.data;
            if (sect.descr != null)
                descr = sect.descr.data;
            fr = SectFragment.create(title, descr, 0);
        }
        else if (c == 1) {
            String article = "";
            if (sect.hasArticle)
                article = sect.entity.data;
            fr = SectFragment.create(null, article, 0);
        }
        return fr;
    }

    @Override
    public int getRowCount() {
        return model.size();
    }

    @Override
    public int getColumnCount(int r) {
        SSect sect = model.get(r);
        int rows = 1;
        if (sect.hasArticle)
            rows += 1;
        return rows;
    }
}
