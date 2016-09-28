package com.skinterface.demo.android;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Action.ActionExecutor
{

    public static final String TAG = "SkinterPhone";

    final static Charset utf8 = Charset.forName("UTF-8");

    static final int CAPS = 0;
    static final String JSON_URL = "http://192.168.2.157:8080/UpStars/Service";

    final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    final Handler handler = new Handler(Looper.getMainLooper());

    private RsvpRequest mRsvpService;
    private ServiceConnection mRsvpConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mRsvpService = RsvpRequest.Stub.asInterface(service);
        }
        public void onServiceDisconnected(ComponentName className) {
            mRsvpService = null;
        }
    };

    TextView tvText;
    TextView tvStatus;
    RecyclerView rvChildren;
    View        grpForward;
    ImageButton btnForward1;
    ImageButton btnForward2;

    String sessionID;
    Map<String,String> storage = new HashMap<>();

    // Main site menu
    //MenuBar menu_main;

    // Loaded size menu
    SSect wholeMenuTree;
    // Current data
    SSect currentData;
    SSect actionAutoNext;
    // describes what was just presented (shown, read) to user
    int justRead;

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
        rvChildren = (RecyclerView) findViewById(R.id.children);
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        rvChildren.setHasFixedSize(false);
        rvChildren.setVisibility(View.GONE);
        grpForward = findViewById(R.id.sf_forward);
        btnForward1 = (ImageButton)findViewById(R.id.sf_forward1);
        btnForward2 = (ImageButton)findViewById(R.id.sf_forward2);
        grpForward.setVisibility(View.GONE);
        btnForward1.setOnClickListener(this);
        btnForward2.setOnClickListener(this);
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
        if (id == R.id.rsvp_notify) {
            int notificationId = 1;
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
        if (id == R.id.rsvp_stop) {
            if (mRsvpService != null) {
                try {
                    mRsvpService.post("stop", null, null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error sending a message to RsvpService", e);
                }
            }
            return true;
        }
        if (id == R.id.rsvp_hello) {
            if (mRsvpService != null) {
                SSect sect = new SSect();
                sect.title = new SEntity();
                sect.title.media = "text";
                sect.title.data = "Hello from handheld!";
                sect.hasArticle = true;
                sect.entity.media = "text";
                sect.entity.data = "With the help of our site you can know the details and the underlying " +
                        "reasons for relations with any person - a partner, a new sympathy, " +
                        "an old friend, your child, and check the compatibility of the child " +
                        "and the nanny, colleagues in the work group and so long, and so forth.";
                postRsvpService(sect);
            }
            return true;
        }
        if (id == R.id.chat_connect) {
            if (mRsvpService != null) {
                try {
                    mRsvpService.post("chat-connect", null, null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error sending a message to RsvpService", e);
                }
            }
            return true;
        }
        if (id == R.id.chat_disconnect) {
            if (mRsvpService != null) {
                try {
                    mRsvpService.post("chat-disconnect", null, null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error sending a message to RsvpService", e);
                }
            }
            return true;
        }
        if (id == R.id.sf_hello) {
            String lang = Locale.getDefault().getLanguage();
            Action hello = Action.create("hello");
            hello.add("lang", lang);
            serverCmd(hello, new SrvCallback() {
                @Override
                public void onSuccess(String result) {
                    if (result == null || result.length() == 0)
                        return;
                    Object obj = null;
                    try {
                        JSONTokener tokener = new JSONTokener(result);
                        obj = tokener.nextValue();
                    } catch (JSONException e) {
                        return;
                    }
                    if (!(obj instanceof JSONObject) || obj == JSONObject.NULL)
                        return;
                    JSONObject jobj = (JSONObject) obj;
                    String id = jobj.optString("session");
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
        if (id == R.id.sf_forward1 || id == R.id.sf_forward2) {
            if (actionAutoNext != null)
                makeActionHandler(actionAutoNext.entity).run();
            return true;
        }

        return false;
    }

    void setStatus(final String text) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            if (text == null || text.length() == 0) {
                tvStatus.setText("");
                tvStatus.setVisibility(View.GONE);
            } else {
                tvStatus.setText(text);
                tvStatus.setVisibility(View.VISIBLE);
            }
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
        bindService(new Intent(this, RsvpService.class), mRsvpConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mRsvpService != null) {
            unbindService(mRsvpConnection);
            mRsvpService = null;
        }
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
        actionAutoNext = null;
        if (this.currentData != null) {
            SSect ds = this.currentData;
            SSect theNext = SSect.makeAction("Continue: Guide", "show-menu");
            if (ds.children != null && ds.children.length > 0 && ds.currListPosition >= 0) {
                theNext = SSect.makeAction("Continue: Stop list", "stop");
                currActions.add(SSect.makeAction("Stop list", "stop"));
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
                        theNext = SSect.makeAction("Continue: List", "list");
                    currActions.add(SSect.makeAction("List", "list"));
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
            actionAutoNext = theNext;
        }
        updateCommands();
    }
    void updateCommands() {
        ArrayList<SSect> actions = new ArrayList<>();
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
        {
            if (actionAutoNext == null) {
                grpForward.setVisibility(View.GONE);
                btnForward2.setImageResource(0);
            } else {
                grpForward.setVisibility(View.VISIBLE);
                String act = actionAutoNext.entity.data;
                if ("read".equals(act))
                    btnForward2.setImageResource(R.drawable.ic_format_align_left_black_48dp);
                else if ("list".equals(act))
                    btnForward2.setImageResource(R.drawable.ic_format_list_bulleted_black_48dp);
                else if ("auto-next-up".equals(act))
                    btnForward2.setImageResource(R.drawable.ic_playlist_play_black_48dp);
                else if ("enter".equals(act))
                    btnForward2.setImageResource(R.drawable.ic_exit_to_app_black_48dp);
                else if ("show-menu".equals(act))
                    btnForward2.setImageResource(R.drawable.ic_more_vert_black_48dp);
            }
        }
    }

    class ActionHandler implements Runnable {
        Action action;
        ActionHandler(Action action) {
            this.action = action;
        }
        public void run() {
            setStatus("");
            String act = action.getAction();
            if ("list".equals(act)) {
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
                words.addTitleWords(currentData.title).addPause();
                playVoice(currentData.title);
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
            else if ("show".equals(act) || "home".equals(act) || "update".equals(act)) {
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
            else if ("show-menu".equals(act)) {
                showMenu(wholeMenuTree);
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

    void postRsvpService(SSect sect) {
        if (mRsvpService == null)
            return;
        try {
            if (sect != null)
                mRsvpService.post("play", null, sect.fillJson(new JSONObject()).toString());
        } catch (RemoteException e) {
            Log.e(TAG, "Error sending a message to RsvpService", e);
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
            CharSequence text = Fmt.toSpannedText(part.text == null ? "" : part.text);
            int beg = sb.length();
            int end = beg + text.length();
            sb.append(text);
            if (part.type == RsvpWords.JR_TITLE) {
                sb.setSpan(new RelativeSizeSpan(1.5f), beg, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                sb.setSpan(new StyleSpan(Typeface.BOLD), beg, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            else if (part.type == RsvpWords.JR_INTRO) {
                sb.setSpan(new StyleSpan(Typeface.ITALIC), beg, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            sb.append('\n').append('\n');
        }
        tvText.setText(sb);
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
                rvChildren.setAdapter(null);
                rvChildren.setVisibility(View.GONE);
            } else {
                if (pos < 0)
                    pos = 0;
                if (pos >= currentData.children.length)
                    pos = currentData.children.length - 1;
                currentData.currListPosition = pos;
                rvChildren.setAdapter(new SSectAdapter(currentData));
                rvChildren.setVisibility(View.VISIBLE);
                RsvpWords words = new RsvpWords()
                        .addTitleWords(currentData.title)
                        .addPause()
                        .addValueWords(currentData);
                play(words);
                stopVoice();
                playVoice(currentData.title);
                playValueVoice(currentData);
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
        rvChildren.setAdapter(null);
        rvChildren.setVisibility(View.GONE);
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
        postRsvpService(ds);
        fillCommands();
    }

    protected void returnToRoom(SSect ds) {
        justRead = 0;
        if (ds == null) {
            ds = new SSect();
            ds.title = new SEntity();
        }
        currentData = ds;
        if (ds.currListPosition >= 0) {
            titleChildAt(ds.currListPosition);
        } else {
            enterToRoom(ds);
        }
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
            String title = "UpStart Guide";
            if (action.title != null && action.title.data != null)
                title = action.title.data;
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(title)
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
                    Log.d(TAG, "The response is: " + conn.getResponseMessage());
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


    class SSectAdapter extends RecyclerView.Adapter<SSectAdapter.ViewHolder> {
        private SSect mSect;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            // each data item is just a string in this case
            public TextView tvTitle;
            public TextView tvIntro;
            public Action action;
            public EdtValue editor;
            public ViewHolder(View v) {
                super(v);
                tvTitle = (TextView) v.findViewById(R.id.tv_title);
                tvIntro = (TextView) v.findViewById(R.id.tv_intro);
            }

            @Override
            public void onClick(View view) {
                if (action != null) {
                    mSect.currListPosition = getAdapterPosition();
                    new ActionHandler(action).run();
                }
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public SSectAdapter(SSect sect) {
            mSect = sect;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public SSectAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_child, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            CharSequence title = "";
            CharSequence descr = null;
            if (position >= 0 && position < mSect.children.length) {
                SSect sect = mSect.children[position];
                if (sect.title != null)
                    title = Fmt.toSpannedText(sect.title.data);
                if (sect.descr != null)
                    descr = Fmt.toSpannedText(sect.descr.data);
                if (sect.hasArticle || sect.hasChildren)
                    holder.action = new Action("enter").add("sectID", sect.guid);
                else if (sect.isAction) {
                    holder.action = Action.create(sect.entity.data);
                    if (sect.entity.props != null) {
                        for (String key : sect.entity.props.keySet())
                            holder.action.add(key, sect.entity.props.get(key));
                    }
                }
                if (sect.isValue) {
                    holder.editor = new EdtValue(sect, MainActivity.this, MainActivity.this);
                }
            }
            holder.tvTitle.setText(title);
            if (holder.action != null) {
                holder.tvTitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_forward_black_48dp, 0);
                holder.itemView.setOnClickListener(holder);
            }
            if (descr == null) {
                holder.tvIntro.setText("");
                holder.tvIntro.setVisibility(View.GONE);
            } else {
                holder.tvIntro.setText(descr);
                holder.tvIntro.setVisibility(View.VISIBLE);
            }
            if (holder.editor != null)
                ((ViewGroup)holder.tvIntro.getParent()).addView(holder.editor.makeWidget(false));
        }

        @Override
        public void onViewRecycled(ViewHolder holder) {
            super.onViewRecycled(holder);
            holder.itemView.setOnClickListener(null);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mSect.children.length;
        }
    }

}
