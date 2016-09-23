package com.skinterface.demo.android;

import com.google.gson.*;

/**
 * Server-Section
 */
class SSect {

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

    int currListPosition;
    SSect returnUp;

    SSect getCurrChild() {
        if (children == null || currListPosition < 0 || currListPosition >= children.length)
            return null;
        return children[currListPosition];
    }

    static SSect fromJson(String json) {
        if (json == null)
            return null;
        JsonElement jel = new JsonParser().parse(json);
        if (!(jel.isJsonObject()))
            return null;
        return SSect.fromJson(jel.getAsJsonObject());
    }

    static SSect fromJson(JsonObject jobj) {
        SSect ds = new SSect();
        SEntity.fromJson(jobj.get("entity").getAsJsonObject(), ds.entity);
        if (jobj.has("guid"))
            ds.guid = jobj.get("guid").getAsString();
        if (jobj.has("isAction"))
            ds.isAction = jobj.get("isAction").getAsBoolean();
        if (jobj.has("isValue"))
            ds.isValue = jobj.get("isValue").getAsBoolean();
        if (jobj.has("hasArticle"))
            ds.hasArticle = jobj.get("hasArticle").getAsBoolean();
        if (jobj.has("hasChildren"))
            ds.hasChildren = jobj.get("hasChildren").getAsBoolean();
        if (jobj.has("nextAsSkip"))
            ds.nextAsSkip = jobj.get("nextAsSkip").getAsBoolean();
        if (jobj.has("title"))
            ds.title = SEntity.fromJson(jobj.get("title").getAsJsonObject(), null);
        if (jobj.has("descr"))
            ds.descr = SEntity.fromJson(jobj.get("descr").getAsJsonObject(), null);
        if (jobj.has("children")) {
            JsonArray jchildren = jobj.get("children").getAsJsonArray();
            ds.children = new SSect[jchildren.size()];
            for (int i=0; i < jchildren.size(); ++i)
                ds.children[i] = SSect.fromJson(jchildren.get(i).getAsJsonObject());
        }
        return ds;
    }

    public JsonObject fillJson(JsonObject jobj) {
        jobj.add("entity", entity.fillJson(new JsonObject()));
        if (guid != null)
            jobj.addProperty("guid", guid);
        if (isAction)
            jobj.addProperty("isAction", isAction);
        if (isValue)
            jobj.addProperty("isValue", isValue);
        if (hasArticle)
            jobj.addProperty("hasArticle", hasArticle);
        if (hasChildren)
            jobj.addProperty("hasChildren", hasChildren);
        if (nextAsSkip)
            jobj.addProperty("nextAsSkip", nextAsSkip);
        if (title != null)
            jobj.add("title", title.fillJson(new JsonObject()));
        if (descr != null)
            jobj.add("descr", descr.fillJson(new JsonObject()));
        if (children != null) {
            JsonArray jchildren = new JsonArray();
            for (SSect c : children) {
                if (c == null)
                    jchildren.add(JsonNull.INSTANCE);
                else
                    jchildren.add(c.fillJson(new JsonObject()));
            }
            jobj.add("children", jchildren);
        }
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

    public String toString() {
        if (title != null)
            return title.data;
        return "";
    }

}
