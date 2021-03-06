package com.tencent.ilivedemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.tencent.ilivedemo.ui.LoginActivity;
import com.tencent.ilivedemo.uiutils.PreferenceUtil;


public class NeedFaceUnityAcct extends Activity {

    private boolean isOn = true;//是否使用FaceUnity

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_faceunity);

        final Button button = (Button) findViewById(R.id.btn_set);
        String isOpen = PreferenceUtil.getString(this, PreferenceUtil.KEY_FACEUNITY_ISON);
        if (TextUtils.isEmpty(isOpen) || "false".equals(isOpen)) {
            isOn = false;
        } else {
            isOn = true;
        }
        button.setText(isOn ? "On" : "Off");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isOn = !isOn;
                button.setText(isOn ? "On" : "Off");
            }
        });

        Button btnToMain = (Button) findViewById(R.id.btn_to_main);
        btnToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NeedFaceUnityAcct.this, LoginActivity.class);
                PreferenceUtil.persistString(NeedFaceUnityAcct.this, PreferenceUtil.KEY_FACEUNITY_ISON,
                        isOn + "");
                startActivity(intent);
                finish();
            }
        });

    }
}
