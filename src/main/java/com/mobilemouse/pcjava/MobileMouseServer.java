package com.mobilemouse.pcjava;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.framing.CloseFrame;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public final class MobileMouseServer extends WebSocketServer {
    private final Gson gson = new Gson();
    private final InputInjector injector = new InputInjector();

    public MobileMouseServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket: 服务已启动");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String path = handshake.getResourceDescriptor();
        if (path == null || path.isEmpty()) path = "/";
        if (!"/ws".equals(path)) {
            conn.close(CloseFrame.REFUSE, "仅支持 /ws");
            return;
        }
        System.out.println("WebSocket: 客户端已连接" + toPeer(conn));
        JsonObject hello = new JsonObject();
        hello.addProperty("type", "hello");
        hello.addProperty("serverVersion", "1.0.0");
        hello.add("features", JsonParser.parseString("[\"mouse\",\"keyboard\",\"text\"]"));
        conn.send(hello.toString());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("WebSocket: 客户端已断开" + toPeer(conn) + " code=" + code + " reason=" + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject root = JsonParser.parseString(message).getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            switch (type) {
                case "hello":
                    // ignore for MVP
                    break;
                case "ping":
                    JsonObject pong = new JsonObject();
                    pong.addProperty("type", "pong");
                    if (root.has("ts")) pong.add("ts", root.get("ts"));
                    conn.send(pong.toString());
                    break;
                case "touchstart":
                    handleTouchStart(root);
                    break;
                case "touchmove":
                    handleTouchMove(root);
                    break;
                case "touchend":
                    handleTouchEnd(root);
                    break;
                case "tap":
                    handleTap(root);
                    break;
                case "doubletap":
                    handleDoubleTap(root);
                    break;
                case "longpress":
                    handleLongPress(root);
                    break;
                case "pinch":
                    handlePinch(root);
                    break;
                case "rotate":
                    handleRotate(root);
                    break;
                case "mouse_move":
                    handleMouseMove(root);
                    break;
                case "mouse_button":
                    handleMouseButton(root);
                    break;
                case "scroll":
                    handleScroll(root);
                    break;
                case "roll":
                    handleTwoMove(root);
                    break;
                case "drag":
                    handleDrag(root);
                    break;
                case "key_event":
                    handleKeyEvent(root);
                    break;
                case "text_input":
                    handleTextInput(root);
                    break;
                default:
                    JsonObject err = new JsonObject();
                    err.addProperty("type", "error");
                    err.addProperty("code", "UNSUPPORTED");
                    err.addProperty("message", "Unknown type: " + type);
                    conn.send(err.toString());
                    break;
            }
        } catch (Exception ex) {
            System.out.println("消息解析错误: " + ex.getMessage());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.out.println("WebSocket 错误: " + ex.getMessage());
    }

    private void handleMouseMove(JsonObject root) {
        double dx = root.has("dx") ? root.get("dx").getAsDouble() : 0.0;
        double dy = root.has("dy") ? root.get("dy").getAsDouble() : 0.0;
        injector.moveRelative((int)Math.round(dx), (int)Math.round(dy));
    }

    private void handleDrag(JsonObject root) {
        // MVP：拖拽按相对移动处理，按键状态由客户端控制
        handleMouseMove(root);
    }

    private void handleMouseButton(JsonObject root) {
        String button = root.has("button") ? safe(root.get("button").getAsString(), "left") : "left";
        String action = root.has("action") ? safe(root.get("action").getAsString(), "down") : "down";
        injector.mouseButton(button, action);
    }

    private void handleScroll(JsonObject root) {
        int dx = root.has("dx") ? (int)Math.round(root.get("dx").getAsDouble()) : 0;
        int dy = root.has("dy") ? (int)Math.round(root.get("dy").getAsDouble()) : 0;
        injector.scroll(dx, dy);
    }

    private void handleTwoMove(JsonObject root) {
        double dx = root.has("ds") ? root.get("ds").getAsDouble() : 0.0;
        double dy = root.has("dy") ? root.get("dy").getAsDouble() : 0.0;
        int vSteps = (int)Math.round(dy * 10); // 调整因子以匹配触摸手势强度
        int hSteps = (int)Math.round(dx * 10);
        if (vSteps != 0) {
            injector.scroll(0, vSteps);
        }
        if (hSteps != 0) {
            injector.setModifiers(false, false, true, false, true); // press SHIFT
            injector.scroll(0, hSteps);
            injector.setModifiers(false, false, true, false, false); // release SHIFT
        }
    }

    private void handleKeyEvent(JsonObject root) {
        String key = root.has("key") ? root.get("key").getAsString() : "";
        String action = root.has("action") ? root.get("action").getAsString() : "down";
        JsonObject mod = root.has("mod") && root.get("mod").isJsonObject() ? root.get("mod").getAsJsonObject() : new JsonObject();
        boolean ctrl = mod.has("ctrl") && mod.get("ctrl").getAsBoolean();
        boolean alt = mod.has("alt") && mod.get("alt").getAsBoolean();
        boolean shift = mod.has("shift") && mod.get("shift").getAsBoolean();
        boolean meta = mod.has("meta") && mod.get("meta").getAsBoolean();

        if ("down".equals(action)) {
            injector.setModifiers(ctrl, alt, shift, meta, true);
            injector.keyEvent(key, true);
        } else if ("up".equals(action)) {
            injector.keyEvent(key, false);
            injector.setModifiers(ctrl, alt, shift, meta, false);
        }
    }

    private void handleTextInput(JsonObject root) {
        String text = root.has("text") ? root.get("text").getAsString() : null;
        if (text != null && !text.isEmpty()) {
            injector.typeText(text);
        }
    }

    // --- 新增手势类型处理 ---
    private volatile boolean touching = false;

    private void handleTouchStart(JsonObject root) {
        touching = true;
        // 仅标记开始，PC 侧不主动按下左键；具体点击/拖拽由 tap/drag 单独事件触发
    }

    private void handleTouchMove(JsonObject root) {
        double dx = root.has("dx") ? root.get("dx").getAsDouble() : 0.0;
        double dy = root.has("dy") ? root.get("dy").getAsDouble() : 0.0;
        injector.moveRelative((int)Math.round(dx), (int)Math.round(dy));
    }

    private void handleTouchEnd(JsonObject root) {
        touching = false;
        // 可根据 vx/vy 做惯性滚动，当前版本不在 PC 端处理
    }

    private void handleTap(JsonObject root) {
        injector.mouseButton("left", "click");
    }

    private void handleDoubleTap(JsonObject root) {
        injector.mouseButton("left", "click");
        try { Thread.sleep(40); } catch (InterruptedException ignored) {}
        injector.mouseButton("left", "click");
    }

    private void handleLongPress(JsonObject root) {
        injector.mouseButton("right", "click");
    }

    private void handlePinch(JsonObject root) {
        // 根据 dscale 映射缩放方向；正为放大，负为缩小（经验值）
        double dscale = root.has("dscale") ? root.get("dscale").getAsDouble() : 0.0;
        int steps = (int)Math.round(dscale * 6); // 调整灵敏度因子
        if (steps == 0) return;
        injector.setModifiers(true, false, false, false, true); // press CTRL
        injector.scroll(0, -steps); // 方向可能需按应用调整；这里约定负为缩放放大
        injector.setModifiers(true, false, false, false, false); // release CTRL
    }

    private void handleRotate(JsonObject root) {
        // 旋转映射为水平滚动近似：按住 SHIFT + 垂直滚动
        double dr = root.has("dr") ? root.get("dr").getAsDouble() : 0.0;
        int steps = (int)Math.round(dr * 8);
        if (steps == 0) return;
        injector.setModifiers(false, false, true, false, true); // press SHIFT
        injector.scroll(0, steps);
        injector.setModifiers(false, false, true, false, false); // release SHIFT
    }

    private String safe(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private String toPeer(WebSocket conn) {
        if (conn == null) return "";
        InetSocketAddress a = conn.getRemoteSocketAddress();
        return a == null ? "" : (" [" + a.getHostString() + ":" + a.getPort() + "]");
    }
}