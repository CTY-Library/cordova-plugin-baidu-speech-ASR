//
//  BaiduSpeechASR.m
//  cordova-plugin-baidu-speech-ASR
//
//  Created by CTY Library on 2026/01/28.
//  Copyright © 2026 CTY Library. All rights reserved.
//

#import "BaiduSpeechASR.h"
#import "BDSpeechBaseKit.h"
#import "BDSEventManager.h"
#import "BDSASRDefines.h"
#import "BDSASRParameters.h"
#import "BDSWakeupDefines.h"
#import "BDSWakeupParameters.h"

// 回调事件类型
static NSString * const EVENT_READY = @"ready";
static NSString * const EVENT_BEGINNING = @"beginning";
static NSString * const EVENT_PARTIAL = @"partial";
static NSString * const EVENT_FINAL = @"final";
static NSString * const EVENT_END = @"end";
static NSString * const EVENT_ERROR = @"error";
static NSString * const EVENT_VOLUME = @"volume";
static NSString * const EVENT_FINISH = @"finish";
static NSString * const EVENT_WAKEUP_SUCCESS = @"wakeup_success";
static NSString * const EVENT_WAKEUP_ERROR = @"wakeup_error";

@implementation BaiduSpeechASR

#pragma mark - 插件生命周期

- (void)pluginInitialize {
    [super pluginInitialize];
    
    NSLog(@"BaiduSpeechASR iOS plugin initializing...");
    
    // 从配置文件中读取参数
    self.apiKey = [self.commandDelegate.settings objectForKey:@"API_KEY"];
    self.secretKey = [self.commandDelegate.settings objectForKey:@"SECRET_KEY"];
    self.appId = [self.commandDelegate.settings objectForKey:@"APP_ID"];
    
    NSLog(@"BaiduSpeechASR config loaded - AppID: %@, APIKey: %@, SecretKey: %@", 
          self.appId, [self maskKey:self.apiKey], [self maskKey:self.secretKey]);
    
    // 检查权限
    [self checkPermissions];
}

- (void)dispose {
    [self releaseResources];
    [super dispose];
}

#pragma mark - Cordova 命令接口

/**
 * 初始化插件
 */
