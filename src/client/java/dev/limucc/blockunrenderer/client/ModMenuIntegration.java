package dev.limucc.blockunrenderer.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.limucc.blockunrenderer.client.gui.ClothConfigScreen;

/**
 * Opens the Cloth Config settings screen from ModMenu (filter mode, trigger mode,
 * lighting, show-underneath, block list). The searchable icon-based block picker is
 * available via the "Open Block Manager" keybind.
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ClothConfigScreen::create;
    }
}
