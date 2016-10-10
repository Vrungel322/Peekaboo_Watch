package com.skinterface.demo.android;

public interface Navigator {

    public static final int FLAG_CAN_EDIT   = 0x1;
    public static final int FLAG_CAN_SEND   = 0x2;
    public static final int FLAG_CAN_ABORT  = 0x4;
    public static final int FLAG_CAN_RETURN = 0x8;

    public static final int FLAG_CHAT       = 0x100;

    SSect currArticle();
    int   currChildrenCount();
    SSect currChildren(int i);

    void doHello();
    void doShowMenu();
    void doReturn();
}
