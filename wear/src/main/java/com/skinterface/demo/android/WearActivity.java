package com.skinterface.demo.android;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

public class WearActivity extends WearableActivity implements View.OnClickListener {
    public static final String TAG = "SkinterWatch";

    Handler handler = new Handler();

    private ViewGroup mContainerView;
    //private DismissOverlayView mDismissOverlay;
    private TextView mTitleView;
    private GestureDetector mDetector;

    private Set<Node> mVoiceNodes;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setAmbientEnabled();

        mContainerView = (ViewGroup) findViewById(R.id.container);

        //mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        //mDismissOverlay.setIntroText(R.string.long_press_intro);
        //mDismissOverlay.showIntroIfNecessary();

        mTitleView = (TextView) findViewById(R.id.title);
        mTitleView.setOnClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);
                        Wearable.CapabilityApi.getCapability(mGoogleApiClient, "rsvp_voice",
                                CapabilityApi.FILTER_REACHABLE)
                                .setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                                    @Override
                                    public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                                        mVoiceNodes = getCapabilityResult.getCapability().getNodes();
                                    }
                                });
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API) // Request access only to the Wearable API
                .build();
        mGoogleApiClient.connect();
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, new CapabilityApi.CapabilityListener() {
            @Override
            public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                mVoiceNodes = capabilityInfo.getNodes();
            }
        }, "rsvp_voice");

        mDetector = new GestureDetector(this, getRsvpFragment(), handler);
        mDetector.setOnDoubleTapListener(getRsvpFragment());

        onNewIntent(getIntent());
    }

    public void startCardsActivity() {
        Intent intent = new Intent(this, CardsActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyDown: " + keyCode + " : " + event);
        return super.onKeyDown(keyCode, event);
    }

    public RsvpFragment getRsvpFragment() {
        return (RsvpFragment)getFragmentManager().findFragmentById(R.id.fr_rsvp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean isCancel = event.getActionMasked() == MotionEvent.ACTION_CANCEL;
        boolean isUp = event.getActionMasked() == MotionEvent.ACTION_UP;
        boolean handled = mDetector.onTouchEvent(event);
        if (isCancel)
            getRsvpFragment().onUpOrCancel(true);
        else if (!handled && isUp)
            getRsvpFragment().onUpOrCancel(false);
        return super.onTouchEvent(event);
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
    public void mergeChatMessage(JSONObject jmsg) {
        boolean own = jmsg.optBoolean("own");
        String partner = own ? jmsg.optString("receiver") : jmsg.optString("sender");;
        if (partner == null || partner.isEmpty())
            return;
        for (SSect sect : SectionsModel.instance.getSections()) {
            if ("chat".equals(sect.entity.media) && partner.equals(sect.entity.name)) {
                mergeChatMessage(jmsg, sect);
                return;
            }
        }
        SSect chat = new SSect();
        chat.entity.media = "chat";
        chat.entity.name = partner;
        chat.title = new SEntity();
        chat.title.media = "text";
        chat.title.data = partner;
        chat.hasChildren = true;
        chat.children = new SSect[0];
        SectionsModel.instance.addSection(chat);
        mergeChatMessage(jmsg, chat);
    }

    public void mergeChatMessage(JSONObject jmsg, SSect chat) {
        long id = jmsg.optLong("id");
        boolean own = jmsg.optBoolean("own");
        long timestamp = jmsg.optLong("timestamp");
        String receiver = jmsg.optString("receiver");
        String sender = jmsg.optString("sender");
        String status = jmsg.optString("status");
        String text = jmsg.optString("text");
        // find this message in the chat
        SSect msg = null;
        if (chat.children != null) {
            for (SSect old : chat.children) {
                if (old.chatId == id) {
                    msg = old;
                    break;
                }
            }
        } else {
            chat.children = new SSect[0];
        }
        if (msg == null) {
            msg = new SSect();
            msg.entity.media = "chat-text-msg";
            msg.entity.role = own ? "sent" : "recv";
            msg.entity.name = status;
            msg.title = new SEntity();
            msg.title.media = "text";
            msg.title.data = text;
            msg.chatId = id;
            msg.chatTimestamp = timestamp;
            msg.padd("sender", sender);
            msg.padd("receiver", receiver);
            int len = chat.children.length;
            SSect[] arr = Arrays.copyOf(chat.children, len+1);
            arr[len] = msg;
            Arrays.sort(arr, new Comparator<SSect>() {
                @Override
                public int compare(SSect msg1, SSect msg2) {
                    if (msg1.chatTimestamp != msg2.chatTimestamp)
                        return Long.compare(msg1.chatTimestamp, msg2.chatTimestamp);
                    return Long.compare(msg1.chatId, msg2.chatId);
                }
            });
            chat.children = arr;
        } else {
            msg.entity.name = status;
            if (text != null && !text.isEmpty())
                msg.title.data = text;
        }
        getRsvpFragment().play(chat);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (intent.hasExtra("RSVP_MESSAGE")) {
            try {
                JSONObject jobj = new JSONObject(intent.getStringExtra("RSVP_MESSAGE"));
                String action = jobj.optString("action");
                if ("sect".equals(action)) {
                    SSect sect = SSect.fromJson(jobj);
                    if (sect != null) {
                        SectionsModel.instance.addSection(sect);
                        playCurrentSect();
                    }
                }
                else if ("stop".equals(action)) {
                    stopCurrentSect();
                }
                else if ("chat".equals(action)) {
                    mergeChatMessage(jobj);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Bad json request", e);
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
    protected void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int i=0; i < permissions.length; i++) {
            Log.i(TAG, "Permissuion request for "+permissions[i]+" result is "+grantResults[i]);
        }
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
            mTitleView.setTextColor(getResources().getColor(android.R.color.white));

//            mClockView.setVisibility(View.VISIBLE);
//            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTitleView.setTextColor(getResources().getColor(android.R.color.black));
//            mClockView.setVisibility(View.GONE);
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

    private String pickBestNodeId() {
        Set<Node> nodes = mVoiceNodes;
        if (nodes == null)
            return null;
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby())
                return node.getId();
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    // TODO: should use DataApi + Asset
    public void sendVoice(String fname) {
        String nodeId = pickBestNodeId();
        if (nodeId == null)
            return;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[16*1024];
        FileInputStream in = null;
        try {
            in = openFileInput(fname);
            int read;
            while ((read = in.read(buffer, 0, buffer.length)) > 0)
                bout.write(buffer, 0, read);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read the sound file into a byte array", e);
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) { /* ignore */}
        }
        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "/voice", bout.toByteArray());
    }

}
