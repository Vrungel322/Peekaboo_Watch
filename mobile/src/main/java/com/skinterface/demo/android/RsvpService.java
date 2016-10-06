package com.skinterface.demo.android;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RsvpService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        CapabilityApi.CapabilityListener, MessageApi.MessageListener, ChannelApi.ChannelListener {

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
                requestChat(json);
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
            if ("sect".equals(action) || "menu".equals(action)) {
                requestPlay(action, SSect.fromJson(data));
                return "true";
            }
            else if ("stop".equals(action)) {
                requestPlay(action, null);
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
        Wearable.ChannelApi.addListener(mGoogleApiClient, this);
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

    final void requestPlay(String action, SSect sect) {
        String nodeId = pickBestNodeId();
        if (nodeId == null)
            return;
        JSONObject json = new JSONObject();
        try {
            json.put("action", action);
            if (sect != null)
                sect.fillJson(json);
        } catch (JSONException e) {}
        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, RSVP_MESSAGE_PATH, json.toString().getBytes(utf8));
    }

    final void requestChat(JSONObject message) {
        String nodeId = pickBestNodeId();
        if (nodeId == null || message == null || message == JSONObject.NULL)
            return;
        try {
            message.put("action", "chat");
        } catch (JSONException e) {}
        Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, RSVP_MESSAGE_PATH, message.toString().getBytes(utf8));
    }

    @Override
    public void onMessageReceived(MessageEvent msg) {
        Log.i(TAG, "received message from node: "+msg.getSourceNodeId()+", path: "+msg.getPath());
        Uri uri = Uri.parse(msg.getPath());
        if ("chat".equals(uri.getScheme())) {
            if ("post".equals(uri.getAuthority())) {
                if (mChatService != null) {
                    try {
                        String path = uri.getPath();
                        if (path.startsWith("/voice/")) {
                            path = new File(getCacheDir(), path.substring(7)).getAbsolutePath();
                            new File(path).setReadable(true, false);
                        }
                        Map<String,String> params = new TreeMap<>();
                        params.put("audio", path);
                        for (String p : uri.getQueryParameterNames())
                            params.put(p, uri.getQueryParameter(p));
                        String res = mChatService.post("post", params, new String(msg.getData(), utf8));
                        if (res != null) {
                            JSONObject jres = new JSONObject(res);
                            jres.put("action", "chat");
                            Wearable.MessageApi.sendMessage(mGoogleApiClient, msg.getSourceNodeId(), RSVP_MESSAGE_PATH, jres.toString().getBytes(utf8));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error sending voice", e);
                    }
                }
                return;
            }
        }
        if ("rsvp".equals(uri.getScheme())) {
            if ("/action".equals(msg.getPath())) {
                final String reqData = new String(msg.getData(), IOUtils.UTF8);
                MainActivity.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        InputStream is = null;
                        try {
                            URL url = new URL(MainActivity.JSON_URL);
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            conn.setReadTimeout(5000 /* milliseconds */);
                            conn.setConnectTimeout(5000 /* milliseconds */);
                            conn.setRequestMethod("POST");
                            conn.setDoInput(true);
                            OutputStream os = conn.getOutputStream();
                            os.write(reqData.getBytes(utf8));
                            os.close();
                            conn.connect();
                            JSONObject jobj = IOUtils.parseHTTPResponce(conn);
                            if (jobj != null) {
                                SSect sect = SSect.fromJson(jobj);
                                requestPlay("sect", sect);
                            }
                        } catch (Throwable e) {
                            Log.e(TAG, "Server connection error", e);
                        } finally {
                            IOUtils.safeClose(is);
                        }
                    }
                });

            }
        }
    }

    @Override
    public void onChannelOpened(Channel channel) {
        String path = channel.getPath();
        if (path.startsWith("/voice/")) {
            File file = new File(getCacheDir(), path.substring(7));
            channel.receiveFile(mGoogleApiClient, Uri.fromFile(file), false);
            return;
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
