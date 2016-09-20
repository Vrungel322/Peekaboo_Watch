package com.skinterface.demo.android;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.Charset;

public class RsvpMessageService extends WearableListenerService {

    public static final String TAG = "SkinterWatch";

    public static final String RSVP_PLAY_MESSAGE_PATH = "/rsvp_demo/play";
    public static final String RSVP_STOP_MESSAGE_PATH = "/rsvp_demo/stop";

    private static final Charset utf8 = Charset.forName("UTF-8");

    @Override
    public void onMessageReceived(MessageEvent msg) {
        Log.i(TAG, "received message from node: "+msg.getSourceNodeId()+", path: "+msg.getPath());
        if (msg.getPath().equals(RSVP_PLAY_MESSAGE_PATH)) {
            String text = new String(msg.getData(), utf8);
            Log.i(TAG, "request to play: "+text);
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("RSVP_DATA", text);
            startActivity(startIntent);
            return;
        }
        if (msg.getPath().equals(RSVP_STOP_MESSAGE_PATH)) {
            Log.i(TAG, "request to stop");
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("RSVP_DATA", "");
            startActivity(startIntent);
            return;
        }
        super.onMessageReceived(msg);
    }
}
