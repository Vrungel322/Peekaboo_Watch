package com.skinterface.demo.android;

import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, CapabilityApi.CapabilityListener
{

    public static final String TAG = "SkinterPhone";

    public static final String RSVP_CAPABILITY = "rsvp_demo";
    public static final String RSVP_PLAY_MESSAGE_PATH = "/rsvp_demo/play";
    public static final String RSVP_STOP_MESSAGE_PATH = "/rsvp_demo/stop";

    final static Charset utf8 = Charset.forName("UTF-8");

    static final int CAPS = 0;
    static final String JSON_URL = "http://192.168.1.100:8888/Service";

    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    final Handler handler = new Handler(Looper.getMainLooper());

    GoogleApiClient mGoogleApiClient;
    boolean connected;
    Set<Node> wearNodes = Collections.emptySet();
    TextView tvText;
    TextView tvStatus;

    String sessionID;
    Map<String,String> storage = new HashMap<>();

    // Main site menu
    //MenuBar menu_main;

    // Loaded size menu
    SSect wholeMenuTree;
    // Current data
    SSect currentData;
    // describes what was just presented (shown, read) to user
    int justRead;

    List<SSect> rootActions = new ArrayList<>();
    List<SSect> currActions = new ArrayList<>();

    Queue<String> voiceQueue = new LinkedList<>();
    //boolean voiceBusy;

    // Rapid serial visual presentation panel (spritz-like reader)
    //Label rsvp_label;
    //Canvas rsvp_canvas;
    //Audio voice_control;
    //Label text_label;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        tvText = (TextView)findViewById(R.id.text);
        tvStatus = (TextView)findViewById(R.id.status);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                // Request access only to the Wearable API
                .addApi(Wearable.API) //addApiIfAvailable
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (doAction(item.getItemId()))
            return true;
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        doAction(v.getId());
    }

    boolean doAction(int id) {
        if (id == R.id.notify) {
            int notificationId = 001;
            // Build intent for notification content
            //Intent viewIntent = new Intent(this, MainActivity.class);
            //viewIntent.putExtra(EXTRA_EVENT_ID, eventId);
            //PendingIntent viewPendingIntent =
            //        PendingIntent.getActivity(this, 0, viewIntent, 0);

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.ic_sms_black_24dp)
                            .setContentTitle("Notification from handheld")
                            .setContentText("Please, play some text")
                            //.setContentIntent(viewPendingIntent)
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                    ;

            // Get an instance of the NotificationManager service
            NotificationManagerCompat notificationManager =
                    NotificationManagerCompat.from(this);

            // Build the notification and issues it with notification manager.
            notificationManager.notify(notificationId, notificationBuilder.build());
            return true;
        }
        if (id == R.id.stop) {
            requestPlay(null);
            return true;
        }
        if (id == R.id.hello) {
            String lang = Locale.getDefault().getLanguage();
            Action hello = Action.create("hello");
            hello.add("lang", lang);
            serverCmd(hello, new SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    JsonElement jobj = new JsonParser().parse(result);
                    if (!jobj.isJsonObject())
                        return;
                    String id = jobj.getAsJsonObject().get("session").getAsString();
                    if (sessionID == null || !sessionID.equals(result))
                        sessionID = id;
                    executeAction(new Action("menu").add("sectID", "site-nav-menu"));
                    executeAction(new Action("home"));
                }
            });
        }
        if (id == R.id.sf_menu) {
            showMenu(wholeMenuTree);
            return true;
        }
        if (id == R.id.sf_where) {
            executeAction(new Action("where"));
            return true;
        }

        return false;
    }

    void setStatus(final String text) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            tvStatus.setText(text == null ? "" : text);
            return;
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                tvStatus.setText(text == null ? "" : text);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
        // Now you can use the Data Layer API
        connected = true;
        setupRsvpNodes();
    }
    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
        connected = false;
    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);
        connected = false;
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

    void requestPlay(String text) {
        tvText.setText(text==null?"":text);
        String nodeId = pickBestNodeId();
        if (nodeId == null)
            return;
        if (text == null)
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, RSVP_STOP_MESSAGE_PATH, null);
        else
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, RSVP_PLAY_MESSAGE_PATH, text.getBytes(utf8));
    }



    public void executeAction(Action action) {
        makeActionHandler(action).run();
    }

    protected Runnable makeActionHandler(Action action) {
        return new ActionHandler(action);
    }

    protected Runnable makeActionHandler(SEntity entity) {
        Action action = Action.create(entity.data);
        if (entity.props != null) {
            for (String key : entity.props.keySet())
                action.add(key, entity.props.get(key));
        }
        return new ActionHandler(action);
    }

    boolean introWasShown(SSect ds) {
        if (ds == null || ds.guid == null || ds.guid.isEmpty())
            return false;
        String val = storage.get(ds.guid+".intro");
        return Boolean.valueOf(val);
    }

    void introSetShown(SSect ds, boolean value) {
        if (ds == null || ds.guid == null || ds.guid.isEmpty())
            return;
        storage.put(ds.guid+".intro", String.valueOf(value));
    }

    void fillCommands() {
        currActions.clear();
        if (this.currentData != null) {
            SSect ds = this.currentData;
            SSect theNext = null;
            if (ds.children != null && ds.children.length > 0 && ds.currListPosition >= 0) {
                SSect aui = ds.children[ds.currListPosition];
                boolean has_prev = ds.currListPosition > 0;
                boolean has_next = ds.currListPosition+1 < ds.children.length;
                if (aui.nextAsSkip) {
                    //if (has_next)
                    //    theNext = SSect.makeAction("Continue: Next", "list-next");
                    //else
                    //    theNext = SSect.makeAction("Continue: Stop list", "stop");
                }
                else if (aui.isAction) {
                    theNext = SSect.copyAction(aui).prependTitle("Continue: Go to ");
                }
                else if (aui.isValue)
                    theNext = SSect.makeAction("Continue: Edit", "enter").padd("sectID", aui.guid).padd("vname", aui.entity.name);
                else
                    theNext = SSect.makeAction("Continue: Enter", "enter").padd("sectID", aui.guid);

                currActions.add(SSect.makeAction("Stop list", "stop"));
                currActions.add(SSect.makeAction("Prev", has_prev ? "list-prev" : "none"));
                if (aui.isAction)
                    currActions.add(SSect.copyAction(aui).prependTitle("Go to: "));
                else if (aui.isValue)
                    currActions.add(SSect.makeAction("Edit", "enter").padd("sectID", aui.guid).padd("vname", aui.entity.name));
                else
                    currActions.add(SSect.makeAction("Enter", "enter").padd("sectID", aui.guid));
                currActions.add(SSect.makeAction("Next", has_next ? "list-next" : "none"));
            } else {
                if (ds.returnUp != null && ds.currListPosition < 0) {
                    currActions.add(SSect.makeAction("Return UP", "return-up"));
                    SSect up = ds.returnUp;
                    SSect[] ch = up.children;
                    if (ch != null && up.currListPosition >= 0) {
                        boolean has_prev = (up.currListPosition > 0);
                        boolean has_next = (up.currListPosition < ch.length - 1);
                        currActions.add(SSect.makeAction("PREV", has_prev ? "sibling-prev" : "none"));
                        currActions.add(SSect.makeAction("NEXT", has_next ? "sibling-next" : "none"));
                        theNext = SSect.makeAction("Continue: UP+Next", "auto-next-up");
                    }
                }
                if (currentData.descr != null)
                    currActions.add(SSect.makeAction("Describe", "descr"));
                else
                    currActions.add(SSect.makeAction("Describe", "none"));
                if (ds.hasArticle) {
                    if (!ds.nextAsSkip && (justRead & RsvpWords.JR_ARTICLE) == 0)
                        theNext = SSect.makeAction("Continue: Read", "read");
                    currActions.add(SSect.makeAction("Read", "read"));
                }
                if (ds.children != null && ds.children.length > 0) {
                    if (!ds.nextAsSkip || ds.returnUp == null)
                        theNext = SSect.makeAction("Continue: List", "list-first");
                    currActions.add(SSect.makeAction("List", "list-first"));
                }
                if (ds.isValue && ds.children == null) {
                    SSect aui = SSect.copyAction(ds);
                    aui.title.data = "Input (" + ds.entity.media + ")";
                    aui.isValue = true;
                    currActions.add(aui);
                }
                if (ds.isAction) {
                    currActions.add(ds);
                    theNext = SSect.copyAction(ds).prependTitle("Continue: Go to ");
                }
            }
            if (theNext != null)
                currActions.add(0, theNext);
        }
        updateCommands();
    }
    void updateCommands() {
        ArrayList<SSect> actions = new ArrayList<>();
        if (!rootActions.isEmpty()) {
            actions.add(SSect.makeAction(getString(R.string.txt_global), null));
            actions.addAll(rootActions);
        }
        if (!currActions.isEmpty()) {
            actions.add(SSect.makeAction(getString(R.string.txt_local), null));
            actions.addAll(currActions);
        }
//        actn_panel.clear();
//        for (final SSect aui : actions) {
//            Widget w;
//            if (aui.isValue) {
//                EdtValue edt = new EdtValue(aui, this, DC);
//                w = edt.makeWidget(false);
//                if (edt.error != null)
//                    setStatus(edt.error);
//                if (aui.title != null) {
//                    FlowPanel pan = new FlowPanel();
//                    pan.add(new Label(aui.title.data, false));
//                    pan.add(w);
//                    w = pan;
//                }
//            }
//            else if (aui.entity.data == null) {
//                w = new Label(aui.title.data, false);
//            }
//            else {
//                w = new Button(aui.title.data, new ClickHandler() {
//                    @Override
//                    public void onClick(ClickEvent event) {
//                        makeActionHandler(aui.entity).execute();
//                    }
//                });
//                if ("none".equals(aui.entity.data))
//                    ((Button)w).setEnabled(false);
//            }
//
//            actn_panel.add(w);
//        }
    }

    class ActionHandler implements Runnable {
        Action action;
        ActionHandler(Action action) {
            this.action = action;
        }
        public void run() {
            setStatus("");
            String act = action.getAction();
            if ("list-next".equals(act)) {
                titleChildAt(currentData.currListPosition+1);
            }
            else if ("list-prev".equals(act)) {
                titleChildAt(currentData.currListPosition-1);
            }
            else if ("list-first".equals(act)) {
                titleChildAt(0);
            }
            else if ("enter".equals(act)) {
                enterDown(currentData, action);
            }
            else if ("return-up".equals(act)) {
                returnUp(currentData, 0);
            }
            else if ("auto-next-up".equals(act)) {
                returnUp(currentData, +1);
                return;
            }
            else if ("sibling-next".equals(act)) {
                returnUp(currentData, +1);
            }
            else if ("sibling-prev".equals(act)) {
                returnUp(currentData, -1);
            }
            else if ("stop".equals(act)) {
                if (currentData != null)
                    currentData.currListPosition = -1;
                enterToRoom(currentData);
            }
            else if ("descr".equals(act)) {
                RsvpWords words = new RsvpWords()
                        .addTitleWords(currentData.title)
                        .addPause()
                        .addIntroWords(currentData.descr);
                introSetShown(currentData, true);
                currentData.currListPosition = -1;
                play(words);
                fillCommands();
                stopVoice();
                playVoice(currentData.title);
                playVoice(currentData.descr);
            }
            else if ("read".equals(act)) {
                stopVoice();
                currentData.currListPosition = -1;
                RsvpWords words = new RsvpWords();
                if ((justRead & RsvpWords.JR_TITLE) == 0) {
                    words.addTitleWords(currentData.title).addPause();
                    playVoice(currentData.title);
                }
                if (currentData.hasArticle) {
                    words.addArticleWords(currentData.entity);
                    playVoice(currentData.entity);
                } else {
                    words.addIntroWords(currentData.descr);
                    playVoice(currentData.descr);
                }
                play(words);
                fillCommands();
            }
            else if ("action".equals(act)) {
                if (currentData.isAction) {
                    makeActionHandler(currentData.entity).run();
                }
            }
            else if ("set".equals(act)) {
                serverCmd(action, new SrvCallback() {
                    @Override
                    public void onSuccess(String result) {
                        //if (result != null)
                        //    executeAction(result);
                    }
                });
                return;
            }
            else if ("where".equals(act)) {
                if (currentData != null) {
                    showWhereAmIData(currentData);
                }
                else {
                    play(new RsvpWords().addWarning("Place is not known"));
                }
            }
            if ("show".equals(act) || "home".equals(act) || "update".equals(act)) {
                serverCmd(action, new SrvCallback() {
                    @Override
                    public void onSuccess(String result) {
                        SSect ds = SSect.fromJson(result);
                        if (ds == null)
                            ds = new SSect();
                        enterToRoom(ds);
                    }
                });
            }
            else if ("menu".equals(act)) {
                action.add("sectID", "site-nav-menu-story");
                serverCmd(action, new SrvCallback() {
                    @Override
                    public void onSuccess(String result) {
                        SSect ds = SSect.fromJson(result);
                        wholeMenuTree = ds;
                    }
                });
            }
        }
        void returnUp(final SSect ds, final int lp_delta) {
            if (ds == null || ds.returnUp == null)
                return;
            justRead = 0;
            final SSect up = ds.returnUp;
            Action show = new Action("show").add("sectID", up.guid);
            if (up.isValue) {
                show.setAction("enter");
                show.add("vname", up.entity.name);
            }
            serverCmd(show, new SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    SSect ds = SSect.fromJson(result);
                    if (ds == null) {
                        ds = up;
                    } else {
                        int lp = up.currListPosition + lp_delta;
                        ds.returnUp = up.returnUp;
                        if (lp < 0 || ds.children == null || lp >= ds.children.length) {
                            ds.currListPosition = -1;
                            if (lp_delta > 0 && ds.returnUp != null && "auto-next-up".equals(action.getAction())) {
                                returnUp(ds, lp_delta);
                                return;
                            }
                        } else {
                            ds.currListPosition = lp;
                            if (lp_delta != 0) {
                                if (!ds.children[lp].nextAsSkip) {
                                    SSect child = ds.children[lp];
                                    Action action = new Action("enter").add("sectID", child.guid);
                                    if (child.isValue)
                                        action.add("vname", child.entity.name);
                                    enterDown(ds, action);
                                    return;
                                }
                            }
                        }
                    }
                    returnToRoom(ds);
                }
            });
        }
        void enterDown(final SSect parent, final Action action) {
            serverCmd(action, new SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    SSect ds = SSect.fromJson(result);
                    if (ds == null)
                        ds = new SSect();
                    ds.returnUp = parent;
                    enterToRoom(ds);
                }
            });
        }
    }

    void play(final RsvpWords words) {
        if (words == null || words.size() == 0) {
            tvStatus.setText("Empty data");
            justRead = 0;
            return;
        }

        justRead = words.getJustRead();

        tvText.setText("");
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (RsvpWords.Part part : words.getText()) {
            int pos = sb.length();
            String title = part.title+": ";
            sb.append(title);
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), pos, pos+title.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.append(part.text.replace('\n',' '));
            sb.append('\n');
        }
        tvText.setText(sb);
        requestPlay(sb.toString());
    }

    protected void stopVoice() {
    }
    protected void playValueVoice(SSect ds) {
    }
    protected void playVoice(SEntity entity) {
    }

    protected void titleChildAt(int pos) {
        justRead = 0;
        if (currentData != null) {
            if (currentData.children == null || currentData.children.length == 0) {
                currentData.currListPosition = -1;
            } else {
                if (pos < 0)
                    pos = 0;
                if (pos >= currentData.children.length)
                    pos = currentData.children.length - 1;
                currentData.currListPosition = pos;
                SSect aui = currentData.children[pos];
                RsvpWords words = new RsvpWords()
                        .addTitleWords(aui.title)
                        .addPause()
                        .addValueWords(aui);
                play(words);
                stopVoice();
                playVoice(aui.title);
                playValueVoice(aui);
            }
        }
        fillCommands();
    }

    protected void showWhereAmIData(SSect ds) {
        RsvpWords words = new RsvpWords();
        words.addTitleWords(ds.title).addPause();
        stopVoice();
        playVoice(ds.title);
        if (ds.getCurrChild() != null) {
            SSect aui = ds.getCurrChild();
            words.addTitleWords(aui.title);
            words.addValueWords(aui);
            playVoice(aui.title);
            playValueVoice(aui);
        } else {
            words.addValueWords(ds);
            playValueVoice(ds);
        }
        play(words);
    }

    protected void enterToRoom(SSect ds) {
        if (ds == null) {
            ds = new SSect();
            ds.title = new SEntity();
        }
        if (ds.children != null && ds.children.length == 0)
            ds.children = null;
        ds.currListPosition = -1;
        currentData = ds;

        if (!introWasShown(ds) && ds.descr == null)
            introSetShown(ds, true);

        tvText.setText("");
        stopVoice();
        RsvpWords words = new RsvpWords();
        words.addTitleWords(ds.title);
        playVoice(ds.title);
        boolean introShown = introWasShown(ds);
        if (!introShown) {
            introSetShown(ds, true);
            words.addIntroWords(ds.descr);
            playVoice(ds.descr);
        }
        else if (ds.descr == null && ds.hasArticle) {
            words.addArticleWords(ds.entity);
            playVoice(ds.entity);
        }
        words.addValueWords(ds);
        playValueVoice(ds);
        play(words);
        fillCommands();
    }

    protected void returnToRoom(SSect ds) {
        justRead = 0;
        if (ds == null) {
            ds = new SSect();
            ds.title = new SEntity();
        }
        currentData = ds;

        tvText.setText("");
        stopVoice();
        RsvpWords words = new RsvpWords();
        words.addTitleWords(ds.title).addPause();
        playVoice(ds.title);
        SSect aui = ds.getCurrChild();
        if (aui != null) {
            words.addTitleWords(aui.title);
            words.addValueWords(aui);
            playVoice(aui.title);
            playValueVoice(aui);
        } else {
            words.addValueWords(ds);
            playValueVoice(ds);
        }
        play(words);
        fillCommands();
    }

    protected SSect findParentMenu(SSect parent, SSect action) {
        if (parent.children == null)
            return null;
        for (SSect child : parent.children) {
            if (child == action)
                return parent;
            SSect found = findParentMenu(child, action);
            if (found != null)
                return found;
        }
        return null;
    }

    protected void showMenu(final SSect action) {
        if (action == null)
            return;
        if (action.children != null && action.children.length > 0) {
            final ArrayAdapter<SSect> adapter = new ArrayAdapter<>(MainActivity.this,
                    android.R.layout.select_dialog_singlechoice,
                    action.children);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("UpStart Guide")
                    .setNegativeButton(R.string.txt_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setNeutralButton(R.string.txt_back, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            showMenu(findParentMenu(wholeMenuTree, action));
                        }
                    })
                    .setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            SSect action = adapter.getItem(which);
                            showMenu(action);
                        }
                    })
                    .show();
        }
        else if (action.entity.data != null) {
            makeActionHandler(action.entity).run();
        }
    }

    interface SrvCallback {
        void onSuccess(String result);
    }
    public void serverCmd(Action action, final SrvCallback callback) {
        final  String reqData = action.serializeToCmd(sessionID, CAPS).toString();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;

                try {
                    URL url = new URL(JSON_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setReadTimeout(5000 /* milliseconds */);
                    conn.setConnectTimeout(5000 /* milliseconds */);
                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    OutputStream os = conn.getOutputStream();
                    os.write(reqData.getBytes(utf8));
                    os.close();
                    // Starts the query
                    conn.connect();
                    int response = conn.getResponseCode();
                    Log.d(TAG, "The response is: " + response);
                    if (response == 200) {
                        StringBuilder sb = new StringBuilder();
                        is = conn.getInputStream();
                        InputStreamReader reader = new InputStreamReader(is, utf8);
                        char[] buffer = new char[4096];
                        int sz;
                        while ((sz = reader.read(buffer)) > 0)
                            sb.append(buffer, 0, sz);
                        reader.close();
                        final String result = sb.toString();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onSuccess(result);
                            }
                        });
                    } else {
                        setStatus("Error on server request");
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "Server connection error", e);
                    setStatus("Error on server request");
                } finally {
                    if (is != null) {
                        try { is.close(); } catch (IOException e) {}
                    }
                }
            }
        });
    }
}
