package com.skinterface.demo.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

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
            SSect sect = SSect.fromJson(json);
            if (sect != null) {
                Intent startIntent = new Intent(this, WearActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startIntent.putExtra("RSVP_SECT", json);
                startActivity(startIntent);
            }
            return;
        }
        super.onMessageReceived(msg);
    }

}
