//
//  BDSpeechTTSBaseKit.h
//  BDSpeechTTSBaseKit
//
//  Created by v_bihongwei on 2025/6/26.
//

#import <Foundation/Foundation.h>
#import "BDSSpeechSynthesizer.h"
#import <AVFoundation/AVFoundation.h>

@interface BDSpeechTTSBaseKit : NSObject

// 获取单例类实现方法
+ (instancetype)sharedInstance;

// 获取鉴权sdk版本号
- (NSString *)getTTSLibVersion;

// 设置通用鉴权库日志开启状态 默认不开启：NO
- (void)setTTSAuthLogStatus:(BOOL)status;

// ak、sk方式鉴权
- (void)setTTSLicenseWithAk:(NSString *)ak AndSK:(NSString *)sk;

// iamkey鉴权方式
- (void)setTTSLicenseWithIamKey:(NSString *)iamkey;

/**
 token鉴权方式
 */
- (void)setTTSLicenseWithAuthToken:(NSString *)token AndExpirationDate:(long long)expirationDate;

/**
 iamkey方式初始化鉴权
 */
- (void)setTTSLicenseWithIAMKey:(NSString *)iamkey AndExpirationDate:(long long)expirationDate;

@end
