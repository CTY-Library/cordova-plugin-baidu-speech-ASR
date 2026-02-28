package com.baidu.speech.cordova;

import android.util.Log;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.SynthesizerResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 百度TTS事件适配器
 * 负责处理语音合成过程中的各种事件
 */
public class BaiduTTSEventAdapter implements SpeechSynthesizerListener {
    
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
    
    @Override
    public void onSynthesizeResponse(SynthesizerResponse response) {
        if (eventListener == null) {
            return;
        }
        
        String eventType = convertEventType(response.getSynthesizeType());
        
        Map<String, Object> data = new HashMap<>();
        data.put("sn", response.getSn());
        data.put("utteranceId", response.getUtteranceId());
        data.put("instanceId", response.getInstanceId());
        data.put("responseType", response.getResponseType());
        
        // 发送事件
        eventListener.onTTSEvent(eventType, data);
        
        Log.d(TAG, "TTS Event: " + eventType + ", sn: " + response.getSn());
    }
    
    /**
     * 转换事件类型
     */
    private String convertEventType(SynthesizerResponse.SynthesizeType type) {
        if (type == null) {
            return "unknown";
        }
        
        switch (type) {
            case SYNTHESIZE_START:
                return "synthesize_start";
            case SYNTHESIZE_DATA_ARRIVED:
                return "synthesize_data";
            case PLAY_START:
                return "play_start";
            case PLAY_FINISH:
                return "play_finish";
            case SYNTHESIZE_FINISH:
                return "synthesize_finish";
            case SYNTHESIZE_STOP:
                return "synthesize_stop";
            case SYNTHESIZE_ERROR:
                return "synthesize_error";
            case PLAY_PROGRESS:
                return "play_progress";
            case ON_NEXT:
                return "on_next";
            case AUDIO_INFO:
                return "audio_info";
            default:
                return "unknown";
        }
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
