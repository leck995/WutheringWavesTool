# KR Launcher Updater

鸣潮（Wuthering Waves）游戏启动器资源更新流程的 Java 复刻实现。本项目基于官方 C# 启动器的更新逻辑，使用 Java 21 重新实现了完整的游戏资源检查、下载、补丁应用、文件移动与冗余清理流程。

## 功能

- **更新检查**：向服务器查询最新版本，对比本地版本，判断是否需要更新
- **资源下载**：支持单文件下载和分块（chunk）下载，支持 HTTP Range 断点续传
- **CDN 轮换重试**：下载失败时自动轮换备用 CDN，每个文件/chunk 独立计数重试
- **MD5 校验**：下载完成后校验文件 MD5，支持三种失败路径（计算异常/不匹配/超过重试次数）
- **补丁应用**：通过 HPatchZ.exe 应用二进制差分补丁（.krdiff），支持进度回调和退出码处理
- **文件移动**：将下载/补丁产物从缓存目录移动到游戏目录
- **冗余清理**：删除旧版本不再需要的文件，同步更新本地资源记录
- **进程中断恢复**：支持在下载、移动、修复阶段被杀死后的断点恢复
- **预下载**：支持在游戏运行期间预先下载新版本资源
- **修复模式**：当文件移动失败时进入修复状态，下次启动自动修复

## 环境要求

| 依赖     | 版本                                            |
| -------- | ----------------------------------------------- |
| JDK      | 21+                                             |
| Maven    | 3.6+                                            |
| 操作系统 | Windows（依赖 hpatchz.exe 和 Windows 共享内存） |

### Maven 依赖

- **Gson 2.11.0** — JSON 序列化/反序列化
- **Apache HttpClient 5 5.6.1** — HTTP 请求
- **SLF4J 2.0.16 + Logback 1.5.13** — 日志
- **JNA 5.14.0** — Windows 原生 API 调用（进程管理、共享内存）

## 编译指南

### 编译

```bash
mvn compile
```

### 打包

```bash
mvn package -DskipTests
```

打包后 `target/` 目录包含：

```
target/
├── kr-launcher-updater-1.0-SNAPSHOT.jar   # 包含所有依赖的 fat JAR
├── original-kr-launcher-updater-1.0-SNAPSHOT.jar  # 原始 JAR（无依赖）
├── hpatchz.exe                             # 自动复制的补丁工具
└── KRApp.conf                              # 自动复制的配置文件
```

`pom.xml` 中配置了 `maven-resources-plugin`，在 `package` 阶段自动将项目根目录的 `hpatchz.exe` 和 `KRApp.conf` 复制到 `target/` 目录，确保 JAR、补丁工具和配置文件在同一路径下。

## 使用方法

### 命令行

```bash
java -jar kr-launcher-updater-1.0-SNAPSHOT.jar [config-path] [game-dir]
```

| 参数          | 说明                                                                                                                        |
| ------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `config-path` | 配置文件路径。支持两种格式：`KRApp.conf`（Base64+XOR 编码）或 JSON 配置文件。若省略，自动使用 JAR 同级目录下的 `KRApp.conf` |
| `game-dir`    | 游戏目录路径（绝对或相对路径）。若省略，在 JAR 同级目录下创建 `Wuthering Waves Game` 子目录作为游戏目录                     |

无参数运行时，程序会自动：

1. 在 JAR 所在目录查找 `KRApp.conf`
2. 在 JAR 所在目录创建 `Wuthering Waves Game` 子目录作为游戏目录

### 配置文件

#### config.json（JSON 格式）

```json
{
  "configUrl": "https://example.com/game/config",
  "backUpConfigUrl": "https://backup.example.com/game/config",
  "gameId": "test",
  "appId": "test",
  "appKey": "test",
  "gameExeName": "Client.exe",
  "gameDirName": "Wuthering Waves"
}
```

#### KRApp.conf（编码格式）

官方启动器使用的配置格式，经 Base64 + XOR 99 编码。程序会自动解码并解析。项目根目录自带一份从游戏客户端提取的 `KRApp.conf`，打包时自动复制到 JAR 同级目录。

### 运行示例

```bash
# 无参数：自动使用 JAR 同级目录的 KRApp.conf，创建 Wuthering Waves Game 目录
java -jar kr-launcher-updater.jar

# 指定 KRApp.conf 和游戏目录
java -jar kr-launcher-updater.jar KRApp.conf "C:\Games\Wuthering Waves"

# 指定 config.json
java -jar kr-launcher-updater.jar config.json "C:\Games\Wuthering Waves"

# 仅指定配置文件，游戏目录自动创建在 JAR 同级目录
java -jar kr-launcher-updater.jar KRApp.conf
```

