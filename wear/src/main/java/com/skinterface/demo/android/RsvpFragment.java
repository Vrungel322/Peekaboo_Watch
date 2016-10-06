package com.skinterface.demo.android;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
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

import static com.skinterface.demo.android.WearActivity.TAG;

public class RsvpFragment extends Fragment implements
        View.OnClickListener,
        GestureDetector.OnGestureListener
{
    private static final int STATE_INIT    = 0;
    private static final int STATE_DONE    = 1;
    private static final int STATE_TITLE   = 2;
    private static final int STATE_ARTICLE = 3;
    private static final int STATE_CHILD   = 4;

    RsvpView mRsvpView;
    TextView mPositionView;

    SSect sect;
    RsvpWords words;
    int state;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.fr_rsvp_round, container, false);
        view.findViewById(R.id.next).setOnClickListener(this);
        view.findViewById(R.id.prev).setOnClickListener(this);
        mRsvpView = (RsvpView) view.findViewById(R.id.rsvp);
        mRsvpView.setListener(((WearActivity)getActivity()).handler);
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
    }

    public void onRsvpPlayStart() {
        updatePosView(true);
    }

    public void onRsvpPlayStop() {
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

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(TAG,"onDown: " + event.toString());
        return true;
    }

    public void onUpOrCancel(boolean cancel) {
        Log.d(TAG,"onUpOrCancel: cancel="+cancel);
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
            ((WearActivity)getActivity()).startRecordVoice();
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
}
