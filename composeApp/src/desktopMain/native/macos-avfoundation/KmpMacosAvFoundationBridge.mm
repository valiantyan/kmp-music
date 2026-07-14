#import <AVFoundation/AVFoundation.h>
#import <Foundation/Foundation.h>
#import <CoreMedia/CoreMedia.h>
#import <jni.h>
#import <math.h>

static const jint KMP_STATUS_ACCEPTED = 0;
static const jint KMP_STATUS_MISSING_FILE = 10;
static const jint KMP_STATUS_UNSUPPORTED_FORMAT = 11;
static const jint KMP_STATUS_PERMISSION_DENIED = 12;
static const jint KMP_STATUS_ENGINE_UNAVAILABLE = 13;
static const jint KMP_STATUS_UNKNOWN = 14;

// 这些错误码是 JNI 边界协议，镜像 Kotlin 侧显式常量，禁止按 enum ordinal 推断。
static const jint KMP_ERROR_MISSING_FILE = 0;
static const jint KMP_ERROR_UNSUPPORTED_FORMAT = 1;
static const jint KMP_ERROR_PERMISSION_DENIED = 2;
static const jint KMP_ERROR_ENGINE_UNAVAILABLE = 3;
static const jint KMP_ERROR_UNKNOWN = 4;

static void *KmpBridgeQueueKey = &KmpBridgeQueueKey;

static NSString *KmpStringFromJString(JNIEnv *env, jstring value) {
    if (value == nullptr) {
        return @"";
    }
    const char *chars = env->GetStringUTFChars(value, nullptr);
    if (chars == nullptr) {
        return @"";
    }
    NSString *result = [NSString stringWithUTF8String:chars] ?: @"";
    env->ReleaseStringUTFChars(value, chars);
    return result;
}

static BOOL KmpDurationMsFromItem(AVPlayerItem *item, int64_t *durationMs) {
    if (item == nil) {
        return NO;
    }
    CMTime duration = item.duration;
    Float64 seconds = CMTimeGetSeconds(duration);
    if (!isfinite(seconds) || seconds <= 0.0) {
        return NO;
    }
    *durationMs = (int64_t)(seconds * 1000.0);
    return YES;
}

@interface KmpMacosAvFoundationBridge : NSObject
@property(nonatomic, assign) JavaVM *javaVm;
@property(nonatomic, assign) jobject callback;
@property(nonatomic, assign) jmethodID onPrepared;
@property(nonatomic, assign) jmethodID onBuffering;
@property(nonatomic, assign) jmethodID onPlaying;
@property(nonatomic, assign) jmethodID onPaused;
@property(nonatomic, assign) jmethodID onProgress;
@property(nonatomic, assign) jmethodID onEnded;
@property(nonatomic, assign) jmethodID onFailed;
@property(nonatomic, assign) jmethodID onInitializationFailed;
@property(nonatomic, strong) AVPlayer *player;
@property(nonatomic, strong) NSMutableArray<id> *observerTokens;
@property(nonatomic, strong) id timeObserverToken;
@property(nonatomic, strong) dispatch_queue_t commandQueue;
@property(nonatomic, copy) NSString *songId;
@property(nonatomic, assign) int64_t generation;
@property(nonatomic, assign) int64_t lastPositionMs;
@property(nonatomic, assign) BOOL released;
@end

@implementation KmpMacosAvFoundationBridge

