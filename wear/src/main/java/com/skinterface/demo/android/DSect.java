package com.skinterface.demo.android;

import java.io.Serializable;

/**
 * Demo-Section
 */
public class DSect extends DEntity implements Serializable {

    public String guid; // section guid (unuque withing site/ontology)
    public boolean isAction; // the node is an action node
    public boolean isValue; // the node is a value node
    public boolean hasArticle; // the node has article text (maybe omited)
    public boolean hasChildren; // nodes's children were omited
    public boolean nextAsSkip; // 'next' means to skip article or child nodes (in unrelated/independant sub-articles)
    public DEntity title; // entity for section's title
    public DEntity descr; // entity for section's description
    public DSect[] children; // id,title,descr of children articles

    int currListPosition;
    DSect returnUp;

    DSect getCurrChild() {
        if (children == null || currListPosition < 0 || currListPosition >= children.length)
            return null;
        return children[currListPosition];
    }

    static DSect makeAction(String title, String action) {
        DSect ds = new DSect();
        ds.media = "action";
        ds.data = action;
        ds.title = new DSect();
        ds.title.media = "text";
        ds.title.role = "title";
        ds.title.data = title;
        return ds;
    }

    static DSect copyAction(DSect action) {
        DSect ds = new DSect();
        ds.copyFrom(action);
        ds.title = new DSect();
        if (action.title != null) {
            ds.title.copyFrom(action.title);
        } else {
            ds.title.media = "text";
            ds.title.role = "title";
            ds.title.data = "";
        }
        return ds;
    }

    public DSect prependTitle(String prefix) {
        if (title == null)
            return this;
        if (title.data != null)
            title.data = prefix + title.data;
        else
            title.data = prefix;
        return this;
    }

    public DSect padd(String name, Object value) {
        super.padd(name, value);
        return this;
    }

}
