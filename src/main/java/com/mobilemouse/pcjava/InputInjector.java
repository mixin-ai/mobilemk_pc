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
        try {
            // 使用剪贴板粘贴，保证 Unicode 文本（Windows 下 Ctrl+V）
            StringSelection sel = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.keyPress(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_V);
            robot.keyRelease(KeyEvent.VK_CONTROL);
        } catch (Exception e) {
            // 回退：粗略逐字符输入（ASCII 或基本拉丁），复杂文本可能失败
            for (char ch : text.toCharArray()) {
                typeChar(ch);
            }
        }
    }

    private void typeChar(char ch) {
        // 简单字符映射：字母、数字、空格、常见符号
        boolean upper = Character.isUpperCase(ch);
        char base = upper ? ch : Character.toUpperCase(ch);
        Integer code = KeyMap.toKeyCode(String.valueOf(base));
        if (code != null) {
            if (!upper && Character.isLetter(ch)) {
                // 小写：按下 SHIFT 以得到对应字符，部分布局不适用，仅作为回退
                robot.keyPress(KeyEvent.VK_SHIFT);
                robot.keyPress(code);
                robot.keyRelease(code);
                robot.keyRelease(KeyEvent.VK_SHIFT);
                return;
            }
            robot.keyPress(code);
            robot.keyRelease(code);
            return;
        }
        if (ch == ' ') { robot.keyPress(KeyEvent.VK_SPACE); robot.keyRelease(KeyEvent.VK_SPACE); return; }
        // 其他基本符号可按需扩展
    }
}