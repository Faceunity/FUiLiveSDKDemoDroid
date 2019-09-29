package com.tencent.ilivedemo.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.tencent.ilivedemo.R;
import com.tencent.ilivedemo.model.StatusObservable;
import com.tencent.ilivedemo.model.UserInfo;
import com.tencent.ilivedemo.uiutils.DlgMgr;
import com.tencent.ilivedemo.view.DemoEditText;
import com.tencent.ilivesdk.ILiveCallBack;
import com.tencent.ilivesdk.core.ILiveLoginManager;

/**
 * Created by xkazerzhang on 2017/5/22.
 */
public class LoginActivity extends Activity implements View.OnClickListener {
    private DemoEditText etAccount, etPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (StatusObservable.getInstance().getObserverCount() > 0){
            // 避免重复打开
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etAccount = (DemoEditText) findViewById(R.id.et_account);
        etPwd = (DemoEditText) findViewById(R.id.et_password);

        UserInfo.getInstance().getCache(getApplicationContext());
        etAccount.setText(UserInfo.getInstance().getAccount());
        etPwd.setText(UserInfo.getInstance().getPassword());
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                login();
                break;
            case R.id.btn_regist:
                regist();
                break;
        }
    }

    private Context getContenxt(){
        return this;
    }

    // 注册
    private void regist(){
        String strAccount = etAccount.getText().toString();
        String strPwd = etPwd.getText().toString();

        if (TextUtils.isEmpty(strAccount) || TextUtils.isEmpty(strPwd)){
            DlgMgr.showMsg(getContenxt(), R.string.msg_input_empty);
            return;
        }

        ILiveLoginManager.getInstance().tlsRegister(strAccount, strPwd, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                DlgMgr.showMsg(getContenxt(), R.string.msg_regist_success);
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                DlgMgr.showMsg(getContenxt(), "Regist failed:"+module+"|"+errCode+"|"+errMsg);
            }
        });
    }

    // 登录
    private void login(){
        String strAccount = etAccount.getText().toString();
        String strPwd = etPwd.getText().toString();

        if (TextUtils.isEmpty(strAccount) || TextUtils.isEmpty(strPwd)){
            DlgMgr.showMsg(getContenxt(), R.string.msg_input_empty);
            return;
        }

        ILiveLoginManager.getInstance().tlsLoginAll(strAccount, strPwd, new ILiveCallBack() {
            @Override
            public void onSuccess(Object data) {
                afterLogin();
            }

            @Override
            public void onError(String module, int errCode, String errMsg) {
                DlgMgr.showMsg(getContenxt(), "Login failed:"+module+"|"+errCode+"|"+errMsg);
            }
        });
    }

    // 登录成功
    private void afterLogin(){
        ILiveLoginManager.getInstance().setUserStatusListener(StatusObservable.getInstance());
        UserInfo.getInstance().setAccount(etAccount.getText().toString());
        UserInfo.getInstance().setPassword(etPwd.getText().toString());
        UserInfo.getInstance().writeToCache(getApplicationContext());
        Intent intent = new Intent(getContenxt(), MenuActivity.class);
        startActivity(intent);
        finish();
    }
}
