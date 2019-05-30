 /*
  * Copyright (C) 2019 Artem Radchenko
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

 package android.renesas.castscreendemo;

 import android.app.Notification;
 import android.app.NotificationManager;
 import android.app.PendingIntent;
 import android.app.Service;
 import android.content.BroadcastReceiver;
 import android.content.Context;
 import android.content.Intent;
 import android.content.IntentFilter;
 import android.content.SharedPreferences;
 import android.hardware.display.DisplayManager;
 import android.hardware.display.VirtualDisplay;
 import android.media.MediaCodecInfo;
 import android.media.MediaFormat;
 import android.media.projection.MediaProjection;
 import android.media.projection.MediaProjectionManager;
 import android.os.Handler;
 import android.os.IBinder;
 import android.os.Message;
 import android.os.Messenger;
 import android.os.StrictMode;
 import android.preference.PreferenceManager;
 import android.renesas.castscreendemo.streaming.RtspServer;
 import android.renesas.castscreendemo.streaming.SessionBuilder;
 import android.renesas.castscreendemo.streaming.VideoQuality;
 import android.util.Log;

 import java.util.ArrayList;

 import static android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR;
 import static android.renesas.castscreendemo.Config.CAST_DISPLAY_NAME;
 import static android.renesas.castscreendemo.Config.DEFAULT_I_FRAME_INTERVAL;
 import static android.renesas.castscreendemo.Config.DEFAULT_VIDEO_BITRATE;
 import static android.renesas.castscreendemo.Config.DEFAULT_VIDEO_BITRATE_MODE;
 import static android.renesas.castscreendemo.Config.DEFAULT_VIDEO_FRAMERATE;

 public class MyCastService extends Service {
     private final String TAG = "CastService";
     private final int NT_ID_CASTING = 5353;
     private Handler mHandler = new Handler(new ServiceHandlerCallback());
     private Messenger mMessenger = new Messenger(mHandler);
     private ArrayList<Messenger> mClients = new ArrayList<Messenger>();
     private IntentFilter mBroadcastIntentFilter;


     private MediaProjectionManager mMediaProjectionManager;
     private String mReceiverIp;
     private int mResultCode;
     private Intent mResultData;
     private String mSelectedFormat;
     private int mSelectedWidth;
     private int mSelectedHeight;
     private int mSelectedDpi;
     private int mSelectedBitrate;
     private int mSelectedBitrateMode;
     private int mSelectedFrameRate;
     private String mSelectedEncoderName;
     private MediaProjection mMediaProjection;
     private VirtualDisplay mVirtualDisplay;
     private DisplayManager mDisplayManager;
     private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
             String action = intent.getAction();
             Log.d(TAG, "Service receive broadcast action: " + action);
             if (action == null) {
                 return;
             }
             if (Config.ACTION_STOP_CAST.equals(action)) {
                 stopScreenCapture();
                 stopSelf();
             }
         }
     };

     @Override
     public void onCreate() {
         super.onCreate();
         StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
         StrictMode.setThreadPolicy(policy);
         mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
         mBroadcastIntentFilter = new IntentFilter();
         mBroadcastIntentFilter.addAction(Config.ACTION_STOP_CAST);
         registerReceiver(mBroadcastReceiver, mBroadcastIntentFilter);
     }

     @Override
     public void onDestroy() {
         super.onDestroy();
         Log.d(TAG, "Destroy service");
         stopScreenCapture();
         unregisterReceiver(mBroadcastReceiver);
     }

     @Override
     public int onStartCommand(Intent intent, int flags, int startId) {
         if (intent == null) {
             return START_NOT_STICKY;
         }
         mReceiverIp = intent.getStringExtra(Config.EXTRA_RECEIVER_IP);
         mResultCode = intent.getIntExtra(Config.EXTRA_RESULT_CODE, -1);
         mResultData = intent.getParcelableExtra(Config.EXTRA_RESULT_DATA);
         Log.d(TAG, "Remove IP: " + mReceiverIp);
         mSelectedWidth = intent.getIntExtra(Config.EXTRA_SCREEN_WIDTH, Config.DEFAULT_SCREEN_WIDTH);
         mSelectedHeight = intent.getIntExtra(Config.EXTRA_SCREEN_HEIGHT, Config.DEFAULT_SCREEN_HEIGHT);
         mSelectedDpi = intent.getIntExtra(Config.EXTRA_SCREEN_DPI, Config.DEFAULT_SCREEN_DPI);
         mSelectedBitrate = intent.getIntExtra(Config.EXTRA_VIDEO_BITRATE, DEFAULT_VIDEO_BITRATE);
         mSelectedBitrateMode = intent.getIntExtra(Config.EXTRA_VIDEO_BITRATE_MODE, DEFAULT_VIDEO_BITRATE_MODE);
         mSelectedFrameRate = intent.getIntExtra(Config.EXTRA_VIDEO_FRAMERATE, DEFAULT_VIDEO_FRAMERATE);
         mSelectedFormat = intent.getStringExtra(Config.EXTRA_VIDEO_FORMAT);
         mSelectedEncoderName = intent.getStringExtra(Config.EXTRA_VIDEO_ENCODER_NAME);
         if (mSelectedFormat == null) {
             mSelectedFormat = Config.DEFAULT_VIDEO_FORMAT;
         }
         Log.d(TAG, "Start with client mode");
         startScreenCapture();

         return START_STICKY;
     }

     @Override
     public IBinder onBind(Intent intent) {
         return mMessenger.getBinder();
     }

     private void showNotification() {
         final Intent notificationIntent = new Intent(Config.ACTION_STOP_CAST);
         PendingIntent notificationPendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
         Notification.Builder builder = new Notification.Builder(this);
         builder.setSmallIcon(R.mipmap.ic_launcher)
                 .setOnlyAlertOnce(true)
                 .setOngoing(true)
                 .setContentTitle(getString(R.string.app_name))
                 .setContentText(getString(R.string.casting_screen))
                 .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.action_stop), notificationPendingIntent);
         NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
         notificationManager.notify(NT_ID_CASTING, builder.build());
     }

     private void dismissNotification() {
         NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
         notificationManager.cancel(NT_ID_CASTING);
     }

     private void startScreenCapture() {
         if (mDisplayManager == null)
             mDisplayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
         if (mMediaProjection == null)
             mMediaProjection = mMediaProjectionManager.getMediaProjection(mResultCode, mResultData);
         Log.w(TAG, "startScreenCapture");
         prepareVirtualDisplay();
         prepareRtspServer();
         showNotification();
         Log.w(TAG, "startScreenCapture: ");
         startRtspServer();
     }

     private void prepareVirtualDisplay() {
         Log.d(TAG, "mResultCode: " + mResultCode + ", mResultData: " + mResultData);
         if (mResultCode != 0 && mResultData != null) {
             if (mVirtualDisplay == null) {
                 Log.w(TAG, "prepareVirtualDisplay: mSelectedWidth=" + mSelectedWidth);
                 Log.w(TAG, "prepareVirtualDisplay: mSelectedHeight=" + mSelectedHeight);
                 Log.w(TAG, "prepareVirtualDisplay: mSelectedDpi=" + mSelectedDpi);
                 mVirtualDisplay = mMediaProjection.createVirtualDisplay(CAST_DISPLAY_NAME, mSelectedWidth,
                         mSelectedHeight, mSelectedDpi, VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, null,
                         null, null);
             } else {
                 Log.e(TAG, "prepareVirtualDisplayMP: Display is already created");
             }
         }
     }

     private void prepareRtspServer() {
         SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
         editor.putString(RtspServer.KEY_PORT, String.valueOf(12345));
         editor.apply();
     }

     private void startRtspServer() {
         VideoQuality videoQuality = new VideoQuality(
                 mSelectedWidth,
                 mSelectedHeight,
                 mSelectedFrameRate,
                 mSelectedBitrate);
         Log.w(TAG, "startRtspServer: " + mSelectedEncoderName);
         SessionBuilder.getInstance()
                 .setVideoQuality(videoQuality)
                 .setVirtualDisplay(mVirtualDisplay)
                 .setVideoEncoderName(mSelectedEncoderName)
                 .setPreviewOrientation(90)
                 .setContext(getApplicationContext())
                 .setVideoEncoder(SessionBuilder.VIDEO_H264);

         startService(new Intent(this, RtspServer.class));
     }

     private MediaFormat getVideoFormat() {
         MediaFormat format = MediaFormat.createVideoFormat(mSelectedFormat, mSelectedWidth, mSelectedHeight);

         format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);


         format.setInteger(MediaFormat.KEY_BIT_RATE, mSelectedBitrate);
         Log.w(TAG, "getVideoFormat: bitrate=" + mSelectedBitrate);
         format.setInteger(MediaFormat.KEY_FRAME_RATE, mSelectedFrameRate);
         format.setInteger(MediaFormat.KEY_CAPTURE_RATE, mSelectedFrameRate);
         format.setInteger(MediaFormat.KEY_BITRATE_MODE, mSelectedBitrateMode);
         format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
         format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, DEFAULT_I_FRAME_INTERVAL);


         return format;
     }

     private void stopScreenCapture() {
         Log.w(TAG, "stopScreenCapture");
         dismissNotification();
         if (mMediaProjection != null) {
             mMediaProjection.stop();
             mMediaProjection = null;
         }
         if (mVirtualDisplay != null) {
             mVirtualDisplay.release();
             mVirtualDisplay = null;
         }

     }

     private class ServiceHandlerCallback implements Handler.Callback {
         @Override
         public boolean handleMessage(Message msg) {
             Log.d(TAG, "Handler got event, what: " + msg.what);
             switch (msg.what) {
                 case Config.MSG_REGISTER_CLIENT: {
                     mClients.add(msg.replyTo);
                     break;
                 }
                 case Config.MSG_UNREGISTER_CLIENT: {
                     mClients.remove(msg.replyTo);
                     break;
                 }
                 case Config.MSG_STOP_CAST: {
                     stopScreenCapture();
                     stopSelf();
                 }
             }
             return false;
         }
     }


 }