package com.tencent.ilivedemo.model;

import com.tencent.ilivedemo.uiutils.DemoFunc;

import org.json.JSONObject;

/**
 * Created by xkazerzhang on 2017/6/22.
 */
public class ReplayInfo {
    private String userId;
    private String roomId;
    private String time;
    private String url;

    public ReplayInfo(JSONObject jsonObject) throws Exception{
        userId = jsonObject.optString("userid");
        roomId = jsonObject.getString("roomid");
        url = jsonObject.getString("url");
        long uTime = jsonObject.getLong("start_time");
        time = DemoFunc.getTimeStr(uTime*1000);
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
