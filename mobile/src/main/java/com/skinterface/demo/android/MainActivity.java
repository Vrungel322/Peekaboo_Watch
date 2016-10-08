package com.skinterface.demo.android;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
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
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        SiteNavigator.SrvClient
{

    public static final String TAG = "SkinterPhone";

    static final int CAPS = 0;

    final static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    final Handler handler = new Handler(Looper.getMainLooper());

    ScrollView mainTextScroller;
    TextView tvText;
    TextView tvStatus;
    RecyclerView rvChildren;
    ImageButton btnForward;

    final SiteNavigator nav = new SiteNavigator(this);
    // Session storage
    Map<String,String> storage = new HashMap<>();
    // Current default next action
    SSect actionAutoNext;
    // describes what was just presented (shown, read) to user
    int justRead;

    Queue<String> voiceQueue = new LinkedList<>();
    //boolean voiceBusy;

    // Rapid serial visual presentation panel (spritz-like reader)
    //Label rsvp_label;
    //Canvas rsvp_canvas;
    //Audio voice_control;
    //Label text_label;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RsvpService.ACTION_CONNECTIONS_CHANGED.equals(intent.getAction())) {
                supportInvalidateOptionsMenu();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mainTextScroller = (ScrollView)findViewById(R.id.text_scroller);
        tvText = (TextView)findViewById(R.id.text);
        tvStatus = (TextView)findViewById(R.id.status);
        rvChildren = (RecyclerView) findViewById(R.id.children);
        rvChildren.setLayoutManager(new LinearLayoutManager(this));
        rvChildren.setHasFixedSize(false);
        rvChildren.setVisibility(View.GONE);
        setButtonEnabled(R.id.sf_return_up, false);
        setButtonEnabled(R.id.sf_descr, false);
        actionAutoNext = SSect.makeAction("Continue: Hello UpStars", "hello");
        btnForward = (ImageButton) findViewById(R.id.sf_next_auto);
        btnForward.setImageResource(R.drawable.ic_flare_black_48dp);
        btnForward.setOnClickListener(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(RsvpService.ACTION_CONNECTIONS_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, intentFilter);
    }

    public void attachToSite(SSect menu) {
        nav.executeAction(new Action("home"));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        {
            MenuItem item = menu.findItem(R.id.connections_watch);
            item.getIcon().mutate();
            if (RsvpService.choosenNode != null)
                item.getIcon().setAlpha(255);
            else
                item.getIcon().setAlpha(64);
        }
        {
            MenuItem item = menu.findItem(R.id.connections_chat);
            item.getIcon().mutate();
            if (RsvpService.mChatService != null)
                item.getIcon().setAlpha(255);
            else
                item.getIcon().setAlpha(64);
        }
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
        if (id == R.id.connections_watch) {
            RsvpService.RsvpNode[] nodes = RsvpService.wearNodes;
            RsvpService.RsvpNode n = RsvpService.choosenNode;
            int index = n == null ? -1 : Arrays.asList(nodes).indexOf(n);
            ArrayAdapter<RsvpService.RsvpNode> adapter = new ArrayAdapter<>(this,
                    android.R.layout.select_dialog_singlechoice, nodes);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which >= 0) {
                        RsvpService.choosenNode = RsvpService.wearNodes[which];
                        RsvpService.choosenNodeId = RsvpService.choosenNode.getId();
                    }
                    else if (which == DialogInterface.BUTTON_NEGATIVE) {
                        RsvpService.choosenNode = null;
                        RsvpService.choosenNodeId = null;
                    }
                    dialog.dismiss();
                    supportInvalidateOptionsMenu();
                }
            };
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Watch connection");
            if (nodes.length == 0)
                builder.setMessage("No nearby nodes");
            else
                builder.setSingleChoiceItems(adapter, index, listener);
            if (index >= 0)
                builder.setNegativeButton("Disconnect", listener);
            else
                builder.setNegativeButton("Close", listener);
            builder.show();
            return true;
        }
        if (id == R.id.watch_notify) {
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
        if (id == R.id.connections_chat) {
            Intent si = new Intent(this, RsvpService.class);
            if (RsvpService.mChatService == null)
                si.setAction("com.skinterface.demo.android.BindToChat");
            else
                si.setAction("com.skinterface.demo.android.UnBindChat");
            startService(si);
            return true;
        }
        if (id == R.id.sf_hello) {
            nav.doHello();
        }
        if (id == R.id.sf_menu) {
            showMenu(nav.wholeMenuTree);
            return true;
        }
        if (id == R.id.sf_descr) {
            nav.executeAction(new Action("descr"));
            return true;
        }
        if (id == R.id.sf_next_auto) {
            if (actionAutoNext != null)
                nav.makeActionHandler(actionAutoNext.entity).run();
            return true;
        }
        if (id == R.id.sf_return_up) {
            nav.executeAction(new Action("return-up"));
            return true;
        }
        if (id == R.id.run_tts) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts_text_files = null;
                tts_lang = GEN_TTS_LANGS[0];
                runTTS();
            }
            return true;
        }

        return false;
    }

    private void setStatus(final String text) {
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
    public ActionHandler makeActionHandler(SiteNavigator nav, Action action) {
        return new ActionHandler(nav, this, action);
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

    void setButtonEnabled(int id, boolean on) {
        ImageButton btn = (ImageButton)findViewById(id);
        if (btn == null || btn.isEnabled() == on)
            return;
        if (on) {
            btn.setEnabled(true);
            btn.setOnClickListener(this);
            btn.setImageAlpha(255);
        } else {
            btn.setEnabled(false);
            btn.setClickable(false);
            btn.setOnClickListener(null);
            btn.setImageAlpha(64);
        }
    }

    void fillCommands() {
        actionAutoNext = null;
        final SSect ds = nav.currentData;
        if (ds != null) {
            SSect theNext = SSect.makeAction("Continue: Guide", "show-menu");
            {
                setButtonEnabled(R.id.sf_return_up, ds.returnUp != null);
                if (ds.returnUp != null && ds.currListPosition < 0) {
                    SSect up = ds.returnUp;
                    SSect[] ch = up.children;
                    if (ch != null && up.currListPosition >= 0) {
                        boolean has_prev = (up.currListPosition > 0);
                        boolean has_next = (up.currListPosition < ch.length - 1);
                        //currActions.add(SSect.makeAction("PREV", has_prev ? "sibling-prev" : "none"));
                        //currActions.add(SSect.makeAction("NEXT", has_next ? "sibling-next" : "none"));
                        theNext = SSect.makeAction("Continue: UP+Next", "auto-next-up");
                    }
                }
                setButtonEnabled(R.id.sf_descr, ds.descr != null);
                if (ds.hasArticle) {
                    if (!ds.nextAsSkip && (justRead & RsvpWords.JR_ARTICLE) == 0)
                        theNext = SSect.makeAction("Continue: Read", "read");
                    //currActions.add(SSect.makeAction("Read", "read"));
                }
                if (ds.children != null && ds.children.length > 0) {
                    if (!ds.nextAsSkip || ds.returnUp == null) {
                        if ((justRead & RsvpWords.JR_LIST) == 0) {
                            theNext = SSect.makeAction("Continue: List", "list");
                        } else {
                            for (int position=0; position < ds.children.length; ++position) {
                                SSect child = ds.children[position];
                                if (child.isAction) {
                                    theNext = SSect.copyAction(child).padd("position", position);
                                    break;
                                }
                                else if (child.hasArticle || child.hasChildren || child.children != null) {
                                    theNext = SSect.makeAction("Continue: Enter", "enter").padd("sectID", child.guid).padd("position", position);
                                    break;
                                }
                            }
                        }
                    }
                    //if ((justRead & RsvpWords.JR_LIST) == 0)
                    //    currActions.add(SSect.makeAction("List", "list"));
                }
                if (ds.isAction) {
                    //currActions.add(ds);
                    theNext = SSect.copyAction(ds).prependTitle("Continue: Go to ");
                }
            }
            actionAutoNext = theNext;
        } else {
            actionAutoNext = SSect.makeAction("Continue: Hello UpStars", "hello");
        }
        updateCommands();
    }
    void updateCommands() {
        ArrayList<SSect> actions = new ArrayList<>();
        {
            if (actionAutoNext == null) {
                btnForward.setVisibility(View.GONE);
            } else {
                btnForward.setVisibility(View.VISIBLE);
                String act = actionAutoNext.entity.data;
                if ("read".equals(act))
                    btnForward.setImageResource(R.drawable.ic_format_align_left_black_48dp);
                else if ("list".equals(act))
                    btnForward.setImageResource(R.drawable.ic_format_list_bulleted_black_48dp);
                else if ("auto-next-up".equals(act))
                    btnForward.setImageResource(R.drawable.ic_redo_black_48dp);
                else if ("enter".equals(act) || "show".equals(act))
                    btnForward.setImageResource(R.drawable.ic_exit_to_app_black_48dp);
                else if ("show-menu".equals(act))
                    btnForward.setImageResource(R.drawable.ic_menu_black_48dp);
                else
                    btnForward.setImageResource(R.drawable.ic_touch_app_black_48dp);
            }
        }
    }

    @Override
    public void siteServerCmd(Action action, SiteNavigator nav, final SrvCallback callback) {
        final  String reqData = action.serializeToCmd(nav.sessionID, CAPS).toString();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream is = null;
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
                    // Starts the query
                    conn.connect();
                    int response = conn.getResponseCode();
                    Log.d(TAG, "The response is: " + conn.getResponseMessage());
                    if (response == 200) {
                        StringBuilder sb = new StringBuilder();
                        is = conn.getInputStream();
                        InputStreamReader reader = new InputStreamReader(is, IOUtils.UTF8);
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
                    IOUtils.safeClose(is);
                }
            }
        });
    }

    protected class ActionHandler extends SiteNavigator.BaseActionHandler {
        protected ActionHandler(SiteNavigator nav, SiteNavigator.SrvClient client, Action action) {
            super(nav, client, action);
        }
        public void run() {
            setStatus("");
            String act = action.getAction();
            if ("list".equals(act)) {
                titleChildAt(0);
            }
            else if ("descr".equals(act)) {
                RsvpWords words = new RsvpWords()
                        .addTitleWords(nav.currentData.title)
                        .addPause()
                        .addIntroWords(nav.currentData.descr);
                introSetShown(nav.currentData, true);
                nav.currentData.currListPosition = -1;
                play(words);
                fillCommands();
                stopVoice();
                playVoice(nav.currentData.title);
                playVoice(nav.currentData.descr);
            }
            else if ("read".equals(act)) {
                stopVoice();
                nav.currentData.currListPosition = -1;
                RsvpWords words = new RsvpWords();
                words.addTitleWords(nav.currentData.title).addPause();
                playVoice(nav.currentData.title);
                if (nav.currentData.hasArticle) {
                    words.addArticleWords(nav.currentData.entity);
                    playVoice(nav.currentData.entity);
                } else {
                    words.addIntroWords(nav.currentData.descr);
                    playVoice(nav.currentData.descr);
                }
                play(words);
                fillCommands();
            }
            else {
                super.run();
            }
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
        mainTextScroller.fullScroll(View.FOCUS_UP);
    }

    protected void stopVoice() {
    }
    protected void playValueVoice(SSect ds) {
    }
    protected void playVoice(SEntity entity) {
    }

    protected void titleChildAt(int pos) {
        justRead = 0;
        final SSect ds = nav.currentData;
        if (ds != null) {
            if (ds.children == null || ds.children.length == 0) {
                ds.currListPosition = -1;
                rvChildren.setAdapter(null);
                rvChildren.setVisibility(View.GONE);
            } else {
                if (pos < 0)
                    pos = 0;
                if (pos >= ds.children.length)
                    pos = ds.children.length - 1;
                ds.currListPosition = pos;
                rvChildren.setAdapter(new SSectAdapter(ds));
                rvChildren.setVisibility(View.VISIBLE);
                RsvpWords words = new RsvpWords()
                        .addTitleWords(ds.title)
                        .addPause()
                        .addValueWords(ds);
                play(words);
                stopVoice();
                playVoice(ds.title);
                playValueVoice(ds);
            }
            justRead |= RsvpWords.JR_LIST;
        }
        fillCommands();
    }

    @Override
    public void showWhereAmIData(final SSect ds) {
        if (ds == null) {
            play(new RsvpWords().addWarning("Place is not known"));
            return;
        }
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

    @Override
    public void enterToRoom(final SSect ds) {
        justRead = 0;
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
        else if (ds.children != null && ds.children.length > 0) {
            rvChildren.setAdapter(new SSectAdapter(ds));
            rvChildren.setVisibility(View.VISIBLE);
        }
        else if (!ds.isValue) {
            words.addIntroWords(ds.descr);
            playVoice(ds.descr);
        }
        words.addValueWords(ds);
        playValueVoice(ds);
        play(words);
        if (rvChildren.getVisibility() == View.VISIBLE)
            justRead |= RsvpWords.JR_LIST;
        fillCommands();
    }

    @Override
    public void returnToRoom(SSect ds) {
        justRead = 0;
        if (ds.currListPosition >= 0) {
            titleChildAt(ds.currListPosition);
        } else {
            nav.doEnterToRoom(ds);
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

    @Override
    public void showMenu(final SSect action) {
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
                            showMenu(findParentMenu(nav.wholeMenuTree, action));
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
            nav.makeActionHandler(action.entity).run();
        }
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
                    nav.executeAction(action);
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
                if (sect.isAction) {
                    holder.action = Action.create(sect.entity.data);
                    if (sect.entity.props != null) {
                        for (String key : sect.entity.props.keySet())
                            holder.action.add(key, sect.entity.props.get(key));
                    }
                }
                else if (sect.hasArticle || sect.hasChildren || sect.children != null)
                    holder.action = new Action("enter").add("sectID", sect.guid).add("position", position);
                if (sect.isValue) {
                    holder.editor = new EdtValue(sect, nav, MainActivity.this);
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

    boolean permissionChecked;
    TextToSpeech tts;
    final String[] GEN_TTS_LANGS = {"ru", "en"};
    File tts_dir;
    ArrayList<String> tts_text_files;
    String tts_lang;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void runTTS() {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    runTTS();
                }
            });
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !permissionChecked) {
            int res = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (res != PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 1);
                return;
            }
            permissionChecked = true;
        }
        if (tts == null) {
            setStatus("Starting TTS");
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if (tts.setLanguage(new Locale(tts_lang)) < tts.LANG_AVAILABLE)
                        throw new RuntimeException("TTS languagre not supported");
                    Set<Voice> voices = tts.getVoices();
                    for (Voice v : voices) {
                        Log.i(TAG, "TTS Voice: " + v);
                        if (tts_lang.equals("ru") && v.getName().equals("ru-RU-locale"))
                            tts.setVoice(v);
                        if (tts_lang.equals("en") && v.getName().equals("en-GB-fis-network")) // en-US-sfg-network en-GB-fis-network
                            tts.setVoice(v);
                    }
                    runTTS();
                }
            });
            return;
        }
        // list TTS files
        if (tts_text_files == null) {
            tts_dir = new File(Environment.getExternalStorageDirectory(), "TTS");
            tts_text_files = new ArrayList<>();
            for (String fname : tts_dir.list()) {
                if (fname.endsWith(tts_lang + ".txt"))
                    tts_text_files.add(fname.substring(0, fname.length() - 4));
            }
        }
        while (!tts_text_files.isEmpty()) {
            String fname = tts_text_files.get(0);
            try {
                setStatus(fname);
                BufferedReader rd = new BufferedReader(new InputStreamReader(
                        new FileInputStream(new File(tts_dir, fname+".txt")), "UTF-8"));
                ArrayList<String> sentences = new ArrayList<>();
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    if (line.isEmpty())
                        continue;
                    sentences.add(line);
                    sb.append(line).append('\n');
                }
                tvText.setText(sb.toString());
                generateTTSFiles(fname, sentences, 0);
                return;
            } catch (IOException e) {
                Log.e(TAG, "Error reading text file for TTS: " + fname+".txt", e);
            }
        }
        // shutdown TTS
        setStatus("Shutdown TTS");
        tvText.setText("");
        tts.shutdown();
        tts = null;
        tts_text_files = null;

        int i = Arrays.binarySearch(GEN_TTS_LANGS, tts_lang);
        if (i+1 < GEN_TTS_LANGS.length) {
            tts_lang = GEN_TTS_LANGS[i+1];
            runTTS();
        } else {
            tts_lang = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void generateTTSFiles(final String fname, final ArrayList<String> list, final int pos) {
        if (pos >= list.size()) {
            new File(tts_dir, fname+".txt").delete();
            tts_text_files.remove(fname);
            runTTS();
            return;
        }
        final File file = new File(tts_dir, fname + "."+pos+".wav");
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }
            @Override
            public void onDone(String utteranceId) {
                generateTTSFiles(fname, list, pos+1);
            }
            @Override
            public void onError(String utteranceId) {
                file.delete();
                generateTTSFiles(fname, list, pos+1);
            }
        });
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "#"+pos);
        tts.synthesizeToFile(list.get(pos), null, file, "#"+pos);
        //tts.speak(list.get(pos), tts.QUEUE_ADD, params, "#"+pos);
    }

}
