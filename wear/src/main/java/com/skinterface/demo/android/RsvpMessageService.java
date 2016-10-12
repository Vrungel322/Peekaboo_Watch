package com.skinterface.demo.android;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

public class RsvpMessageService extends WearableListenerService {

    public static final String TAG = "SkinterWatch";

    public static Handler handler = new Handler(Looper.getMainLooper());

    private static SparseArray<Request> requests = new SparseArray<>();

    final static class Request {
        final SrvCallback callback;
        final String responce;
        final long timeout;

        Request(SrvCallback callback, long timeout) {
            this.callback = callback;
            this.responce = null;
            this.timeout = timeout;
        }
        Request(String responce, long timeout) {
            this.callback = null;
            this.responce = responce;
            this.timeout = timeout;
        }
    }

    public static synchronized void addPendingRequest(int requestId, final SrvCallback callback) {
        if (callback == null)
            return;
        Request req = requests.get(requestId);
        if (req != null) {
            long time = SystemClock.uptimeMillis();
            requests.remove(requestId);
            if (req.timeout >= time) {
                final String responce = req.responce;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(responce);
                    }
                });
                return;
            }
        }
        req = new Request(callback, SystemClock.uptimeMillis() + 5000);
        requests.put(requestId, req);
    }

    public static synchronized void addIncomingReplay(int requestId, final String responce) {
        Request req = requests.get(requestId);
        if (req != null) {
            long time = SystemClock.uptimeMillis();
            requests.remove(requestId);
            if (req.timeout >= time && req.callback != null) {
                final SrvCallback callback = req.callback;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onSuccess(responce);
                    }
                });
                return;
            }
        }
        req = new Request(responce, SystemClock.uptimeMillis() + 5000);
        requests.put(requestId, req);
    }

    @Override
    public void onMessageReceived(MessageEvent msg) {
        Log.i(TAG, "received message from node: "+msg.getSourceNodeId()+", path: "+msg.getPath());
        if (msg.getPath().equals(IOUtils.RSVP_ACTION_PATH)) {
            if (msg.getData() == null)
                return;
            String json = new String(msg.getData(), IOUtils.UTF8);
            Log.i(TAG, "request: "+json);
            Intent startIntent = new Intent(this, WearActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("RSVP_MESSAGE", json);
            startActivity(startIntent);
            return;
        }
        if (msg.getPath().startsWith(IOUtils.RSVP_REPLAY_PATH)) {
            int requestId = Integer.parseInt(msg.getPath().substring(IOUtils.RSVP_REPLAY_PATH.length()));
            if (msg.getData() == null) {
                addIncomingReplay(requestId, null);
                return;
            }
            String json = new String(msg.getData(), IOUtils.UTF8);
            Log.i(TAG, "responce: "+json);
            addIncomingReplay(requestId, json);
            return;
        }
        if (msg.getPath().startsWith(IOUtils.CHAT_ACTION_PATH)) {
            if (msg.getData() == null)
                return;
            String json = new String(msg.getData(), IOUtils.UTF8);
            Log.i(TAG, "request: "+json);
            Intent startIntent = new Intent(this, WearActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("CHAT_MESSAGE", json);
            startActivity(startIntent);
            return;
        }
        if (msg.getPath().startsWith(IOUtils.CHAT_REPLAY_PATH)) {
            int requestId = Integer.parseInt(msg.getPath().substring(IOUtils.CHAT_REPLAY_PATH.length()));
            if (msg.getData() == null) {
                addIncomingReplay(requestId, null);
                return;
            }
            String json = new String(msg.getData(), IOUtils.UTF8);
            Log.i(TAG, "responce: "+json);
            addIncomingReplay(requestId, json);
            return;
        }
        super.onMessageReceived(msg);
    }

}
