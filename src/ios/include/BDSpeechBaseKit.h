//
//  BDSpeechBaseKit.h
//  BDSpeechBaseKit
//
//  Created by v_bihongwei on 2025/6/3.
//

#import <Foundation/Foundation.h>
#import "BDSASRDefines.h"
#import "BDSASRParameters.h"
#import "BDSEventManager.h"
#import "fcntl.h"

@interface BDSpeechBaseKit : NSObject

// 获取单例类实现方法
+ (instancetype)sharedInstance;

// 设置识别类
- (BDSEventManager *)getBDSEventManager;

// 设置唤醒类
- (BDSEventManager *)getWakeupEventManager;

// 设置通用鉴权库日志开启状态 默认不开启：NO
- (void)setAuthLogStatus:(BOOL)status;

// ak、sk方式鉴权
- (void)setASRLicenseWithAk:(NSString *)ak AndSK:(NSString *)sk AndAppcode:(NSString *)code;

// iamkey鉴权方式
- (void)setASRLicenseWithIamKey:(NSString *)iamkey Andsk:(NSString *)ak AndAppCode:(NSString *)code;

// 获取鉴权asr鉴权sdk版本号
- (NSString *)getASRLibVersion;

// 临时token鉴权方式
- (void)setASRLicenseWithAuthToken:(NSString *)token AndExpirationDate:(long long)expirationDate AndAk:(NSString *)ak AndAppCode:(NSString *)code;


// 临时iamkey方式初始化鉴权
- (void)setASRLicenseWithIAMKey:(NSString *)iamkey AndExpirationDate:(long long)expirationDate AndAk:(NSString *)ak AndAppCode:(NSString *)code;

@end
