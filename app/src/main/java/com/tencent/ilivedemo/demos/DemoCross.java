package com.tencent.ilivedemo.demos;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
import com.tencent.av.sdk.AVVideoCtrl;
import com.tencent.ilivedemo.R;
import com.tencent.ilivedemo.model.Constants;
import com.tencent.ilivedemo.model.MessageObservable;
import com.tencent.ilivedemo.model.StatusObservable;
import com.tencent.ilivedemo.model.UserInfo;
import com.tencent.ilivedemo.uiutils.DemoFunc;
import com.tencent.ilivedemo.uiutils.DlgMgr;
import com.tencent.ilivedemo.view.DemoEditText;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveConstants;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.core.ILiveRoomOption;
import com.tencent.ilivesdk.data.ILiveMessage;
import com.tencent.ilivesdk.data.msg.ILiveTextMessage;
import com.tencent.ilivesdk.listener.ILiveMessageListener;
import com.tencent.ilivesdk.tools.quality.ILiveQualityData;
import com.tencent.ilivesdk.tools.quality.LiveInfo;
import com.tencent.ilivesdk.view.AVRootView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.tencent.ilivesdk.data.ILiveMessage.ILIVE_MSG_TYPE_CUSTOM;
import static com.tencent.ilivesdk.data.ILiveMessage.ILIVE_MSG_TYPE_TEXT;

/**
 * Created by xkazerzhang on 2017/5/24.
 */
public class DemoCross extends Activity implements View.OnClickListener, ILiveMessageListener, ILiveLoginManager.TILVBStatusListener{
    private final String TAG = "DemoCross";
    private DemoEditText etRoom, etDstRoom, etDstUser;
    private TextView tvMsg, tvCross;
    private ScrollView svScroll;
    private AVRootView arvRoot;

    private boolean isCameraOn = true;
    private boolean isMicOn = true;
    private boolean isFlashOn = false;
    private boolean isCross = false;

    private OkHttpClient okHttpClient;
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
        setContentView(R.layout.demo_cross);

        UserInfo.getInstance().getCache(getApplicationContext());

        arvRoot = (AVRootView)findViewById(R.id.arv_root);
        etRoom = (DemoEditText)findViewById(R.id.et_room);
        etDstRoom = (DemoEditText)findViewById(R.id.et_dst_room);
        etDstUser = (DemoEditText)findViewById(R.id.et_dst_user);
//        etRoom.setText(""+UserInfo.getInstance().getRoom());
        tvMsg = (TextView)findViewById(R.id.tv_msg);
        svScroll = (ScrollView)findViewById(R.id.sv_scroll);

        etDstRoom = (DemoEditText)findViewById(R.id.et_dst_room);
        tvCross = (TextView)findViewById(R.id.tv_cross);

