package com.skinterface.demo.android;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;

public class RsvpMessageService extends WearableListenerService {

    public static final String TAG = "SkinterWatch";

    public static final String RSVP_MESSAGE_PATH = "/rsvp_demo";

    private static final Charset utf8 = Charset.forName("UTF-8");

    @Override
    public void onMessageReceived(MessageEvent msg) {
        Log.i(TAG, "received message from node: "+msg.getSourceNodeId()+", path: "+msg.getPath());
        if (msg.getPath().equals(RSVP_MESSAGE_PATH)) {
            if (msg.getData() == null)
                return;
            String json = new String(msg.getData(), utf8);
            Log.i(TAG, "request: "+json);
            String text = "";
            try {
                JSONObject jobj = new JSONObject(json);
                if ("play".equals(jobj.optString("action"))) {
                    text = jobj.optString("text", "");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("RSVP_DATA", text);
            startActivity(startIntent);
            return;
        }
        super.onMessageReceived(msg);
    }
}
