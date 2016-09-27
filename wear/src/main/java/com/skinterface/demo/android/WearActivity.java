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
            sect.title.data = "Сайт UpStars";
            sect.descr = new SEntity();
            sect.descr.media = "text";
            sect.descr.data = "Вы находитесь в большом, просторном, круглом зале сайта UpStars. " +
                    "На высоком сводчатом потолке изображено звёздное небо с рисунками созвездий. " +
                    "На стенах зала изображены Знаки Зодиака и Планеты в виде древних богов в " +
                    "декорациях астрологических Домов. На изображениях видны поясняющие надписи.";
            sect.hasArticle = true;
            sect.entity.media = "text";
            sect.entity.data = "Рядом с вами оказалась дежурная звёздочка-Гид. Вы можете обращаться " +
                    "к ней за помощью для быстрого перемещения по этому сайту, или за пояснениями " +
                    "по поводу астрологических терминов." +
                    "\n" +
                    "Недалеко от вас находится стенд с информацией для посетителей сайта, а левее " +
                    "расположен вход в канцелярию, через которую вы сможете связаться с администрацией " +
                    "сайта или заказать составление индивидуального гороскопа.";
            SSect child0 = new SSect();
            child0.title = new SEntity();
            child0.title.media = "text";
            child0.title.data = "О Планетах";
            child0.descr = new SEntity();
            child0.descr.media = "text";
            child0.descr.data = "О Планетах";
            SSect child1 = new SSect();
            child1.title = new SEntity();
            child1.title.media = "text";
            child1.title.data = "Солнце";
            child1.descr = new SEntity();
            child1.descr.media = "text";
            child1.descr.data = "Солнце в астрологии - центральная фигура, квинтэссенция гороскопа.";
            SSect child2 = new SSect();
            child2.title = new SEntity();
            child2.title.media = "text";
            child2.title.data = "Луна";
            child2.descr = new SEntity();
            child2.descr.media = "text";
            child2.descr.data = "Вторая по степени важности фигура - Луна - в астрологии связана с воспринимающим началом в человеке.";
            sect.children = new SSect[]{child0, child1, child2};
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
