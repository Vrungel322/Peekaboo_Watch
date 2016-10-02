package com.skinterface.demo.android;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.skinterface.demo.android.WearActivity.TAG;

public class RsvpFragment extends Fragment implements
        View.OnClickListener,
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener
{
    private static final String VOICE_FILE_NAME = "voice.wav";

    private static final int STATE_INIT    = 0;
    private static final int STATE_DONE    = 1;
    private static final int STATE_TITLE   = 2;
    private static final int STATE_ARTICLE = 3;
    private static final int STATE_CHILD   = 4;

    static final int MSG_RSVP_PLAY_STARTED  = 1;
    static final int MSG_RSVP_PLAY_FINISHED = 2;

    static final int MSG_SOUND_PLAY_FAIL    = 10;
    static final int MSG_SOUND_PLAY_STARTED = 11;
    static final int MSG_SOUND_PLAY_FINISHED= 12;
    static final int MSG_SOUND_REC_FAIL     = 13;
    static final int MSG_SOUND_REC_STARTED  = 14;
    static final int MSG_SOUND_REC_PROGRESS = 15;
    static final int MSG_SOUND_REC_FINISHED = 16;

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_RSVP_PLAY_STARTED:
                onRsvpPlayStart();
                break;
            case MSG_RSVP_PLAY_FINISHED:
                onRsvpPlayStop();
                break;
            case MSG_SOUND_PLAY_STARTED:
                break;
            case MSG_SOUND_PLAY_FINISHED:
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

    RsvpView mRsvpView;
    TextView mPositionView;

    SSect sect;
    RsvpWords words;
    int state;
    SoundRecorder recorder;
    boolean recording;
    int recording_time;
    int recording_size;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.fr_rsvp_round, container, false);
        view.findViewById(R.id.next).setOnClickListener(this);
        view.findViewById(R.id.prev).setOnClickListener(this);
        mRsvpView = (RsvpView) view.findViewById(R.id.rsvp);
        mRsvpView.setListener(handler);
        mPositionView = (TextView) view.findViewById(R.id.position);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (state == STATE_INIT)
            onNext();
    }

    public void play(SSect sect) {
        if (this.sect == sect)
            return;
        this.state = STATE_INIT;
        this.sect = sect;
        this.words = null;
        onNext();
    }

    public void stop() {
        this.state = STATE_DONE;
        //this.sect = null;
        this.words = null;
        if (mRsvpView != null)
            mRsvpView.stop(null);
        if (mPositionView != null)
            mPositionView.setText("");
    }

    private void updatePosView(boolean playing) {
        if (mRsvpView == null)
            return;
        if (recording) {
            String txt = String.format("%1$tM:%1$tS %2$1.2fmb ", (long)recording_time, recording_size / (float)(1024*1024));
            mPositionView.setText(txt);
            return;
        }
        if (sect == null) {
            mPositionView.setText("");
            return;
        }
        SpannableStringBuilder sb = new SpannableStringBuilder();
        sb.append("*T*");
        if (sect.hasArticle)
            sb.append("A*");
        switch (state) {
        case STATE_INIT:
            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            break;
        case STATE_DONE:
            sb.setSpan(new StyleSpan(Typeface.BOLD), sb.length()-1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            break;
        case STATE_TITLE:
            if (playing)
                sb.setSpan(new StyleSpan(Typeface.BOLD), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                sb.setSpan(new StyleSpan(Typeface.BOLD), 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            break;
        case STATE_ARTICLE:
            if (playing)
                sb.setSpan(new StyleSpan(Typeface.BOLD), 3, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                sb.setSpan(new StyleSpan(Typeface.BOLD), 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            break;
        }
        if (sect.children != null && sect.children.length > 0) {
            int cnt = sect.children.length;
            int pos = state - STATE_CHILD;
            if (pos < 0) { pos = -1; playing = false; }
            if (pos >= cnt) { pos = cnt-1; }
            if (playing) {
                sb.insert(sb.length()-1, "*"+pos+"<1>"+(cnt-pos-1));
            } else {
                sb.insert(sb.length()-1, "*"+(pos+1)+"<:>"+(cnt-pos-1));
            }
        }
        mPositionView.setText(sb);
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
            return;
        }
        if (state == STATE_TITLE) {
            // show article if any
            state = STATE_ARTICLE;
            if (sect.hasArticle) {
                words = new RsvpWords();
                words.addArticleWords(sect.entity);
                mRsvpView.play(words);
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
                return;
            } else {
                state = STATE_DONE;
            }
        }
        if (state >= STATE_CHILD) {
            int pos = state - STATE_CHILD + 1;
            if (pos < sect.children.length) {
                state = STATE_CHILD + pos;
                SSect child = sect.children[pos];
                words = new RsvpWords();
                words.addTitleWords(child.title);
                words.addIntroWords(child.descr);
                mRsvpView.play(words);
            }
            return;
        }
        state = STATE_DONE;
    }

    private void onPrev() {
        if (mRsvpView == null)
            return;
        if (sect == null) {
            state = STATE_DONE;
            mRsvpView.stop(null);
            mPositionView.setText("");
            return;
        }

        if (state > STATE_CHILD) {
            int pos = state - STATE_CHILD - 1;
            if (pos >= sect.children.length)
                pos = sect.children.length - 1;
            if (pos >= 0) {
                state = STATE_CHILD + pos;
                SSect child = sect.children[pos];
                words = new RsvpWords();
                words.addTitleWords(child.title);
                words.addIntroWords(child.descr);
                mRsvpView.play(words);
                return;
            }
            state = STATE_CHILD;
        }
        if (state == STATE_CHILD || state == STATE_ARTICLE) {
            // show title & description
            state = STATE_TITLE;
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            words.addIntroWords(sect.descr);
            mRsvpView.play(words);
            return;
        }
        state = STATE_INIT;
        mRsvpView.stop(null);
        updatePosView(false);
        return;
    }

    public void onRsvpPlayStart() {
        updatePosView(true);
    }

    public void onRsvpPlayStop() {
        updatePosView(false);
    }

    public void onSoundRecFail() {
        recording = false;
        recording_time = 0;
        if (getActivity() != null)
            new File(getActivity().getFilesDir(), VOICE_FILE_NAME).delete();
    }

    public void onSoundRecStart() {
        recording = true;
        recording_time = 0;
        updatePosView(false);
    }

    private void onSoundRecProgress(int millis, int size) {
        recording_time = millis;
        recording_size = size;
        Log.i(TAG, "recording "+millis+" ms / "+size+" bytes");
        updatePosView(false);
    }

    public void onSoundRecStop(int millis, int size) {
        recording = false;
        recording_time = millis;
        recording_size = size;
        Activity activity = getActivity();
        if (activity instanceof WearActivity)
            ((WearActivity)activity).sendVoice(VOICE_FILE_NAME);
        updatePosView(false);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.next) {
            onNext();
        }
        else if (v.getId() == R.id.prev) {
            onPrev();
        }
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
            } else {
                recorder = new SoundRecorder(getActivity(), VOICE_FILE_NAME, this.handler);
            }
            recorder.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Error on voice recording", e);
        }
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(TAG,"onDown: " + event.toString());
        return true;
    }

    public void onUpOrCancel(boolean cancel) {
        Log.d(TAG,"onUpOrCancel: cancel="+cancel);
        if (recording) {
            try {
                recorder.stopRecording();
            } catch (Exception e) {
                Log.e(TAG, "Error on voice recording", e);
            }
        }
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velX, float velY) {
        Log.d(TAG, "onFling: " + event1.toString()+event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(TAG, "onLongPress: " + event.toString());
        int x = (int)event.getX();
        int y = (int)event.getY();
        int Cx = (mRsvpView.getLeft() + mRsvpView.getRight()) / 2;
        int Cy = (mRsvpView.getTop() + mRsvpView.getBottom()) / 2;
        int dx2 = (Cx -x)*(Cx - x);
        int dy2 = (Cy -y)*(Cy - y);
        if (dx2 < 50*50 && dy2 < 50*50)
            startRecordVoice();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
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
