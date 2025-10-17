## MobileMouse PC (Java 8)

用 Java 8 重写的 PC 端最小实现，兼容 `PROTOCOL.md` v1.0.0。

### 功能
- 监听 `ws://<PC_IP>:8988/ws`，握手发送 `hello`，支持 `ping/pong`。
- 鼠标：`mouse_move` 相对移动、`mouse_button` 左/右/中按下/抬起、`drag` 按移动处理。
- 滚动：`scroll` 垂直滚动（水平滚动暂不支持）。
- 键盘：`key_event`（支持常用键与修饰键）。
- 文本：`text_input` 通过剪贴板粘贴保证 Unicode（Ctrl+V）。

### 构建与运行（Windows/macOS）
```bash
cd pc/MobileMouse.PC.Java
mvn -q -DskipTests package
java -jar target/mobilemouse-pc-java-1.0.0-jar-with-dependencies.jar 8988
```

运行后控制台会打印本机 IPv4 列表，手机端输入 `ws://<IPv4>:8988/ws` 连接。

### 配置
首次运行会在可执行同级目录生成 `config.json`：
```json
{
  "sensitivity": 1.0,
  "scrollSpeed": 1.0,
  "invertScroll": false
}
```

### 注意
- 本实现使用 `java.awt.Robot` 注入，适合基础触摸板与键盘功能；水平滚动与更复杂键位可按需扩展。
- 文本输入优先走剪贴板粘贴，确保 Emoji/中文等 Unicode 正确；若被安全策略拦截，请改为逐字符注入或使用 JNI 调用 `SendInput`。