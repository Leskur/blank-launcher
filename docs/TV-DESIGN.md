# SlateLauncher 电视设计基准

本轮基于 Android TV 官方设计指南调整现有原生 View 界面。保留单页网格、透明默认卡片、中性深灰背景、置顶排序和原生 Leanback 设置。

## 从规范到设计

- 布局：以电视常见的 960 × 540 dp 逻辑画布考虑比例；电视横向安全边距 48dp、上下 24dp。窄窗口横向边距 24dp。网格由可用宽度计算，最多六列，每格两侧各留 10dp，容纳焦点放大。
- 字体：使用系统无衬线字体；时间 26sp、区域标题 24sp、应用名及搜索 16sp、辅助信息 14sp。名称维持单行，长名称聚焦停留后滚动。
- 颜色：背景 #121212、默认控件 #212121、聚焦表面 #303030，主文字 #EBEBEB、辅助文字 #A0A0A0、焦点边框 #F0F0F0。应用图标保留品牌色。
- 层级：固定顶部时钟、搜索、设置；下方应用标题及数量；应用区独立滚动，不显示底部常驻操作提示。没有增加页面切换。
- 焦点：卡片聚焦放大 1.05 倍并显示 2dp 浅色边框，按确认短暂回缩，不添加聚焦阴影。搜索框也有明确焦点边框。返回桌面时按包名恢复应用焦点和滚动位置。
- 空状态：区分没有搜索结果与没有可启动应用，分别提供清除搜索和设置入口。

以上具体尺寸、配色及页面结构是 SlateLauncher 的设计选择，不代表官方强制值。实体电视仍需检查远距离阅读、厂商 overscan 和遥控器行为。

## 官方参考

- [专为 TV 设计](https://developer.android.com/design/ui/tv/guides/foundations/design-for-tv?hl=zh-cn)
- [布局](https://developer.android.com/design/ui/tv/guides/styles/layouts)
- [字体](https://developer.android.com/design/ui/tv/guides/styles/typography)
- [颜色系统](https://developer.android.com/design/ui/tv/guides/styles/color-system)
- [焦点系统](https://developer.android.com/design/ui/tv/guides/styles/focus-system)
- [导航](https://developer.android.com/design/ui/tv/guides/foundations/navigation-on-tv)
- [卡片](https://developer.android.com/design/ui/tv/guides/components/cards)

## 应用管理更新

长按真实应用显示置顶／取消置顶、应用详情、卸载。详情打开系统应用信息页，卸载交由系统确认。演示应用仅显示置顶操作。置顶应用和其他应用分别沿用名称字母排序；旧的置顶记录保留，但保存顺序不再影响显示。

## 本轮验证

2026-11-18：JDK 21，assembleDebug 和 lintDebug 通过。仅更新 emulator-5554 电视模拟器，未操作实体手机。

- 检查真实应用页面，方向键进入网格，返回聚焦首项。
- 方向键移动到 F-Droid，长按确认显示置顶管理菜单，返回关闭。
- 演示模式显示 69 个应用，多次向下导航可滚动到后续应用，当前焦点仍在可见区域；结束后关闭演示模式。
- 搜索无匹配字符串，验证 0 个结果和清除入口；返回清空查询并恢复 8 个真实应用。
- 打开桌面设置并返回。
- 运行期间 AndroidRuntime 日志未发现崩溃。

预览见 [实际应用](redesign-preview.png)。实体电视、不同字体缩放及所有系统版本尚未验证；应用切换后的焦点恢复已实现，但未覆盖所有外部应用生命周期场景。