- (instancetype)initWithEnv:(JNIEnv *)env callback:(jobject)callback {
    self = [super init];
    if (self == nil) {
        return nil;
    }
    env->GetJavaVM(&_javaVm);
    _callback = env->NewGlobalRef(callback);
    _commandQueue = dispatch_queue_create("com.yanhao.kmpmusic.macos.avfoundation.bridge", DISPATCH_QUEUE_SERIAL);
    dispatch_queue_set_specific(_commandQueue, KmpBridgeQueueKey, (__bridge void *)self, NULL);
    _player = [[AVPlayer alloc] init];
    _observerTokens = [NSMutableArray array];
    _songId = @"";
    _generation = 0;
    _lastPositionMs = 0;
    _released = NO;
    jclass callbackClass = env->GetObjectClass(callback);
    _onPrepared = env->GetMethodID(callbackClass, "onPrepared", "(JJZ)V");
    _onBuffering = env->GetMethodID(callbackClass, "onBuffering", "(JJJZ)V");
    _onPlaying = env->GetMethodID(callbackClass, "onPlaying", "(JJJZ)V");
    _onPaused = env->GetMethodID(callbackClass, "onPaused", "(JJJZ)V");
    _onProgress = env->GetMethodID(callbackClass, "onProgress", "(JJJZ)V");
    _onEnded = env->GetMethodID(callbackClass, "onEnded", "(J)V");
    _onFailed = env->GetMethodID(callbackClass, "onFailed", "(JILjava/lang/String;Ljava/lang/String;)V");
    _onInitializationFailed = env->GetMethodID(callbackClass, "onInitializationFailed", "(ILjava/lang/String;)V");
    env->DeleteLocalRef(callbackClass);
    if (_callback == nullptr || !_onPrepared || !_onPlaying || !_onProgress || !_onEnded || !_onFailed) {
        return nil;
    }
    return self;
}

- (void)performOnCommandQueueSync:(dispatch_block_t)block {
    if (dispatch_get_specific(KmpBridgeQueueKey) == (__bridge void *)self) {
        block();
        return;
    }
    dispatch_sync(self.commandQueue, block);
}

- (void)dealloc {
    [self releaseBridge];
    if (_callback != nullptr && _javaVm != nullptr) {
        BOOL didAttach = NO;
        JNIEnv *env = [self attachedEnv:&didAttach];
        if (env != nullptr) {
            env->DeleteGlobalRef(_callback);
        }
        [self detachIfNeeded:didAttach];
        _callback = nullptr;
    }
}

- (JNIEnv *)attachedEnv:(BOOL *)didAttach {
    *didAttach = NO;
    if (_javaVm == nullptr) {
        return nullptr;
    }
    JNIEnv *env = nullptr;
    jint result = _javaVm->GetEnv((void **)&env, JNI_VERSION_1_6);
    if (result == JNI_OK) {
        return env;
    }
    if (result == JNI_EDETACHED && _javaVm->AttachCurrentThread((void **)&env, nullptr) == JNI_OK) {
        *didAttach = YES;
        return env;
    }
    return nullptr;
}

- (void)detachIfNeeded:(BOOL)didAttach {
    if (didAttach && _javaVm != nullptr) {
        _javaVm->DetachCurrentThread();
    }
}

- (jint)prepareSongId:(NSString *)songId
             mediaUri:(NSString *)mediaUri
           generation:(int64_t)generation
      startPositionMs:(int64_t)startPositionMs {
    __block jint status = KMP_STATUS_ACCEPTED;
    [self performOnCommandQueueSync:^{
        if (self.released) {
            status = KMP_STATUS_ENGINE_UNAVAILABLE;
            return;
        }
        NSURL *url = [NSURL URLWithString:mediaUri];
        if (url == nil) {
            [self emitFailed:generation errorType:KMP_ERROR_UNSUPPORTED_FORMAT songId:songId message:@"macOS AVFoundation 无法解析音频 URI"];
            status = KMP_STATUS_UNSUPPORTED_FORMAT;
            return;
        }
        if (url.isFileURL && ![[NSFileManager defaultManager] fileExistsAtPath:url.path]) {
            [self emitFailed:generation errorType:KMP_ERROR_MISSING_FILE songId:songId message:@"macOS AVFoundation 找不到本地音频文件"];
            status = KMP_STATUS_MISSING_FILE;
            return;
        }
        [self removeObservers];
        self.generation = generation;
        self.songId = songId ?: @"";
        self.lastPositionMs = MAX(startPositionMs, 0);
        AVPlayerItem *item = [AVPlayerItem playerItemWithURL:url];
        [self installObserversForItem:item generation:generation songId:self.songId];
        [self.player replaceCurrentItemWithPlayerItem:item];
        [self installTimeObserverForItem:item generation:generation];
        if (self.lastPositionMs > 0) {
            [self.player seekToTime:CMTimeMake(self.lastPositionMs, 1000)];
        }
        [self emitPrepared:generation item:item];
    }];
    return status;
}

