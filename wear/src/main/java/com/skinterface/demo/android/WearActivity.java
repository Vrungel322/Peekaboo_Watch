package com.skinterface.demo.android;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class WearActivity extends WearableActivity implements View.OnClickListener {
    public static final String TAG = "SkinterWatch";

    public static final String VOICE_FILE_NAME = "voice.wav";

    static final int MSG_RSVP_PLAY_STARTED  = 1;
    static final int MSG_RSVP_PLAY_FINISHED = 2;

    static final int MSG_SOUND_PLAY_FAIL    = 10;
    static final int MSG_SOUND_PLAY_STARTED = 11;
    static final int MSG_SOUND_PLAY_PROGRESS= 12;
    static final int MSG_SOUND_PLAY_FINISHED= 13;
    static final int MSG_SOUND_REC_FAIL     = 14;
    static final int MSG_SOUND_REC_STARTED  = 15;
    static final int MSG_SOUND_REC_PROGRESS = 16;
    static final int MSG_SOUND_REC_FINISHED = 17;

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_RSVP_PLAY_STARTED:
                getRsvpFragment().onRsvpPlayStart();
                break;
            case MSG_RSVP_PLAY_FINISHED:
                getRsvpFragment().onRsvpPlayStop();
                break;
            case MSG_SOUND_PLAY_STARTED:
                onSoundPlayStart();
                break;
            case MSG_SOUND_PLAY_PROGRESS:
                onSoundPlayProgress(msg.arg1, msg.arg2);
                break;
            case MSG_SOUND_PLAY_FINISHED:
                onSoundPlayStop();
                break;
            case MSG_SOUND_REC_FAIL:
                onSoundRecFail();
                break;
            case MSG_SOUND_REC_STARTED:
                onSoundRecStart();
                break;
            case MSG_SOUND_REC_PROGRESS:
                onSoundRecProgress(msg.arg1, msg.arg2);
                break;
            case MSG_SOUND_REC_FINISHED:
                onSoundRecStop(msg.arg1, msg.arg2);
                break;
            }
        }
    };

    private ViewGroup mContainerView;
    private TextView mTitleView;
    private GestureDetector mDetector;
    private SoundRecorder recorder;
    private SectionsModel model;

    private Set<Node> mVoiceNodes;
    private GoogleApiClient mGoogleApiClient;

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
        if (keyCode == KeyEvent.KEYCODE_STEM_2)
            showMenu(model.getMenu());
        return super.onKeyUp(keyCode, event);
    }

    public RsvpFragment getRsvpFragment() {
        return (RsvpFragment)getFragmentManager().findFragmentById(R.id.fr_rsvp);
    }
    public WearMenuFragment getMenuFragment() {
        return (WearMenuFragment)getFragmentManager().findFragmentByTag("menu");
    }
    public VoiceFragment getVoiceFragment() {
        return (VoiceFragment)getFragmentManager().findFragmentByTag("voice_confirm");
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
        ChatSectionsModel model = ChatSectionsModel.getChatModel(partner);
        model.mergeChatMessage(jmsg);
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
                else if ("menu".equals(action)) {
                    UpStarsSectionsModel.get().setMenu(jobj);
                    model = UpStarsSectionsModel.get();
                    getRsvpFragment().play(UpStarsSectionsModel.get().currArticle());
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
        }
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
        if (id == R.id.title)
            showMenu(model.getMenu());
        if (id == R.id.text || id == R.id.clock)
            startCardsActivity();
    }

    public SoundRecorder getRecorder() {
        return recorder;
    }

    public void sendVoiceAbort() {
        VoiceFragment fr = getVoiceFragment();
        if (fr != null)
            getFragmentManager().beginTransaction().remove(fr).commit();
    }

    public void sendVoiceConfirm(VoiceFragment fr) {
        String nodeId = pickBestNodeId();
        if (nodeId == null) {
            if (fr != null)
                fr.onDataSendFail();
            return;
        }
        // TODO: should use DataApi + Asset
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buffer = new byte[16*1024];
        FileInputStream in = null;
        try {
            in = openFileInput(VOICE_FILE_NAME);
            int read;
            while ((read = in.read(buffer, 0, buffer.length)) > 0)
                bout.write(buffer, 0, read);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read the sound file into a byte array", e);
            if (fr != null)
                fr.onDataSendFail();
            return;
        } finally {
            IOUtils.safeClose(in);
        }
        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "/voice", bout.toByteArray())
                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
            @Override
            public void onResult(@NonNull MessageApi.SendMessageResult result) {
                VoiceFragment fr = getVoiceFragment();
                if (fr != null) {
                    if (result.getStatus().isSuccess())
                        getFragmentManager().beginTransaction().remove(fr).commit();
                    else
                        fr.onDataSendFail();
                }
            }
        });
    }

    public void onSoundRecFail() {
        new File(getFilesDir(), VOICE_FILE_NAME).delete();
        VoiceFragment fr = getVoiceFragment();
        if (fr != null)
            fr.onSoundRecFail();
    }

    public void onSoundRecStart() {
        VoiceFragment fr = getVoiceFragment();
        if (fr != null)
            fr.onSoundRecStart();
    }

    public void onSoundRecProgress(int millis, int size) {
        Log.i(TAG, "recording "+millis+" ms / "+size+" bytes");
        VoiceFragment fr = getVoiceFragment();
        if (fr != null)
            fr.onSoundRecProgress(millis, size);
    }

    public void onSoundRecStop(int millis, int size) {
        VoiceFragment fr = getVoiceFragment();
        if (fr != null)
            fr.onSoundRecStop(millis, size);
    }

    public void onSoundPlayStart() {
        VoiceFragment fr = getVoiceFragment();
        if (fr != null)
            fr.onSoundPlayStart();
    }

    public void onSoundPlayProgress(int millis, int total) {
        Log.i(TAG, "playing "+millis+" ms / "+total+" ms");
        VoiceFragment fr = getVoiceFragment();
        if (fr != null)
            fr.onSoundPlayProgress(millis, total);
    }

    public void onSoundPlayStop() {
        VoiceFragment fr = getVoiceFragment();
        if (fr != null)
            fr.onSoundPlayStop();
    }

    public void startRecordVoice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int res = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
            if (res != PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
        }
        try {
            if (recorder != null) {
                if (recorder.getState() != SoundRecorder.State.IDLE)
                    return;
            } else {
                recorder = new SoundRecorder(this, VOICE_FILE_NAME, this.handler);
            }
            recorder.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Error on voice recording", e);
        }
        VoiceFragment fr = VoiceFragment.create();
        getFragmentManager().beginTransaction().add(R.id.container, fr, "voice_confirm").commit();
    }

    public void stopRecordVoice() {
        try {
            recorder.stopRecording();
        } catch (Exception e) {
            Log.e(TAG, "Error on voice recording", e);
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

    protected void exitMenu(final SSect menu) {
        Fragment fr = getFragmentManager().findFragmentByTag("menu");
        if (fr != null)
            getFragmentManager().beginTransaction().remove(fr).commit();
        if (menu == null)
            return;
        if ("show".equals(menu.entity.data)) {
            String nodeId = pickBestNodeId();
            if (nodeId == null)
                return;
            Action action = Action.create(menu.entity.data);
            if (menu.entity.props != null) {
                for (String key : menu.entity.props.keySet())
                    action.add(key, menu.entity.props.get(key));
            }
            String reqData = action.serializeToCmd(sessionID, 0).toString();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, "/action", reqData.getBytes(IOUtils.UTF8));
        }
        else if ("chat".equals(menu.entity.data)) {
            model = ChatSectionsModel.getChatModel(menu.entity.val("room"));
            getRsvpFragment().stop();
            getRsvpFragment().setSiteTitle(model.currArticle().title.data);
        }
    }

    protected void showMenu(final SSect action) {
        stopCurrentSect();
        if (action == null)
            return;
        if (action.children != null && action.children.length > 0) {
            WearMenuFragment fr = WearMenuFragment.create(wholeMenuTree);
            getFragmentManager().beginTransaction().add(R.id.container, fr, "menu").commit();
        }
        else if (action.entity.data != null) {
            exitMenu(action);
        }
    }


}
