package com.lty.videofile;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Author: luqihua
 * Time: 2017/12/4
 * Description: FileUtil
 */

public class FileUtil {
    private static final String ROOT_DIR = "aaamedia";//以aaa开头容易查找
    private static String sRootPath = "";
    private static boolean hasInitialize = false;

    public static void init(Context context) {
        if (hasInitialize) return;

        if (isSDCardMounted()) {
            sRootPath = context.getExternalFilesDir(null) + "/video/";
            //LogUtils.e("kkk", path);
        } else {
            sRootPath = context.getCacheDir() + "/video/";
        }
        create(sRootPath);
        hasInitialize = true;
    }
    private static void create(String path) {
        File folderDir = new File(path);
        if (!folderDir.exists() && folderDir.mkdirs()) {

        }
    }

    private static boolean isSDCardMounted() {
        boolean isSDCard = false;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            isSDCard = true;
        }
        return isSDCard;
    }


    public static File newMp4File() {
        SimpleDateFormat format = new SimpleDateFormat("MM_dd_HH_mm_ss", Locale.CHINA);
        return new File(sRootPath, "mp4_" + format.format(new Date()) + ".mp4");
    }

    public static File newAccFile() {
        SimpleDateFormat format = new SimpleDateFormat("MM_dd_HH_mm_ss", Locale.CHINA);
        return new File(sRootPath, "acc_" + format.format(new Date()) + ".acc");
    }
}
