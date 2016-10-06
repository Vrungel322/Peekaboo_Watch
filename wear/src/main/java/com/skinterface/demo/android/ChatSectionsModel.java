package com.skinterface.demo.android;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;

public class ChatSectionsModel extends SectionsModel {

    static final SSect chatMenu;
    private static final HashMap<String,ChatSectionsModel> chatRooms = new HashMap<>();
    static {
        chatMenu = SSect.makeMenu("Peekaboo");
        chatMenu.children = new SSect[] {
                SSect.makeAction("User 1", "chat").padd("room", "userone"),
                SSect.makeAction("User 2", "chat").padd("room", "usertwo"),
                SSect.makeAction("User 3", "chat").padd("room", "userthree"),
        };
    }

    private final SSect chat_room;

    public static synchronized ChatSectionsModel getChatModel(String room) {
        ChatSectionsModel model = chatRooms.get(room);
        if (model != null)
            return model;
        room = room.intern();
        SSect chat = new SSect();
        chat.entity.media = "chat-room";
        chat.entity.name = room;
        chat.entity.data = "chat";
        chat.title = new SEntity();
        chat.title.media = "text";
        chat.title.data = room;
        chat.hasChildren = true;
        chat.children = new SSect[0];
        model = new ChatSectionsModel(chat);
        chatRooms.put(room, model);
        return model;
    }

    private ChatSectionsModel(SSect chat_room) {
        this.chat_room = chat_room;
    }

    public SSect getMenu() {
        return chatMenu;
    }

    public SSect currArticle() {
        return chat_room;
    }

    public int size() {
        return chat_room.children.length;
    }

    public SSect get(int i) {
        return chat_room.children[i];
    }

    public void mergeChatMessage(JSONObject jmsg) {
        long id = jmsg.optLong("id");
        boolean own = jmsg.optBoolean("own");
        long timestamp = jmsg.optLong("timestamp");
        String receiver = jmsg.optString("receiver");
        String sender = jmsg.optString("sender");
        String status = jmsg.optString("status");
        String text = jmsg.optString("text");
        // find this message in the chat
        SSect msg = null;
        if (chat_room.children != null) {
            for (SSect old : chat_room.children) {
                if (old.chatId == id) {
                    msg = old;
                    break;
                }
            }
        } else {
            chat_room.children = new SSect[0];
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
            int len = chat_room.children.length;
            SSect[] arr = Arrays.copyOf(chat_room.children, len+1);
            arr[len] = msg;
            Arrays.sort(arr, new Comparator<SSect>() {
                @Override
                public int compare(SSect msg1, SSect msg2) {
                    if (msg1.chatTimestamp != msg2.chatTimestamp)
                        return Long.compare(msg1.chatTimestamp, msg2.chatTimestamp);
                    return Long.compare(msg1.chatId, msg2.chatId);
                }
            });
            chat_room.children = arr;
        } else {
            msg.entity.name = status;
            if (text != null && !text.isEmpty())
                msg.title.data = text;
        }
        notifyDataChanged();
    }

}
