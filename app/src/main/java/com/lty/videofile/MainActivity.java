package com.lty.videofile;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.lty.videofile.databinding.ActivityMainBinding;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

import static android.os.Environment.DIRECTORY_DCIM;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding mBinding;
    public Activity mActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!EventBus.getDefault().isRegistered(this)) {//加上判断
            EventBus.getDefault().register(this);
        }
        mActivity = this;
        mBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        initView();
        showAvailableSize();
        initOpenRear();
        initOpenFront();
        initProfile();
    }

    private void initProfile() {
        //1、最高  6、1080P  5、720P  4、480P
        int gaoqingdu = SPUtils.getInstance().getAccountData("initProfile", 6);
        switch (gaoqingdu) {
            case 4:
                mBinding.checkbox1.setChecked(false);
                mBinding.checkbox2.setChecked(false);
                mBinding.checkbox3.setChecked(false);
                mBinding.checkbox4.setChecked(true);
                break;
            case 5:
                mBinding.checkbox1.setChecked(false);
                mBinding.checkbox2.setChecked(false);
                mBinding.checkbox3.setChecked(true);
                mBinding.checkbox4.setChecked(false);
                break;
            case 1:
                mBinding.checkbox1.setChecked(false);
                mBinding.checkbox2.setChecked(true);
                mBinding.checkbox3.setChecked(false);
                mBinding.checkbox4.setChecked(false);
                break;
            default:
                mBinding.checkbox1.setChecked(true);
                mBinding.checkbox2.setChecked(false);
                mBinding.checkbox3.setChecked(false);
                mBinding.checkbox4.setChecked(false);
                break;
        }
        //1、最高  6、1080P  5、720P  4、480P
        mBinding.llCheck1.setOnClickListener(v -> {
            SPUtils.getInstance().setAccountData("initProfile", 6);
            mBinding.checkbox1.setChecked(true);
            mBinding.checkbox2.setChecked(false);
            mBinding.checkbox3.setChecked(false);
            mBinding.checkbox4.setChecked(false);
        });
        mBinding.llCheck2.setOnClickListener(v -> {
            SPUtils.getInstance().setAccountData("initProfile", 1);
            mBinding.checkbox2.setChecked(true);
            mBinding.checkbox1.setChecked(false);
            mBinding.checkbox3.setChecked(false);
            mBinding.checkbox4.setChecked(false);
        });
        mBinding.llCheck3.setOnClickListener(v -> {
            SPUtils.getInstance().setAccountData("initProfile", 5);
            mBinding.checkbox2.setChecked(false);
            mBinding.checkbox1.setChecked(false);
            mBinding.checkbox3.setChecked(true);
            mBinding.checkbox4.setChecked(false);
        });
        mBinding.llCheck4.setOnClickListener(v -> {
            SPUtils.getInstance().setAccountData("initProfile", 4);
            mBinding.checkbox2.setChecked(false);
            mBinding.checkbox1.setChecked(false);
            mBinding.checkbox3.setChecked(false);
            mBinding.checkbox4.setChecked(true);
        });
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        if (!SPUtils.getInstance().getAccountData("openCamera", false)) {
            mBinding.box2.setChecked(true);
            mBinding.box1.setChecked(false);
        } else {
            mBinding.box1.setChecked(true);
            mBinding.box2.setChecked(false);
        }
        mBinding.ll1.setOnClickListener(v -> {
            mBinding.box1.setChecked(true);
            mBinding.box2.setChecked(false);
            SPUtils.getInstance().setAccountData("openCamera", true);
        });
        mBinding.ll2.setOnClickListener(v -> {
            mBinding.box2.setChecked(true);
            mBinding.box1.setChecked(false);
            SPUtils.getInstance().setAccountData("openCamera", false);
        });
        mBinding.imgOpen.setOnClickListener(v -> {
            if (XXPermissions.isGranted(mActivity, Permission.CAMERA, Permission.MANAGE_EXTERNAL_STORAGE, Permission.RECORD_AUDIO, Permission.SYSTEM_ALERT_WINDOW)) {
                openService();
            } else {
                XXPermissions.with(mActivity).permission(Permission.CAMERA, Permission.MANAGE_EXTERNAL_STORAGE, Permission.RECORD_AUDIO, Permission.SYSTEM_ALERT_WINDOW).request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(List<String> permissions, boolean all) {
                        if (all) {
                            openService();
                        } else {
                            Toast.makeText(MainActivity.this, "请先授权", Toast.LENGTH_SHORT).show();
                        }

                    }

                    @Override
                    public void onDenied(List<String> permissions, boolean never) {
                        Toast.makeText(MainActivity.this, "请先授权", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        mBinding.imgStop.setOnClickListener(v -> stopSevrice());
        mBinding.tvFrontJiaodu.setOnClickListener(v -> {
            int pro = SPUtils.getInstance().getAccountData("openFrontCamera", 0);
            if (pro >= 360) {
                SPUtils.getInstance().setAccountData("openFrontCamera", 0);
            } else {
                pro += 90;
                SPUtils.getInstance().setAccountData("openFrontCamera", pro);
            }
            initOpenFront();
        });
        mBinding.tvRearJiaodu.setOnClickListener(v -> {
            int pro = SPUtils.getInstance().getAccountData("openRearCamera", 0);
            if (pro >= 360) {
                SPUtils.getInstance().setAccountData("openRearCamera", 0);
            } else {
                pro += 90;
                SPUtils.getInstance().setAccountData("openRearCamera", pro);
            }
            initOpenRear();
        });
    }

    private void initOpenFront() {
        int pro = SPUtils.getInstance().getAccountData("openFrontCamera", 0);
        mBinding.tvFrontJiaodu.setText("后置摄像头旋转" + pro + "度");
    }

    private void initOpenRear() {
        int pro = SPUtils.getInstance().getAccountData("openRearCamera", 0);
        mBinding.tvRearJiaodu.setText("前置摄像头旋转" + pro + "度");
    }

    private void stopSevrice() {
        mBinding.imgOpen.setText("开始视频录制");
        stopService(new Intent(mActivity, MonitorService.class));
        Log.e("CameraMedia", "停止服务");
    }


    private void openService() {
        if (!MonitorService.isStarted) {
            Log.e("CameraMedia", "开启服务");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(mActivity, MonitorService.class));
            } else {
                startService(new Intent(mActivity, MonitorService.class));
            }
            mBinding.imgOpen.setText("视频录制中");
            Toast.makeText(mActivity, "视频录制中", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (EventBus.getDefault().isRegistered(this)) {//加上判断
            EventBus.getDefault().unregister(this);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getNull(BaseEvent event) {
        Log.e("CameraMedia", "BaseEvent");
        stopSevrice();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                openService();
            }
        }, 500);
    }

    /**
     * 显示存储的剩余空间
     */

    @SuppressLint("SetTextI18n")
    public void showAvailableSize() {

        long romSize = getAvailSpace(Environment.getDataDirectory().getAbsolutePath());//手机内部存储大小

        long sdSize = getAvailSpace(Environment.getExternalStorageDirectory().getAbsolutePath());//外部存储大小
        mBinding.tvSize.setText("内存可用空间: " + Formatter.formatFileSize(this, romSize) + "\n" + "SD卡可用空间:" + Formatter.formatFileSize(this, sdSize));

    }

    /**
     * 获取某个目录的可用空间
     */

    public long getAvailSpace(String path) {

        StatFs statfs = new StatFs(path);

        long size = statfs.getBlockSize();//获取分区的大小

        long count = statfs.getAvailableBlocks();//获取可用分区块的个数

        return size * count;

    }

    /**
     * 获取系统可读写的总空间
     *
     * @return
     */

    public String getSysTotalSize() {
        long zhao = getTotalSize("/data");
        if (zhao / 1024 > 1024) {
            String gb = (zhao / 1024 / 1024) + "GB";
            if (zhao / 1024 % 1024 > 0) {
                return gb + (zhao % 1024) + "MB";
            } else {
                return gb;
            }
        } else {
            return zhao + "MB";
        }
    }

    /**
     * 计算总空间
     *
     * @param path
     * @return
     */

    private long getTotalSize(String path) {
        StatFs fileStats = new StatFs(path);
        fileStats.restat(path);
        return (long) fileStats.getBlockCount() * fileStats.getBlockSize();
    }
}