- (jint)playGeneration:(int64_t)generation {
    __block jint status = KMP_STATUS_ACCEPTED;
    [self performOnCommandQueueSync:^{
        if (![self isGenerationCurrent:generation]) {
            return;
        }
        [self.player play];
        [self emitPlaying:generation];
        [self emitProgress:generation];
    }];
    return status;
}

- (jint)pauseGeneration:(int64_t)generation {
    [self performOnCommandQueueSync:^{
        if (![self isGenerationCurrent:generation]) {
            return;
        }
        [self.player pause];
        [self emitPaused:generation];
    }];
    return KMP_STATUS_ACCEPTED;
}

- (jint)seekGeneration:(int64_t)generation positionMs:(int64_t)positionMs {
    [self performOnCommandQueueSync:^{
        if (![self isGenerationCurrent:generation]) {
            return;
        }
        self.lastPositionMs = MAX(positionMs, 0);
        __weak KmpMacosAvFoundationBridge *weakSelf = self;
        [self.player seekToTime:CMTimeMake(self.lastPositionMs, 1000) completionHandler:^(BOOL finished) {
            KmpMacosAvFoundationBridge *strongSelf = weakSelf;
            if (strongSelf == nil || !finished || ![strongSelf isGenerationCurrent:generation]) {
                return;
            }
            [strongSelf emitProgress:generation];
        }];
    }];
    return KMP_STATUS_ACCEPTED;
}

- (jint)stopGeneration:(int64_t)generation {
    [self performOnCommandQueueSync:^{
        if (![self isGenerationCurrent:generation]) {
            return;
        }
        [self.player pause];
        [self removeObservers];
        [self.player replaceCurrentItemWithPlayerItem:nil];
    }];
    return KMP_STATUS_ACCEPTED;
}

- (jint)setVolume:(float)volume {
    [self performOnCommandQueueSync:^{
        self.player.volume = MIN(MAX(volume, 0.0f), 1.0f);
    }];
    return KMP_STATUS_ACCEPTED;
}

- (jint)releaseBridge {
    [self performOnCommandQueueSync:^{
        if (self.released) {
            return;
        }
        self.released = YES;
        [self.player pause];
        [self removeObservers];
        [self.player replaceCurrentItemWithPlayerItem:nil];
    }];
    return KMP_STATUS_ACCEPTED;
}

- (void)installObserversForItem:(AVPlayerItem *)item generation:(int64_t)generation songId:(NSString *)songId {
    __weak KmpMacosAvFoundationBridge *weakSelf = self;
    id endedToken = [[NSNotificationCenter defaultCenter] addObserverForName:AVPlayerItemDidPlayToEndTimeNotification object:item queue:nil usingBlock:^(__unused NSNotification *notification) {
        KmpMacosAvFoundationBridge *strongSelf = weakSelf;
        if (strongSelf != nil && [strongSelf isGenerationCurrent:generation]) {
            [strongSelf emitEnded:generation];
        }
    }];
    id failedToken = [[NSNotificationCenter defaultCenter] addObserverForName:AVPlayerItemFailedToPlayToEndTimeNotification object:item queue:nil usingBlock:^(__unused NSNotification *notification) {
        KmpMacosAvFoundationBridge *strongSelf = weakSelf;
        if (strongSelf != nil && [strongSelf isGenerationCurrent:generation]) {
            [strongSelf emitFailed:generation errorType:KMP_ERROR_UNKNOWN songId:songId message:@"macOS AVFoundation 播放到结束前失败"];
        }
    }];
    [self.observerTokens addObject:endedToken];
    [self.observerTokens addObject:failedToken];
}

- (void)installTimeObserverForItem:(AVPlayerItem *)item generation:(int64_t)generation {
    [self removeTimeObserver];
    __weak KmpMacosAvFoundationBridge *weakSelf = self;
    self.timeObserverToken = [self.player addPeriodicTimeObserverForInterval:CMTimeMake(1, 4) queue:self.commandQueue usingBlock:^(__unused CMTime time) {
        KmpMacosAvFoundationBridge *strongSelf = weakSelf;
        if (strongSelf != nil && [strongSelf isGenerationCurrent:generation]) {
            [strongSelf emitProgress:generation item:item];
        }
    }];
}

- (void)removeObservers {
    for (id token in self.observerTokens) {
        [[NSNotificationCenter defaultCenter] removeObserver:token];
    }
    [self.observerTokens removeAllObjects];
    [self removeTimeObserver];
}

