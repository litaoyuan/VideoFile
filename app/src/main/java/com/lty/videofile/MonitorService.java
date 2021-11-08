package com.lty.videofile;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import static android.os.Environment.DIRECTORY_DCIM;
import static android.os.Environment.DIRECTORY_MOVIES;

/**
 * @author litaoyuanli
 * @date 10/26/21
 * 描述信息
 */
public class MonitorService extends Service {
    public static boolean isStarted = false;
    private WindowManager mWindowManager;
    private View mRecorderView;
    private AutoFitTextureView textureView;
    private WindowManager.LayoutParams mLayoutParams;
    private CameraMediaRecorderHelper cameraMediaRecorderHelper;

    ///为了使照片竖直显示
    @Override
    public void onCreate() {
        super.onCreate();
        isStarted = true;
        //创建通知，帮助service保活
        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("视频")
                .setContentText("视频");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String notificationId = "RtStartServiceId";
            String notificationName = "RtStartServiceName";
            NotificationChannel channel = new NotificationChannel(notificationId, notificationName, NotificationManager.IMPORTANCE_HIGH);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            builder.setChannelId(notificationId);
        }
        Notification notification = builder.build();
        startForeground(1, notification);
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        mRecorderView = LayoutInflater.from(this).inflate(R.layout.recorder_layout, null);
        textureView = mRecorderView.findViewById(R.id.sv_recorder);
        mLayoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.N_MR1) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_TOAST;
        } else {
            mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mLayoutParams.gravity = Gravity.LEFT;
        mLayoutParams.width = 1;
        mLayoutParams.height = 1;
        mWindowManager.addView(mRecorderView, mLayoutParams);
        Configuration newConfig = getResources().getConfiguration();
        cameraMediaRecorderHelper = new CameraMediaRecorderHelper(textureView, newConfig.orientation);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        isStarted = false;
        if (cameraMediaRecorderHelper != null) {
            cameraMediaRecorderHelper.stopRecordingVideo();
        }
        stopForeground(true);
        if (mWindowManager != null) {
            if (mRecorderView != null) {
                mWindowManager.removeView(mRecorderView);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}