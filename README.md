# 补水预览 / Thirst Preview

为 Rlcraft-CEAddons v2.7.涛 制作的客户端模组，与坚定意志 SimpleDifficulty 0.3.9 联动。

## 使用

- 鼠标移到背包或 JEI 的食物、饮料上，显示每次使用补充的水分点数和水滴格数。**2 点 = 1 格水滴**。
- 按住 Shift，查看水分饱和度和已知的脱水概率。
- 手持补水物品时，口渴条上闪烁显示可恢复的部分，超过 20 点的部分不会画到条外。正在使用的物品优先，其次主手，再其次副手。
- 水壶显示每口的补水量，空水壶不显示补水预览。
- 支持当前水滴纹理和坚定意志经典纹理。AppleSkin 的饥饿度提示继续正常显示。

例如，当前整合包的苹果补水 1 点（半格），西瓜补水 3 点（1.5 格），牛奶补水 4 点（2 格）。

## 安装及卸载

环境：Minecraft 1.12.2、Forge 14.23.5.2860、SimpleDifficulty 0.3.9、Java 8。

将 `build/libs/thirstpreview-1.0.0.jar` 放到本整合包的 `mods` 文件夹，然后重启游戏。客户端安装即可。不要把源码包放进 mods。

卸载时移除该 JAR；配置文件可保留。本模组只读取补水规则、添加客户端显示。

第一次启动后生成 `config/thirstpreview.cfg`：

| 设置 | 默认 | 用途 |
|---|---|---|
| showTooltip | true | 背包和 JEI 中的补水说明 |
| showHud | true | 手持时的口渴条恢复预览 |
| showDetailsWithShift | true | 仅在按 Shift 时显示饱和度与脱水概率；false 表示一直显示详情 |

修改配置后重启游戏。

## 数据来源和适用范围

读取坚定意志已经加载的 `config/simpledifficulty/consumableThirst.json`，按它的 metadata、NBT 和首条匹配顺序解析。也支持坚定意志原生饮料、水壶和原版可饮用药水的默认值。

水分饱和度采用坚定意志的绝对点数。提示显示每次使用的名义数值，实际水分条受 20 点上限限制。

联机时，客户端与服务端应使用相同的坚定意志食物配置。任意第三方脚本直接修改口渴能力的额外效果，以及额外给水壶叠加 JSON 消费规则的玩法，未包含在本版预测范围内。

## 源码与构建

源码位于本目录 `src`。使用 Java 8 设置 JAVA_HOME 后运行：

```powershell
.\gradlew.bat test build --console=plain
```

`libs/SimpleDifficulty-1.12.2-0.3.9.jar` 是构建期引用，不会打入成品。构建使用 ForgeGradle 自动重混淆，成品只包含本模组的类和语言资源。

源码压缩包不附带坚定意志本体。单独解压源码后，先创建 `libs` 文件夹，将整合包中的 `SimpleDifficulty-1.12.2-0.3.9.jar` 复制进去再构建。

## 游戏内验收

1. 重启后在模组列表中确认出现 Thirst Preview。
2. 在背包和 JEI 查看苹果、西瓜、牛奶和纯净水，确认显示补水行；按 Shift 查看详情。
3. 口渴未满时依次手持食物和水壶，确认闪烁水滴对应将要恢复的部分；消耗一次后对比。
4. 确认空水壶、木棍不显示恢复预览；副手放饮料时也能预览。
5. 在水下、低口渴、GUI 缩放改变后检查预览位置；按 F1 隐藏界面时不显示。

自动化验证和构建的具体结果见 `VERIFICATION.md`。这些检查不替代在实际存档中食用/饮用的验收。

## 参考

- [SimpleDifficulty 原始项目](https://github.com/Charles445/SimpleDifficulty)
- [SimpleDifficulty 发布页](https://www.curseforge.com/minecraft/mc-mods/simpledifficulty)

开发时以本整合包已安装的 0.3.9 JAR 及本地配置为准。
