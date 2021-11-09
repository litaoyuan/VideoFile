package com.lty.videofile;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.RecoverableSecurityException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import static android.os.Environment.DIRECTORY_MOVIES;

/**
 * @author litaoyuanli
 * @date 11/6/21
 * 描述信息
 */
public class CameraMediaRecorderHelper {
    String TAG = "CameraMediaRecorderHelper";
    //传感器方向；
    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;

    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();


    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private AutoFitTextureView mTextureView;
    private CameraManager mCameraManager;
    private android.hardware.camera2.CameraDevice CameraDevice = null;
    //创建的session
    private CameraCaptureSession mCameraCaptureSession = null;
    //传感器方向；
    private int mSensorOrientation;

    //Camrea 的一些属性
    private CameraCharacteristics mCameraCharacteristics;
    //默认使用前置摄像头
    private int currentCameraId = CameraCharacteristics.LENS_FACING_FRONT;
    private Handler mCameraHandler;
    //创建线程
    private HandlerThread handlerThread;
    private SurfaceTexture surfaceTexture;
    //设置预览流surface
    private Surface mSurface;
    //获取到最佳预览尺寸；
    private Size previewSize;
    //获取到最佳视频尺寸；
    private Size mVideoSize;
    int preWidth;
    int preHeight;
    private MediaRecorder mMediaRecorder;
    //mediaRecorder 使用的surface 只能用mediaCodec 进行创建；
    private Surface mRecorderSurface;
    //视频会话捕获请求
    private CaptureRequest.Builder mVideoBuilder;
    //视频录制状态的标记
    private boolean mIsRecordingVideo;
    private CamcorderProfile profile;
    private int rotation;

    @SuppressLint("NewApi")
    public CameraMediaRecorderHelper(AutoFitTextureView mTextureView, int rotation) {
        this.mTextureView = mTextureView;
        this.rotation = rotation;
        currentCameraId = SPUtils.getInstance().getAccountData("openCamera", false) ? 1 : 0;
        //初始化线程
        initHandler();
        //设置textureView的监听；
        initTextureViewSetSurfaceTextureListener(mTextureView);
    }


    /***
     * 1
     * 初始化handler 线程绑定handlerThread 的looper ，相机传递参数时需要这个线程
     */
    private void initHandler() {
        handlerThread = new HandlerThread("camera_handler_Therad");
        handlerThread.start();
        mCameraHandler = new Handler(handlerThread.getLooper());
    }

