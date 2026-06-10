package com.skyrunmod.client.gui;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** ModMenu entry point — opens the SkyRunMod cloth-config screen from the mods list. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SkyRunClothConfig::create;
    }
}
