# 第三方依赖声明

Dynamic Island 原创代码采用根目录 LICENSE 中的 MIT 许可证。下列依赖的许可证独立适用，不因本项目的 MIT 许可而改变。

## JLayer 1.0.1

- 坐标：`javazoom:jlayer:1.0.1`
- 项目：https://www.javazoom.net/javalayer/javalayer.html
- 作者及版权：JavaZOOM 与 JLayer 源文件所列贡献者，详见对应源文件头部。
- 源文件声明 GNU Library General Public License version 2 or later；本次分发选择 LGPL 2.1，文本见 `licenses/LGPL-2.1.txt`。
- 上游 1.0.1 二进制未修改，以独立嵌入 JAR `META-INF/jars/jlayer-1.0.1.jar` 链接。
- 对应源码随 GitHub Release 提供为 `jlayer-1.0.1-sources.jar`，也可从以下 Maven Central 地址取得：
  https://repo.maven.apache.org/maven2/javazoom/jlayer/1.0.1/jlayer-1.0.1-sources.jar
- 允许按照 LGPL 修改或替换该库，以及为调试其修改进行必要的逆向工程。本项目提供完整构建源码，可用兼容修改版 JLayer 重新构建；也可在保留嵌入路径的情况下替换安装包中的库。

## ZXing Core 3.5.3

- 坐标：`com.google.zxing:core:3.5.3`
- 项目：https://github.com/zxing/zxing
- Copyright 2007 ZXing authors.
- 许可证：Apache License 2.0，见 `licenses/Apache-2.0.txt`。
- 上游二进制未修改，以独立嵌入 JAR `META-INF/jars/core-3.5.3.jar` 分发。
- 上游 NOTICE 原文见 `licenses/ZXing-NOTICE.txt`，保留其中第三方声明。

## 单独安装或运行的组件

Fabric Loader、Fabric API 及网易云 API 按各自项目的许可证提供。网易云 API 服务单独运行，不包含在本模组安装包内；本项目不分发音乐内容或 Minecraft 游戏文件。

## LiquidGlassShader（移植）

- 项目：https://github.com/Jacquesqwq/LiquidGlassShader
- 原作者版权声明：Copyright (c) 2026 Jack。
- 许可证：MIT，完整原文见 `licenses/LiquidGlassShader-MIT.txt`，随本模组分发。
- 参考版本：`4bf62de7c89c285d997cd457daac8ee50837a738`。
- 移植来源：`liquidGlassV2Tinted.frag` 的折射函数、色散、亮度自适应着色、饱和度及 Fresnel 材质处理。
- 修改：GLSL 120 固定功能输入迁移为 GLSL 330；使用 Minecraft 26.3 RenderPearl 管线与独立场景副本；增加局部九点模糊、像素空间圆角及透明度合成。
- 移植代码：`src/main/resources/assets/dynamicisland/shaders/core/liquid_glass.fsh`。Java 渲染桥为本项目新增实现，不使用旧版 LWJGL2 的直接 OpenGL 状态修改。
