# Legacy 测试脚本

本目录归档了项目早期用于手动调试和探索的脚本（含 `main` 方法，非 JUnit 单元测试）。

## 说明

- 这些文件**不是**自动化单元测试，不会被 `mvn test` 执行（无 `@Test` 注解）。
- 它们记录了开发过程中对下载、解压、JSON 解析、截图、正则、JavaFX Stage 等功能的探索性验证。
- 保留这些文件是为了让历史调试思路可追溯，便于未来需要复现某次验证时参考。

## 目录结构

| 路径 | 原始用途 |
|---|---|
| `legacy/Main.java` | 启动入口探索 |
| `legacy/FileDownloader.java` | 文件下载验证 |
| `legacy/GzipTest.java` | Gzip 解压测试 |
| `legacy/LogFileRead2.java` | 游戏日志读取验证 |
| `legacy/NavJsonInit.java` | 导航 JSON 初始化 |
| `legacy/RegexTest.java` | 正则表达式测试 |
| `legacy/SnapTest.java` | 截图功能测试 |
| `legacy/StageDemo.java` | JavaFX Stage 演示 |
| `legacy/ZipUnpackTest.java` | ZIP 解包测试 |
| `legacy/download/DownloadJsonCrerat.java` | 下载 JSON 构造 |
| `legacy/other/QAutrDecodeDemo.java` | QAuth 解码演示 |
| `legacy/other/QAutrRequestTest.java` | QAuth 请求测试 |
| `legacy/plugin/CreateJson.java` | 插件 JSON 生成 |
| `legacy/update/DownloadTest.java` | 更新下载测试 |
| `legacy/update/ResourcesLoad.java` | 资源加载验证 |

## 后续处理

这些脚本在后续重构阶段（阶段 3 之后）会评估是否转化为真正的单元测试，或最终删除。
