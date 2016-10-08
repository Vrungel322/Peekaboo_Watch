package com.skinterface.demo.android;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ChatNavigator implements Navigator {

    public static final String TAG = "SkinterPhone";

    final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    final static Handler handler = new Handler(Looper.getMainLooper());

    public final SrvClient client;

    String userID;
    String userName;

    // Loaded site menu
    SSect wholeMenuTree;

    public interface SrvClient {
        void attachToChat(JSONObject jobj);
        void showMenu(SSect menu);
        void chatServerCmd(Action action, ChatNavigator nav, SrvCallback callback);
    }

    public ChatNavigator(ChatNavigator.SrvClient client) {
        this.client = client;
    }

    @Override
    public void doHello() {
        Action attach = Action.create("attach");
        client.chatServerCmd(attach, this, new SrvCallback() {
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
                wholeMenuTree = SSect.makeMenu("Peekaboo");
                try {
                    JSONObject jobj = (JSONObject) obj;
                    userID = jobj.getString("id");
                    userName = jobj.getString("name");
                    ArrayList<SSect> children = new ArrayList<>();
                    JSONArray jarr = jobj.getJSONArray("contacts");
                    for (int i=0; i < jarr.length(); ++i) {
                        JSONObject jc = jarr.getJSONObject(i);
                        String id = jc.getString("id");
                        String name = jc.getString("name");
                        children.add(SSect.makeAction(name, "chat").padd("room", id));
                    }
                    wholeMenuTree.children = children.toArray(new SSect[0]);
                    client.attachToChat(jobj);
                } catch (Exception e) {
                    Log.e(TAG, "Error in parsing peekaboo attach info", e);
                }
            }
        });
    }

    @Override
    public void doShowMenu() {
        if (wholeMenuTree == null)
            client.showMenu(SiteNavigator.chooseModelMenu);
        else
            client.showMenu(wholeMenuTree);
    }
}
