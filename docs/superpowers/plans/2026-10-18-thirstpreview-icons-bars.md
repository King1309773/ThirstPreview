# Thirst Preview 图标与横条 Implementation Plan

> **For agentic workers:** 在现有项目执行已获确认的设计；使用测试先行和独立审查。根代理处理 tooltip 与接线；仅将互不重叠的横条适配文件交给一个实现代理。不改温度 HUD、不修改其他模组、不安装或上传。

**Goal:** 同时交付测量布局内的补水水滴图标与当前 GraveStone 横条上的手持回复预览。

**Architecture:** 现有解析器不变。Tooltip 使用单独渲染器与纯布局模型；横条使用哈希限定、只读配置反射和每帧高度捕获。共享可恢复的 GL 状态作用域，原生水滴预览继续独立使用。

**Tech Stack:** JDK8 / Forge 1.12.2 / SimpleDifficulty 0.3.9 / JUnit4；可选 GraveStone 已核实实现，不引入编译期必装依赖。

## 1. 基线

- [ ] 当前源码完整 test --offline --rerun-tasks，确认原 34 项测试通过。
- [ ] 保存本地 Git 基线（项目此前无 Git），只纳入源码、测试、构建工具与说明，不纳入 libs、dist、游戏配置或安装包。

## 2. 提示框图标（根代理）

新文件：client/TooltipHydrationLayout.java、client/TooltipHydrationRenderer.java、client/PreviewRenderState.java；新测试：client/TooltipHydrationLayoutTest.java。前缀分别为 src/main/java/dev/rlcraftceaddons/thirstpreview/ 与 src/test/java/dev/rlcraftceaddons/thirstpreview/。

- [ ] 先写失败用例：8→4整滴、3→整+半、0灰色计数、负值红色计数、22→单滴×11、Integer.MIN_VALUE 不溢出、预留行唯一识别、换行/边界不越框。
- [ ] 定向 test --tests '*TooltipHydrationLayoutTest'，观察缺失实现失败后实现纯布局；每行最多10滴，每滴9像素、步长9。
- [ ] ItemTooltipEvent 添加带唯一格式标记的可测量预留行；Shift/一直详情保留精确补水值、饱和度和概率。原数据解析不变。
- [ ] RenderTooltipEvent.PostText 使用最终 getLines/X/Y/Width/Height 和实际 FontRenderer，计算预留行位置；图标只占自己的行，若被换行或无法可靠确定则在行内降级数值，不画到框外。
- [ ] 使用 ThirstGui 的原生贴图，完整 u36 / 半滴u45 / v0，按照经典配置选图；独立状态作用域还原颜色、纹理、混合、深度和矩阵，避免污染 AppleSkin。
- [ ] PreviewRenderState 提供静态 capture() 和 close()，供两个渲染器复用；不触碰物品、能力数据或目标模组渲染函数。

## 3. 横条适配（单独实现代理，根代理接线）

只允许代理新增 client/GraveStoneBarMath.java、client/GraveStoneBarPreview.java 及对应测试，不编辑 ClientEvents、入口、构建或 tooltip 文件，不启动 Gradle（避免并发），用 JDK/JUnit 定向运行独立纯测试。

- [ ] 先写纯几何与帧校验失败测试，再实现：filledWidth=ceil(clamp(value,0,20)*77/20)，rightEdge=x+79，增/减区间均限制在内部77×7内。
- [ ] API 固定：preview.beginFrame()；preview.capture(int scaledWidth,int scaledHeight,boolean commonVisible,boolean nativeDisplay)；preview.render(int scaledWidth,int scaledHeight,int current,int restored,float alpha)。
- [ ] capture 在 Post(ALL) LOW，记录左右高度和侧配置；render 在 Post(ALL) LOWEST 之后执行，要求同一帧/尺寸/侧与高度变化为+10，使用完成后的sideHeight-10取得目标y，不修改高度。
- [ ] 注册自有 ClientEvents 改到 FMLPostInitializationEvent，以晚于已核实目标 init/静态订阅器；仍检查+10，没有可靠几何则不画。
- [ ] 仅启用指定 GraveStone JAR SHA256 5D0013C23B177281688D5FA191922C61F06FC60A1530FC700196F85109BBB354，指纹只在探测时计算一次；使用类来源确定目标Jar并读取公开 com.zzt.grave_stone.config.ModConfig 字段。
- [ ] 检查额外条总开关、SD条、原生接管、ShowHydrationBar、HydrationRightSide；无类/指纹不符/异常只禁用本适配器，原 tooltip/原生路径仍可用。
- [ ] render 复用 PreviewRenderState.capture()，只画当前→回复区间；正值浅蓝脉冲，负值红色；不遮边框数字，不调用目标私有函数。

## 4. 接线、版本、审查与交付

- [ ] ClientEvents 保留原生 AIR 路径；Post(ALL) LOW 捕获横条，LOWEST 绘制；原生与横条依据原生接管标志互斥；公共可见性包含F1、菜单、玩家/视角、世界、模式、口渴开关。
- [ ] 中英文添加图标行短标题/计数与说明，保留所有原配置键。入口和build同步1.1.0，元数据仍客户端附属。
- [ ] 定向测试后完整运行 test build --offline --rerun-tasks，分别用原2.7和目标v涛.3规则文件回归，汇总真实测试数。
- [ ] 先做规格审查再做代码质量审查，修正后重测；必须检查缺少GraveStone的安全类加载、同帧高度/顺序错配、tooltip换行及GL还原。
- [ ] 产物检查无第三方类/测试/嵌套jar，记录 SHA256；交付带 icons-bars-test 名称的本地JAR、说明和源码快照，旧安装包保持不变。
