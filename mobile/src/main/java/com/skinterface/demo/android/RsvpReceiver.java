package com.skinterface.demo.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class RsvpReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ("com.peekaboo.started".equals(action)) {
            Intent si = new Intent(context, RsvpService.class);
            si.setAction("com.skinterface.demo.android.BindToChat");
            context.startService(si);
        }
    }
}
