//
//  BaiduTTSManager.m
//  cordova-plugin-baidu-speech-ASR
//
//  Created by CTY Library on 2026/02/28.
//  Copyright © 2026 CTY Library. All rights reserved.
//

#import "BaiduTTSManager.h"
#import <AVFoundation/AVFoundation.h>

@implementation BaiduTTSManager

#pragma mark - 初始化

- (instancetype)init {
    self = [super init];
    if (self) {
        [self setupDefaultValues];
    }
    return self;
}

- (void)setupDefaultValues {
    // 设置默认值
    self.voice = AVSpeechSynthesisVoiceDefaultLanguage;
    self.rate = AVSpeechUtteranceDefaultSpeechRate;
    self.pitch = 1.0f;
    self.volume = 1.0f;
    self.isInitialized = NO;
    self.isSpeaking = NO;
}

- (BOOL)initialize {
    NSLog(@"BaiduTTSManager iOS initializing...");
    
    // 检查系统是否支持语音合成
    if (![AVSpeechSynthesizer class]) {
        NSLog(@"AVSpeechSynthesizer not supported on this device");
        return NO;
    }
    
    // 创建语音合成器
    self.speechSynthesizer = [[AVSpeechSynthesizer alloc] init];
    self.speechSynthesizer.delegate = self;
    
    self.isInitialized = YES;
    NSLog(@"BaiduTTSManager iOS initialized successfully");
    return YES;
}

#pragma mark - 语音合成方法

- (BOOL)speak:(NSString *)text {
    if (!self.isInitialized) {
        NSLog(@"TTS not initialized");
        return NO;
    }
    
    if (!text || text.length == 0) {
        NSLog(@"Text cannot be empty");
        return NO;
    }
    
    if (self.isSpeaking) {
        NSLog(@"TTS is already speaking");
        return NO;
    }
    
    // 创建语音请求
    AVSpeechUtterance *utterance = [AVSpeechUtterance speechUtteranceWithString:text];
    utterance.voice = [AVSpeechSynthesisVoice voiceWithLanguage:self.voice];
    utterance.rate = self.rate;
    utterance.pitchMultiplier = self.pitch;
    utterance.volume = self.volume;
    
    // 开始语音合成
    [self.speechSynthesizer speakUtterance:utterance];
    
    NSLog(@"TTS speak started: %@", text);
    
    // 发送开始事件
    if (self.eventHandler) {
        self.eventHandler(@"speak_start", @{@"text": text});
    }
    
    return YES;
}

- (BOOL)synthesize:(NSString *)text {
    // iOS系统API不支持仅合成不播放，这里直接调用speak
    return [self speak:text];
}

- (BOOL)pause {
    if (!self.isInitialized || !self.isSpeaking) {
        return NO;
    }
    
    if ([self.speechSynthesizer pauseSpeakingAtBoundary:AVSpeechBoundaryImmediate]) {
        NSLog(@"TTS paused");
        
        // 发送暂停事件
        if (self.eventHandler) {
            self.eventHandler(@"speak_paused", @{});
        }
        return YES;
    }
    
    return NO;
}

- (BOOL)resume {
    if (!self.isInitialized) {
        return NO;
    }
    
    if ([self.speechSynthesizer continueSpeaking]) {
        NSLog(@"TTS resumed");
        
        // 发送恢复事件
        if (self.eventHandler) {
            self.eventHandler(@"speak_resumed", @{});
        }
        return YES;
    }
    
    return NO;
}

- (BOOL)stop {
    if (!self.isInitialized) {
        return NO;
    }
    
    if ([self.speechSynthesizer stopSpeakingAtBoundary:AVSpeechBoundaryImmediate]) {
        self.isSpeaking = NO;
        NSLog(@"TTS stopped");
        
        // 发送停止事件
        if (self.eventHandler) {
            self.eventHandler(@"speak_stopped", @{});
        }
        return YES;
    }
    
    return NO;
}

