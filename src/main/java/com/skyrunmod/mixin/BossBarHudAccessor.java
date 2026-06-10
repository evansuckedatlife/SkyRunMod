package com.skyrunmod.mixin;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;

/**
 * Exposes the client's active boss bars. {@link BossBarHud#bossBars} is package-private, so an
 * accessor mixin is the clean way to read boss names/health — used by slayer and Kuudra phase
 * detection. Read-only; no vanilla behavior is changed.
 */
@Mixin(BossBarHud.class)
public interface BossBarHudAccessor {
    @Accessor("bossBars")
    Map<UUID, ClientBossBar> skyrunmod$getBossBars();
}
