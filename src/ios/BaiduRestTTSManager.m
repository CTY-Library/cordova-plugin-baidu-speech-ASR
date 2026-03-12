//
//  BaiduRestTTSManager.m
//  cordova-plugin-baidu-speech-ASR
//
//  Created by CTY Library on 2026/03/12.
//  Copyright © 2026 CTY Library. All rights reserved.
//

#import "BaiduRestTTSManager.h"

static NSString *const kBaiduTTSBaseURL = @"https://tsn.baidu.com/text2audio";
static NSString *const kBaiduOAuthURL = @"https://aip.baidubce.com/oauth/2.0/token";

@implementation BaiduRestTTSManager

#pragma mark - 初始化

- (instancetype)init {
    self = [super init];
    if (self) {
        [self setupDefaultValues];
    }
    return self;
}

- (void)setupDefaultValues {
    // 设置默认参数
//    self.voice = @"zh";
//    self.speed = 5.0;
//    self.pitch = 5.0;
//    self.volume = 9.0;
    
    // 初始化状态
    self.isInitialized = NO;
    self.isSpeaking = NO;
    self.isPaused = NO;
    
    NSLog(@"BaiduRestTTSManager initialized with default values");
}

- (BOOL)initialize:(NSString *)apiKey secretKey:(NSString *)secretKey {
    NSLog(@"BaiduRestTTSManager initializing...");
    
    if (!apiKey || apiKey.length == 0 || !secretKey || secretKey.length == 0) {
        NSLog(@"API key or secret key is empty");
        return NO;
    }
    
    self.apiKey = apiKey;
    self.secretKey = secretKey;
    
    // 获取访问令牌
    __block BOOL success = NO;
    dispatch_semaphore_t semaphore = dispatch_semaphore_create(0);
    
    [self getAccessToken:^(BOOL tokenSuccess, NSString *token) {
        success = tokenSuccess;
        if (success) {
            self.accessToken = token;
            self.isInitialized = YES;
            NSLog(@"BaiduRestTTSManager initialized successfully");
        } else {
            NSLog(@"Failed to get access token");
        }
        dispatch_semaphore_signal(semaphore);
    }];
    
    dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER);
    
    return success;
}

#pragma mark - REST API方法

- (void)getAccessToken:(void(^)(BOOL success, NSString *token))completion {
    NSString *urlString = [NSString stringWithFormat:@"%@?grant_type=client_credentials&client_id=%@&client_secret=%@",
                           kBaiduOAuthURL, self.apiKey, self.secretKey];
    
    NSURL *url = [NSURL URLWithString:urlString];
    NSURLRequest *request = [NSURLRequest requestWithURL:url];
    
    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithRequest:request
                                                             completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        if (error) {
            NSLog(@"Failed to get access token: %@", error.localizedDescription);
            completion(NO, nil);
            return;
        }
        
        NSError *jsonError;
        NSDictionary *jsonDict = [NSJSONSerialization JSONObjectWithData:data options:0 error:&jsonError];
        
        if (jsonError) {
            NSLog(@"Failed to parse access token response: %@", jsonError.localizedDescription);
            completion(NO, nil);
            return;
        }
        
        NSString *token = jsonDict[@"access_token"];
        if (token && token.length > 0) {
            NSLog(@"Access token obtained successfully");
            completion(YES, token);
        } else {
            NSLog(@"No access token in response: %@", jsonDict);
            completion(NO, nil);
        }
    }];
    
    [task resume];
}

