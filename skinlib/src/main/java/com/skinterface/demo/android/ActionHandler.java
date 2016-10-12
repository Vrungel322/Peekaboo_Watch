package com.skinterface.demo.android;

public abstract class ActionHandler implements Runnable {
    protected final Navigator navigator;
    protected final Navigator.NavClient navclient;
    protected final Action action;

    protected ActionHandler(Navigator nav, Navigator.NavClient client, Action action) {
        this.navigator = nav;
        this.navclient = client;
        this.action = action;
    }

    public abstract void run();
}
