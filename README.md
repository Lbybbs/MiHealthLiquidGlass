# MiHealthLiquidGlass

LSPosed (libxposed API 102) 模块 —— 给 **小米运动健康**（`com.mi.health`，v3.58.0）加上一条
**整块液态玻璃底栏**：底栏变成一块实时模糊 + 折射下方滚动内容的玻璃，保留原生 Tab 切换。

渲染引擎：[QWEA0/Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android)（View 体系，
API 33+ 走 AGSL 透镜管线）。风格对齐 HyperOS 4 / MIUIX / iOS 26 液态玻璃。

---

## 1. 已完成的逆向分析（不用重做）

APK 用 apktool 解包后确认：

- 包名 `com.mi.health`，compileSdk 35。
- 重度 **React Native**（自研 YRN 运行时：`com.xiaomi.yrn.*`、`libyrnbridge`、`libfolly_*`、`libyoga`、`libv8executor` 等）。
- **但主界面底栏是原生 Material `TabLayout`**，不是 RN，可直接注入。布局 `main_activity_main`（id `0x7f0e06ef`）：

```
LinearLayout (vertical)                          ← app 根布局，挂在 android.R.id.content 下
 ├─ FrameLayout main_fl_content        id 0x7f0b0ebe   weight=1  ← 内容区（RN 页面渲染于此）
 ├─ View        divider
 └─ FrameLayout main_fl_bottom_container id 0x7f0b0ebd ← 底栏容器
      └─ Material TabLayout main_tl_bottom id 0x7f0b0ec1 ← 原生 Tab
```

- 启动链路：`com.xiaomi.fitness.login.SplashActivity`（别名 `com.mi.health.home.HomeActivity`）→
  `com.xiaomi.fitness.main.MainActivity`。

> 资源 id 用**名称**在运行时解析（`Resources.getIdentifier`），所以 app 升级导致 id 重排也能稳定命中。

## 2. 注入原理

模块 hook 框架方法 `android.app.Instrumentation.callActivityOnResume`（不依赖 app 私有类），
每当 Activity resume 时检查它是否持有 `main_tl_bottom` 等视图；命中则在 `main_activity_main` 上做手术：

1. 把 `main_fl_bottom_container` 从竖直 LinearLayout 里**摘出来**，以 BOTTOM 覆盖方式挂到
   `android.R.id.content` 上 → 内容区随后填满整屏，**内容滚到底栏后面**。
2. 清掉底栏的不透明背景。
3. 在底栏容器 index 0 插入 `LiquidGlassView`，`backdropSource = main_fl_content`（采样背景），
   `enableDynamicBackground = true`；原生 `TabLayout` 仍在顶层，触摸/切换逻辑原样保留。

## 3. 构建（推荐 GitHub 云构建，无需本地 SDK）

工程已带好 Git 云构建所需的全部文件：`.github/workflows/build.yml`（GitHub Actions，跑在
托管 runner 上，runner 自带 Android SDK，只需公开仓库源，**不需要任何 secrets/token**）、
Gradle Wrapper（`gradlew` + `gradle-wrapper.jar`）。

**一键云构建步骤：**

1. 在 GitHub 建一个（私有/公开均可）仓库，把本目录内容推送上去（用你自己的登录方式）。
2. Actions 里跑 `Build module APK`（或在 `workflow_dispatch` 手动触发）。
3. 构建完成后在 Actions 的 **Artifacts** 里下载 `MiHealthLiquidGlass` 下的 APK。

> 私有仓库要先把 `Settings → Actions → General → Workflow permissions` 设为可读/写，否则上传
> artifact 可能受限。

**本地命令行构建（备用，需要 Android SDK + JDK 17）：**

```powershell
cd liquid-glass-mihealth
.\gradlew.bat :app:assembleRelease   # release 默认用 debug 签名，直接可装
```

依赖仓库（已在 `settings.gradle.kts` 配置）：

- `io.github.libxposed:api:102.0.0`（compileOnly，框架提供）
- `io.github.libxposed:service:102.0.0`
- `com.github.QWEA0:liquidglass:v2.0.2`（JitPack）

> **重要**：本仓库**没有** Android SDK/Gradle，因此这个工程**未在本机编译验证**。它按
> Android 官方向导的版本组合（AGP 8.7.2 + Gradle 8.9 + Kotlin 1.9.24 + compileSdk 35）配置，
> 首次构建若版本不匹配，按你环境微调。
>
> **最可能的两个报错与对策**：
> 1. `requires API 36`（QWEA0 的 AAR 用 compileSdk 36 编译，若暴露了 API 36 类型）：
>    把 `app/build.gradle.kts` 的 `compileSdk` 升到 `36`，并在 CI 的 sdkmanager 行补
>    `"platforms;android-36"`，同时 `gradle.properties` 加 `android.suppressUnsupportedCompileSdk=36`。
> 2. AGP/Gradle 版本不匹配：按你环境的实际版本同步 `build.gradle.kts` 与 `gradle-wrapper.properties`。

## 4. 安装与启用

1. 安装上面的 APK。
2. LSPosed 中启用本模块，作用域勾 `com.mi.health`（scope.list 已限定）。
3. **强制停止**小米运动健康后重开。

日志 tag：`MiHealthLiquidGlass`（LSPosed → 模块日志可查）。

## 5. 已知限制 / 待你真机验证的点

| 项 | 现状 | 需要你做的 |
|---|---|---|
| 视图 id 是否匹配 v3.58.0 | 已按反编译命名解析 | 真机跑一次确认没静默放弃注入 |
| 内容是否真的滚到底栏后面 | 手术做了覆盖+内容全屏 | 看是否有东西挡着/要加内容底部 padding |
| 底栏里 Tab 是否还能点 | 原生 TabLayout 保留在上层 | 点一下确认切换正常 |
| 折射效果/参数 | 用默认参数 | 按喜好调 `refractionHeight`/`bevelWidth`/`dispersionStrength`/`material` |
| 深浅色下图标文字颜色 | 目前透出 app 原本 Tab 颜色 | 如需跟随背景翻色，加一个前景色翻转 |
| Android 13 以下 | 有降级提示路径，但未深入 | 你设备是 13+，可忽略 |

## 6. 路线图

- [x] 分析 APK、定位原生底栏与资源 id
- [x] 模块骨架（libxposed 102 + QWEA0 整块玻璃底栏）
- [ ] 真机验证 + 参数微调
- [ ] 内容滚动到底栏后的底部 padding 处理
- [ ] 浅色/深色下 Tab 图标文字自适应翻转
- [ ] （可选）升级为「选中项液滴」HeyBox 风格
- [ ] （可选）加一个模块内设置页开关/参数

## 7. 许可

- 本模块代码：MIT（作者自用/研究）。
- [QWEA0/Liquid-Glass-Android](https://github.com/QWEA0/Liquid-Glass-Android)：MIT。
- [libxposed/api](https://github.com/libxposed/api)：Apache-2.0。
- 本项目仅供学习研究，请勿用于侵权用途。
