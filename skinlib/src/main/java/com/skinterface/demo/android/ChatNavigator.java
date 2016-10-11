package com.skinterface.demo.android;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatNavigator implements Navigator {

    public static final String TAG = "SkinterPhone";

    private static HashMap<String, SSect> chatRooms = new HashMap<>();
    private SSect wholeMenuTree;

    public final SrvClient client;

    private String userID;
    private String userName;

    // Current chat room
    private SSect chat_room;
    // Current section (maybe the room, or sub-sections for new messages)
    private SSect currentData;

    interface SrvClient {
        void showMenu(SSect menu);
        void enterToRoom(SSect sect, int flags);
        void returnToRoom(SSect sect, int flags);
        void sendChatConfirm(final String sender, final String receiver, final String text);
        void sendVoiceConfirm(final String sender, final String receiver);
        void chatServerCmd(Action action, String data, ChatNavigator nav, SrvCallback callback);
    }

    public ChatNavigator(ChatNavigator.SrvClient client) {
        this.client = client;
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
    public void doHello() {
        Action attach = Action.create("attach");
        client.chatServerCmd(attach, "", this, new SrvCallback() {
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
                    client.showMenu(wholeMenuTree);
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

    public SSect doEnterToRoom(String partnerId) {
        chat_room = chatRooms.get(partnerId);
        currentData = chat_room;
        client.enterToRoom(chat_room, FLAG_CAN_EDIT|FLAG_CHAT);
        client.chatServerCmd(new Action("list-messages").add("id", partnerId), null, null, null);
        return chat_room;
    }

    public void doReturn() {
        currentData = chat_room;
        client.returnToRoom(currentData, FLAG_CAN_EDIT|FLAG_CHAT);
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

    public void composeNewChatMessage(String text) {
        SSect msg = new SSect();
        msg.entity.media = "chat-text-msg";
        msg.entity.role = "composing";
        msg.entity.name = "unconfirmed";
        msg.title = new SEntity();
        msg.title.media = "text";
        msg.title.data = text;
        msg.timestamp = System.currentTimeMillis();
        msg.padd("sender", userID);
        msg.padd("receiver", chat_room.entity.name);
        currentData = msg;
        client.enterToRoom(msg, FLAG_CAN_SEND|FLAG_CAN_ABORT);
    }

    public void composeNewChatMessageResult(boolean confirmed) {
        if (currentData == null)
            return;
        if (!"chat-text-msg".equals(currentData.entity.media))
            return;
        if (!"composing".equals(currentData.entity.role))
            return;
        if (confirmed)
            client.sendChatConfirm(userID, chat_room.entity.name, currentData.title.data);
        doReturn();
    }

    public static SSect mergeChatMessage(JSONObject jmsg) {
        boolean own = jmsg.optBoolean("own");
        String partner = own ? jmsg.optString("receiver") : jmsg.optString("sender");;
        if (partner == null || partner.isEmpty())
            return null;
        SSect chat = chatRooms.get(partner);
        if (chat == null)
            return null;
        if (chat.children == null)
            chat.children = new SSect[0];

        long id = jmsg.optLong("id");
        long timestamp = jmsg.optLong("timestamp");
        String receiver = jmsg.optString("receiver");
        String sender = jmsg.optString("sender");
        String status = jmsg.optString("status");
        String text = jmsg.optString("text");
        if (sender != null) sender = sender.intern();
        if (receiver != null) receiver = receiver.intern();
        // find this message in the chat, or find insert position
        int ins = -1;
        SSect msg = null;
        for (int pos=0; pos < chat.children.length; ++pos) {
            SSect old = chat.children[pos];
            if (old.chatId == id) {
                msg = old;
                break;
            }
            if (ins < 0 && old.timestamp > timestamp)
                ins = pos;
        }
        if (msg == null) {
            msg = new SSect();
            msg.entity.media = "chat-text-msg";
            msg.entity.role = own ? "send" : "recv";
            msg.entity.name = (status == null) ? null : status.intern();
            msg.guid = UUID.randomUUID().toString();
            msg.title = new SEntity();
            msg.title.media = "text";
            msg.title.data = text;
            msg.chatId = id;
            msg.timestamp = timestamp;
            msg.padd("sender", sender);
            msg.padd("receiver", receiver);
            ArrayList<SSect> lst = new ArrayList<>(Arrays.asList(chat.children));
            if (ins < 0) {
                lst.add(msg);
                if (chat.currListPosition == chat.children.length)
                    chat.currListPosition += 1;
            } else {
                lst.add(ins, msg);
                if (chat.currListPosition >= ins)
                    chat.currListPosition += 1;
            }
            chat.children = lst.toArray(new SSect[lst.size()]);
        } else {
            if (msg.timestamp == 0)
                msg.timestamp = timestamp;
            msg.entity.name = (status == null) ? null : status.intern();
            if (text != null && !text.isEmpty())
                msg.title.data = text;
        }
        return msg;
    }

}
