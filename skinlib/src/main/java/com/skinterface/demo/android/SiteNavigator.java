package com.skinterface.demo.android;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class SiteNavigator implements Action.ActionExecutor {

    public static final String TAG = "SkinterPhone";

    final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    final static Handler handler = new Handler(Looper.getMainLooper());

    static final SSect chooseModelMenu;
    static {
        chooseModelMenu = SSect.makeMenu("Site");
        chooseModelMenu.children = new SSect[] {
                SSect.makeAction("UpStars", "upstars"),
                SSect.makeAction("Peekaboo", "peekaboo"),
        };
    }


    public final SrvClient client;

    String sessionID;

    // Loaded site menu
    SSect wholeMenuTree;
    // Current data
    SSect currentData;

    public interface SrvCallback {
        void onSuccess(String result);
    }
    public interface SrvClient {
        ActionHandler makeActionHandler(Action action);
        void enterToRoom(SSect sect);
        void returnToRoom(SSect sect);
        void showWhereAmIData(SSect sect);
        void showMenu(SSect menu);
        void serverCmd(Action action, SiteNavigator.SrvCallback callback);
    }
    public static abstract class ActionHandler implements Runnable {
        public final SiteNavigator nav;
        public final SrvClient client;
        public final Action action;
        protected ActionHandler(SiteNavigator nav, SrvClient client, Action action) {
            this.nav = nav;
            this.client = client;
            this.action = action;
        }
        public abstract void run();
    }

    public SiteNavigator(SrvClient client) {
        this.client = client;
    }

    @Override
    public final void executeAction(Action action) {
        client.makeActionHandler(action).run();
    }

    public final ActionHandler makeActionHandler(SEntity entity) {
        Action action = Action.create(entity.data);
        if (entity.props != null) {
            for (String key : entity.props.keySet())
                action.add(key, entity.props.get(key));
        }
        return client.makeActionHandler(action);
    }

    public void doHello() {
        String lang = Locale.getDefault().getLanguage();
        Action hello = Action.create("hello");
        hello.add("lang", lang);
        client.serverCmd(hello, new SiteNavigator.SrvCallback() {
            @Override
            public void onSuccess(String result) {
                if (result == null || result.length() == 0)
                    return;
                Object obj = null;
                try {
                    JSONTokener tokener = new JSONTokener(result);
                    obj = tokener.nextValue();
                } catch (JSONException e) {
                    return;
                }
                if (!(obj instanceof JSONObject) || obj == JSONObject.NULL)
                    return;
                JSONObject jobj = (JSONObject) obj;
                String id = jobj.optString("session");
                if (sessionID == null || !sessionID.equals(result))
                    sessionID = id;
                executeAction(new Action("menu").add("sectID", "site-nav-menu"));
                executeAction(new Action("home"));
            }
        });
    }

    public void doEnterToRoom(SSect ds) {
        if (ds == null) {
            ds = new SSect();
            ds.title = new SEntity();
        }
        if (ds.children != null && ds.children.length == 0)
            ds.children = null;
        ds.currListPosition = -1;
        currentData = ds;
        client.enterToRoom(ds);
    }

    private void returnToRoom(SSect ds) {
        if (ds == null) {
            ds = new SSect();
            ds.title = new SEntity();
        }
        currentData = ds;
        client.returnToRoom(ds);
    }

    public void doShowMenu() {
        if (wholeMenuTree == null)
            client.showMenu(chooseModelMenu);
        else
            client.showMenu(wholeMenuTree);
    }


    protected static class BaseActionHandler extends ActionHandler {
        public BaseActionHandler(SiteNavigator nav, SrvClient client, Action action) {
            super(nav, client, action);
        }

        @Override
        public void run() {
            String act = action.getAction();
            if ("enter".equals(act)) {
                enterDown(nav.currentData, action);
            }
            else if ("return-up".equals(act)) {
                returnUp(nav.currentData, 0);
            }
            else if ("auto-next-up".equals(act)) {
                returnUp(nav.currentData, +1);
            }
            else if ("sibling-next".equals(act)) {
                returnUp(nav.currentData, +1);
            }
            else if ("sibling-prev".equals(act)) {
                returnUp(nav.currentData, -1);
            }
            else if ("stop".equals(act)) {
                if (nav.currentData != null)
                    nav.currentData.currListPosition = -1;
                nav.doEnterToRoom(nav.currentData);
            }
            else if ("where".equals(act)) {
                client.showWhereAmIData(nav.currentData);
            }
            else if ("action".equals(act)) {
                if (nav.currentData.isAction) {
                    nav.makeActionHandler(nav.currentData.entity).run();
                }
            }
            else if ("set".equals(act)) {
                client.serverCmd(action, new SiteNavigator.SrvCallback() {
                    @Override
                    public void onSuccess(String result) {
                        //if (result != null)
                        //    executeAction(result);
                    }
                });
            }
            else if ("show".equals(act) || "home".equals(act) || "update".equals(act)) {
                if (action.val("position") != null) {
                    if (nav.currentData != null)
                        nav.currentData.currListPosition = Integer.parseInt(action.val("position"));
                    action.del("position");
                }
                client.serverCmd(action, new SiteNavigator.SrvCallback() {
                    @Override
                    public void onSuccess(String result) {
                        SSect ds = SSect.fromJson(result);
                        if (ds == null)
                            ds = new SSect();
                        nav.doEnterToRoom(ds);
                    }
                });
            }
            else if ("show-menu".equals(act)) {
                nav.doShowMenu();
            }
            else if ("menu".equals(act)) {
                client.serverCmd(action, new SiteNavigator.SrvCallback() {
                    @Override
                    public void onSuccess(String result) {
                        SSect ds = SSect.fromJson(result);
                        nav.wholeMenuTree = ds;
                    }
                });
            }
            else if ("hello".equals(act)) {
                nav.doHello();
            }
        }

        void returnUp(final SSect ds, final int lp_delta) {
            if (ds == null || ds.returnUp == null)
                return;
            final SSect up = ds.returnUp;
            Action show = new Action("show").add("sectID", up.guid);
            if (up.isValue) {
                show.setAction("enter");
                show.add("vname", up.entity.name);
            }
            client.serverCmd(show, new SiteNavigator.SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    SSect ds = SSect.fromJson(result);
                    if (ds == null) {
                        ds = up;
                    } else {
                        int lp = up.currListPosition + lp_delta;
                        ds.returnUp = up.returnUp;
                        if (lp < 0 || ds.children == null || lp >= ds.children.length) {
                            ds.currListPosition = -1;
                            if (lp_delta > 0 && ds.returnUp != null && "auto-next-up".equals(action.getAction())) {
                                returnUp(ds, lp_delta);
                                return;
                            }
                        } else {
                            ds.currListPosition = lp;
                            if (lp_delta != 0) {
                                if (!ds.children[lp].nextAsSkip) {
                                    SSect child = ds.children[lp];
                                    Action action = new Action("enter").add("sectID", child.guid);
                                    if (child.isValue)
                                        action.add("vname", child.entity.name);
                                    enterDown(ds, action);
                                    return;
                                }
                            }
                        }
                    }
                    nav.doEnterToRoom(ds);
                }
            });
        }
        void enterDown(final SSect parent, final Action action) {
            if (action.val("position") != null) {
                parent.currListPosition = Integer.parseInt(action.val("position"));
                action.del("position");
            }
            client.serverCmd(action, new SiteNavigator.SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    SSect ds = SSect.fromJson(result);
                    if (ds == null)
                        ds = new SSect();
                    ds.returnUp = parent;
                    nav.doEnterToRoom(ds);
                }
            });
        }
    }
}
