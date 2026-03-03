var exec = require(" cordova/exec\);

var BaiduSpeechASR = {
 /**
 * 初始化百度语音识别ASR SDK
 * @param {Object} config - 配置参数
 * @param {string} config.apiKey - 百度API Key
 * @param {string} config.secretKey - 百度Secret Key
 * @param {string} config.appId - 百度App ID
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 init: function (config, success, error) {
 exec(success, error, \BaiduSpeechASR\, \init\, [config]);
 },

 /**
 * 开始语音识别
 * @param {Object} params - 识别参数
 * @param {number} [params.pid=1537] - 识别模型ID，1537为普通话模型
 * @param {number} [params.rate=16000] - 采样率
 * @param {string} [params.language=\zh\] - 语言
 * @param {string} [params.cuid=\\] - 设备唯一标识
 * @param {Function} success - 结果回调（含中间/最终结果）
 * @param {Function} error - 错误回调
 */
 startRecognition: function (params, success, error) {
 exec(success, error, \BaiduSpeechASR\, \startRecognition\, [params || {}]);
 },

 /**
 * 停止语音识别
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 stopRecognition: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \stopRecognition\, []);
 },

 /**
 * 取消语音识别
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 cancelRecognition: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \cancelRecognition\, []);
 },

 /**
 * 释放SDK资源
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 release: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \release\, []);
 },

 /**
 * 开始语音唤醒
 * @param {Object} params - 唤醒参数
 * @param {string} [params.wakeupWords=\\] - 唤醒词
 * @param {Function} success - 唤醒回调
 * @param {Function} error - 错误回调
 */
 startWakeup: function (params, success, error) {
 exec(success, error, \BaiduSpeechASR\, \startWakeup\, [params || {}]);
 },

 /**
 * 停止语音唤醒
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 stopWakeup: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \stopWakeup\, []);
 },

 /**
 * 设置识别参数
 * @param {Object} params - 参数配置
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 setParams: function (params, success, error) {
 exec(success, error, \BaiduSpeechASR\, \setParams\, [params]);
 },

 /**
 * 获取SDK版本信息
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 getVersion: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \getVersion\, []);
 },

 /**
 * 检查录音权限
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 checkPermission: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \checkPermission\, []);
 },

 /**
 * 请求录音权限
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 requestPermission: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \requestPermission\, []);
 },

 /**
 * 初始化百度语音合成TTS SDK
 * @param {Object} config - 配置参数
 * @param {string} config.apiKey - 百度API Key
 * @param {string} config.secretKey - 百度Secret Key
 * @param {string} config.appId - 百度App ID
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 initTTS: function (config, success, error) {
 exec(success, error, \BaiduSpeechASR\, \initTTS\, [config]);
 },

 /**
 * 开始语音合成并播放
 * @param {Object} params - 合成参数
 * @param {string} params.text - 要合成的文本
 * @param {string} [params.speaker=\4\] - 发音人ID
 * @param {number} [params.speed=5] - 语速(1-15)
 * @param {number} [params.pitch=5] - 音调(1-15)
 * @param {number} [params.volume=5] - 音量(1-15)
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 speak: function (params, success, error) {
 exec(success, error, \BaiduSpeechASR\, \speak\, [params || {}]);
 },

 /**
 * 仅合成不播放
 * @param {Object} params - 合成参数
 * @param {string} params.text - 要合成的文本
 * @param {string} [params.speaker=\4\] - 发音人ID
 * @param {number} [params.speed=5] - 语速(1-15)
 * @param {number} [params.pitch=5] - 音调(1-15)
 * @param {number} [params.volume=5] - 音量(1-15)
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 synthesize: function (params, success, error) {
 exec(success, error, \BaiduSpeechASR\, \synthesize\, [params || {}]);
 },

 /**
 * 暂停语音播放
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 pauseTTS: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \pauseTTS\, []);
 },

 /**
 * 恢复语音播放
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 resumeTTS: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \resumeTTS\, []);
 },

 /**
 * 停止语音播放
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 stopTTS: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \stopTTS\, []);
 },

 /**
 * 设置TTS参数
 * @param {Object} params - 参数配置
 * @param {string} [params.speaker] - 发音人ID
 * @param {number} [params.speed] - 语速(1-15)
 * @param {number} [params.pitch] - 音调(1-15)
 * @param {number} [params.volume] - 音量(1-15)
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 setTTSParams: function (params, success, error) {
 exec(success, error, \BaiduSpeechASR\, \setTTSParams\, [params || {}]);
 },

 /**
 * 获取TTS状态
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 getTTSStatus: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \getTTSStatus\, []);
 },

 /**
 * 获取TTS版本信息
 * @param {Function} success - 成功回调
 * @param {Function} error - 错误回调
 */
 getTTSVersion: function (success, error) {
 exec(success, error, \BaiduSpeechASR\, \getTTSVersion\, []);
 }
};

module.exports = BaiduSpeechASR;