- (void)init:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR init called");
    
    // 获取参数
    NSDictionary *config = [command.arguments objectAtIndex:0];
    if (config && [config isKindOfClass:[NSDictionary class]]) {
        NSString *userApiKey = config[@"apiKey"];
        NSString *userSecretKey = config[@"secretKey"];
        NSString *userAppId = config[@"appId"];
        
        if (userApiKey && ![userApiKey isEqualToString:@""]) {
            self.apiKey = userApiKey;
        }
        if (userSecretKey && ![userSecretKey isEqualToString:@""]) {
            self.secretKey = userSecretKey;
        }
        if (userAppId && ![userAppId isEqualToString:@""]) {
            self.appId = userAppId;
        }
    }
    
    // 验证参数
    if (!self.apiKey || !self.secretKey || !self.appId || 
        [self.apiKey isEqualToString:@""] || 
        [self.secretKey isEqualToString:@""] || 
        [self.appId isEqualToString:@""]) {
        
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"Missing configuration parameters. Please set API_KEY, SECRET_KEY, APP_ID"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    // 检查权限
    if (![self checkPermissions]) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"Required permissions not granted"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    // 初始化百度SDK
    BOOL success = [self initializeBaiduSDK];
    if (success) {
        self.isInitialized = YES;
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                     messageAsString:@"SDK initialized successfully"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } else {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"SDK initialization failed"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

/**
 * 开始语音识别
 */
- (void)startRecognition:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR startRecognition called");
    
    if (!self.isInitialized) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"SDK not initialized. Please call init() first"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    if (self.isRecognizing) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"Recognition already in progress"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    // 保存回调ID
    self.recognitionCallbackId = command.callbackId;
    
    // 获取参数
    NSDictionary *params = [command.arguments objectAtIndex:0];
    
    // 开始识别
    BOOL success = [self startRecognitionWithParams:params];
    if (success) {
        self.isRecognizing = YES;
        
        // 发送开始成功的回调，保持通道打开
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                     messageAsString:@"Recognition started"];
        [result setKeepCallback:YES];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } else {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"Failed to start recognition"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

/**
 * 停止语音识别
 */
- (void)stopRecognition:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR stopRecognition called");
    
    if (!self.isInitialized) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"SDK not initialized"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    if (!self.isRecognizing) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"No recognition in progress"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    // 停止识别
    [self stopRecognitionInternal];
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                 messageAsString:@"Recognition stopped"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * 取消语音识别
 */
- (void)cancelRecognition:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR cancelRecognition called");
    
    if (!self.isInitialized) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"SDK not initialized"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    // 取消识别
    [self cancelRecognitionInternal];
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                 messageAsString:@"Recognition cancelled"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * 释放资源
 */
- (void)release:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR release called");
    
    [self releaseResources];
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                 messageAsString:@"SDK released"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * 开始语音唤醒
 */
- (void)startWakeup:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR startWakeup called");
    
    if (!self.isInitialized) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"SDK not initialized"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    if (self.isWakeupActive) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"Wakeup already active"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    // 保存回调ID
    self.wakeupCallbackId = command.callbackId;
    
    // 获取参数
    NSDictionary *params = [command.arguments objectAtIndex:0];
    
    // 开始唤醒
    BOOL success = [self startWakeupWithParams:params];
    if (success) {
        self.isWakeupActive = YES;
        
        // 发送开始成功的回调，保持通道打开
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                     messageAsString:@"Wakeup started"];
        [result setKeepCallback:YES];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } else {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"Failed to start wakeup"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

/**
 * 停止语音唤醒
 */
- (void)stopWakeup:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR stopWakeup called");
    
    if (!self.isInitialized) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR 
                                                     messageAsString:@"SDK not initialized"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }
    
    // 停止唤醒
    [self stopWakeupInternal];
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                 messageAsString:@"Wakeup stopped"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * 设置参数
 */
- (void)setParams:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR setParams called");
    
    // 参数将在startRecognition/startWakeup时设置
    // 这里只是保存参数，实际使用时再传递
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                 messageAsString:@"Parameters saved"];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * 获取版本信息
 */
- (void)getVersion:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR getVersion called");
    
    NSString *version = @"3.5.0"; // 百度SDK版本
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                 messageAsString:version];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * 检查权限
 */
- (void)checkPermission:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR checkPermission called");
    
    NSDictionary *permissions = [self getPermissionStatus];
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                 messageAsDictionary:permissions];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

/**
 * 请求权限
 */
- (void)requestPermission:(CDVInvokedUrlCommand*)command {
    NSLog(@"BaiduSpeechASR requestPermission called");
    
    // 在iOS中，权限通常在首次使用时自动请求
    // 这里返回当前权限状态
    NSDictionary *permissions = [self getPermissionStatus];
    
    CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK 
                                                 messageAsDictionary:permissions];
    [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
}

#pragma mark - 百度SDK核心方法

/**
 * 初始化百度SDK
 */
- (BOOL)initializeBaiduSDK {
    NSLog(@"Initializing Baidu SDK...");
    
    try {
        // 设置在线身份验证
        [[BDSpeechBaseKit sharedInstance] setASRLicenseWithAk:self.apiKey 
                                                          AndSK:self.secretKey 
                                                      AndAppcode:self.appId];
        
        // 创建语音识别事件管理器
        self.asrEventManager = [BDSEventManager createEventManagerWithName:BDS_ASR_NAME];
        if (!self.asrEventManager) {
            NSLog(@"Failed to create ASR event manager");
            return NO;
        }
        
        // 设置代理
        [self.asrEventManager setDelegate:self];
        
        // 创建语音唤醒事件管理器
        self.wakeupEventManager = [BDSEventManager createEventManagerWithName:BDS_WAKEUP_NAME];
        if (!self.wakeupEventManager) {
            NSLog(@"Failed to create wakeup event manager");
            return NO;
        }
        
        // 设置代理
        [self.wakeupEventManager setDelegate:self];
        
        NSLog(@"Baidu SDK initialized successfully");
        return YES;
        
    } @catch (NSException *exception) {
        NSLog(@"Baidu SDK initialization failed: %@", exception.reason);
        return NO;
    }
}

