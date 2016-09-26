package com.skinterface.demo.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.GridViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WearActivity extends WearableActivity implements View.OnClickListener
{
    public static final String TAG = "SkinterWatch";

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private ViewGroup mContainerView;
    private TextView mTextView;
    private TextView mClockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (ViewGroup) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);

        mTextView.setOnClickListener(this);
        mClockView.setOnClickListener(this);

        getFragmentManager().beginTransaction()
                .add(android.R.id.content, new RsvpFragment(), "rsvp")
                .commit();

        onNewIntent(getIntent());
    }

    public void startCardsActivity() {
        Intent intent = new Intent(this, CardsActivity.class);
        startActivity(intent);
    }

    public RsvpFragment getRsvpFragment() {
        return (RsvpFragment)getFragmentManager().findFragmentByTag("rsvp");
    }

    public void playCurrentSect() {
        if (SectionsModel.instance.size() <= 0)
            return;
        SSect sect = SectionsModel.instance.last();
        RsvpFragment fr = getRsvpFragment();
        if (fr != null)
            fr.play(sect);
    }
    public void stopCurrentSect() {
        RsvpFragment fr = getRsvpFragment();
        if (fr != null)
            fr.stop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent.hasExtra("RSVP_SECT")) {
            SSect sect = SSect.fromJson(intent.getStringExtra("RSVP_SECT"));
            if (sect != null) {
                SectionsModel.instance.addSection(sect);
                playCurrentSect();
            }
        } else {
            SSect sect = new SSect();
            sect.title = new SEntity();
            sect.title.media = "text";
            sect.title.data = "9th Day";
            sect.hasArticle = true;
            sect.entity.media = "text";
            sect.entity.data =
                    "      Best action: attention. The hardest day of the Moon cycle. Best you can do on\n" +
                            "      the day is carrying the trash out, mend holes and don’t get into anything.\n" +
                            "      The day is not purposed for divination. The dark movement of Hecate favors\n" +
                            "      leaving, end of useless connections, cutting tails, bad habits. Good for a\n" +
                            "      submission, penance, summing up month results.\n" +
                            "\n" +
                            "      Anatomical match: hands and feet. Keep them safe on the day. Manicure and\n" +
                            "      pedicure done on the day will not stay long.\n" +
                            "\n" +
                            "      People born on the day are long-livers, though sickly, constantly fighting\n" +
                            "      against something, looking for something. Traditional astrology they are\n" +
                            "      considered to be scapegoats for the whole Zodiac, but the practice and\n" +
                            "      modern epoch somewhat correct this information. They would rather themselves\n" +
                            "      with all the work, even at the expense of their health, just to prove they\n" +
                            "      are right and independent.\n";
            SectionsModel.instance.addSection(sect);
            playCurrentSect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        playCurrentSect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCurrentSect();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
        stopCurrentSect();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.text || id == R.id.clock || id == R.id.pager) {
            startCardsActivity();
            return;
        }
    }
}
