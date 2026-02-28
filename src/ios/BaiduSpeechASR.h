//
//  BaiduSpeechASR.h
//  cordova-plugin-baidu-speech-ASR
//
//  Created by CTY Library on 2026/01/28.
//  Copyright © 2026 CTY Library. All rights reserved.
//

#import <Cordova/CDVPlugin.h>
#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>
#import <Speech/Speech.h>

// 百度语音识别SDK头文件 (使用本地SDK)
#import "BDSpeechBaseKit.h"
#import "BDSEventManager.h"
#import "BDSASRDefines.h"
#import "BDSASRParameters.h"
#import "BDSWakeupDefines.h"
#import "BDSWakeupParameters.h"

// TTS管理器
@class BaiduTTSManager;

NS_ASSUME_NONNULL_BEGIN

/**
 * 百度语音识别 Cordova 插件 iOS 端实现
 * 基于百度语音识别iOS SDK V3.0.13
 */
@interface BaiduSpeechASR : CDVPlugin <BDSClientASRDelegate, BDSClientWakeupDelegate>

// 语音识别事件管理器
@property (nonatomic, strong) BDSEventManager *asrEventManager;

// 语音唤醒事件管理器
@property (nonatomic, strong) BDSEventManager *wakeupEventManager;

// TTS管理器
@property (nonatomic, strong) BaiduTTSManager *ttsManager;

// 回调上下文
@property (nonatomic, strong) NSString *recognitionCallbackId;
@property (nonatomic, strong) NSString *wakeupCallbackId;
@property (nonatomic, strong) NSString *ttsCallbackId;

// 配置参数
@property (nonatomic, strong) NSString *apiKey;
@property (nonatomic, strong) NSString *secretKey;
@property (nonatomic, strong) NSString *appId;

// 状态标识
@property (nonatomic, assign) BOOL isInitialized;
@property (nonatomic, assign) BOOL isRecognizing;
@property (nonatomic, assign) BOOL isWakeupActive;

@end

NS_ASSUME_NONNULL_END