/**
 * 开始识别
 */
- (BOOL)startRecognitionWithParams:(NSDictionary *)params {
    NSLog(@"Starting recognition with params: %@", params);
    
    if (!self.asrEventManager) {
        NSLog(@"ASR event manager is nil");
        return NO;
    }
    
    try {
        // 设置识别参数
        [self configureRecognitionParams:params];
        
        // 发送开始识别指令
        [self.asrEventManager sendCommand:BDS_ASR_CMD_START];
        
        NSLog(@"Recognition start command sent successfully");
        return YES;
        
    } @catch (NSException *exception) {
        NSLog(@"Failed to start recognition: %@", exception.reason);
        return NO;
    }
}

/**
 * 停止识别
 */
- (void)stopRecognitionInternal {
    NSLog(@"Stopping recognition...");
    
    if (self.asrEventManager) {
        [self.asrEventManager sendCommand:BDS_ASR_CMD_STOP];
    }
    
    self.isRecognizing = NO;
    self.recognitionCallbackId = nil;
}

/**
 * 取消识别
 */
- (void)cancelRecognitionInternal {
    NSLog(@"Cancelling recognition...");
    
    if (self.asrEventManager) {
        [self.asrEventManager sendCommand:BDS_ASR_CMD_CANCEL];
    }
    
    self.isRecognizing = NO;
    self.recognitionCallbackId = nil;
}

/**
 * 开始唤醒
 */
- (BOOL)startWakeupWithParams:(NSDictionary *)params {
    NSLog(@"Starting wakeup with params: %@", params);
    
    if (!self.wakeupEventManager) {
        NSLog(@"Wakeup event manager is nil");
        return NO;
    }
    
    try {
        // 设置唤醒参数
        [self configureWakeupParams:params];
        
        // 发送开始唤醒指令
        [self.wakeupEventManager sendCommand:BDS_WP_CMD_START];
        
        NSLog(@"Wakeup start command sent successfully");
        return YES;
        
    } @catch (NSException *exception) {
        NSLog(@"Failed to start wakeup: %@", exception.reason);
        return NO;
    }
}

/**
 * 停止唤醒
 */
- (void)stopWakeupInternal {
    NSLog(@"Stopping wakeup...");
    
    if (self.wakeupEventManager) {
        [self.wakeupEventManager sendCommand:BDS_WP_CMD_STOP];
    }
    
    self.isWakeupActive = NO;
    self.wakeupCallbackId = nil;
}

/**
 * 释放资源
 */
- (void)releaseResources {
    NSLog(@"Releasing resources...");
    
    // 停止识别
    if (self.isRecognizing) {
        [self cancelRecognitionInternal];
    }
    
    // 停止唤醒
    if (self.isWakeupActive) {
        [self stopWakeupInternal];
    }
    
    // 释放事件管理器
    self.asrEventManager = nil;
    self.wakeupEventManager = nil;
    
    // 清空回调ID
    self.recognitionCallbackId = nil;
    self.wakeupCallbackId = nil;
    
    self.isInitialized = NO;
}

#pragma mark - BDSClientASRDelegate

/**
 * 语音识别代理回调
 */
- (void)VoiceRecognitionClientWorkStatus:(int)workStatus obj:(id)aObj {
    NSLog(@"Received recognition work status: %d, obj: %@", workStatus, aObj);
    
    [self handleRecognitionWorkStatus:workStatus obj:aObj];
}

