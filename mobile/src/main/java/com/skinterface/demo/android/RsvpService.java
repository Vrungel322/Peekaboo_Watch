package com.skinterface.demo.android;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.peekaboo.presentation.services.ChatListener;
import com.peekaboo.presentation.services.ChatRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RsvpService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener, MessageApi.MessageListener {

    public static final String TAG = "RsvpService";

    public static final String RSVP_CAPABILITY = "rsvp_demo";
    public static final String RSVP_MESSAGE_PATH = "/rsvp_demo";

    final static Charset utf8 = Charset.forName("UTF-8");

    GoogleApiClient mGoogleApiClient;
    Set<Node> wearNodes = Collections.emptySet();

    private ChatListener mChatListener = new ChatListener.Stub() {
        @Override
        public void onMessage(String action, String message) throws RemoteException {
            Log.i(TAG, "Incoming chat action:'"+action+"' message:"+message);
            try {
                JSONObject json = new JSONObject(message);
                long id = json.optLong("id");
                boolean own = json.optBoolean("own");
                long timestamp = json.optLong("timestamp");
                String receiver = json.optString("receiver");
                String sender = json.optString("sender");
                String status = json.optString("status");
                String text = json.optString("text");
                requestPlay("message: "+status+" text: "+text);
            } catch (JSONException e) {
                Log.e(TAG, "mesage decode error", e);
            }
        }
    };

    private ChatRequest mChatService;
    private ServiceConnection mChatConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mChatService = ChatRequest.Stub.asInterface(service);
            Log.i(TAG, "Chat service connected");
            try {
                mChatService.listen(mChatListener);
                Log.i(TAG, "Chat service listener set");
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot setup chat listener", e);
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "Chat service disconnected");
            mChatService = null;
        }
    };


    RsvpRequest.Stub mBinder = new RsvpRequest.Stub() {
        @Override
        public String post(String action, Map params, String data) throws RemoteException {
            if ("play".equals(action)) {
                requestPlay(SSect.fromJson(data));
                return "true";
            }
            else if ("stop".equals(action)) {
                requestPlay((String)null);
                return "true";
            }
            else if ("chat-connect".equals(action)) {
                if (mChatService == null) {
                    Intent bi = new Intent();
                    bi.setAction(Intent.ACTION_MAIN);
                    bi.setComponent(new ComponentName(
                            "com.peekaboo", "com.peekaboo.presentation.services.WearLink"));
                    try {
                        bindService(bi, mChatConnection, Context.BIND_AUTO_CREATE);
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot bind chat service", e);
                    }
                }
                return "true";
            }
            else if ("chat-disconnect".equals(action)) {
                if (mChatService != null) {
                    try {
                        mChatService.listen(null);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Cannot disconnect chat listener", e);
                    }
                    try {
                        unbindService(mChatConnection);
                    } catch (Exception e) {
                        Log.e(TAG, "Cannot unbind chat service", e);
                    }
                    mChatService = null;
                }
                return "true";
            }
            return "false";
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ("com.skinterface.demo.android.BindToChat".equals(intent.getAction())) {
            if (mChatService == null) {
                Intent bi = new Intent();
                bi.setAction(Intent.ACTION_MAIN);
                bi.setComponent(new ComponentName(
                        "com.peekaboo", "com.peekaboo.presentation.services.WearLink"));
                bindService(bi, mChatConnection, Context.BIND_AUTO_CREATE);
            }

        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                // Request access only to the Wearable API
                .addApi(Wearable.API) //addApiIfAvailable
                .build();
        mGoogleApiClient.connect();
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
        // Now you can use the Data Layer API
        setupRsvpNodes();
    }
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);
    }

    void setupRsvpNodes() {
        Wearable.CapabilityApi.getCapability( mGoogleApiClient, RSVP_CAPABILITY,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(
                new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        if (result.getStatus().isSuccess())
                            updateRsvpNodes(result.getCapability());
                    }
                });
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient, this, RSVP_CAPABILITY);
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        updateRsvpNodes(capabilityInfo);
    }
    void updateRsvpNodes(CapabilityInfo capabilityInfo) {
        wearNodes = capabilityInfo.getNodes();
    }
    String pickBestNodeId() {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : wearNodes) {
            if (node.isNearby())
                return node.getId();
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    final void requestPlay(String text) {
        String nodeId = pickBestNodeId();
        if (nodeId == null)
            return;
        JSONObject json = new JSONObject();
        try {
            json.put("action", "play");
            if (text != null)
                json.put("text", text);
        } catch (JSONException e) {}
        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, RSVP_MESSAGE_PATH, json.toString().getBytes(utf8));
    }

    final void requestPlay(SSect sect) {
        String nodeId = pickBestNodeId();
        if (nodeId == null)
            return;
        JSONObject json = new JSONObject();
        try {
            json.put("action", "play");
            if (sect != null)
                sect.fillJson(json);
        } catch (JSONException e) {}
        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, RSVP_MESSAGE_PATH, json.toString().getBytes(utf8));
    }

    @Override
    public void onMessageReceived(MessageEvent msg) {
        Log.i(TAG, "received message from node: "+msg.getSourceNodeId()+", path: "+msg.getPath());
        if ("/voice".equals(msg.getPath())) {
            if (mChatService != null) {
                byte[] data = msg.getData();
                Map<String,String> params = new TreeMap<>();
                params.put("rate", "22050");
                params.put("fmt", "PCM16");
                try {
                    mChatService.post("voice", params, data);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error sending voice", e);
                }
            }

        }
    }
}
