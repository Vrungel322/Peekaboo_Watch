package com.skinterface.demo.android;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class SectionsModel {

    public interface SectionsListener {
        void onSectionsChanged(SectionsModel model);
    }

    private final CopyOnWriteArrayList<SectionsListener> listeners = new CopyOnWriteArrayList<>();

    public void addSectionsListener(SectionsListener l) {
        if (l != null && !listeners.contains(l))
            listeners.add(l);
    }

    public void removeSectionsListener(SectionsListener l) {
        if (l != null)
            listeners.remove(l);
    }

    protected void notifyDataChanged() {
        for (SectionsListener l : listeners) {
            if (l != null)
                l.onSectionsChanged(this);
        }
    }

    public abstract SSect currArticle();

    public abstract int size();

    public abstract SSect get(int i);

}

