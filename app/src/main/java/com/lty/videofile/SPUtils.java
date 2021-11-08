package com.lty.videofile;

import android.content.Context;
import android.content.SharedPreferences;


/**
 * @author litaoyuanli
 * @date 2020/4/7
 * @description sp数据持久化工具
 */
public class SPUtils {
    private static SPUtils spUtils = null;

    private SPUtils() {
    }

    public static SPUtils getInstance() {
        if (spUtils == null) {
            synchronized (SPUtils.class) {
                if (spUtils == null) {
                    spUtils = new SPUtils();
                }
            }
        }
        return spUtils;
    }

    private SharedPreferences mUserPreferences;

    private void initUserPreference() {
        if (null == mUserPreferences) {
            mUserPreferences = BaseApplication.getInstance().getSharedPreferences("sp_save_data_ltylty", Context.MODE_PRIVATE);
        }
    }

    public void setAccountData(String key, boolean value) {
        initUserPreference();
        mUserPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getAccountData(String key, boolean defauleValue) {
        initUserPreference();
        return mUserPreferences.getBoolean(key, defauleValue);
    }

    public void setAccountData(String key, String value) {
        initUserPreference();
        mUserPreferences.edit().putString(key, value).apply();
    }

    public long getAccountData(String key, long defauleValue) {
        initUserPreference();
        return mUserPreferences.getLong(key, defauleValue);
    }

    public void setAccountData(String key, long value) {
        initUserPreference();
        mUserPreferences.edit().putLong(key, value).apply();
    }

    public int getAccountData(String key, int defauleValue) {
        initUserPreference();
        return mUserPreferences.getInt(key, defauleValue);
    }

    public void setAccountData(String key, int value) {
        initUserPreference();
        mUserPreferences.edit().putInt(key, value).apply();
    }

    public String getAccountData(String key) {
        initUserPreference();
        return mUserPreferences.getString(key, "");
    }

    public String getAccountData(String key, String value) {
        initUserPreference();
        return mUserPreferences.getString(key, value);
    }


}
