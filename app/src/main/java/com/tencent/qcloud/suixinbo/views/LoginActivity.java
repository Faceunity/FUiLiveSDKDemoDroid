package com.tencent.qcloud.suixinbo.views;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.qcloud.suixinbo.R;
import com.tencent.qcloud.suixinbo.model.MySelfInfo;
import com.tencent.qcloud.suixinbo.presenters.InitBusinessHelper;
import com.tencent.qcloud.suixinbo.presenters.LoginHelper;
import com.tencent.qcloud.suixinbo.presenters.viewinface.LoginView;
import com.tencent.qcloud.suixinbo.utils.SxbLog;
import com.tencent.qcloud.suixinbo.views.customviews.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 登录类
 */
public class LoginActivity extends Activity implements View.OnClickListener, LoginView {
    TextView mBtnLogin, mBtnRegister;
    EditText mPassWord, mUserName;
    private static final String TAG = LoginActivity.class.getSimpleName();
    private LoginHelper mLoginHeloper;
    private final int REQUEST_PHONE_PERMISSIONS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InitBusinessHelper.initApp(getApplicationContext());
        SxbLog.i(TAG, "LoginActivity onCreate");
        mLoginHeloper = new LoginHelper(this, this);
        checkPermission();
        //获取个人数据本地缓存
        MySelfInfo.getInstance().getCache(getApplicationContext());
        if (needLogin() == true) {//本地没有账户需要登录
            initView();
        } else {
            //有账户登录直接IM登录
            SxbLog.i(TAG, "LoginActivity onCreate");
            mLoginHeloper.iLiveLogin(MySelfInfo.getInstance().getId(), MySelfInfo.getInstance().getUserSig());
        }

        // 初始化直播模块
/*        ILVLiveConfig liveConfig = new ILVLiveConfig();
        liveConfig.messageListener(MessageEvent.getInstance());
        ILVLiveManager.getInstance().init(liveConfig);*/
    }

    @Override
    protected void onDestroy() {
        mLoginHeloper.onDestory();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.registerNewUser) {
            Intent intent = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
            finish();
        }
        if (view.getId() == R.id.btn_login) {//登录账号系统TLS
            String strUser = mUserName.getText().toString();
            String strPwd = mPassWord.getText().toString();
            if (TextUtils.isEmpty(strUser)) {
                Toast.makeText(LoginActivity.this, "name can not be empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(strPwd)) {
                Toast.makeText(LoginActivity.this, "password can not be empty!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (strUser.length() < 4 || strUser.length() > 24 || Pattern.compile("^[0-9]*$").matcher(strUser).matches()
                    || !Pattern.compile("^[a-zA-Z0-9_]*$").matcher(strUser).matches()) {
                Toast.makeText(LoginActivity.this, R.string.str_hint_account, Toast.LENGTH_SHORT).show();
                return;
            }
            if (strPwd.length() < 8 || strPwd.length() > 16) {
                Toast.makeText(LoginActivity.this, R.string.str_hint_pwd, Toast.LENGTH_SHORT).show();
                return;
            }
            mLoginHeloper.standardLogin(mUserName.getText().toString(), mPassWord.getText().toString());
        }
    }

    private void initView() {
        if (null == mBtnLogin) {
            setContentView(R.layout.activity_independent_login);
            mBtnLogin = (TextView) findViewById(R.id.btn_login);
            mUserName = (EditText) findViewById(R.id.username);
            mPassWord = (EditText) findViewById(R.id.password);
            mBtnRegister = (TextView) findViewById(R.id.registerNewUser);
            mBtnRegister.setOnClickListener(this);
            mBtnLogin.setOnClickListener(this);
        }else{  // 登录失败清空密码
            mPassWord.setText("");
        }
    }


    /**
     * 判断是否需要登录
     *
     * @return true 代表需要重新登录
     */
    public boolean needLogin() {
        if (MySelfInfo.getInstance().getId() != null) {
            return false;//有账号不需要登录
        } else {
            return true;//需要登录
        }

    }


    /**
     * 直接跳转主界面
     */
    private void jumpIntoHomeActivity() {
        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }


    @Override
    public void loginSucc() {
        Toast.makeText(LoginActivity.this, "" + MySelfInfo.getInstance().getId() + " login ", Toast.LENGTH_SHORT).show();
        jumpIntoHomeActivity();
    }

    @Override
    public void loginFail(String mode,int code ,String errorinfo) {
        Toast.makeText(LoginActivity.this, "login fail" + MySelfInfo.getInstance().getId() + " : "+errorinfo, Toast.LENGTH_SHORT).show();
        initView();
    }

    void checkPermission() {
        final List<String> permissionsList = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if ((checkSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.READ_PHONE_STATE);
            if ((checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if ((checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.ACCESS_NETWORK_STATE);
            if ((checkSelfPermission(Manifest.permission.CHANGE_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED))
                permissionsList.add(Manifest.permission.CHANGE_NETWORK_STATE);
            if (permissionsList.size() != 0) {
                requestPermissions(permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_PHONE_PERMISSIONS);
            }
        }
    }
}