### 日志

日志同时输出到控制台和文件 `launcher-updater.log`（按天滚动，保留 7 天）。日志级别配置在 `src/main/resources/logback.xml` 中。

## 更新流程

程序启动后的完整更新流程：

```
1. CheckUpdate（检查更新）
   ├── 获取服务器配置
   ├── 对比本地/服务器版本
   └── 判断状态：UP_TO_DATE / NEED_DOWNLOAD / DOWNLOADING / REPAIRING / ROLLBACK
       │
2. UpdateFlow（执行更新）                                    │
   ├── Prepare（准备阶段）                                   │
   │   ├── 下载索引文件                                      │
   │   ├── 检查本地已有文件（大小+MD5）                      │
   │   ├── 构建下载列表                                      │
   │   └── 检测中断恢复（downloading/moving/repairing 状态） │
   │                                                         │
   ├── Download（下载阶段）                                  │
   │   ├── 单文件下载（带 Range 续传 + CDN 轮换重试）        │
   │   ├── 分块下载（每 chunk 独立重试 + 合并 + MD5 校验）   │
   │   └── 下载完成后 MD5 校验（三种失败路径处理）           │
   │                                                         │
   ├── Apply（应用阶段）                                     │
   │   ├── KillProcess（杀游戏进程）                         │
   │   ├── PatchApply（HPatchZ 补丁应用 + MD5 校验）         │
   │   │   └── 失败时降级为重新下载                          │
   │   └── NopApply（无补丁时直接移动）                      │
   │                                                         │
   ├── Move（移动阶段）                                      │
   │   ├── 保存 STATE_MOVING 配置                            │
   │   ├── MoveFileTask（移动文件到游戏目录）                │
   │   └── 失败时保存 STATE_REPAIRING                        │
   │                                                         │
   └── Cleanup（清理阶段）                                   │
       ├── DeleteRedundantFiles（删除冗余文件）              │
       ├── 更新 GameResourceRecord                           │
       └── 删除缓存目录                                      │
```

## 进度显示

程序运行时会在控制台输出详细的进度信息，覆盖下载、应用、校验、移动、删除各阶段。所有日志同时写入 `launcher-updater.log` 文件。

### 进度状态

更新流程通过 6 个状态号报告进度：

| 状态号 | 名称        | 含义                                   | 显示字段                            |
| ------ | ----------- | -------------------------------------- | ----------------------------------- |
| 1      | DOWNLOAD    | 整体下载进度                           | 已下载/总大小 (百分比) \| 速度/s, ETA |
| 2      | APPLY       | HPatchZ 补丁应用进度                   | 已打补丁/总字节 (百分比) \| 已处理/总文件数 |
| 3      | VERIFY      | 补丁后 MD5 校验进度                    | 已校验/总字节 (百分比)              |
| 4      | RE_DOWNLOAD | 补丁失败文件的重下载进度               | 同 DOWNLOAD                         |
| 5      | MOVE        | 文件移动进度（从缓存移到游戏目录）     | 已处理/总文件数 (百分比)            |
| 6      | DELETE      | 冗余文件删除进度                       | 已处理/总文件数 (百分比)            |

### 下载速度与 ETA 算法

下载速度采用 **1 秒滑动窗口 + 缓存**算法，与官方 C# 启动器的 `KRSpeedMonitor` 保持逻辑一致：

- 首次回调：初始化基准时间和字节，返回速度 0
- 距上次计算 > 1000ms：重新计算 `speed = deltaBytes / elapsedSeconds`，更新基准；速度 > 0 时缓存，否则返回 0
- 距上次计算 ≤ 1000ms：返回上次缓存的速度（避免高频回调导致速度抖动）

ETA = 剩余字节 / 速度，最大值封顶为 `359999` 秒（约 100 小时），对应 C# `KRUpdateProgressInfo.MAX_REMAINING_TIME`。

### 文件级与分块级日志

除总体进度外，下载引擎还输出文件级和分块级详细日志：

| 日志标签       | 说明                                                     |
| -------------- | -------------------------------------------------------- |
| `[FILE START]` | 单个文件开始下载（文件序号、URL、大小、chunk 数）        |
| `[FILE DONE]`  | 单个文件下载完成（耗时）                                 |
| `[FILE FAILED]`| 单个文件下载失败（耗时）                                 |
| `[SINGLE DL]`  | 单文件下载开始（文件名、大小）                           |
| `[CHUNK DL]`   | 分块下载开始（文件名、chunk 总数、总大小）               |
| `[CHUNK DL]`   | 分块下载进度（每累计 1% 输出一次，或每 chunk 完成时）    |
| `[MERGE]`      | 分块合并开始（文件名、chunk 数）                         |

