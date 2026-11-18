# Blank Launcher

极简 Android TV 桌面：纯色深灰背景、透明应用网格、置顶排序和搜索。使用 Java、Android 原生 View 和 AndroidX Leanback 设置组件，最低支持 Android 8.0（API 26）。

## Android TV 设计更新

依据官方电视设计指南调整安全边距、字体层级、网格间距、应用图标和焦点按压反馈，增加应用数量及操作提示。保留单页网格与中性纯色背景。设计依据与验证记录见 [TV 设计基准](docs/TV-DESIGN.md)，新版预览见 [截图](docs/redesign-preview.png)。

## 构建

安装 JDK 17 或以上及 Android SDK 36，在 Android Studio 中打开本目录，或设置 `JAVA_HOME` 和 `ANDROID_HOME` 后执行：

```powershell
.\gradlew.bat assembleDebug lintDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 安装与使用

```powershell
adb -s <设备序列号> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <设备序列号> shell am start -n com.leskur.blanklauncher/.MainActivity
```

- 方向键移动焦点，确认键打开应用。
- 长按确认键打开管理菜单，可置顶、取消置顶、查看系统应用详情或进入系统卸载确认。
- 单页面应用列表，不区分首页与抽屉；置顶应用排在前面，两组内部均按名称字母顺序排列，不提供手动排序。
- 返回键清除搜索并回到列表顶部。已有置顶和本地历史记录保留。
- Home 键回到列表顶部。旧版收藏会自动迁移为置顶。
- 全部应用支持名称、英文、拼音及拼音首字母搜索。Android 8/9 支持名称及常用汉字首字母，完整拼音使用 Android 10 及以上的系统 ICU。
- 右上角进入设置，其中可打开系统默认桌面选择页。
- 同时发现 TV 应用和普通应用，同一包名优先保留 TV 入口。
- 安装、卸载和更新应用后自动刷新；未安装可启动应用时显示设置入口。

默认桌面切换取决于电视厂商支持情况。项目不会禁用原有桌面，也不需要网络或查询全部安装包权限。

### 设为默认桌面

先确认 Slate 能正常打开，“桌面设置 → 桌面切换说明”提供 ADB 停用和恢复的操作说明，不跳转系统默认桌面选择页。

通过 ADB 停用原桌面。不同设备的包名不同，以下仅适用于本项目已验证的电视模拟器（`com.google.android.tvlauncher`），不要直接当作所有电视通用命令：

```powershell
adb -s <设备序列号> shell pm disable-user --user 0 com.google.android.tvlauncher
```

停用后验证 Home 键与重启。只有 Slate 一个正常桌面入口时可能直接进入；多个桌面或不同系统的行为可能不同。恢复示例：

```powershell
adb -s <设备序列号> shell pm enable --user 0 com.google.android.tvlauncher
```

这两条命令不会卸载原桌面。执行前确认连接的是目标设备，并保留恢复命令。

## 第一版范围

应用网格未选中时底色透明，只显示图标和名称；遥控器选中时显示深灰底板、白色边框和轻微放大。

顶部使用普通固定栏，与应用列表上下分开。列表独立滚动，不会进入时间、搜索和设置背后；已移除背景模糊和渐变覆盖效果。

名称默认单行省略显示；选中长名称并停留后自动横向滚动，移开焦点立即恢复省略显示。短名称不滚动。

当前使用纯中性深灰背景（#121212），不使用壁纸、渐变或模糊。已清理应用内的历史壁纸资源。

桌面设置使用官方 `LeanbackSettingsFragmentCompat` 与 `LeanbackPreferenceFragmentCompat`（leanback-preference 1.2.0），打开时右侧滑入并压暗左侧桌面。标题区 #1C1C1C，内容区 #212121；使用说明和关于是独立的二级页面，返回键逐级返回。Leanback 已进入弃用维护阶段，官方新项目推荐 Compose for TV；此处为贴合系统设置参考界面使用稳定版组件。


包含本地应用展示、置顶排序、应用搜索、遥控器操作和系统设置入口。采用纯色界面，最近使用和字母索引暂时不显示，不提供壁纸、天气或影视推荐。

当前电视预览见 [截图](docs/preview.png)。

Android TV 的应用入口与 banner 声明参考 [Android 官方文档](https://developer.android.com/training/tv/get-started/create)。构建版本兼容性参考 [Android Gradle Plugin 官方文档](https://developer.android.com/build/releases/about-agp)。