- (void)removeTimeObserver {
    if (self.timeObserverToken != nil) {
        [self.player removeTimeObserver:self.timeObserverToken];
        self.timeObserverToken = nil;
    }
}

- (BOOL)isGenerationCurrent:(int64_t)generation {
    return !self.released && generation == self.generation;
}

- (int64_t)currentPositionMs {
    Float64 seconds = CMTimeGetSeconds(self.player.currentTime);
    if (!isfinite(seconds)) {
        return self.lastPositionMs;
    }
    self.lastPositionMs = MAX((int64_t)(seconds * 1000.0), 0);
    return self.lastPositionMs;
}

- (void)emitPrepared:(int64_t)generation item:(AVPlayerItem *)item {
    int64_t durationMs = 0;
    BOOL hasDuration = KmpDurationMsFromItem(item, &durationMs);
    [self callPrepared:generation durationMs:durationMs hasDuration:hasDuration];
}

- (void)emitPlaying:(int64_t)generation {
    int64_t durationMs = 0;
    BOOL hasDuration = KmpDurationMsFromItem(self.player.currentItem, &durationMs);
    [self callTimed:self.onPlaying generation:generation positionMs:[self currentPositionMs] durationMs:durationMs hasDuration:hasDuration];
}

- (void)emitPaused:(int64_t)generation {
    int64_t durationMs = 0;
    BOOL hasDuration = KmpDurationMsFromItem(self.player.currentItem, &durationMs);
    [self callTimed:self.onPaused generation:generation positionMs:[self currentPositionMs] durationMs:durationMs hasDuration:hasDuration];
}

- (void)emitProgress:(int64_t)generation {
    [self emitProgress:generation item:self.player.currentItem];
}

- (void)emitProgress:(int64_t)generation item:(AVPlayerItem *)item {
    int64_t durationMs = 0;
    BOOL hasDuration = KmpDurationMsFromItem(item, &durationMs);
    [self callTimed:self.onProgress generation:generation positionMs:[self currentPositionMs] durationMs:durationMs hasDuration:hasDuration];
}

- (void)emitEnded:(int64_t)generation {
    BOOL didAttach = NO;
    JNIEnv *env = [self attachedEnv:&didAttach];
    if (env != nullptr && self.callback != nullptr) {
        env->CallVoidMethod(self.callback, self.onEnded, (jlong)generation);
    }
    [self detachIfNeeded:didAttach];
}

- (void)emitFailed:(int64_t)generation errorType:(jint)errorType songId:(NSString *)songId message:(NSString *)message {
    BOOL didAttach = NO;
    JNIEnv *env = [self attachedEnv:&didAttach];
    if (env != nullptr && self.callback != nullptr) {
        jstring javaSongId = songId.length == 0 ? nullptr : env->NewStringUTF(songId.UTF8String);
        jstring javaMessage = env->NewStringUTF((message ?: @"macOS AVFoundation 播放失败").UTF8String);
        env->CallVoidMethod(self.callback, self.onFailed, (jlong)generation, errorType, javaSongId, javaMessage);
        if (javaSongId != nullptr) {
            env->DeleteLocalRef(javaSongId);
        }
        env->DeleteLocalRef(javaMessage);
    }
    [self detachIfNeeded:didAttach];
}

- (void)callPrepared:(int64_t)generation durationMs:(int64_t)durationMs hasDuration:(BOOL)hasDuration {
    BOOL didAttach = NO;
    JNIEnv *env = [self attachedEnv:&didAttach];
    if (env != nullptr && self.callback != nullptr) {
        env->CallVoidMethod(self.callback, self.onPrepared, (jlong)generation, (jlong)durationMs, (jboolean)hasDuration);
    }
    [self detachIfNeeded:didAttach];
}

- (void)callTimed:(jmethodID)method generation:(int64_t)generation positionMs:(int64_t)positionMs durationMs:(int64_t)durationMs hasDuration:(BOOL)hasDuration {
    BOOL didAttach = NO;
    JNIEnv *env = [self attachedEnv:&didAttach];
    if (env != nullptr && self.callback != nullptr && method != nullptr) {
        env->CallVoidMethod(self.callback, method, (jlong)generation, (jlong)positionMs, (jlong)durationMs, (jboolean)hasDuration);
    }
    [self detachIfNeeded:didAttach];
}

