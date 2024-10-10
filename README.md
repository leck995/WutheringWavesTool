## 简介
***
WutheringWavesTool是一款鸣潮的小工具，日常使用可以替代原生的启动器，同时提供一些有用的功能。需要注意的时，该助手若要有良好的体验，需要登录库街区(程序内添加账号时有具体教程)

[下载地址](https://github.com/leck995/WutheringWavesTool/releases)

## 功能
___
1. 替代原生的启动器，同时显示基本用户数据，包括不限于每日体力，任务电台，各种宝箱数量。
2. 库街区签到，最大支持9个账号同时签到；签到统计，可以查看签到一共获取了多少物品。
3. 抽卡分析，提供简单的抽卡分析功能，支持详情和缩略两种显示模式。
4. 游戏时长统计，只要使用本助手启动鸣潮，即可如同steam一样统计每日游玩时长与总时长(推荐，计划会推出年度总结)。
5. 查看角色，可以查看拥有的角色以及详情，包括共鸣连，等级，武器，声骸等等
6. WeGame版本支持（不保证，目前缺乏有效反馈）

注：国际服目前只能使用抽卡分析和游玩时长统计两个功能；

## 运行截图
***
![image.png](https://github.com/leck995/WutheringWavesTool/blob/new-ui/temp/01.png)

![image.png](https://github.com/leck995/WutheringWavesTool/blob/new-ui/temp/02.png)

![image.png](https://github.com/leck995/WutheringWavesTool/blob/new-ui/temp/03.png)

![image.png](https://github.com/leck995/WutheringWavesTool/blob/new-ui/temp/04.png)

![image.png](https://github.com/leck995/WutheringWavesTool/blob/new-ui/temp/05.png)
![image.png](https://github.com/leck995/WutheringWavesTool/blob/new-ui/temp/06.png)
## 教程


***
软件启动先在设置中设置鸣潮游戏目录（类似于“D:\Game\Wuthering Waves”）,这是最优先的一步。

#### 关于游戏时长统计
只要使用本软件启动游戏，将自动统计鸣潮游玩时间。

#### 关于签到
签到需要手动添加:库街区ID、鸣潮ID以及库街区Token，Token获取请前往 [库街区](https://wiki.kurobbs.com/mc/home) ，登录后获取，具体获取Token请善用搜索引擎（现内置简易教程）。

添加账号请务必设置一个主账号，所有的账号相关界面默认会使用主账号信息，从而获取游戏内相关数据

签到最大支持9个账号同时签到(我想普通玩家玩家不可能有9个以上的账号)


#### 关于抽卡分析
首次使用，请先打开鸣潮游戏中的抽卡历史界面，否则无法获取到抽卡数据。

软件启动先在设置中设置鸣潮游戏目录（类似于“D:\Game\Wuthering Waves”），设置完毕后进入到抽卡分析界面，点击获取，即可获取抽卡信息。
后续对于该账号的数据获取可直接点击刷新即可。

注：如果仅需要抽卡分析功能，可在设置中将抽卡分析设置为启动页。


## 问题解答
### 注意，教程可能部分存在过时,可前往[WIKI](https://github.com/leck995/WutheringWavesTool/wiki)结合使用
***
#### 1. 提示无法获取抽卡数据连接
* 确保游戏安装根目录正确，获取前打开游戏的抽卡历史界面。
* 如果曾经修改过游戏配置，例如解锁帧数，查看WutheringWaves\Wuthering Waves Game\Client\Saved\Config\WindowsNoEditor的Engine.ini，寻找并移除global=none
#### 2. 已经添加账号且设置为主账号，却仍提示账号不存在
可尝试重启助手，如发现可以复现，请提交issues
#### 3. 助手内所有的文本均乱码或字体异常
检查操作系统是否修改过默认字体，可下载带有font标志的安装包进行使用



## 感谢与支持
***
### 感谢以下开源项目
* [OpenJFX](https://openjfx.io/)
* [Nfx-lib](https://github.com/xdsswar/nfx-lib)
* [Controlsfx](https://github.com/controlsfx/controlsfx)
* [MvvmFX](https://github.com/sialcasa/mvvmFX)
* [Jnativehook](https://github.com/kwhat/jnativehook)
* [Thumbnailator](https://github.com/coobird/thumbnailator)
* [Jackson](https://github.com/FasterXML/jackson)
* [JNA](https://github.com/java-native-access/jna)
* [Atlantafx](https://github.com/mkpaz/atlantafx)
* [Ikonli](https://github.com/kordamp/ikonli)
* [Sqlite-jdbc](https://github.com/xerial/sqlite-jdbc)
* [Collapse](https://github.com/CollapseLauncher/Collapse)

项目背景作者为[Rafa](https://www.pixiv.net/artworks/120767239)，感谢。

### 赞助
倘若喜欢本程序，欢迎支持一下开发者，您的支持会加快软件的开发进度。

您可以按以下格式留言：[名称]:[想说的话]

![image.png](https://github.com/leck995/WutheringWavesTool/blob/new-ui/temp/99.png)
<style>
  p {text-indent: 2em;}
</style>
