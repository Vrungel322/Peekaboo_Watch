package com.skinterface.demo.android;

/*
 * Copyright (C) 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * A helper class to provide methods to record audio input from the MIC to the internal storage
 * and to playback the same recorded audio file.
 */
public class SoundRecorder {

    private static final String TAG = "SoundRecorder";
    private static final int RECORDING_RATE = 22050;
    private static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNELS_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private final String mOutputFileName;
    private final AudioManager mAudioManager;
    private final Context mContext;
    private State mState = State.IDLE;
    private int mFileSize;
    private int mDuration;

    private Handler mHandler;
    private AsyncTask<Void, Void, Boolean> mRecordingAsyncTask;
    private AsyncTask<Void, Void, Boolean> mPlayingAsyncTask;

    public enum State {
        IDLE, RECORDING, PLAYING
    }

    public SoundRecorder(Context context, String outputFileName, Handler handler) {
        mOutputFileName = outputFileName;
        mHandler = handler;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mContext = context;
    }

    public State getState() {
        return mState;
    }

    /**
     * Starts recording from the MIC.
     */
    public void startRecording() {
        if (mState != State.IDLE) {
            Log.w(TAG, "Requesting to start recording while state was not IDLE");
            return;
        }

        mRecordingAsyncTask = new AsyncTask<Void, Void, Boolean>() {

            private AudioRecord mAudioRecord;

            @Override
            protected void onPreExecute() {
                mState = State.RECORDING;
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                int BUFFER_SIZE = AudioRecord.getMinBufferSize(RECORDING_RATE, CHANNEL_IN, FORMAT);
                try {
                    if (BUFFER_SIZE < 0)
                        return Boolean.FALSE;
                    mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                            RECORDING_RATE, CHANNEL_IN, FORMAT, BUFFER_SIZE * 3);
                    if (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                        Log.e(TAG, "Created recorder with rate " + RECORDING_RATE);
                    } else {
                        Log.e(TAG, "FAIL with rate "+RECORDING_RATE);
                        mAudioRecord.release();
                        mAudioRecord = null;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "FAIL with rate "+RECORDING_RATE);
                }
                if (mAudioRecord == null)
                    return Boolean.FALSE;
                BufferedOutputStream bufferedOutputStream = null;
                try {
                    mFileSize = 0;
                    mDuration = 0;
                    long started = SystemClock.uptimeMillis();
                    bufferedOutputStream = new BufferedOutputStream(
                            mContext.openFileOutput(mOutputFileName, Context.MODE_PRIVATE));
                    byte[] buffer = new byte[BUFFER_SIZE];
                    mAudioRecord.startRecording();
                    mHandler.sendEmptyMessage(WearActivity.MSG_SOUND_REC_STARTED);
                    while (!isCancelled()) {
                        int read = mAudioRecord.read(buffer, 0, buffer.length);
                        bufferedOutputStream.write(buffer, 0, read);
                        mFileSize += read;
                        mDuration = (int)(SystemClock.uptimeMillis() - started);
                        mHandler.obtainMessage(WearActivity.MSG_SOUND_REC_PROGRESS,
                                mDuration, mFileSize).sendToTarget();
                    }
                    mDuration = (int)(SystemClock.uptimeMillis() - started);
                    bufferedOutputStream.close();
                    mState = State.IDLE;
                    mHandler.obtainMessage(WearActivity.MSG_SOUND_REC_FINISHED,
                            mDuration, mFileSize, mOutputFileName).sendToTarget();
                    return Boolean.TRUE;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to record data: " + e);
                    mState = State.IDLE;
                    mHandler.sendEmptyMessage(WearActivity.MSG_SOUND_REC_FAIL);
                    return Boolean.FALSE;
                } finally {
                    IOUtils.safeClose(bufferedOutputStream);
                    mAudioRecord.release();
                    mAudioRecord = null;
                }
            }

            @Override
            protected void onPostExecute(Boolean res) {
                mState = State.IDLE;
                mRecordingAsyncTask = null;
            }

            @Override
            protected void onCancelled() {
                mRecordingAsyncTask = null;
                if (mState == State.RECORDING) {
                    Log.d(TAG, "Stopping the recording ...");
                    mState = State.IDLE;
                } else {
                    Log.w(TAG, "Requesting to stop recording while state was not RECORDING");
                }
            }
        };

        mRecordingAsyncTask.execute();
    }

    public void stopRecording() {
        if (mRecordingAsyncTask != null) {
            mRecordingAsyncTask.cancel(true);
        }
    }

    public void stopPlaying() {
        if (mPlayingAsyncTask != null) {
            mPlayingAsyncTask.cancel(true);
        }
    }

    /**
     * Starts playback of the recorded audio file.
     */
    public void startPlay() {
        if (mState != State.IDLE) {
            Log.w(TAG, "Requesting to play while state was not IDLE");
            return;
        }

        if (!new File(mContext.getFilesDir(), mOutputFileName).exists()) {
            // there is no recording to play
            mHandler.sendEmptyMessage(WearActivity.MSG_SOUND_PLAY_FAIL);
            return;
        }
        final int intSize = AudioTrack.getMinBufferSize(RECORDING_RATE, CHANNELS_OUT, FORMAT);

        mPlayingAsyncTask = new AsyncTask<Void, Void, Boolean>() {

            private AudioTrack mAudioTrack;

            @Override
            protected void onPreExecute() {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0 /* flags */);
                mState = State.PLAYING;
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                FileInputStream in = null;
                try {
                    mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, RECORDING_RATE,
                            CHANNELS_OUT, FORMAT, intSize, AudioTrack.MODE_STREAM);
                    byte[] buffer = new byte[intSize * 2];
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        mAudioTrack.setVolume(AudioTrack.getMaxVolume() / 2);
                    mAudioTrack.play();
                    long started = SystemClock.uptimeMillis();
                    mHandler.sendEmptyMessage(WearActivity.MSG_SOUND_PLAY_STARTED);
                    in = mContext.openFileInput(mOutputFileName);
                    BufferedInputStream bis = new BufferedInputStream(in);
                    int read;
                    while (!isCancelled() && (read = bis.read(buffer, 0, buffer.length)) > 0) {
                        mAudioTrack.write(buffer, 0, read);
                        int millis = (int)(SystemClock.uptimeMillis() - started);
                        mHandler.obtainMessage(WearActivity.MSG_SOUND_PLAY_PROGRESS,
                                millis, mDuration).sendToTarget();
                    }
                    mState = State.IDLE;
                    mHandler.sendEmptyMessage(WearActivity.MSG_SOUND_PLAY_FINISHED);
                    return Boolean.TRUE;
                } catch (Exception e) {
                    Log.e(TAG, "Failed to playback", e);
                    mState = State.IDLE;
                    mHandler.sendEmptyMessage(WearActivity.MSG_SOUND_PLAY_FAIL);
                } finally {
                    IOUtils.safeClose(in);
                    if (mAudioTrack != null)
                        mAudioTrack.release();
                }
                return Boolean.FALSE;
            }

            @Override
            protected void onPostExecute(Boolean res) {
                cleanup();
            }

            @Override
            protected void onCancelled() {
                cleanup();
            }

            private void cleanup() {
                mState = State.IDLE;
                mPlayingAsyncTask = null;
            }
        };

        mPlayingAsyncTask.execute();
    }

    /**
     * Cleans up some resources related to {@link AudioTrack} and {@link AudioRecord}
     */
    public void cleanup() {
        Log.d(TAG, "cleanup() is called");
        stopPlaying();
        stopRecording();
    }
}