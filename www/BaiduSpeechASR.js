var exec = require('cordova/exec');

var BaiduSpeechASR = {
    /**
     * 初始化百度语音识别SDK
     * @param {Object} config - 配置参数
     * @param {string} config.apiKey - 百度API Key
     * @param {string} config.secretKey - 百度Secret Key  
     * @param {string} config.appId - 百度App ID
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    init: function (config, success, error) {
        exec(success, error, 'BaiduSpeechASR', 'init', [config]);
    },
    
    /**
     * 开始语音识别
     * @param {Object} params - 识别参数
     * @param {number} [params.pid=1537] - 识别模型ID，1537为普通话模型
     * @param {number} [params.rate=16000] - 采样率
     * @param {string} [params.language='zh'] - 语言
     * @param {string} [params.cuid=''] - 设备唯一标识
     * @param {Function} success - 结果回调（含中间/最终结果）
     * @param {Function} error - 错误回调
     */
    startRecognition: function (params, success, error) {
        exec(success, error, 'BaiduSpeechASR', 'startRecognition', [params || {}]);
    },
    
    /**
     * 停止语音识别
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    stopRecognition: function (success, error) {
        exec(success, error, 'BaiduSpeechASR', 'stopRecognition', []);
    },
    
    /**
     * 取消语音识别
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    cancelRecognition: function (success, error) {
        exec(success, error, 'BaiduSpeechASR', 'cancelRecognition', []);
    },
    
    /**
     * 释放SDK资源
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    release: function (success, error) {
        exec(success, error, 'BaiduSpeechASR', 'release', []);
    },
    
    /**
     * 开始语音唤醒
     * @param {Object} params - 唤醒参数
     * @param {string} [params.wakeupWords=''] - 唤醒词
     * @param {Function} success - 唤醒回调
     * @param {Function} error - 错误回调
     */
    startWakeup: function (params, success, error) {
        exec(success, error, 'BaiduSpeechASR', 'startWakeup', [params || {}]);
    },
    
    /**
     * 停止语音唤醒
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    stopWakeup: function (success, error) {
        exec(success, error, 'BaiduSpeechASR', 'stopWakeup', []);
    },
    
    /**
     * 设置识别参数
     * @param {Object} params - 参数配置
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    setParams: function (params, success, error) {
        exec(success, error, 'BaiduSpeechASR', 'setParams', [params]);
    },
    
    /**
     * 获取SDK版本信息
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    getVersion: function (success, error) {
        exec(success, error, 'BaiduSpeechASR', 'getVersion', []);
    },
    
    /**
     * 检查麦克风权限
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    checkPermission: function (success, error) {
        exec(success, error, 'BaiduSpeechASR', 'checkPermission', []);
    },
    
    /**
     * 请求麦克风权限
     * @param {Function} success - 成功回调
     * @param {Function} error - 失败回调
     */
    requestPermission: function (success, error) {
        exec(success, error, 'BaiduSpeechASR', 'requestPermission', []);
    }
};

module.exports = BaiduSpeechASR;
