package com.mobilemouse.pcjava;

import java.awt.event.KeyEvent;

public final class KeyMap {
    public static Integer toKeyCode(String key) {
        if (key == null || key.isEmpty()) return null;
        switch (key) {
            case "Enter": return KeyEvent.VK_ENTER;
            case "Escape": return KeyEvent.VK_ESCAPE;
            case "Backspace": return KeyEvent.VK_BACK_SPACE;
            case "Tab": return KeyEvent.VK_TAB;
            case "Space": return KeyEvent.VK_SPACE;
            case "ArrowLeft": return KeyEvent.VK_LEFT;
            case "ArrowUp": return KeyEvent.VK_UP;
            case "ArrowRight": return KeyEvent.VK_RIGHT;
            case "ArrowDown": return KeyEvent.VK_DOWN;
            case "Delete": return KeyEvent.VK_DELETE;
            default:
                if (key.length() == 1) {
                    char c = key.charAt(0);
                    if (c >= 'A' && c <= 'Z') return (int)c;
                    if (c >= 'a' && c <= 'z') return (int)Character.toUpperCase(c);
                    if (c >= '0' && c <= '9') return (int)c;
                }
                return null;
        }
    }
}