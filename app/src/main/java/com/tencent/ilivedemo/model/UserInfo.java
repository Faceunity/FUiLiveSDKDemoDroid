package com.tencent.ilivedemo.model;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by tencent on 2016/8/18.
 */

public class UserInfo {
    private String account;
    private String password;
    private int room = 4321;

    private String replayUrl;

    private static UserInfo instance;

    public static UserInfo getInstance() {
        if (null == instance) {
            synchronized (UserInfo.class) {
                if (null == instance) {
                    instance = new UserInfo();
                }
            }
        }
        return instance;
    }

    private UserInfo() {
    }

    ;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRoom() {
        return room;
    }

    public void setRoom(int room) {
        this.room = room;
    }

    public String getReplayUrl() {
        return replayUrl;
    }

    public void setReplayUrl(String replayUrl) {
        this.replayUrl = replayUrl;
    }

    public void writeToCache(Context context) {
        SharedPreferences shareInfo = context.getSharedPreferences(Constants.USERINFO, 0);
        SharedPreferences.Editor editor = shareInfo.edit();
        editor.putString(Constants.ACCOUNT, account);
        editor.putString(Constants.PWD, password);
        editor.putInt(Constants.ROOM, room);
        editor.commit();
    }

    public void clearCache(Context context) {
        SharedPreferences shareInfo = context.getSharedPreferences(Constants.USERINFO, 0);
        SharedPreferences.Editor editor = shareInfo.edit();
        editor.clear();
        editor.commit();
    }

    public void getCache(Context context) {
        SharedPreferences shareInfo = context.getSharedPreferences(Constants.USERINFO, 0);
        account = shareInfo.getString(Constants.ACCOUNT, null);
        password = shareInfo.getString(Constants.PWD, null);
        room = shareInfo.getInt(Constants.ROOM, 1234);
    }
}
