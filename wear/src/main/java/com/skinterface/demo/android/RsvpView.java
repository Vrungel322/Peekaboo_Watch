package com.skinterface.demo.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class RsvpView extends SurfaceView implements SurfaceHolder.Callback {

    public interface RsvpViewListener {
        void onRsvpPlayStart();
        void onRsvpPlayStop();
    }

    static final int MSG_PLAY_STARTED  = 1;
    static final int MSG_PLAY_FINISHED = 2;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            RsvpViewListener l = listener;
            switch (msg.what) {
            case MSG_PLAY_STARTED:
                if (l != null)
                    l.onRsvpPlayStart();
                break;
            case MSG_PLAY_FINISHED:
                if (l != null)
                    l.onRsvpPlayStop();
                break;
            }
        }
    };

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> future;
    private SurfaceHolder surfaceHolder;
    private Paint paint = new Paint();
    private RsvpViewListener listener;

    static final int BLACK = 0xFF222222;
    static final int GRAY = 0xFFAAAAAA;
    static final int RED = 0xFFFF5555;
    static final int WHITE = 0xFFEEEEEE;
    int CANVAS_W = 480;
    int CANVAS_H = 160;
    int PIVOT_X = 180;
    int PIVOT_H = 24;
    int LINE_HW = 6;
    int TEXT_SZ = 56;
    int TEXT_Y = 100;

    public RsvpView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public RsvpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    public RsvpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getHolder().addCallback(this);
    }

    public RsvpViewListener getListener() {
        return listener;
    }

    public void setListener(RsvpViewListener listener) {
        this.listener = listener;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        CANVAS_W = width;
        CANVAS_H = height;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.surfaceHolder = holder;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop(future);
    }

    class RsvpPlayer implements Runnable {
        final RsvpWords words;
        final int length;
        final float[] width = new float[8];
        int wordsPosition = -5;
        int pauseCount;
        ScheduledFuture<?> future;

        RsvpPlayer(RsvpWords words) {
            this.words = words;
            this.length = words.size();
        }

        @Override
        public void run() {
            SurfaceHolder holder = surfaceHolder;
            Canvas c2d = null;
            try {
                if (--pauseCount > 0)
                    return;

                if (holder != null)
                    c2d = holder.lockCanvas(null);
                if (c2d == null)
                    return;
                Paint paint = RsvpView.this.paint;
                c2d.drawColor(BLACK);
                paint.setColor(BLACK);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(2*LINE_HW);
                //paint.setColor(BLACK);
                //c2d.drawRect(0, PIVOT_H, CANVAS_W, CANVAS_H - 2* PIVOT_H, paint);
                //c2d.drawRect(0, 0, CANVAS_W, CANVAS_H, paint);
                paint.setColor(GRAY);
                c2d.drawLine(0, LINE_HW, CANVAS_W, LINE_HW, paint);
                c2d.drawLine(0, CANVAS_H-LINE_HW, CANVAS_W, CANVAS_H-LINE_HW, paint);
                paint.setColor(RED);
                c2d.drawLine(PIVOT_X, 2*LINE_HW, PIVOT_X, PIVOT_H, paint);
                c2d.drawLine(PIVOT_X, CANVAS_H-2*LINE_HW, PIVOT_X, CANVAS_H-PIVOT_H, paint);

                wordsPosition += 1;
                if (wordsPosition < 0)
                    return;
                if (wordsPosition >= length) {
                    stop(future);
                    return;
                }
                RsvpWords.Word w = words.get(wordsPosition);
                pauseCount = w.weight;
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
                c2d.drawText(w.word, PIVOT_X-offs, TEXT_Y, paint);
                if (w.icon != 0 && w.icon != ' ')
                    c2d.drawText(String.valueOf(w.icon), 2, TEXT_Y, paint);

            } finally {
                if (c2d != null)
                    holder.unlockCanvasAndPost(c2d);
            }
        }
    }

    void play(final RsvpWords words) {
        ScheduledFuture<?> sf = future;
        if (sf != null)
            sf.cancel(false);
        setKeepScreenOn(true);
        RsvpPlayer player = new RsvpPlayer(words);
        sf = executor.scheduleAtFixedRate(player, 100, 35, TimeUnit.MILLISECONDS);
        player.future = sf;
        future = sf;
        handler.sendEmptyMessage(MSG_PLAY_STARTED);
    }

    void stop(ScheduledFuture<?> sf) {
        if (sf == null) {
            sf = future;
            if (sf == null)
                return;
        }
        sf.cancel(false);
        setKeepScreenOn(false);
        if (sf == future)
            future = null;
        handler.sendEmptyMessage(MSG_PLAY_FINISHED);
    }
}