    /***
     * 2
     * 设置TextureView监听器， 监听surfaceview 的回调
     * @param mTextureView
     */
    private void initTextureViewSetSurfaceTextureListener(TextureView mTextureView) {

        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            public void onSurfaceTextureSizeChanged(@Nullable SurfaceTexture surface, int width, int height) {


            }

            public void onSurfaceTextureUpdated(@Nullable SurfaceTexture surface) {
            }

            public boolean onSurfaceTextureDestroyed(@Nullable SurfaceTexture surface) {
                //释放camera
                releaseCamera();
                return true;
            }

            public void onSurfaceTextureAvailable(@Nullable SurfaceTexture surface, int width, int height) {
                preHeight = height;
                preWidth = width;
                Log.e(TAG, "width==" + width);
                Log.e(TAG, "height==" + height);
                Log.e(TAG, "相机打开");
                openCamera();
            }
        });

    }


    public void openCamera() {
        //创建相机的管理者
        mCameraManager = (CameraManager) BaseApplication.getContext().getSystemService(Context.CAMERA_SERVICE);
        //设置相机的特征信息；
        setCameraInfo(mCameraManager);
        if (ContextCompat.checkSelfPermission(BaseApplication.getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            mCameraManager.openCamera(String.valueOf(currentCameraId), mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }


    }


    /***
     * 设置摄像头信息；
     * @param mCameraManager
     */
    public void setCameraInfo(CameraManager mCameraManager) {
        try {
            // 返回当前设备中可用的相机列表
            String[] cameraIdList = mCameraManager.getCameraIdList();
            if (cameraIdList == null || cameraIdList.length <= 0) {
                return;

            }
            //遍历所有可用的相机设备；
            for (int i = 0; i < cameraIdList.length; i++) {
                //根据摄像头id返回该摄像头的相关信息
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(cameraIdList[i]);
                //获取摄像头方向。前置摄像头（LENS_FACING_FRONT）或 后置摄像头
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == currentCameraId) {
                    //设备等于前置；
//                    mCameraId = cameraIdList[i];
                    //后置特征信息；
                    mCameraCharacteristics = cameraCharacteristics;
                }
            }
            //相机是否支持新特征
            int supportLevel = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);

            if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                Log.e("initCameraInfo: ", "相机硬件不支持新特性");
                return;
            }
            // 获取摄像头支持的配置属性
            //获取StreamConfigurationMap，它是管理摄像头支持的所有输出格式和尺寸
            StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            //传感器方向；
            mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            // 预览集合中的数据；
            Size[] preSizes = map.getOutputSizes(SurfaceTexture.class);
            // 获取最佳的预览尺寸
            // Size previewSize =chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), pictureSize.getWidth(), pictureSize.getHeight(), 1080, 1920);
            previewSize = getMatchingPreSize(preSizes);

            //录制视频可用的尺寸大小
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

    }


    /**
     * 摄像头状态接口回调类:
     * 主要是负责回调摄像头的开启/断开/异常/销毁.我们使用CameraManager打开指定id的摄像头时需要添加这个回调.
     */

    public CameraDevice.StateCallback mStateCallback = new android.hardware.camera2.CameraDevice.StateCallback() {

        /**
         * 摄像头打开时
         * @param camera
         */
        @Override
        public void onOpened(@NonNull android.hardware.camera2.CameraDevice camera) {
            CameraDevice = camera;
            //当camera打开成功返回可用cameradevice 时，我们去创建session;
            Log.e(TAG, "相机打开好了");
            createSession();
        }

        /***
         * 摄像头断开；
         * @param camera
         */
        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            if (CameraDevice != null) {
                CameraDevice.close();
                CameraDevice = null;
            }
            Log.e(TAG, "摄像头断开");
        }

        /***
         * 摄像头出现异常操作
         * @param camera
         * @param error
         */
        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            if (CameraDevice != null) {
                CameraDevice.close();
                CameraDevice = null;
            }
            Log.e(TAG, "摄像头出现异常操作");
        }
    };


    /***
     *  创建session
     *
     */
    private void createSession() {
        if (null == CameraDevice || !mTextureView.isAvailable() || null == previewSize) {
            return;
        }
        Log.e(TAG, "createSession");
        mSurface = new Surface(mTextureView.getSurfaceTexture());
        try {
            //在mediaRecorder prepare()之后这个设置的mRecorderSurface 才可以使用,相当于我们创建
            //session 时就就初始化了一次mediaRecorder,并且这个mediRecorder状态到了prepare()时这个surface 才可以使用
            initMediaRecorder();
            SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
            //设置缓冲区预览的大小
            if (surfaceTexture != null) {
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            }
            //发起创建session 的请求 预览只需要一个surface
            CameraDevice.createCaptureSession(Arrays.asList(mSurface, mRecorderSurface), sessionStateCb, mCameraHandler);

        } catch (CameraAccessException | IllegalStateException | FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    /****
     * 创建session 的回调
     */
    private CameraCaptureSession.StateCallback sessionStateCb = new CameraCaptureSession
            .StateCallback() {

        /***
         * session 创建成功所走的回调函数；
         * @param session
         */
        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "session onConfigured id: " + session.getDevice().getId());
            mCameraCaptureSession = session;
            //发送视频预览请求；
            sendVideoPreviewRequest();
        }

        /***
         * session 创建失败的回调；
         * @param session
         */
        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
            Log.d(TAG, "create session fail id: " + session.getDevice().getId());
        }
    };

    /****
     * 发送视频录制的会话请求。使其重复进行捕获
     */
    private void sendVideoPreviewRequest() {
        try {
            mVideoBuilder = CameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            //预览的surface加入到管道；
            mVideoBuilder.addTarget(mSurface);
            //加入到管道中,视频录制的surface
            mVideoBuilder.addTarget(mRecorderSurface);
            mVideoBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            mVideoBuilder.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, new Range<>(30, 30));
            //进行重复请求预览
            mCameraCaptureSession.setRepeatingRequest(mVideoBuilder.build(), CaptureCallback, mCameraHandler);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    /****
     * 设置捕获的监听器；
     */
    public CameraCaptureSession.CaptureCallback CaptureCallback = new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            //super.onCaptureCompleted(session, request, result);
            if (result != null) {
                //可以从result 获取到一些底层返回的数据结果；如AE的状态什么的；
                Integer integer = result.get(CaptureResult.CONTROL_AE_STATE);
            }
        }


    };

