package com.baidu.speech.cordova;

import android.content.Context;
import android.util.Log;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.SynthesizerResponse;
import com.baidu.tts.client.TtsEntity;
import com.baidu.tts.client.TtsMode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 百度语音合成管理器
 * 负责TTS功能的初始化、配置和语音合成
 */
public class BaiduTTSManager {
    
    private static final String TAG = "BaiduTTSManager";
    
    // 语音合成器
    private SpeechSynthesizer speechSynthesizer;
    
    // 上下文
    private Context context;
    
    // 事件适配器
    private BaiduTTSEventAdapter eventAdapter;
    
    // 配置参数
    private String apiKey;
    private String secretKey;
    private String appId;
    private String speaker = "4100"; // 默认发音人
    private int speed = 5; // 默认语速
    private int pitch = 5; // 默认音调
    private int volume = 5; // 默认音量
    
    // 状态标识
    private boolean isInitialized = false;
    private boolean isSpeaking = false;
    
    public BaiduTTSManager(Context context) {
        this.context = context;
        this.eventAdapter = new BaiduTTSEventAdapter();
    }
    
    /**
     * 初始化TTS
     */
    public boolean initialize(String apiKey, String secretKey, String appId) {
        Log.d(TAG, "Initializing TTS...");
        
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.appId = appId;
        
        try {
            // 创建语音合成器
            speechSynthesizer = new SpeechSynthesizer(context, appId);
            
            // 设置参数
            setupOnlineParams();
            
            // 初始化在线TTS服务
            boolean success = speechSynthesizer.initializeTts();
            if (success) {
                isInitialized = true;
                Log.d(TAG, "TTS initialized successfully");
                
                // 发送初始化成功事件
                eventAdapter.sendEvent("tts_initialized");
                return true;
            } else {
                Log.e(TAG, "Failed to initialize TTS");
                eventAdapter.sendEvent("tts_error", new HashMap<String, Object>() {{
                    put("message", "TTS initialization failed");
                }});
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "TTS initialization error", e);
            eventAdapter.sendEvent("tts_error", new HashMap<String, Object>() {{
                put("message", "TTS initialization error: " + e.getMessage());
            }});
            return false;
        }
    }
    
    /**
     * 设置在线参数
     */
    private void setupOnlineParams() {
        // 设置认证参数
        speechSynthesizer.setApiKey(apiKey);
        speechSynthesizer.setSecretKey(secretKey);
        
        // 设置发音人
        speechSynthesizer.setSpeaker(speaker);
        
        // 设置语速、音调、音量
        speechSynthesizer.setSpeed(speed);
        speechSynthesizer.setPitch(pitch);
        speechSynthesizer.setVolume(volume);
    }
    
    /**
     * 开始语音合成并播放
     */
    public boolean speak(String text) {
        if (!isInitialized) {
            Log.e(TAG, "TTS not initialized");
            return false;
        }
        
        if (isSpeaking) {
            Log.w(TAG, "TTS is already speaking");
            return false;
        }
        
        try {
            TtsEntity ttsEntity = new TtsEntity(text, TtsMode.ONLINE);
            boolean success = speechSynthesizer.speak(ttsEntity);
            
            if (success) {
                isSpeaking = true;
                Log.d(TAG, "TTS speak started: " + text);
                
                // 发送开始播放事件
                eventAdapter.sendEvent("speak_start", new HashMap<String, Object>() {{
                    put("text", text);
                }});
                return true;
            } else {
                Log.e(TAG, "Failed to speak");
                eventAdapter.sendEvent("speak_error", new HashMap<String, Object>() {{
                    put("message", "Failed to start speaking");
                }});
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Speak error", e);
            eventAdapter.sendEvent("speak_error", new HashMap<String, Object>() {{
                put("message", "Speak error: " + e.getMessage());
            }});
            return false;
        }
    }
    
    /**
     * 仅合成不播放
     */
    public boolean synthesize(String text) {
        if (!isInitialized) {
            Log.e(TAG, "TTS not initialized");
            return false;
        }
        
        try {
            TtsEntity ttsEntity = new TtsEntity(text, TtsMode.ONLINE);
            boolean success = speechSynthesizer.synthesize(ttsEntity);
            
            if (success) {
                Log.d(TAG, "TTS synthesize started: " + text);
                return true;
            } else {
                Log.e(TAG, "Failed to synthesize");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Synthesize error", e);
            return false;
        }
    }
    
    /**
     * 暂停播放
     */
    public boolean pause() {
        if (!isInitialized || !isSpeaking) {
            return false;
        }
        
        try {
            speechSynthesizer.pause();
            Log.d(TAG, "TTS paused");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Pause error", e);
            return false;
        }
    }
    
    /**
     * 恢复播放
     */
    public boolean resume() {
        if (!isInitialized) {
            return false;
        }
        
        try {
            speechSynthesizer.resume();
            Log.d(TAG, "TTS resumed");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Resume error", e);
            return false;
        }
    }
    
    /**
     * 停止播放
     */
    public boolean stop() {
        if (!isInitialized) {
            return false;
        }
        
        try {
            speechSynthesizer.stop();
            isSpeaking = false;
            Log.d(TAG, "TTS stopped");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Stop error", e);
            return false;
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        if (speechSynthesizer != null) {
            speechSynthesizer.release();
            speechSynthesizer = null;
        }
        isInitialized = false;
        isSpeaking = false;
        Log.d(TAG, "TTS released");
    }
    
    /**
     * 设置发音人
     */
    public void setSpeaker(String speaker) {
        this.speaker = speaker;
        if (isInitialized && speechSynthesizer != null) {
            speechSynthesizer.setSpeaker(speaker);
        }
    }
    
    /**
     * 设置语速 (1-15)
     */
    public void setSpeed(int speed) {
        this.speed = Math.max(1, Math.min(15, speed));
        if (isInitialized && speechSynthesizer != null) {
            speechSynthesizer.setSpeed(this.speed);
        }
    }
    
    /**
     * 设置音调 (1-15)
     */
    public void setPitch(int pitch) {
        this.pitch = Math.max(1, Math.min(15, pitch));
        if (isInitialized && speechSynthesizer != null) {
            speechSynthesizer.setPitch(this.pitch);
        }
    }
    
    /**
     * 设置音量 (1-15)
     */
    public void setVolume(int volume) {
        this.volume = Math.max(1, Math.min(15, volume));
        if (isInitialized && speechSynthesizer != null) {
            speechSynthesizer.setVolume(this.volume);
        }
    }
    
    /**
     * 设置事件回调
     */
    public void setEventListener(BaiduTTSEventAdapter.TTSEventListener listener) {
        if (eventAdapter != null) {
            eventAdapter.setEventListener(listener);
        }
    }
    
    /**
     * 获取状态信息
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("initialized", isInitialized);
        status.put("speaking", isSpeaking);
        status.put("speaker", speaker);
        status.put("speed", speed);
        status.put("pitch", pitch);
        status.put("volume", volume);
        return status;
    }
    
    /**
     * 获取版本信息
     */
    public String getVersion() {
        return "6.2.7"; // 百度TTS SDK版本
    }
}
