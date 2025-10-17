package com.mobilemouse.pcjava;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public final class InputInjector {
    private final Robot robot;
    private final AppConfig config = AppConfig.loadOrCreate();

    public InputInjector() {
        try {
            robot = new Robot();
            robot.setAutoWaitForIdle(true);
            robot.setAutoDelay(2);
        } catch (AWTException e) {
            throw new RuntimeException("无法初始化输入注入: " + e.getMessage(), e);
        }
    }

    public void moveRelative(int dx, int dy) {
        if (dx == 0 && dy == 0) return;
        double s = config.sensitivity <= 0 ? 1.0 : config.sensitivity;
        Point p = MouseInfo.getPointerInfo().getLocation();
        int nx = (int)Math.round(p.x + dx * s);
        int ny = (int)Math.round(p.y + dy * s);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        nx = Math.max(0, Math.min(nx, screen.width - 1));
        ny = Math.max(0, Math.min(ny, screen.height - 1));
        robot.mouseMove(nx, ny);
    }

    public void mouseButton(String button, String action) {
        int mask = InputEvent.BUTTON1_DOWN_MASK;
        switch (button) {
            case "right": mask = InputEvent.BUTTON3_DOWN_MASK; break;
            case "middle": mask = InputEvent.BUTTON2_DOWN_MASK; break;
            default: mask = InputEvent.BUTTON1_DOWN_MASK; break;
        }
        if ("down".equals(action)) {
            robot.mousePress(mask);
        } else if ("up".equals(action)) {
            robot.mouseRelease(mask);
        } else if ("click".equals(action)) {
            robot.mousePress(mask);
            robot.mouseRelease(mask);
        }
    }

    public void scroll(int dx, int dy) {
        int stepsY = dy;
        double speed = config.scrollSpeed <= 0 ? 1.0 : config.scrollSpeed;
        if (config.invertScroll) stepsY = -stepsY;
        stepsY = (int)Math.round(stepsY * speed);
        if (stepsY != 0) {
            robot.mouseWheel(stepsY);
        }
        // Java AWT Robot 不支持水平滚动；如需模拟，可考虑按住 Shift 再垂直滚动。
        // 这里保持简单实现：忽略 dx。
    }

    public void setModifiers(boolean ctrl, boolean alt, boolean shift, boolean meta, boolean press) {
        if (press) {
            if (ctrl) robot.keyPress(KeyEvent.VK_CONTROL);
            if (alt) robot.keyPress(KeyEvent.VK_ALT);
            if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
            if (meta) robot.keyPress(KeyEvent.VK_META);
        } else {
            if (meta) robot.keyRelease(KeyEvent.VK_META);
            if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
            if (alt) robot.keyRelease(KeyEvent.VK_ALT);
            if (ctrl) robot.keyRelease(KeyEvent.VK_CONTROL);
        }
    }

    public void keyEvent(String key, boolean isDown) {
        Integer code = KeyMap.toKeyCode(key);
        if (code == null) return;
        if (isDown) robot.keyPress(code);
        else robot.keyRelease(code);
    }

    public void typeText(String text) {
        if (text == null || text.isEmpty()) return;
        StringBuilder bufUnknown = new StringBuilder();
        for (char ch : text.toCharArray()) {
            Stroke st = mapChar(ch);
            if (st == null) {
                bufUnknown.append(ch);
                continue;
            }
            if (bufUnknown.length() > 0) {
                paste(bufUnknown.toString());
                bufUnknown.setLength(0);
            }
            typeKey(st.code, st.shift);
        }
        if (bufUnknown.length() > 0) {
            paste(bufUnknown.toString());
        }
    }

    private static final class Stroke {
        final int code; final boolean shift;
        Stroke(int code, boolean shift) { this.code = code; this.shift = shift; }
    }

    private Stroke mapChar(char ch) {
        switch (ch) {
            case '\n': case '\r': return new Stroke(KeyEvent.VK_ENTER, false);
            case '\t': return new Stroke(KeyEvent.VK_TAB, false);
            case '\b': return new Stroke(KeyEvent.VK_BACK_SPACE, false);
            case ' ': return new Stroke(KeyEvent.VK_SPACE, false);
            case ',': return new Stroke(KeyEvent.VK_COMMA, false);
            case '.': return new Stroke(KeyEvent.VK_PERIOD, false);
            case '/': return new Stroke(KeyEvent.VK_SLASH, false);
            case '-': return new Stroke(KeyEvent.VK_MINUS, false);
            case '=': return new Stroke(KeyEvent.VK_EQUALS, false);
            case '[': return new Stroke(KeyEvent.VK_OPEN_BRACKET, false);
            case ']': return new Stroke(KeyEvent.VK_CLOSE_BRACKET, false);
            case ';': return new Stroke(KeyEvent.VK_SEMICOLON, false);
            case '\'': return new Stroke(KeyEvent.VK_QUOTE, false);
            case '\\': return new Stroke(KeyEvent.VK_BACK_SLASH, false);
            case '`': return new Stroke(KeyEvent.VK_BACK_QUOTE, false);
            case '!': return new Stroke(KeyEvent.VK_1, true);
            case '@': return new Stroke(KeyEvent.VK_2, true);
            case '#': return new Stroke(KeyEvent.VK_3, true);
            case '$': return new Stroke(KeyEvent.VK_4, true);
            case '%': return new Stroke(KeyEvent.VK_5, true);
            case '^': return new Stroke(KeyEvent.VK_6, true);
            case '&': return new Stroke(KeyEvent.VK_7, true);
            case '*': return new Stroke(KeyEvent.VK_8, true);
            case '(': return new Stroke(KeyEvent.VK_9, true);
            case ')': return new Stroke(KeyEvent.VK_0, true);
            case '_': return new Stroke(KeyEvent.VK_MINUS, true);
            case '+': return new Stroke(KeyEvent.VK_EQUALS, true);
            case '{': return new Stroke(KeyEvent.VK_OPEN_BRACKET, true);
            case '}': return new Stroke(KeyEvent.VK_CLOSE_BRACKET, true);
            case ':': return new Stroke(KeyEvent.VK_SEMICOLON, true);
            case '"': return new Stroke(KeyEvent.VK_QUOTE, true);
            case '<': return new Stroke(KeyEvent.VK_COMMA, true);
            case '>': return new Stroke(KeyEvent.VK_PERIOD, true);
            case '?': return new Stroke(KeyEvent.VK_SLASH, true);
            case '|': return new Stroke(KeyEvent.VK_BACK_SLASH, true);
            case '~': return new Stroke(KeyEvent.VK_BACK_QUOTE, true);
            default:
                if (ch >= 'a' && ch <= 'z') {
                    return new Stroke(KeyEvent.VK_A + (ch - 'a'), false);
                }
                if (ch >= 'A' && ch <= 'Z') {
                    return new Stroke(KeyEvent.VK_A + (ch - 'A'), true);
                }
                if (ch >= '0' && ch <= '9') {
                    return new Stroke(KeyEvent.VK_0 + (ch - '0'), false);
                }
                return null;
        }
    }

    private void typeKey(int code, boolean shift) {
        if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(code);
        robot.keyRelease(code);
        if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
    }

    private void paste(String text) {
        if (text == null || text.isEmpty()) return;
        StringSelection sel = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
    }
}