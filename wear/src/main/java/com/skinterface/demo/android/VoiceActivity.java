package com.skinterface.demo.android;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.CircularButton;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import java.io.File;

public class VoiceActivity extends WearableActivity implements View.OnClickListener {

    CircularButton mButton1;
    CircularButton mButton2;
    TextView mPositionView;
    int playback_time;
    int recording_time;
    int recording_size;
    boolean recording_failed;

    SoundRecorder recorder;

    public static void startForResult(int requestCode, Activity context) {
        Intent intent = new Intent(context, VoiceActivity.class);
        context.startActivityForResult(intent, requestCode);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
            case SoundRecorder.ACTION_SOUND_PLAY_FAIL:
                onSoundPlayStop();
                break;
            case SoundRecorder.ACTION_SOUND_PLAY_STARTED:
                onSoundPlayStart();
                break;
            case SoundRecorder.ACTION_SOUND_PLAY_PROGRESS:
                onSoundPlayProgress(
                        intent.getIntExtra(SoundRecorder.EXTRA_PROGRESS, 0),
                        intent.getIntExtra(SoundRecorder.EXTRA_DURATION, 0));
                break;
            case SoundRecorder.ACTION_SOUND_PLAY_FINISHED:
                onSoundPlayStop();
                break;
            case SoundRecorder.ACTION_SOUND_REC_FAIL:
                onSoundRecFail();
                break;
            case SoundRecorder.ACTION_SOUND_REC_STARTED:
                onSoundRecStart();
                break;
            case SoundRecorder.ACTION_SOUND_REC_PROGRESS:
                onSoundRecProgress(
                        intent.getIntExtra(SoundRecorder.EXTRA_PROGRESS, 0),
                        intent.getIntExtra(SoundRecorder.EXTRA_FILESIZE, 0));
                break;
            case SoundRecorder.ACTION_SOUND_REC_FINISHED:
                onSoundRecStop(
                        intent.getIntExtra(SoundRecorder.EXTRA_DURATION, 0),
                        intent.getIntExtra(SoundRecorder.EXTRA_FILESIZE, 0));
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_voice);
        mPositionView = (TextView) findViewById(R.id.position);
        mButton1 = (CircularButton) findViewById(android.R.id.button1);
        mButton2 = (CircularButton) findViewById(android.R.id.button2);
        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);

        recorder = new SoundRecorder(this, WearActivity.VOICE_FILE_NAME);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SoundRecorder.ACTION_SOUND_PLAY_FAIL);
        intentFilter.addAction(SoundRecorder.ACTION_SOUND_PLAY_STARTED);
        intentFilter.addAction(SoundRecorder.ACTION_SOUND_PLAY_PROGRESS);
        intentFilter.addAction(SoundRecorder.ACTION_SOUND_PLAY_FINISHED);
        intentFilter.addAction(SoundRecorder.ACTION_SOUND_REC_FAIL);
        intentFilter.addAction(SoundRecorder.ACTION_SOUND_REC_STARTED);
        intentFilter.addAction(SoundRecorder.ACTION_SOUND_REC_PROGRESS);
        intentFilter.addAction(SoundRecorder.ACTION_SOUND_REC_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        updatePosView();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int res = checkSelfPermission(Manifest.permission.RECORD_AUDIO);
            if (res != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode != RESULT_OK) {
            setResult(RESULT_CANCELED, new Intent());
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onPause();
        recorder.startRecording();
    }

    @Override
    protected void onPause() {
        super.onPause();
        recorder.cleanup();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (recorder == null || recording_failed) {
            setResult(RESULT_CANCELED, new Intent());
            finish();
            return;
        }
        switch (recorder.getState()) {
        case IDLE:
            if (id == android.R.id.button1)
                recorder.startPlay();
            else if (id == android.R.id.button2) {
                setResult(RESULT_OK, new Intent());
                finish();
            }
            break;
        case RECORDING:
            if (id == android.R.id.button1)
                recorder.stopRecording();
            break;
        case PLAYING:
            if (id == android.R.id.button1)
                recorder.stopPlaying();
            break;
        }
    }

    private void updatePosView() {
        if (mPositionView == null)
            return;
        if (recorder == null) {
            if (!TextUtils.isEmpty(mPositionView.getText()))
                mPositionView.setText("");
            mButton1.setImageResource(R.drawable.ic_close_black_48dp);
            mButton2.setVisibility(View.INVISIBLE);
            return;
        }
        if (recorder.getState() == SoundRecorder.State.IDLE) {
            if (recording_failed) {
                mPositionView.setText("Recording failed");
                mButton1.setImageResource(R.drawable.ic_close_black_48dp);
                mButton2.setVisibility(View.INVISIBLE);
            }
            else {
                mPositionView.setText("Send\nvoice message?");
                mButton1.setImageResource(R.drawable.ic_play_arrow_black_48dp);
                mButton2.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (recorder.getState() == SoundRecorder.State.RECORDING) {
            String txt = String.format("%1$tM:%1$tS %2$1.2fmb ", (long) recording_time, recording_size / (float) (1024 * 1024));
            mPositionView.setText(txt);
            mButton1.setImageResource(R.drawable.ic_stop_black_48dp);
            mButton2.setVisibility(View.INVISIBLE);
            return;
        }
        if (recorder.getState() == SoundRecorder.State.PLAYING) {
            String txt = String.format("%1$tM:%1$tS / %2$tM:%2$tS ", (long) playback_time, (long) recording_time);
            mPositionView.setText(txt);
            mButton1.setImageResource(R.drawable.ic_stop_black_48dp);
            mButton2.setVisibility(View.INVISIBLE);
            return;
        }
    }

    public void onSoundRecFail() {
        recording_time = 0;
        recording_failed = true;
        String filesDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();

        new File(filesDir, WearActivity.VOICE_FILE_NAME).delete();
        updatePosView();
    }

    public void onSoundRecStart() {
        recording_time = 0;
        recording_size = 0;
        recording_failed = false;
        updatePosView();
    }

    public void onSoundRecProgress(int millis, int size) {
        recording_time = millis;
        recording_size = size;
        updatePosView();
    }

    public void onSoundRecStop(int millis, int size) {
        recording_time = millis;
        recording_size = size;
        updatePosView();
    }

    public void onSoundPlayStart() {
        playback_time = 0;
        updatePosView();
    }

    public void onSoundPlayProgress(int millis, int total) {
        playback_time = millis;
        updatePosView();
    }

    public void onSoundPlayStop() {
        updatePosView();
    }

}
