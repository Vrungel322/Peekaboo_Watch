package com.skinterface.demo.android;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.lang.ref.WeakReference;

public class RsvpView extends SurfaceView implements SurfaceHolder.Callback {

    public static final String ACTION_RSVP_EVENT     = "action.RSVP_EVENT";
    public static final String EXTRA_RSVP_STATE      = "state";
    public static final String RSVP_STATE_LOADED     = "loaded";
    public static final String RSVP_STATE_STARTED    = "started";
    public static final String RSVP_STATE_FINISHED   = "finished";

    static class RsvpState {
        final RsvpWords words;
        final int length;
        int next_pos;
        int tic; // 1000 /60
        long till_time;
        boolean playing;
        RsvpState(RsvpWords words, int tic, boolean playing) {
            this.words = words;
            this.length = words.size();
            this.tic = tic;
            this.till_time = SystemClock.uptimeMillis() + 4*tic;
            this.playing = playing;
        }
    }

    private final float[]    width = new float[8];
    private final Paint      paint = new Paint();
    private final RsvpThread thread = new RsvpThread(this);
    private Drawable         icon_mic;
    private Canvas           canvas;
    private int              initial_tic;
    private RsvpState        rsvpState;

    static final int BLACK = 0xFF0F0F0F;
    static final int GRAY = 0xFF4B4b4b;
    static final int RED = 0xFFD83332;
    static final int WHITE = 0xFFEEEEEE;
    int CANVAS_W = 480;
    int CANVAS_H = 116;
    int PIVOT_X = 180;
    int PIVOT_H = 14;
    int LINE_HW = 4;
    int TEXT_SZ = 48;
    int TEXT_Y = 76;

    public RsvpView(Context context) {
        super(context);
        init();
    }

