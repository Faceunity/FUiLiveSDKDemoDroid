package com.tencent.ilivedemo.model;

/**
 * Created by xkazerzhang on 2017/5/24.
 */
public class Constants {
    // 存储
    public static final String USERINFO = "userInfo";
    public static final String ACCOUNT = "account";
    public static final String PWD = "password";
    public static final String ROOM = "room";

    // 角色
    public static final String ROLE_MASTER = "LiveMaster";
    public static final String ROLE_GUEST = "Guest";
    public static final String ROLE_LIVEGUEST = "LiveGuest";

    public static final String HD_ROLE = "HD";
    public static final String SD_ROLE = "SD";
    public static final String LD_ROLE = "LD";
    public static final String HD_GUEST_ROLE = "HDGuest";
    public static final String SD_GUEST_ROLE = "SDGuest";
    public static final String LD_GUEST_ROLE = "LDGuest";

    // 直播业务id和appid，可在控制台的 直播管理中查看
    public static final int BIZID = 8525;
    public static final int APPID = 1253488539;     // 直播appid

    // 直播的API鉴权Key，可在控制台的 直播管理 => 接入管理 => 直播码接入 => 接入配置 中查看
    public static final String MIX_API_KEY = "45eeb9fc2e4e6f88b778e0bbd9de3737";
    // 固定地址
    public static final String MIX_SERVER = "http://fcgi.video.qcloud.com";

    public static final int MAX_SIZE = 50;

    /** 命令字 */
    public static final String CMD_KEY = "userAction";
    /** 命令参数 */
    public static final String CMD_PARAM = "actionParam";


    /** 无效消息 */
    public static final int ILVLIVE_CMD_NONE            = 0x700;         //1792
    /** 请求跨房连麦，C2C消息 */
    public static final int ILVLIVE_CMD_LINKROOM_REQ    = 0x708;        // 1800
    /** 同意跨房连麦，C2C消息 */
    public static final int ILVLIVE_CMD_LINKROOM_ACCEPT = 0x709;        // 1801
    /** 拒绝跨房连麦，C2C消息 */
    public static final int ILVLIVE_CMD_LINKROOM_REFUSE = 0x70A;        // 1802
    /** 跨房连麦者达到上限, C2C消息 */
    public static final int ILVLIVE_CMD_LINKROOM_LIMIT  = 0x70B;        // 1803
    /** 跨房连麦成功 */
    public static final int ILVLIVE_CMD_LINKROOM_SUCC   = 0x70C;        // 1804
    /** 取消跨房连麦 */
    public static final int ILVLIVE_CMD_UNLINKROOM      = 0x70D;        // 1805
}
