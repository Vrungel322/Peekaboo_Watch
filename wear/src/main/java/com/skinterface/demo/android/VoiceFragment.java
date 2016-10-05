package com.skinterface.demo.android;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.wearable.view.CircularButton;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class VoiceFragment extends Fragment implements View.OnClickListener {

    CircularButton mButton1;
    CircularButton mButton2;
    TextView mPositionView;
    int playback_time;
    int recording_time;
    int recording_size;
    boolean recording_failed;
    boolean sending_failed;

    public static VoiceFragment create() {
        VoiceFragment fr = new VoiceFragment();
//        Bundle args = new Bundle();
//        fr.setArguments(args);
        return fr;
    }

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.voice_confirm, container, false);
        mPositionView = (TextView) view.findViewById(R.id.position);
        mButton1 = (CircularButton) view.findViewById(android.R.id.button1);
        mButton2 = (CircularButton) view.findViewById(android.R.id.button2);
        mButton1.setOnClickListener(this);
        mButton2.setOnClickListener(this);
        updatePosView();

        SwipeDismissLayout dismissLayout = new SwipeDismissLayout(getActivity());
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        dismissLayout.setLayoutParams(lp);
        dismissLayout.addView(view, new ViewGroup.MarginLayoutParams(lp.MATCH_PARENT, lp.MATCH_PARENT));
        dismissLayout.setOnDismissedListener(new SwipeDismissLayout.OnDismissedListener() {
            @Override
            public void onDismissed(SwipeDismissLayout layout) {
                ((WearActivity) getActivity()).sendVoiceAbort();
            }
        });
        dismissLayout.setOnSwipeProgressChangedListener(new SwipeDismissLayout.OnSwipeProgressChangedListener() {
            @Override
            public void onSwipeProgressChanged(SwipeDismissLayout layout, float progress, float translate) {
                getView().setScrollX(-(int)translate);
            }
            @Override
            public void onSwipeCancelled(SwipeDismissLayout layout) {
                getView().setScrollX(0);
            }
        });
        return dismissLayout;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        WearActivity activity = (WearActivity) getActivity();
        SoundRecorder recorder = activity.getRecorder();
        if (recorder == null || recording_failed || sending_failed) {
            activity.sendVoiceAbort();
            return;
        }
        switch (recorder.getState()) {
        case IDLE:
            if (id == android.R.id.button1)
                recorder.startPlay();
            else if (id == android.R.id.button2)
                activity.sendVoiceConfirm(this);
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
        SoundRecorder recorder = ((WearActivity) getActivity()).getRecorder();
        if (recorder == null) {
            if (!TextUtils.isEmpty(mPositionView.getText()))
                mPositionView.setText("");
            mButton1.setImageResource(R.drawable.ic_close_black_48dp);
            mButton2.setVisibility(View.INVISIBLE);
            return;
        }
        if (recorder.getState() == SoundRecorder.State.IDLE) {
            if (sending_failed) {
                mPositionView.setText("Send voice failed");
                mButton1.setImageResource(R.drawable.ic_close_black_48dp);
                mButton2.setVisibility(View.INVISIBLE);
            }
            else if (recording_failed) {
                mPositionView.setText("Recording failed");
                mButton1.setImageResource(R.drawable.ic_close_black_48dp);
                mButton2.setVisibility(View.INVISIBLE);
            }
            else {
                mPositionView.setText("Send voice message?");
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

    public void onDataSendFail() {
        sending_failed = true;
        updatePosView();
    }

}