/**
 * 处理识别工作状态
 */
- (void)handleRecognitionWorkStatus:(int)workStatus obj:(id)aObj {
    NSString *eventTypeStr = @"";
    NSString *message = @"";
    
    switch (workStatus) {
        case EVoiceRecognitionClientWorkStatusStartWorkIng:
            eventTypeStr = EVENT_READY;
            message = @"Recognition engine ready";
            break;
            
        case EVoiceRecognitionClientWorkStatusStart:
            eventTypeStr = EVENT_BEGINNING;
            message = @"检测到开始说话";
            break;
            
        case EVoiceRecognitionClientWorkStatusFlushData:
            eventTypeStr = EVENT_PARTIAL;
            if (aObj && [aObj isKindOfClass:[NSDictionary class]]) {
                message = aObj[@"results"] ?: @"";
            } else if (aObj && [aObj isKindOfClass:[NSString class]]) {
                message = aObj;
            }
            break;
            
        case EVoiceRecognitionClientWorkStatusFinish:
            eventTypeStr = EVENT_FINAL;
            if (aObj && [aObj isKindOfClass:[NSDictionary class]]) {
                message = aObj[@"results"] ?: @"";
            } else if (aObj && [aObj isKindOfClass:[NSString class]]) {
                message = aObj;
            }
            self.isRecognizing = NO;
            break;
            
        case EVoiceRecognitionClientWorkStatusEnd:
            eventTypeStr = EVENT_END;
            message = @"识别结束";
            break;
            
        case EVoiceRecognitionClientWorkStatusError:
            eventTypeStr = EVENT_ERROR;
            if (aObj && [aObj isKindOfClass:[NSDictionary class]]) {
                message = [NSString stringWithFormat:@"错误码: %@, 描述: %@", 
                          aObj[@"error_code"] ?: @"未知", 
                          aObj[@"error_desc"] ?: @"未知错误"];
            } else {
                message = @"识别发生未知错误";
            }
            self.isRecognizing = NO;
            break;
            
        case EVoiceRecognitionClientWorkStatusMeterLevel:
            eventTypeStr = EVENT_VOLUME;
            if (aObj && [aObj isKindOfClass:[NSNumber class]]) {
                message = [NSString stringWithFormat:@"%@", aObj];
            } else {
                message = @"0";
            }
            break;
            
        case EVoiceRecognitionClientWorkStatusRecorderEnd:
            eventTypeStr = EVENT_FINISH;
            message = @"录音机关闭";
            break;
            
        case EVoiceRecognitionClientWorkStatusLongSpeechEnd:
            eventTypeStr = EVENT_FINISH;
            message = @"长语音识别结束";
            break;
            
        default:
            NSLog(@"Unknown recognition work status: %d", workStatus);
            return;
    }
    
    [self sendCallback:eventTypeStr message:message callbackId:self.recognitionCallbackId];
}

#pragma mark - BDSClientWakeupDelegate

/**
 * 语音唤醒代理回调
 */
- (void)WakeupClientWorkStatus:(int)workStatus obj:(id)aObj {
    NSLog(@"Received wakeup work status: %d, obj: %@", workStatus, aObj);
    
    [self handleWakeupWorkStatus:workStatus obj:aObj];
}

/**
 * 处理唤醒工作状态
 */