/*************************------------------录制的方法----------------*************************************/

    /***
     * mediaRecorder 初始化的配置
     * @throws IOException
     */
    public void initMediaRecorder() throws IOException {
        if (mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
            //创建surface
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mRecorderSurface = MediaCodec.createPersistentInputSurface();
            }
        } else {
            // 重置之前的实例
            mMediaRecorder.reset();

        }
        if (mRecorderSurface != null) {
            // 设置surface
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mMediaRecorder.setInputSurface(mRecorderSurface);
            }
        }
//音频源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //视频源,意思是从Surface里面读取画面去录制
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //输出格式
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //捕获率
        mMediaRecorder.setCaptureRate(30);
        //帧率
        mMediaRecorder.setVideoFrameRate(30);
        //码率
        mMediaRecorder.setVideoEncodingBitRate(Integer.MAX_VALUE);
        //直接采用QUALITY_HIGH,这样可以提高视频的录制质量，但是不能设置编码格式和帧率等参数。
        profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        //视频宽高
        Log.e(TAG, "打印视频分辨率===" + profile.videoFrameWidth + "===" + profile.videoFrameHeight);
        mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        // 视频编码格式
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //音频编码格式
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //录制时间最大设置
        mMediaRecorder.setMaxDuration(-1);
        //文件最大设置
        mMediaRecorder.setMaxFileSize(-1);
        mMediaRecorder.setOnInfoListener((mr, what, extra) -> {
            Toast.makeText(BaseApplication.getContext(), "onInfo信息回掉了", Toast.LENGTH_SHORT).show();
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                mr.stop();
            }
        });
