package com.skyrunmod.client;

import org.lwjgl.glfw.GLFW;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

/**
 * Registers SkyRunMod keybinds. Both default to unbound so they never clash with vanilla or other
 * mods; the player assigns them from Options → Controls (category "SkyRunMod").
 */
public final class SkyRunKeybinds {
    private static KeyBinding toggleOverlayKey;
    private static KeyBinding resetRunKey;

    private SkyRunKeybinds() {
    }

    public static void register() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("skyrunmod", "main"));

        toggleOverlayKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyrunmod.toggle_overlay",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category));

        resetRunKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.skyrunmod.reset_run",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(SkyRunKeybinds::onClientTick);
    }

    private static void onClientTick(net.minecraft.client.MinecraftClient client) {
        SkyRunState state = SkyRunState.get();
        if (state == null) {
            return;
        }

        while (toggleOverlayKey.wasPressed()) {
            boolean enabled = !state.config().overlayEnabled;
            state.config().overlayEnabled = enabled;
            state.persist();
            SkyRunChat.feedback(enabled ? "Overlay enabled" : "Overlay hidden");
        }

        while (resetRunKey.wasPressed()) {
            state.resetRun();
            SkyRunChat.feedback("Run reset — personal bests kept");
        }
    }
}
