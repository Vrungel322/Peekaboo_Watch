package com.skinterface.demo.android;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.view.CircularButton;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import static com.skinterface.demo.android.WearActivity.TAG;

public class RsvpFragment extends Fragment implements
        SectionsModel.SectionsListener,
        View.OnClickListener,
        GestureDetector.OnGestureListener,
        View.OnLongClickListener
{
    private static final int STATE_INIT    = 0;
    private static final int STATE_DONE    = 1;
    private static final int STATE_TITLE   = 3;
    private static final int STATE_ARTICLE = 4;
    private static final int STATE_CHILD   = 5;

    public static final int FN1_MASK   = 0xF;
    public static final int FN1_EDIT   = 0x1;
    public static final int FN1_SEND   = 0x2;

    public static final int FN2_MASK   = 0xF0;
    public static final int FN2_RETURN = 0x10;
    public static final int FN2_CANCEL = 0x20;

    public static final int NAV_MODE   = 0xF00000;
    public static final int NAV_CHAT   = 0x100000;
    public static final int NAV_SITE   = 0x200000;

    private static final String CHILD_POS_END  = new String("end");

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
                updatePosView();
            }
        }
    };

    private Rect rect = new Rect();

    RsvpView mRsvpView;
    CircularButton mRecordButton;
    CircularButton mPrevButton;
    CircularButton mNextButton;
    TextView mPositionView;
    TextView mPrevText;
    TextView mNextText;
    TextView mChatMessageNewView;
    TextView mChatMessageTimeView;
    ImageView mChatMessageStatusView;

    SSect sect;
    RsvpWords words;
    int state;
    int flags;
    String cguid;

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
        mChatMessageNewView = (TextView) view.findViewById(R.id.chat_message_new);
        mChatMessageTimeView = (TextView) view.findViewById(R.id.chat_message_time);
        mChatMessageStatusView = (ImageView) view.findViewById(R.id.chat_message_status);

        mChatMessageNewView.setOnClickListener(this);
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

    public void load(SSect sect, int flags, boolean play) {
        if (this.sect == sect)
            return;
        if (mRsvpView != null)
            mRsvpView.stop(null);
        this.sect = sect;
        this.words = null;
        this.flags = flags;
        switch ( (flags & FN1_MASK) ) {
        case FN1_EDIT:
            mRecordButton.setImageResource(R.drawable.ic_record);
            mRecordButton.setVisibility(View.VISIBLE);
            break;
        case FN1_SEND:
            mRecordButton.setImageResource(R.drawable.ic_send);
            mRecordButton.setVisibility(View.VISIBLE);
            break;
        case 0:
        default:
            mRecordButton.setVisibility(View.INVISIBLE);
            break;
        }
        switch ( (flags & FN2_MASK) ) {
        case FN2_RETURN:
            mPrevButton.setImageResource(R.drawable.ic_return);
            mPrevButton.setVisibility(View.VISIBLE);
            break;
        case FN2_CANCEL:
            mPrevButton.setImageResource(R.drawable.ic_close);
            mPrevButton.setVisibility(View.VISIBLE);
            break;
        case 0:
        default:
            mPrevButton.setVisibility(View.INVISIBLE);
            break;
        }
        if (sect == null) {
            mNextButton.setVisibility(View.INVISIBLE);
        } else {
            mNextButton.setVisibility(View.VISIBLE);
        }
        if ((flags & NAV_MODE) == NAV_CHAT && sect != null) {
            this.state = STATE_CHILD;
            this.cguid = CHILD_POS_END;
        } else {
            this.state = STATE_INIT;
            this.cguid = null;
        }
        updatePosView();
        onNext(play);
    }

    public boolean toChild(int idx, boolean play) {
        if (sect == null || sect.children == null)
            return false;
        if (idx < 0) idx = -1;
        if (idx > sect.children.length) idx = sect.children.length;
        sect.currListPosition = idx;
        this.state = STATE_CHILD;
        if (idx >= sect.children.length)
            this.cguid = CHILD_POS_END;
        else if (idx < 0)
            this.cguid = null;
        else
            this.cguid = sect.children[idx].guid;
        onRepeat(play);
        return true;
    }

    public void update() {
        if (cguid != null) {
            if (cguid == CHILD_POS_END) {
                sect.currListPosition = sect.children.length;
            } else {
                for (int pos = 0; pos < sect.children.length; ++pos) {
                    if (cguid == sect.children[pos].guid) {
                        sect.currListPosition = pos;
                        break;
                    }
                }
            }
            updatePosView();
        }
    }

    public void stop() {
        this.words = null;
        if (mRsvpView != null)
            mRsvpView.stop(null);
        if (mPositionView != null)
            mPositionView.setText("");
    }

    private void updatePosView() {
        if (mRsvpView == null)
            return;
        if (sect == null) {
            mPositionView.setText(getSpeedString());
            mPrevText.setText("");
            mNextText.setText("");
            mChatMessageNewView.setText("");
            mChatMessageNewView.setVisibility(View.GONE);
            mChatMessageTimeView.setText("");
            mChatMessageTimeView.setVisibility(View.GONE);
            mChatMessageStatusView.setImageResource(0);
            mChatMessageStatusView.setVisibility(View.INVISIBLE);
            return;
        }
        if ((flags & NAV_MODE) == NAV_CHAT) {
            int new_messages = 0;
            String status = null;
            long timestamp = 0;
            if (sect != null && sect.children != null) {
                int pos = sect.currListPosition;
                if (pos >= 0 && pos < sect.children.length) {
                    SSect child = sect.children[pos];
                    status = child.entity.name;
                    timestamp = child.timestamp;
                }
                for (int p=0; p < sect.children.length; ++p) {
                    SSect msg = sect.children[p];
                    if (msg.entity.role == "recv" && !"read".equals(msg.entity.name))
                        new_messages += 1;
                }
            }
            if (new_messages > 0) {
                mChatMessageNewView.setText(Integer.toString(new_messages));
                mChatMessageNewView.setVisibility(View.VISIBLE);
            } else {
                mChatMessageNewView.setText("");
                mChatMessageNewView.setVisibility(View.GONE);
            }
            long elapsed = System.currentTimeMillis() - timestamp;
            if (timestamp == 0 || elapsed < 0) {
                mChatMessageTimeView.setText("");
                mChatMessageTimeView.setVisibility(View.GONE);
            }
            else if (elapsed < 24*60*60*1000 ) {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                mChatMessageTimeView.setText(sdf.format(elapsed));
                mChatMessageTimeView.setVisibility(View.VISIBLE);
            }
            else {
                long days = elapsed / (24*60*60*1000);
                if (days == 1) {
                    SimpleDateFormat sdf = new SimpleDateFormat("1'd' HH'h'");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    mChatMessageTimeView.setText(sdf.format(elapsed));
                }
                else if (days <= 7) {
                    SimpleDateFormat sdf = new SimpleDateFormat("D'd' HH'h'");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    mChatMessageTimeView.setText(sdf.format(elapsed));
                }
                else {
                    mChatMessageTimeView.setText(days + " days");
                }
                mChatMessageTimeView.setVisibility(View.VISIBLE);
            }
            if (status == "sent") {
                mChatMessageStatusView.setImageResource(R.drawable.ic_sending);
                mChatMessageStatusView.setVisibility(View.VISIBLE);
            }
            else if (status == "delivered") {
                mChatMessageStatusView.setImageResource(R.drawable.ic_sent_unread);
                mChatMessageStatusView.setVisibility(View.VISIBLE);
            }
            else if (status == "read") {
                mChatMessageStatusView.setImageResource(R.drawable.ic_sent_read);
                mChatMessageStatusView.setVisibility(View.VISIBLE);
            }
            else {
                mChatMessageStatusView.setImageResource(0);
                mChatMessageStatusView.setVisibility(View.INVISIBLE);
            }
        } else {
            mChatMessageNewView.setText("");
            mChatMessageNewView.setVisibility(View.GONE);
            mChatMessageTimeView.setText("");
            mChatMessageTimeView.setVisibility(View.GONE);
            mChatMessageStatusView.setImageResource(0);
            mChatMessageStatusView.setVisibility(View.INVISIBLE);
        }



//        SpannableStringBuilder sb = new SpannableStringBuilder();
//        sb.append("*T*");
//        if (sect.hasArticle)
//            sb.append("A*");
//        switch (state) {
//        case STATE_INIT:
//            sb.setSpan(new StyleSpan(Typeface.BOLD), 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            break;
//        case STATE_DONE:
//            sb.setSpan(new StyleSpan(Typeface.BOLD), sb.length()-1, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            break;
//        case STATE_TITLE:
//            if (playing)
//                sb.setSpan(new StyleSpan(Typeface.BOLD), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            else
//                sb.setSpan(new StyleSpan(Typeface.BOLD), 2, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            break;
//        case STATE_ARTICLE:
//            if (playing)
//                sb.setSpan(new StyleSpan(Typeface.BOLD), 3, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            else
//                sb.setSpan(new StyleSpan(Typeface.BOLD), 4, 5, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//            break;
//        }
//        if (sect.children != null && sect.children.length > 0) {
//            int cnt = sect.children.length;
//            int pos = sect.currListPosition;
//            if (pos < 0) { pos = -1; playing = false; }
//            if (pos >= cnt) { pos = cnt-1; }
//            if (playing) {
//                sb.insert(sb.length()-1, "*"+pos+"<1>"+(cnt-pos-1));
//            } else {
//                sb.insert(sb.length()-1, "*"+(pos+1)+"<:>"+(cnt-pos-1));
//            }
//            mPrevText.setText(Integer.toString(pos+1));
//            mNextText.setText(Integer.toString(cnt-pos-1));
//        }
        mPositionView.setText(getSpeedString());
    }

    private void updateActions() {
        if ((flags & NAV_MODE) == NAV_SITE) {
            int vis = View.INVISIBLE;
            if (sect == null)
                ;
            else if (state == STATE_CHILD && sect.children != null && sect.children.length > 0) {
                if (sect.currListPosition >= 0 && sect.currListPosition <= sect.children.length) {
                    SSect child = sect.children[sect.currListPosition];
                    if (child.hasArticle || child.hasChildren) {
                        mRecordButton.setImageResource(R.drawable.ic_enter);
                        vis = View.VISIBLE;
                    }
                }
            }
            else if (state == STATE_INIT || state == STATE_DONE) {
                if (sect.hasArticle) {
                    mRecordButton.setImageResource(R.drawable.ic_enter);
                    vis = View.VISIBLE;
                }
                else if (sect.children != null && sect.children.length > 0) {
                    mRecordButton.setImageResource(R.drawable.ic_enter);
                    vis = View.VISIBLE;
                }
            }
            mRecordButton.setVisibility(vis);
        }
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
        updateActions();
        if (sect == null) {
            state = STATE_DONE;
            cguid = null;
            mRsvpView.stop(null);
            mPositionView.setText(getSpeedString());
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
            if (sect.descr != null)
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
        if (state == STATE_CHILD) {
            int pos = sect.currListPosition;
            if (pos < sect.children.length) {
                SSect child = sect.children[pos];
                words = new RsvpWords();
                words.addTitleWords(child.title);
                if (child.descr != null)
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
            cguid = null;
            mRsvpView.stop(null);
            mPositionView.setText(getSpeedString());
            updateActions();
            return;
        }
        if (play && mRsvpView.isPaused()) {
            mRsvpView.resume();
            return;
        }
        if (state == STATE_INIT || state == STATE_DONE) {
            // show title & description
            state = STATE_TITLE;
            cguid = null;
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            if (sect.descr != null)
                words.addIntroWords(sect.descr);
            mRsvpView.load(words, play);
            updateActions();
            return;
        }
        if (state == STATE_TITLE) {
            // show article if any
            state = STATE_ARTICLE;
            cguid = null;
            if (sect.hasArticle) {
                words = new RsvpWords();
                words.addArticleWords(sect.entity);
                mRsvpView.load(words, play);
                updateActions();
                return;
            }
        }
        if (state == STATE_ARTICLE) {
            // show children if any
            if (sect.children != null && sect.children.length > 0) {
                sect.currListPosition = 0;
                state = STATE_CHILD;
                SSect child = sect.children[0];
                cguid = child.guid;
                words = new RsvpWords();
                words.addTitleWords(child.title);
                if (child.descr != null)
                    words.addIntroWords(child.descr);
                mRsvpView.load(words, play);
                updateActions();
                return;
            }
            else if ((flags & NAV_MODE) == NAV_CHAT) {
                sect.currListPosition = (sect.children == null) ? -1 : sect.children.length;
                state = STATE_CHILD;
                cguid = CHILD_POS_END;
            }
            else {
                state = STATE_DONE;
                cguid = null;
            }
        }
        if (state == STATE_CHILD) {
            int pos = sect.currListPosition + 1;
            if (pos < sect.children.length) {
                sect.currListPosition = pos;
                SSect child = sect.children[pos];
                cguid = child.guid;
                words = new RsvpWords();
                words.addTitleWords(child.title);
                if (child.descr != null)
                    words.addIntroWords(child.descr);
                mRsvpView.load(words, play);
            } else {
                cguid = CHILD_POS_END;
                sect.currListPosition = sect.children.length;
            }
            updateActions();
            return;
        }
        state = STATE_DONE;
        cguid = null;
        updateActions();
    }

    private void onPrev() {
        if (mRsvpView == null)
            return;
        if (sect == null) {
            state = STATE_DONE;
            cguid = null;
            mRsvpView.stop(null);
            mPositionView.setText("");
            updateActions();
            return;
        }

        if (state == STATE_CHILD) {
            int pos = sect.currListPosition - 1;
            if (pos >= sect.children.length)
                pos = sect.children.length - 1;
            if (pos >= 0 && sect.children.length > 0) {
                sect.currListPosition = pos;
                SSect child = sect.children[pos];
                cguid = child.guid;
                words = new RsvpWords();
                words.addTitleWords(child.title);
                if (child.descr != null)
                    words.addIntroWords(child.descr);
                mRsvpView.load(words, false);
                updateActions();
                return;
            }
            else if ((flags & NAV_MODE) == NAV_CHAT) {
                sect.currListPosition = -1;
                cguid = null;
                updatePosView();
                updateActions();
                return;
            } else {
                sect.currListPosition = -1;
                cguid = null;
                state = STATE_ARTICLE;
                // fall down
            }
        }
        if (state == STATE_ARTICLE) {
            // show title & description
            state = STATE_TITLE;
            cguid = null;
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            if (sect.descr != null)
                words.addIntroWords(sect.descr);
            mRsvpView.load(words, false);
            updateActions();
            return;
        }
        state = STATE_INIT;
        cguid = null;
        mRsvpView.stop(null);
        updatePosView();
        updateActions();
    }

    @Override
    public void onSectionsChanged(SectionsModel model) {
        updatePosView();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.record) {
            if ( (flags & FN1_MASK) == FN1_EDIT ) {
                ((WearActivity) getActivity()).startVoiceRecognition();
            }
            else if ( (flags & FN1_MASK) == FN1_SEND ) {
                ((WearActivity) getActivity()).composeNewChatMessageResult(true); // send
            }

            if ((flags & NAV_MODE) == NAV_SITE) {
                if (state == STATE_CHILD && sect.children != null && sect.children.length > 0) {
                    if (sect.currListPosition >= 0 && sect.currListPosition <= sect.children.length) {
                        SSect child = sect.children[sect.currListPosition];
                        if (child.hasArticle || child.hasChildren) {
                            Action action = new Action("enter").add("sectID", child.guid);
                            Navigator nav = ((WearActivity) getActivity()).nav;
                            if (nav instanceof SiteNavigator)
                                ((SiteNavigator) nav).executeAction(action);
                        }
                    }
                }
                else if (state == STATE_INIT || state == STATE_DONE) {
                    if (sect.hasArticle) {
                        state = STATE_ARTICLE;
                        onRepeat(true);
                        return;
                    }
                    else if (sect.children != null && sect.children.length > 0) {
                        state = STATE_CHILD;
                        toChild(0, false);
                        return;
                    }
                }
            }
        }
        if (id == R.id.prev) {
            if ( (flags & FN2_MASK) == FN2_RETURN ) {
                onPrev();
            }
            else if ( (flags & FN2_MASK) == FN2_CANCEL ) {
                ((WearActivity) getActivity()).composeNewChatMessageResult(false); // cancel
            }
        }
        if (id == R.id.next) {
            onNext(true);
        }
        if (id == R.id.chat_message_new) {
            for (int p=0; p < sect.children.length; ++p) {
                SSect msg = sect.children[p];
                if (msg.entity.role == "recv" && !"read".equals(msg.entity.name)) {
                    toChild(p, false);
                    return;
                }
            }
            toChild(sect.children.length, false);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.record) {
            if ( (flags & FN1_MASK) == FN1_EDIT ) {
                ((WearActivity) getActivity()).startVoiceRecording();
                return true;
            }
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
        ((WearActivity)getActivity()).handler.removeMessages(WearActivity.RSVP_SPEED);
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
        int x = (int)event.getX();
        int y = (int)event.getY();
        mRsvpView.getHitRect(rect);
        rect.left = rect.right-150;
        if (rect.contains(x, y)) {
            ((WearActivity)getActivity()).handler.obtainMessage(WearActivity.RSVP_SPEED, 1, 0).sendToTarget();
            return;
        }
        mRsvpView.getHitRect(rect);
        rect.right = rect.left+150;
        if (rect.contains(x, y)) {
            ((WearActivity)getActivity()).handler.obtainMessage(WearActivity.RSVP_SPEED, -1, 0).sendToTarget();
            return;
        }
    }

    public void accelerate(int delta) {
        if (delta > 0) {
            int tic = mRsvpView.getTic();
            int idx = 0;
            for (; idx < TICS.length-1; ++idx) {
                if (tic >= TICS[idx])
                    break;
            }
            if (idx+1 < TICS.length) {
                mRsvpView.setTic(TICS[idx + 1]);
                PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
                        .edit().putInt("rsvp_tic", mRsvpView.getTic()).apply();
                Message msg = ((WearActivity)getActivity()).handler.obtainMessage(WearActivity.RSVP_SPEED, delta, 0);
                ((WearActivity)getActivity()).handler.sendMessageDelayed(msg, 1600);
            }
        }
        else if (delta < 0) {
            int tic = mRsvpView.getTic();
            int idx = 0;
            for (; idx < TICS.length-1; ++idx) {
                if (tic >= TICS[idx])
                    break;
            }
            if (idx > 0) {
                mRsvpView.setTic(TICS[idx - 1]);
                PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext())
                        .edit().putInt("rsvp_tic", mRsvpView.getTic()).apply();
                Message msg = ((WearActivity)getActivity()).handler.obtainMessage(WearActivity.RSVP_SPEED, delta, 0);
                ((WearActivity)getActivity()).handler.sendMessageDelayed(msg, 1600);
            }
        }
        mPositionView.setText(getSpeedString());
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
