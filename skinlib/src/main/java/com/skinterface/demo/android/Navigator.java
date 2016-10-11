package com.skinterface.demo.android;

public interface Navigator {

    int FLAG_CAN_EDIT   = 0x1;
    int FLAG_CAN_SEND   = 0x2;
    int FLAG_CAN_ABORT  = 0x4;
    int FLAG_CAN_RETURN = 0x8;

    int FLAG_CHAT       = 0x100;
    int FLAG_SITE       = 0x200;

    SSect currArticle();
    int   currChildrenCount();
    SSect currChildren(int i);

    void doHello();
    void doShowMenu();
    void doReturn();
}