分块进度按 **1% 间隔**输出：`int threshold = Math.max(1, chunks.size() / 100)`。若单次下载的 chunk 数本身就小于总 chunk 数的 1%，则每完成一个 chunk 都会汇报。

### 终端输出示例

```
02:57:10.123 [CDNDownload-worker] INFO  c.k.l.download.DownloadTask - [SINGLE DL] config.json | 2.3 KB
02:57:10.456 [CDNDownload-worker] INFO  c.k.l.download.CDNDownloadTask - [FILE START] [1/86] https://cdn.example.com/.../pakchunk0.pak (1.2 GB, 12 chunks)
02:57:11.789 [CDNDownload-worker] INFO  c.k.l.download.DownloadTask - [CHUNK DL] pakchunk0-WindowsNoEditor.pak | 12 chunks, 1.2 GB total
02:57:13.001 [main] INFO  com.kr.launcher.Main - [DOWNLOAD] 1.2 GB/104.5 GB (1%) | 30.5 MB/s, ETA 57m14s
02:57:14.002 [CDNDownload-worker] INFO  c.k.l.download.DownloadTask - [CHUNK DL] pakchunk0-WindowsNoEditor.pak | 1/12 chunks (8%)
02:57:15.003 [main] INFO  com.kr.launcher.Main - [DOWNLOAD] 2.5 GB/104.5 GB (2%) | 31.2 MB/s, ETA 55m32s
02:57:20.123 [CDNDownload-worker] INFO  c.k.l.download.DownloadTask - [MERGE] pakchunk0-WindowsNoEditor.pak | merging 12 chunks
02:57:22.456 [CDNDownload-worker] INFO  c.k.l.download.CDNDownloadTask - [FILE DONE] [1/86] ... in 12.3s

03:15:45.789 [main] INFO  com.kr.launcher.Main - [APPLY] 1.2 GB/27.8 GB (4%) | 12/285 files
03:18:20.123 [main] INFO  com.kr.launcher.Main - [VERIFY] 5.6 GB/27.8 GB (20%)
03:25:10.456 [main] INFO  com.kr.launcher.Main - [MOVE] 45/285 files (15%)
03:26:30.789 [main] INFO  com.kr.launcher.Main - [DELETE] 3/10 files (30%)
```

> 注：日志中使用 ASCII 字符 `|` 而非 Unicode `—`，以兼容 Windows 非 UTF-8 终端（GBK/CP936）。

## 代码结构

