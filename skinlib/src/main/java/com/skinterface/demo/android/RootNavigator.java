package com.skinterface.demo.android;

import android.os.Bundle;

import java.util.Collections;
import java.util.List;

public class RootNavigator implements Navigator {

    private static RootNavigator instance = new RootNavigator();

    private SSect chooseModelMenu;

    public static RootNavigator get() {
        return instance;
    }


    private RootNavigator () {
        chooseModelMenu = SSect.makeMenu("Site");
        chooseModelMenu.children = new SSect[] {
                SSect.makeAction("UpStars", "upstars"),
                SSect.makeAction("Peekaboo", "peekaboo"),
        };
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public SSect siteMenu() {
        return chooseModelMenu;
    }

    @Override
    public SSect currArticle() {
        return null;
    }

    @Override
    public int currChildrenCount() {
        return 0;
    }

    @Override
    public SSect currChildren(int i) {
        return null;
    }

    @Override
    public SSect getSectByGUID(String guid) {
        return null;
    }

    @Override
    public void setJustShown(int jr_flags) {
    }

    @Override
    public boolean isJustShown(int jr_flag) {
        return false;
    }

    @Override
    public boolean isEverShown(int jr_flag) {
        return false;
    }

    @Override
    public void doHello(NavClient client) {
        client.showMenu(this, chooseModelMenu);
    }

    @Override
    public void doShowMenu(NavClient client) {
        client.showMenu(this, chooseModelMenu);
    }

    @Override
    public void doReturn(NavClient client) {
    }

    @Override
    public void doUserInput(NavClient client, String text) {
    }

    @Override
    public UIAction getUIAction(int dir) {
        return null;
    }

}
