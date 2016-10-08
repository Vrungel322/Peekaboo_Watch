package com.skinterface.demo.android;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.Channel;
import com.google.android.gms.wearable.ChannelApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.peekaboo.presentation.services.ChatListener;
import com.peekaboo.presentation.services.ChatRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RsvpService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener, MessageApi.MessageListener, ChannelApi.ChannelListener {

    public static final String TAG = "RsvpService";

    public static final String ACTION_CONNECTIONS_CHANGED = "action.connections_changed";

    public static final String RSVP_CAPABILITY = "rsvp_demo";

    public static class RsvpNode implements Node {
        public static RsvpNode[] emptyArray = new RsvpNode[0];
        private final Node node;
        public RsvpNode(Node node) {
            this.node = node;
        }
        @Override
        public String getId() {
            return node.getId();
        }
        @Override
        public String getDisplayName() {
            return node.getDisplayName();
        }
        @Override
        public boolean isNearby() {
            return node.isNearby();
        }
        @Override
        public String toString() {
            return getDisplayName();
        }
    }

    public static RsvpNode[] wearNodes = RsvpNode.emptyArray;
    public static RsvpNode choosenNode;
    public static String choosenNodeId;

    GoogleApiClient mGoogleApiClient;

    private ChatListener mChatListener = new ChatListener.Stub() {
        @Override
        public void onMessage(String action, String message) throws RemoteException {
            Log.i(TAG, "Incoming chat action:'"+action+"' message:"+message);
            try {
                JSONObject json = new JSONObject(message);
                requestChat(json);
            } catch (JSONException e) {
                Log.e(TAG, "mesage decode error", e);
            }
        }
    };

    private static String mChatAttachInfo;
    public static ChatRequest mChatService;
    private ServiceConnection mChatConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mChatService = ChatRequest.Stub.asInterface(service);
            Log.i(TAG, "Chat service connected");
            try {
                mChatService.listen(mChatListener);
                mChatAttachInfo = mChatService.post("attach", null, null);
                Log.i(TAG, "Chat service listener set");
                Intent intent = new Intent(ACTION_CONNECTIONS_CHANGED);
                LocalBroadcastManager.getInstance(RsvpService.this).sendBroadcast(intent);
            } catch (RemoteException e) {
                Log.e(TAG, "Cannot setup chat listener", e);
            }
        }
        public void onServiceDisconnected(ComponentName className) {
            Log.i(TAG, "Chat service disconnected");
            mChatService = null;
            mChatAttachInfo = null;
            Intent intent = new Intent(ACTION_CONNECTIONS_CHANGED);
            LocalBroadcastManager.getInstance(RsvpService.this).sendBroadcast(intent);
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ("com.skinterface.demo.android.BindToChat".equals(intent.getAction())) {
            chatConnect();
        }
        else if ("com.skinterface.demo.android.UnBindChat".equals(intent.getAction())) {
            chatDisconnect();
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
        Wearable.ChannelApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void chatConnect() {
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
    }

    private void chatDisconnect() {
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
            mChatAttachInfo = null;
            Intent intent = new Intent(ACTION_CONNECTIONS_CHANGED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
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
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);
    }

    void setupRsvpNodes() {
        Wearable.CapabilityApi.getCapability( mGoogleApiClient, RSVP_CAPABILITY,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(
                new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(@NonNull CapabilityApi.GetCapabilityResult result) {
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
        Set<Node> nodes = capabilityInfo.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            wearNodes = RsvpNode.emptyArray;
        } else {
            ArrayList<RsvpNode> arr = new ArrayList<>(nodes.size());
            for (Node n : nodes)
                arr.add(new RsvpNode(n));
            wearNodes = arr.toArray(RsvpNode.emptyArray);
        }
        String nodeId = choosenNodeId;
        RsvpNode node = null;
        if (nodeId == null) {
            for (RsvpNode n : wearNodes) {
                if (n.isNearby()) {
                    node = n;
                    break;
                }
            }
        } else {
            for (RsvpNode n : wearNodes) {
                if (n.isNearby() && nodeId.equals(n.getId())) {
                    node = n;
                    break;
                }
            }
        }
        choosenNode = node;
        Intent intent = new Intent(ACTION_CONNECTIONS_CHANGED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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

    final void requestChat(JSONObject message) {
        String nodeId = pickBestNodeId();
        if (nodeId == null || message == null || message == JSONObject.NULL)
            return;
        try {
            message.put("action", "chat");
        } catch (JSONException e) {}
        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, IOUtils.CHAT_ACTION_PATH, message.toString().getBytes(IOUtils.UTF8));
    }

    @SuppressLint("SetWorldReadable")
    @Override
    public void onMessageReceived(final MessageEvent msg) {
        Log.i(TAG, "received message from node: "+msg.getSourceNodeId()+", path: "+msg.getPath());
        Uri uri = Uri.parse(msg.getPath());
        if (msg.getPath().startsWith(IOUtils.CHAT_ACTION_PATH)) {
            byte[] responce = null;
            if (mChatService != null && mChatAttachInfo != null) {
                try {
                    JSONObject jres = new JSONObject(mChatAttachInfo);
                    jres.put("action", "attach");
                    responce = jres.toString().getBytes(IOUtils.UTF8);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending voice", e);
                }
            }
            Wearable.MessageApi.sendMessage(mGoogleApiClient, msg.getSourceNodeId(), IOUtils.CHAT_REPLAY_PATH+msg.getRequestId(), responce);
            return;
        }
        if (msg.getPath().startsWith(IOUtils.CHAT_POST_PATH)) {
            if (mChatService != null) {
                try {
                    String path = uri.getPath().substring(IOUtils.CHAT_POST_PATH.length());
                    if (path.startsWith("/voice/")) {
                        path = new File(getCacheDir(), path.substring(7)).getAbsolutePath();
                        new File(path).setReadable(true, false);
                    }
                    Map<String,String> params = new TreeMap<>();
                    params.put("audio", path);
                    for (String p : uri.getQueryParameterNames())
                        params.put(p, uri.getQueryParameter(p));
                    String res = mChatService.post("post", params, new String(msg.getData(), IOUtils.UTF8));
                    if (res != null) {
                        JSONObject jres = new JSONObject(res);
                        jres.put("action", "chat");
                        Wearable.MessageApi.sendMessage(mGoogleApiClient, msg.getSourceNodeId(), IOUtils.RSVP_ACTION_PATH, jres.toString().getBytes(IOUtils.UTF8));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error sending voice", e);
                }
            }
            return;
        }
        if (msg.getPath().startsWith(IOUtils.RSVP_ACTION_PATH)) {
            final String reqData = new String(msg.getData(), IOUtils.UTF8);
            MainActivity.executor.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] responce = null;
                    try {
                        URL url = new URL(IOUtils.UpStars_JSON_URL);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setReadTimeout(5000);
                        conn.setConnectTimeout(5000);
                        conn.setRequestMethod("POST");
                        conn.setDoInput(true);
                        OutputStream os = conn.getOutputStream();
                        os.write(reqData.getBytes(IOUtils.UTF8));
                        os.close();
                        conn.connect();
                        JSONObject jobj = IOUtils.parseHTTPResponce(conn);
                        if (jobj != null)
                            responce = jobj.toString().getBytes(IOUtils.UTF8);
                    } catch (Throwable e) {
                        Log.e(TAG, "Server connection error", e);
                    }
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, msg.getSourceNodeId(), IOUtils.RSVP_REPLAY_PATH+msg.getRequestId(), responce);
                }
            });
        }
    }

    @Override
    public void onChannelOpened(Channel channel) {
        String path = channel.getPath();
        if (path.startsWith("/voice/")) {
            File file = new File(getCacheDir(), path.substring(7));
            channel.receiveFile(mGoogleApiClient, Uri.fromFile(file), false);
        }
    }

    @Override
    public void onChannelClosed(Channel channel, int i, int i1) {
    }

    @Override
    public void onInputClosed(Channel channel, int i, int i1) {
    }

    @Override
    public void onOutputClosed(Channel channel, int i, int i1) {
    }
}
