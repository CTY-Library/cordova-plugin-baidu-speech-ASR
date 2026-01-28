package com.baidu.speech.cordova;

import android.util.Log;
import com.baidu.speech.EventListener;
import com.baidu.speech.asr.SpeechConstant;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 百度语音识别事件适配器
 * 基于官方SDK的RecogEventAdapter实现
 */
public class BaiduRecogEventAdapter implements EventListener {
    
    private BaiduIRecogListener listener;
    private static final String TAG = "BaiduRecogEventAdapter";
    
    public BaiduRecogEventAdapter(BaiduIRecogListener listener) {
        this.listener = listener;
    }
    
    @Override
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String logMessage = "name:" + name + "; params:" + params;
        Log.i(TAG, logMessage);
        
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_LOADED)) {
            Log.d(TAG, "ASR_LOADED event received");
            listener.onOfflineLoaded();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_UNLOADED)) {
            Log.d(TAG, "ASR_UNLOADED event received");
            listener.onOfflineUnLoaded();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_READY)) {
            Log.d(TAG, "ASR_READY event received - engine ready");
            // 引擎准备就绪，可以开始说话
            listener.onAsrReady();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_BEGIN)) {
            Log.d(TAG, "ASR_BEGIN event received - user started speaking");
            // 检测到用户的已经开始说话
            listener.onAsrBegin();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_END)) {
            Log.d(TAG, "ASR_END event received - user stopped speaking");
            // 检测到用户的已经停止说话
            listener.onAsrEnd();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
            Log.d(TAG, "Received ASR_PARTIAL event: " + params);
            BaiduRecogResult recogResult = BaiduRecogResult.parseJson(params);
            // 识别结果
            String[] results = recogResult.getResultsRecognition();
            Log.d(TAG, "Parsed results: " + java.util.Arrays.toString(results));
            Log.d(TAG, "Is final result: " + recogResult.isFinalResult());
            Log.d(TAG, "Is partial result: " + recogResult.isPartialResult());
            
            if (recogResult.isFinalResult()) {
                // 最终识别结果，长语音每一句话会回调一次
                Log.d(TAG, "Calling onAsrFinalResult");
                listener.onAsrFinalResult(results, recogResult);
            } else if (recogResult.isPartialResult()) {
                // 临时识别结果
                Log.d(TAG, "Calling onAsrPartialResult");
                listener.onAsrPartialResult(results, recogResult);
            } else if (recogResult.isNluResult()) {
                // 语义理解结果
                String nluResult = new String(data, offset, length);
                listener.onAsrOnlineNluResult(nluResult);
            }
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_FINISH)) {
            // 识别结束
            BaiduRecogResult recogResult = BaiduRecogResult.parseJson(params);
            if (recogResult.hasError()) {
                int errorCode = recogResult.getError();
                int subErrorCode = recogResult.getSubError();
                Log.e(TAG, "asr error:" + params);
                listener.onAsrFinishError(errorCode, subErrorCode, recogResult.getDesc(), recogResult);
            } else {
                listener.onAsrFinish(recogResult);
            }
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_LONG_SPEECH)) {
            // 长语音
            listener.onAsrLongFinish();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_EXIT)) {
            listener.onAsrExit();
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_VOLUME)) {
            // 音量回调
            Volume vol = parseVolumeJson(params);
            listener.onAsrVolume(vol.volumePercent, vol.volume);
        } else if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_AUDIO)) {
            if (data.length != length) {
                Log.e(TAG, "internal error: asr.audio callback data length is not equal to length param");
            }
            listener.onAsrAudio(data, offset, length);
        }
    }
    
    private Volume parseVolumeJson(String jsonStr) {
        Volume vol = new Volume();
        vol.origalJson = jsonStr;
        try {
            JSONObject json = new JSONObject(jsonStr);
            vol.volumePercent = json.getInt("volume-percent");
            vol.volume = json.getInt("volume");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return vol;
    }
    
    private class Volume {
        private int volumePercent = -1;
        private int volume = -1;
        private String origalJson;
    }
}
