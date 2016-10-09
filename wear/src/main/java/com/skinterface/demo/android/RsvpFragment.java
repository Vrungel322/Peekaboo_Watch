package com.skinterface.demo.android;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.CircularButton;
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
        SectionsModel.SectionsListener,
        View.OnClickListener,
        GestureDetector.OnGestureListener,
        View.OnLongClickListener
{
    private static final int STATE_INIT    = 0;
    private static final int STATE_DONE    = 1;
    private static final int STATE_COMPOSE = 2;
    private static final int STATE_TITLE   = 3;
    private static final int STATE_ARTICLE = 4;
    private static final int STATE_CHILD   = 5;

    private static final int[] TICS = {
            50, //calcTic(200),
            45, //calcTic(220),
            40, //calcTic(250),
            35, //calcTic(280),
            30, //calcTic(330),
            26, //calcTic(380),
            23, //calcTic(430),
            20, //calcTic(430),
            18, //calcTic(550),
            17, //calcTic(590),
            16, //calcTic(620),
            15, //calcTic(670),
            14, //calcTic(710),
    };
    private static int calcTic(int wpm) {
        int word_millis = 60*1000 / wpm;
        return word_millis / RsvpWords.DEFAULT_WEIGHT;
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RsvpView.ACTION_RSVP_EVENT.equals(intent.getAction())) {
                String state = intent.getStringExtra(RsvpView.EXTRA_RSVP_STATE);
                updatePosView(RsvpView.RSVP_STATE_STARTED.equals(state));
            }
        }
    };


    RsvpView mRsvpView;
    CircularButton mRecordButton;
    CircularButton mPrevButton;
    CircularButton mNextButton;
    TextView mPositionView;
    TextView mPrevText;
    TextView mNextText;

    SSect sect;
    RsvpWords words;
    int state;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(RsvpView.ACTION_RSVP_EVENT));
        View view = inflater.inflate(R.layout.fr_rsvp_round, container, false);
        mPrevButton = (CircularButton) view.findViewById(R.id.prev);
        mNextButton = (CircularButton) view.findViewById(R.id.next);
        mRecordButton = (CircularButton) view.findViewById(R.id.record);
        mRsvpView = (RsvpView) view.findViewById(R.id.rsvp);
        mPositionView = (TextView) view.findViewById(R.id.position);
        mPrevText = (TextView) view.findViewById(R.id.prev_text);
        mNextText = (TextView) view.findViewById(R.id.next_text);

        mPrevButton.setOnClickListener(this);
        mNextButton.setOnClickListener(this);
        mRecordButton.setOnClickListener(this);
        mRecordButton.setOnLongClickListener(this);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        onRepeat(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRsvpView != null)
            mRsvpView.stop(null);
    }

    public void load(SSect sect, boolean play) {
        if (this.sect == sect)
            return;
        this.sect = sect;
        this.words = null;
        if (sect != null &&
                "chat-text-msg".equals(sect.entity.media) &&
                "composing".equals(sect.entity.role))
        {
            this.state = STATE_COMPOSE;
            mPrevButton.setImageResource(R.drawable.ic_close);
            mPrevButton.setVisibility(View.VISIBLE);
            mRecordButton.setImageResource(R.drawable.ic_send);
        } else {
            if (this.state == STATE_COMPOSE) {
                mPrevButton.setImageResource(R.drawable.ic_close);
                mPrevButton.setVisibility(View.INVISIBLE);
                mRecordButton.setImageResource(R.drawable.ic_record);
            }
            this.state = STATE_INIT;
        }
        onNext(play);
    }

    public void load_to_child(SSect sect, int idx, boolean play) {
        this.state = STATE_CHILD + idx;
        this.sect = sect;
        this.words = null;
        onRepeat(play);
    }

    public void stop() {
        this.state = STATE_DONE;
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
            mPositionView.setText(getSpeedString());
            mPrevText.setText("");
            mNextText.setText("");
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
            mPrevText.setText(Integer.toString(pos+1));
            mNextText.setText(Integer.toString(cnt-pos-1));
        }
        mPositionView.setText(sb);
    }

    private String getSpeedString() {
        int tic = mRsvpView.getTic();
        int word_millis = tic * RsvpWords.DEFAULT_WEIGHT;
        int words_per_minute = 60*1000 / word_millis;
        words_per_minute = (words_per_minute + 4) / 10;
        return (words_per_minute*10) + " wpm";
    }

    private void onRepeat(boolean play) {
        if (mRsvpView == null)
            return;
        if (sect == null) {
            state = STATE_DONE;
            mRsvpView.stop(null);
            mPositionView.setText(getSpeedString());
            return;
        }
        if (state == STATE_COMPOSE) {
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            mRsvpView.load(words, play);
            return;
        }
        if (state == STATE_INIT || state == STATE_DONE) {
            mRsvpView.stop(null);
            mRsvpView.load(words, play);
            return;
        }
        if (state == STATE_TITLE) {
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            words.addIntroWords(sect.descr);
            mRsvpView.load(words, play);
            return;
        }
        if (state == STATE_ARTICLE) {
            if (sect.hasArticle) {
                words = new RsvpWords();
                words.addArticleWords(sect.entity);
                mRsvpView.load(words, play);
                return;
            }
            return;
        }
        if (state >= STATE_CHILD) {
            int pos = state - STATE_CHILD;
            if (pos < sect.children.length) {
                SSect child = sect.children[pos];
                words = new RsvpWords();
                words.addTitleWords(child.title);
                words.addIntroWords(child.descr);
                mRsvpView.load(words, play);
            }
            return;
        }
    }

    private void onNext(boolean play) {
        if (mRsvpView == null)
            return;
        if (sect == null) {
            state = STATE_DONE;
            mRsvpView.stop(null);
            mPositionView.setText(getSpeedString());
            return;
        }
        if (play && mRsvpView.isPaused()) {
            mRsvpView.resume();
            return;
        }
        if (state == STATE_COMPOSE) {
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            mRsvpView.load(words, play);
            return;
        }
        if (state == STATE_INIT || state == STATE_DONE) {
            // show title & description
            state = STATE_TITLE;
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            words.addIntroWords(sect.descr);
            mRsvpView.load(words, play);
            return;
        }
        if (state == STATE_TITLE) {
            // show article if any
            state = STATE_ARTICLE;
            if (sect.hasArticle) {
                words = new RsvpWords();
                words.addArticleWords(sect.entity);
                mRsvpView.load(words, play);
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
                mRsvpView.load(words, play);
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
                mRsvpView.load(words, play);
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

        if (state == STATE_COMPOSE) {
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            mRsvpView.load(words, false);
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
                mRsvpView.load(words, false);
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
            mRsvpView.load(words, false);
            return;
        }
        state = STATE_INIT;
        mRsvpView.stop(null);
        updatePosView(false);
    }

    @Override
    public void onSectionsChanged(SectionsModel model) {
        updatePosView(false);
    }

    @Override
    public void onClick(View v) {
        if (state == STATE_COMPOSE) {
            if (v.getId() == R.id.record)
                ((WearActivity) getActivity()).composeNewChatMessageResult(true); // send
            else if (v.getId() == R.id.next)
                onNext(true);
            else if (v.getId() == R.id.prev)
                ((WearActivity) getActivity()).composeNewChatMessageResult(false); // cancel
        } else {
            if (v.getId() == R.id.record)
                ((WearActivity) getActivity()).startVoiceRecognition();
            else if (v.getId() == R.id.next)
                onNext(true);
            else if (v.getId() == R.id.prev)
                onPrev();
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.record) {
            ((WearActivity) getActivity()).startVoiceRecording();
            return true;
        }
        return false;
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
        Log.d(TAG, "onFling: vx="+velX+"; vy="+velY+"; from="+event1.toString()+"; to="+event2.toString());
        int vx = (int)velX;
        int vy = (int)velY;
        boolean RtoL = false;
        boolean BtoT = false;
        if (vx < 0) {
            RtoL = true;
            vx = -vx;
        }
        if (vy < 0) {
            BtoT = true;
            vy = -vy;
        }
        if (vx > vy*5/4) {
            if (RtoL)
                onNext(false);
            else
                onPrev();
        }
        else if (vy > vx*5/4) {

        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(TAG, "onLongPress: " + event.toString());
//        int x = (int)event.getX();
//        int y = (int)event.getY();
//        int Cx = (mRsvpView.getLeft() + mRsvpView.getRight()) / 2;
//        Rect r = new Rect(Cx - 50, mRsvpView.getTop(), Cx + 50, mRsvpView.getBottom());
//        if (r.contains(x, y)) {
//            ((WearActivity) getActivity()).startRecordVoice();
//            return;
//        }
//        r.set(mRsvpView.getRight()-100, r.top, mRsvpView.getRight(), r.bottom);
//        if (r.contains(x, y)) {
//            int tic = mRsvpView.getTic();
//            int idx = 0;
//            for (; idx < TICS.length-1; ++idx) {
//                if (tic >= TICS[idx])
//                    break;
//            }
//            if (idx+1 < TICS.length) {
//                mRsvpView.setTic(TICS[idx + 1]);
//                PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
//                        .edit().putInt("rsvp_tic", mRsvpView.getTic()).apply();
//            }
//            mPositionView.setText(getSpeedString());
//            return;
//        }
//        r.set(mRsvpView.getLeft(), r.top, mRsvpView.getLeft()+100, r.bottom);
//        if (r.contains(x, y)) {
//            int tic = mRsvpView.getTic();
//            int idx = 0;
//            for (; idx < TICS.length-1; ++idx) {
//                if (tic >= TICS[idx])
//                    break;
//            }
//            if (idx > 0) {
//                mRsvpView.setTic(TICS[idx - 1]);
//                PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
//                        .edit().putInt("rsvp_tic", mRsvpView.getTic()).apply();
//            }
//            mPositionView.setText(getSpeedString());
//            return;
//        }
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
