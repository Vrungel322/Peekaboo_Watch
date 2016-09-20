package com.skinterface.demo.android;

import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, CapabilityApi.CapabilityListener
{

    public static final String TAG = "SkinterPhone";

    public static final String RSVP_CAPABILITY = "rsvp_demo";
    public static final String RSVP_PLAY_MESSAGE_PATH = "/rsvp_demo/play";
    public static final String RSVP_STOP_MESSAGE_PATH = "/rsvp_demo/stop";

    private static Charset utf8 = Charset.forName("UTF-8");

    GoogleApiClient mGoogleApiClient;
    private boolean connected;
    private Set<Node> wearNodes = Collections.emptySet();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.play1).setOnClickListener(this);
        findViewById(R.id.play2).setOnClickListener(this);
        findViewById(R.id.stop).setOnClickListener(this);
        findViewById(R.id.notify).setOnClickListener(this);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                // Request access only to the Wearable API
                .addApi(Wearable.API) //addApiIfAvailable
                .build();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.notify) {
            int notificationId = 001;
            // Build intent for notification content
            //Intent viewIntent = new Intent(this, MainActivity.class);
            //viewIntent.putExtra(EXTRA_EVENT_ID, eventId);
            //PendingIntent viewPendingIntent =
            //        PendingIntent.getActivity(this, 0, viewIntent, 0);

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(R.drawable.cast_ic_notification_play)
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
            return;
        }
        if (v.getId() == R.id.play1) {
            requestPlay("Луна в Овне\n" +
                    "Достаточно острое положение для Луны. Естественная физиологическая\n" +
                    "чувствительность притуплена тем, что Вы не проводите энергию через тело, она\n" +
                    "головой опосредована. С одной стороны, в эти дни тело становится как бы более\n" +
                    "выносливым, но это только на поверхности, т.к. просто тело не чувствует\n" +
                    "усталости и других своих потребностей. Тогда ощущения и питание, любое\n" +
                    "обслуживание тела происходят “от головы”, т.е. завязаны на принятые вами нормы и\n" +
                    "правила. Зато, если Вы тренированный человек – легко сможете уговорить себя\n" +
                    "выздороветь. Мозг при таком положении Луны перенасыщен энергией. Если специально\n" +
                    "не расслабляться - головные боли, головокружения, испорченные зубы (в т.ч.\n" +
                    "механически)и нарушения сна. В эти дни хорошо идёт острая, свежая пища, мясное.\n" +
                    "Противопоказаны манипуляции с зубами, лицевая пластика.");
        }
        if (v.getId() == R.id.play2) {
            requestPlay("Луна в Тельце\n" +
                    "Плодородное положение Луны. Она даёт много энергии для роста, движения. Хорошие\n" +
                    "дни для ухаживания за организмом другого человека, животных, растений, для\n" +
                    "рутинной, постоянной работы. Можете получить удовольствие от вязания, например.\n" +
                    "Выносливость в эти дни выше, нагрузки можно увеличить, а вот подняться тяжелее.\n" +
                    "Заболевшее горло говорит скорее о недостатке нагрузки. В питании в эти дни\n" +
                    "организм неприхотлив, переварить может много чего. Хороши заниматься хозяйством.\n" +
                    "Эти дни придают телу специфическое очарование здорового животного.\n" +
                    "\n" +
                    "Противопоказаны манипуляции с нижней челюстью, горлом.");
        }
        if (v.getId() == R.id.stop) {
            requestPlay(null);
        }
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

    private void setupRsvpNodes() {
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
    private void updateRsvpNodes(CapabilityInfo capabilityInfo) {
        wearNodes = capabilityInfo.getNodes();
    }
    private String pickBestNodeId() {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : wearNodes) {
            if (node.isNearby())
                return node.getId();
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    private void requestPlay(String text) {
        String nodeId = pickBestNodeId();
        if (nodeId == null)
            return;
        if (text == null)
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, RSVP_STOP_MESSAGE_PATH, null);
        else
            Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, RSVP_PLAY_MESSAGE_PATH, text.getBytes(utf8));
    }

}
