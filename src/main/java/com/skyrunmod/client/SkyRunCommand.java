package com.skyrunmod.client;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.skyrunmod.core.CommissionParser;
import com.skyrunmod.util.TextUtil;
import com.skyrunmod.util.TimeFormat;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Client-side {@code /skyrun} command. Runs entirely on the client (never sent to the server), so it
 * is safe on Hypixel.
 */
public final class SkyRunCommand {
    private SkyRunCommand() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("skyrun")
                        .executes(ctx -> help(ctx.getSource()))
                        .then(literal("help").executes(ctx -> help(ctx.getSource())))
                        .then(literal("reset").executes(SkyRunCommand::reset))
                        .then(literal("save").executes(SkyRunCommand::save))
                        .then(literal("toggle").executes(SkyRunCommand::toggle))
                        .then(literal("pb")
                                .then(argument("key", StringArgumentType.greedyString())
                                        .executes(SkyRunCommand::pb)))
                        .then(literal("sob")
                                .then(argument("prefix", StringArgumentType.greedyString())
                                        .executes(SkyRunCommand::sumOfBest)))
                        .then(literal("tab").executes(SkyRunCommand::dumpTab))));
    }

    private static int help(FabricClientCommandSource source) {
        source.sendFeedback(accent("SkyRunMod commands:"));
        source.sendFeedback(line("/skyrun reset", "start a fresh run (keeps PBs)"));
        source.sendFeedback(line("/skyrun save", "write PBs + settings to disk"));
        source.sendFeedback(line("/skyrun toggle", "show/hide the overlay"));
        source.sendFeedback(line("/skyrun pb <key>", "look up a personal best"));
        source.sendFeedback(line("/skyrun sob <prefix>", "sum of best for keys under a prefix"));
        source.sendFeedback(line("/skyrun tab", "debug: dump commission lines from the tab list"));
        return 1;
    }

    /**
     * Diagnostic: prints the commission-relevant tab lines and how the parser interprets them. Run
     * this in the Dwarven Mines / Crystal Hollows if commissions aren't tracking, so the exact
     * Hypixel wording can be matched.
     */
    private static int dumpTab(CommandContext<FabricClientCommandSource> ctx) {
        FabricClientCommandSource source = ctx.getSource();
        ClientPlayNetworkHandler handler = MinecraftClient.getInstance().getNetworkHandler();
        if (handler == null) {
            source.sendFeedback(Text.literal("Not connected to a server.").formatted(Formatting.RED));
            return 0;
        }

        List<String> lines = new ArrayList<>();
        for (PlayerListEntry entry : handler.getListedPlayerListEntries()) {
            Text displayName = entry.getDisplayName();
            if (displayName == null) {
                continue;
            }
            String clean = TextUtil.clean(displayName.getString());
            if (!clean.isEmpty()) {
                lines.add(clean);
            }
        }

        boolean header = CommissionParser.hasHeader(lines);
        Map<String, Double> parsed = CommissionParser.parse(lines);
        source.sendFeedback(accent("Commission header detected: " + (header ? "yes" : "no")));

        if (parsed.isEmpty()) {
            source.sendFeedback(Text.literal("Parsed commissions: none").formatted(Formatting.GRAY));
        } else {
            source.sendFeedback(accent("Parsed commissions:"));
            parsed.forEach((name, frac) ->
                    source.sendFeedback(line("  " + name, String.format("%.1f%%", frac * 100.0d))));
        }

        // Show candidate raw lines so a mismatched format is obvious.
        source.sendFeedback(accent("Tab lines containing ':' / '%' / 'DONE' / 'commission':"));
        int shown = 0;
        for (String l : lines) {
            String lower = l.toLowerCase();
            if (lower.contains("commission") || l.contains("%") || l.contains(":") || lower.contains("done")) {
                source.sendFeedback(Text.literal("  | " + l).formatted(Formatting.DARK_GRAY));
                if (++shown >= 30) {
                    break;
                }
            }
        }
        return 1;
    }

    private static int reset(CommandContext<FabricClientCommandSource> ctx) {
        SkyRunState state = SkyRunState.get();
        if (state == null) {
            return 0;
        }
        state.resetRun();
        ctx.getSource().sendFeedback(accent("Run reset — personal bests kept."));
        return 1;
    }

    private static int save(CommandContext<FabricClientCommandSource> ctx) {
        SkyRunState state = SkyRunState.get();
        if (state == null) {
            return 0;
        }
        state.persist();
        ctx.getSource().sendFeedback(accent("Personal bests + settings saved."));
        return 1;
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        SkyRunState state = SkyRunState.get();
        if (state == null) {
            return 0;
        }
        boolean enabled = !state.config().overlayEnabled;
        state.config().overlayEnabled = enabled;
        state.persist();
        ctx.getSource().sendFeedback(accent(enabled ? "Overlay enabled." : "Overlay hidden."));
        return 1;
    }

    private static int pb(CommandContext<FabricClientCommandSource> ctx) {
        SkyRunState state = SkyRunState.get();
        if (state == null) {
            return 0;
        }
        String key = StringArgumentType.getString(ctx, "key");
        Optional<Long> best = state.engine().personalBestMillis(key);
        if (best.isPresent()) {
            ctx.getSource().sendFeedback(line(key, TimeFormat.duration(best.get())));
        } else {
            ctx.getSource().sendFeedback(Text.literal("No PB recorded for '" + key + "'.").formatted(Formatting.RED));
        }
        return 1;
    }

    private static int sumOfBest(CommandContext<FabricClientCommandSource> ctx) {
        SkyRunState state = SkyRunState.get();
        if (state == null) {
            return 0;
        }
        String prefix = StringArgumentType.getString(ctx, "prefix");
        OptionalLong sob = state.engine().sumOfBest(prefix);
        if (sob.isPresent()) {
            ctx.getSource().sendFeedback(line("Sum of best (" + prefix + ")", TimeFormat.duration(sob.getAsLong())));
        } else {
            ctx.getSource().sendFeedback(Text.literal("No PBs under '" + prefix + "'.").formatted(Formatting.RED));
        }
        return 1;
    }

    private static Text accent(String text) {
        return Text.literal(text).formatted(Formatting.AQUA);
    }

    private static Text line(String label, String value) {
        return Text.literal(label + " ").formatted(Formatting.GRAY)
                .append(Text.literal(value).formatted(Formatting.WHITE));
    }
}
