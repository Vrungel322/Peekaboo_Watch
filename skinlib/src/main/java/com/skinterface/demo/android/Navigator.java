package com.skinterface.demo.android;

public interface Navigator {

    SSect currArticle();
    int   currChildrenCount();
    SSect currChildren(int i);

    void doHello();
    void doShowMenu();
    void doReturn();
}
