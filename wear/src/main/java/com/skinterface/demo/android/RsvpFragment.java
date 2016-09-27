package com.skinterface.demo.android;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class RsvpFragment extends Fragment implements RsvpView.RsvpViewListener, View.OnClickListener {

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.fr_rsvp_round, container, false);
        view.findViewById(R.id.cards).setOnClickListener(this);
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
        if (v.getId() == R.id.cards) {
            WearActivity activity = (WearActivity)getActivity();
            activity.startCardsActivity();
        }
    }
}
