package com.tencent.ilivedemo.model;

import com.tencent.ilivesdk.data.ILiveMessage;
import com.tencent.ilivesdk.listener.ILiveMessageListener;

import java.util.LinkedList;

/**
 * 消息观察者
 */
public class MessageObservable implements ILiveMessageListener {
    // 消息监听链表
    private LinkedList<ILiveMessageListener> listObservers = new LinkedList<>();
    // 句柄
    private static MessageObservable instance;


    public static MessageObservable getInstance(){
        if (null == instance){
            synchronized (MessageObservable.class){
                if (null == instance){
                    instance = new MessageObservable();
                }
            }
        }
        return instance;
    }


    // 添加观察者
    public void addObserver(ILiveMessageListener listener){
        if (!listObservers.contains(listener)){
            listObservers.add(listener);
        }
    }

    // 移除观察者
    public void deleteObserver(ILiveMessageListener listener){
        listObservers.remove(listener);
    }

    @Override
    public void onNewMessage(ILiveMessage message) {
        // 拷贝链表
        LinkedList<ILiveMessageListener> tmpList = new LinkedList<>(listObservers);
        for (ILiveMessageListener listener : tmpList){
            listener.onNewMessage(message);
        }
    }
}
