package com.mobilemouse.pcjava;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;

public final class AppConfig {
    public double sensitivity = 1.0;     // cursor movement multiplier
    public double scrollSpeed = 1.0;     // multiply to wheel steps
    public boolean invertScroll = false; // invert vertical scroll

    private static final String CONFIG_NAME = "config.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static File getConfigFile() {
        String base = new File(".").getAbsoluteFile().getParent();
        // place next to the executable/jar working directory
        return new File(base, CONFIG_NAME);
    }

    public static AppConfig loadOrCreate() {
        File f = getConfigFile();
        try {
            if (!f.exists()) {
                AppConfig def = new AppConfig();
                try (Writer w = new OutputStreamWriter(new FileOutputStream(f), "UTF-8")) {
                    gson.toJson(def, w);
                }
                return def;
            }
            try (Reader r = new InputStreamReader(new FileInputStream(f), "UTF-8")) {
                AppConfig c = gson.fromJson(r, AppConfig.class);
                return c == null ? new AppConfig() : c;
            }
        } catch (Exception e) {
            return new AppConfig();
        }
    }
}