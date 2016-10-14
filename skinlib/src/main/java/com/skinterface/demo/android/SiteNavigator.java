package com.skinterface.demo.android;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;

import com.skinterface.demo.android.lib.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
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
    // Stack of entered rooms
    private Stack<SSect> eneterPath = new Stack<>();

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

    @Override
    public void doHello(final NavClient client) {
        currentData = null;
        eneterPath.clear();
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

    @Override
    public void doShowMenu(final NavClient client) {
        if (wholeMenuTree == null)
            RootNavigator.get().doShowMenu(client);
        else
            client.showMenu(this, wholeMenuTree);
    }

    @Override
    public void doReturn(final NavClient client) {
    }

    public void doDefaultAction(final NavClient client, int dir) {
        //if (defaultAction != null)
        //    client.makeActionHandler(this, defaultAction.action).run();
    }

    @Override
    public void doUserInput(final NavClient client, String text) {
        SSect val = currArticle();
        if (val == null)
            return;
        if (val.getCurrChild() != null)
            val = val.getCurrChild();
        if (!val.isValue)
            return;
        if ("int".equals(val.entity.media)) {
            try {
                int res = Integer.parseInt(text);
                client.makeActionHandler(this, Action.create("set").add(val.entity.name, text)).run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if ("real".equals(val.entity.media)) {
            try {
                text = text.replace(',','.');
                double res = Double.parseDouble(text);
                client.makeActionHandler(this, Action.create("set").add(val.entity.name, text)).run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if ("date".equals(val.entity.media) || "datetime".equals(val.entity.media)) {
            try {
                client.makeActionHandler(this, Action.create("set").add(val.entity.name, text)).run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if ("geolocation".equals(val.entity.media)) {
            Action action = new Action("places-autocomplete").add("text", text);
            client.sendServerCmd(this, action, new SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    try {
                        JSONObject jobj = new JSONObject(result);
                        JSONArray jarr = jobj.getJSONArray("places");
                        // make menu list
                        ArrayList<SSect> menuList = new ArrayList<>();
                        for (int i = 0; i < jarr.length(); ++i) {
                            JSONObject jpi = jarr.getJSONObject(i);
                            SSect sect = SSect.makeMenu(jpi.getString("description"));
                            sect.entity.data = "place-selected";
                            sect.entity.padd("placeId", jpi.getString("placeId"));
                            menuList.add(sect);
                        }
                        SSect menu = SSect.makeMenu("Places");
                        menu.children = menuList.toArray(new SSect[menuList.size()]);
                        client.showMenu(SiteNavigator.this, menu);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            client.makeActionHandler(this, Action.create("set").add(val.entity.name, text)).run();
        }
    }

    public void doGeolocationDetails(final NavClient client, String text) {
        SSect val = currArticle();
        if (val == null)
            return;
        if (val.getCurrChild() != null)
            val = val.getCurrChild();
        if (!val.isValue)
            return;
        if (!"geolocation".equals(val.entity.media))
            return;

        PlaceInfo geocode = PlaceInfo.fromJson(text);
        String lat = geocode.latitude;
        String lon = geocode.longitude;
        Context context = (Context)client;
        String address = geocode.description + " ("+lat+"°"+context.getString(R.string.txt_edt_latitude)+" / "+lon+"°"+context.getString(R.string.txt_edt_longitude)+")";
        Action action = Action.create("set");
        action.add(val.entity.name + ".address", address);
        action.add(val.entity.name + ".latitude", lat);
        action.add(val.entity.name + ".longitude", lon);
        action.add(val.entity.name + ".timezone", geocode.timezone);
        try {
            JSONObject jobj = new JSONObject();
            jobj.put("address", address);
            jobj.put("latitude", lat);
            jobj.put("longitude", lon);
            val.entity.data = jobj.toString();
            action.add(val.entity.name, val.entity.data);
            client.makeActionHandler(this, action).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        if (nav.currentData != null && !TextUtils.equals(nav.currentData.guid, ds.guid))
                            nav.eneterPath.push(nav.currentData);
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
            if (ds == null || nav.eneterPath.isEmpty())
                return;
            SSect up = nav.eneterPath.peek();
            Action show = new Action("show").add("sectID", up.guid);
            if (up.isValue) {
                show.setAction("enter");
                show.add("vname", up.entity.name);
            }
            client.sendServerCmd(nav, show, new SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    SSect ds = SSect.fromJson(result);
                    SSect up = nav.eneterPath.pop();
                    if (ds == null) {
                        ds = up;
                    } else {
                        int lp = up.currListPosition + lp_delta;
                        if (lp < 0 || ds.children == null || lp >= ds.children.length) {
                            ds.currListPosition = -1;
                            if (lp_delta > 0 && !nav.eneterPath.isEmpty() && "auto-next-up".equals(action.getAction())) {
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
                    if (parent != null && !TextUtils.equals(parent.guid, ds.guid))
                        nav.eneterPath.add(parent);
                    nav.doEnterToRoom(client, ds);
                }
            });
        }
    }


    @Override
    public UIAction getUIAction(int dir) {
        SSect ds = currArticle();
        if (ds == null) {
            if (dir == DEFAULT_ACTION_ENTER)
                return new UIAction("Hello UpStars", "hello");
            return null;
        }
        if (ds.currListPosition < 0 || ds.children == null || ds.children.length == 0) {
            // not in child list, act on the main article
            if (dir == DEFAULT_ACTION_ENTER) {
                if (ds.isAction) {
                    UIAction action = new UIAction("Go to", ds.entity.data);
                    if (ds.entity.props != null) {
                        for (String key : ds.entity.props.keySet())
                            action.padd(key, ds.entity.val(key));
                    }
                    return action;
                }
                if (ds.isValue)
                    return new UIAction("Edit", "edit");
                if (ds.hasArticle)
                    return new UIAction("Read", "read");
                if (ds.isAction)
                    return new UIAction("Exec", "exec");
                if (ds.children != null && ds.children.length > 0)
                    return new UIAction("List", "list");
                return new UIAction("Guide", "show-menu");
            }
            if (dir == DEFAULT_ACTION_LEAVE) {
                if (!eneterPath.isEmpty())
                    return new UIAction("UP", "return-up");
            }
            if (dir == DEFAULT_ACTION_NEXT) {
                if (!eneterPath.isEmpty()) {
                    SSect up = eneterPath.peek();
                    SSect[] ch = up.children;
                    if (ch != null && up.currListPosition >= 0)
                        return new UIAction("UP+Next", "auto-next-up");
                }
            }
            if (dir == DEFAULT_ACTION_PREV) {
                if (!eneterPath.isEmpty()) {
                    SSect up = eneterPath.peek();
                    SSect[] ch = up.children;
                    if (ch != null && up.currListPosition >= 0)
                        return new UIAction("UP+Next", "auto-prev-up");
                }
            }
        }
        else if (ds.currListPosition >= ds.children.length) {
            // past the end of child list
            if (dir == DEFAULT_ACTION_LEAVE) {
                return new UIAction("Describe", "descr");
            }
            if (dir == DEFAULT_ACTION_NEXT) {
                if (!eneterPath.isEmpty()) {
                    SSect up = eneterPath.peek();
                    SSect[] ch = up.children;
                    if (ch != null && up.currListPosition >= 0)
                        return new UIAction("UP+Next", "auto-next-up");
                    else
                        return new UIAction("UP", "return-up");
                }
            }
            if (dir == DEFAULT_ACTION_PREV) {
                return new UIAction("Previous", "prev");
            }
        }
        else {
            SSect child = ds.getCurrChild();
            // in the middle of child list
            if (dir == DEFAULT_ACTION_ENTER) {
                if (child.isAction) {
                    UIAction action = new UIAction("Go to", child.entity.data);
                    if (child.entity.props != null) {
                        for (String key : child.entity.props.keySet())
                            action.padd(key, child.entity.val(key));
                    }
                    return action;
                }
                else if (child.isValue)
                    return new UIAction("Edit", "edit").padd("vname", child.entity.name);
                else if (child.hasArticle || child.hasChildren || child.children != null)
                    return new UIAction("Enter", "enter").padd("sectID", child.guid);
            }
            if (dir == DEFAULT_ACTION_LEAVE) {
                return new UIAction("Describe", "descr");
            }
            if (dir == DEFAULT_ACTION_NEXT) {
                if (ds.currListPosition < ds.children.length-1)
                    return new UIAction("Next", "next");
                if (!eneterPath.isEmpty()) {
                    SSect up = eneterPath.peek();
                    SSect[] ch = up.children;
                    if (ch != null && up.currListPosition >= 0 && !up.nextAsSkip)
                        return new UIAction("UP+Next", "auto-next-up");
                }
            }
            if (dir == DEFAULT_ACTION_PREV) {
                if (ds.currListPosition > 0)
                    return new UIAction("Prev", "prev");
                if (!eneterPath.isEmpty()) {
                    SSect up = eneterPath.peek();
                    SSect[] ch = up.children;
                    if (ch != null && up.currListPosition >= 0 && !up.nextAsSkip)
                        return new UIAction("UP+Next", "auto-prev-up");
                }
            }
        }
        return null;
    }
}
