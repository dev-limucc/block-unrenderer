package dev.limucc.blockunrenderer.client.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** MC 26.2+: screen management moved from Minecraft to Gui. */
public final class ScreenNav {

    private ScreenNav() {}

    public static Screen current(Minecraft mc) {
        return mc.gui.screen();
    }

    public static void open(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }
}
