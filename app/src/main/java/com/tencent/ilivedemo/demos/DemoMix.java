package com.tencent.ilivedemo.demos;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.TIMMessage;
import com.tencent.TIMUserProfile;
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
import com.tencent.ilivesdk.ILiveFunc;
import com.tencent.ilivesdk.ILiveMemStatusLisenter;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLoginManager;
import com.tencent.ilivesdk.core.ILiveRoomManager;
import com.tencent.ilivesdk.core.ILiveRoomOption;
import com.tencent.ilivesdk.data.ILiveMessage;
import com.tencent.ilivesdk.data.msg.ILiveTextMessage;
import com.tencent.ilivesdk.listener.ILiveMessageListener;
import com.tencent.ilivesdk.view.AVRootView;
import com.tencent.ilivesdk.view.AVVideoView;
import com.tencent.rtmp.ITXLivePlayListener;
import com.tencent.rtmp.TXLiveConstants;
import com.tencent.rtmp.TXLivePlayConfig;
import com.tencent.rtmp.TXLivePlayer;
import com.tencent.rtmp.ui.TXCloudVideoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.tencent.ilivesdk.data.ILiveMessage.ILIVE_MSG_TYPE_TEXT;

/**
 * Created by xkazerzhang on 2017/7/11.
 */
public class DemoMix extends Activity implements View.OnClickListener, ILiveMessageListener,
        ILiveMemStatusLisenter, ITXLivePlayListener, ILiveLoginManager.TILVBStatusListener{
    private final String TAG = "DemoMix";
    private static final int MSG_RESUME_PLAY = 0x101;
    private AVRootView avRootView;
    private TXCloudVideoView txvvPlayerView;

    private DemoEditText etRoom;
    private TextView tvMsg, tvStatus, tvMixAddr;

    private String strMsg = "";
    private OkHttpClient mOkHttpClient;
    private int mPlayType = TXLivePlayer.PLAY_TYPE_LIVE_RTMP;
    private TXLivePlayer mTxlpPlayer;
    private ArrayList<String> renderList = new ArrayList<>();
    private Button btnArr[] = new Button[4];
    private int curTemplateId = -1;
    private int iMaxDelay = 5000;

    private boolean bFirstPlay = true;

    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_RESUME_PLAY:
                    if (null != mTxlpPlayer) {
                        if (!bFirstPlay && TXLivePlayer.PLAY_TYPE_LIVE_FLV == mPlayType) {
                            mTxlpPlayer.resume();
                        } else {
                            mTxlpPlayer.setPlayListener(DemoMix.this);
                            mTxlpPlayer.startPlay(getMixPlayUrl(), mPlayType);
                            bFirstPlay = false;
                        }
                    }
                    break;
            }
            return false;
        }
    });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_mix);

        avRootView = (AVRootView)findViewById(R.id.av_root_view);
        txvvPlayerView = (TXCloudVideoView)findViewById(R.id.txcv_view);
        etRoom = (DemoEditText)findViewById(R.id.et_room);
        tvMsg = (TextView)findViewById(R.id.tv_msg);
        tvStatus = (TextView)findViewById(R.id.tv_play_status);
        tvMixAddr = (TextView)findViewById(R.id.tv_mix_addr);

        btnArr[0] = (Button)findViewById(R.id.btn_template1);
        btnArr[1] = (Button)findViewById(R.id.btn_template2);
        btnArr[2] = (Button)findViewById(R.id.btn_template3);
        btnArr[3] = (Button)findViewById(R.id.btn_template4);

        ILiveRoomManager.getInstance().initAvRootView(avRootView);
        MessageObservable.getInstance().addObserver(this);
        StatusObservable.getInstance().addObserver(this);

        avRootView.setSubCreatedListener(new AVRootView.onSubViewCreatedListener() {
            @Override
            public void onSubViewCreated() {
                int subWidth = (avRootView.getWidth()-50)/4;
                int subHeight = avRootView.getHeight()-20;
                int curLeft = 0;
                for (int i=0; i<4; i++){
                    AVVideoView videoView = avRootView.getViewByIndex(i);
                    curLeft += 10;
                    videoView.setPosTop(10);
                    videoView.setPosLeft(curLeft);
                    videoView.setPosWidth(subWidth);
                    videoView.setPosHeight(subHeight);
                    videoView.autoLayout();
                    curLeft += subWidth;
                }
            }
        });

        mOkHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        mTxlpPlayer = new TXLivePlayer(this);

        mTxlpPlayer.setPlayerView(txvvPlayerView);
        mTxlpPlayer.setConfig(new TXLivePlayConfig());
        mTxlpPlayer.setRenderMode(TXLiveConstants.RENDER_MODE_ADJUST_RESOLUTION);
        mTxlpPlayer.setRenderRotation(TXLiveConstants.RENDER_ROTATION_LANDSCAPE);

        updateTemplateBtns(renderList.size());
    }

    @Override
    protected void onPause() {
        super.onPause();
        ILiveRoomManager.getInstance().onPause();
        txvvPlayerView.onPause();
        if (null != mTxlpPlayer) {
            if (TXLivePlayer.PLAY_TYPE_LIVE_FLV == mPlayType) {
                mTxlpPlayer.pause();
            } else {
                mTxlpPlayer.setPlayListener(null);
                mTxlpPlayer.stopPlay(true);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ILiveRoomManager.getInstance().onResume();
        txvvPlayerView.onResume();
        if (ILiveRoomManager.getInstance().isEnterRoom())
            mHandler.sendEmptyMessage(MSG_RESUME_PLAY);
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
            case R.id.iv_return:
                finish();
                break;
            case R.id.btn_copy:
                ClipboardManager cmb = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("text", tvMixAddr.getText().toString());
                cmb.setPrimaryClip(clipData);
                DlgMgr.showMsg(this, "Copy Success");
                break;
            case R.id.btn_template1:
            case R.id.btn_template2:
            case R.id.btn_template3:
            case R.id.btn_template4:
                curTemplateId = (Integer) view.getTag();
                cancelMixStream(true, 0);
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
        }
    }

    @Override
    public boolean onEndpointsUpdateInfo(int eventid, String[] updateList) {
        if (null == updateList || 0 == updateList.length) {
            return false;
        }
        switch (eventid) {
            case ILiveConstants.TYPE_MEMBER_CHANGE_HAS_CAMERA_VIDEO:
                for (String id : updateList) {
                    String stream = UserInfo.getInstance().getRoom() + "_" + id + "_main";
                    if (!renderList.contains(stream))
                        renderList.add(stream);
                }
                cancelMixStream(true, iMaxDelay);
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_HAS_SCREEN_VIDEO:
                for (String id : updateList) {
                    String stream = UserInfo.getInstance().getRoom() + "_" + id + "_aux";
                    if (!renderList.contains(stream))
                        renderList.add(stream);
                }
                cancelMixStream(true, iMaxDelay);
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_NO_CAMERA_VIDEO:
                for (String id : updateList) {
                    String stream = UserInfo.getInstance().getRoom() + "_" + id + "_main";
                    renderList.remove(stream);
                }
                cancelMixStream(true, iMaxDelay);
                break;
            case ILiveConstants.TYPE_MEMBER_CHANGE_NO_SCREEN_VIDEO:
                for (String id : updateList) {
                    String stream = UserInfo.getInstance().getRoom() + "_" + id + "_aux";
                    renderList.remove(stream);
                }
                cancelMixStream(true, iMaxDelay);
                break;
        }
        return false;
    }

    @Override
    public void onPlayEvent(int event, Bundle param) {
        if (TXLiveConstants.PLAY_EVT_PLAY_PROGRESS == event) {       // 忽略process事件
            return;
        }
        Log.v(TAG, "onPlayEvent->event:" + event + "|" + param.getString(TXLiveConstants.EVT_DESCRIPTION));
        tvStatus.setText(event + "|" + param.getString(TXLiveConstants.EVT_DESCRIPTION));
        //错误还是要明确的报一下
        if (event < 0) {
            Toast.makeText(getApplicationContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION), Toast.LENGTH_SHORT).show();
        }

        if (event == TXLiveConstants.PLAY_ERR_NET_DISCONNECT) {
            DlgMgr.showMsg(getContext(), param.getString(TXLiveConstants.EVT_DESCRIPTION));
        }
    }

    @Override
    public void onNetStatus(Bundle bundle) {

    }

    @Override
    public void onForceOffline(int error, String message) {
        finish();
    }

    private Context getContenxt(){
        return DemoMix.this;
    }

    // 添加消息
    private void addMessage(String sender, String msg){
        strMsg += "["+sender+"]  "+msg+"\n";
        tvMsg.setText(strMsg);
    }

    private void joinRoom(){
        final int roomId = DemoFunc.getIntValue(etRoom.getText().toString(), -1);
        if (-1 == roomId){
            DlgMgr.showMsg(getContenxt(), getString(R.string.str_tip_num_error));
            return;
        }
        ILiveRoomOption option = new ILiveRoomOption("")
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
    private void createRoom(){
        int roomId = DemoFunc.getIntValue(etRoom.getText().toString(), -1);
        if (-1 == roomId){
            DlgMgr.showMsg(getContenxt(), getString(R.string.str_tip_num_error));
            return;
        }
        ILiveRoomOption option = new ILiveRoomOption(ILiveLoginManager.getInstance().getMyUserId())
                .setRoomMemberStatusLisenter(this)
                .videoMode(ILiveConstants.VIDEOMODE_NORMAL)
                .controlRole(Constants.ROLE_MASTER)
                .autoFocus(true);
        UserInfo.getInstance().setRoom(roomId);
        ILiveRoomManager.getInstance().createRoom(UserInfo.getInstance().getRoom(),
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

    private Context getContext(){
        return DemoMix.this;
    }

    private void afterCreate(){
        UserInfo.getInstance().setRoom(ILiveRoomManager.getInstance().getRoomId());
        UserInfo.getInstance().writeToCache(this);
        etRoom.setEnabled(false);
        findViewById(R.id.tv_create).setVisibility(View.INVISIBLE);
        cancelMixStream(false, 0);
        //addMessage("Mix", "View : "+txvvPlayerView.getWidth()+"*"+txvvPlayerView.getHeight());
    }

    private JSONObject getStreamJsonInfo(String streamId, int layer) throws JSONException {
        JSONObject jsonStream = new JSONObject();
        JSONObject jsonParams = new JSONObject();

        jsonParams.put("image_layer", layer);
        jsonStream.put("input_stream_id", streamId);
        jsonStream.put("layout_params", jsonParams);

        return jsonStream;
    }

    private int getMixTemplate() {
        if (-1 == curTemplateId) {
            switch (renderList.size()) {
                case 2:
                    return 10;
                case 3:
                    return 310;
                case 4:
                    return 410;
                case 1:
                default:
                    return 10;
            }
        }else{
            return curTemplateId;
        }
    }

    private String getMixOutputStream(){
        return ILiveLoginManager.getInstance().getMyUserId() + "_0";
    }

    private String getMixPlayUrl(){
        return "rtmp://"+Constants.BIZID+".liveplay.myqcloud.com/live/"+getMixOutputStream();
    }

    private String getMixPostData() {
        JSONObject jsonMix = new JSONObject();
        try {
            jsonMix.put("timestamp", ILiveFunc.getCurrentSec());
            jsonMix.put("eventid", ILiveFunc.getCurrentSec());
            JSONObject jsonInterface = new JSONObject();
            jsonInterface.put("interfaceName", "Mix_StreamV2");
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("app_id", Constants.APPID);
            jsonParam.put("interface", "mix_streamv2.start_mix_stream_advanced");
            jsonParam.put("mix_stream_session_id", getMixOutputStream());
            int templateId = getMixTemplate();
            Log.v(TAG, "getMixPostData->mix template id:"+templateId);
            jsonParam.put("mix_stream_template_id", templateId);
            jsonParam.put("output_stream_id", getMixOutputStream());     // 输出流
            jsonParam.put("output_stream_type", 1);         // 新流
            JSONArray jsonList = new JSONArray();
            for (int i = 0; i < renderList.size(); i++) {
                //直播码=BIZID_MD5(房间号_用户名_数据类型)
                String stream = renderList.get(i);
                String streamId = Constants.BIZID + "_" + ILiveFunc.getMD5(stream, true);
                Log.v(TAG, "streamId: " + streamId + "/" + stream);
                jsonList.put(i, getStreamJsonInfo(streamId, i + 1));
            }
            jsonParam.put("input_stream_list", jsonList);
            jsonInterface.put("para", jsonParam);
            jsonMix.put("interface", jsonInterface);

        } catch (JSONException e) {
            Log.e(TAG, "mixStream->generate json failed: " + e.toString());
        }

        return jsonMix.toString();
    }

    private String getCancelMixPostData() {
        JSONObject jsonMix = new JSONObject();
        try {
            jsonMix.put("timestamp", ILiveFunc.getCurrentSec());
            jsonMix.put("eventid", ILiveFunc.getCurrentSec());
            JSONObject jsonInterface = new JSONObject();
            jsonInterface.put("interfaceName", "Mix_StreamV2");
            JSONObject jsonParam = new JSONObject();
            jsonParam.put("app_id", Constants.APPID);
            jsonParam.put("interface", "mix_streamv2.start_mix_stream_advanced");
            jsonParam.put("mix_stream_session_id", getMixOutputStream());
            jsonParam.put("output_stream_id", getMixOutputStream());     // 输出流
            JSONArray jsonList = new JSONArray();
            String stream = UserInfo.getInstance().getRoom() + "_" + ILiveLoginManager.getInstance().getMyUserId() + "_main";
            String streamId = Constants.BIZID + "_" + ILiveFunc.getMD5(stream, true);
            Log.v(TAG, "streamId: " + streamId + "/" + stream);
            jsonList.put(0, getStreamJsonInfo(getMixOutputStream(), 1));
            jsonParam.put("input_stream_list", jsonList);
            jsonInterface.put("para", jsonParam);
            jsonMix.put("interface", jsonInterface);
        } catch (JSONException e) {
            Log.e(TAG, "getCancelMixPostData->generate json failed: " + e.toString());
        }

        return jsonMix.toString();
    }

    private int setupButton(Integer[] templates){
        int size = templates.length<=btnArr.length ? templates.length : btnArr.length;
        for (int i=0; i<size; i++){
            btnArr[i].setText(getContext().getString(R.string.str_template, templates[i]));
            btnArr[i].setTag(templates[i]);
        }
        return size;
    }

    private void updateTemplateBtns(int count){
        int size = 0;
        switch (count){
            case 2:
                size = setupButton(new Integer[]{10, 20, 30, 40});
                break;
            case 3:
                size = setupButton(new Integer[]{310, 390, 391});
                break;
            case 4:
                size = setupButton(new Integer[]{410});
                break;
            default:
                size = 0;
                break;
        }
        for (int i=0; i<btnArr.length; i++){
            btnArr[i].setVisibility(i>=size ? View.INVISIBLE : View.VISIBLE);
        }
    }

    private void sendMixRequest(){
        if (renderList.size() < 2){
            return;
        }else{
            if (bFirstPlay)
                mHandler.sendEmptyMessage(MSG_RESUME_PLAY);
            updateTemplateBtns(renderList.size());
        }
        long uTime = ILiveFunc.getCurrentSec() + 300;     // 有效期为300秒
        String sign = ILiveFunc.getMD5(Constants.MIX_API_KEY + uTime, true);
        Log.e(TAG, "mixStream->mix key+t:" + Constants.MIX_API_KEY + uTime + ", " + sign);
        String url = Constants.MIX_SERVER
                + "/common_access?interface=mix_streamv2.start_mix_stream_advanced&t="+uTime+"&sign="
                + sign + "&appid=" + Constants.APPID;
        String postData = getMixPostData();

        Log.d(TAG, "mixStream->url: " + url);
        Log.d(TAG, "mixStream->post: " + postData);
        final RequestBody reqBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                , postData);
        Request request = new Request.Builder()
                .url(url)
                .post(reqBody)
                .build();

        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                Log.d(TAG, "mixStream->onFailure: " + e.toString());
                ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        DlgMgr.showMsg(getContext(), "混流失败: "+e.toString());
                    }
                }, 0);

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String rsp = response.body().string();
                Log.d(TAG, "mixStream->onResponse: " + rsp);
                ILiveSDK.getInstance().runOnMainThread(new Runnable() {
                    @Override
                    public void run() {
                        int ret = -1;
                        String errMsg = "Parse Json Response failed";
                        try{
                            JSONObject jsonRsp = new JSONObject(rsp);
                            ret = jsonRsp.getInt("code");
                            errMsg = jsonRsp.optString("message");
                        }catch (JSONException e){
                        }

                        if (0 != ret && !isFinishing()){
                            DlgMgr.showMsg(getContext(), "混流失败: "+ret+"|"+errMsg);
                        }else{
                            tvMixAddr.setText(getMixPlayUrl());
                        }
                    }
                }, 0);
            }
        });
    }

    // 取消混流
    private void cancelMixStream(final boolean bMix, final int delay){
        long uTime = ILiveFunc.getCurrentSec() + 300;     // 有效期为300秒
        String sign = ILiveFunc.getMD5(Constants.MIX_API_KEY + uTime, true);
        Log.e(TAG, "cancelMixStream->mix key+t:" + Constants.MIX_API_KEY + uTime + ", " + sign);
        String url = Constants.MIX_SERVER
                + "/common_access?interface=mix_streamv2.start_mix_stream_advanced&t="+uTime+"&sign="
                + sign + "&appid=" + Constants.APPID;
        String postData = getCancelMixPostData();

        Log.d(TAG, "cancelMixStream->url: " + url);
        Log.d(TAG, "cancelMixStream->post: " + postData);
        final RequestBody reqBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8")
                , postData);
        Request request = new Request.Builder()
                .url(url)
                .post(reqBody)
                .build();
        mOkHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (bMix)
                    mixStream(delay);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (bMix)
                    mixStream(delay);
            }
        });
    }

    // 混流
    private void mixStream(int delay) {
        ILiveSDK.getInstance().runOnMainThread(new Runnable() {
            @Override
            public void run() {
                sendMixRequest();
            }
        }, delay);
    }
}
