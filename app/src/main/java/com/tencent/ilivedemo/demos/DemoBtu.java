package com.tencent.ilivedemo.demos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.ilivedemo.R;
import com.tencent.ilivedemo.model.Constants;
import com.tencent.ilivedemo.model.MessageObservable;
import com.tencent.ilivedemo.model.StatusObservable;
import com.tencent.ilivedemo.model.UserInfo;
import com.tencent.ilivedemo.ui.RadioGroupDialog;
import com.tencent.ilivedemo.uiutils.DemoFunc;
import com.tencent.ilivedemo.uiutils.DlgMgr;
import com.tencent.ilivedemo.view.DemoEditText;
import com.tencent.ilivefilter.TILFilter;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.core.ILiveRoomOption;
import com.tencent.ilivesdk.data.ILiveMessage;
import com.tencent.ilivesdk.data.msg.ILiveCustomMessage;
import com.tencent.ilivesdk.data.msg.ILiveTextMessage;
import com.tencent.ilivesdk.listener.ILiveMessageListener;
import com.tencent.ilivesdk.tools.quality.ILiveQualityData;
import com.tencent.ilivesdk.tools.quality.LiveInfo;
import com.tencent.ilivesdk.view.AVRootView;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Map;

import static com.tencent.ilivesdk.data.ILiveMessage.*;

/**
 * Created by xkazerzhang on 2017/5/24.
 */
