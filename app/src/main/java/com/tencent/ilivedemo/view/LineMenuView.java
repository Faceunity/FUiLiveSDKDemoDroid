package com.tencent.ilivedemo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.tencent.ilivedemo.R;


/**
 * 设置等页面条状控制或显示信息的控件
 */
public class LineMenuView extends LinearLayout {
    private TextView tvTitle;
    private ImageView ivIcon;

    public LineMenuView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_line_menu, this);

        tvTitle = (TextView)findViewById(R.id.lm_title);
        ivIcon = (ImageView)findViewById(R.id.lm_icon);

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.lmvattr);
        int count = ta.getIndexCount();
        for (int i=0; i<count; i++){
            int itemId = ta.getIndex(i);
            switch (itemId){
                case R.styleable.lmvattr_icon:
                    int resId = ta.getResourceId(itemId, -1);
                    if (-1 != resId)
                        setIcon(resId);
                    break;
                case R.styleable.lmvattr_title:
                    setTitle(ta.getString(itemId));
                    break;
            }
        }
    }


    // 设置 Icon
    public void setIcon(int res){
        ivIcon.setImageResource(res);
    }

    // 设置标题
    public void setTitle(String title){
        tvTitle.setText(title);
    }
}