- (void)release {
    if (self.speechSynthesizer) {
        [self.speechSynthesizer stopSpeakingAtBoundary:AVSpeechBoundaryImmediate];
        self.speechSynthesizer.delegate = nil;
        self.speechSynthesizer = nil;
    }
    
    self.isInitialized = NO;
    self.isSpeaking = NO;
    NSLog(@"TTS released");
}

#pragma mark - 参数设置方法

- (void)setVoice:(NSString *)voice {
    _voice = voice ? voice : AVSpeechSynthesisVoiceDefaultLanguage;
}

- (void)setRate:(float)rate {
    _rate = MAX(AVSpeechUtteranceMinimumSpeechRate, MIN(AVSpeechUtteranceMaximumSpeechRate, rate));
}

- (void)setPitchMultiplier:(float)pitch {
    _pitch = MAX(0.5f, MIN(2.0f, pitch));
}

- (void)setVolume:(float)volume {
    _volume = MAX(0.0f, MIN(1.0f, volume));
}

#pragma mark - 状态和版本信息

- (NSDictionary *)getStatus {
    return @{
        @"initialized": @(self.isInitialized),
        @"speaking": @(self.isSpeaking),
        @"voice": self.voice ?: @"",
        @"rate": @(self.rate),
        @"pitch": @(self.pitch),
        @"volume": @(self.volume)
    };
}

- (NSString *)getVersion {
    return @"1.0.0"; // iOS系统TTS版本
}

#pragma mark - AVSpeechSynthesizerDelegate

- (void)speechSynthesizer:(AVSpeechSynthesizer *)synthesizer didStartSpeechUtterance:(AVSpeechUtterance *)utterance {
    NSLog(@"Speech started: %@", utterance.speechString);
    self.isSpeaking = YES;
    
    if (self.eventHandler) {
        self.eventHandler(@"play_start", @{
            @"text": utterance.speechString ?: @""
        });
    }
}

- (void)speechSynthesizer:(AVSpeechSynthesizer *)synthesizer didFinishSpeechUtterance:(AVSpeechUtterance *)utterance {
    NSLog(@"Speech finished: %@", utterance.speechString);
    self.isSpeaking = NO;
    
    if (self.eventHandler) {
        self.eventHandler(@"play_finish", @{
            @"text": utterance.speechString ?: @""
        });
    }
}

- (void)speechSynthesizer:(AVSpeechSynthesizer *)synthesizer didPauseSpeechUtterance:(AVSpeechUtterance *)utterance {
    NSLog(@"Speech paused: %@", utterance.speechString);
    
    if (self.eventHandler) {
        self.eventHandler(@"play_paused", @{
            @"text": utterance.speechString ?: @""
        });
    }
}

- (void)speechSynthesizer:(AVSpeechSynthesizer *)synthesizer didContinueSpeechUtterance:(AVSpeechUtterance *)utterance {
    NSLog(@"Speech continued: %@", utterance.speechString);
    
    if (self.eventHandler) {
        self.eventHandler(@"play_resumed", @{
            @"text": utterance.speechString ?: @""
        });
    }
}

- (void)speechSynthesizer:(AVSpeechSynthesizer *)synthesizer didCancelSpeechUtterance:(AVSpeechUtterance *)utterance {
    NSLog(@"Speech cancelled: %@", utterance.speechString);
    self.isSpeaking = NO;
    
    if (self.eventHandler) {
        self.eventHandler(@"play_cancelled", @{
            @"text": utterance.speechString ?: @""
        });
    }
}

- (void)speechSynthesizer:(AVSpeechSynthesizer *)synthesizer willSpeakRangeOfSpeechString:(NSRange)characterRange utterance:(AVSpeechUtterance *)utterance {
    // 播放进度回调
    float progress = 0.0f;
    if (utterance.speechString.length > 0) {
        progress = (float)(characterRange.location + characterRange.length) / utterance.speechString.length;
    }
    
    if (self.eventHandler) {
        self.eventHandler(@"play_progress", @{
            @"progress": @(progress),
            @"location": @(characterRange.location),
            @"length": @(characterRange.length),
            @"text": utterance.speechString ?: @""
        });
    }
}

@end
