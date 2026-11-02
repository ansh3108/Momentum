package com.momentum;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MomentumConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "momentum.json");

    // Feature Toggles
    public boolean enableAdrenaline = true;
    public boolean enableViewmodelPhysics = true;
    public boolean enableBlockAnimations = true;

    public static MomentumConfig INSTANCE = new MomentumConfig();

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                INSTANCE = GSON.fromJson(reader, MomentumConfig.class);
            } catch (Exception e) {
                System.err.println("Failed to load Momentum config!");
            }
        }
        save(); // Instantly generates the file with default values if it doesn't exist
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            System.err.println("Failed to save Momentum config!");
        }
    }
}