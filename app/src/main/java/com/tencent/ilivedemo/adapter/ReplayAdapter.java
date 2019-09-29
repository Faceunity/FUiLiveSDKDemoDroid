package com.tencent.ilivedemo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.tencent.ilivedemo.R;
import com.tencent.ilivedemo.model.ReplayInfo;

import java.util.ArrayList;

/**
 * Created by xkazerzhang on 2017/6/22.
 */
public class ReplayAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<ReplayInfo> listReplay;

    private class ViewHolder{
        ImageView ivCover;
        TextView tvRoomid;
        TextView tvTime;
    }


    public ReplayAdapter(Context ctx, ArrayList<ReplayInfo> list){
        context = ctx;
        listReplay = list;
    }

    @Override
    public int getCount() {
        return listReplay.size();
    }

    @Override
    public Object getItem(int i) {
        return listReplay.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView != null) {
            holder = (ViewHolder)convertView.getTag();
        } else {
            convertView = LayoutInflater.from(context).inflate(R.layout.view_replay, null);

            holder = new ViewHolder();
            holder.ivCover = (ImageView) convertView.findViewById(R.id.iv_cover);
            holder.tvRoomid = (TextView) convertView.findViewById(R.id.tv_room_id);
            holder.tvTime = (TextView) convertView.findViewById(R.id.tv_time);

            convertView.setTag(holder);
        }

        ReplayInfo info = listReplay.get(position);
        if (null != info){
            holder.tvRoomid.setText(info.getRoomId());
            holder.tvTime.setText(info.getTime());
        }

        return convertView;
    }
}
