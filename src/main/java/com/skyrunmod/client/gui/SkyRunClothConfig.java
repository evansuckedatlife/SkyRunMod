package com.skyrunmod.client.gui;

import java.util.function.Consumer;

import com.skyrunmod.client.SkyRunState;
import com.skyrunmod.config.SkyRunConfig;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Builds the SkyRunMod settings screen with cloth-config over the instance {@link SkyRunConfig}.
 * Saving runs {@link SkyRunState#persist()} so changes hit {@code config/skyrunmod/settings.json}.
 */
public final class SkyRunClothConfig {
    private SkyRunClothConfig() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("SkyRunMod"));

        SkyRunState state = SkyRunState.get();
        if (state == null) {
            // Mod not initialised yet (shouldn't happen from the mods list) — empty screen.
            return builder.build();
        }
        SkyRunConfig config = state.config();
        builder.setSavingRunnable(state::persist);

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory overlay = builder.getOrCreateCategory(Text.literal("Overlay"));
        overlay.addEntry(toggle(eb, "Overlay enabled", config.overlayEnabled, v -> config.overlayEnabled = v));
        overlay.addEntry(toggle(eb, "Draw background", config.overlayBackground, v -> config.overlayBackground = v));
        overlay.addEntry(toggle(eb, "Show PB delta", config.showPbDelta, v -> config.showPbDelta = v));
        overlay.addEntry(toggle(eb, "Show recent average (EMA)", config.showEma, v -> config.showEma = v));
        overlay.addEntry(toggle(eb, "Show session rates panel", config.showRates, v -> config.showRates = v));
        overlay.addEntry(eb.startIntField(Text.literal("Overlay X"), config.overlayX)
                .setSaveConsumer(v -> config.overlayX = v).build());
        overlay.addEntry(eb.startIntField(Text.literal("Overlay Y"), config.overlayY)
                .setSaveConsumer(v -> config.overlayY = v).build());
        overlay.addEntry(eb.startFloatField(Text.literal("Overlay scale"), config.overlayScale)
                .setSaveConsumer(v -> config.overlayScale = v).build());
        overlay.addEntry(eb.startIntSlider(Text.literal("Max split rows"), config.maxSplitRows, 1, 32)
                .setDefaultValue(8)
                .setSaveConsumer(v -> config.maxSplitRows = v).build());

        ConfigCategory activities = builder.getOrCreateCategory(Text.literal("Activities"));
        activities.addEntry(toggle(eb, "Commissions", config.enableCommissions, v -> config.enableCommissions = v));
        activities.addEntry(toggle(eb, "Slayers", config.enableSlayer, v -> config.enableSlayer = v));
        activities.addEntry(toggle(eb, "Dungeons", config.enableDungeon, v -> config.enableDungeon = v));
        activities.addEntry(toggle(eb, "Kuudra", config.enableKuudra, v -> config.enableKuudra = v));
        activities.addEntry(toggle(eb, "Diana", config.enableDiana, v -> config.enableDiana = v));
        activities.addEntry(toggle(eb, "Mining runs", config.enableMining, v -> config.enableMining = v));
        activities.addEntry(toggle(eb, "Fishing", config.enableFishing, v -> config.enableFishing = v));

        ConfigCategory data = builder.getOrCreateCategory(Text.literal("Data"));
        data.addEntry(toggle(eb, "Persist personal bests to disk", config.persistPersonalBests,
                v -> config.persistPersonalBests = v));

        return builder.build();
    }

    private static AbstractConfigListEntry<?> toggle(
            ConfigEntryBuilder eb, String label, boolean value, Consumer<Boolean> save) {
        return eb.startBooleanToggle(Text.literal(label), value).setSaveConsumer(save).build();
    }
}
