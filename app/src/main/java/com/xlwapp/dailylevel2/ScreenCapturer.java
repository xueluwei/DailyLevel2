package com.xlwapp.dailylevel2;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wow on 3/23/18.
 */

public class ScreenCapturer {
    private static MediaProjection sMediaProjection;
    boolean isScreenCaptureStarted;
    OnImageCaptureScreenListener listener;
    private int mDensity;
    private Display mDisplay;
    private int mWidth;
    private int mHeight;
    private ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;
    private Handler mHandler;
    private String STORE_DIR;
    private Context mContext;

    public ScreenCapturer(Context context, MediaProjection mediaProjection, String savePath) {
        sMediaProjection = mediaProjection;
        mContext = context;

        isScreenCaptureStarted = false;

        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                mHandler = new Handler();
                Looper.loop();
            }
        }.start();

        Log.d("WOW", ":" + savePath);
        if (TextUtils.isEmpty(savePath)) {
            String externalStorageState = Environment.getExternalStorageState();
            if (externalStorageState.equals(Environment.MEDIA_MOUNTED)){
                //sd卡已经安装，可以进行相关文件操作
                String externalFilesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath();
                Log.d("WOW", "externalFilesDir:" + externalFilesDir);
                if (externalFilesDir != null) {
                    STORE_DIR = externalFilesDir;
                } else {
                    Toast.makeText(mContext, "No save path assigned!", Toast.LENGTH_SHORT);
                }
            }
        } else {
            STORE_DIR = savePath;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ScreenCapturer startProjection() {
        if (sMediaProjection != null) {
            File storeDir = new File(STORE_DIR);
            if (!storeDir.exists()) {
                boolean success = storeDir.mkdirs();
                if (!success) {
                    Log.d("WOW", "mkdir " + storeDir + "  failed");
                    return this;
                } else {
                    Log.d("WOW", "mkdir " + storeDir + "  success");
                }
            } else {
                Log.d("WOW", " " + storeDir + "  exist");
            }

        } else {
            Log.d("WOW", "get mediaprojection failed");
        }

        try {
            Thread.sleep(500); // 防止截屏截到 显示截屏权限的窗口
            isScreenCaptureStarted = true;
        } catch (InterruptedException e) {

        }

        WindowManager window = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = window.getDefaultDisplay();
        final DisplayMetrics metrics = new DisplayMetrics();
        // use getMetrics is 2030, use getRealMetrics is 2160, the diff is NavigationBar's height
        mDisplay.getRealMetrics(metrics);
        mDensity = metrics.densityDpi;
        mWidth = metrics.widthPixels;//size.x;
        mHeight = metrics.heightPixels;//size.y;

        //start capture reader
        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(
                "ScreenShot",
                mWidth,
                mHeight,
                mDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                mImageReader.getSurface(),
                null,
                mHandler);

        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {

                if (isScreenCaptureStarted) {
                    isScreenCaptureStarted = false;
                    Log.e("WOW", "start");
                    Image image = null;
                    FileOutputStream fos = null;
                    Bitmap bitmap = null;

                    try {
                        image = reader.acquireLatestImage();
                        if (image != null) {
//                            bitmap = ImageUtils.image_ARGB8888_2_bitmap(metrics, image);
                            bitmap = ImageUtils.image_2_bitmap(image, Bitmap.Config.ARGB_8888);
                            Log.e("WOW", "bitmap: " + bitmap);
                            if (null != listener) {
                                listener.imageCaptured(ImageUtils.bitmap2byte(bitmap, 80));
                            }

                            Date currentDate = new Date();
                            SimpleDateFormat date = new SimpleDateFormat("yyyyMMddhhmmss");
                            String fileName = STORE_DIR + File.separator + date.format(
                                    currentDate) + ".png";
                            fos = new FileOutputStream(fileName);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.fromFile(new File(fileName))));
                            Log.d("WOW", "End now!!!!!!  Screenshot saved in " + fileName);
                            Toast.makeText(mContext, "Screenshot saved in " + fileName,
                                    Toast.LENGTH_LONG);
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        if (null != fos) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        if (null != bitmap) {
                            bitmap.recycle();
                        }
                        if (null != image) {
                            image.close(); // close it when used and
                        }
                    }
                }
            }
        }, mHandler);
        sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
        return this;
    }

    public ScreenCapturer stopProjection() {
        isScreenCaptureStarted = false;
        Log.d("WOW", "Screen captured");
        mHandler.post(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                if (sMediaProjection != null) {
                    sMediaProjection.stop();
                }
            }
        });
        return this;
    }

    public ScreenCapturer setListener(OnImageCaptureScreenListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnImageCaptureScreenListener {
        public void imageCaptured(byte[] image);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class MediaProjectionStopCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mVirtualDisplay != null) {
                        mVirtualDisplay.release();
                    }
                    if (mImageReader != null) {
                        mImageReader.setOnImageAvailableListener(null, null);
                    }
                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
                }
            });
        }
    }
}
