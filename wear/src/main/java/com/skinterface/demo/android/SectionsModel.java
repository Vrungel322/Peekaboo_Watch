package com.skinterface.demo.android;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class SectionsModel {

    public interface SectionsListener {
        void onSectionsChanged();
    }

    public static final SectionsModel instance = new SectionsModel();

    private final CopyOnWriteArrayList<SSect> sections = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<SectionsListener> listeners = new CopyOnWriteArrayList<>();

    public void addSectionsListener(SectionsListener l) {
        if (l != null && !listeners.contains(l))
            listeners.add(l);
    }

    public void removeSectionsListener(SectionsListener l) {
        if (l != null)
            listeners.remove(l);
    }

    public void addSection(SSect sect) {
        sections.add(sect);
        for (SectionsListener l : listeners)
            l.onSectionsChanged();
    }

    public int size() {
        return sections.size();
    }

    public SSect get(int i) {
        return sections.get(i);
    }

    public SSect last() {
        return sections.get(sections.size()-1);
    }

    public List<SSect> getSections() {
        return Collections.unmodifiableList(sections);
    }

}