- (void)performSynthesis:(NSString *)text completion:(void(^)(BOOL success, NSData *audioData))completion {
    if (!self.accessToken || self.accessToken.length == 0) {
        NSLog(@"No access token available");
        completion(NO, nil);
        return;
    }
    
    // 构建请求参数
    NSDictionary *params = @{
        @"tex": text,
        @"tok": self.accessToken,
        @"cuid": @"cordova_ios_device",
        @"ctp": @"1",
        @"lan": @"zh",
        @"spd": @"5",
        @"pit": @"5",
        @"vol": @"100000",
        @"per": @"5",
        @"aue": @"3"  // mp3格式
    };
    
    // 构建URL
    NSMutableArray *paramPairs = [NSMutableArray array];
    for (NSString *key in params) {
        NSString *value = params[key];
        NSString *encodedValue = [value stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]];
        [paramPairs addObject:[NSString stringWithFormat:@"%@=%@", key, encodedValue]];
    }
    
    NSString *urlString = [NSString stringWithFormat:@"%@?%@", kBaiduTTSBaseURL, [paramPairs componentsJoinedByString:@"&"]];
    NSURL *url = [NSURL URLWithString:urlString];
    
    NSLog(@"TTS synthesis request: %@", urlString);
    
    NSURLRequest *request = [NSURLRequest requestWithURL:url];
    
    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithRequest:request
                                                             completionHandler:^(NSData *data, NSURLResponse *response, NSError *error) {
        if (error) {
            NSLog(@"TTS synthesis failed: %@", error.localizedDescription);
            completion(NO, nil);
            return;
        }
        
        // 检查响应是否为JSON错误
        NSString *contentType = [(NSHTTPURLResponse *)response allHeaderFields][@"Content-Type"];
        if ([contentType containsString:@"application/json"]) {
            NSError *jsonError;
            NSDictionary *jsonDict = [NSJSONSerialization JSONObjectWithData:data options:0 error:&jsonError];
            if (!jsonError && jsonDict[@"err_no"]) {
                NSLog(@"TTS API error: %@", jsonDict);
                completion(NO, nil);
                return;
            }
        }
        
        if (data.length > 0) {
            NSLog(@"TTS synthesis successful, audio data size: %lu bytes", (unsigned long)data.length);
            completion(YES, data);
        } else {
            NSLog(@"TTS synthesis failed: no audio data");
            completion(NO, nil);
        }
    }];
    
    [task resume];
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
    
    if (self.isSpeaking && !self.isPaused) {
        NSLog(@"TTS is already speaking");
        return NO;
    }
    
    self.currentText = text;
    self.currentSentenceId = arc4random();
    
    // 发送开始事件
    if (self.eventHandler) {
        self.eventHandler(@"speak_start", @{
            @"text": text,
            @"sentenceId": @(self.currentSentenceId)
        });
    }
    
    // 执行合成并播放
    [self performSynthesis:text completion:^(BOOL success, NSData *audioData) {
        if (success) {
            self.isSpeaking = YES;
            [self playAudioData:audioData];
        } else {
            if (self.eventHandler) {
                self.eventHandler(@"speak_error", @{@"message": @"Synthesis failed"});
            }
        }
    }];
    
    return YES;
}

- (BOOL)synthesize:(NSString *)text {
    if (!self.isInitialized) {
        NSLog(@"TTS not initialized");
        return NO;
    }
    
    if (!text || text.length == 0) {
        NSLog(@"Text cannot be empty");
        return NO;
    }
    
    self.currentText = text;
    self.currentSentenceId = arc4random();
    
    // 发送开始事件
    if (self.eventHandler) {
        self.eventHandler(@"synthesize_start", @{
            @"text": text,
            @"sentenceId": @(self.currentSentenceId)
        });
    }
    
    // 仅合成不播放
    [self performSynthesis:text completion:^(BOOL success, NSData *audioData) {
        if (success) {
            NSString *filePath = [self saveAudioData:audioData];
            if (filePath) {
                if (self.eventHandler) {
                    self.eventHandler(@"synthesize_complete", @{
                        @"text": text,
                        @"sentenceId": @(self.currentSentenceId),
                        @"filePath": filePath
                    });
                }
            } else {
                if (self.eventHandler) {
                    self.eventHandler(@"synthesize_error", @{@"message": @"Failed to save audio file"});
                }
            }
        } else {
            if (self.eventHandler) {
                self.eventHandler(@"synthesize_error", @{@"message": @"Synthesis failed"});
            }
        }
    }];
    
    return YES;
}

#pragma mark - 音频播放

- (void)playAudioData:(NSData *)audioData {
    NSError *error;
    self.audioPlayer = [[AVAudioPlayer alloc] initWithData:audioData fileTypeHint:@"mp3" error:&error];
    
    if (error) {
        NSLog(@"Failed to create audio player: %@", error.localizedDescription);
        if (self.eventHandler) {
            self.eventHandler(@"speak_error", @{@"message": error.localizedDescription});
        }
        return;
    }
    
    self.audioPlayer.delegate = self;
    self.audioPlayer.volume = self.volume / 15.0;
    if(self.audioPlayer.volume<=0){
        self.audioPlayer.volume = 10000;
    }
    
    if ([self.audioPlayer prepareToPlay]) {
        if ([self.audioPlayer play]) {
            NSLog(@"Audio playback started");
            if (self.eventHandler) {
                self.eventHandler(@"speak_progress", @{@"state": @"playing"});
            }
        } else {
            NSLog(@"Failed to play audio");
            if (self.eventHandler) {
                self.eventHandler(@"speak_error", @{@"message": @"Failed to play audio"});
            }
        }
    } else {
        NSLog(@"Failed to prepare audio player");
        if (self.eventHandler) {
            self.eventHandler(@"speak_error", @{@"message": @"Failed to prepare audio player"});
        }
    }
}

