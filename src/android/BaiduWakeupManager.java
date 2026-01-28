package com.baidu.speech.cordova;

import android.content.Context;
import android.util.Log;

import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONObject;

import java.util.Map;

public class BaiduWakeupManager {
    private static final String TAG = "BaiduWakeupManager";
    
    private Context context;
    private EventManager eventManager;
    private boolean isInited = false;
    private WakeupCallback callback;
    private EventListener eventListener;
    
    public interface WakeupCallback {
        void onWakeupSuccess(JSONObject result);
        void onWakeupError(int errorCode, String errorMessage);
        void onWakeupReady();
    }
    
    public BaiduWakeupManager(Context context) {
        this.context = context;
    }
    
    public void init(String apiKey, String secretKey, String appId, WakeupCallback callback) {
        this.callback = callback;
        
        try {
            // 基于官网集成方式创建EventManager
            eventManager = EventManagerFactory.create(context, "wp");
            
            eventListener = new EventListener() {
                @Override
                public void onEvent(String name, String params, byte[] data, int offset, int length) {
                    handleWakeupEvent(name, params, data, offset, length);
                }
            };
            
            eventManager.registerListener(eventListener);
            
            isInited = true;
            Log.d(TAG, "百度语音唤醒器初始化成功");
            
        } catch (Exception e) {
            Log.e(TAG, "百度语音唤醒器初始化失败: " + e.getMessage(), e);
            if (callback != null) {
                callback.onWakeupError(-1, "初始化失败: " + e.getMessage());
            }
        }
    }
    
    public void startWakeup(Map<String, Object> params) {
        if (!isInited || eventManager == null) {
            if (callback != null) {
                callback.onWakeupError(2001, "SDK未初始化");
            }
            return;
        }
        
        try {
            // 设置唤醒参数 - 基于官网集成方式
            JSONObject paramJson = new JSONObject();
            
            // 基础参数 - 官网推荐格式
            paramJson.put("accept-audio-data", false); // 不接受音频数据
            paramJson.put("accept-audio-volume", true); // 接受音量回调
            
            // 用户自定义参数
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    try {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        
                        // 参数映射到官网格式
                        if (key.equals("acceptAudioData")) {
                            paramJson.put("accept-audio-data", value);
                        } else if (key.equals("acceptAudioVolume")) {
                            paramJson.put("accept-audio-volume", value);
                        } else {
                            // 其他参数直接添加
                            paramJson.put(key, value);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "设置参数失败: " + entry.getKey());
                    }
                }
            }
            
            Log.d(TAG, "唤醒参数(官网格式): " + paramJson.toString());
            
            // 发送开始唤醒命令 - 基于官网集成方式
            eventManager.send(SpeechConstant.WAKEUP_START, paramJson.toString(), null, 0, 0);
            Log.d(TAG, "开始语音唤醒");
            
        } catch (Exception e) {
            Log.e(TAG, "开始唤醒失败: " + e.getMessage(), e);
            if (callback != null) {
                callback.onWakeupError(-1, "开始唤醒失败: " + e.getMessage());
            }
        }
    }
    
    public void stopWakeup() {
        if (!isInited || eventManager == null) {
            return;
        }
        
        try {
            eventManager.send(SpeechConstant.WAKEUP_STOP, null, null, 0, 0);
            Log.d(TAG, "停止语音唤醒");
        } catch (Exception e) {
            Log.e(TAG, "停止唤醒失败: " + e.getMessage(), e);
        }
    }
    
    public void release() {
        if (eventManager != null) {
            try {
                eventManager.unregisterListener(eventListener);
                eventManager = null;
            } catch (Exception e) {
                Log.e(TAG, "释放资源失败: " + e.getMessage(), e);
            }
        }
        
        isInited = false;
        callback = null;
        Log.d(TAG, "百度语音唤醒器已释放");
    }
    
    private void handleWakeupEvent(String name, String params, byte[] data, int offset, int length) {
        Log.d(TAG, "唤醒事件: " + name + ", 参数: " + params);
        
        if (callback == null) {
            return;
        }
        
        try {
            switch (name) {
                case SpeechConstant.CALLBACK_EVENT_WAKEUP_READY:
                    callback.onWakeupReady();
                    break;
                    
                case SpeechConstant.CALLBACK_EVENT_WAKEUP_SUCCESS:
                    if (params != null) {
                        JSONObject result = new JSONObject(params);
                        callback.onWakeupSuccess(result);
                    }
                    break;
                    
                case SpeechConstant.CALLBACK_EVENT_WAKEUP_ERROR:
                    if (params != null) {
                        JSONObject errorJson = new JSONObject(params);
                        int errorCode = errorJson.optInt("error", -1);
                        String errorMessage = errorJson.optString("desc", "未知错误");
                        callback.onWakeupError(errorCode, errorMessage);
                    }
                    break;
                    
                default:
                    Log.d(TAG, "未处理的事件: " + name);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "处理唤醒事件失败: " + e.getMessage(), e);
        }
    }
    
    public boolean isInited() {
        return isInited;
    }
}
