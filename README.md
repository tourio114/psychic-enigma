# 灵动岛 Dynamic Island（开发预览）

目标：Minecraft Java 26.3 / Fabric Loader 0.19.5+ / Fabric API 26.3 / Java 25。

**交付状态：已使用 OpenJDK 25、Gradle 9.6.0、Loom 1.17.21 完成编译，生成 `build/libs/dynamic-island-0.1.0.jar`。已核对包内元数据、入口及嵌入依赖；尚未进入游戏验证界面、战斗同步和网易云登录播放。源码 ZIP 不能直接放进 mods。**

## 功能代码

- 顶部黑色圆角胶囊，默认显示电脑本地时间，可开关秒数。
- 战斗时展开，显示攻击目标名称、服务端同步生命值和血条；5 秒后收起。
- 默认右 Shift 打开配置；“修改快捷键”进入 Minecraft 原生按键设置。
- “拖动设置位置”调整位置，保存相对屏幕坐标，自动限制在屏幕内。
- 网易云歌曲搜索、点击播放、暂停/继续、停止、音量调整，播放时显示歌名。
- 网易云 App 扫码登录，轮询确认、二维码过期处理、退出并清除本地凭证。
- 获取音乐地址时携带登录 cookie，使用账号已有权限。无播放地址、登录失效和仅有试听地址会提示。仅支持 API 返回的 MP3 音频；其他编码会显示播放失败。
- HTTP 请求与音频解码运行在后台线程；关闭游戏清理线程和音频资源。

## 构建

安装 **JDK 25 或更高版本**、Python 3，并确保能访问 Fabric Maven、Maven Central、Mojang 下载服务器和 Gradle 下载站。

Windows：双击 `build.bat`。也可在工程目录执行：

```powershell
py -3 tools/build.py
```

Linux / macOS：

```sh
./build.sh
```

脚本从 Gradle 官方下载 9.6.0，核对官方 SHA-256 后运行构建。如果已经安装 Gradle 9.6.0，可直接运行 `gradle build`。Loom 固定为已成功构建的 1.17.21；版本依赖见 build.gradle。

成功后取 `build/libs/dynamic-island-0.1.0.jar`，不要取 `-sources.jar`。

也提供 `.github/workflows/build.yml`：把工程放进你自己的 GitHub 仓库后，在 Actions 手动运行并下载构建产物。此会话没有创建仓库、上传代码或运行远程构建。

已构建的开发预览 JAR 也保存在 `releases/dynamic-island-0.1.0.jar`，校验值见 `releases/SHA256SUMS`。

## 安装与生命值同步

客户端安装 Fabric Loader，将本模组和对应 26.3 的 Fabric API 放入游戏目录 `mods/`。进入世界后按右 Shift。

准确血量由服务端提供：单人游戏使用内置服务器；多人游戏需要服务器也安装同一个模组与 Fabric API。服务端入口不加载音乐、界面等客户端类。

仅客户端安装且服务器未安装时，仍可使用时钟和音乐，战斗面板显示“血量未同步”。不根据客户端猜测剩余血量。

## 网易云 API 与 VIP 登录

需部署用户指定项目：https://github.com/TH911/NeteaseCloudMusicApi

服务端单独运行，并不包含在模组 JAR 内。仓库说明要求 Node.js 18+。例如：

```sh
git clone https://github.com/TH911/NeteaseCloudMusicApi.git
cd NeteaseCloudMusicApi
npm install
node app.js
```

本机部署默认填写 `http://localhost:3000`；远端部署填写自己的 HTTPS 服务地址。这里的 localhost 指运行 Minecraft 的电脑。

1. 配置菜单填写 API，点击“保存 API”。
2. 点击“扫码登录”，用网易云音乐手机 App 扫码并确认。
3. 显示登录成功后搜索歌曲并点击播放。
4. 凭证过期时重新扫码。二维码最多等待 150 秒，可点击关闭或 Esc 取消。

调用路径：`/login/qr/key`、`/login/qr/create`、`/login/qr/check`、`/cloudsearch`、`/song/url/v1`。调用使用 POST 表单；cookie 通过请求正文传至用户指定 API，不追加到 URL。

登录状态不能突破账号权限。VIP、版权、地区及设备策略仍由网易云决定。只返回试听链接时会提示确认 VIP 权限，不将试听当成完整歌曲。

登录凭证保存在 `config/dynamicisland.session`，与 API 地址绑定；更换 API 不会把旧凭证发给新地址。凭证为本地明文，Unix 下使用仅当前用户可读写的文件权限。不要分享此文件；仅连接自己信任的 API。退出登录会清除本地凭证并停止播放，不代表撤销该账号在其他设备的登录。

普通设置保存在 `config/dynamicisland.json`。模组使用独立音频输出，音量通过模组按钮调整，不跟随 Minecraft 音乐音量。

## 待完成验证

- 已完成：真实 Gradle 编译，修正 26.3 输入、HUD、配置界面和网络注册接口。
- 进入 26.3 单人世界：检查右 Shift、改键、时间、显示秒数、拖动、缩放与重启后保存。
- 攻击生物：检查伤害后的数值、连续攻击、治疗、死亡、切换目标、超时和断线清理。
- 用专用服务器检查双端安装及仅客户端安装两种情况。
- 自建 API 测试扫码、授权、取消、过期、重启后凭证、退出、错误地址和超时。
- 分别测试普通歌曲、VIP 账号歌曲、非 VIP 无权限歌曲、试听歌曲和地区受限歌曲。
- 测试播放、暂停、切歌、停止、自然结束、断网、无音频设备与退出游戏。

## 参考

- Fabric 26.3 更新：https://fabricmc.net/2026/09/15/263.html
- Fabric HUD：https://docs.fabricmc.net/develop/rendering/hud
- Fabric 按键：https://docs.fabricmc.net/develop/key-mappings
- Fabric 界面：https://docs.fabricmc.net/develop/rendering/gui/custom-screens
- 网易云 API 文档：https://docs-neteasecloudmusicapi.focalors.ltd/

代码许可证 MIT。JLayer 与 ZXing 作为独立依赖嵌入，保留其各自许可证。
