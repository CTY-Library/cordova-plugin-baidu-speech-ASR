package com.baidu.speech.cordova;

import com.baidu.speech.asr.SpeechConstant;

import java.util.HashMap;
import java.util.Map;

/**
 * 百度语音识别参数配置工具类
 * 提供常用的参数配置方法
 */
public class BaiduASRParams {
    
    /**
     * 创建默认的在线识别参数 - 基于官网集成方式
     */
    public static Map<String, Object> createDefaultOnlineParams() {
        Map<String, Object> params = new HashMap<>();
        
        // 官网推荐的基础参数格式
        params.put("accept-audio-data", false); // 不接受音频数据
        params.put("disable-punctuation", false); // 不禁用标点符号
        params.put("accept-audio-volume", true); // 接受音量回调
        params.put("pid", 1537); // 普通话(支持简单的英文识别)
        
        // VAD参数
        params.put("vad-endpoint-timeout", 8000); // 静音超时时间
        params.put("vad-endpoint-silence-time", 2000); // 静音检测时间
        
        return params;
    }
    
    /**
     * 创建长语音识别参数 - 基于官网集成方式
     */
    public static Map<String, Object> createLongSpeechParams() {
        Map<String, Object> params = createDefaultOnlineParams();
        params.put("vad-endpoint-timeout", 30000); // 长语音需要更长的超时时间
        return params;
    }
    
    /**
     * 创建英文识别参数 - 基于官网集成方式
     */
    public static Map<String, Object> createEnglishParams() {
        Map<String, Object> params = new HashMap<>();
        
        params.put("accept-audio-data", false);
        params.put("disable-punctuation", false);
        params.put("accept-audio-volume", true);
        params.put("pid", 1737); // 英语
        params.put("vad-endpoint-timeout", 8000);
        params.put("vad-endpoint-silence-time", 2000);
        
        return params;
    }
    
    /**
     * 创建粤语识别参数 - 基于官网集成方式
     */
    public static Map<String, Object> createCantoneseParams() {
        Map<String, Object> params = new HashMap<>();
        
        params.put("accept-audio-data", false);
        params.put("disable-punctuation", false);
        params.put("accept-audio-volume", true);
        params.put("pid", 1637); // 粤语
        params.put("vad-endpoint-timeout", 8000);
        params.put("vad-endpoint-silence-time", 2000);
        
        return params;
    }
    
    /**
     * 创建高精度识别参数 - 基于官网集成方式
     */
    public static Map<String, Object> createHighAccuracyParams() {
        Map<String, Object> params = createDefaultOnlineParams();
        
        // 启用标点符号优化
        params.put("disable-punctuation", false);
        
        // 设置更长的静音超时，提高准确性
        params.put("vad-endpoint-timeout", 10000);
        params.put("vad-endpoint-silence-time", 3000);
        
        return params;
    }
    
    /**
     * 创建快速识别参数（适合短语音） - 基于官网集成方式
     */
    public static Map<String, Object> createFastParams() {
        Map<String, Object> params = new HashMap<>();
        
        params.put("accept-audio-data", false);
        params.put("disable-punctuation", true); // 禁用标点符号，加快速度
        params.put("accept-audio-volume", true);
        params.put("pid", 1537);
        
        // 较短的超时时间
        params.put("vad-endpoint-timeout", 3000);
        params.put("vad-endpoint-silence-time", 1000);
        
        return params;
    }
    
    /**
     * 根据语言代码创建参数
     * @param language 语言代码：zh, en, cantonese
     */
    public static Map<String, Object> createParamsByLanguage(String language) {
        switch (language.toLowerCase()) {
            case "en":
            case "english":
                return createEnglishParams();
            case "cantonese":
                return createCantoneseParams();
            case "zh":
            case "chinese":
            default:
                return createDefaultOnlineParams();
        }
    }
    
    /**
     * 获取所有支持的PID列表
     */
    public static String[] getSupportedPIDs() {
        return new String[]{
            "1536 - 普通话(支持纯中文)",
            "1537 - 普通话(支持简单的英文识别)", 
            "1737 - 英语",
            "1637 - 粤语",
            "1837 - 四川话"
        };
    }
}