- (void)handleWakeupWorkStatus:(int)workStatus obj:(id)aObj {
    NSString *eventTypeStr = @"";
    NSString *message = @"";
    
    switch (workStatus) {
        case EWakeupEngineWorkStatusStarted:
            eventTypeStr = EVENT_WAKEUP_SUCCESS;
            message = @"唤醒引擎启动成功";
            break;
            
        case EWakeupEngineWorkStatusStopped:
            eventTypeStr = EVENT_WAKEUP_SUCCESS;
            message = @"唤醒引擎停止";
            break;
            
        case EWakeupEngineWorkStatusLoaded:
            eventTypeStr = EVENT_WAKEUP_SUCCESS;
            message = @"唤醒引擎加载完成";
            break;
            
        case EWakeupEngineWorkStatusError:
            eventTypeStr = EVENT_WAKEUP_ERROR;
            if (aObj && [aObj isKindOfClass:[NSDictionary class]]) {
                message = [NSString stringWithFormat:@"唤醒错误: %@", 
                          aObj[@"error_desc"] ?: @"未知错误"];
            } else {
                message = @"唤醒发生未知错误";
            }
            self.isWakeupActive = NO;
            break;
            
        default:
            NSLog(@"Unknown wakeup work status: %d", workStatus);
            return;
    }
    
    [self sendCallback:eventTypeStr message:message callbackId:self.wakeupCallbackId];
}

#pragma mark - 辅助方法

/**
 * 配置识别参数
 */
- (void)configureRecognitionParams:(NSDictionary *)params {
    // 设置默认参数
    [self.asrEventManager setParameter:@(EVR_STRATEGY_ONLINE) forKey:BDS_ASR_STRATEGY];
    
    // 设置用户参数
    if (params[@"pid"]) {
        [self.asrEventManager setParameter:params[@"pid"] forKey:BDS_ASR_PROPERTY_LIST];
    }
    if (params[@"language"]) {
        NSInteger languageValue = [self getLanguageValue:params[@"language"]];
        [self.asrEventManager setParameter:@(languageValue) forKey:BDS_ASR_LANGUAGE];
    }
    if (params[@"accept-audio-volume"]) {
        [self.asrEventManager setParameter:params[@"accept-audio-volume"] forKey:BDS_ASR_ENABLE_LOCAL_VAD];
    }
    if (params[@"rate"]) {
        NSInteger rateValue = [self getSampleRateValue:params[@"rate"]];
        [self.asrEventManager setParameter:@(rateValue) forKey:BDS_ASR_SAMPLE_RATE];
    }
    if (params[@"cuid"]) {
        [self.asrEventManager setParameter:params[@"cuid"] forKey:BDS_ASR_CUID];
    }
    
    // 设置默认值
    if (![self.asrEventManager getParameter:BDS_ASR_PROPERTY_LIST]) {
        [self.asrEventManager setParameter:@[@(EVoiceRecognitionPropertyInput)] forKey:BDS_ASR_PROPERTY_LIST];
    }
    if (![self.asrEventManager getParameter:BDS_ASR_LANGUAGE]) {
        [self.asrEventManager setParameter:@(EVoiceRecognitionLanguageChinese) forKey:BDS_ASR_LANGUAGE];
    }
    if (![self.asrEventManager getParameter:BDS_ASR_ENABLE_LOCAL_VAD]) {
        [self.asrEventManager setParameter:@(YES) forKey:BDS_ASR_ENABLE_LOCAL_VAD];
    }
    if (![self.asrEventManager getParameter:BDS_ASR_SAMPLE_RATE]) {
        [self.asrEventManager setParameter:@(EVoiceRecognitionRecordSampleRate16K) forKey:BDS_ASR_SAMPLE_RATE];
    }
}

/**
 * 配置唤醒参数
 */
- (void)configureWakeupParams:(NSDictionary *)params {
    // 设置默认唤醒参数
    // 这里可以根据实际需求配置唤醒词等参数
}

/**
 * 发送回调 - 参考AISpeechTranscriber的实现
 */