public class DemoBtu extends Activity implements View.OnClickListener, ILiveMessageListener,
        SeekBar.OnSeekBarChangeListener, ILiveLoginManager.TILVBStatusListener {
    private final String TAG = "DemoBtu";
    private DemoEditText etRoom;
    private TextView tvMsg;
    private ScrollView svScroll;
    private AVRootView arvRoot;
    private SeekBar sbBeauty, sbWhite;
    private LinearLayout llBeautyControl;

    private boolean bAvsdkBeauty = true;
    private TILFilter mUDFilter;    // 美颜处理器

    private boolean isCameraOn = true;
    private boolean isMicOn = true;
    private boolean isFlashOn = false;

    private boolean isInfoOn = true;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private String strMsg = "";

    private Runnable infoRun = new Runnable() {
        @Override
        public void run() {
            ILiveQualityData qualityData = ILiveRoomManager.getInstance().getQualityData();
            if (null != qualityData){
                String info = "上行速率:\t"+qualityData.getSendKbps()+"kbps\t"
                        +"上行丢包率:\t"+qualityData.getSendLossRate()/100+"%\n\n"
                        +"下行速率:\t"+qualityData.getRecvKbps()+"kbps\t"
                        +"下行丢包率:\t"+qualityData.getRecvLossRate()/100+"%\n\n"
                        +"应用CPU:\t"+qualityData.getAppCPURate()+"\t"
                        +"系统CPU:\t"+qualityData.getSysCPURate()+"\n\n";
                for (Map.Entry<String, LiveInfo> entry: qualityData.getLives().entrySet()){
                    info += "\t"+entry.getKey()+"-"+entry.getValue().getWidth()+"*"+entry.getValue().getHeight()+"\n\n";
                }
                ((TextView)findViewById(R.id.tv_status)).setText(info);
            }
            if (ILiveRoomManager.getInstance().isEnterRoom()) {
                mainHandler.postDelayed(infoRun, 2000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_btu);

        UserInfo.getInstance().getCache(getApplicationContext());

        arvRoot = (AVRootView) findViewById(R.id.arv_root);
        etRoom = (DemoEditText) findViewById(R.id.et_room);
//        etRoom.setText("" + UserInfo.getInstance().getRoom());
        tvMsg = (TextView) findViewById(R.id.tv_msg);
        svScroll = (ScrollView)findViewById(R.id.sv_scroll);
        llBeautyControl = (LinearLayout) findViewById(R.id.ll_btu_control);

        sbBeauty = (SeekBar) findViewById(R.id.sb_beauty);
        sbWhite = (SeekBar)findViewById(R.id.sb_white);
        sbBeauty.setOnSeekBarChangeListener(this);
        sbWhite.setOnSeekBarChangeListener(this);

        ILiveRoomManager.getInstance().initAvRootView(arvRoot);
        MessageObservable.getInstance().addObserver(this);
        StatusObservable.getInstance().addObserver(this);

        initRoleDialog();

        initILiveBeauty();
    }

    @Override
    protected void onPause() {
        super.onPause();
        ILiveRoomManager.getInstance().onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ILiveRoomManager.getInstance().onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MessageObservable.getInstance().deleteObserver(this);
        StatusObservable.getInstance().deleteObserver(this);
        ILiveRoomManager.getInstance().onDestory();

        if (null != mUDFilter) {
            mUDFilter.setFilter(-1);
            mUDFilter.destroyFilter();
            mUDFilter = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_create:
                createRoom();
                break;
            case R.id.iv_camera:
                isCameraOn = !isCameraOn;
                ILiveRoomManager.getInstance().enableCamera(ILiveRoomManager.getInstance().getCurCameraId(),
                        isCameraOn);
                ((ImageView) findViewById(R.id.iv_camera)).setImageResource(
                        isCameraOn ? R.mipmap.ic_camera_on : R.mipmap.ic_camera_off);
                break;
            case R.id.iv_switch:
                Log.v(TAG, "switch->cur: "+ILiveRoomManager.getInstance().getActiveCameraId()+"/"+ILiveRoomManager.getInstance().getCurCameraId());
                if (ILiveConstants.NONE_CAMERA != ILiveRoomManager.getInstance().getActiveCameraId()) {
                    ILiveRoomManager.getInstance().switchCamera(1 - ILiveRoomManager.getInstance().getActiveCameraId());
                }else{
                    ILiveRoomManager.getInstance().switchCamera(ILiveConstants.FRONT_CAMERA);
                }
                break;
            case R.id.iv_flash:
                toggleFlash();
                break;
            case R.id.iv_mic:
                isMicOn = !isMicOn;
                ILiveRoomManager.getInstance().enableMic(isMicOn);
                ((ImageView) findViewById(R.id.iv_mic)).setImageResource(
                        isMicOn ? R.mipmap.ic_mic_on : R.mipmap.ic_mic_off);
                break;
            case R.id.iv_beauty:
                if (View.VISIBLE == llBeautyControl.getVisibility())
                    llBeautyControl.setVisibility(View.INVISIBLE);
                else
                    llBeautyControl.setVisibility(View.VISIBLE);
                break;
            case R.id.iv_switch_btu:
                showBtuTypeDlg();
                break;
            case R.id.iv_return:
                finish();
                break;
            case R.id.iv_info:
                isInfoOn = !isInfoOn;
                ((ImageView)findViewById(R.id.iv_info)).setImageResource(isInfoOn ? R.mipmap.ic_info_on : R.mipmap.ic_info_off);
                findViewById(R.id.tv_status).setVisibility(isInfoOn ? View.VISIBLE : View.INVISIBLE);
                break;
            case R.id.iv_role:
                if (null != roleDialog)
                    roleDialog.show();
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (null == ILiveSDK.getInstance().getAvVideoCtrl()){
            return;
        }
        switch (seekBar.getId()) {
            case R.id.sb_beauty:
                if (bAvsdkBeauty)
                    ILiveRoomManager.getInstance().enableBeauty(progress);
                else
                    mUDFilter.setBeauty(progress);
                break;
            case R.id.sb_white:
                if (bAvsdkBeauty)
                    ILiveRoomManager.getInstance().enableWhite(progress);
                else
                    mUDFilter.setWhite(progress);
                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onNewMessage(ILiveMessage message) {
        switch (message.getMsgType()){
            case ILIVE_MSG_TYPE_TEXT:
                ILiveTextMessage textMessage = (ILiveTextMessage)message;
                addMessage(textMessage.getSender(), DemoFunc.getLimitString(textMessage.getText(), Constants.MAX_SIZE));
                break;
            case ILIVE_MSG_TYPE_CUSTOM:
                ILiveCustomMessage customMessage = (ILiveCustomMessage)message;
                String data = new String(customMessage.getData());
                try {
                    JSONTokener jsonParser = new JSONTokener(data);
                    JSONObject json = (JSONObject) jsonParser.nextValue();
                    int action = json.getInt(Constants.CMD_KEY);
                    if (action == Constants.ILVLIVE_CMD_LINKROOM_REQ)
                        linkRoomReq(customMessage.getSender());
                }catch (Exception e){
                    // 处理异常
                }
                break;
        }
    }

    @Override
    public void onForceOffline(int error, String message) {
        finish();
    }

    private Context getContenxt() {
        return this;
    }

    // 角色对话框
    private RadioGroupDialog roleDialog;
    private void initRoleDialog() {
        final String[] roles = new String[]{ "高清(960*540,25fps)","标清(640*368,20fps)", "流畅(640*368,15fps)"};
        final String[] values = new String[]{Constants.HD_ROLE, Constants.SD_ROLE, Constants.LD_ROLE};
        roleDialog = new RadioGroupDialog(this, roles);
        roleDialog.setTitle(R.string.str_dt_change_role);
        roleDialog.setSelected(0);

        roleDialog.setOnItemClickListener(new RadioGroupDialog.onItemClickListener() {
            @Override
            public void onItemClick(int position) {
                ILiveRoomManager.getInstance().changeRole(values[position], new ILiveCallBack() {
                    @Override
                    public void onSuccess(Object data) {}

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                        DlgMgr.showMsg(getContenxt(), "change failed:"+module+"|"+errCode+"|"+errMsg);
                    }
                });
            }
        });
    }

    // 添加消息
    private void addMessage(String sender, String msg) {
        strMsg += "[" + sender + "]  " + msg + "\n";
        tvMsg.setText(strMsg);
        svScroll.fullScroll(View.FOCUS_DOWN);
    }

    private void joinRoom(){
        final int roomId = DemoFunc.getIntValue(etRoom.getText().toString(), -1);
        if (-1 == roomId){
            DlgMgr.showMsg(getContenxt(), getString(R.string.str_tip_num_error));
            return;
        }

        ILiveRoomOption option = new ILiveRoomOption()
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .controlRole(Constants.ROLE_LIVEGUEST)
                .autoFocus(true);
        ILiveRoomManager.getInstance().joinRoom(roomId, option, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                afterCreate();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                DlgMgr.showMsg(getContenxt(), "create failed:"+module+"|"+errCode+"|"+errMsg);
            }
        });
    }

    private void showChoiceDlg(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("房间已存在，是否加入房间？")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        joinRoom();
                        dialogInterface.dismiss();
                    }
                });
        DlgMgr.showAlertDlg(this, builder);
    }

    // 加入房间
    private void createRoom() {
        int roomId = DemoFunc.getIntValue(etRoom.getText().toString(), -1);
        if (-1 == roomId){
            DlgMgr.showMsg(getContenxt(), getString(R.string.str_tip_num_error));
            return;
        }
        ILiveRoomOption option = new ILiveRoomOption()
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .controlRole(Constants.ROLE_MASTER)
                .autoFocus(true);
        ILiveRoomManager.getInstance().createRoom(roomId,
                option, new ILiveCallBack() {
                    @Override
                    public void onSuccess(Object data) {
                        afterCreate();
                    }

                    @Override
                    public void onError(String module, int errCode, String errMsg) {
                        if (module.equals(ILiveConstants.Module_IMSDK) && 10021 == errCode){
                            // 被占用，改加入
                            showChoiceDlg();
                        }else {
                            DlgMgr.showMsg(getContenxt(), "create failed:" + module + "|" + errCode + "|" + errMsg);
                        }
                    }
                });
    }

    private void afterCreate() {
        UserInfo.getInstance().setRoom(ILiveRoomManager.getInstance().getRoomId());
        UserInfo.getInstance().writeToCache(this);
        etRoom.setEnabled(false);
        findViewById(R.id.tv_create).setVisibility(View.INVISIBLE);
        findViewById(R.id.ll_controller).setVisibility(View.VISIBLE);
        findViewById(R.id.iv_info).setVisibility(View.VISIBLE);
        findViewById(R.id.iv_role).setVisibility(View.VISIBLE);
        mainHandler.postDelayed(infoRun, 500);
        Log.v(TAG, "afterCreate->show control");
    }

    private void toggleFlash() {
        if (ILiveConstants.BACK_CAMERA != ILiveRoomManager.getInstance().getActiveCameraId()) {
            return;
        }
        AVVideoCtrl videoCtrl = ILiveSDK.getInstance().getAvVideoCtrl();
        if (null == videoCtrl) {
            return;
        }

        final Object cam = videoCtrl.getCamera();
        if ((cam == null) || (!(cam instanceof Camera))) {
            return;
        }
        final Camera.Parameters camParam = ((Camera) cam).getParameters();
        if (null == camParam) {
            return;
        }

        Object camHandler = videoCtrl.getCameraHandler();
        if ((camHandler == null) || (!(camHandler instanceof Handler))) {
            return;
        }

        //对摄像头的操作放在摄像头线程
        if (isFlashOn == false) {
            ((Handler) camHandler).post(new Runnable() {
                public void run() {
                    try {
                        camParam.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        ((Camera) cam).setParameters(camParam);
                        isFlashOn = true;
                    } catch (RuntimeException e) {
                        Log.d(TAG, "setParameters->RuntimeException");
                    }
                }
            });
        } else {
            ((Handler) camHandler).post(new Runnable() {
                public void run() {
                    try {
                        camParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                        ((Camera) cam).setParameters(camParam);
                        isFlashOn = false;
                    } catch (RuntimeException e) {
                        Log.d(TAG, "setParameters->RuntimeException");
                    }
                }
            });
        }
    }

    // 生成内部信令
    private String jsonToString(int cmd, String param) {
        JSONObject inviteCmd = new JSONObject();
        try {
            inviteCmd.put(Constants.CMD_KEY, cmd);
            inviteCmd.put(Constants.CMD_PARAM, param);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return inviteCmd.toString();
    }

    // 拒绝跨房连麦
    private void refuseLink(String id){
        String command = jsonToString(Constants.ILVLIVE_CMD_LINKROOM_REFUSE, "");
        ILiveCustomMessage customMessage = new ILiveCustomMessage(command.getBytes(), "");
        ILiveRoomManager.getInstance().sendC2COnlineMessage(id, customMessage, null);
    }

    // 同意跨房连麦
    private void acceptLink(String id){
        String command = jsonToString(Constants.ILVLIVE_CMD_LINKROOM_ACCEPT, "");
        ILiveCustomMessage customMessage = new ILiveCustomMessage(command.getBytes(), "");
        ILiveRoomManager.getInstance().sendC2COnlineMessage(id, customMessage, null);
    }

    private void linkRoomReq(final String id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.live_title_link);
        builder.setMessage("[" + id + "]" + getString(R.string.link_req_tips));
        builder.setNegativeButton(R.string.str_btn_refuse, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                refuseLink(id);
            }
        });
        builder.setPositiveButton(R.string.str_btn_agree, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                acceptLink(id);
            }
        });
        DlgMgr.showAlertDlg(this, builder);
    }

    private void initILiveBeauty(){
        if (null == mUDFilter){
            Log.d(TAG, "FILTER->created");
            mUDFilter = new TILFilter(this);
            mUDFilter.setFilter(1);
            mUDFilter.setBeauty(0);
            mUDFilter.setWhite(0);
        }
    }

    private void changeBeauty(){
        if (null == ILiveSDK.getInstance().getAvVideoCtrl()){
            return;
        }
        sbBeauty.setProgress(0);
        sbWhite.setProgress(0);
        mUDFilter.setBeauty(0);
        mUDFilter.setWhite(0);
        ILiveRoomManager.getInstance().enableBeauty(0);
        ILiveRoomManager.getInstance().enableWhite(0);
        if (!bAvsdkBeauty) {
            ILiveSDK.getInstance().getAvVideoCtrl().setLocalVideoPreProcessCallback(new AVVideoCtrl.LocalVideoPreProcessCallback() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void onFrameReceive(AVVideoCtrl.VideoFrame var1) {
                    mUDFilter.processData(var1.data, var1.dataLen, var1.width, var1.height, var1.srcType);
                }
            });
        }else{
            ILiveSDK.getInstance().getAvVideoCtrl().setLocalVideoPreProcessCallback(null);
        }
    }

    private void showBtuTypeDlg(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContenxt())
                .setTitle(R.string.str_title_btu_type)
                .setSingleChoiceItems(R.array.str_btu_type, bAvsdkBeauty?0:1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (bAvsdkBeauty != (0==i)){
                            bAvsdkBeauty = (0==i);
                            changeBeauty();
                            Log.v(TAG, "showBtuTypeDlg->Change beauty: "+bAvsdkBeauty);
                        }
                    }
                });
        DlgMgr.showAlertDlg(getContenxt(), builder);
    }
}
