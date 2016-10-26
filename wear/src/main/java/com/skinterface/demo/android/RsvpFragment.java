package com.skinterface.demo.android;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Build;
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
                RsvpView.RsvpState rsvpState = null;
                if (mRsvpView != null)
                    rsvpState = mRsvpView.getRsvpState();
                Log.d(TAG, "rsvp event: " + rsvpState);
                updatePlayButton();
                updatePosView();
            }
        }
    };

    private Rect rect = new Rect();

    WearActivity activity;
    UIAction mEnterAction;
    UIAction mLeaveAction;
    UIAction mNextAction;
    UIAction mPrevAction;
    RsvpView mRsvpView;
    View mDownBgView;
    View mDownButton;
    View mPrevButton;
    View mNextButton;
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
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (WearActivity) getActivity();
    }


//    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.activity = (WearActivity) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        activity = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                broadcastReceiver, new IntentFilter(RsvpView.ACTION_RSVP_EVENT));
        View view = inflater.inflate(R.layout.fr_rsvp_round, container, false);
        mPrevButton = view.findViewById(R.id.prev);
        mNextButton = view.findViewById(R.id.next);
        mDownButton = view.findViewById(R.id.record);
        mDownBgView = view.findViewById(R.id.record_bg);
        mRsvpView = (RsvpView) view.findViewById(R.id.rsvp);
        mPositionView = (TextView) view.findViewById(R.id.position);
        mPrevText = (TextView) view.findViewById(R.id.prev_text);
        mNextText = (TextView) view.findViewById(R.id.next_text);
        mChatMessageNewView = (TextView) view.findViewById(R.id.chat_message_new);
        mChatMessageTimeView = (TextView) view.findViewById(R.id.chat_message_time);
        mChatMessageStatusView = (ImageView) view.findViewById(R.id.chat_message_status);

        mChatMessageNewView.setOnClickListener(this);
        mPrevButton.setClickable(false);//mPrevButton.setOnClickListener(this);
        mNextButton.setClickable(false);//mNextButton.setOnClickListener(this);
        mDownButton.setClickable(false);//mDownButton.setOnClickListener(this);
        mDownButton.setLongClickable(false);//mDownButton.setOnLongClickListener(this);
        mDownBgView.setClickable(false);
        mDownBgView.setLongClickable(false);

        if (saved != null) {
            SSect s = activity.nav.getSectByGUID(saved.getString("rsvp.sect.guid"));
            if (s != null) {
                sect = s;
                cguid = saved.getString("rsvp.cguid.guid");
                state = saved.getInt("rsvp.state");
                flags = saved.getInt("rsvp.flags");
                updateButtons();
                fixupChildPosition();
                onRepeat(false);
            }
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (sect != null) {
            outState.putString("rsvp.sect.guid", sect.guid);
            outState.putString("rsvp.cguid.guid", cguid);
            outState.putInt("rsvp.state", state);
            outState.putInt("rsvp.flags", flags);
        }
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
            mRsvpView.stop(false);
    }

    private void setImageResource(View btn, int id) {
        if (id != 0)
            btn.setVisibility(View.VISIBLE);
        if (btn instanceof ImageView) {
            ((ImageView) btn).setImageResource(id);
        }
        else if (btn instanceof CircularButton) {
            if (id == 0)
                ((CircularButton) btn).setImageDrawable(null);
            else
                ((CircularButton) btn).setImageResource(0);
        }
    }

    private void setImageResource(View btn, String action) {
        if (action == null || action.isEmpty()) {
            setImageResource(btn, 0);
            return;
        }
        switch (action) {
        case "hello":     setImageResource(btn, R.drawable.ic_flare); return;
        case "show-menu":
            if ((flags & NAV_MODE) == NAV_CHAT)
                setImageResource(btn, R.drawable.ic_chat);
            else
                setImageResource(btn, R.drawable.ic_flare);
            return;
        case "edit":      setImageResource(btn, R.drawable.ic_record); return;
        case "record":    setImageResource(btn, R.drawable.ic_record); return;
        case "send":      setImageResource(btn, R.drawable.ic_send); return;
        case "play":      setImageResource(btn, R.drawable.ic_play); return;
        case "pause":     setImageResource(btn, R.drawable.ic_pause); return;
        case "replay":    setImageResource(btn, R.drawable.ic_replay); return;
        case "close":     setImageResource(btn, R.drawable.ic_close); return;
        case "return":    setImageResource(btn, R.drawable.ic_return); return;
        case "enter":     setImageResource(btn, R.drawable.ic_enter); return;
        case "read":      setImageResource(btn, R.drawable.ic_enter); return;
        case "list":      setImageResource(btn, R.drawable.ic_enter); return;
        case "next":      setImageResource(btn, R.drawable.ic_nav_next); return;
        case "prev":      setImageResource(btn, R.drawable.ic_nav_prev); return;
        case "auto-next-up": setImageResource(btn, R.drawable.ic_nav_next_over); return;
        case "auto-prev-up": setImageResource(btn, R.drawable.ic_nav_prev_over); return;
        default:          setImageResource(btn, R.drawable.ic_exec); return;
        }
    }

    private void setImageResource(View btn, UIAction action) {
        if (action == null)
            setImageResource(btn, 0);
        else
            setImageResource(btn, action.getAct());
    }

    private void updatePlayButton() {
        if (mRsvpView == null)
            return;
        RsvpView.RsvpState rsvpState = mRsvpView.getRsvpState();
        if (rsvpState == null || rsvpState.length == 0 || rsvpState.next_pos >= rsvpState.length)
            setImageResource(mNextButton, mNextAction);
        else if (!rsvpState.playing)
            setImageResource(mNextButton, R.drawable.ic_play);
        else
            setImageResource(mNextButton, R.drawable.ic_pause);
    }

    void updateButtons() {
        Navigator nav = activity.nav;
        if (nav == null) {
            mEnterAction = null;
            mLeaveAction = null;
            mNextAction = null;
            mPrevAction = null;
            mDownButton.setVisibility(View.INVISIBLE);
            mDownBgView.setVisibility(View.INVISIBLE);
            mPrevButton.setVisibility(View.INVISIBLE);
            mNextButton.setVisibility(View.INVISIBLE);
            return;
        }
        mEnterAction = nav.getUIAction(Navigator.DEFAULT_ACTION_ENTER);
        mLeaveAction = nav.getUIAction(Navigator.DEFAULT_ACTION_LEAVE);
        mNextAction = nav.getUIAction(Navigator.DEFAULT_ACTION_NEXT);
        mPrevAction = nav.getUIAction(Navigator.DEFAULT_ACTION_PREV);

        setImageResource(mDownButton, mEnterAction);
        if (mEnterAction != null) {
            mDownButton.setVisibility(View.VISIBLE);
            mDownBgView.setVisibility(View.VISIBLE);
        } else {
            mDownButton.setVisibility(View.INVISIBLE);
            mDownBgView.setVisibility(View.INVISIBLE);
        }

        setImageResource(mNextButton, mNextAction);
        if (mNextAction != null)
            mNextButton.setVisibility(View.VISIBLE);
        else
            mNextButton.setVisibility(View.INVISIBLE);

        setImageResource(mPrevButton, mPrevAction);
        if (mPrevAction != null)
            mPrevButton.setVisibility(View.VISIBLE);
        else
            mPrevButton.setVisibility(View.INVISIBLE);
    }

    public void load(SSect sect, int flags, boolean play) {
        if (this.sect == sect && sect != null)
            return;
        if (mRsvpView != null)
            mRsvpView.load(null, false);
        this.sect = sect;
        this.words = null;
        this.flags = flags;

        if ((flags & NAV_MODE) == NAV_CHAT && sect != null) {
            this.state = STATE_CHILD;
            this.cguid = CHILD_POS_END;
        } else {
            this.state = STATE_INIT;
            this.cguid = null;
        }

        updateButtons();
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

    public void fixupChildPosition() {
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
        }
        updatePosView();
    }

    public void stop(boolean clean) {
        this.words = null;
        if (mRsvpView != null)
            mRsvpView.stop(clean);
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
                    timestamp = Long.parseLong(child.entity.val("timestamp", "0"));
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
        if (sect.children != null && sect.children.length > 0) {
            int cnt = sect.children.length;
            int pos = sect.currListPosition;
            if (pos < 0) {
                mPrevText.setText("");
                mNextText.setText(Integer.toString(cnt));
            }
            else if (pos >= cnt) {
                mPrevText.setText(Integer.toString(cnt));
                mNextText.setText("");
            }
            else {
                mPrevText.setText(Integer.toString(pos+1));
                mNextText.setText(Integer.toString(cnt-pos-1));
            }
        }
        mPositionView.setText(getSpeedString());
    }

    private void updateActions() {
        updateButtons();
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
            mRsvpView.stop(true);
            mPositionView.setText(getSpeedString());
            return;
        }
        if (state == STATE_INIT || state == STATE_DONE) {
            mRsvpView.stop(true);
            mRsvpView.load(words, play);
            return;
        }
        if (state == STATE_TITLE) {
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            if (sect.descr != null)
                words.addIntroWords(sect.descr);
            if (sect.isValue)
                words.addValueWords(sect);
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
            if (pos < sect.children.length && pos > -1) {
                SSect child = sect.children[pos];
                words = new RsvpWords();
                words.addTitleWords(child.title);
                if (child.descr != null)
                    words.addIntroWords(child.descr);
                if (child.isValue)
                    words.addValueWords(child);
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
            mRsvpView.stop(true);
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
            if (sect.isValue)
                words.addValueWords(sect);
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
                if (child.isValue)
                    words.addValueWords(child);
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
                if (child.isValue)
                    words.addValueWords(child);
                mRsvpView.load(words, play);
            } else {
                cguid = CHILD_POS_END;
                sect.currListPosition = sect.children.length;
                mRsvpView.stop(false);
                updatePosView();
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
            mRsvpView.stop(true);
            mPositionView.setText("");
            updateActions();
            return;
        }
        //if (mRsvpView.isRewindable()) {
        //    mRsvpView.rewind();
        //    return;
        //}
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
                if (child.isValue)
                    words.addValueWords(child);
                mRsvpView.load(words, false);
                updateActions();
                return;
            }
            else if ((flags & NAV_MODE) == NAV_CHAT) {
                sect.currListPosition = -1;
                cguid = null;
                mRsvpView.stop(false);
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
            if (sect.isValue)
                words.addValueWords(sect);
            mRsvpView.load(words, false);
            updateActions();
            return;
        }
        state = STATE_INIT;
        cguid = null;
        mRsvpView.stop(false);
        updatePosView();
        updateActions();
    }

    @Override
    public void onSectionsChanged(SectionsModel model) {
        updatePosView();
    }


    private boolean doSimpleNav(Action action, boolean autoplay) {
        if (action == null || sect == null)
            return false;
        String act = action.getAction();
        if ("list".equals(act)) {
            if (sect.children != null && sect.children.length > 0)
                toChild(0, autoplay);
            return true;
        }
        else if ("read".equals(act)) {
            if (sect.hasArticle) {
                state = STATE_ARTICLE;
                onRepeat(autoplay);
            }
            return true;
        }
        else if ("descr".equals(act)) {
            state = STATE_TITLE;
            onRepeat(autoplay);
            return true;
        }
        else if ("next".equals(act)) {
            onNext(autoplay);
            return true;
        }
        else if ("prev".equals(act)) {
            onPrev();
            return true;
        }
        else if ("replay".equals(act)) {
            onRepeat(autoplay);
            return true;
        }
        return false;
    }

    private void doAction(int id, boolean button, boolean autoplay) {
        if (id == R.id.record) {
            if (mEnterAction != null) {
                if (!doSimpleNav(mEnterAction.action, autoplay))
                    activity.makeActionHandler(activity.nav, mEnterAction.action).run();
                return;
            }
        }
        if (id == R.id.title) {
            if (mLeaveAction != null) {
                if (!doSimpleNav(mLeaveAction.action, autoplay))
                    activity.makeActionHandler(activity.nav, mLeaveAction.action).run();
                return;
            }
        }
        if (id == R.id.prev) {
            if (!button && mRsvpView != null && mRsvpView.isRewindable()) {
                mRsvpView.rewind();
                return;
            }
            if (mPrevAction != null) {
                if (!doSimpleNav(mPrevAction.action, autoplay))
                    activity.makeActionHandler(activity.nav, mPrevAction.action).run();
                return;
            }
            onPrev();
        }
        if (id == R.id.next) {
            if (button && mRsvpView != null) {
                if (mRsvpView.isPlaying()) {
                    mRsvpView.pause();
                    return;
                }
                if (mRsvpView.isPaused()) {
                    mRsvpView.resume();
                    return;
                }
            }
            if (mNextAction != null) {
                if (!doSimpleNav(mNextAction.action, autoplay))
                    activity.makeActionHandler(activity.nav, mNextAction.action).run();
                return;
            }
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
    public void onClick(View v) {
        doAction(v.getId(), true, true);
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.record) {
            if (mEnterAction != null && "edit".equals(mEnterAction.action.getAction())) {
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

    public void onUpOrCancel(MotionEvent event, boolean cancel) {
        Log.d(TAG,"onUpOrCancel: "+(cancel?"cancel":"up")+event);
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
                doAction(R.id.next, false, false);
            else
                doAction(R.id.prev, false, false);
        }
        else if (vy > vx*5/4) {
            if (BtoT)
                doAction(R.id.title, false, false);
            else
                doAction(R.id.record, false, false);
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

        // check 'bottom round' and 'bottom flat' button pressed
        int r = 70;
        int cx = 480/2;
        int cy = 480-70+32;
        int d2 = (cx-x)*(cx-x) + (cy-y)*(cy-y);
        if (d2 < r*r || y > (480-94)) {
            if (mEnterAction != null && "edit".equals(mEnterAction.action.getAction())) {
                ((WearActivity) getActivity()).startVoiceRecording();
                return;
            }
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

        int x = (int)event.getX();
        int y = (int)event.getY();

        // check 'bottom round' and 'bottom flat' button pressed
        int r = 70;
        int cx = 480/2;
        int cy = 480-70+32;
        int d2 = (cx-x)*(cx-x) + (cy-y)*(cy-y);
        if (d2 < r*r || y > (480-94)) {
            doAction(R.id.record, true, true);
            return true;
        }

        // check 'next' button pressed
        if (y > 272 && y < (480-94) && x > 280) {
            doAction(R.id.next, true, true);
            return true;
        }

        // check 'prev' button pressed
        if (y > 272 && y < (480-94) && x < 200) {
            doAction(R.id.prev, true, true);
            return true;
        }

        return true;
    }

}
