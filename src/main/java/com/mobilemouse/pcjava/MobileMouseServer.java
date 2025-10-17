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
                case "mouse_move":
                    handleMouseMove(root);
                    break;
                case "mouse_button":
                    handleMouseButton(root);
                    break;
                case "scroll":
                    handleScroll(root);
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

    private String safe(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s;
    }

    private String toPeer(WebSocket conn) {
        if (conn == null) return "";
        InetSocketAddress a = conn.getRemoteSocketAddress();
        return a == null ? "" : (" [" + a.getHostString() + ":" + a.getPort() + "]");
    }
}