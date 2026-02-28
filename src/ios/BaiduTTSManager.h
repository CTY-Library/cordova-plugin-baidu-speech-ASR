//
//  BaiduTTSManager.h
//  cordova-plugin-baidu-speech-ASR
//
//  Created by CTY Library on 2026/02/28.
//  Copyright © 2026 CTY Library. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * 百度语音合成管理器 iOS 端
 * 基于系统AVSpeechSynthesizer实现
 */
@interface BaiduTTSManager : NSObject

// 语音合成器
@property (nonatomic, strong) AVSpeechSynthesizer *speechSynthesizer;

// 配置参数
@property (nonatomic, strong) NSString *voice; // 声音类型
@property (nonatomic, assign) float rate; // 语速
@property (nonatomic, assign) float pitch; // 音调
@property (nonatomic, assign) float volume; // 音量

// 状态标识
@property (nonatomic, assign) BOOL isInitialized;
@property (nonatomic, assign) BOOL isSpeaking;

// 事件回调
@property (nonatomic, copy) void(^eventHandler)(NSString *type, NSDictionary *data);

/**
 * 初始化TTS
 */
- (BOOL)initialize;

/**
 * 开始语音合成并播放
 */
- (BOOL)speak:(NSString *)text;

/**
 * 仅合成不播放
 */
- (BOOL)synthesize:(NSString *)text;

/**
 * 暂停播放
 */
- (BOOL)pause;

/**
 * 恢复播放
 */
- (BOOL)resume;

/**
 * 停止播放
 */
- (BOOL)stop;

/**
 * 释放资源
 */
- (void)release;

/**
 * 设置声音类型
 */
- (void)setVoice:(NSString *)voice;

/**
 * 设置语速 (0.0-1.0)
 */
- (void)setRate:(float)rate;

/**
 * 设置音调 (0.5-2.0)
 */
- (void)setPitchMultiplier:(float)pitch;

/**
 * 设置音量 (0.0-1.0)
 */
- (void)setVolume:(float)volume;

/**
 * 获取状态信息
 */
- (NSDictionary *)getStatus;

/**
 * 获取版本信息
 */
- (NSString *)getVersion;

@end

NS_ASSUME_NONNULL_END
