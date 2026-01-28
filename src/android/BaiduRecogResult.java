package com.baidu.speech.cordova;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 百度语音识别结果类
 * 基于官方SDK的RecogResult实现
 */
public class BaiduRecogResult {
    
    private String origalJson;
    private String[] resultsRecognition;
    private String resultType;
    private int error = 0;
    private int subError = 0;
    private String desc;
    private long sn;
    
    /**
     * 解析JSON结果
     */
    public static BaiduRecogResult parseJson(String jsonStr) {
        BaiduRecogResult result = new BaiduRecogResult();
        result.origalJson = jsonStr;
        
        try {
            JSONObject json = new JSONObject(jsonStr);
            
            // 解析识别结果
            if (json.has("results_recognition")) {
                Object resultsObj = json.get("results_recognition");
                if (resultsObj instanceof org.json.JSONArray) {
                    org.json.JSONArray array = (org.json.JSONArray) resultsObj;
                    result.resultsRecognition = new String[array.length()];
                    for (int i = 0; i < array.length(); i++) {
                        result.resultsRecognition[i] = array.getString(i);
                    }
                }
            }
            
            // 解析结果类型
            if (json.has("result_type")) {
                result.resultType = json.getString("result_type");
            }
            
            // 解析错误信息
            if (json.has("err_no")) {
                result.error = json.getInt("err_no");
            }
            if (json.has("sub_err")) {
                result.subError = json.getInt("sub_err");
            }
            if (json.has("desc")) {
                result.desc = json.getString("desc");
            }
            
            // 解析序列号
            if (json.has("sn")) {
                result.sn = json.getLong("sn");
            }
            
        } catch (JSONException e) {
            android.util.Log.e("BaiduRecogResult", "解析JSON失败: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 是否为最终结果
     */
    public boolean isFinalResult() {
        return "final_result".equals(resultType);
    }
    
    /**
     * 是否为临时结果
     */
    public boolean isPartialResult() {
        return "partial_result".equals(resultType);
    }
    
    /**
     * 是否为NLU结果
     */
    public boolean isNluResult() {
        return "nlu_result".equals(resultType);
    }
    
    /**
     * 是否有错误
     */
    public boolean hasError() {
        return error != 0;
    }
    
    /**
     * 获取识别结果文本
     */
    public String getRecognitionText() {
        if (resultsRecognition != null && resultsRecognition.length > 0) {
            return resultsRecognition[0];
        }
        return "";
    }
    
    /**
     * 获取所有识别结果
     */
    public String[] getResultsRecognition() {
        return resultsRecognition;
    }
    
    /**
     * 获取结果类型
     */
    public String getResultType() {
        return resultType;
    }
    
    /**
     * 获取错误码
     */
    public int getError() {
        return error;
    }
    
    /**
     * 获取子错误码
     */
    public int getSubError() {
        return subError;
    }
    
    /**
     * 获取错误描述
     */
    public String getDesc() {
        return desc;
    }
    
    /**
     * 获取原始JSON
     */
    public String getOrigalJson() {
        return origalJson;
    }
    
    /**
     * 获取序列号
     */
    public long getSn() {
        return sn;
    }
    
    @Override
    public String toString() {
        return "BaiduRecogResult{" +
                "resultType='" + resultType + '\'' +
                ", error=" + error +
                ", subError=" + subError +
                ", desc='" + desc + '\'' +
                ", recognitionText='" + getRecognitionText() + '\'' +
                '}';
    }
}
