package com.lty.videofile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

/**
 * Created by liting on 17/2/17.
 */

public class PathUtil {

    public static String getFileStorePath(Context context) {
        String path = "";

        if (isSDCardMounted()) {
            path = context.getExternalFilesDir(null) + "/video/";
            //LogUtils.e("kkk", path);
        } else {
            path = context.getCacheDir() + "/video/";
        }
        create(path);
        return path;
    }

    public static String getApkPath(Context context) {
        String path = getFileStorePath(context) + "apk/";
        create(path);
        return path;
    }

    private static boolean isSDCardMounted() {
        boolean isSDCard = false;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            isSDCard = true;
        }
        return isSDCard;
    }

    private static void create(String path) {
        File folderDir = new File(path);
        if (!folderDir.exists() && folderDir.mkdirs()) {

        }
    }

    public static File createFile(Context context, String fullPath) {
        File file = new File(fullPath);
        if (file.exists()) {
            return file;
        }

        try {
            createParentFile(file.getParentFile());
            file.createNewFile();
            return file;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File createFolder(String path) {
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    private static void createParentFile(File file) throws IOException {
        if (file != null && !file.exists()) {
            createParentFile(file.getParentFile());
            file.mkdir();
        }
    }

    public static boolean isFileExist(String fullPath) {
        File file = new File(fullPath);
        return file.exists() && file.length() > 200;
    }

    public static boolean isDirectoryExist(String fullPath) {
        File file = new File(fullPath);
        return file.exists() && file.isDirectory();
    }


    public static void deleteFile(String filePath) {
        if (TextUtils.isEmpty(filePath))
            return;
        try {
            File file = new File(filePath);
            if (file.isFile() && file.exists()) {
                file.delete();
                Log.e("delete file", filePath);
            }
        } catch (Exception e) {
            Log.e("文件报错", e.toString());
        }
    }

    public static void deleteFile(Context context,String path) {
        deleteFolderFile(path, false);
    }



    public static void copyFile(File sourceFile, File targetFile) throws IOException {
        if (!sourceFile.exists()) {
            return;
        }
        if (!sourceFile.isFile()) {
            return;
        }
        if (!sourceFile.canRead()) {
            return;
        }
        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }
        if (targetFile.exists()) {
            targetFile.delete();
        }
        BufferedInputStream inBuff = null;
        BufferedOutputStream outBuff = null;
        try {
            // 新建文件输入流并对它进行缓冲
            inBuff = new BufferedInputStream(new FileInputStream(sourceFile));

            // 新建文件输出流并对它进行缓冲
            outBuff = new BufferedOutputStream(new FileOutputStream(targetFile));

            // 缓冲数组
            byte[] b = new byte[1024 * 5];
            int len;
            while ((len = inBuff.read(b)) != -1) {
                outBuff.write(b, 0, len);
            }
            // 刷新此缓冲的输出流
            outBuff.flush();
        } finally {
            // 关闭流
            if (inBuff != null)
                inBuff.close();
            if (outBuff != null)
                outBuff.close();
        }
    }


    /**
     * 获取文件夹大小
     *
     * @param file File实例
     * @return long
     */
    public static long getFolderSize(File file) {

        long size = 0;
        try {
            File[] fileList = file.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                if (fileList[i].isDirectory()) {
                    size = size + getFolderSize(fileList[i]);

                } else {
                    size = size + fileList[i].length();

                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //return size/1048576;
        return size;
    }

    /**
     * 删除指定目录下文件及目录
     *
     * @param deleteThisPath
     * @param filePath
     * @return
     */
    public static void deleteFolderFile(String filePath, boolean deleteThisPath) {
        if (!TextUtils.isEmpty(filePath)) {
            try {
                File file = new File(filePath);
                if (file.isDirectory()) {// 处理目录
                    File files[] = file.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        deleteFolderFile(files[i].getAbsolutePath(), true);
                    }
                }
                if (deleteThisPath) {
                    if (!file.isDirectory()) {// 如果是文件，删除
                        file.delete();
                    } else {// 目录
                        if (file.listFiles().length == 0) {// 目录下没有文件或者目录，删除
                            file.delete();
                        }
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 格式化单位
     *
     * @param size
     * @return
     */
    public static String getFormatSize(double size) {
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return "0KB";//size + "Byte(s)";
        }

        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(Double.toString(kiloByte));
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "KB";
        }

        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(Double.toString(megaByte));
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }

        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(Double.toString(gigaByte));
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }

    public static int[] getImageSize(String path) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        return new int[]{photoW, photoH};
    }

    public static void saveBitmapFile(Bitmap bitmap, String path) {

        File file = new File(path);//将要保存图片的路径
        try {
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));

            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
            bos.flush();
            bos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


//    /**
//     * 移动目录
//     * @param srcDirName     源目录完整路径
//     * @param destDirName    目的目录完整路径
//     * @return 目录移动成功返回true，否则返回false
//     */
//    public  static boolean moveDirectory(String srcDirName, String destDirName) {
//
//        File srcDir = new File(srcDirName);
//        if(!srcDir.exists() || !srcDir.isDirectory())
//            return false;
//
//        File destDir = new File(destDirName);
//        if(!destDir.exists())
//            destDir.mkdirs();
//
//        /**
//         * 如果是文件则移动，否则递归移动文件夹。删除最终的空源文件夹
//         * 注意移动文件夹时保持文件夹的树状结构
//         */
//        File[] sourceFiles = srcDir.listFiles();
//        for (File sourceFile : sourceFiles) {
//            if (sourceFile.isFile())
//                moveFile(sourceFile.getAbsolutePath(), destDir.getAbsolutePath());
//            else if (sourceFile.isDirectory())
//                moveDirectory(sourceFile.getAbsolutePath(), destDir.getAbsolutePath() + File.separator + sourceFile.getName());
//            else
//                ;
//        }
//        return srcDir.delete();
//    }

    /**
     * 移动文件
     *
     * @param srcFileName 源文件完整路径
     * @param destDirName 目的目录完整路径
     * @return 文件移动成功返回true，否则返回false
     */
    public static boolean moveFile(String srcFileName, String destDirName, String name) {

        File srcFile = new File(srcFileName);
        if (!srcFile.exists() || !srcFile.isFile())
            return false;

        File destDir = new File(destDirName);
        if (!destDir.exists())
            destDir.mkdirs();

        return srcFile.renameTo(new File(destDir + name));
    }
}