```
src/main/java/com/kr/launcher/
├── Main.java                     # 程序入口，解析参数，启动更新流程
│
├── config/                       # 配置管理
│   ├── KRAppConfLoader.java      #   KRApp.conf 解码与加载（Base64+XOR）
│   ├── LauncherDownloadConfigHelper.java  # 下载配置读写
│   └── ResourceConfigManager.java #   资源配置管理器（游戏目录、缓存目录等）
│
├── flow/                         # 更新流程编排
│   ├── CheckUpdateFlow.java      #   检查更新流程（版本对比、状态判断）
│   ├── UpdateFlow.java           #   核心更新流程（下载→应用→移动→清理）
│   ├── RepairFlow.java           #   修复流程（文件校验与修复）
│   ├── PredownloadFlow.java      #   预下载流程
│   ├── ResUpdateModule.java      #   更新模块生命周期管理
│   └── ResCheckFlow.java         #   资源检查流程
│
├── download/                     # 下载引擎
│   ├── DownloadTask.java         #   核心下载任务（单文件+分块，重试，MD5校验）
│   ├── CDNDownloadTask.java      #   CDN 下载任务（带速度限制）
│   ├── ResourcesDownloadTask.java #   资源下载任务（包装 CDNDownloadTask）
│   ├── RetryHelper.java          #   重试策略（区分可重试/不可重试错误）
│   ├── DownloadTaskBuilder.java  #   下载任务构建器
│   ├── DownloadError.java        #   下载错误码定义
│   └── ...                       #   事件参数类
│
├── patch/                        # 补丁应用
│   ├── PatchExecutor.java        #   HPatchZ 进程管理（启动/进度/退出码）
│   ├── SharedMemory.java         #   Windows 共享内存（与 HPatchZ 通信）
│   └── PatchStartResult.java     #   启动结果
│
├── task/                         # 更新任务
│   ├── PrepareTask.java          #   准备任务（索引下载、文件检查、中断恢复）
│   ├── PatchApplyTask.java       #   补丁应用任务（HPatchZ + MD5 校验）
│   ├── ZipApplyTask.java         #   ZIP 解压任务
│   ├── NopApplyTask.java         #   空应用任务（无补丁时直接移动）
│   ├── GroupApplyTask.java       #   分组应用任务
│   ├── MoveFileTask.java         #   文件移动任务
│   ├── CheckFileTask.java        #   文件检查任务
│   ├── DirectoryCheckTask.java   #   目录检查任务
│   ├── FileChunkCheckTask.java   #   分块文件检查任务
│   └── ApplyTask.java            #   应用任务基类
│
├── model/                        # 数据模型
│   ├── UpdateInfo.java           #   更新信息（版本、CDN列表、大小等）
│   ├── IndexFile.java            #   索引文件（资源列表、补丁信息、删除列表）
│   ├── FileInfo.java             #   文件信息（路径、大小、MD5）
│   ├── ChunkInfo.java            #   分块信息（起始、结束、MD5）
│   ├── MixedFileInfo.java        #   混合文件信息（补丁条目）
│   ├── DownloadInfo.java         #   下载信息
│   ├── MoveFileRecord.java       #   移动文件记录
│   ├── LauncherDownloadConfig.java #   下载配置（状态、版本）
│   ├── GameResourceRecord.java   #   本地资源记录
│   ├── ResStateInfo.java         #   资源状态信息
│   ├── UpdateResult.java         #   更新结果
│   ├── CdnConfig.java            #   CDN 配置
│   └── ...                       #   其他模型类
│
└── util/                         # 工具类
    ├── HttpUtils.java            #   HTTP 请求工具
    ├── FileUtils.java            #   文件操作工具
    ├── MD5Utils.java             #   MD5 计算（含抛异常变体）
    ├── PathUtils.java            #   路径处理工具
    ├── GameProcessUtils.java     #   游戏进程管理（查找/杀死）
    ├── ExceptionUtils.java       #   异常→HRESULT 映射
    ├── ResourceHelper.java       #   资源辅助方法
    ├── ZipUtils.java             #   ZIP 解压工具
    ├── JsonUtils.java            #   JSON 序列化工具
    ├── ByteUtils.java            #   字节格式化工具
    ├── VersionUtils.java         #   版本比较工具
    ├── UrlUtils.java             #   URL 处理工具
    └── ...                       #   其他工具类

src/main/resources/
├── config.json                   # 默认配置模板
└── logback.xml                   # 日志配置
```

## 关键设计

### CDN 重试策略

下载失败时按 `_currentRetryCount % backUpUrls.size()` 轮换 CDN。`backUpUrls` 列表构造为 `[backup1, backup2, ..., backupN, primary]`，重试序列为：primary → backup1 → backup2 → ... → backupN → primary → ...

- **单文件**：使用 `currentRetryCount` 字段计数
- **单 chunk**：每个 chunk 有独立的局部 `chunkRetry` 计数器
- **外层重试**（合并 MD5 失败）：所有 chunk 重置为 primary URL，外层 `outerRetry` 计数

### MD5 校验三路径

下载完成后的 MD5 校验有三种失败路径，每种有不同的重试行为：

1. **MD5 计算异常** → `CanRetry(hresult)` 检查：不可重试 → FAILED；可重试 → 当作不匹配
2. **MD5 不匹配** → 直接重试（不检查 CanRetry，仅检查重试次数）
3. **超过重试次数** → FAILED

### 进程中断恢复

通过配置文件和文件系统状态检测中断：

| 中断阶段 | 检测方式                          | 恢复行为                   |
| -------- | --------------------------------- | -------------------------- |
| 下载中   | 下载配置文件存在 + chunk 目录存在 | 复用已下载 chunk，继续下载 |
| 合并中   | 主文件大小不匹配 + chunk 目录存在 | 复用好 chunk，重新合并     |
| 移动中   | 配置 `state=moving`               | 获取 ORIGIN 索引，重新移动 |
| 移动失败 | 配置 `state=repairing`            | 进入修复流程               |

### 补丁启动失败降级

当 HPatchZ.exe 无法启动时（`startAsyncEx` 失败），不直接报错，而是回调 `START_PATCH_PROCESS_FAILED` 退出码，进入默认退出码处理流程 → 对空 temp 目录做 MD5 检查 → 所有文件失败 → 返回 `APPLY_CHECK_MD5_NOT_MATCH` → 触发重新下载所有补丁文件。
