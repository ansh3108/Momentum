package com.momentum;

import net.fabricmc.api.ModInitializer;

public class Momentum implements ModInitializer {
    public static final String MOD_ID = "momentum";

    public static float terrorStrength = 0.0f;
    public static float pulsePhase = 0.0f;

    @Override
    public void onInitialize() {
        MomentumConfig.load();
    }
}