- (NSString *)saveAudioData:(NSData *)audioData {
    NSString *fileName = [NSString stringWithFormat:@"tts_%ld_%ld.mp3", (long)[[NSDate date] timeIntervalSince1970], (long)self.currentSentenceId];
    NSString *filePath = [NSTemporaryDirectory() stringByAppendingPathComponent:fileName];
    
    BOOL success = [audioData writeToFile:filePath atomically:YES];
    if (success) {
        NSLog(@"Audio file saved to: %@", filePath);
        return filePath;
    } else {
        NSLog(@"Failed to save audio file");
        return nil;
    }
}

#pragma mark - AVAudioPlayerDelegate

- (void)audioPlayerDidFinishPlaying:(AVAudioPlayer *)player successfully:(BOOL)flag {
    NSLog(@"Audio playback finished");
    self.isSpeaking = NO;
    
    if (self.eventHandler) {
        self.eventHandler(@"speak_complete", @{
            @"text": self.currentText,
            @"sentenceId": @(self.currentSentenceId)
        });
    }
}

- (void)audioPlayerDecodeErrorDidOccur:(AVAudioPlayer *)player error:(NSError *)error {
    NSLog(@"Audio player decode error: %@", error.localizedDescription);
    self.isSpeaking = NO;
    
    if (self.eventHandler) {
        self.eventHandler(@"speak_error", @{@"message": error.localizedDescription});
    }
}

#pragma mark - 播放控制

- (BOOL)pause {
    if (self.audioPlayer && self.audioPlayer.isPlaying) {
        
            self.isPaused = YES;
            NSLog(@"Audio paused");
            
            if (self.eventHandler) {
                self.eventHandler(@"speak_paused", @{});
            }
            return YES;
         
    }
    return NO;
}

- (BOOL)resume {
    if (self.audioPlayer && self.isPaused) {
        if ([self.audioPlayer play]) {
            self.isPaused = NO;
            NSLog(@"Audio resumed");
            
            if (self.eventHandler) {
                self.eventHandler(@"speak_resumed", @{});
            }
            return YES;
        }
    }
    return NO;
}

- (BOOL)stop {
    if (self.audioPlayer) {
        [self.audioPlayer stop];
        self.audioPlayer = nil;
        self.isSpeaking = NO;
        self.isPaused = NO;
        
        NSLog(@"Audio stopped");
        
        if (self.eventHandler) {
            self.eventHandler(@"speak_stopped", @{});
        }
        return YES;
    }
    return NO;
}

#pragma mark - 参数设置

- (void)setVoice:(NSString *)voice {
    if (voice && voice.length > 0) {
        self.voice = voice;
        NSLog(@"Voice set to: %@", voice);
    }
}

- (void)setSpeed:(float)speed {
    // 限制语速范围 1-15
    speed = MAX(1.0, MIN(15.0, speed));
    self.speed = speed;
    NSLog(@"Speed set to: %.1f", speed);
}

- (void)setPitch:(float)pitch {
    // 限制音调范围 1-15
    pitch = MAX(1.0, MIN(15.0, pitch));
    self.pitch = pitch;
    NSLog(@"Pitch set to: %.1f", pitch);
}

- (void)setVolume:(float)volume {
    // 限制音量范围 1-15
    volume = MAX(1.0, MIN(15.0, volume));
    self.volume = volume;
    
    // 如果正在播放，更新播放器音量
    if (self.audioPlayer) {
        self.audioPlayer.volume = volume / 15.0;
    }
    
    NSLog(@"Volume set to: %.1f", volume);
}

#pragma mark - 状态查询

- (NSDictionary *)getStatus {
    return @{
        @"isInitialized": @(self.isInitialized),
        @"isSpeaking": @(self.isSpeaking),
        @"isPaused": @(self.isPaused),
        @"voice": self.voice,
        @"speed": @(self.speed),
        @"pitch": @(self.pitch),
        @"volume": @(self.volume)
    };
}

- (NSString *)getVersion {
    return @"1.0.0-REST-API";
}

#pragma mark - 资源管理

- (void)cleanupResources {
    [self stop];
    self.accessToken = nil;
    self.apiKey = nil;
    self.secretKey = nil;
    self.isInitialized = NO;
    NSLog(@"BaiduRestTTSManager cleaned up");
}

@end
