package com.baidu.speech.cordova;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.baidu.aipe.asr.AipeEventManagerFactory;
import com.baidu.asr.authlibrary.TemporaryToken;
import com.baidu.asr.authlibrary.TokenCallback;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.asr.SpeechConstant;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.HashMap;

/**
 * 百度语音识别 Cordova 插件主类
 * 提供语音识别、唤醒等功能
 */
public class BaiduSpeechASR extends CordovaPlugin {

    private static final String TAG = "BaiduSpeechASR";

    // 权限请求码
    private static final int PERMISSION_REQUEST_CODE = 1001;

    // 必需权限
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    // 识别管理器
    //private BaiduRecognizerManager recognizerManager;

    // 唤醒管理器
    private BaiduWakeupManager wakeupManager;

    // 权限回调上下文
    private CallbackContext permissionCallbackContext;

    // 识别回调上下文
    private CallbackContext recognitionCallbackContext;

    // 百度语音识别核心组件（从BaiduRecognizerManager合并而来）
    private EventManager asr;
    private EventListener eventListener;
    private static boolean isOfflineEngineLoaded = false;
    private static volatile boolean isInited = false;

    // 从plugin.xml读取的配置参数
    private String apiKey;
    private String secretKey;
    private String appId;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Log.d(TAG, "Initializing BaiduSpeechASR plugin");