        ILiveRoomManager.getInstance().initAvRootView(arvRoot);
        MessageObservable.getInstance().addObserver(this);
        StatusObservable.getInstance().addObserver(this);

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
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
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_create:
                createRoom();
                break;
            case R.id.tv_cross:
                corssRoom();
                break;
            case R.id.tv_cancel_cross:
                cancelCross();
                break;
            case R.id.iv_camera:
                isCameraOn = !isCameraOn;
                ILiveRoomManager.getInstance().enableCamera(ILiveRoomManager.getInstance().getCurCameraId(),
                        isCameraOn);
                ((ImageView)findViewById(R.id.iv_camera)).setImageResource(
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
                ((ImageView)findViewById(R.id.iv_mic)).setImageResource(
                        isMicOn ? R.mipmap.ic_mic_on : R.mipmap.ic_mic_off);
                break;
            case R.id.iv_return:
                finish();
                break;
            case R.id.iv_info:
                isInfoOn = !isInfoOn;
                ((ImageView)findViewById(R.id.iv_info)).setImageResource(isInfoOn ? R.mipmap.ic_info_on : R.mipmap.ic_info_off);
                findViewById(R.id.tv_status).setVisibility(isInfoOn ? View.VISIBLE : View.INVISIBLE);
                break;
        }
    }

    @Override
    public void onNewMessage(ILiveMessage message) {
        switch (message.getMsgType()){
            case ILIVE_MSG_TYPE_TEXT:
                ILiveTextMessage textMessage = (ILiveTextMessage)message;
                addMessage(textMessage.getSender(), DemoFunc.getLimitString(textMessage.getText(), Constants.MAX_SIZE));
                break;
            case ILIVE_MSG_TYPE_CUSTOM:
                break;
        }
    }

    @Override
    public void onForceOffline(int error, String message) {
        finish();
    }

    private Context getContenxt(){
        return this;
    }

    // 添加消息
    private void addMessage(String sender, String msg){
        strMsg += "["+sender+"]  "+msg+"\n";
        tvMsg.setText(strMsg);
        svScroll.fullScroll(View.FOCUS_DOWN);
    }

    // 加入房间
    private void createRoom(){
        int roomId = DemoFunc.getIntValue(etRoom.getText().toString(), -1);
        if (-1 == roomId){
            DlgMgr.showMsg(getContenxt(), getString(R.string.str_tip_num_error));
            return;
        }
        ILiveRoomOption option = new ILiveRoomOption()
                .controlRole(Constants.ROLE_MASTER)
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .autoFocus(true);
        ILiveRoomManager.getInstance().createRoom(roomId,
                option, new ILiveCallBack() {
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

    private void afterCreate(){
        UserInfo.getInstance().setRoom(ILiveRoomManager.getInstance().getRoomId());
        UserInfo.getInstance().writeToCache(this);
        etRoom.setEnabled(false);
        findViewById(R.id.tv_create).setVisibility(View.INVISIBLE);
        findViewById(R.id.ll_controller).setVisibility(View.VISIBLE);
        findViewById(R.id.iv_info).setVisibility(View.VISIBLE);
        mainHandler.postDelayed(infoRun, 500);
    }

    private void toggleFlash(){
        if (ILiveConstants.BACK_CAMERA != ILiveRoomManager.getInstance().getActiveCameraId()){
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

    private Context getContext(){
        return this;
    }

    private void corssRoom(){
        int dstRoom = DemoFunc.getIntValue(etDstRoom.getText().toString(), -1);
        if (-1 == dstRoom){
            DlgMgr.showMsg(getContenxt(), getString(R.string.str_tip_num_error));
            return;
        }
        String dstUser = etDstUser.getText().toString();
        requestSign(dstRoom, dstUser);
    }

    private void cancelCross(){
        if (ILiveRoomManager.getInstance().isEnterRoom()) {
            ILiveRoomManager.getInstance().unlinkRoom(new ILiveCallBack() {
                @Override
                public void onSuccess(Object data) {
                }

                @Override
                public void onError(String module, int errCode, String errMsg) {
                    DlgMgr.showMsg(getContext(), "linkRoom->Failed: " + module + "|" + errCode + "|" + errMsg);
                }
            });
        }
    }

    private void requestSign(final int dstRoomId, final String dstUser){
        String postBody = "";
        try {
            JSONObject jsonReq = new JSONObject();
            jsonReq.put("mygroup", ILiveRoomManager.getInstance().getRoomId());
            jsonReq.put("myid", ILiveLoginManager.getInstance().getMyUserId());
            jsonReq.put("remotegroup", dstRoomId);
            jsonReq.put("remotehost", dstUser);
            postBody = jsonReq.toString();
        }catch (JSONException e){
        }
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                postBody);
        Request request = new Request.Builder()
                .url("https://sxb.qcloud.com/easy/encode")
                .post(requestBody)
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        DlgMgr.showMsg(getContext(), "Request fail: "+e.toString());
                    }
                }, 0);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String data = response.body().string();
                ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        ILiveRoomManager.getInstance().linkRoom(dstRoomId, dstUser, data, new ILiveCallBack() {
                            @Override
                            public void onSuccess(Object data) {
                            }

                            @Override
                            public void onError(String module, int errCode, String errMsg) {
                                DlgMgr.showMsg(getContext(), "linkRoom->Failed: "+module+"|"+errCode+"|"+errMsg);
                            }
                        });

                    }
                }, 0);
            }
        });
    }
}
