package com.tencent.ilivedemo.demos;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.tencent.ilivedemo.R;
import com.tencent.ilivedemo.adapter.ReplayAdapter;
import com.tencent.ilivedemo.model.ReplayInfo;
import com.tencent.ilivedemo.model.UserInfo;
import com.tencent.ilivedemo.uiutils.DlgMgr;
import com.tencent.ilivesdk.ILiveSDK;

import org.json.JSONArray;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by xkazerzhang on 2017/6/22.
 */
public class DemoReplayList extends Activity implements SwipeRefreshLayout.OnRefreshListener, View.OnClickListener {
    private ListView lvReplay;
    private SwipeRefreshLayout srlSwipe;

    private OkHttpClient okHttpClient;
    private ArrayList<ReplayInfo> listReplay;
    private ReplayAdapter replayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.demo_replaylist);

        srlSwipe = (SwipeRefreshLayout)findViewById(R.id.srl_swipe);
        lvReplay = (ListView)findViewById(R.id.lv_list);

        srlSwipe.setOnRefreshListener(this);
        lvReplay.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int postion, long l) {
                ReplayInfo info = listReplay.get(postion);
                if (null != info){
                    UserInfo.getInstance().setReplayUrl(info.getUrl());
                    startActivity(new Intent(getContext(), DemoReplay.class));
                }else{
                    DlgMgr.showMsg(getContext(), "Get Play Url Fail!");
                }
            }
        });

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .build();

        listReplay = new ArrayList<>();
        replayAdapter = new ReplayAdapter(this, listReplay);

        lvReplay.setAdapter(replayAdapter);
        onRefresh();
    }

    @Override
    public void onRefresh() {
        Request request = new Request.Builder()
                .url("https://sxb.qcloud.com/recordlist")
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
                        try {
                            JSONArray jsonRsp = new JSONArray(data);
                            listReplay.clear();
                            for (int i=0; i<jsonRsp.length(); i++){
                                listReplay.add(new ReplayInfo(jsonRsp.getJSONObject(i)));
                            }
                            replayAdapter.notifyDataSetChanged();
                        }catch (Exception e){
                            DlgMgr.showMsg(getContext(), "Parse fail: "+e.toString());
                        }
                        srlSwipe.setRefreshing(false);
                    }
                }, 0);
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.iv_return:
                finish();
                break;
        }
    }

    private Context getContext(){
        return this;
    }
}
