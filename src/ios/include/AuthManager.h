//
//  AuthManager.h
//  AuthManager
//
//  Created by v_bihongwei on 2025/6/30.
//

#import <Foundation/Foundation.h>

// 协议声明
@protocol TemporaryTokenAndKeyDelegate <NSObject>
// 协议方法获取临时token
- (void)getToken;
- (void)getIAMKey;
@end

NS_ASSUME_NONNULL_BEGIN

@interface AuthManager : NSObject

// 获取公共协议属性
@property (assign,nonatomic,) id<TemporaryTokenAndKeyDelegate> temporaryTokenAndKeyDelegate;

// 鉴权sdk模块日志开关
@property (nonatomic, assign)BOOL isAuthLog;

// 获取单例类实现方法
+ (instancetype)sharedInstance;

// accesstoken鉴权方式
/**
 param:
 token: 传入的临时token
 expirationDate：传入的有效期时间
 */
- (void)setCommonLicenseWithAuthToken:(NSString *)token AndExpirationDate:(long long)expirationDate;

// iamkey鉴权方式
/**
 param:
 iamkey: 传入临时iamkey
 expirationDate：传入的有效期时间
 */
- (void)setCommonLicenseWithIAMKey:(NSString *)iamkey AndExpirationDate:(long long)expirationDate;

@end

NS_ASSUME_NONNULL_END
