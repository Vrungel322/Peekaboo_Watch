package com.skinterface.demo.android;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ChatNavigator implements Navigator {

    public static final String TAG = "SkinterPhone";

    private static HashMap<String, SSect> chatRooms = new HashMap<>();
    private static SSect wholeMenuTree;

    private String userID;
    private String userName;

    // Current chat room
    private SSect chat_room;
    // Current section (maybe the room, or sub-sections for new messages)
    private SSect currentData;

    interface Client extends NavClient {
        void sendChatConfirm(final String sender, final String receiver, final String text);
        void sendVoiceConfirm(final String sender, final String receiver);
    }

    public ChatNavigator(Bundle saved) {
        if (saved != null) {
            userID = saved.getString("userID");
            userName = saved.getString("userName");
            if (saved.containsKey("chatRoom")) {
                chat_room = chatRooms.get(saved.getString("chatRoom"));
                if (chat_room != null)
                    chat_room.currListPosition = saved.getInt("chatPos");
                currentData = chat_room;
            }
            if (saved.containsKey("composing")) {
                currentData = SSect.fromJson(saved.getString("composing"));
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (userID != null) {
            outState.putString("userID", userID);
            outState.putString("userName", userName);
        }
        if (chat_room != null) {
            outState.putString("chatRoom", chat_room.entity.name);
            outState.putInt("chatPos", chat_room.currListPosition);
        }
        if (currentData != currentData) {
            outState.putString("composing", currentData.fillJson(new JSONObject()).toString());
        }
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
        if (guid == null || chat_room == null)
            return null;
        if (currentData != null && guid.equals(currentData.guid))
            return currentData;
        if (guid.equals(chat_room.guid))
            return chat_room;
        for (SSect msg : chat_room.children) {
            if (guid.equals(msg.guid))
                return msg;
        }
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


    public String getUserId() {
        return userID;
    }

    public String getUserName() {
        return userName;
    }

    public String getPartnerId() {
        return chat_room == null ? null : chat_room.entity.name;
    }

    public String getPartnerName() {
        return chat_room == null ? null : chat_room.title.data;
    }


    @Override
    public void doHello(final NavClient client) {
        Action attach = Action.create("attach");
        client.sendServerCmd(this, attach, new SrvCallback() {
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
                HashMap<String,String> predefined = new HashMap<>();
                predefined.put("121", "AidenEagleton");
                predefined.put("202", "JohnEagleton");
                predefined.put("294", "PeekabooChat");
                predefined.put("629", "userone");
                predefined.put("642", "Future");
                wholeMenuTree = SSect.makeMenu("Peekaboo");
                try {
                    JSONObject jobj = (JSONObject) obj;
                    userID = jobj.getString("id").intern();
                    userName = jobj.getString("name");
                    ArrayList<SSect> children = new ArrayList<>();
                    JSONArray jarr = jobj.getJSONArray("contacts");
                    for (int i=0; i < jarr.length(); ++i) {
                        JSONObject jc = jarr.getJSONObject(i);
                        String id = jc.getString("id");
                        String name = jc.getString("name");
                        children.add(SSect.makeAction(name, "chat").padd("room", id));
                        makeChatRoom(id, name);
                        predefined.remove(id);
                    }
                    for (String id : predefined.keySet()) {
                        String name = predefined.get(id);
                        children.add(SSect.makeAction(name, "chat").padd("room", id));
                        makeChatRoom(id, name);
                    }
                    wholeMenuTree.children = children.toArray(new SSect[0]);
                    client.showMenu(ChatNavigator.this, wholeMenuTree);
                } catch (Exception e) {
                    Log.e(TAG, "Error in parsing peekaboo attach info", e);
                }
            }
        });
    }

    @Override
    public void doShowMenu(final NavClient client) {
        if (wholeMenuTree == null)
            RootNavigator.get().doShowMenu(client);
        else
            client.showMenu(this, wholeMenuTree);
    }

    public SSect doEnterToRoom(final NavClient client, String partnerId) {
        chat_room = chatRooms.get(partnerId);
        currentData = chat_room;
        client.enterToRoom(this, chat_room, FLAG_CAN_EDIT|FLAG_CHAT);
        client.sendServerCmd(this, new Action("list-messages").add("id", partnerId), null);
        return chat_room;
    }

    @Override
    public void doReturn(final NavClient client) {
        currentData = chat_room;
        client.returnToRoom(this, currentData, FLAG_CAN_EDIT|FLAG_CHAT);
    }

    @Override
    public void doUserInput(final NavClient client, String text) {
        if (client instanceof Client && !TextUtils.isEmpty(text))
            composeNewChatMessage((Client) client, text);
    }

    private void makeChatRoom(String partenrId, String userName) {
        SSect chat = chatRooms.get(partenrId);
        if (chat != null)
            return;
        partenrId = partenrId.intern();
        chat = new SSect();
        chat.entity.media = "chat-room";
        chat.entity.name = partenrId;
        chat.entity.data = "chat";
        chat.title = new SEntity();
        chat.title.media = "text";
        chat.title.data = userName;
        chat.hasChildren = true;
        chat.children = new SSect[0];
        chatRooms.put(partenrId, chat);
    }

    public void composeNewChatMessage(Client client, String text) {
        String guid = new StringBuilder()
                .append("comp").append('-')
                .append(0).append('-')
                .append(getUserId()).append('-')
                .append(getPartnerId())
                .toString();
        SSect msg = new SSect();
        msg.entity.media = "chat-text-msg";
        msg.entity.role = "comp";
        msg.entity.name = "unconfirmed";
        msg.guid = guid;
        msg.title = new SEntity();
        msg.title.media = "text";
        msg.title.data = text;
        msg.padd("timestamp", System.currentTimeMillis());
        msg.padd("sender", userID);
        msg.padd("receiver", chat_room.entity.name);
        currentData = msg;
        client.enterToRoom(this, msg, FLAG_CAN_SEND|FLAG_CAN_ABORT);
    }

    public void composeNewChatMessageResult(Client client, boolean confirmed) {
        if (currentData == null)
            return;
        if (!"chat-text-msg".equals(currentData.entity.media))
            return;
        if (!"comp".equals(currentData.entity.role))
            return;
        if (confirmed)
            client.sendChatConfirm(userID, chat_room.entity.name, currentData.title.data);
        doReturn(client);
    }

    public static SSect mergeChatMessage(JSONObject jmsg) throws JSONException {
        boolean own = jmsg.optBoolean("own");
        String partner = own ? jmsg.optString("receiver") : jmsg.optString("sender");;
        if (partner == null || partner.isEmpty())
            return null;
        SSect chat = chatRooms.get(partner);
        if (chat == null)
            return null;
        if (chat.children == null)
            chat.children = new SSect[0];

        long id = jmsg.getLong("id");
        long timestamp = jmsg.getLong("timestamp");
        String role = own ? "send" : "recv";
        String receiver = jmsg.getString("receiver").intern();
        String sender = jmsg.getString("sender").intern();
        String status = jmsg.getString("status").intern();

        String guid = new StringBuilder()
                .append(role).append('-')
                .append(id).append('-')
                .append(sender).append('-')
                .append(receiver)
                .toString();

        String text = jmsg.optString("text");
        // find this message in the chat, or find insert position
        int ins = -1;
        SSect msg = null;
        for (int pos=chat.children.length-1; pos >= 0; --pos) {
            SSect old = chat.children[pos];
            if (guid.equals(old.guid)) {
                msg = old;
                break;
            }
            if (ins < 0) {
                long old_timestamp = Long.parseLong(old.entity.val("timestamp", "0"));
                if (old_timestamp <= timestamp)
                    ins = pos + 1;
            }
        }
        if (msg == null) {
            msg = new SSect();
            msg.entity.media = "chat-text-msg";
            msg.entity.role = role;
            msg.entity.name = status;
            msg.guid = guid;
            msg.title = new SEntity();
            msg.title.media = "text";
            msg.title.data = text;
            msg.padd("timestamp", timestamp);
            msg.padd("sender", sender);
            msg.padd("receiver", receiver);
            ArrayList<SSect> lst = new ArrayList<>(Arrays.asList(chat.children));
            if (ins < 0)
                ins = 0;
            lst.add(ins, msg);
            if (chat.currListPosition >= ins)
                chat.currListPosition += 1;
            chat.children = lst.toArray(new SSect[lst.size()]);
        } else {
            msg.entity.name = (status == null) ? null : status.intern();
            if (text != null && !text.isEmpty())
                msg.title.data = text;
        }
        return msg;
    }

    @Override
    public UIAction getUIAction(int dir) {
        if (chat_room == null) {
            if (dir == DEFAULT_ACTION_ENTER)
                return new UIAction("Chat Room", "show-menu");
            return null;
        }
        if (currentData == chat_room) {
            if (dir == DEFAULT_ACTION_ENTER)
                return new UIAction("New Message", "edit");
            if (dir == DEFAULT_ACTION_NEXT) {
                if (currentData.currListPosition < currentData.children.length)
                    return new UIAction("Next", "next");
            }
            return null;
        }
        if (currentData != null && "comp".equals(currentData.entity.role)) {
            // composing a message
            if (dir == DEFAULT_ACTION_ENTER)
                return new UIAction("Send Message", "send");
            if (dir == DEFAULT_ACTION_LEAVE)
                return new UIAction("Close", "close");
            if (dir == DEFAULT_ACTION_NEXT)
                return new UIAction("Play", "replay");
            if (dir == DEFAULT_ACTION_PREV)
                return new UIAction("Close", "close");
            return null;
        }
        return null;
    }
}
