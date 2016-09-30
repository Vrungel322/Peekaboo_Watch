package com.skinterface.demo.android;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.skinterface.demo.android.WearActivity.TAG;

public class RsvpFragment extends Fragment implements
        RsvpView.RsvpViewListener,
        View.OnClickListener,
        View.OnTouchListener,
        SoundRecorder.OnVoicePlaybackStateChangedListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener
{
    private static final int STATE_INIT    = 0;
    private static final int STATE_DONE    = 1;
    private static final int STATE_TITLE   = 2;
    private static final int STATE_ARTICLE = 3;
    private static final int STATE_CHILD   = 4;

    RsvpView mRsvpView;
    TextView mPositionView;
    View mRecordView;

    Rect rect = new Rect();

    SSect sect;
    RsvpWords words;
    int state;
    SoundRecorder recorder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.fr_rsvp_round, container, false);
        view.findViewById(R.id.record).setOnTouchListener(this);
        view.findViewById(R.id.rsvp).setOnTouchListener(this);
        view.findViewById(R.id.next).setOnClickListener(this);
        mRsvpView = (RsvpView) view.findViewById(R.id.rsvp);
        mRsvpView.setListener(this);
        mPositionView = (TextView) view.findViewById(R.id.position);
        mRecordView = view.findViewById(R.id.record);
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
        if (mPositionView != null)
            mPositionView.setText("");
    }

    private void onNext() {
        if (mRsvpView == null)
            return;
        if (sect == null) {
            state = STATE_DONE;
            mRsvpView.stop(null);
            mPositionView.setText("");
            return;
        }
        if (state == STATE_INIT || state == STATE_DONE) {
            // show title & description
            state = STATE_TITLE;
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            words.addIntroWords(sect.descr);
            mRsvpView.play(words);
            String p = "|T|";
            if (sect.hasArticle)
                p += ">A";
            if (sect.children != null && sect.children.length > 0)
                p += ">"+sect.children.length;
            mPositionView.setText(p);
            return;
        }
        if (state == STATE_TITLE) {
            // show article if any
            state = STATE_ARTICLE;
            if (sect.hasArticle) {
                words = new RsvpWords();
                words.addArticleWords(sect.entity);
                mRsvpView.play(words);
                String p = "T|A|";
                if (sect.children != null && sect.children.length > 0)
                    p += ">"+sect.children.length;
                mPositionView.setText(p);
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
                String p = sect.hasArticle ? "TA|" : "T|";
                if (sect.children.length > 1)
                    p += "1>"+(sect.children.length-1);
                else
                    p += "0";
                mPositionView.setText(p);
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
                String p = sect.hasArticle ? "TA|" : "T|";
                p += pos+">"+(sect.children.length-1);
                mPositionView.setText(p);
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
        mRsvpView.playIcons();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.next) {
            onNext();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int id = v.getId();
        if (!(id == R.id.record || id == R.id.rsvp))
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
            if (recorder != null) {
                if (recorder.getState() != SoundRecorder.State.IDLE)
                    return;
                recorder = new SoundRecorder(getActivity(), "voice.raw", this);
            }
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
        Activity activity = getActivity();
        if (activity instanceof WearActivity)
            ((WearActivity)activity).sendVoice("voice.raw");
    }

    @Override
    public void onPlaybackStopped() {
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(TAG,"onDown: " + event.toString());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2,
                           float velocityX, float velocityY) {
        Log.d(TAG, "onFling: " + event1.toString()+event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(TAG, "onLongPress: " + event.toString());
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        Log.d(TAG, "onScroll: " + e1.toString()+e2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(TAG, "onDoubleTap: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }
}
