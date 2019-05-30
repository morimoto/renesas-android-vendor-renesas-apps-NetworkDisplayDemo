/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.renesas.castscreendemo.streaming;

import android.content.Context;
import android.hardware.Camera.CameraInfo;
import android.hardware.display.VirtualDisplay;
import android.preference.PreferenceManager;

import java.io.IOException;

/**
 * Call {@link #getInstance()} to get access to the SessionBuilder.
 */
public class SessionBuilder {

    public final static String TAG = "SessionBuilder";

    /**
     * Can be used with {@link #setVideoEncoder}.
     */
    public final static int VIDEO_NONE = 0;

    /**
     * Can be used with {@link #setVideoEncoder}.
     */
    public final static int VIDEO_H264 = 1;
    // The SessionManager implements the singleton pattern
    private static volatile SessionBuilder sInstance = null;
    // Default configuration
    private VideoQuality mVideoQuality = VideoQuality.DEFAULT_VIDEO_QUALITY;
    private Context mContext;
    private int mVideoEncoder = VIDEO_H264;
    private int mCamera = CameraInfo.CAMERA_FACING_BACK;
    private int mTimeToLive = 64;
    private int mOrientation = 0;
    private boolean mFlash = false;
    private String mOrigin = null;
    private String mDestination = null;
    private Session.Callback mCallback = null;
    private VirtualDisplay mVirtualDisplay;
    private String mEncoderName = "test";

    // Removes the default public constructor
    private SessionBuilder() {
    }

    /**
     * Returns a reference to the {@link SessionBuilder}.
     *
     * @return The reference to the {@link SessionBuilder}
     */
    public final static SessionBuilder getInstance() {
        if (sInstance == null) {
            synchronized (SessionBuilder.class) {
                if (sInstance == null) {
                    SessionBuilder.sInstance = new SessionBuilder();
                }
            }
        }
        return sInstance;
    }

    /**
     * Creates a new {@link Session}.
     *
     * @return The new Session
     * @throws IOException
     */
    public Session build() {
        Session session;

        session = new Session();
        session.setOrigin(mOrigin);
        session.setDestination(mDestination);
        session.setTimeToLive(mTimeToLive);
        session.setCallback(mCallback);

        switch (mVideoEncoder) {
            case VIDEO_H264:
                VideoStream stream = new VideoStream(mCamera);
                if (mContext != null)
                    stream.setPreferences(PreferenceManager.getDefaultSharedPreferences(mContext));
                session.addVideoTrack(stream);
                break;
        }

        if (session.getVideoTrack() != null) {
            VideoStream video = session.getVideoTrack();
            video.setVideoQuality(mVideoQuality);
            video.setVirtualDisplay(mVirtualDisplay);
            video.setPreviewOrientation(mOrientation);
            video.setDestinationPorts(5006);
            video.setEncoderName(mEncoderName);
        }

        return session;

    }

    public SessionBuilder setFlashEnabled(boolean enabled) {
        mFlash = enabled;
        return this;
    }

    public VirtualDisplay getVirtualDisplay() {
        return mVirtualDisplay;
    }

    public SessionBuilder setVirtualDisplay(VirtualDisplay display) {
        mVirtualDisplay = display;
        return this;
    }

    /**
     * Sets the orientation of the preview.
     *
     * @param orientation The orientation of the preview
     */
    public SessionBuilder setPreviewOrientation(int orientation) {
        mOrientation = orientation;
        return this;
    }

    public SessionBuilder setCallback(Session.Callback callback) {
        mCallback = callback;
        return this;
    }

    /**
     * Returns the context set with {@link #setContext(Context)}
     */
    public Context getContext() {
        return mContext;
    }

    /**
     * Access to the context is needed for the H264Stream class to store some stuff in the SharedPreferences.
     * Note that you should pass the Application context, not the context of an Activity.
     **/
    public SessionBuilder setContext(Context context) {
        mContext = context;
        return this;
    }

    /**
     * Returns the destination ip address set with {@link #setDestination(String)}.
     */
    public String getDestination() {
        return mDestination;
    }

    /**
     * Sets the destination of the session.
     */
    public SessionBuilder setDestination(String destination) {
        mDestination = destination;
        return this;
    }

    /**
     * Returns the origin ip address set with {@link #setOrigin(String)}.
     */
    public String getOrigin() {
        return mOrigin;
    }

    /**
     * Sets the origin of the session. It appears in the SDP of the session.
     */
    public SessionBuilder setOrigin(String origin) {
        mOrigin = origin;
        return this;
    }

    /**
     * Returns the id of the {@link android.hardware.Camera} set with {@link #setCamera(int)}.
     */
    public int getCamera() {
        return mCamera;
    }

    public SessionBuilder setCamera(int camera) {
        mCamera = camera;
        return this;
    }

    /**
     * Returns the video encoder set with {@link #setVideoEncoder(int)}.
     */
    public int getVideoEncoder() {
        return mVideoEncoder;
    }

    /**
     * Sets the default video encoder.
     */
    public SessionBuilder setVideoEncoder(int encoder) {
        mVideoEncoder = encoder;
        return this;
    }

    /**
     * Returns the VideoQuality set with {@link #setVideoQuality(VideoQuality)}.
     */
    public VideoQuality getVideoQuality() {
        return mVideoQuality;
    }

    /**
     * Sets the video stream quality.
     */
    public SessionBuilder setVideoQuality(VideoQuality quality) {
        mVideoQuality = quality.clone();
        return this;
    }

    /**
     * Returns the flash state set with {@link #setFlashEnabled(boolean)}.
     */
    public boolean getFlashState() {
        return mFlash;
    }

    /**
     * Returns the time to live set with {@link #setTimeToLive(int)}.
     */
    public int getTimeToLive() {
        return mTimeToLive;
    }

    public SessionBuilder setTimeToLive(int ttl) {
        mTimeToLive = ttl;
        return this;
    }

    /**
     * Returns a new {@link SessionBuilder} with the same configuration.
     */
    public SessionBuilder clone() {
        return new SessionBuilder()
                .setDestination(mDestination)
                .setOrigin(mOrigin)
                .setVirtualDisplay(mVirtualDisplay)
                .setPreviewOrientation(mOrientation)
                .setVideoQuality(mVideoQuality)
                .setVideoEncoder(mVideoEncoder)
                .setFlashEnabled(mFlash)
                .setCamera(mCamera)
                .setVideoEncoderName(mEncoderName)
                .setTimeToLive(mTimeToLive)
                .setContext(mContext)
                .setCallback(mCallback);
    }

    public SessionBuilder setVideoEncoderName(String mSelectedEncoderName) {
        mEncoderName = mSelectedEncoderName;
        return this;
    }
}

