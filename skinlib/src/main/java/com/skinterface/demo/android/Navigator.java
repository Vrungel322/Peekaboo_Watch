package com.skinterface.demo.android;

import android.os.Bundle;

import java.util.List;

public interface Navigator {

    int FLAG_CAN_EDIT   = 0x1;
    int FLAG_CAN_SEND   = 0x2;
    int FLAG_CAN_ABORT  = 0x4;
    int FLAG_CAN_RETURN = 0x8;

    int FLAG_CHAT       = 0x100;
    int FLAG_SITE       = 0x200;

    // Action for right button (when it's not in 'play/pause' state) and swipe right-to-left action
    int DEFAULT_ACTION_NEXT  = 0;
    // Action for left button (when it's not in 'more/menu' state) and swipe left-to-right action
    int DEFAULT_ACTION_PREV  = 1;
    // Action for down button and swipe top-to-down action
    int DEFAULT_ACTION_ENTER = 2;
    // Action for up/return button and swipe down-to-top action
    int DEFAULT_ACTION_LEAVE = 3;

    // Just-read Title
    public static final int JR_TITLE   = 0x0001;
    // Just-read Intro
    public static final int JR_INTRO   = 0x0002;
    // Just-read Article
    public static final int JR_ARTICLE = 0x0004;
    // Just-read Value
    public static final int JR_VALUE   = 0x0008;
    // Just-read Warning
    public static final int JR_WARNING = 0x0010;
    // Just-read Menu
    public static final int JR_MENU    = 0x0020;
    // Just-read list of children
    public static final int JR_LIST    = 0x0040;


    interface NavClient {
        boolean isStory();
        ActionHandler makeActionHandler(Navigator nav, Action action);
        void showMenu(Navigator nav, SSect menu);
        void enterToRoom(Navigator nav, SSect sect, int flags);
        void returnToRoom(Navigator nav, SSect sect, int flags);
        void showWhereAmIData(Navigator nav, SSect sect, int flags);

        void updateActions(Navigator nav, List<UIAction> actions);
        void sendServerCmd(Navigator nav, Action action, SrvCallback callback);
    }

    void onSaveInstanceState(Bundle outState);

    SSect siteMenu();
    SSect currArticle();
    int   currChildrenCount();
    SSect currChildren(int i);
    SSect getSectByGUID(String guid);

    // set 'just-shown' flags for current article
    void setJustShown(int jr_flags);
    // check 'just-shown' flag for current article
    boolean isJustShown(int jr_flag);
    // check if any element of article was shown before
    boolean isEverShown(int jr_flag);

    void doHello(NavClient client);
    void doShowMenu(NavClient client);
    void doReturn(NavClient client);
    void doUserInput(NavClient client, String text);

    UIAction getUIAction(int dir);
}