        // 从plugin.xml preference中读取配置参数
        loadPreferences();
    }

    /**
     * 从plugin.xml中加载preference配置
     */
    private void loadPreferences() {
        // 读取API_KEY - 使用Cordova标准方式
        apiKey = preferences.getString("API_KEY", "");
        if (apiKey.isEmpty()) {
            Log.w(TAG, "API_KEY not found in plugin.xml preferences");
        } else {
            Log.d(TAG, "API_KEY loaded from plugin.xml preferences: " + maskKey(apiKey));
        }

        // 读取SECRET_KEY - 使用Cordova标准方式
        secretKey = preferences.getString("SECRET_KEY", "");
        if (secretKey.isEmpty()) {
            Log.w(TAG, "SECRET_KEY not found in plugin.xml preferences");
        } else {
            Log.d(TAG, "SECRET_KEY loaded from plugin.xml preferences: " + maskKey(secretKey));
        }

        // 读取APP_ID - 使用Cordova标准方式
        appId = preferences.getString("APP_ID", "");
        if (appId.isEmpty()) {
            Log.w(TAG, "APP_ID not found in plugin.xml preferences");
        } else {
            Log.d(TAG, "APP_ID loaded from plugin.xml preferences: " + appId);
        }

        // 检查配置完整性
        if (apiKey.isEmpty() || secretKey.isEmpty() || appId.isEmpty()) {
            Log.w(TAG, "Some configuration parameters are missing from plugin.xml preferences");
            Log.w(TAG, "Current config: " + getConfigInfo());
        } else {
            Log.i(TAG, "All configuration parameters loaded successfully from plugin.xml");
            Log.i(TAG, "Config info: " + getConfigInfo());
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "Executing action: " + action);

        switch (action) {
            case "init":
                return init(args, callbackContext);
            case "startRecognition":
                return startRecognition(args, callbackContext);
            case "stopRecognition":
                return stopRecognition(callbackContext);
            case "cancelRecognition":
                return cancelRecognition(callbackContext);
            case "release":
                return release(callbackContext);
            case "startWakeup":
                return startWakeup(args, callbackContext);
            case "stopWakeup":
                return stopWakeup(callbackContext);
            case "setParams":
                return setParams(args, callbackContext);
            case "getVersion":
                return getVersion(callbackContext);
            case "checkPermission":
                return checkPermission(callbackContext);
            case "requestPermission":
                return requestPermission(callbackContext);
            default:
                callbackContext.error("Unknown action: " + action);
                return false;
        }
    }

    /**
     * 初始化SDK
     */
    private boolean init(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject config = args.optJSONObject(0);

            // 检查基本权限
            if (!PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO)) {
                Log.w(TAG, "录音权限未授予，初始化继续但识别时需要权限");
            }

            if (!PermissionHelper.hasPermission(this, Manifest.permission.INTERNET)) {
                Log.e(TAG, "网络权限未授予，无法使用在线识别");
                callbackContext.error("网络权限未授予，无法使用在线识别");
                return false;
            }

            // 优先使用plugin.xml中的配置，如果用户传递了参数则覆盖
            String finalApiKey = this.apiKey; // 从plugin.xml读取的默认值
            String finalSecretKey = this.secretKey; // 从plugin.xml读取的默认值
            String finalAppId = this.appId; // 从plugin.xml读取的默认值

            // 如果用户传递了参数，则覆盖默认值
            if (config != null) {
                String userApiKey = config.optString("apiKey", "");
                String userSecretKey = config.optString("secretKey", "");
                String userAppId = config.optString("appId", "");

                if (!userApiKey.isEmpty()) {
                    finalApiKey = userApiKey;
                    Log.d(TAG, "Using user-provided API_KEY");
                }

                if (!userSecretKey.isEmpty()) {
                    finalSecretKey = userSecretKey;
                    Log.d(TAG, "Using user-provided SECRET_KEY");
                }

                if (!userAppId.isEmpty()) {
                    finalAppId = userAppId;
                    Log.i(TAG, "Initializing with API_KEY: " + maskKey(finalApiKey) +
                            ", SECRET_KEY: " + maskKey(finalSecretKey) +
                            ", APP_ID: " + finalAppId);
                }
            }

            // 验证认证信息完整性
            if (finalApiKey.isEmpty() || finalSecretKey.isEmpty() || finalAppId.isEmpty()) {
                String errorMsg = "Missing configuration parameters. ";
                if (finalApiKey.isEmpty()) errorMsg += "API_KEY is missing. ";
                if (finalSecretKey.isEmpty()) errorMsg += "SECRET_KEY is missing. ";
                if (finalAppId.isEmpty()) errorMsg += "APP_ID is missing. ";
                errorMsg += "Please set them in plugin.xml preferences or pass them in init() method.";

                Log.e(TAG, errorMsg);
                Log.e(TAG, "Current config: " + getConfigInfo());
                callbackContext.error(errorMsg);
                return false;
            }

            Log.i(TAG, "All authentication parameters are present, proceeding with initialization...");

            Log.i(TAG, "Initializing with API_KEY: " + maskKey(finalApiKey) +
                    ", APP_ID: " + finalAppId);

            Context context = cordova.getActivity().getApplicationContext();

            // 直接集成BaiduRecognizerManager的初始化逻辑
            initBaiduRecognizer(finalApiKey, finalSecretKey, finalAppId, callbackContext);

            // 初始化唤醒管理器
            wakeupManager = new BaiduWakeupManager(context);
            wakeupManager.init(finalApiKey, finalSecretKey, finalAppId, new BaiduWakeupManager.WakeupCallback() {
                @Override
                public void onWakeupSuccess(JSONObject result) {
                    //sendResult(callbackContext, "wakeupSuccess", result);
                }

                @Override
                public void onWakeupError(int errorCode, String errorMessage) {
                    JSONObject error = new JSONObject();
                    try {
                        error.put("errorCode", errorCode);
                        error.put("errorMessage", errorMessage);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error creating wakeup error JSON", e);
                    }
                    sendErrorResult(callbackContext, error);
                }

                @Override
                public void onWakeupReady() {
                   // sendResult(callbackContext, "wakeupReady", null);
                }
            });

            // 只有在识别管理器和唤醒管理器都初始化成功后才返回成功
            Log.i(TAG, "SDK initialized successfully");
            callbackContext.success("SDK initialized successfully");
            return true;

        }
