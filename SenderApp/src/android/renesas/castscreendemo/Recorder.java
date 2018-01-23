package android.renesas.castscreendemo;

import android.content.Context;
import android.content.Intent;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Created by artem on 11/8/17.
 */

public class Recorder {
    private static final String TAG = "Recorder";

    private MediaCodec mVideoEncoder;
    private MediaCodec.BufferInfo mVideoBufferInfo;
    private Handler mHandler;
    private Context mContext;
    private OutputStream mSocketOutputStream;
    final Intent mStopCastIntent = new Intent(Config.ACTION_STOP_CAST);
    private MediaFormat mFormat;
    private Runnable mWriteDataRunnable;

    public Recorder(Context context, Handler handler) {
        this.mContext=context;
        mHandler=handler;


    }

    private Handler mDrainHandler = new Handler();
    private Runnable mDrainEncoderRunnable = new Runnable() {
        @Override
        public void run() {
            drainEncoder();
        }
    };

    public void startEncoding(OutputStream socketOutputStream) {
        if(socketOutputStream==null ) {
            mContext.sendBroadcast(mStopCastIntent);
            return;
        }
        this.mSocketOutputStream=socketOutputStream;
        mDrainHandler.post(mDrainEncoderRunnable);
    }


    public Surface prepareEncoder(final MediaFormat format, MediaCodec encoder) {
        Surface inputSurface=null;
        mFormat=format;
        try {
            mVideoBufferInfo = new MediaCodec.BufferInfo();
            mVideoEncoder = encoder;
            mVideoEncoder.setCallback(new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                    Log.w(TAG, "onInputBufferAvailable() [" + index + "]");
                }

                @Override
                public void onOutputBufferAvailable(@NonNull MediaCodec codec, final int index, @NonNull MediaCodec.BufferInfo info) {
                    mVideoBufferInfo=info;
                    if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.e(TAG, "onOutputBufferAvailable: EOS");
                        return;
                    }
                    if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
                        Log.w(TAG, "onOutputBufferAvailable: BUFFER_FLAG_KEY_FRAME");
                    }
                    if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) != 0) {
                        Log.w(TAG, "onOutputBufferAvailable: BUFFER_FLAG_PARTIAL_FRAME");
                    }
                    if ((mVideoBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Log.w(TAG, "onOutputBufferAvailable: BUFFER_FLAG_CODEC_CONFIG");
                        mFormat=codec.getOutputFormat(index);
                    }

                    final ByteBuffer data = codec.getOutputBuffer(index);
                    if(data==null) {
                        Log.e(TAG, "onOutputBufferAvailable: no data" );
                        return;
                    }

                    data.position(mVideoBufferInfo.offset);
                    int size = data.remaining();
                    final byte[] buffer = new byte[size];
                    data.get(buffer);
                    mVideoEncoder.releaseOutputBuffer(index, true);

                    writeSampleData(buffer, 0, size);
                    }

                @Override
                public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                    Log.e(TAG, "onError: code  "+e.getErrorCode()+" "+e.getDiagnosticInfo() );
                }

                @Override
                public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                    Log.e(TAG, " INFO_OUTPUT_FORMAT_CHANGED");
                    mFormat=format;
                }
            });
            mVideoEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            inputSurface = mVideoEncoder.createInputSurface();
            mVideoEncoder.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initial encoder, e: " + e);
            mContext.sendBroadcast(mStopCastIntent);
        }

        return inputSurface;
    }


    private boolean drainEncoder() {
        Log.w(TAG, "drainEncoder" );
        mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
        return true;
    }

    private void writeSampleData(final byte[] buffer, final int offset, final int size) {
        mWriteDataRunnable=new Runnable() {
            @Override
            public void run() {
                if (mSocketOutputStream != null) {
                    try {

                        mSocketOutputStream.write(buffer, offset, size);

                    } catch (IOException e) {
                        Log.e(TAG, "Failed to write data to socket, stop casting",e);
                        e.printStackTrace();
                        mContext.sendBroadcast(mStopCastIntent);
                    }
                }else {
                    Log.e(TAG, "writeSampleData: socket null" );
                    mContext.sendBroadcast(mStopCastIntent);
                }
            }
        };
        mHandler.post(mWriteDataRunnable);
    }



    public void releaseEncoders() {
        Log.w(TAG, "releaseEncoders: ");
        mDrainHandler.removeCallbacks(mDrainEncoderRunnable);
        mHandler.removeCallbacks(mWriteDataRunnable);
        if (mVideoEncoder != null) {
            mVideoEncoder.stop();
            mVideoEncoder.release();
            mVideoEncoder = null;
        }
        mVideoBufferInfo = null;
    }

}