- (void)sendCallback:(NSString *)type message:(NSString *)message callbackId:(NSString *)callbackId {
    if (!callbackId) {
        NSLog(@"CallbackId is null, cannot send callback");
        return;
    }
    
    try {
        NSDictionary *resultDict = @{
            @"type": type,
            @"message": message,
            @"timestamp": @([[NSDate date] timeIntervalSince1970] * 1000)
        };
        
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultDict];
        
        // 根据事件类型决定是否保持回调
        BOOL shouldKeepCallback = [self shouldKeepCallbackForType:type];
        [pluginResult setKeepCallback:shouldKeepCallback];
        
        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackId];
        
        NSLog(@"Callback sent - type: %@, message: %@, keepCallback: %@", type, message, shouldKeepCallback ? @"YES" : @"NO");
        
    } catch (Exception e) {
        NSLog(@"Failed to send callback: %@", e.reason);
    }
}

/**
 * 判断是否应该保持回调通道
 */
- (BOOL)shouldKeepCallbackForType:(NSString *)type {
    // 临时结果和音量变化需要保持回调
    if ([type isEqualToString:EVENT_PARTIAL] || [type isEqualToString:EVENT_VOLUME]) {
        return YES;
    }
    
    // 最终结果、错误和结束事件不需要保持回调
    if ([type isEqualToString:EVENT_FINAL] || [type isEqualToString:EVENT_ERROR] || 
        [type isEqualToString:EVENT_END] || [type isEqualToString:EVENT_FINISH]) {
        return NO;
    }
    
    // 其他事件默认保持回调
    return YES;
}

/**
 * 检查权限
 */
- (BOOL)checkPermissions {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    NSError *error;
    
    [session setCategory:AVAudioSessionCategoryPlayAndRecord error:&error];
    if (error) {
        NSLog(@"Failed to set audio session category: %@", error.localizedDescription);
        return NO;
    }
    
    [session setActive:YES error:&error];
    if (error) {
        NSLog(@"Failed to activate audio session: %@", error.localizedDescription);
        return NO;
    }
    
    return YES;
}

/**
 * 获取权限状态
 */
- (NSDictionary *)getPermissionStatus {
    AVAudioSessionRecordPermission recordPermission = [[AVAudioSession sharedInstance] recordPermission];
    
    BOOL hasRecordPermission = (recordPermission == AVAudioSessionRecordPermissionGranted);
    
    return @{
        @"RECORD_AUDIO": @(hasRecordPermission),
        @"hasAllPermissions": @(hasRecordPermission)
    };
}

/**
 * 隐藏敏感信息
 */
- (NSString *)maskKey:(NSString *)key {
    if (!key || key.length <= 8) {
        return @"****";
    }
    return [NSString stringWithFormat:@"%@****%@", 
            [key substringToIndex:4], 
            [key substringFromIndex:key.length - 4]];
}

/**
 * 获取语言枚举值
 */
- (NSInteger)getLanguageValue:(NSString *)language {
    if ([language isEqualToString:@"zh"] || [language isEqualToString:@"chinese"]) {
        return EVoiceRecognitionLanguageChinese;
    } else if ([language isEqualToString:@"en"] || [language isEqualToString:@"english"]) {
        return EVoiceRecognitionLanguageEnglish;
    } else if ([language isEqualToString:@"yue"] || [language isEqualToString:@"cantonese"]) {
        return EVoiceRecognitionLanguageCantonese;
    } else if ([language isEqualToString:@"sichuan"] || [language isEqualToString:@"sichuanese"]) {
        return EVoiceRecognitionLanguageSichuanDialect;
    }
    return EVoiceRecognitionLanguageChinese; // 默认中文
}

/**
 * 获取采样率枚举值
 */
- (NSInteger)getSampleRateValue:(NSString *)rate {
    if ([rate isEqualToString:@"8000"] || [rate isEqualToString:@"8k"]) {
        return EVoiceRecognitionRecordSampleRate8K;
    } else if ([rate isEqualToString:@"16000"] || [rate isEqualToString:@"16k"]) {
        return EVoiceRecognitionRecordSampleRate16K;
    }
    return EVoiceRecognitionRecordSampleRate16K; // 默认16k
}

@end
