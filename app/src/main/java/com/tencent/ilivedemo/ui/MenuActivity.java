package com.tencent.ilivedemo.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.tencent.ilivedemo.R;
import com.tencent.ilivedemo.demos.DemoBtu;
import com.tencent.ilivedemo.demos.DemoCross;
import com.tencent.ilivedemo.demos.DemoGuest;
import com.tencent.ilivedemo.demos.DemoHost;
import com.tencent.ilivedemo.demos.DemoLiveGuest;
import com.tencent.ilivedemo.demos.DemoMix;
import com.tencent.ilivedemo.demos.DemoRender;
import com.tencent.ilivedemo.demos.DemoReplay;
import com.tencent.ilivedemo.demos.DemoReplayList;
import com.tencent.ilivedemo.model.StatusObservable;
import com.tencent.ilivedemo.uiutils.DemoFunc;
import com.tencent.ilivedemo.uiutils.DlgMgr;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.ILiveSDK;
import com.tencent.ilivesdk.core.ILiveLoginManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 示例菜单
 */
public class MenuActivity extends Activity implements View.OnClickListener, ILiveLoginManager.TILVBStatusListener{
    private final static int REQ_PERMISSION_CODE = 0x100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mainmenu);
        StatusObservable.getInstance().addObserver(this);

        checkPermission();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StatusObservable.getInstance().deleteObserver(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.lmv_host:
                enterDemo(DemoHost.class);
                break;
            case R.id.lmv_guest:
                enterDemo(DemoGuest.class);
                break;
            case R.id.lmv_live_guest:
                enterDemo(DemoLiveGuest.class);
                break;
            case R.id.lmv_cross:
                enterDemo(DemoCross.class);
                break;
            case R.id.lmv_beauty:
                enterDemo(DemoBtu.class);
                break;
            case R.id.lmv_render:
                enterDemo(DemoRender.class);
                break;
            case R.id.lmv_log:
                showLogDialog();
                break;
            case R.id.lmv_replay:
                enterDemo(DemoReplayList.class);
                break;
            case R.id.lmv_mix:
                enterDemo(DemoMix.class);
                break;
        }
    }

    @Override
    public void onForceOffline(int error, String message) {
        DlgMgr.showMsg(getContext(), getString(R.string.str_tips_offline)).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                enterDemo(LoginActivity.class);
                finish();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION_CODE:
                for (int ret : grantResults){
                    if (PackageManager.PERMISSION_GRANTED != ret){
                        DlgMgr.showMsg(getContext(), "用户没有允许需要的权限，使用可能会受到限制！");
                    }
                }
                break;
            default:
                break;
        }
    }

    private void enterDemo(Class clsActivity){
        startActivity(new Intent(this, clsActivity));
    }

    private Context getContext(){
        return this;
    }

    private void showLogDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.str_menu_log);
        builder.setView(LayoutInflater.from(getContext()).inflate(R.layout.dialog_log, null));

        final AlertDialog dialog = DlgMgr.showAlertDlg(getContext(), builder);
        final EditText etDate = (EditText)dialog.findViewById(R.id.et_date);
        dialog.findViewById(R.id.btn_cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
        dialog.findViewById(R.id.btn_upload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int date = 0;
                if (!TextUtils.isEmpty(etDate.getText().toString()))
                    date = DemoFunc.getIntValue(etDate.getText().toString(), -1);
                if (date > 7){
                    DlgMgr.showMsg(getContext(), getString(R.string.str_tip_log_limit));
                }else {
                    ILiveSDK.getInstance().uploadLog("report log", date, new ILiveCallBack<String>() {
                        @Override
                        public void onSuccess(final String logKey) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setTitle(R.string.msg_title);
                            builder.setMessage("Log report succ:"+logKey);
                            builder.setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                            builder.setPositiveButton("Copy", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ClipboardManager cmb = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
                                    ClipData clipData = ClipData.newPlainText("text", logKey);
                                    cmb.setPrimaryClip(clipData);
                                }
                            });
                            DlgMgr.showAlertDlg(getContext(), builder);
                        }

                        @Override
                        public void onError(String module, int errCode, String errMsg) {
                            Toast.makeText(getContext(), "failed:" + module + "|" + errCode + "|" + errMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                dialog.dismiss();
            }
        });
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_PHONE_STATE)) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)){
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(MenuActivity.this,
                        (String[]) permissions.toArray(new String[0]),
                        REQ_PERMISSION_CODE);
                return false;
            }
        }

        return true;
    }
}
