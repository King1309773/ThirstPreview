# 验证记录

## 1.1.1：移除提示前缀

日期：2026-10-20（中国标准时间）。

- 普通水滴行、Shift 数值行与空间不足时的文字回退均去掉“补水：”前缀；英文同步去掉“Hydration:”。
- Java 8 构建及 `reobfJar` 成功；78 项既有测试重新运行，0 失败、0 错误、0 跳过。
- 已检查成品中英文语言文件，图标标签为空，两个数值格式均无此前缀。
- 与 1.1.0 成品逐项比较，仅两份语言文件、版本入口类及 mcmod.info 不同；其余渲染、补水计算与适配代码内容一致。
- 尚未启动游戏进行实际画面验收。

1.1.1 JAR：41440 字节。SHA-256：`25AB888D7D5A02B154395AE872C68E8BC4399E4D96EF7932134D8A83AE9B4722`。

## 1.1.0：图标与横条适配

日期：2026-10-18（中国标准时间）。版本：Thirst Preview 1.1.0 图标与横条测试版。目标实例：Rlcraft-CEAddons v涛.3，同时回归原 v2.7.涛 规则。

## 已完成

- Java 8、Forge 1.12.2-14.23.5.2860、SimpleDifficulty 0.3.9 构建成功，包括 `reobfJar` 运行时重混淆。
- **78 项测试，0 失败、0 错误、0 跳过**：原有数值/手持策略/水滴/解析 34 项；tooltip 布局 7 项、接线静态契约 3 项；横条几何和帧模型 22 项、可选适配桥接 12 项。
- 分别使用原 v2.7.涛 和目标 v涛.3 的真实补水规则回归，均通过；文件均含 119 个物品键、133 条补水规则，抽查苹果、西瓜、牛奶、药水 NBT 和 Rustic 流体 NBT。未改解析器。
- 覆盖果汁 metadata、水壶单口与空壶、只读检查不修改真实物品 NBT、原生饮料回退、药水类型回退和 JSON 匹配优先级。
- 整滴 u36、半滴 u45、灰色空槽 u0、可染红灰阶轮廓 u9 均来自 SD 原生贴图；经典/当前两套均已静态核对。蓝色填充图不能通过红色乘色直接变红，负值因此使用原生灰阶轮廓。
- Forge 2860 GuiUtils 的实际换行、最终坐标、10px 行距及标题后 2px 间隔已核对；Pre 完整预检，不安全则由原生文本重新排版；PostText 仅在自己的预留行及屏内绘制。
- AppleSkin 1.0.14 的面板位于 tooltip 外部，补水图标位于内部；这属于静态布局核对，不是实际画面验证。
- 当前 GraveStone JAR 指纹及公开配置字段经字节码核对；只读匹配，无必装依赖、无第三方类打包、无私有渲染调用。数字在条框外，叠层仅占内部新增/损失段。
- 已覆盖缺目标、指纹不符、配置异常、绘制异常、原生与自定义路径互斥条件、帧重用、缩放/分辨率变化及高度错配时跳过。
- 独立规格审查发现并修正了负图标实际颜色和 tooltip 失败回退两处缺口，复核通过。最终代码质量审查未发现 Critical/Important 问题，可交付本地测试；明确保留静态接线检查与简化字体模型的测试盲点。另只读核验了当前 JEI 及 great-scrollable-tooltips 的提示框传参与最终坐标路径。
- 完整构建成功，25 个本模组类，中英文语言资源，版本 1.1.0；无测试类、无其他模组类或嵌套依赖 JAR。
- 发布准备仅更新源码说明与 GitHub 发布附件，没有自动替换游戏实例中的已安装模组；未修改温度 HUD、其他模组、游戏配置或存档。

发布包 1.1.0 SHA-256：`3DAF17DAAF5960097293D491AC968A03E5AD483565B814EC88B0AE782907565A`，41448 字节。

已安装旧包 1.0.0 SHA-256：`842F77D3EBC75778E55128BA134EFAE2D415063514E280C6A45E3C300604BE6C`，未改动。

构建与验证示例（将路径替换为自己的测试整合包；本机验证使用目标实例中的实际文件）：

```powershell
$packRules = 'C:/Minecraft/Instance/config/simpledifficulty/consumableThirst.json'
$graveStoneJar = 'C:/Minecraft/Instance/mods/GraveStone-1.0.0.jar'
.\gradlew.bat test build --rerun-tasks `
  '-Dorg.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8 -Dnet.minecraftforge.gradle.test_certs=false -Dsun.net.client.defaultConnectTimeout=4000 -Dsun.net.client.defaultReadTimeout=4000' `
  "-PpackRules=$packRules" `
  "-PgravestoneJar=$graveStoneJar"
```

本机具备所需依赖缓存。旧 ForgeGradle 的额外网络探测曾导致一次构建停滞，仅停止了该次自有构建进程，加入子 JVM 的探测开关和连接/读取超时后重跑成功；未停止游戏进程或修改实际下载的 TLS 校验。工具仍提示旧 Gradle API 将不兼容未来 Gradle 5，本项目固定使用 Gradle 4.10.3。

## 尚未完成的游戏验收

没有使用本次新 JAR 启动游戏，也没有进入存档进行食用/饮用、OpenGL 实际状态或完整整合包兼容性实测。3 项接线检查是静态源码契约，不是端到端 GUI 测试；其余测试不覆盖真实画面。自动测试与字节码核对不能替代游戏验收，具体步骤见 README。

其他模组在同一阶段修改高度会保守跳过横条；高度增量不是公共实际几何事件，不能声称对任意未知渲染器都能绝对识别。第三方主动删除 tooltip 行、使用自定义非 Forge 提示框或在最后阶段另行改写布局也不承诺支持。本版本作为 GitHub Prerelease 发布，供实际整合包验证，尚未完成游戏内画面验收。
