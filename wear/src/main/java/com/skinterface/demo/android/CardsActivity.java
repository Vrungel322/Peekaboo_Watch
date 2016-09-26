package com.skinterface.demo.android;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.GridViewPager;
import android.support.wearable.view.WatchViewStub;
import android.widget.TextView;

public class CardsActivity extends Activity {

    public static final String TAG = "SkinterWatch";

    private TextView mTextView;
    private TextView mClockView;
    private GridViewPager mPager;
    private SectionsPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cards);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                mTextView = (TextView) stub.findViewById(R.id.clock);
                mPager = (GridViewPager) findViewById(R.id.pager);
                mPagerAdapter = new SectionsPagerAdapter(getFragmentManager(), SectionsModel.instance);
                mPager.setAdapter(mPagerAdapter);
            }
        });
    }
}