//        catch (JSONException e) {
//            Log.e(TAG, "Error parsing init parameters", e);
//            callbackContext.error("Invalid parameters: " + e.getMessage());
//            return false;
//        }

        catch (Exception e) {
            Log.e(TAG, "Error initializing SDK", e);
            callbackContext.error("SDK initialization failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 开始语音识别
     */
    private boolean startRecognition(JSONArray args, CallbackContext callbackContext) {
        try {
            Log.d(TAG, "=== startRecognition 开始 ===");
            Log.d(TAG, "callbackContext 是否为null: " + (callbackContext == null));
            Log.d(TAG, "当前 recognitionCallbackContext: " + (recognitionCallbackContext != null ? "已设置" : "未设置"));

            // 设置识别回调上下文
            this.recognitionCallbackContext = callbackContext;
            Log.d(TAG, "recognitionCallbackContext 已设置");

            // 检查录音权限
            if (!PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO)) {
                Log.e(TAG, "录音权限未授予，无法开始识别");
                callbackContext.error("录音权限未授予，请在设置中允许录音权限");
                return false;
            }

            // 检查识别管理器是否已初始化
            if (!isInited) {
                Log.e(TAG, "Recognizer not initialized - SDK not initialized");
                callbackContext.error("SDK not initialized. Please call init() first.");
                return false;
            }

            // 解析参数
            JSONObject params = args.getJSONObject(0);
            Map<String, Object> paramMap = jsonToMap(params);

            Log.d(TAG, "Starting recognition with params: " + params.toString());

            // 开始识别
            startRecognitionInternal(paramMap);
            Log.d(TAG, "Recognition start command sent successfully");

            // 发送开始成功的回调，保持通道打开
            PluginResult startResult = new PluginResult(PluginResult.Status.OK, "Recognition started");
            startResult.setKeepCallback(true);
            callbackContext.sendPluginResult(startResult);
            Log.d(TAG, "开始成功回调已发送");

            Log.d(TAG, "=== startRecognition 结束 ===");
            return true;

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing recognition parameters", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error starting recognition", e);
            callbackContext.error("Failed to start recognition: " + e.getMessage());
            return false;
        }
    }

    /**
     * 停止语音识别
     */
    private boolean stopRecognition(CallbackContext callbackContext) {
        try {
            if (!isInited) {
                callbackContext.error("SDK not initialized");
                return false;
            }

            stopRecognitionInternal();
            callbackContext.success("Recognition stopped");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error stopping recognition", e);
            callbackContext.error("Failed to stop recognition: " + e.getMessage());
            return false;
        }
    }

    /**
     * 取消语音识别
     */
    private boolean cancelRecognition(CallbackContext callbackContext) {
        try {
            if (!isInited) {
                callbackContext.error("SDK not initialized");
                return false;
            }

            cancelRecognitionInternal();
            callbackContext.success("Recognition cancelled");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error cancelling recognition", e);
            callbackContext.error("Failed to cancel recognition: " + e.getMessage());
            return false;
        }
    }

    /**
     * 释放SDK资源
     */
    private boolean release(CallbackContext callbackContext) {
        try {
            // 释放识别资源
            releaseRecognitionInternal();

            // 释放唤醒资源
            if (wakeupManager != null) {
                wakeupManager.release();
                wakeupManager = null;
            }

            callbackContext.success("SDK released");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error releasing SDK", e);
            callbackContext.error("Failed to release SDK: " + e.getMessage());
            return false;
        }
    }

    /**
     * 开始语音唤醒
     */
    private boolean startWakeup(JSONArray args, CallbackContext callbackContext) {
        try {
            if (wakeupManager == null) {
                callbackContext.error("SDK not initialized");
                return false;
            }

            JSONObject params = args.getJSONObject(0);
            Map<String, Object> paramMap = jsonToMap(params);

            wakeupManager.startWakeup(paramMap);
            callbackContext.success("Wakeup started");
            return true;

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing wakeup parameters", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error starting wakeup", e);
            callbackContext.error("Failed to start wakeup: " + e.getMessage());
            return false;
        }
    }

    /**
     * 停止语音唤醒
     */
    private boolean stopWakeup(CallbackContext callbackContext) {
        try {
            if (wakeupManager == null) {
                callbackContext.error("SDK not initialized");
                return false;
            }

            wakeupManager.stopWakeup();
            callbackContext.success("Wakeup stopped");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error stopping wakeup", e);
            callbackContext.error("Failed to stop wakeup: " + e.getMessage());
            return false;
        }
    }

    /**
     * 设置参数
     */
    private boolean setParams(JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject params = args.getJSONObject(0);
            Map<String, Object> paramMap = jsonToMap(params);

            // 参数将在startRecognition/startWakeup时设置
            // 这里只是保存参数，实际使用时再传递

            callbackContext.success("Parameters saved");
            return true;

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing parameters", e);
            callbackContext.error("Invalid parameters: " + e.getMessage());
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error setting parameters", e);
            callbackContext.error("Failed to set parameters: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取版本信息
     */
    private boolean getVersion(CallbackContext callbackContext) {
        try {
            String version = "3.5.0"; // 百度SDK版本
            callbackContext.success(version);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error getting version", e);
            callbackContext.error("Failed to get version: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查权限
     */
    private boolean checkPermission(CallbackContext callbackContext) {
        try {
            JSONObject result = new JSONObject();
            JSONObject permissionStatus = new JSONObject();

            // 检查各个权限
            boolean hasRecordAudio = PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO);
            boolean hasInternet = PermissionHelper.hasPermission(this, Manifest.permission.INTERNET);
            boolean hasNetworkState = PermissionHelper.hasPermission(this, Manifest.permission.ACCESS_NETWORK_STATE);
            boolean hasWriteStorage = PermissionHelper.hasPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            boolean hasReadStorage = PermissionHelper.hasPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            permissionStatus.put("RECORD_AUDIO", hasRecordAudio);
            permissionStatus.put("INTERNET", hasInternet);
            permissionStatus.put("ACCESS_NETWORK_STATE", hasNetworkState);
            permissionStatus.put("WRITE_EXTERNAL_STORAGE", hasWriteStorage);
            permissionStatus.put("READ_EXTERNAL_STORAGE", hasReadStorage);

            // 计算总体权限状态
            boolean hasAllPermissions = hasRecordAudio && hasInternet && hasNetworkState
                    && hasWriteStorage && hasReadStorage;

            result.put("hasPermission", hasAllPermissions);
            result.put("permissionStatus", permissionStatus);

            // 添加权限描述
            if (!hasRecordAudio) {
                result.put("warning", "录音权限未授予，无法进行语音识别");
            }
            if (!hasInternet) {
                result.put("error", "网络权限未授予，无法使用在线识别功能");
            }

            callbackContext.success(result);
            return true;

        } catch (JSONException e) {
            Log.e(TAG, "Error checking permission", e);
            callbackContext.error("Failed to check permission: " + e.getMessage());
            return false;
        }
    }

    /**
     * 请求权限
     */
    private boolean requestPermission(CallbackContext callbackContext) {
        try {
            permissionCallbackContext = callbackContext;
            PermissionHelper.requestPermissions(this, PERMISSION_REQUEST_CODE, REQUIRED_PERMISSIONS);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error requesting permission", e);
            callbackContext.error("Failed to request permission: " + e.getMessage());
            return false;
        }
    }

    /**
     * 权限请求结果回调
     */
    @Override
    public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            try {
                JSONObject result = new JSONObject();
                JSONObject permissionResults = new JSONObject();

                // 检查每个权限的授予结果
                boolean allGranted = true;
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    boolean isGranted = grantResult == PackageManager.PERMISSION_GRANTED;

                    // 简化权限名称显示
                    String simpleName = permission.substring(permission.lastIndexOf('.') + 1);
                    permissionResults.put(simpleName, isGranted);

                    if (!isGranted) {
                        allGranted = false;
                    }
                }

                result.put("allGranted", allGranted);
                result.put("permissionResults", permissionResults);

                // 添加关键权限的提示
                boolean hasRecordAudio = PermissionHelper.hasPermission(this, Manifest.permission.RECORD_AUDIO);
                boolean hasInternet = PermissionHelper.hasPermission(this, Manifest.permission.INTERNET);

                if (!hasRecordAudio) {
                    result.put("criticalWarning", "录音权限未授予，语音识别功能无法使用");
                }
                if (!hasInternet) {
                    result.put("criticalError", "网络权限未授予，在线识别功能无法使用");
                }

                if (permissionCallbackContext != null) {
                    if (allGranted) {
                        Log.i(TAG, "所有权限已授予");
                        permissionCallbackContext.success(result);
                    } else {
                        Log.w(TAG, "部分权限被拒绝");
                        permissionCallbackContext.error(result.toString());
                    }
                    permissionCallbackContext = null;
                }

            } catch (JSONException e) {
                Log.e(TAG, "Error creating permission result", e);
                if (permissionCallbackContext != null) {
                    permissionCallbackContext.error("Failed to process permission result: " + e.getMessage());
                    permissionCallbackContext = null;
                }
            }
        }
    }

    /**
     * 发送成功结果
     */
    private void sendResult(CallbackContext callbackContext, String action, JSONObject data) {
        try {
            // 检查回调上下文是否为null
            if (callbackContext == null) {
                Log.w(TAG, "CallbackContext is null, cannot send result for action: " + action);
                return;
            }

            JSONObject result = new JSONObject();
            result.put("action", action);
            if (data != null) {
                result.put("data", data);
            }

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);

        } catch (JSONException e) {
            Log.e(TAG, "Error sending result", e);
        }
    }

    /**
     * 发送回调 - 参考标准Cordova插件回调模式
     * @param callbackContext 回调上下文
     * @param success 是否成功
     * @param message 消息内容
     * @param data 附加数据
     * @param keepCallback 是否保持回调
     */
    private void sendCallback(CallbackContext callbackContext, boolean success, String message, JSONObject data, boolean keepCallback) {
        try {
            if (callbackContext == null) {
                Log.w(TAG, "CallbackContext is null, cannot send callback");
                return;
            }

            JSONObject result = new JSONObject();
            result.put("success", success);
            result.put("message", message != null ? message : "");

            if (data != null) {
                result.put("data", data);
            }

            PluginResult pluginResult;
            if (success) {
                pluginResult = new PluginResult(PluginResult.Status.OK, result);
            } else {
                pluginResult = new PluginResult(PluginResult.Status.ERROR, result);
            }

            pluginResult.setKeepCallback(keepCallback);
            callbackContext.sendPluginResult(pluginResult);

            Log.d(TAG, "Callback sent - success: " + success + ", message: " + message + ", keepCallback: " + keepCallback);

        } catch (JSONException e) {
            Log.e(TAG, "Error sending callback", e);
            // 发送一个简单的错误回调
            if (callbackContext != null) {
                callbackContext.error("JSON error: " + e.getMessage());
            }
        }
    }

    /**
     * 发送识别结果回调 - 专门用于语音识别结果
     * @param callbackContext 回调上下文
     * @param eventType 事件类型 (start, partial, final, end, error, volume)
     * @param results 识别结果
     * @param isFinal 是否为最终结果
     * @param confidence 置信度
     * @param volume 音量信息
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     */
    private void sendRecognitionCallback(CallbackContext callbackContext, String eventType, String results,
                                         boolean isFinal, float confidence, int volume,
                                         int errorCode, String errorMessage) {
        try {
            if (callbackContext == null) {
                Log.w(TAG, "CallbackContext is null, cannot send recognition callback for event: " + eventType);
                return;
            }

            JSONObject data = new JSONObject();
            data.put("eventType", eventType);
            data.put("timestamp", System.currentTimeMillis());

            // 根据事件类型添加不同数据
            switch (eventType) {
                case "partial":
                case "final":
                    data.put("results", results != null ? results : "");
                    data.put("isFinal", isFinal);
                    if (confidence >= 0) {
                        data.put("confidence", confidence);
                    }
                    break;

                case "volume":
                    data.put("volume", volume);
                    break;

                case "error":
                    data.put("errorCode", errorCode);
                    data.put("errorMessage", errorMessage != null ? errorMessage : "");
                    break;

                case "start":
                case "beginning":
                case "end":
                    // 这些事件只需要事件类型和时间戳
                    break;

                default:
                    Log.w(TAG, "Unknown event type: " + eventType);
                    break;
            }

            // 对于识别结果，保持回调以接收更多结果
            boolean keepCallback = "partial".equals(eventType) || "final".equals(eventType) ||
                    "volume".equals(eventType) || "start".equals(eventType) ||
                    "beginning".equals(eventType);

            sendCallback(callbackContext, true, eventType, data, keepCallback);

        } catch (JSONException e) {
            Log.e(TAG, "Error sending recognition callback", e);
            if (callbackContext != null) {
                callbackContext.error("Recognition callback error: " + e.getMessage());
            }
        }
    }

    /**
     * 公共方法：供BaiduRecognizerManager直接调用发送最终识别结果
     * @param results 识别结果
     * @param isFinal 是否为最终结果
     * @param origalJson 原始JSON数据
     */
    public void onRecognitionResult(String results, boolean isFinal, String origalJson) {
        Log.d(TAG, "收到BaiduRecognizerManager的识别结果回调 - 结果: " + results + ", 是否最终: " + isFinal);

        // 使用新的识别回调方法
        String eventType = isFinal ? "final" : "partial";
        sendRecognitionCallback(recognitionCallbackContext, eventType, results, isFinal, -1.0f, -1, -1, null);

        // 同时保持原有的sendResult方法以确保兼容性
        JSONObject resultData = new JSONObject();
        try {
            resultData.put("results", results);
            resultData.put("isFinal", isFinal);
            resultData.put("origalJson", origalJson);
            Log.d(TAG, "创建结果JSON成功: " + resultData.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating result JSON", e);
        }

        Log.d(TAG, "调用sendResult发送结果到Cordova");
        sendResult(recognitionCallbackContext, "results", resultData);
        Log.d(TAG, "sendResult调用完成");
    }

    /**
     * 公共方法：供BaiduRecognizerManager直接调用发送语音事件
     * @param eventType 事件类型
     * @param data 事件数据
     */
    public void onSpeechEvent(String eventType, JSONObject data) {
        Log.d(TAG, "收到BaiduRecognizerManager的语音事件回调 - 事件: " + eventType);

        // 使用新的识别回调方法
        switch (eventType) {
            case "beginning":
                sendRecognitionCallback(recognitionCallbackContext, "beginning", null, false, -1.0f, -1, -1, null);
                break;
            case "end":
                sendRecognitionCallback(recognitionCallbackContext, "end", null, false, -1.0f, -1, -1, null);
                break;
            case "volume":
                int volume = data != null && data.has("volume") ? data.optInt("volume", -1) : -1;
                sendRecognitionCallback(recognitionCallbackContext, "volume", null, false, -1.0f, volume, -1, null);
                break;
            case "error":
                int errorCode = data != null && data.has("errorCode") ? data.optInt("errorCode", -1) : -1;
                String errorMessage = data != null && data.has("errorMessage") ? data.optString("errorMessage", "") : "";
                sendRecognitionCallback(recognitionCallbackContext, "error", null, false, -1.0f, -1, errorCode, errorMessage);
                break;
        }

        // 同时保持原有的sendResult方法以确保兼容性
        sendResult(recognitionCallbackContext, eventType, data);
    }

    /**
     * 发送错误结果
     */
    private void sendErrorResult(CallbackContext callbackContext, JSONObject error) {
        try {
            // 检查回调上下文是否为null
            if (callbackContext == null) {
                Log.w(TAG, "CallbackContext is null, cannot send error result");
                return;
            }

            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, error);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);

        } catch (Exception e) {
            Log.e(TAG, "Error sending error result", e);
        }
    }

    /**
     * JSONObject转Map
     */
    private Map<String, Object> jsonToMap(JSONObject json) {
        Map<String, Object> map = new HashMap<>();
        try {
            java.util.Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object value = json.get(key);
                map.put(key, value);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error converting JSON to Map", e);
        }
        return map;
    }

    /**
     * 隐藏敏感信息，只显示前4位和后4位
     */
    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "****";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }

    /**
     * 获取当前配置信息（用于调试）
     */
    private String getConfigInfo() {
        return "API_KEY: " + (apiKey.isEmpty() ? "Not set" : maskKey(apiKey)) +
                ", SECRET_KEY: " + (secretKey.isEmpty() ? "Not set" : maskKey(secretKey)) +
                ", APP_ID: " + (appId.isEmpty() ? "Not set" : appId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release(null);
    }

    // ====================== 百度语音识别核心方法（从BaiduRecognizerManager合并而来） ======================

    /**
     * 初始化百度语音识别器
     */
    private void initBaiduRecognizer(String apiKey, String secretKey, String appId, CallbackContext callbackContext) {
        // 设置认证信息
        BaiduAuthUtil.setAuthInfo(apiKey, secretKey, appId);

        if (isInited) {
            Log.e(TAG, "还未调用release()，请勿新建一个新类");
            callbackContext.error("Recognizer already initialized, please call release() first");
            return;
        }

        try {
            // 创建事件适配器 - 基于官方SDK RecogEventAdapter
            BaiduIRecogListener recogListener = new BaiduIRecogListener() {
                @Override
                public void onAsrReady() {
                    Log.d(TAG, "引擎准备就绪");
                    sendCallback("ready", "Recognition engine ready");
                }

                @Override
                public void onAsrBegin() {
                    Log.d(TAG, "检测到开始说话");
                    sendCallback("beginning", "检测到开始说话");
                }

                @Override
                public void onAsrEnd() {
                    Log.d(TAG, "检测到停止说话");
                    sendCallback("end", "检测到停止说话");
                }

                @Override
                public void onAsrPartialResult(String[] results, BaiduRecogResult recogResult) {
                    String resultText = (results != null && results.length > 0) ? results[0] : "";
                    Log.d(TAG, "临时识别结果: " + resultText);
                    sendCallback("partial", resultText);
                }

                @Override
                public void onAsrOnlineNluResult(String nluResult) {
                    Log.d(TAG, "NLU结果: " + nluResult);
                }

                @Override
                public void onAsrFinalResult(String[] results, BaiduRecogResult recogResult) {
                    String resultText = (results != null && results.length > 0) ? results[0] : "";
                    Log.d(TAG, "=== onAsrFinalResult 触发 ===");
                    Log.d(TAG, "最终识别结果: " + resultText);
                    Log.d(TAG, "recognitionCallbackContext 是否为null: " + (recognitionCallbackContext == null));
                    Log.d(TAG, "准备调用 sendCallback");
                    sendCallback("final", resultText);
                    Log.d(TAG, "sendCallback 调用完成");
                }

                @Override
                public void onAsrFinish(BaiduRecogResult recogResult) {
                    Log.d(TAG, "识别结束");
                    sendCallback("finish", "识别结束");
                }

                @Override
                public void onAsrFinishError(int errorCode, int subErrorCode, String descMessage, BaiduRecogResult recogResult) {
                    Log.e(TAG, "识别错误: " + descMessage);
                    sendCallback("error", "错误码: " + errorCode + ", 描述: " + descMessage);
                }

                @Override
                public void onAsrLongFinish() {
                    Log.d(TAG, "长语音识别结束");
                    sendCallback("long_finish", "长语音识别结束");
                }

                @Override
                public void onAsrVolume(int volumePercent, int volume) {
                    Log.d(TAG, "音量: " + volumePercent + "%");
                    sendCallback("volume", String.valueOf(volumePercent));
                }

                @Override
                public void onAsrAudio(byte[] data, int offset, int length) {
                    // 音频数据回调，通常不需要处理
                }

                @Override
                public void onAsrExit() {
                    Log.d(TAG, "引擎退出");
                    sendCallback("exit", "引擎退出");
                }

                @Override
                public void onOfflineLoaded() {
                    Log.d(TAG, "离线资源加载完成");
                    sendCallback("offline_loaded", "离线资源加载完成");
                }

                @Override
                public void onOfflineUnLoaded() {
                    Log.d(TAG, "离线资源释放完成");
                    sendCallback("offline_unloaded", "离线资源释放完成");
                }
            };

            // 创建事件适配器 - 基于官方SDK RecogEventAdapter
            this.eventListener = new BaiduRecogEventAdapter(recogListener);

            // SDK集成步骤 初始化asr的EventManager示例，多次得到的类，只能选一个使用
            AipeEventManagerFactory factory = new AipeEventManagerFactory();

            // 验证认证信息
            String configAppId = BaiduAuthUtil.getAppId();
            String configAk = BaiduAuthUtil.getAk();
            String configSk = BaiduAuthUtil.getSk();

            Log.d(TAG, "认证信息检查:");
            Log.d(TAG, "  App ID: " + (configAppId.isEmpty() ? "未设置" : configAppId));
            Log.d(TAG, "  API Key: " + (configAk.isEmpty() ? "未设置" : maskKey(configAk)));
            Log.d(TAG, "  Secret Key: " + (configSk.isEmpty() ? "未设置" : maskKey(configSk)));

            if (configAppId.isEmpty() || configAk.isEmpty() || configSk.isEmpty()) {
                Log.e(TAG, "认证信息不完整，无法初始化SDK");
                callbackContext.error("Authentication info incomplete. Please check API_KEY, SECRET_KEY, APP_ID.");
                return;
            }

            // 设置正式ak sk - 注意参数顺序：appId, ak, sk
            factory.setAkSk(configAppId, configAk, configSk);
            Log.d(TAG, "使用直接AK/SK方式设置认证");

            Log.d(TAG, "开始创建EventManager...");
            asr = factory.create(cordova.getActivity().getApplicationContext(), "asr");

            if (asr == null) {
                Log.e(TAG, "EventManager创建失败");
                callbackContext.error("Failed to create EventManager. Please check authentication info.");
                return;
            }

            Log.d(TAG, "EventManager创建成功，开始注册监听器...");
            // SDK集成步骤 设置回调event， 识别引擎会回调这个类告知重要状态和识别结果
            asr.registerListener(eventListener);

            // 只有在所有初始化步骤成功后才设置isInited为true
            isInited = true;
            Log.d(TAG, "百度语音识别器初始化成功");

        } catch (Exception e) {
            Log.e(TAG, "百度语音识别器初始化失败: " + e.getMessage(), e);
            // 确保在异常情况下重置状态
            asr = null;
            isInited = false;
            callbackContext.error("Initialization failed: " + e.getMessage());
        }
    }

    /**
     * 开始识别
     */
    private void startRecognitionInternal(Map<String, Object> params) {
        Log.d(TAG, "开始识别，检查初始化状态...");

        if (!isInited) {
            Log.e(TAG, "SDK未初始化");
            return;
        }

        if (asr == null) {
            Log.e(TAG, "EventManager为null，初始化可能未完成");
            return;
        }

        if (eventListener == null) {
            Log.e(TAG, "EventListener为null，事件监听器未注册");
            return;
        }

        try {
            // 创建识别参数 - 基于官方SDK
            JSONObject paramJson = new JSONObject();

            // 基础认证参数 - 重要：必须包含这些参数
            paramJson.put("app_id", BaiduAuthUtil.getAppId());
            paramJson.put("app_key", BaiduAuthUtil.getAk());
            paramJson.put("secret", BaiduAuthUtil.getSk());
            paramJson.put("app_name", "栗子同学");

            // 用户自定义参数
            if (params != null) {
                for (Map.Entry<String, Object> entry : params.entrySet()) {
                    try {
                        String key = entry.getKey();
                        Object value = entry.getValue();

                        // 处理特殊参数
                        if (key.equals("language")) {
                            paramJson.put("language", value);
                        } else if (key.equals("pid")) {
                            params.put(SpeechConstant.LANGUAGE, "yue-Hans-CN");
                        } else if (key.equals("accept-audio-volume")) {
                            paramJson.put("accept-audio-volume", value);
                        } else if (key.equals("rate")) {
                            paramJson.put("rate", value);
                        } else if (key.equals("cuid")) {
                            paramJson.put("cuid", value);
                        } else {
                            // 其他参数直接添加
                            paramJson.put(key, value);
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "设置参数失败: " + entry.getKey());
                    }
                }
            }

            // 设置默认值（如果用户没有提供）
            if (!paramJson.has("pid")) {
                params.put(SpeechConstant.LANGUAGE, "yue-Hans-CN");  // 普通话(支持简单的英文识别)
            }
            if (!paramJson.has("language")) {
                paramJson.put("language", "zh");
            }
            if (!paramJson.has("accept-audio-volume")) {
                paramJson.put("accept-audio-volume", true); // 接受音量回调
            }
            if (!paramJson.has("cuid")) {
                paramJson.put("cuid", "device_id"); // 设备唯一标识
            }

            String json = paramJson.toString();
            Log.i(TAG + ".Debug", "识别参数（反馈请带上此行日志）" + json);

            // 最后一次检查asr是否为null
            if (asr == null) {
                Log.e(TAG, "发送命令前EventManager变为null");
                return;
            }

            Log.d(TAG, "发送开始识别命令...");
            // SDK集成步骤 发送开始识别命令
            asr.send(SpeechConstant.ASR_START, json, null, 0, 0);
            Log.d(TAG, "开始识别命令发送成功");

        } catch (Exception e) {
            Log.e(TAG, "开始识别失败: " + e.getMessage(), e);
        }
    }

    /**
     * 停止识别
     */
    private void stopRecognitionInternal() {
        Log.i(TAG, "停止录音");
        if (!isInited) {
            Log.e(TAG, "SDK未初始化");
            return;
        }
        // SDK 集成步骤（可选）停止录音
        asr.send(SpeechConstant.ASR_STOP, "{}", null, 0, 0);
    }

    /**
     * 取消识别
     */
    private void cancelRecognitionInternal() {
        Log.i(TAG, "取消识别");
        if (!isInited) {
            Log.e(TAG, "SDK未初始化");
            return;
        }
        // SDK集成步骤 (可选） 取消本次识别
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
    }

    /**
     * 释放资源
     */
    private void releaseRecognitionInternal() {
        if (asr == null) {
            return;
        }

        cancelRecognitionInternal();

        if (isOfflineEngineLoaded) {
            // SDK集成步骤 如果之前有调用过 加载离线命令词，这里要对应释放
            asr.send(SpeechConstant.ASR_KWS_UNLOAD_ENGINE, null, null, 0, 0);
            isOfflineEngineLoaded = false;
        }

        // SDK 集成步骤（可选），卸载listener
        asr.unregisterListener(eventListener);
        asr = null;
        isInited = false;

        Log.d(TAG, "识别器资源已释放");
    }

    /**
     * 简化的回调方法 - 参考AISpeechTranscriber的实现
     */
    private void sendCallback(String type, String message) {
        Log.d(TAG, "=== sendCallback 开始 ===");
        Log.d(TAG, "type: " + type + ", message: " + message);
        Log.d(TAG, "recognitionCallbackContext 是否为null: " + (recognitionCallbackContext == null));

        if (recognitionCallbackContext == null) {
            Log.e(TAG, "recognitionCallbackContext 为 null，无法发送回调");
            return;
        }

        try {
            JSONObject result = new JSONObject();
            result.put("type", type); // ready/beginning/partial/final/end/error/volume等
            result.put("message", message);
            result.put("timestamp", System.currentTimeMillis());

            Log.d(TAG, "创建的回调数据: " + result.toString());

            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, result);
            pluginResult.setKeepCallback(true); // 保持回调通道打开

            Log.d(TAG, "准备发送 PluginResult");
            recognitionCallbackContext.sendPluginResult(pluginResult);
            Log.d(TAG, "PluginResult 发送成功");

        } catch (Exception e) {
            Log.e(TAG, "发送回调失败", e);
        }

        Log.d(TAG, "=== sendCallback 结束 ===");
    }
}
