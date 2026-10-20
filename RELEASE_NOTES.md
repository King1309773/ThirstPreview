# Thirst Preview 1.1.1 / 补水预览

与坚定意志 SimpleDifficulty 联动的客户端补水显示模组。本版本作为测试版（Prerelease）发布。

## 本次修改

- 食物和饮料的水滴提示不再显示“补水：”字样，直接显示水滴图标。
- Shift 精确数值和空间不足时的文字提示同样去掉此前缀；英文提示同步去掉“Hydration:”。
- 水分饱和度、脱水概率、补水数值及口渴条预览沿用 1.1.0 的行为。

## 环境与安装

Minecraft 1.12.2、Forge 14.23.5.2860、SimpleDifficulty 0.3.9、Java 8。客户端安装即可。

退出游戏，移出旧版 Thirst Preview，将附件 `thirstpreview-1.1.1.jar` 放入 `mods`，然后重启。不要同时放入多个版本。原 `config/thirstpreview.cfg` 可以保留。

`thirstpreview-1.1.1-source.zip` 为源码，不能放进 `mods`。首次构建需要自行把 SimpleDifficulty 0.3.9 JAR 放入源码的 `libs` 文件夹。

## 验证状态

Java 8 构建及运行时重混淆成功，78 项既有测试全部通过。已检查成品的中英文提示不带此前缀；与 1.1.0 相比仅语言文本和版本标识变化。详细结果见源码中的 VERIFICATION.md。校验文件 SHA256SUMS.txt 随发布附件提供。

**尚未使用此 JAR 启动游戏进行实际画面验收。** 横条只适配已核验的 GraveStone 1.0.0；不能确认位置时会跳过预览。联机时客户端与服务端的补水规则应一致。详细限制和验收步骤见 README。
