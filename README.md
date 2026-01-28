# Cordova 百度语音识别插件

基于百度语音识别SDK V3.5.0的Cordova插件，提供语音识别和语音唤醒功能。

## 功能特性

- ✅ **语音识别** - 实时语音转文字
- ✅ **语音唤醒** - 关键词唤醒检测
- ✅ **在线识别** - 高精度云端识别
- ✅ **离线识别** - 本地命令词识别
- ✅ **多语言支持** - 中文、英文等
- ✅ **权限管理** - 自动处理麦克风权限
- ✅ **错误处理** - 完善的错误回调机制

## 安装

### 通过npm安装（推荐）
```bash
cordova plugin add cordova-plugin-baidu-speech-ASR \
  --variable API_KEY=your_api_key \
  --variable SECRET_KEY=your_secret_key \
  --variable APP_ID=your_app_id
```

### 通过本地安装
```bash
cordova plugin add /path/to/cordova-plugin-baidu-speech-ASR \
  --variable API_KEY=your_api_key \
  --variable SECRET_KEY=your_secret_key \
  --variable APP_ID=your_app_id
```

### 获取百度语音API密钥

1. 访问 [百度AI开放平台](https://ai.baidu.com/)
2. 创建应用并获取：
   - **API Key**
   - **Secret Key** 
   - **App ID**

(因为这些密钥是已经指定给当前特定的应用的包名,所以没有安全性问题)

## 支持平台

- ✅ Android (API 16+)
- ⏳ iOS 

## 使用方法

### 1. 初始化插件

```javascript
// 初始化SDK
BaiduSpeechASR.init({
    apiKey: 'your_api_key',
    secretKey: 'your_secret_key',
    appId: 'your_app_id'
}, function(success) {
    console.log('初始化成功:', success);
}, function(error) {
    console.error('初始化失败:', error);
});
```

### 2. 语音识别

```javascript
// 开始语音识别
BaiduSpeechASR.startRecognition({
    pid: 1537,        // 普通话模型
    rate: 16000,      // 采样率
    language: 'zh',   // 语言
    cuid: 'device_id' // 设备ID
}, function(result) {
    console.log('识别结果:', result);
    
    // 解析结果
    if (result.action === 'results') {
        const data = result.data;
        console.log('识别文字:', data.results);
        console.log('是否最终结果:', data.isFinal);
    }
}, function(error) {
    console.error('识别错误:', error);
});

// 停止识别
BaiduSpeechASR.stopRecognition(function(success) {
    console.log('识别已停止');
}, function(error) {
    console.error('停止失败:', error);
});

// 取消识别
BaiduSpeechASR.cancelRecognition(function(success) {
    console.log('识别已取消');
}, function(error) {
    console.error('取消失败:', error);
});
```

### 3. 语音唤醒

```javascript
// 开始语音唤醒
BaiduSpeechASR.startWakeup({
    wakeupWords: '小度小度'  // 唤醒词
}, function(result) {
    console.log('唤醒结果:', result);
    
    if (result.action === 'wakeupSuccess') {
        console.log('唤醒成功:', result.data.word);
    }
}, function(error) {
    console.error('唤醒错误:', error);
});

// 停止唤醒
BaiduSpeechASR.stopWakeup(function(success) {
    console.log('唤醒已停止');
}, function(error) {
    console.error('停止失败:', error);
});
```

### 4. 权限管理

```javascript
// 检查权限
BaiduSpeechASR.checkPermission(function(result) {
    console.log('权限状态:', result.hasPermission);
}, function(error) {
    console.error('检查权限失败:', error);
});

// 请求权限
BaiduSpeechASR.requestPermission(function(success) {
    console.log('权限获取成功:', success);
}, function(error) {
    console.error('权限获取失败:', error);
});
```

### 5. 完整示例

```javascript
class SpeechRecognitionApp {
    constructor() {
        this.isRecognizing = false;
        this.initSDK();
    }
    
    initSDK() {
        BaiduSpeechASR.init({
            apiKey: 'your_api_key',
            secretKey: 'your_secret_key',
            appId: 'your_app_id'
        }, (success) => {
            console.log('SDK初始化成功');
            this.checkPermissions();
        }, (error) => {
            console.error('SDK初始化失败:', error);
        });
    }
    
    checkPermissions() {
        BaiduSpeechASR.checkPermission((result) => {
            if (!result.hasPermission) {
                this.requestPermissions();
            } else {
                console.log('权限已获取');
            }
        });
    }
    
    requestPermissions() {
        BaiduSpeechASR.requestPermission((success) => {
            console.log('权限获取成功');
        }, (error) => {
            console.error('权限获取失败:', error);
        });
    }
    
    startRecognition() {
        if (this.isRecognizing) {
            console.log('正在识别中...');
            return;
        }
        
        this.isRecognizing = true;
        
        BaiduSpeechASR.startRecognition({
            pid: 1537,
            rate: 16000,
            language: 'zh'
        }, (result) => {
            this.handleRecognitionResult(result);
        }, (error) => {
            console.error('识别错误:', error);
            this.isRecognizing = false;
        });
    }
    
    handleRecognitionResult(result) {
        switch (result.action) {
            case 'ready':
                console.log('识别准备就绪');
                break;
            case 'beginningOfSpeech':
                console.log('开始说话');
                break;
            case 'endOfSpeech':
                console.log('停止说话');
                break;
            case 'results':
                const data = result.data;
                console.log('识别结果:', data.results);
                
                if (data.isFinal) {
                    console.log('最终结果:', data.results);
                    this.isRecognizing = false;
                }
                break;
            case 'event':
                console.log('事件:', result.data);
                break;
        }
    }
    
    stopRecognition() {
        BaiduSpeechASR.stopRecognition((success) => {
            console.log('识别已停止');
            this.isRecognizing = false;
        }, (error) => {
            console.error('停止失败:', error);
        });
    }
}

// 使用示例
const app = new SpeechRecognitionApp();

// 开始识别
app.startRecognition();

// 停止识别
// app.stopRecognition();
```

## API 参考

### BaiduSpeechASR.init(config, success, error)

初始化百度语音识别SDK。

**参数：**
- `config` (Object): 配置对象
  - `apiKey` (string): 百度API Key
  - `secretKey` (string): 百度Secret Key
  - `appId` (string): 百度App ID
- `success` (function): 成功回调
- `error` (function): 失败回调

### BaiduSpeechASR.startRecognition(params, success, error)

开始语音识别。

**参数：**
- `params` (Object): 识别参数
  - `pid` (number): 识别模型ID，默认1537（普通话）
  - `rate` (number): 采样率，默认16000
  - `language` (string): 语言，默认'zh'
  - `cuid` (string): 设备唯一标识
- `success` (function): 结果回调
- `error` (function): 错误回调

### BaiduSpeechASR.stopRecognition(success, error)

停止语音识别。

### BaiduSpeechASR.cancelRecognition(success, error)

取消语音识别。

### BaiduSpeechASR.startWakeup(params, success, error)

开始语音唤醒。

**参数：**
- `params` (Object): 唤醒参数
  - `wakeupWords` (string): 唤醒词，默认'小度小度'
- `success` (function): 唤醒回调
- `error` (function): 错误回调

### BaiduSpeechASR.stopWakeup(success, error)

停止语音唤醒。

### BaiduSpeechASR.release(success, error)

释放SDK资源。

### BaiduSpeechASR.checkPermission(success, error)

检查麦克风权限。

### BaiduSpeechASR.requestPermission(success, error)

请求麦克风权限。

## 识别模型ID (pid)

| pid | 模型 | 描述 |
|-----|------|------|
| 1537 | 普通话(支持简单的英文识别) | 普通话模型 |
| 1737 | 英语 | 英语模型 |
| 1637 | 粤语 | 粤语模型 |
| 1837 | 四川话 | 四川话模型 |

## 错误码

| 错误码 | 描述 |
|--------|------|
| 1001 | 网络连接错误 |
| 1002 | 网络请求超时 |
| 1003 | 音频格式错误 |
| 1004 | 参数错误 |
| 1005 | 权限被拒绝 |
| 2001 | SDK未初始化 |
| 2002 | 识别器正在运行 |
| 2003 | 识别器未运行 |

## 常见问题

### 1. 依赖冲突问题

如果遇到支付宝SDK或条码扫描SDK的重复类错误，请在主项目的 `app/build.gradle` 中添加以下配置：

```gradle
android {
    packagingOptions {
        pickFirst 'META-INF/DEPENDENCIES'
        pickFirst 'META-INF/LICENSE'
        pickFirst 'META-INF/LICENSE.txt'
        pickFirst 'META-INF/NOTICE'
        pickFirst 'META-INF/NOTICE.txt'
        pickFirst 'META-INF/INDEX.LIST'
        
        // 排除重复的META-INF文件
        exclude 'META-INF/AL2.0'
        exclude 'META-INF/LGPL2.1'
        exclude 'META-INF/LICENSE.md'
        exclude 'META-INF/NOTICE.md'
        exclude 'META-INF/maven/**'
        
        // 排除BuildConfig类
        exclude '**/BuildConfig.class'
        exclude '**/BuildConfig$*.class'
    }
    
    lintOptions {
        checkReleaseBuilds false
        abortOnError false
    }
}

configurations.all {
    resolutionStrategy {
        force 'com.google.zxing:core:3.3.3'
        force 'com.alipay.sdk:alipaysdk-android:15.8.00'
        force 'androidx.appcompat:appcompat:1.1.0'
        force 'androidx.core:core:1.1.0'
    }
}
```

### 2. 权限问题

确保在AndroidManifest.xml中添加了必要的权限：

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### 3. 网络问题

确保设备有网络连接，百度语音识别需要联网使用。

### 4. 音频问题

确保设备麦克风正常工作，并且没有被其他应用占用。

### 5. 配置问题

确保API Key、Secret Key、App ID正确配置。

## 开发环境要求

- Cordova 9.0+
- Android SDK API 16+
- Gradle 4.6+

## 许可证

MIT License

## 更新日志

### v1.0.0
- 初始版本发布
- 支持语音识别功能
- 支持语音唤醒功能
- 支持Android平台
- 支持IOS平台