    public RsvpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RsvpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        initial_tic = PreferenceManager.getDefaultSharedPreferences(getContext().getApplicationContext())
                .getInt("rsvp_tic", 35);
        getHolder().addCallback(this);
        icon_mic = null;//getContext().getResources().getDrawable(R.drawable.ic_mic_white_48dp);
        if (icon_mic != null) {
            icon_mic.mutate();
            icon_mic.setAlpha(64);
        }
        this.thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        CANVAS_W = width;
        CANVAS_H = height;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop(null);
    }

    void load(final RsvpWords words, boolean play) {
        if (words == null) {
            stop(null);
            return;
        }
        try { setKeepScreenOn(true); } catch (Exception e) {}
        rsvpState = new RsvpState(words, initial_tic, play);
        thread.requestStateTransit(RsvpThread.STATE_RSVP);
    }

    boolean isPaused() {
        RsvpState s = rsvpState;
        if (s == null)
            return false;
        return !s.playing;
    }

    void resume() {
        RsvpState s = rsvpState;
        if (s == null)
            return;
        if (s.playing)
            return;
        s.playing = true;
        s.till_time = SystemClock.uptimeMillis() + 4 * s.tic;
        thread.requestStateTransit(RsvpThread.STATE_RSVP);
    }

    void pause() {
        RsvpState s = rsvpState;
        if (s == null)
            return;
        if (!s.playing)
            return;
        s.playing = false;
        thread.requestStateTransit(RsvpThread.STATE_RSVP);
    }

    void stop(RsvpState s) {
        if (s == null) {
            s = rsvpState;
            if (s == null)
                return;
        }
        if (rsvpState == s) {
            rsvpState = null;
            thread.requestStateTransit(RsvpThread.STATE_STOP);
            if (rsvpState == null)
                try { setKeepScreenOn(false); } catch (Exception e) {}
        }
    }

    int getTic() {
        return initial_tic;
    }

    void setTic(int tic) {
        if (tic <  13) tic = 13;
        if (tic > 100) tic = 100;
        if (tic != initial_tic) {
            initial_tic = tic;
            RsvpState s = rsvpState;
            if (s != null)
                s.tic = tic;
        }
    }

    boolean lock(boolean fill_empty) {
        canvas = getHolder().lockCanvas();
        if (fill_empty && canvas != null)
            drawEmptyCanvas();
        return canvas != null;
    }

    void unlock() {
        if (canvas != null)
            getHolder().unlockCanvasAndPost(canvas);
        canvas = null;
    }
    private void drawEmptyCanvas() {
        Paint paint = this.paint;
        canvas.drawColor(BLACK);
        paint.setColor(BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2*LINE_HW);
        paint.setColor(GRAY);
        canvas.drawLine(0, LINE_HW, CANVAS_W, LINE_HW, paint);
        canvas.drawLine(0, CANVAS_H-LINE_HW, CANVAS_W, CANVAS_H-LINE_HW, paint);
        paint.setColor(RED);
        canvas.drawLine(PIVOT_X, 2*LINE_HW, PIVOT_X, PIVOT_H, paint);
        canvas.drawLine(PIVOT_X, CANVAS_H-2*LINE_HW, PIVOT_X, CANVAS_H-PIVOT_H, paint);
    }

    private void drawNextIdle() {
        try {
            if (!lock(true))
                return;
            if (icon_mic != null) {
                icon_mic.setBounds(CANVAS_W/2-40, CANVAS_H/2-40, CANVAS_W/2+40, CANVAS_H/2+40);
                icon_mic.draw(canvas);
            }
        } finally {
            unlock();
        }
    }

    private RsvpState drawNextWord() {
        try {
            final RsvpState s = rsvpState;
            if (s == null)
                return null;
            if (!lock(true)) {
                if (s.till_time != 0)
                    s.till_time = SystemClock.uptimeMillis() + 4 * s.tic;
                return s;
            }

            int pos = s.next_pos;
            if (pos == 0) {
                if (s.playing) {
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(
                            new Intent(ACTION_RSVP_EVENT)
                                    .putExtra(EXTRA_RSVP_STATE, RSVP_STATE_STARTED));
                    s.next_pos += 1;
                } else {
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(
                            new Intent(ACTION_RSVP_EVENT)
                                    .putExtra(EXTRA_RSVP_STATE, RSVP_STATE_LOADED));
                }
            }
            else if (s.playing && pos < s.length)
                s.next_pos += 1;

            if (pos >= s.length) {
                //Log.d("rsvp", "play finished with length:"+length);
                if (s == rsvpState)
                    rsvpState = null;
                stop(null);
                return rsvpState;
            }

            RsvpWords.Word w = s.words.get(pos);
            if (s.playing)
                s.till_time += w.weight * s.tic;
            else
                s.till_time = 0;
            //Log.d("rsvp", "play word:'"+w.word+"', pause:"+pauseCount);
            int length = w.word.length();
            for (int p=length-1; p > 0; --p) {
                char ch = w.word.charAt(p);
                if (ch == '.' || ch == ',' || ch == ';' || ch == ':' || ch == '-')
                    length -= 1;
                else
                    break;
            }
            int bestLetter = 4;
            switch (length) {
            case 0:
                bestLetter = 0; // first
                break;
            case 1:
                bestLetter = 1; // first
                break;
            case 2:
            case 3:
            case 4:
            case 5:
                bestLetter = 2; // second
                break;
            case 6:
            case 7:
            case 8:
            case 9:
                bestLetter = 3; // third
                break;
            }

            paint.setColor(WHITE);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(TEXT_SZ);
            paint.setTypeface(Typeface.MONOSPACE);
            paint.setStrokeWidth(0);
            float offs = 0;
            if (bestLetter > 0) {
                paint.getTextWidths(w.word, 0, bestLetter, width);
                for (int i=0; i < bestLetter-1; ++i)
                    offs += width[i];
                offs += width[bestLetter-1] / 2;
            }
            canvas.drawText(w.word, PIVOT_X-offs, TEXT_Y, paint);
            if (w.icon != 0 && w.icon != ' ')
                canvas.drawText(String.valueOf(w.icon), 2, TEXT_Y, paint);

            return rsvpState;
        } finally {
            unlock();
        }
    }


    static final class RsvpThread extends Thread {

        private static final int STATE_IDLE = 0; // nothing to do
        private static final int STATE_EXIT = 1; // asked to shutdown
        private static final int STATE_STOP = 2; // asked to stop, need to draw icons and become idle
        private static final int STATE_RSVP = 3; // drawing RSVP words

        private final Object lock = new Object();
        private final WeakReference<RsvpView> view_ref;
        private volatile int mState;
        private volatile boolean mStateUpdated;

        RsvpThread(RsvpView view) {
            super("rsvp view");
            setDaemon(true);
            this.view_ref = new WeakReference<>(view);
        }

        private int doWait(int state, long till_time) {
            synchronized (lock) {
                if (mState == state && !mStateUpdated) {
                    try {
                        if (till_time == 0) {
                            lock.wait();
                        } else {
                            long millis = till_time - SystemClock.uptimeMillis();
                            if (millis <= 0) {
                                mStateUpdated = false;
                                return mState;
                            }
                            lock.wait(millis);
                        }
                    } catch (InterruptedException e) {
                        mState = STATE_EXIT;
                        return STATE_EXIT;
                    }
                }
                mStateUpdated = false;
                return mState;
            }
        }

        boolean requestStateTransit(int old_state, int new_state) {
            synchronized (lock) {
                if (mState == old_state) {
                    mState = new_state;
                    lock.notifyAll();
                    if (new_state == STATE_IDLE) {
                        RsvpView view = view_ref.get();
                        if (view != null && old_state == STATE_RSVP)
                            LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(
                                    new Intent(ACTION_RSVP_EVENT)
                                            .putExtra(EXTRA_RSVP_STATE, RSVP_STATE_FINISHED));
                    }
                    return true;
                }
                return false;
            }
        }

        boolean requestStateTransit(int new_state) {
            synchronized (lock) {
                mState = new_state;
                mStateUpdated = true;
                lock.notifyAll();
                if (new_state == STATE_IDLE) {
                    RsvpView view = view_ref.get();
                    if (view != null)
                        LocalBroadcastManager.getInstance(view.getContext()).sendBroadcast(
                                new Intent(ACTION_RSVP_EVENT)
                                        .putExtra(EXTRA_RSVP_STATE, RSVP_STATE_FINISHED));
                }
                return true;
            }
        }

        @Override
        public void run() {
            for (;;) {
                int state = doWait(STATE_IDLE, 0);

                if (state == STATE_IDLE)
                    continue;
                else if (state == STATE_EXIT)
                    return;
                else if (state == STATE_STOP) {
                    RsvpView view = view_ref.get();
                    if (view == null)
                        return;
                    view.drawNextIdle();
                    requestStateTransit(STATE_STOP, STATE_IDLE);
                }
                else if (state == STATE_RSVP) {
                    // get RsvpState or stop
                    RsvpView view = view_ref.get();
                    if (view == null)
                        return;
                    RsvpState s = view.rsvpState;
                    if (s == null) {
                        requestStateTransit(state, STATE_STOP);
                        continue;
                    }
                    // check we need to draw the next word now
                    if (s.till_time == 0 || s.till_time - SystemClock.uptimeMillis() > 16) {
                        doWait(STATE_RSVP, s.till_time);
                        continue;
                    }
                    s = view.drawNextWord();
                    if (s == null)
                        requestStateTransit(state, STATE_STOP);
                    else
                        doWait(STATE_RSVP, s.till_time);
                }
            }
        }
    }
}
