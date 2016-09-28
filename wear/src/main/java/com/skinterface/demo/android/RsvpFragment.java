package com.skinterface.demo.android;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.skinterface.demo.android.WearActivity.TAG;

public class RsvpFragment extends Fragment implements
        RsvpView.RsvpViewListener,
        View.OnClickListener,
        View.OnTouchListener,
        SoundRecorder.OnVoicePlaybackStateChangedListener
{
    private static final int STATE_INIT    = 0;
    private static final int STATE_DONE    = 1;
    private static final int STATE_TITLE   = 2;
    private static final int STATE_ARTICLE = 3;
    private static final int STATE_CHILD   = 4;

    RsvpView mRsvpView;
    TextView tvNextText;

    SSect sect;
    RsvpWords words;
    int state;
    SoundRecorder recorder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.fr_rsvp_round, container, false);
        view.findViewById(R.id.record).setOnTouchListener(this);
        view.findViewById(R.id.next).setOnClickListener(this);
        tvNextText = (TextView) view.findViewById(R.id.next_text);
        mRsvpView = (RsvpView) view.findViewById(R.id.rsvp);
        mRsvpView.setListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (state == STATE_INIT)
            onNext();
    }

    public void play(SSect sect) {
        this.state = STATE_INIT;
        this.sect = sect;
        this.words = null;
        onNext();
    }

    public void stop() {
        this.state = STATE_DONE;
        this.sect = null;
        this.words = null;
        if (mRsvpView != null)
            mRsvpView.stop(null);
        if (tvNextText != null)
            tvNextText.setText("");
    }

    private void onNext() {
        if (mRsvpView == null)
            return;
        if (sect == null) {
            state = STATE_DONE;
            mRsvpView.stop(null);
            tvNextText.setText("");
            return;
        }
        if (state == STATE_INIT || state == STATE_DONE) {
            // show title & description
            state = STATE_TITLE;
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            words.addIntroWords(sect.descr);
            mRsvpView.play(words);
            if (sect.hasArticle)
                tvNextText.setText("A");
            else if (sect.children != null && sect.children.length > 0)
                tvNextText.setText("L");
            else
                tvNextText.setText("");
            return;
        }
        if (state == STATE_TITLE) {
            // show article if any
            state = STATE_ARTICLE;
            if (sect.hasArticle) {
                words = new RsvpWords();
                words.addArticleWords(sect.entity);
                mRsvpView.play(words);
                if (sect.children != null && sect.children.length > 0)
                    tvNextText.setText("L");
                else
                    tvNextText.setText("");
                return;
            }
        }
        if (state == STATE_ARTICLE) {
            // show children if any
            if (sect.children != null && sect.children.length > 0) {
                state = STATE_CHILD;
                SSect child = sect.children[0];
                words = new RsvpWords();
                words.addTitleWords(child.title);
                words.addIntroWords(child.descr);
                mRsvpView.play(words);
                if (sect.children.length > 1)
                    tvNextText.setText(Integer.toString(sect.children.length-1));
                else
                    tvNextText.setText("");
                return;
            } else {
                state = STATE_DONE;
            }
        }
        if (state >= STATE_CHILD) {
            int pos = 1 + state - STATE_CHILD;
            if (pos < sect.children.length) {
                state = STATE_CHILD + pos;
                SSect child = sect.children[pos];
                words = new RsvpWords();
                words.addTitleWords(child.title);
                words.addIntroWords(child.descr);
                mRsvpView.play(words);
                if (sect.children.length > pos+1)
                    tvNextText.setText(Integer.toString(sect.children.length-pos-1));
                else
                    tvNextText.setText("");
                return;
            }
        }
        state = STATE_DONE;
    }

    @Override
    public void onRsvpPlayStart() {
    }

    @Override
    public void onRsvpPlayStop() {
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.next) {
            onNext();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() != R.id.record)
            return false;
        switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN:
            startRecordVoice();
            break;
        case MotionEvent.ACTION_UP:
        case MotionEvent.ACTION_CANCEL:
            stopRecordVoice();
            break;
        }
        return true;
    }

    private void startRecordVoice() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int res = getActivity().checkSelfPermission(Manifest.permission.RECORD_AUDIO);
            if (res != PERMISSION_GRANTED) {
                getActivity().requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
        }
        try {
            recorder = new SoundRecorder(getActivity(), "voice.raw", this);
            recorder.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Error on voice recording", e);
        }
    }
    private void stopRecordVoice() {
        try {
            recorder.stopRecording();
        } catch (Exception e) {
            Log.e(TAG, "Error on voice recording", e);
        }
    }

    @Override
    public void onRecordingStopped() {
        recorder.startPlay();
        Activity activity = getActivity();
        if (activity instanceof WearActivity)
            ((WearActivity)activity).sendVoice("voice.raw");
    }

    @Override
    public void onPlaybackStopped() {
    }
}
