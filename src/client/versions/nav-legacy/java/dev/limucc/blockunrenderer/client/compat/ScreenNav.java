package dev.limucc.blockunrenderer.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** MC 26.1: the current screen lives directly on Minecraft. */
public final class ScreenNav {

    private ScreenNav() {}

    public static Screen current(Minecraft mc) {
        return mc.screen;
    }

    public static void open(Minecraft mc, Screen screen) {
        mc.setScreen(screen);
    }
}
