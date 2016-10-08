package com.skinterface.demo.android;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class ChatSectionsModel extends SectionsModel {

    private static final ChatSectionsModel model = new ChatSectionsModel();

    public static ChatSectionsModel get() { return model; }



    private String userID;
    private String userName;

    private HashMap<String, SSect> chatRooms = new HashMap<>();
    private SSect chat_room;


    public void setAttachInfo(JSONObject jobj) {
        try {
            userID = jobj.getString("id");
            userName = jobj.getString("name");
            ArrayList<SSect> children = new ArrayList<>();
            JSONArray jarr = jobj.getJSONArray("contacts");
            for (int i=0; i < jarr.length(); ++i) {
                JSONObject jc = jarr.getJSONObject(i);
                String id = jc.getString("id");
                String name = jc.getString("name");
                makeChatRoom(id, name);
            }
        } catch (Exception e) {
        }
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

    private ChatSectionsModel() {
    }

    public void setChatRoom(String partnerId) {
        chat_room = chatRooms.get(partnerId);
        notifyDataChanged();
    }

    public SSect currArticle() {
        return chat_room;
    }

    public int size() {
        if (chat_room == null)
            return 0;
        return chat_room.children.length;
    }

    public SSect get(int i) {
        if (chat_room == null || chat_room.children == null || i >= chat_room.children.length)
            return null;
        return chat_room.children[i];
    }

    public void addChatMessage(String text) {
        SSect msg = new SSect();
        msg.entity.media = "chat-text-msg";
        msg.entity.role = "sent";
        msg.entity.name = "unconfirmed";
        msg.title = new SEntity();
        msg.title.media = "text";
        msg.title.data = text;
        msg.chatTimestamp = System.currentTimeMillis();
        msg.padd("sender", userID);
        msg.padd("receiver", chat_room.entity.name);
        int len = chat_room.children.length;
        SSect[] arr = Arrays.copyOf(chat_room.children, len+1);
        arr[len] = msg;
        chat_room.children = arr;
        notifyDataChanged();
    }

    public void mergeChatMessage(JSONObject jmsg) {

        boolean own = jmsg.optBoolean("own");
        String partner = own ? jmsg.optString("receiver") : jmsg.optString("sender");;
        if (partner == null || partner.isEmpty())
            return;
        SSect chat = chatRooms.get(partner);
        if (chat == null)
            return;

        long id = jmsg.optLong("id");
        long timestamp = jmsg.optLong("timestamp");
        String receiver = jmsg.optString("receiver");
        String sender = jmsg.optString("sender");
        String status = jmsg.optString("status");
        String text = jmsg.optString("text");
        // find this message in the chat
        SSect msg = null;
        if (chat.children != null) {
            for (SSect old : chat.children) {
                if (old.chatId == id) {
                    msg = old;
                    break;
                }
            }
        } else {
            chat.children = new SSect[0];
        }
        if (msg == null) {
            msg = new SSect();
            msg.entity.media = "chat-text-msg";
            msg.entity.role = own ? "sent" : "recv";
            msg.entity.name = status;
            msg.title = new SEntity();
            msg.title.media = "text";
            msg.title.data = text;
            msg.chatId = id;
            msg.chatTimestamp = timestamp;
            msg.padd("sender", sender);
            msg.padd("receiver", receiver);
            int len = chat.children.length;
            SSect[] arr = Arrays.copyOf(chat.children, len+1);
            arr[len] = msg;
            Arrays.sort(arr, new Comparator<SSect>() {
                @Override
                public int compare(SSect msg1, SSect msg2) {
                    if (msg1.chatTimestamp != msg2.chatTimestamp)
                        return Long.compare(msg1.chatTimestamp, msg2.chatTimestamp);
                    return Long.compare(msg1.chatId, msg2.chatId);
                }
            });
            chat.children = arr;
        } else {
            msg.entity.name = status;
            if (text != null && !text.isEmpty())
                msg.title.data = text;
        }
        notifyDataChanged();
    }

}
