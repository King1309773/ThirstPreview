# Thirst Preview 1.1.0 / 补水预览

与坚定意志 SimpleDifficulty 联动的客户端补水显示模组。本版本作为测试版（Prerelease）发布。

## 功能

- 背包与 JEI 的物品说明显示补水水滴图标，2 点为一滴，支持半滴。按 Shift 查看精确点数、水分饱和度和已知脱水概率。
- 手持食物、饮料或水壶时，在原生口渴条预览可恢复的部分；支持副手、正在使用的物品和 20 点上限。
- 新增已核验 GraveStone 1.0.0 横向水条的可选适配，回复为浅蓝色段，损失为红色段。
- 读取 SimpleDifficulty 已加载的补水规则，支持 metadata 和 NBT；空水壶不显示补水预览。
- 补水图标位于物品提示框内部，与 AppleSkin 的外部饥饿度面板共存；空间不足时回退文字。

## 环境与安装

Minecraft 1.12.2、Forge 14.23.5.2860、SimpleDifficulty 0.3.9、Java 8。客户端安装即可。

退出游戏，移出旧版 Thirst Preview，将附件 `thirstpreview-1.1.0.jar` 放入 `mods`，然后重启。不要同时放入多个版本。原 `config/thirstpreview.cfg` 可以保留。

`thirstpreview-1.1.0-source.zip` 为源码，不能放进 `mods`。首次构建需要自行把 SimpleDifficulty 0.3.9 JAR 放入源码的 `libs` 文件夹。

## 验证状态

发布前重新完成 Java 8 构建与运行时重混淆，78 项测试全部通过（0 失败、0 错误、0 跳过）。测试使用目标整合包的补水规则和限定 GraveStone JAR，已核对成品不含第三方模组类或测试类。

**尚未使用此 JAR 启动游戏进行实际画面、食用/饮用及完整整合包兼容性验收。** 横条只适配已核验的 GraveStone 1.0.0；不能确认位置时会跳过预览。联机时客户端与服务端的补水规则应一致。详细限制和验收步骤见 README 与 TESTING 附件。

模组 JAR：41,448 字节。

SHA-256：`3DAF17DAAF5960097293D491AC968A03E5AD483565B814EC88B0AE782907565A`
