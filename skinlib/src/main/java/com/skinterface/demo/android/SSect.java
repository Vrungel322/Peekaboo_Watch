package com.skinterface.demo.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.Serializable;

/**
 * Server-Section
 */
public class SSect implements Serializable {

    final
    public SEntity entity = new SEntity(); // entity for this section
    public String  guid; // section guid (unuque withing site/ontology)
    public boolean isAction; // the node is an action node
    public boolean isValue; // the node is a value node
    public boolean hasArticle; // the node has article text (maybe omited)
    public boolean hasChildren; // nodes's children were omited
    public boolean nextAsSkip; // 'next' means to skip article or child nodes (in unrelated/independant sub-articles)
    public SEntity title; // entity for section's title
    public SEntity descr; // entity for section's description
    public SSect[] children; // id,title,descr of children articles

    int currListPosition = -1;
    SSect returnUp;

    SSect getCurrChild() {
        if (children == null || currListPosition < 0 || currListPosition >= children.length)
            return null;
        return children[currListPosition];
    }

    static SSect fromJson(String json) {
        if (json == null || json.length() == 0)
            return null;
        JSONTokener tokener = new JSONTokener(json);
        Object obj = null;
        try { obj = tokener.nextValue(); } catch (JSONException e) {}
        if (!(obj instanceof JSONObject) || obj == JSONObject.NULL)
            return null;
        return SSect.fromJson((JSONObject) obj);
    }

    static SSect fromJson(JSONObject jobj) {
        SSect ds = new SSect();
        try {
            SEntity.fromJson(jobj.getJSONObject("entity"), ds.entity);
            if (jobj.has("guid"))
                ds.guid = jobj.getString("guid");
            if (jobj.has("isAction"))
                ds.isAction = jobj.getBoolean("isAction");
            if (jobj.has("isValue"))
                ds.isValue = jobj.getBoolean("isValue");
            if (jobj.has("hasArticle"))
                ds.hasArticle = jobj.getBoolean("hasArticle");
            if (jobj.has("hasChildren"))
                ds.hasChildren = jobj.getBoolean("hasChildren");
            if (jobj.has("nextAsSkip"))
                ds.nextAsSkip = jobj.getBoolean("nextAsSkip");
            if (jobj.has("title"))
                ds.title = SEntity.fromJson(jobj.getJSONObject("title"), null);
            if (jobj.has("descr"))
                ds.descr = SEntity.fromJson(jobj.getJSONObject("descr"), null);
            if (jobj.has("children")) {
                JSONArray jchildren = jobj.getJSONArray("children");
                ds.children = new SSect[jchildren.length()];
                for (int i = 0; i < jchildren.length(); ++i)
                    ds.children[i] = SSect.fromJson(jchildren.getJSONObject(i));
            }
        } catch (JSONException e) {}
        return ds;
    }

    public JSONObject fillJson(JSONObject jobj) {
        try {
            jobj.put("entity", entity.fillJson(new JSONObject()));
            if (guid != null)
                jobj.put("guid", guid);
            if (isAction)
                jobj.put("isAction", isAction);
            if (isValue)
                jobj.put("isValue", isValue);
            if (hasArticle)
                jobj.put("hasArticle", hasArticle);
            if (hasChildren)
                jobj.put("hasChildren", hasChildren);
            if (nextAsSkip)
                jobj.put("nextAsSkip", nextAsSkip);
            if (title != null)
                jobj.put("title", title.fillJson(new JSONObject()));
            if (descr != null)
                jobj.put("descr", descr.fillJson(new JSONObject()));
            if (children != null) {
                JSONArray jchildren = new JSONArray();
                for (SSect c : children) {
                    if (c == null)
                        jchildren.put(JSONObject.NULL);
                    else
                        jchildren.put(c.fillJson(new JSONObject()));
                }
                jobj.put("children", jchildren);
            }
        } catch (JSONException e) {}
        return jobj;
    }

    public SSect padd(String name, Object value) {
        entity.padd(name, value);
        return this;
    }

    static SSect makeAction(String title, String action) {
        SSect ds = new SSect();
        ds.entity.media = "action";
        ds.entity.data = action;
        ds.title = new SEntity();
        ds.title.media = "text";
        ds.title.role = "title";
        ds.title.data = title;
        return ds;
    }

    static SSect copyAction(SSect action) {
        SSect ds = new SSect();
        ds.entity.copyFrom(action.entity);
        ds.title = new SEntity();
        if (action.title != null) {
            ds.title.copyFrom(action.title);
        } else {
            ds.title.media = "text";
            ds.title.role = "title";
            ds.title.data = "";
        }
        return ds;
    }

    public SSect prependTitle(String prefix) {
        if (title == null)
            return this;
        if (title.data != null)
            title.data = prefix + title.data;
        else
            title.data = prefix;
        return this;
    }

    static SSect makeMenu(String title) {
        SSect ds = new SSect();
        ds.entity.media = "none";
        ds.title = new SEntity();
        ds.title.media = "text";
        ds.title.role = "title";
        ds.title.data = title;
        return ds;
    }

    public Action toAction() {
        Action action = Action.create(entity.data);
        if (entity.props != null) {
            for (String key : entity.props.keySet())
                action.add(key, entity.props.get(key));
        }
        return action;
    }

    public String toString() {
        if (title != null)
            return title.data;
        return "";
    }

}
