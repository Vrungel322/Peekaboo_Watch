package com.skinterface.demo.android;

import org.json.JSONObject;

public class UpStarsSectionsModel extends SectionsModel {

    private static final UpStarsSectionsModel model = new UpStarsSectionsModel();

    public static UpStarsSectionsModel get() { return model; }

    private static String sessionID;
    private static SSect site_menu;
    private static SSect curr_sect;

    private UpStarsSectionsModel() {
    }

    public void setMenu(JSONObject jobj) {
        site_menu = SSect.fromJson(jobj);
        sessionID = site_menu.entity.val("session");
    }

    public SSect getMenu() {
        return site_menu;
    }

    public String getSessionId() {
        return sessionID;
    }

    public SSect currArticle() {
        return curr_sect;
    }

    public int size() {
        if (curr_sect == null || curr_sect.children == null)
            return 0;
        return curr_sect.children.length;
    }

    public SSect get(int i) {
        if (curr_sect == null || curr_sect.children == null || i < 0 || i >= curr_sect.children.length)
            return null;
        return curr_sect.children[i];
    }

    public void enterRoom(SSect room) {
        curr_sect = room;
        notifyDataChanged();
    }

}
