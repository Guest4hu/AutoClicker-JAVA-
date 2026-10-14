package com.example.autoclicker.input.hotkey;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tabela de mapeamento estático entre os keycodes nativos do JNativeHook e os códigos virtuais do AWT KeyEvent.
 */
public class NativeKeyConfig {

    private static final Map<Integer, Integer> KEY_MAP = new HashMap<>();

    static {
        // --- Letras (A - Z) ---
        KEY_MAP.put(NativeKeyEvent.VC_A, KeyEvent.VK_A);
        KEY_MAP.put(NativeKeyEvent.VC_B, KeyEvent.VK_B);
        KEY_MAP.put(NativeKeyEvent.VC_C, KeyEvent.VK_C);
        KEY_MAP.put(NativeKeyEvent.VC_D, KeyEvent.VK_D);
        KEY_MAP.put(NativeKeyEvent.VC_E, KeyEvent.VK_E);
        KEY_MAP.put(NativeKeyEvent.VC_F, KeyEvent.VK_F);
        KEY_MAP.put(NativeKeyEvent.VC_G, KeyEvent.VK_G);
        KEY_MAP.put(NativeKeyEvent.VC_H, KeyEvent.VK_H);
        KEY_MAP.put(NativeKeyEvent.VC_I, KeyEvent.VK_I);
        KEY_MAP.put(NativeKeyEvent.VC_J, KeyEvent.VK_J);
        KEY_MAP.put(NativeKeyEvent.VC_K, KeyEvent.VK_K);
        KEY_MAP.put(NativeKeyEvent.VC_L, KeyEvent.VK_L);
        KEY_MAP.put(NativeKeyEvent.VC_M, KeyEvent.VK_M);
        KEY_MAP.put(NativeKeyEvent.VC_N, KeyEvent.VK_N);
        KEY_MAP.put(NativeKeyEvent.VC_O, KeyEvent.VK_O);
        KEY_MAP.put(NativeKeyEvent.VC_P, KeyEvent.VK_P);
        KEY_MAP.put(NativeKeyEvent.VC_Q, KeyEvent.VK_Q);
        KEY_MAP.put(NativeKeyEvent.VC_R, KeyEvent.VK_R);
        KEY_MAP.put(NativeKeyEvent.VC_S, KeyEvent.VK_S);
        KEY_MAP.put(NativeKeyEvent.VC_T, KeyEvent.VK_T);
        KEY_MAP.put(NativeKeyEvent.VC_U, KeyEvent.VK_U);
        KEY_MAP.put(NativeKeyEvent.VC_V, KeyEvent.VK_V);
        KEY_MAP.put(NativeKeyEvent.VC_W, KeyEvent.VK_W);
        KEY_MAP.put(NativeKeyEvent.VC_X, KeyEvent.VK_X);
        KEY_MAP.put(NativeKeyEvent.VC_Y, KeyEvent.VK_Y);
        KEY_MAP.put(NativeKeyEvent.VC_Z, KeyEvent.VK_Z);

        // --- Números Principais (0 - 9) ---
        KEY_MAP.put(NativeKeyEvent.VC_0, KeyEvent.VK_0);
        KEY_MAP.put(NativeKeyEvent.VC_1, KeyEvent.VK_1);
        KEY_MAP.put(NativeKeyEvent.VC_2, KeyEvent.VK_2);
        KEY_MAP.put(NativeKeyEvent.VC_3, KeyEvent.VK_3);
        KEY_MAP.put(NativeKeyEvent.VC_4, KeyEvent.VK_4);
        KEY_MAP.put(NativeKeyEvent.VC_5, KeyEvent.VK_5);
        KEY_MAP.put(NativeKeyEvent.VC_6, KeyEvent.VK_6);
        KEY_MAP.put(NativeKeyEvent.VC_7, KeyEvent.VK_7);
        KEY_MAP.put(NativeKeyEvent.VC_8, KeyEvent.VK_8);
        KEY_MAP.put(NativeKeyEvent.VC_9, KeyEvent.VK_9);

        // --- Teclas de Função (F1 - F24) ---
        KEY_MAP.put(NativeKeyEvent.VC_F1, KeyEvent.VK_F1);
        KEY_MAP.put(NativeKeyEvent.VC_F2, KeyEvent.VK_F2);
        KEY_MAP.put(NativeKeyEvent.VC_F3, KeyEvent.VK_F3);
        KEY_MAP.put(NativeKeyEvent.VC_F4, KeyEvent.VK_F4);
        KEY_MAP.put(NativeKeyEvent.VC_F5, KeyEvent.VK_F5);
        KEY_MAP.put(NativeKeyEvent.VC_F6, KeyEvent.VK_F6);
        KEY_MAP.put(NativeKeyEvent.VC_F7, KeyEvent.VK_F7);
        KEY_MAP.put(NativeKeyEvent.VC_F8, KeyEvent.VK_F8);
        KEY_MAP.put(NativeKeyEvent.VC_F9, KeyEvent.VK_F9);
        KEY_MAP.put(NativeKeyEvent.VC_F10, KeyEvent.VK_F10);
        KEY_MAP.put(NativeKeyEvent.VC_F11, KeyEvent.VK_F11);
        KEY_MAP.put(NativeKeyEvent.VC_F12, KeyEvent.VK_F12);
        KEY_MAP.put(NativeKeyEvent.VC_F13, KeyEvent.VK_F13);
        KEY_MAP.put(NativeKeyEvent.VC_F14, KeyEvent.VK_F14);
        KEY_MAP.put(NativeKeyEvent.VC_F15, KeyEvent.VK_F15);
        KEY_MAP.put(NativeKeyEvent.VC_F16, KeyEvent.VK_F16);
        KEY_MAP.put(NativeKeyEvent.VC_F17, KeyEvent.VK_F17);
        KEY_MAP.put(NativeKeyEvent.VC_F18, KeyEvent.VK_F18);
        KEY_MAP.put(NativeKeyEvent.VC_F19, KeyEvent.VK_F19);
        KEY_MAP.put(NativeKeyEvent.VC_F20, KeyEvent.VK_F20);
        KEY_MAP.put(NativeKeyEvent.VC_F21, KeyEvent.VK_F21);
        KEY_MAP.put(NativeKeyEvent.VC_F22, KeyEvent.VK_F22);
        KEY_MAP.put(NativeKeyEvent.VC_F23, KeyEvent.VK_F23);
        KEY_MAP.put(NativeKeyEvent.VC_F24, KeyEvent.VK_F24);

        // --- Modificadores e Controles ---
        KEY_MAP.put(NativeKeyEvent.VC_SPACE, KeyEvent.VK_SPACE);
        KEY_MAP.put(NativeKeyEvent.VC_ENTER, KeyEvent.VK_ENTER);
        KEY_MAP.put(NativeKeyEvent.VC_ESCAPE, KeyEvent.VK_ESCAPE);
        KEY_MAP.put(NativeKeyEvent.VC_TAB, KeyEvent.VK_TAB);
        KEY_MAP.put(NativeKeyEvent.VC_BACKSPACE, KeyEvent.VK_BACK_SPACE);
        KEY_MAP.put(NativeKeyEvent.VC_CAPS_LOCK, KeyEvent.VK_CAPS_LOCK);
        KEY_MAP.put(NativeKeyEvent.VC_SHIFT, KeyEvent.VK_SHIFT);
        KEY_MAP.put(NativeKeyEvent.VC_CONTROL, KeyEvent.VK_CONTROL);
        KEY_MAP.put(NativeKeyEvent.VC_ALT, KeyEvent.VK_ALT);
        KEY_MAP.put(NativeKeyEvent.VC_META, KeyEvent.VK_META);
        KEY_MAP.put(NativeKeyEvent.VC_CONTEXT_MENU, KeyEvent.VK_CONTEXT_MENU);

        // --- Navegação e Edição ---
        KEY_MAP.put(NativeKeyEvent.VC_UP, KeyEvent.VK_UP);
        KEY_MAP.put(NativeKeyEvent.VC_DOWN, KeyEvent.VK_DOWN);
        KEY_MAP.put(NativeKeyEvent.VC_LEFT, KeyEvent.VK_LEFT);
        KEY_MAP.put(NativeKeyEvent.VC_RIGHT, KeyEvent.VK_RIGHT);
        KEY_MAP.put(NativeKeyEvent.VC_INSERT, KeyEvent.VK_INSERT);
        KEY_MAP.put(NativeKeyEvent.VC_DELETE, KeyEvent.VK_DELETE);
        KEY_MAP.put(NativeKeyEvent.VC_HOME, KeyEvent.VK_HOME);
        KEY_MAP.put(NativeKeyEvent.VC_END, KeyEvent.VK_END);
        KEY_MAP.put(NativeKeyEvent.VC_PAGE_UP, KeyEvent.VK_PAGE_UP);
        KEY_MAP.put(NativeKeyEvent.VC_PAGE_DOWN, KeyEvent.VK_PAGE_DOWN);
        KEY_MAP.put(NativeKeyEvent.VC_PRINTSCREEN, KeyEvent.VK_PRINTSCREEN);
        KEY_MAP.put(NativeKeyEvent.VC_SCROLL_LOCK, KeyEvent.VK_SCROLL_LOCK);
        KEY_MAP.put(NativeKeyEvent.VC_PAUSE, KeyEvent.VK_PAUSE);
        KEY_MAP.put(NativeKeyEvent.VC_CLEAR, KeyEvent.VK_CLEAR);

        // --- Símbolos e Pontuação ---
        KEY_MAP.put(NativeKeyEvent.VC_BACKQUOTE, KeyEvent.VK_BACK_QUOTE);
        KEY_MAP.put(NativeKeyEvent.VC_MINUS, KeyEvent.VK_MINUS);
        KEY_MAP.put(NativeKeyEvent.VC_EQUALS, KeyEvent.VK_EQUALS);
        KEY_MAP.put(NativeKeyEvent.VC_OPEN_BRACKET, KeyEvent.VK_OPEN_BRACKET);
        KEY_MAP.put(NativeKeyEvent.VC_CLOSE_BRACKET, KeyEvent.VK_CLOSE_BRACKET);
        KEY_MAP.put(NativeKeyEvent.VC_BACK_SLASH, KeyEvent.VK_BACK_SLASH);
        KEY_MAP.put(NativeKeyEvent.VC_SEMICOLON, KeyEvent.VK_SEMICOLON);
        KEY_MAP.put(NativeKeyEvent.VC_QUOTE, KeyEvent.VK_QUOTE);
        KEY_MAP.put(NativeKeyEvent.VC_COMMA, KeyEvent.VK_COMMA);
        KEY_MAP.put(NativeKeyEvent.VC_PERIOD, KeyEvent.VK_PERIOD);
        KEY_MAP.put(NativeKeyEvent.VC_SLASH, KeyEvent.VK_SLASH);
        KEY_MAP.put(NativeKeyEvent.VC_UNDERSCORE, KeyEvent.VK_UNDERSCORE);

        // --- Teclado Numérico / Bloqueio ---
        KEY_MAP.put(NativeKeyEvent.VC_NUM_LOCK, KeyEvent.VK_NUM_LOCK);
        KEY_MAP.put(NativeKeyEvent.VC_SEPARATOR, KeyEvent.VK_SEPARATOR);

        // --- Teclas Internacionais (Japonês) ---
        KEY_MAP.put(NativeKeyEvent.VC_KATAKANA, KeyEvent.VK_KATAKANA);
        KEY_MAP.put(NativeKeyEvent.VC_KANJI, KeyEvent.VK_KANJI);
        KEY_MAP.put(NativeKeyEvent.VC_HIRAGANA, KeyEvent.VK_HIRAGANA);

        // --- Teclas Sun / Especiais ---
        KEY_MAP.put(NativeKeyEvent.VC_SUN_HELP, KeyEvent.VK_HELP);
        KEY_MAP.put(NativeKeyEvent.VC_SUN_STOP, KeyEvent.VK_STOP);
        KEY_MAP.put(NativeKeyEvent.VC_SUN_PROPS, KeyEvent.VK_PROPS);
        KEY_MAP.put(NativeKeyEvent.VC_SUN_FIND, KeyEvent.VK_FIND);
        KEY_MAP.put(NativeKeyEvent.VC_SUN_AGAIN, KeyEvent.VK_AGAIN);
        KEY_MAP.put(NativeKeyEvent.VC_SUN_COPY, KeyEvent.VK_COPY);
        KEY_MAP.put(NativeKeyEvent.VC_SUN_CUT, KeyEvent.VK_CUT);
        KEY_MAP.put(NativeKeyEvent.VC_SUN_UNDO, KeyEvent.VK_UNDO);
        KEY_MAP.put(NativeKeyEvent.VC_SUN_INSERT, KeyEvent.VK_INSERT);

        // --- Indefinido ---
        KEY_MAP.put(NativeKeyEvent.VC_UNDEFINED, KeyEvent.VK_UNDEFINED);
    }

    private NativeKeyConfig() {}

    public static int toAwtKeyCode(int nativeKeyCode) {
        return KEY_MAP.getOrDefault(nativeKeyCode, KeyEvent.VK_UNDEFINED);
    }

    public static boolean isSupported(int nativeKeyCode) {
        return KEY_MAP.containsKey(nativeKeyCode);
    }

    public static Map<Integer, Integer> getKeyMap() {
        return Collections.unmodifiableMap(KEY_MAP);
    }
}
