package com.skinterface.demo.android;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class RsvpFragment extends Fragment implements RsvpView.RsvpViewListener, View.OnClickListener {

    RsvpView mRsvpView;

    SSect sect;
    RsvpWords words;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle saved) {
        View view = inflater.inflate(R.layout.fr_rsvp_round, container, false);
        view.findViewById(R.id.cards).setOnClickListener(this);
        view.findViewById(R.id.next).setOnClickListener(this);
        mRsvpView = (RsvpView) view.findViewById(R.id.rsvp);
        mRsvpView.setListener(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sect != null) {
            RsvpWords words = new RsvpWords();
            words.addTitleWords(sect.title);
            words.addIntroWords(sect.descr);
            if (sect.hasArticle)
                words.addArticleWords(sect.entity);
            mRsvpView.play(words);
        }
    }

    public void play(SSect sect) {
        this.sect = sect;
        this.words = null;
        if (sect != null) {
            words = new RsvpWords();
            words.addTitleWords(sect.title);
            words.addIntroWords(sect.descr);
            if (sect.hasArticle)
                words.addArticleWords(sect.entity);
        }
        if (mRsvpView != null) {
            if (words == null)
                mRsvpView.stop(null);
            else
                mRsvpView.play(words);
        }
    }

    public void stop() {
        this.sect = null;
        this.words = null;
        if (mRsvpView != null)
            mRsvpView.stop(null);
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
            play(sect);
        }
        if (v.getId() == R.id.cards) {
            WearActivity activity = (WearActivity)getActivity();
            activity.startCardsActivity();
        }
    }
}
