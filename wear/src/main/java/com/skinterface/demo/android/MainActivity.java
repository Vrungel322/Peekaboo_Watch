package com.skinterface.demo.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{

    public static final String TAG = "SkinterWatch";

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);

    private BoxInsetLayout mContainerView;
    private RsvpView mRsvpView;
    private TextView mTextView;
    private TextView mClockView;

    GoogleApiClient mGoogleApiClient;
    private boolean connected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mRsvpView = (RsvpView) findViewById(R.id.rsvp);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                // Request access only to the Wearable API
                .addApiIfAvailable(Wearable.API)
                .build();
        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent.hasExtra("RSVP_DATA")) {
            SEntity entity = new SEntity();
            entity.data = intent.getStringExtra("RSVP_DATA");
            if (entity.data == null || entity.data.length() == 0) {
                RsvpWords words = new RsvpWords();
                mRsvpView.play(words);
            } else {
                RsvpWords words = new RsvpWords();
                words.addTitleWords(entity);
                mRsvpView.play(words);
            }
        } else {
            SEntity entity = new SEntity();
            entity.data =
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
            RsvpWords words = new RsvpWords();
            words.addTitleWords(entity);
            mRsvpView.play(words);
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
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
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
        // Now you can use the Data Layer API
        connected = true;
    }
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
        connected = false;
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);
        connected = false;
    }

}
