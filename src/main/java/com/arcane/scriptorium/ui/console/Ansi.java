package com.arcane.scriptorium.ui.console;

public final class Ansi {
    public static final String RESET = "\u001B[0m";
    public static final String DIM = "\u001B[2m";
    public static final String BLUE = "\u001B[34m";
    public static final String CYAN = "\u001B[36m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String YELLOW = "\u001B[33m";
    public static final String GREEN = "\u001B[32m";
    public static final String RED = "\u001B[31m";
    public static final String GRAY = "\u001B[90m";

    private Ansi() {
    }

    public static boolean isEnabled() {
        return System.console() != null && System.getenv("NO_COLOR") == null;
    }

    public static String paint(boolean enabled, String color, String value) {
        if (!enabled) {
            return value;
        }
        return color + value + RESET;
    }
}
