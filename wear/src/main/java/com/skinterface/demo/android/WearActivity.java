package com.skinterface.demo.android;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class WearActivity extends WearableActivity implements
        View.OnClickListener,
        ChatNavigator.SrvClient,
        SiteNavigator.SrvClient
{
    public static final String TAG = "SkinterWatch";

    public static final String VOICE_FILE_NAME = "voice.raw";

    static final int MENU_REQUEST_CODE      = 1000;
    static final int SPEECH_REQUEST_CODE    = 1001;
    static final int AUDIO_REQUEST_CODE     = 1002;

    final Handler handler = new Handler(Looper.getMainLooper());

    private ViewGroup mContainerView;
    private TextView mTitleView;
    private GestureDetector mDetector;
    private SectionsModel model;

    private Set<Node> mVoiceNodes;
    private GoogleApiClient mGoogleApiClient;

    Navigator nav = new SiteNavigator(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyUp: " + keyCode + " : " + event);
        if (keyCode == KeyEvent.KEYCODE_STEM_2) {
            nav.doShowMenu();
        }
        return super.onKeyUp(keyCode, event);
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
        SSect sect = model.currArticle();
        if (sect == null)
            return;
        RsvpFragment fr = getRsvpFragment();
        if (fr != null)
            fr.load(sect, true);
    }
    public void stopCurrentSect() {
        RsvpFragment fr = getRsvpFragment();
        if (fr != null)
            fr.stop();
    }
    public void mergeChatMessage(JSONObject jmsg) {
        ChatSectionsModel.get().mergeChatMessage(jmsg);
    }

    public void addChatMessage(String text) {
        if (model instanceof ChatSectionsModel)
            ((ChatSectionsModel) model).addChatMessage(text);
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
                        if (model instanceof UpStarsSectionsModel) {
                            ((UpStarsSectionsModel) model).enterRoom(sect);
                            playCurrentSect();
                        }
                    }
                }
                else if ("menu".equals(action)) {
                    model = UpStarsSectionsModel.get();
                    getRsvpFragment().load(UpStarsSectionsModel.get().currArticle(), true);
                }
                else if ("stop".equals(action)) {
                    stopCurrentSect();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Bad json request", e);
            }
        }
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
                    addChatMessage(results.get(0));
            }
        }
        else if (requestCode == AUDIO_REQUEST_CODE) {
            if (resultCode == RESULT_OK)
                sendVoiceConfirm();
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
        if (id == R.id.title) {
            nav.doShowMenu();
        }
        if (id == R.id.text || id == R.id.clock)
            startCardsActivity();
    }

    public void sendVoiceConfirm() {
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
                    json.put("receiver", null);
                    json.put("sender", null);
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
            nav = new SiteNavigator(this);
            model = UpStarsSectionsModel.get();
            mTitleView.setText("UpStars");
            getRsvpFragment().load(null, false);
            nav.doHello();
        }
        else if ("peekaboo".equals(menu.entity.data)) {
            nav = new ChatNavigator(this);
            model = ChatSectionsModel.get();
            mTitleView.setText("Peekaboo");
            getRsvpFragment().load(null, false);
            nav.doHello();
        }
        else if ("show".equals(menu.entity.data)) {
            if (nav instanceof SiteNavigator)
                ((SiteNavigator)nav).makeActionHandler(menu.entity).run();
        }
        else if ("chat".equals(menu.entity.data)) {
            String id = menu.entity.val("room");
            SSect chat = ChatSectionsModel.get().setChatRoom(id);
            if (chat != null) {
                mTitleView.setText(chat.title.data);
                chatServerCmd(new Action("list-messages").add("id", id), null, null);
                getRsvpFragment().load_to_child(chat, chat.children.length, false);
            }
        }
    }

    @Override
    public void showMenu(final SSect menu) {
        stopCurrentSect();
        if (menu == null)
            return;
        if (menu.children != null && menu.children.length > 0)
            MenuActivity.startForResult(MENU_REQUEST_CODE, this, menu);
        else if (menu.entity.data != null)
            exitMenu(menu);
    }

    @Override
    public SiteNavigator.ActionHandler makeActionHandler(SiteNavigator nav, Action action) {
        return new ActionHandler(nav, this, action);
    }

    @Override
    public void enterToRoom(SSect sect) {
        UpStarsSectionsModel.get().enterRoom(sect);
        getRsvpFragment().load(sect, true);
    }

    @Override
    public void returnToRoom(SSect sect) {
        UpStarsSectionsModel.get().enterRoom(sect);
        getRsvpFragment().load(sect, true);
    }

    @Override
    public void showWhereAmIData(SSect sect) {
        UpStarsSectionsModel.get().enterRoom(sect);
        getRsvpFragment().load(sect, true);
    }

    public void attachToSite(SSect menu) {
        model = UpStarsSectionsModel.get();
        ((SiteNavigator)nav).executeAction(new Action("home"));
    }

    public void attachToChat(JSONObject jobj) {
        model = ChatSectionsModel.get();
        ChatSectionsModel.get().setAttachInfo(jobj);
        nav.doShowMenu();
    }

    @Override
    public void chatServerCmd(Action action, ChatNavigator nav, final SrvCallback callback) {
        Uri.Builder uri = new Uri.Builder().path(IOUtils.CHAT_ACTION_PATH+action.getAction());
        if (action.params != null) {
            for (Map.Entry<String, String> e : action.params.entrySet()) {
                if (e.getValue() != null)
                    uri.appendQueryParameter(e.getKey(), e.getValue());
                else
                    uri.appendQueryParameter(e.getKey(), "");
            }
        }
        String reqData = "";
        String nodeId = pickBestNodeId();
        if (nodeId != null) {
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, nodeId, uri.toString(), reqData.getBytes(IOUtils.UTF8));
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

    @Override
    public void siteServerCmd(Action action, SiteNavigator nav, final SrvCallback callback) {
        final  String reqData = action.serializeToCmd(nav.sessionID, 0).toString();
        String nodeId = pickBestNodeId();
        if (nodeId != null) {
            PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, nodeId, IOUtils.RSVP_ACTION_PATH, reqData.getBytes(IOUtils.UTF8));
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

    protected class ActionHandler extends SiteNavigator.BaseActionHandler {
        protected ActionHandler(SiteNavigator nav, SiteNavigator.SrvClient client, Action action) {
            super(nav, client, action);
        }
        public void run() {
            String act = action.getAction();
            if ("list".equals(act)) {
            }
            else if ("descr".equals(act)) {
            }
            else if ("read".equals(act)) {
            }
            else {
                super.run();
            }
        }
    }
}
