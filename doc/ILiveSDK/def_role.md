## 默认配置


[腾讯云控制台](https://console.cloud.tencent.com/rav)可以按平台，配置自己定制的音视频参数(角色)。sdk会在登录成功时从后台拉取这个配置。

但是在网络不理想的情况下，(首次)拉配置失败后，将面临进房找不到角色的问题，这里推荐使用默认角色来进行配置：

### 1.配置默认角色

* 功能说明：极端情况下，登录时有可能会拉不到控制台spear配置，且本地没有缓存的spear配置时，此时进房会使用SDK自带的默认配置，此时可能与业务侧期望的行为不一致，此时需要单独配置;
* 接口：ilivesdk 1.9.6之后版本，在原登录接口中添加可选参数用于配置默认角色(进房角色不存在时将使用默认角色)

```
// Android接口如下：
/**
 * iLiveSDK 登录(配置默认角色配置)
 *
 * @param id 用户id
 * @param sig 用户密钥
 * @param roleCfg 默认角色配置(拉不到网络配置时使用此角色)
 */
public void iLiveLogin(final String id, String sig, String roleCfg, ILiveCallBack tilvbLoginListener);


// iOS/macOS接口如下：
/**
 网络不稳定，拉取spear配置经常失败时，才会用到本接口(一般适用于海外)
 */
- (void)iLiveLogin:(NSString *)uid sig:(NSString *)sig spearCfg:(NSString *)roleCfg succ:(TCIVoidBlock)succ failed:(TCIErrorBlock)failed;
```


参数|类型|描述
--:|:--:|:--
userId|String|用户标识
userSig|String|用户签名
roleCfg|String|角色配置

* `roleCfg `获取：根据 <a href="#getSpear">如何获取Spear配置</a> 中的操作后，拉到业务自身的spear json串后，在json中找到需要默认角色如`LiveMaster`，字符串内容如下，将其作为参数传入到`roleCfg`即可;

```
{
    "audio":{
        "aec":1,
        "agc":0,
        "ans":1,
        "anti_dropout":0,
        "au_scheme":1,
        "channel":2,
        "codec_prof":4106,
        "frame":40,
        "kbps":24,
        "max_antishake_max":1000,
        "max_antishake_min":400,
        "min_antishake":120,
        "sample_rate":48000,
        "silence_detect":0
    },
    "is_default":1,
    "net":{
        "rc_anti_dropout":1,
        "rc_init_delay":100,
        "rc_max_delay":500
    },
    "role":"LiveMaster",
    "type":1,
    "video":{
        "anti_dropout":0,
        "codec_prof":5,
        "format":-2,
        "format_fix_height":480,
        "format_fix_width":640,
        "format_max_height":-1,
        "format_max_width":-1,
        "fps":15,
        "fqueue_time":-1,
        "live_adapt":0,
        "maxkbps":400,
        "maxqp":-1,
        "minkbps":400,
        "minqp":-1,
        "qclear":1,
        "small_video_upload":0
    }
}
```

### 2.从本地Spear配置启动

* 功能说明：默认情况下，登录是到服务器拉取Spear配置，其有可能会失败; 此时即需要从本地进行加载spear配置;
* 接口如下：

```
// Android
/**
 * iLiveSDK 登录(配置自定义Spear配置，忽略Spear后台配置)
 * http://conf.voice.qcloud.com/index.php?sdk_appid=[sdkappid]&interface=Voice_Conf_Download&platform=1
 *
 * @param id 用户id
 * @param sig 用户密钥
 * @param spearCfg 自定义spear配置
 */
public void iLiveLoginWithSpear(final String id, String sig, String spearCfg, ILiveCallBack tilvbLoginListener);


// iOS/macOS接口如下：
/**
 * 登录（配置自定spear配置，忽略spear后台配置）
 * @param uid      用户id
 * @param sig      用户签名
 * @param config   自定义spear配置（从 http://conf.voice.qcloud.com/index.php?sdk_appid=sdkappid&interface=Voice_Conf_Download&platform=1(platform:1 ios,2 android) 获取）
 * @param succ     成功回调
 * @param failed   失败回调
 */
- (void)iLiveLoginWithCustomSpearCfg:(NSString *)spearCfg uid:(NSString *)uid sig:(NSString *)sig  succ:(TCIVoidBlock)succ failed:(TCIErrorBlock)failed;

```

参数|类型|描述
--:|:--:|:--
userId|String|用户标识
userSig|String|用户签名
spearCfg|String|自定义配置信息

* `spearCfg`获取：根据 <a href="#getSpear">如何获取Spear配置</a> 中的操作后，拉到业务自身的spear json串后，将json字符串作为参数传入到`spearCfg `即可;

同时在[统一事件回调](EventListener.md)中添加onSetSpearConfigEvent上抛配置是否成功


### <a name="getSpear"></a>3.如何获取Spear配置

**这里不推荐用户自行填写配置参数**，即便是打算自定义配置，也建议在Spear后台配置好后，通过以下地址获取:
```
http://conf.voice.qcloud.com/index.php?sdk_appid=[sdkappid]&interface=Voice_Conf_Download&platform=[platform]
```

* sdkappid 为用户自己的应用标识
* platform 为对应的平台类型((0 : pc/web, 1 : ios, 2 : android,  4: mac)

例如sdkappid为1400049564的iOS平台配置可通过下面地址获取(可以直接浏览器打开):
```
http://conf.voice.qcloud.com/index.php?sdk_appid=1400049564&interface=Voice_Conf_Download&platform=1
```

拉到的配置如下，**请勿自行修改其中的参数**

```
{
    "data":{
        "biz_id":1400049564,
        "conf":[
            {
                "audio":{
                    "aec":1,
                    "agc":0,
                    "ans":1,
                    "anti_dropout":0,
                    "au_scheme":1,
                    "channel":2,
                    "codec_prof":4106,
                    "frame":40,
                    "kbps":24,
                    "max_antishake_max":1000,
                    "max_antishake_min":400,
                    "min_antishake":120,
                    "sample_rate":48000,
                    "silence_detect":0
                },
                "is_default":1,
                "net":{
                    "rc_anti_dropout":1,
                    "rc_init_delay":100,
                    "rc_max_delay":500
                },
                "role":"LiveMaster",
                "type":1,
                "video":{
                    "anti_dropout":0,
                    "codec_prof":5,
                    "format":-2,
                    "format_fix_height":480,
                    "format_fix_width":640,
                    "format_max_height":-1,
                    "format_max_width":-1,
                    "fps":15,
                    "fqueue_time":-1,
                    "live_adapt":0,
                    "maxkbps":400,
                    "maxqp":-1,
                    "minkbps":400,
                    "minqp":-1,
                    "qclear":1,
                    "small_video_upload":0
                }
            },
            {
                "audio":{
                    "aec":1,
                    "agc":0,
                    "ans":1,
                    "anti_dropout":0,
                    "au_scheme":1,
                    "channel":2,
                    "codec_prof":4106,
                    "frame":40,
                    "kbps":24,
                    "max_antishake_max":1000,
                    "max_antishake_min":400,
                    "min_antishake":120,
                    "sample_rate":48000,
                    "silence_detect":0
                },
                "is_default":0,
                "net":{
                    "rc_anti_dropout":1,
                    "rc_init_delay":500,
                    "rc_max_delay":1000
                },
                "role":"Guest",
                "type":2,
                "video":{
                    "anti_dropout":0,
                    "codec_prof":5,
                    "format":-2,
                    "format_fix_height":480,
                    "format_fix_width":640,
                    "format_max_height":-1,
                    "format_max_width":-1,
                    "fps":15,
                    "fqueue_time":-1,
                    "live_adapt":0,
                    "maxkbps":400,
                    "maxqp":-1,
                    "minkbps":400,
                    "minqp":-1,
                    "qclear":1,
                    "small_video_upload":0
                }
            },
            {
                "audio":{
                    "aec":1,
                    "agc":0,
                    "ans":1,
                    "anti_dropout":0,
                    "au_scheme":1,
                    "channel":2,
                    "codec_prof":4106,
                    "frame":40,
                    "kbps":24,
                    "max_antishake_max":1000,
                    "max_antishake_min":400,
                    "min_antishake":120,
                    "sample_rate":48000,
                    "silence_detect":0
                },
                "is_default":0,
                "net":{
                    "rc_anti_dropout":1,
                    "rc_init_delay":100,
                    "rc_max_delay":500
                },
                "role":"LiveGuest",
                "type":3,
                "video":{
                    "anti_dropout":0,
                    "codec_prof":5,
                    "format":-2,
                    "format_fix_height":480,
                    "format_fix_width":640,
                    "format_max_height":-1,
                    "format_max_width":-1,
                    "fps":15,
                    "fqueue_time":-1,
                    "live_adapt":0,
                    "maxkbps":400,
                    "maxqp":-1,
                    "minkbps":400,
                    "minqp":-1,
                    "qclear":1,
                    "small_video_upload":0
                }
            }
        ],
        "platform":1,
        "scheme":1,
        "sequence":20
    },
    "errmsg":"success.",
    "retcode":0
}

```

