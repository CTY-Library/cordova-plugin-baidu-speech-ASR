package com.baidu.speech.cordova;

import com.baidu.asr.authlibrary.TemporaryToken;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * 百度语音识别认证工具类
 * 基于官方SDK的AuthUtil实现
 */
public class BaiduAuthUtil {
    
    private static String apiKey = "";
    private static String secretKey = "";
    private static String appId = "";
    
    /**
     * 设置认证信息
     */
    public static void setAuthInfo(String ak, String sk, String id) {
        android.util.Log.d("BaiduAuthUtil", "设置认证信息:");
        android.util.Log.d("BaiduAuthUtil", "  API Key: " + (ak.isEmpty() ? "未设置" : maskKey(ak)));
        android.util.Log.d("BaiduAuthUtil", "  Secret Key: " + (sk.isEmpty() ? "未设置" : maskKey(sk)));
        android.util.Log.d("BaiduAuthUtil", "  App ID: " + (id.isEmpty() ? "未设置" : id));
        
        apiKey = ak;
        secretKey = sk;
        appId = id;
        
        android.util.Log.d("BaiduAuthUtil", "认证信息设置完成");
    }
    
    /**
     * 隐藏敏感信息
     */
    private static String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
    
    /**
     * 获取API Key
     */
    public static String getAk() {
        android.util.Log.d("BaiduAuthUtil", "获取API Key: " + (apiKey.isEmpty() ? "未设置" : maskKey(apiKey)));
        return apiKey;
    }
    
    /**
     * 获取Secret Key
     */
    public static String getSk() {
        android.util.Log.d("BaiduAuthUtil", "获取Secret Key: " + (secretKey.isEmpty() ? "未设置" : maskKey(secretKey)));
        return secretKey;
    }
    
    /**
     * 获取App ID
     */
    public static String getAppId() {
        android.util.Log.d("BaiduAuthUtil", "获取App ID: " + (appId.isEmpty() ? "未设置" : appId));
        return appId;
    }
    
    /**
     * 获取IAM Key (预留)
     */
    public static String getIamKey() {
        return "";
    }
    
    /**
     * 获取临时Token
     * 基于官方SDK的getToken实现
     */
    public static TemporaryToken getToken() {
        if (apiKey.isEmpty() || secretKey.isEmpty()) {
            android.util.Log.e("BaiduAuthUtil", "API Key或Secret Key未设置");
            return null;
        }
        
        // 获取token地址
        String authHost = "https://aip.baidubce.com/oauth/2.0/token?";
        String getAccessTokenUrl = authHost
                // 1. grant_type为固定参数
                + "grant_type=client_credentials"
                // 2. 官网获取的 API Key
                + "&client_id=" + apiKey
                // 3. 官网获取的 Secret Key
                + "&client_secret=" + secretKey;
        
        try {
            URL realUrl = new URL(getAccessTokenUrl);
            // 打开和URL之间的连接
            HttpURLConnection connection = (HttpURLConnection) realUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.connect();
            
            // 获取所有响应头字段
            Map<String, List<String>> map = connection.getHeaderFields();
            
            // 定义 BufferedReader输入流来读取URL的响应
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String result = "";
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
            
            android.util.Log.d("BaiduAuthUtil", "Token response: " + result);
            
            JSONObject jsonObject = new JSONObject(result);
            String accessToken = jsonObject.getString("access_token");
            long time = jsonObject.getLong("expires_in");
            
            return new TemporaryToken(accessToken, System.currentTimeMillis() + time * 1000);
            
        } catch (Exception e) {
            android.util.Log.e("BaiduAuthUtil", "获取token失败: " + e.getMessage(), e);
        }
        return null;
    }
    
    /**
     * 检查认证信息是否完整
     */
    public static boolean isAuthInfoComplete() {
        return !apiKey.isEmpty() && !secretKey.isEmpty() && !appId.isEmpty();
    }
    
    /**
     * 清空认证信息
     */
    public static void clearAuthInfo() {
        apiKey = "";
        secretKey = "";
        appId = "";
    }
}
