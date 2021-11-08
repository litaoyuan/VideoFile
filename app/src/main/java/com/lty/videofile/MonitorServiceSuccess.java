package com.lty.videofile;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * @author litaoyuanli
 * @date 10/26/21
 * 描述信息
 */
public class MonitorServiceSuccess extends Service {
    // 定义MediaRecorder
    private MediaRecorder mMediaRecorder;
    private int type;
    public static boolean isStarted = false;
    private String path;
    private WindowManager mWindowManager;
    private View mRecorderView;
    // 摄像头ID（通常0代表后置摄像头，1代表前置摄像头）
    private String mCameraId = "1";
    // 定义代表摄像头的成员变量
    private CameraDevice cameraDeviceVideo;
    // 预览尺寸
    private Size previewSize;
    private CaptureRequest.Builder previewRequestBuilder;
    // 定义用于预览照片的捕获请求
    private CaptureRequest previewRequest;
    // 定义CameraCaptureSession成员变量
    private CameraCaptureSession captureSession;
    private Size mVideoSize;
    private Size mPreviewSize;
    private AutoFitTextureView textureView;
    private WindowManager.LayoutParams mLayoutParams;
    private String TAG = MonitorServiceSuccess.class.getName();

    ///为了使照片竖直显示
    @Override
    public void onCreate() {
        super.onCreate();
        mCameraId = SPUtils.getInstance().getAccountData("openCamera", false) ? "1" : "0";
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            openCamera(1, 1);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("打印服务11111===", type + "");
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        isStarted = false;
        if (mMediaRecorder != null) {
            try {
                stopRecordingVideo();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                Log.e("视频录制stop====", e.getMessage());
            }
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
        type = intent.getIntExtra("type", 0);
        Log.e("打印服务===", type + "");
        return null;
    }


    private void setUpMediaRecorder(int width, int height) throws IOException {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 获取指定摄像头的特性
            CameraCharacteristics characteristics = manager.getCameraCharacteristics("0");
            // 获取摄像头支持的配置属性
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // 获取摄像头支持的最大尺寸
            Size largest = Collections.max(
                    Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                    new VideoActivity.CompareSizesByArea());
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height, mVideoSize);
            // 获取最佳的预览尺寸
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, largest);
            // 根据选中的预览尺寸来调整预览组件（TextureView的）的长宽比
            int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                textureView.setAspectRatio(previewSize.getWidth(), previewSize.getHeight());
            } else {
                textureView.setAspectRatio(previewSize.getHeight(), previewSize.getWidth());
            }
            SetpMediaRecorder();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            System.out.println("出现错误。");
        }

    }

    private void SetpMediaRecorder() {
        try {
            mMediaRecorder = new MediaRecorder();
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            String mNextVideoAbsolutePath = "";
            mNextVideoAbsolutePath = PathUtil.getFileStorePath(this) + "/video_" + System.currentTimeMillis() + ".mp4";
            mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
            mMediaRecorder.setVideoEncodingBitRate(Integer.MAX_VALUE);
            mMediaRecorder.setVideoFrameRate(30);
            Toast.makeText(this, "手机分辨率width==" + mVideoSize.getWidth() + "==手机分辨率height=" + mVideoSize.getHeight(), Toast.LENGTH_LONG).show();
            mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setOnErrorListener((mr, what, extra) -> {
                Toast.makeText(this, "视频录制错误了", Toast.LENGTH_LONG).show();
            });
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                Log.i(TAG, "chooseVideoSize: " + size.toString());
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private void stopRecordingVideo() {
        try {
            mMediaRecorder.stop();

        } catch (RuntimeException stopException) {
        }
        mMediaRecorder.release();
        mMediaRecorder = null;
        Toast.makeText(this, "录制视频已保存", Toast.LENGTH_LONG).show();
    }

    private void updatePreview() {
        if (null == cameraDeviceVideo) {
            return;
        }
        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startRecordingVideo() {
        if (null == cameraDeviceVideo || !textureView.isAvailable() || null == mPreviewSize) {
            return;
        }
        try {

            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            CaptureRequest.Builder captureRequestBuilder = cameraDeviceVideo.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 获取手机方向
            int rotation = mWindowManager.getDefaultDisplay().getRotation();
            Log.e("打印相机方向===",""+rotation);
            // 根据设备方向计算设置照片的方向
            int orientation = 90;
            if (rotation == 1) {
                orientation = 270;
            } else if (rotation == 2) {
                orientation = 180;
            }
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, orientation);

            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            if (mMediaRecorder == null) {
                SetpMediaRecorder();
            }
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            captureRequestBuilder.addTarget(recorderSurface);
            previewRequestBuilder.addTarget(recorderSurface);

            cameraDeviceVideo.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession CaptureSession) {
                    captureSession = CaptureSession;
                    updatePreview();
                    mMediaRecorder.start();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e("配置失败", "配置失败");
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

//    // 根据手机的旋转方向确定预览图像的方向
//    private void configureTransform(int viewWidth, int viewHeight) {
//        if (null == previewSize) {
//            return;
//        }
//        // 获取手机的旋转方向
//        int rotation = mWindowManager.getDefaultDisplay().getRotation();
//        Matrix matrix = new Matrix();
//        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
//        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
//        float centerX = viewRect.centerX();
//        float centerY = viewRect.centerY();
//        // 处理手机横屏的情况
//        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
//            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
//            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
//            float scale = Math.max(
//                    (float) viewHeight / previewSize.getHeight(),
//                    (float) viewWidth / previewSize.getWidth());
//            matrix.postScale(scale, scale, centerX, centerY);
//            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
//        }
//        // 处理手机倒置的情况
//        else if (Surface.ROTATION_180 == rotation) {
//            matrix.postRotate(180, centerX, centerY);
//        }
//        textureView.setTransform(matrix);
//    }

    // 打开摄像头
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openCamera(int width, int height) {
        //表示输出设置（拍照后的保存设置）
        try {
            setUpMediaRecorder(width, height);
        } catch (IOException e) {
            e.printStackTrace();
        }
//        configureTransform(width, height);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // 如果用户没有授权使用摄像头，直接返回
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            // 打开摄像头
            manager.openCamera(mCameraId, stateCallback, null); // ①
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface surface = new Surface(texture);
            // 创建作为预览的CaptureRequest.Builder
            previewRequestBuilder = cameraDeviceVideo.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将textureView的surface作为CaptureRequest.Builder的目标

            //给此次请求添加一个Surface对象作为图像的输出目标，
            // CameraDevice返回的数据送到这个target surface中
            previewRequestBuilder.addTarget(surface);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求和拍照请求
            //第一个参数是一个数组，表示对相机捕获到的数据进行处理的相关容器组
            //第二个参数是状态回调
            //第三个参数设置线程
            cameraDeviceVideo.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() // ③
                    {
                        //完成配置时回调，可以开始拍照或预览、录像
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // 如果摄像头为null，直接结束方法
                            if (null == cameraDeviceVideo) {
                                return;
                            }
                            // 当摄像头已经准备好时，开始显示预览
                            captureSession = cameraCaptureSession;
                            // 设置自动对焦模式
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                            // 设置自动曝光模式
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                            // 开始显示相机预览
                            previewRequest = previewRequestBuilder.build();
                            try {
                                // 设置预览时连续捕获图像数据
                                captureSession.setRepeatingRequest(previewRequest, null, null);  // ④
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                            startRecordingVideo();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e("配置失败！", "配置失败！");
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private static Size chooseOptimalSize(Size[] choices
            , int width, int height, Size aspectRatio) {
        // 收集摄像头支持的打过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        // 如果找到多个预览尺寸，获取其中面积最小的。
        if (bigEnough.size() > 0) {
            return Collections.max(bigEnough, new CompareSizesByArea());
        } else {
            System.out.println("找不到合适的预览尺寸！！！");
            return choices[0];
        }
    }

    // 为Size定义一个比较器Comparator
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // 强转为long保证不会发生溢出
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        //  摄像头被打开时激发该方法
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            cameraDeviceVideo = cameraDevice;
            // 开始预览
            createCameraPreviewSession();  // ②
        }

        // 摄像头断开连接时激发该方法
        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            cameraDeviceVideo.close();
            cameraDeviceVideo = null;
        }

        // 打开摄像头出现错误时激发该方法
        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            cameraDeviceVideo.close();
            cameraDeviceVideo = null;
        }
    };
}
