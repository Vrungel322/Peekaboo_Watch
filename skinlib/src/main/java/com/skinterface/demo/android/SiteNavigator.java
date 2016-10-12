package com.skinterface.demo.android;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SiteNavigator implements Navigator {

    public static final String TAG = "SkinterPhone";

    private static final Map<String, SSectInfo> sectInfoMap = new ConcurrentHashMap<>();

    private String sessionID;

    class SSectInfo {
        int jr_flags;
    }

    // Loaded site menu
    private SSect wholeMenuTree;
    // Current data
    private SSect currentData;
    private int jr_flags;

    public interface Client extends NavClient {
        void attachToSite(SiteNavigator nav, SSect menu);
    }

    public SiteNavigator(Bundle saved) {
        if (saved != null) {
            sessionID = saved.getString("sessionID");
            if (saved.containsKey("menu"))
                wholeMenuTree = SSect.fromJson(saved.getString("menu"));
            if (saved.containsKey("curr"))
                currentData = SSect.fromJson(saved.getString("curr"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (sessionID != null)
            outState.putString("sessionID", sessionID);
        if (wholeMenuTree != null)
            outState.putString("menu", wholeMenuTree.fillJson(new JSONObject()).toString());
        if (currentData != null)
            outState.putString("curr", currentData.fillJson(new JSONObject()).toString());
    }

    public String getSessionID() {
        return sessionID;
    }

    @Override
    public SSect siteMenu() {
        return wholeMenuTree;
    }

    @Override
    public SSect currArticle() {
        return currentData;
    }

    @Override
    public int currChildrenCount() {
        if (currentData == null || currentData.children == null)
            return 0;
        return currentData.children.length;
    }

    @Override
    public SSect currChildren(int i) {
        if (currentData == null || currentData.children == null || i < 0 || i >= currentData.children.length)
            return null;
        return currentData.children[i];
    }

    @Override
    public SSect getSectByGUID(String guid) {
        if (guid == null)
            return null;
        if (currentData != null && guid.equals(currentData.guid))
            return currentData;
        return null;
    }

    @Override
    public void setJustShown(int jr_flags) {
        this.jr_flags = jr_flags;
        if (currentData == null || currentData.guid == null || currentData.guid.isEmpty())
            return;
        SSectInfo info = sectInfoMap.get(currentData.guid);
        if (info == null)
            sectInfoMap.put(currentData.guid, info=new SSectInfo());
        info.jr_flags |= jr_flags;
    }
    @Override
    public boolean isJustShown(int jr_flag) {
        return (this.jr_flags & jr_flag) != 0;
    }
    @Override
    public boolean isEverShown(int jr_flag) {
        if (currentData == null || currentData.guid == null)
            return false;
        SSectInfo info = sectInfoMap.get(currentData.guid);
        if (info == null)
            return false;
        return (info.jr_flags & jr_flags) != 0;
    }


    public void doHello(final NavClient client) {
        String lang = Locale.getDefault().getLanguage();
        Action hello = Action.create("hello");
        hello.add("lang", lang);
        client.sendServerCmd(this, hello, new SrvCallback() {
            @Override
            public void onSuccess(String result) {
                if (result == null || result.length() == 0)
                    return;
                Object obj;
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
                String nav_menu = client.isStory() ? "site-nav-menu-story" : "site-nav-menu";
                client.makeActionHandler(SiteNavigator.this, new Action("menu").add("sectID", nav_menu)).run();
            }
        });
    }

    private boolean guidMatchCurrent(SSect ds) {
        if (ds == null || ds.guid == null || ds.guid.isEmpty())
            return false;
        SSect cd = currentData;
        if (cd == null || cd.guid == null || cd.guid.isEmpty())
            return false;
        return ds.guid.equals(cd.guid);
    }

    public void doEnterToRoom(final NavClient client, SSect ds) {
        if (ds == null) {
            ds = new SSect();
            ds.title = new SEntity();
        }
        ds.currListPosition = -1;
        if (!guidMatchCurrent(ds))
            jr_flags = 0;
        currentData = ds;
        client.enterToRoom(this, ds, FLAG_SITE);
    }

    private void returnToRoom(final NavClient client, SSect ds) {
        if (ds == null) {
            ds = new SSect();
            ds.title = new SEntity();
        }
        if (!guidMatchCurrent(ds))
            jr_flags = 0;
        currentData = ds;
        client.returnToRoom(this, ds, FLAG_SITE);
    }

    public void doShowMenu(final NavClient client) {
        if (wholeMenuTree == null)
            RootNavigator.get().doShowMenu(client);
        else
            client.showMenu(this, wholeMenuTree);
    }

    public void doReturn(final NavClient client) {
    }

    public void doDefaultAction(final NavClient client) {
        if (defaultAction != null)
            client.makeActionHandler(this, defaultAction.action).run();
    }


    protected static class BaseActionHandler extends ActionHandler {
        final SiteNavigator nav;
        final Client client;
        protected BaseActionHandler(SiteNavigator nav, Client client, Action action) {
            super(nav, client, action);
            this.nav = nav;
            this.client = client;
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
                nav.doEnterToRoom(client, nav.currentData);
            }
            else if ("where".equals(act)) {
                client.showWhereAmIData(nav, nav.currentData, FLAG_SITE);
            }
            else if ("action".equals(act)) {
                if (nav.currentData.isAction) {
                    client.makeActionHandler(nav, nav.currentData.toAction()).run();
                }
            }
            else if ("set".equals(act)) {
                client.sendServerCmd(nav, action, new SrvCallback() {
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
                client.sendServerCmd(nav, action, new SrvCallback() {
                    @Override
                    public void onSuccess(String result) {
                        SSect ds = SSect.fromJson(result);
                        if (ds == null)
                            ds = new SSect();
                        nav.doEnterToRoom(client, ds);
                    }
                });
            }
            else if ("show-menu".equals(act)) {
                nav.doShowMenu(client);
            }
            else if ("menu".equals(act)) {
                client.sendServerCmd(nav, action, new SrvCallback() {
                    @Override
                    public void onSuccess(String result) {
                        SSect ds = SSect.fromJson(result);
                        nav.wholeMenuTree = ds;
                        client.attachToSite(nav, ds);
                    }
                });
            }
            else if ("hello".equals(act)) {
                nav.doHello(client);
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
            client.sendServerCmd(nav, show, new SrvCallback() {
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
                    nav.doEnterToRoom(client, ds);
                }
            });
        }
        void enterDown(final SSect parent, final Action action) {
            if (action.val("position") != null) {
                parent.currListPosition = Integer.parseInt(action.val("position"));
                action.del("position");
            }
            client.sendServerCmd(nav, action, new SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    SSect ds = SSect.fromJson(result);
                    if (ds == null)
                        ds = new SSect();
                    ds.returnUp = parent;
                    nav.doEnterToRoom(client, ds);
                }
            });
        }
    }


    private UIAction defaultAction;
    private ArrayList<UIAction> allActions = new ArrayList<>();

    @Override
    public void fillActions(NavClient client) {
        final SSect ds = currArticle();
        allActions = new ArrayList<>();
        if (ds == null) {
            defaultAction = new UIAction("Hello UpStars", "hello");
            return;
        }
        defaultAction = new UIAction("Guide", "show-menu");
        {
            if (ds.returnUp != null)
                allActions.add(new UIAction("Return Up", "return-up"));
            if (ds.returnUp != null && ds.currListPosition < 0) {
                SSect up = ds.returnUp;
                SSect[] ch = up.children;
                if (ch != null && up.currListPosition >= 0) {
                    //boolean has_prev = (up.currListPosition > 0);
                    //boolean has_next = (up.currListPosition < ch.length - 1);
                    //currActions.add(SSect.makeAction("PREV", has_prev ? "sibling-prev" : "none"));
                    //currActions.add(SSect.makeAction("NEXT", has_next ? "sibling-next" : "none"));
                    defaultAction = new UIAction("UP+Next", "auto-next-up");
                }
            }
            if (ds.descr != null)
                allActions.add(new UIAction("Describe", "descr"));
            if (ds.hasArticle) {
                if (!ds.nextAsSkip && !isJustShown(JR_ARTICLE))
                    defaultAction = new UIAction("Read", "read");
                //currActions.add(SSect.makeAction("Read", "read"));
            }
            if (ds.children != null && ds.children.length > 0) {
                if (!ds.nextAsSkip || ds.returnUp == null) {
                    if (!isJustShown(JR_LIST)) {
                        defaultAction = new UIAction("List", "list");
                    } else {
                        for (int position=0; position < ds.children.length; ++position) {
                            SSect child = ds.children[position];
                            if (child.isAction) {
                                defaultAction = new UIAction(child).padd("position", position);
                                break;
                            }
                            else if (child.hasArticle || child.hasChildren || child.children != null) {
                                defaultAction = new UIAction("Enter", "enter").padd("sectID", child.guid).padd("position", position);
                                break;
                            }
                        }
                    }
                }
                //if ((justRead & RsvpWords.JR_LIST) == 0)
                //    currActions.add(SSect.makeAction("List", "list"));
            }
            if (ds.isAction) {
                //currActions.add(ds);
                defaultAction = new UIAction(ds).prependTitle("Go to ");
            }
        }
        client.updateActions(this, allActions);
    }


    @Override
    public UIAction getDefaultUIAction(int dir) {
        if (dir == DEFAULT_ACTION_FORW)
            return defaultAction;
        return null;
    }

    @Override
    public List<UIAction> getUIActions() {
        return allActions;
    }
}
