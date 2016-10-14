package com.skinterface.demo.android;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WearActivity extends WearableActivity implements
        View.OnClickListener,
        ChatNavigator.Client,
        SiteNavigator.Client
{
    public static final String TAG = "SkinterWatch";

    public static final String VOICE_FILE_NAME = "voice.raw";

    static final int MENU_REQUEST_CODE      = 1000;
    static final int SPEECH_REQUEST_CODE    = 1001;
    static final int AUDIO_REQUEST_CODE     = 1002;
    static final int NUMERIC_REQUEST_CODE   = 1003;

    static final int RSVP_SPEED = 1;

    final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case RSVP_SPEED:
                getRsvpFragment().accelerate(msg.arg1);
                return;
            }
        }
    };

    private ViewGroup mContainerView;
    private TextView mTitleView;
    private GestureDetector mDetector;

    private Set<Node> mVoiceNodes;
    private GoogleApiClient mGoogleApiClient;

    Navigator nav = RootNavigator.get();

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_main);
//        setAmbientEnabled();

        mContainerView = (ViewGroup) findViewById(R.id.container);

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

        if (saved != null && saved.containsKey("navigator")) {
            Bundle b = saved.getBundle("navigator");
            String clazz = b.getString("class");
            if ("SiteNavigator".equals(clazz))
                nav = new SiteNavigator(b);
            else if ("ChatNavigator".equals(clazz))
                nav = new ChatNavigator(b);
        }

        onNewIntent(getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (nav instanceof ChatNavigator) {
            Bundle b = new Bundle();
            b.putString("class", "ChatNavigator");
            nav.onSaveInstanceState(b);
            outState.putBundle("navigator", b);
        }
        else if (nav instanceof SiteNavigator) {
            Bundle b = new Bundle();
            b.putString("class", "SiteNavigator");
            nav.onSaveInstanceState(b);
            outState.putBundle("navigator", b);
        }
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyUp: " + keyCode + " : " + event);
        if (keyCode == KeyEvent.KEYCODE_STEM_2) {
            nav.doShowMenu(this);
        }
        return super.onKeyUp(keyCode, event);
    }

    public RsvpFragment getRsvpFragment() {
        return (RsvpFragment)getFragmentManager().findFragmentById(R.id.fr_rsvp);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: "+event);
        boolean isCancel = event.getActionMasked() == MotionEvent.ACTION_CANCEL;
        boolean isUp = event.getActionMasked() == MotionEvent.ACTION_UP;
        boolean handled = mDetector.onTouchEvent(event);
        if (isCancel)
            getRsvpFragment().onUpOrCancel(event, true);
        else if (!handled && isUp)
            getRsvpFragment().onUpOrCancel(event, false);
        return handled || super.onTouchEvent(event);
    }

    public void mergeChatMessage(JSONObject jmsg) {
        try {
            SSect msg = ChatNavigator.mergeChatMessage(jmsg);
            if (msg != null)
                getRsvpFragment().update();
        } catch (JSONException e) {
            Log.e(TAG, "Bad chat message", e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

//        if (intent.hasExtra("RSVP_MESSAGE")) {
//            try {
//                JSONObject jobj = new JSONObject(intent.getStringExtra("RSVP_MESSAGE"));
//                String action = jobj.optString("action");
//                if ("sect".equals(action)) {
//                    SSect sect = SSect.fromJson(jobj);
//                    if (sect != null) {
//                        if (nav instanceof UpStarsSectionsModel) {
//                            ((UpStarsSectionsModel) model).enterRoom(sect);
//                            playCurrentSect();
//                        }
//                    }
//                }
//                else if ("menu".equals(action)) {
//                    model = UpStarsSectionsModel.get();
//                    getRsvpFragment().load(UpStarsSectionsModel.get().currArticle(), true);
//                }
//                else if ("stop".equals(action)) {
//                    stopCurrentSect();
//                }
//            } catch (JSONException e) {
//                Log.e(TAG, "Bad json request", e);
//            }
//        }
        if (intent.hasExtra("CHAT_MESSAGE")) {
            try {
                JSONObject jobj = new JSONObject(intent.getStringExtra("CHAT_MESSAGE"));
                String action = jobj.optString("action");
                if ("chat".equals(action)) {
                    mergeChatMessage(jobj);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Bad json request", e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MENU_REQUEST_CODE) {
            if (resultCode == RESULT_OK)
                exitMenu(SSect.fromJson(data.getStringExtra(MenuActivity.RESULT_EXTRA)));
        }
        else if (requestCode == SPEECH_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty())
                    nav.doUserInput(this, results.get(0));
            }
        }
        else if (requestCode == AUDIO_REQUEST_CODE) {
            if (resultCode == RESULT_OK && nav instanceof ChatNavigator) {
                String sender = ((ChatNavigator)nav).getUserId();
                String receiver = ((ChatNavigator)nav).getPartnerId();
                sendVoiceConfirm(sender, receiver);
            }
        }
        else if (requestCode == NUMERIC_REQUEST_CODE) {
            if (resultCode == RESULT_OK)
                nav.doUserInput(this, data.getStringExtra("text"));
        }
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getRsvpFragment().stop(false);
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
        getRsvpFragment().stop(true);
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
        if (id == R.id.title) {
            //startActivityForResult(new Intent(this,InputActivity.class),NUMERIC_REQUEST_CODE);
            nav.doShowMenu(this);
        }
        if (id == R.id.text || id == R.id.clock)
            startCardsActivity();
    }

    public void sendChatConfirm(final String sender, final String receiver, final String text) {
        new AsyncTask<Void,Void,Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    String nodeId = pickBestNodeId();
                    if (nodeId == null)
                        return Boolean.FALSE;
                    long timestamp = System.currentTimeMillis();
                    String post = new Uri.Builder()
                            .path(IOUtils.CHAT_POST_PATH)
                            .toString();
                    JSONObject json = new JSONObject();
                    json.put("timestamp", timestamp);
                    json.put("receiver", receiver);
                    json.put("sender", sender);
                    json.put("text", text);
                    byte[] data = json.toString().getBytes(IOUtils.UTF8);
                    MessageApi.SendMessageResult messageResult = Wearable.MessageApi
                            .sendMessage(mGoogleApiClient, nodeId, post, data).await();
                    if (!messageResult.getStatus().isSuccess())
                        return Boolean.FALSE;
                    return Boolean.TRUE;
                } catch (Exception e) {
                    Log.e(TAG, "Error during audio message post", e);
                    return Boolean.FALSE;
                }
            }
        }.execute();
    }

    public void sendVoiceConfirm(final String sender, final String receiver) {
        new AsyncTask<Void,Void,Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    String nodeId = pickBestNodeId();
                    if (nodeId == null)
                        return Boolean.FALSE;
                    long timestamp = System.currentTimeMillis();
                    String path = "/voice/" + VOICE_FILE_NAME;
                    ChannelApi.OpenChannelResult channelResult = Wearable.ChannelApi
                            .openChannel(mGoogleApiClient, nodeId, path)
                            .await(1000, TimeUnit.MILLISECONDS);
                    if (!channelResult.getStatus().isSuccess())
                        return Boolean.FALSE;
                    Uri uri = Uri.fromFile(new File(getFilesDir(), VOICE_FILE_NAME));
                    if (!channelResult.getChannel().sendFile(mGoogleApiClient, uri).await().isSuccess())
                        return Boolean.FALSE;
                    String post = new Uri.Builder()
                            .path(IOUtils.CHAT_POST_PATH+path)
                            .appendQueryParameter("fmt", "PCM16")
                            .appendQueryParameter("rate", "" + SoundRecorder.RECORDING_RATE)
                            .toString();
                    JSONObject json = new JSONObject();
                    json.put("timestamp", timestamp);
                    json.put("receiver", receiver);
                    json.put("sender", sender);
                    byte[] data = json.toString().getBytes(IOUtils.UTF8);
                    MessageApi.SendMessageResult messageResult = Wearable.MessageApi
                            .sendMessage(mGoogleApiClient, nodeId, post, data).await();
                    if (!messageResult.getStatus().isSuccess())
                        return Boolean.FALSE;
                    return Boolean.TRUE;
                } catch (Exception e) {
                    Log.e(TAG, "Error during audio message post", e);
                    return Boolean.FALSE;
                }
            }
        }.execute();
    }

    public void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    public void startVoiceRecording() {
        VoiceActivity.startForResult(AUDIO_REQUEST_CODE, this);
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

    protected void exitMenu(final SSect menu) {
        if (menu == null)
            return;
        if ("upstars".equals(menu.entity.data)) {
            nav = new SiteNavigator(null);
            mTitleView.setText("UpStars");
            getRsvpFragment().load(null, 0, false);
            nav.doHello(this);
        }
        else if ("peekaboo".equals(menu.entity.data)) {
            nav = new ChatNavigator(null);
            mTitleView.setText("Peekaboo");
            getRsvpFragment().load(null, 0, false);
            nav.doHello(this);
        }
        else if ("show".equals(menu.entity.data)) {
            makeActionHandler(nav, menu.toAction()).run();
        }
        else if ("place-selected".equals(menu.entity.data)) {
            String placeId = menu.entity.val("placeId");
            Action action = new Action("place-details")
                    .add("placeId", placeId)
                    .add("description", menu.title.data);
            sendServerCmd(nav, action, new SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    ((SiteNavigator)nav).doGeolocationDetails(WearActivity.this, result);
                }
            });
        }
        else if ("chat".equals(menu.entity.data)) {
            String id = menu.entity.val("room");
            if (nav instanceof ChatNavigator) {
                SSect chat = ((ChatNavigator)nav).doEnterToRoom(this, id);
                if (chat != null) {
                    mTitleView.setText(chat.title.data);
                    //getRsvpFragment().load(chat, RsvpFragment.FN1_EDIT|RsvpFragment.IS_CHAT, false);
                    //getRsvpFragment().toChild(chat.children.length, false);
                }
            }
        }
    }

    @Override
    public void showMenu(Navigator nav, final SSect menu) {
        getRsvpFragment().stop(false);
        if (menu == null)
            return;
        if (menu.children != null && menu.children.length > 0)
            MenuActivity.startForResult(MENU_REQUEST_CODE, this, menu);
        else if (menu.entity.data != null)
            exitMenu(menu);
    }

    @Override
    public ActionHandler makeActionHandler(Navigator nav, Action action) {
        if (nav instanceof SiteNavigator)
            return new SiteActionHandler((SiteNavigator)nav, this, action);
        if (nav instanceof ChatNavigator)
            return new ChatActionHandler((ChatNavigator)nav, this, action);
        throw new UnsupportedOperationException("Unknown navigator: "+nav.getClass());
    }

    private int flagsNavToRsvp(int nav_flags) {
        int flags = 0;
        if ((nav_flags & Navigator.FLAG_CHAT) != 0)
            flags |= RsvpFragment.NAV_CHAT;
        else if ((nav_flags & Navigator.FLAG_SITE) != 0)
            flags |= RsvpFragment.NAV_SITE;
        return flags;
    }

    @Override
    public void enterToRoom(Navigator nav, SSect sect, int flags) {
        if (nav instanceof ChatNavigator) {
            getRsvpFragment().load(sect, flagsNavToRsvp(flags), false);
            if (sect.currListPosition == 0 && sect.children != null && (flags & Navigator.FLAG_CHAT) != 0)
                getRsvpFragment().toChild(sect.children.length, false);
        } else {
            getRsvpFragment().load(sect, flagsNavToRsvp(flags), true);
        }
    }

    @Override
    public void returnToRoom(Navigator nav, SSect sect, int flags) {
        getRsvpFragment().load(sect, flagsNavToRsvp(flags), false);
        getRsvpFragment().toChild(sect.currListPosition, false);
    }

    @Override
    public void showWhereAmIData(Navigator nav, SSect sect, int flags) {
        getRsvpFragment().load(sect, flagsNavToRsvp(flags), true);
    }

    @Override
    public boolean isStory() {
        return true;
    }

    @Override
    public void updateActions(Navigator nav, List<UIAction> actions) {

    }

    public void attachToSite(SiteNavigator nav, SSect menu) {
        makeActionHandler(nav, new Action("home")).run();
    }

    @Override
    public void sendServerCmd(Navigator nav, Action action, final SrvCallback callback) {
        if (nav instanceof SiteNavigator)
            siteServerCmd((SiteNavigator)nav, action, callback);
        else if (nav instanceof ChatNavigator)
            chatServerCmd((ChatNavigator)nav, action, callback);
        else
            throw new UnsupportedOperationException("Unknown navigator: "+nav.getClass());
    }

    private void chatServerCmd(ChatNavigator nav, Action action, final SrvCallback callback) {
        Uri.Builder uri = new Uri.Builder().path(IOUtils.CHAT_ACTION_PATH+action.getAction());
        if (action.params != null) {
            for (Map.Entry<String, String> e : action.params.entrySet()) {
                if (e.getValue() != null)
                    uri.appendQueryParameter(e.getKey(), e.getValue());
                else
                    uri.appendQueryParameter(e.getKey(), "");
            }
        }
        String nodeId = pickBestNodeId();
        if (nodeId != null) {
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, nodeId, uri.toString(), new byte[0]);
            if (callback != null)
                result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (sendMessageResult.getStatus().isSuccess())
                                RsvpMessageService.addPendingRequest(sendMessageResult.getRequestId(), callback);
                        }
                    });
        }
    }

    private void siteServerCmd(SiteNavigator nav, Action action, final SrvCallback callback) {
        String nodeId = pickBestNodeId();
        if (nodeId != null) {
            PendingResult<MessageApi.SendMessageResult> result;
            if ("places-autocomplete".equals(action.getAction()) || "place-details".equals(action.getAction())) {
                Uri.Builder uri = new Uri.Builder().path(IOUtils.HELP_ACTION_PATH+action.getAction());
                if (action.params != null) {
                    for (Map.Entry<String, String> e : action.params.entrySet()) {
                        if (e.getValue() != null)
                            uri.appendQueryParameter(e.getKey(), e.getValue());
                        else
                            uri.appendQueryParameter(e.getKey(), "");
                    }
                }
                result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, nodeId, uri.toString(), new byte[0]);
            } else {
                final  String reqData = action.serializeToCmd(nav.getSessionID(), 0).toString();
                result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, nodeId, IOUtils.RSVP_ACTION_PATH, reqData.getBytes(IOUtils.UTF8));
            }
            if (callback != null)
                result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (sendMessageResult.getStatus().isSuccess())
                                RsvpMessageService.addPendingRequest(sendMessageResult.getRequestId(), callback);
                        }
                    });
        }
        else
        {
            final  String reqData = action.serializeToCmd(nav.getSessionID(), 0).toString();
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    InputStream is = null;
                    try {
                        URL url = new URL(IOUtils.UpStars_JSON_URL);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(5000);
                        conn.setConnectTimeout(5000);
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(reqData.getBytes(IOUtils.UTF8));
                        os.close();
                        // Starts the query
                        conn.connect();
                        int response = conn.getResponseCode();
                        Log.d(TAG, "The response is: " + conn.getResponseMessage());
                        if (response == 200) {
                            StringBuilder sb = new StringBuilder();
                            is = conn.getInputStream();
                            InputStreamReader reader = new InputStreamReader(is, IOUtils.UTF8);
                            char[] buffer = new char[4096];
                            int sz;
                            while ((sz = reader.read(buffer)) > 0)
                                sb.append(buffer, 0, sz);
                            reader.close();
                            return sb.toString();
                        } else {
                            cancel(false);
                            return null;
                        }
                    } catch (Throwable e) {
                        Log.e(TAG, "Server connection error", e);
                        cancel(false);
                        return null;
                    } finally {
                        IOUtils.safeClose(is);
                    }
                }

                @Override
                protected void onPostExecute(String result) {
                    if (!isCancelled() && callback != null)
                        callback.onSuccess(result);
                }
            }.execute();
        }
    }

    protected class SiteActionHandler extends SiteNavigator.BaseActionHandler {
        protected SiteActionHandler(SiteNavigator nav, SiteNavigator.Client client, Action action) {
            super(nav, client, action);
        }
        public void run() {
            String act = action.getAction();
            if ("edit".equals(act)) {
                SSect val = nav.currArticle();
                if (val == null)
                    return;
                if (val.getCurrChild() != null)
                    val = val.getCurrChild();
                if (!val.isValue)
                    return;
                String str = val.entity.data;
                if ("int".equals(val.entity.media))
                    startActivityForResult(
                            new Intent(WearActivity.this,InputActivity.class).putExtra(val.entity.media, str),
                            NUMERIC_REQUEST_CODE);
                else if ("real".equals(val.entity.media))
                    startActivityForResult(
                            new Intent(WearActivity.this,InputActivity.class).putExtra(val.entity.media, str),
                            NUMERIC_REQUEST_CODE);
                else if ("date".equals(val.entity.media) || "datetime".equals(val.entity.media))
                    startActivityForResult(
                            new Intent(WearActivity.this,InputActivity.class).putExtra(val.entity.media, str),
                            NUMERIC_REQUEST_CODE);
                else if ("geolocation".equals(val.entity.media)) {
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    startActivityForResult(intent, SPEECH_REQUEST_CODE);
                }
                return;
            }
            super.run();
        }
    }
    protected class ChatActionHandler extends ActionHandler {
        final ChatNavigator nav;
        final ChatNavigator.Client client;
        protected ChatActionHandler(ChatNavigator nav, ChatNavigator.Client client, Action action) {
            super(nav, client, action);
            this.nav = nav;
            this.client = client;
        }
        public void run() {
            String act = action.getAction();
            if ("show-menu".equals(act)) {
                nav.doShowMenu(client);
            }
            else if ("edit".equals(act)) {
                startVoiceRecognition();
            }
            else if ("send".equals(act)) {
                nav.composeNewChatMessageResult(client, true);
            }
            else if ("close".equals(act)) {
                nav.composeNewChatMessageResult(client, false);
            }

        }
    }
}