@end

static KmpMacosAvFoundationBridge *KmpBridgeFromHandle(jlong handle) {
    if (handle == 0) {
        return nil;
    }
    return (__bridge KmpMacosAvFoundationBridge *)(void *)handle;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_yanhao_kmpmusic_playback_MacosAvFoundationNativeBindings_create(JNIEnv *env, jclass, jobject callback) {
    @autoreleasepool {
        KmpMacosAvFoundationBridge *bridge = [[KmpMacosAvFoundationBridge alloc] initWithEnv:env callback:callback];
        if (bridge == nil) {
            return 0;
        }
        return (jlong)(__bridge_retained void *)bridge;
    }
}

extern "C" JNIEXPORT jint JNICALL Java_com_yanhao_kmpmusic_playback_MacosAvFoundationNativeBindings_prepare(JNIEnv *env, jclass, jlong handle, jstring songId, jstring mediaUri, jlong generation, jlong startPositionMs) {
    KmpMacosAvFoundationBridge *bridge = KmpBridgeFromHandle(handle);
    if (bridge == nil) {
        return KMP_STATUS_ENGINE_UNAVAILABLE;
    }
    return [bridge prepareSongId:KmpStringFromJString(env, songId) mediaUri:KmpStringFromJString(env, mediaUri) generation:generation startPositionMs:startPositionMs];
}

extern "C" JNIEXPORT jint JNICALL Java_com_yanhao_kmpmusic_playback_MacosAvFoundationNativeBindings_play(JNIEnv *, jclass, jlong handle, jlong generation) {
    KmpMacosAvFoundationBridge *bridge = KmpBridgeFromHandle(handle);
    return bridge == nil ? KMP_STATUS_ENGINE_UNAVAILABLE : [bridge playGeneration:generation];
}

extern "C" JNIEXPORT jint JNICALL Java_com_yanhao_kmpmusic_playback_MacosAvFoundationNativeBindings_pause(JNIEnv *, jclass, jlong handle, jlong generation) {
    KmpMacosAvFoundationBridge *bridge = KmpBridgeFromHandle(handle);
    return bridge == nil ? KMP_STATUS_ENGINE_UNAVAILABLE : [bridge pauseGeneration:generation];
}

extern "C" JNIEXPORT jint JNICALL Java_com_yanhao_kmpmusic_playback_MacosAvFoundationNativeBindings_seekTo(JNIEnv *, jclass, jlong handle, jlong generation, jlong positionMs) {
    KmpMacosAvFoundationBridge *bridge = KmpBridgeFromHandle(handle);
    return bridge == nil ? KMP_STATUS_ENGINE_UNAVAILABLE : [bridge seekGeneration:generation positionMs:positionMs];
}

extern "C" JNIEXPORT jint JNICALL Java_com_yanhao_kmpmusic_playback_MacosAvFoundationNativeBindings_stop(JNIEnv *, jclass, jlong handle, jlong generation) {
    KmpMacosAvFoundationBridge *bridge = KmpBridgeFromHandle(handle);
    return bridge == nil ? KMP_STATUS_ENGINE_UNAVAILABLE : [bridge stopGeneration:generation];
}

extern "C" JNIEXPORT jint JNICALL Java_com_yanhao_kmpmusic_playback_MacosAvFoundationNativeBindings_setVolume(JNIEnv *, jclass, jlong handle, jfloat volume) {
    KmpMacosAvFoundationBridge *bridge = KmpBridgeFromHandle(handle);
    return bridge == nil ? KMP_STATUS_ENGINE_UNAVAILABLE : [bridge setVolume:volume];
}

extern "C" JNIEXPORT jint JNICALL Java_com_yanhao_kmpmusic_playback_MacosAvFoundationNativeBindings_release(JNIEnv *, jclass, jlong handle) {
    if (handle == 0) {
        return KMP_STATUS_ACCEPTED;
    }
    KmpMacosAvFoundationBridge *bridge = (__bridge_transfer KmpMacosAvFoundationBridge *)(void *)handle;
    return [bridge releaseBridge];
}
