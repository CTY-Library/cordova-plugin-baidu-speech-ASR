package com.baidu.speech.cordova;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 百度TTS事件适配器
 * 负责处理语音合成过程中的各种事件
 */
public class BaiduTTSEventAdapter {
    
    private static final String TAG = "BaiduTTSEventAdapter";
    
    // 事件监听器
    private TTSEventListener eventListener;
    
    /**
     * TTS事件监听器接口
     */
    public interface TTSEventListener {
        void onTTSEvent(String type, Map<String, Object> data);
    }
    
    public void setEventListener(TTSEventListener listener) {
        this.eventListener = listener;
    }
    
    /**
     * 发送TTS事件的简化方法
     */
    public void sendEvent(String type, Map<String, Object> data) {
        if (eventListener != null) {
            eventListener.onTTSEvent(type, data);
        }
    }
    
    /**
     * 发送TTS事件的简化方法（无数据）
     */
    public void sendEvent(String type) {
        sendEvent(type, new HashMap<String, Object>());
    }
}
