//
//  BaiduRestTTSManager.h
//  cordova-plugin-baidu-speech-ASR
//
//  Created by CTY Library on 2026/03/12.
//  Copyright © 2026 CTY Library. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AVFoundation/AVFoundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * 百度REST API语音合成管理器 iOS 端
 * 基于百度REST API实现
 */
@interface BaiduRestTTSManager : NSObject <AVAudioPlayerDelegate>

// 事件处理器
@property (nonatomic, copy) void (^eventHandler)(NSString *event, NSDictionary *data);

// API配置
@property (nonatomic, strong) NSString *apiKey;
@property (nonatomic, strong) NSString *secretKey;
@property (nonatomic, strong) NSString *accessToken;

// 合成参数
@property (nonatomic, strong) NSString *voice;
@property (nonatomic, assign) float speed;
@property (nonatomic, assign) float pitch;
@property (nonatomic, assign) float volume;

// 状态管理
@property (nonatomic, assign) BOOL isInitialized;
@property (nonatomic, assign) BOOL isSpeaking;
@property (nonatomic, assign) BOOL isPaused;

// 音频播放器
@property (nonatomic, strong) AVAudioPlayer *audioPlayer;

// 当前合成任务
@property (nonatomic, strong) NSString *currentText;
@property (nonatomic, assign) NSInteger currentSentenceId;

#pragma mark - 初始化

/**
 * 初始化TTS管理器
 */
- (instancetype)init;

/**
 * 设置API密钥
 */
- (BOOL)initialize:(NSString *)apiKey secretKey:(NSString *)secretKey;

#pragma mark - 语音合成方法

/**
 * 语音合成并播放
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
- (void)cleanupResources;

#pragma mark - 参数设置

/**
 * 设置发音人
 */
- (void)setVoice:(NSString *)voice;

/**
 * 设置语速 (1-15)
 */
- (void)setSpeed:(float)speed;

/**
 * 设置音调 (1-15)
 */
- (void)setPitch:(float)pitch;

/**
 * 设置音量 (1-15)
 */
- (void)setVolume:(float)volume;

#pragma mark - 状态查询

/**
 * 获取当前状态
 */
- (NSDictionary *)getStatus;

/**
 * 获取版本信息
 */
- (NSString *)getVersion;

#pragma mark - 私有方法

/**
 * 获取访问令牌
 */
- (void)getAccessToken:(void(^)(BOOL success, NSString *token))completion;

/**
 * 执行REST API合成请求
 */
- (void)performSynthesis:(NSString *)text completion:(void(^)(BOOL success, NSData *audioData))completion;

/**
 * 播放音频数据
 */
- (void)playAudioData:(NSData *)audioData;

/**
 * 保存音频数据到文件
 */
- (NSString *)saveAudioData:(NSData *)audioData;

@end

NS_ASSUME_NONNULL_END