//        //设置视频录制输出的方向方向；
//        Log.e(TAG, "传感器方向==" + mSensorOrientation);
//        Log.e(TAG, "屏幕方向==" + rotation);
//        switch (mSensorOrientation) {
//            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
//                Log.e(TAG, "视频旋转方向==" + DEFAULT_ORIENTATIONS.get(rotation));
//                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
//                break;
//            case SENSOR_ORIENTATION_INVERSE_DEGREES:
//                Log.e(TAG, "视频旋转方向==" + INVERSE_ORIENTATIONS.get(rotation));
//                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
//                break;
//        }
        if (currentCameraId == 1) {//后置
            int pro = SPUtils.getInstance().getAccountData("openRearCamera", 0);
            ;
            mMediaRecorder.setOrientationHint(pro);
        } else {
            int pro = SPUtils.getInstance().getAccountData("openFrontCamera", 0);
            ;
            mMediaRecorder.setOrientationHint(pro);
        }
        String videoPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_MOVIES) + "/video_" + timeStamp2Date(System.currentTimeMillis()) + ".mp4";
        Log.e(TAG, "打印视频路径===" + videoPath);
        mMediaRecorder.setOutputFile(videoPath);
        try {
            mMediaRecorder.prepare();
            //开始进行录制；
            mMediaRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        }
        Log.d(TAG, "media recorder prepared!");
    }


    /***
     * 开始录制视频；
     */
    public void startRecordingVideo() {
        if (null == CameraDevice || !mTextureView.isAvailable() || null == previewSize) {
            return;
        }
        Log.d(TAG, "edc start video session");
        try {
            mIsRecordingVideo = true;
            //开启子线程，为了防止卡顿出现
            new Thread(() -> {
                try {
                    //初始化mediaRecorde；
                    initMediaRecorder();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }).start();

        } catch (Exception e) {
            mIsRecordingVideo = false;
            e.printStackTrace();
        }

    }

    /***
     * 停止视频录制；
     */

    public void stopRecordingVideo() {
        releaseCamera();
        mIsRecordingVideo = false;
        if (mMediaRecorder != null) {
            Log.d(TAG, "stop media recorder");
            try {
                mMediaRecorder.stop();
                Toast.makeText(BaseApplication.getContext(), "服务停止了视频保存成功", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }


    /**
     * 切换摄像头
     */
    public void switchCamera() {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                //遍历camera所有id 拿到相应的特征；
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                if (currentCameraId == CameraCharacteristics.LENS_FACING_BACK && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    currentCameraId = CameraCharacteristics.LENS_FACING_FRONT;
                    CameraDevice.close();
                    openCamera();
                    break;
                } else if (currentCameraId == CameraCharacteristics.LENS_FACING_FRONT && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    currentCameraId = CameraCharacteristics.LENS_FACING_BACK;
                    CameraDevice.close();
                    openCamera();
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /***
     * 释放资源；
     */
    public void releaseCamera() {
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        if (surfaceTexture != null) {
            surfaceTexture.release();
            surfaceTexture = null;
        }
        if (mCameraCaptureSession != null) {
            try {
                mCameraCaptureSession.stopRepeating();
                mCameraCaptureSession.abortCaptures();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
            mCameraCaptureSession = null;
        }

        if (CameraDevice != null) {
            CameraDevice.close();
            CameraDevice = null;
        }
        if (mCameraHandler != null) {
            mCameraHandler.removeCallbacksAndMessages(null);
            mCameraHandler = null;
        }
        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }

        mCameraManager = null;
        sessionStateCb = null;
        mStateCallback = null;


    }

/**************************辅助方法*************************************/

    /***
     * 获取到最佳的预览尺寸；
     * @param previewSize
     * @return
     */
    private Size getMatchingPreSize(Size[] previewSize) {
        Size selectSize = null;
        try {


            int textureViewWidth = preWidth; //屏幕分辨率宽
            int textureViewHeigt = preHeight; //屏幕分辨率高
            /**
             * 循环40次,让宽度范围从最小逐步增加,找到最符合屏幕宽度的分辨率,
             * 你要是不放心那就增加循环,肯定会找到一个分辨率,不会出现此方法返回一个null的Size的情况
             * ,但是循环越大后获取的分辨率就越不匹配
             */
            for (int j = 1; j < 41; j++) {
                for (int i = 0; i < previewSize.length; i++) { //遍历所有Size
                    Size itemSize = previewSize[i];
                    Log.e("tag", "当前itemSize 宽=" + itemSize.getWidth() + "高=" + itemSize.getHeight());
                    //判断当前Size高度小于屏幕宽度+j*5  &&  判断当前Size高度大于屏幕宽度-j*5
                    if (itemSize.getHeight() < (textureViewWidth + j * 5) && itemSize.getHeight() > (textureViewWidth - j * 5)) {
                        if (selectSize != null) { //如果之前已经找到一个匹配的宽度
                            if (Math.abs(textureViewHeigt - itemSize.getWidth()) < Math.abs(textureViewHeigt - selectSize.getWidth())) { //求绝对值算出最接近设备高度的尺寸
                                selectSize = itemSize;
                                continue;
                            }
                        } else {
                            selectSize = itemSize;
                        }

                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.e("tag", "getMatchingSize2: 选择的分辨率宽度=" + selectSize.getWidth());
        Log.e("tag", "getMatchingSize2: 选择的分辨率高度=" + selectSize.getHeight());
        return selectSize;
    }

    /***
     * 匹配合适的预览尺寸
     * @param choices
     * @param width
     * @param height
     * @param aspectRatio
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // 收集摄像头支持的大过预览Surface的分辨率
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            //没有合适的预览尺寸
            return choices[0];
        }
    }


    private static Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e("chooseVideoSize", "Couldn't find any suitable video size");
        return choices[choices.length - 1];


    }


    /**
     * 为Size定义一个比较器Comparator获取支持的最大尺寸；
     */
    static class CompareSizesByArea implements Comparator<Size> {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /***
     * 判断是否在录制状态；
     * @return
     */
    public boolean ismIsRecordingVideo() {
        return mIsRecordingVideo;
    }

    public String timeStamp2Date(long time) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日HH时mm分ss秒");
        return sdf.format(new Date(time));
    }
}
