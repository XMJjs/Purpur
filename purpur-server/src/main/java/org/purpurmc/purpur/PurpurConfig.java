package org.purpurmc.purpur;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.purpurmc.purpur.command.PurpurCommand;
import org.purpurmc.purpur.protocol.CarpetServerProtocol.CarpetRule;
import org.purpurmc.purpur.protocol.CarpetServerProtocol.CarpetRules;
import org.purpurmc.purpur.protocol.PcaSyncProtocol;
import org.purpurmc.purpur.protocol.bladeren.BladerenProtocol.PurpurFeature;
import org.purpurmc.purpur.protocol.bladeren.BladerenProtocol.PurpurFeatureSet;
import org.purpurmc.purpur.protocol.rei.REIServerProtocol;
import org.purpurmc.purpur.protocol.servux.logger.DataLogger;
import org.purpurmc.purpur.protocol.syncmatica.SyncmaticaProtocol;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.purpurmc.purpur.task.TPSBarTask;

@SuppressWarnings("unused")
public class PurpurConfig {
    private static final String HEADER = "This is the main configuration file for Purpur.\n"
            + "As you can see, there's tons to configure. Some options may impact gameplay, so use\n"
            + "with caution, and make sure you know what each option does before configuring.\n"
            + "\n"
            + "If you need help with the configuration or have any questions related to Purpur,\n"
            + "join us in our Discord guild.\n"
            + "\n"
            + "Website: https://purpurmc.org \n"
            + "Docs: https://purpurmc.org/docs \n";
    private static File CONFIG_FILE;
    public static YamlConfiguration config;

    private static Map<String, Command> commands;

    public static int version;
    static boolean verbose;

    public static void init(File configFile) {
        CONFIG_FILE = configFile;
        config = new YamlConfiguration();
        try {
            config.load(CONFIG_FILE);
        } catch (IOException ignore) {
        } catch (InvalidConfigurationException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not load purpur.yml, please correct your syntax errors", ex);
            throw Throwables.propagate(ex);
        }
        config.options().header(HEADER);
        config.options().copyDefaults(true);
        verbose = getBoolean("verbose", false);

        commands = new HashMap<>();
        commands.put("purpur", new PurpurCommand("purpur"));

        version = getInt("config-version", 48);
        set("config-version", 48);

        readConfig(PurpurConfig.class, null);

        // Purpur start - protocol config: ensure placement protocols always bypass the use-item distance check
        if (protocol.alternativeBlockPlacement != ProtocolConfig.AlternativePlaceType.NONE) {
            modify.disableDistanceCheckForUseItem = true;
        }
        // Purpur end - protocol config

        Block.BLOCK_STATE_REGISTRY.forEach(BlockBehaviour.BlockStateBase::initCache);
    }

    protected static void log(String s) {
        if (verbose) {
            log(Level.INFO, s);
        }
    }

    protected static void log(Level level, String s) {
        Bukkit.getLogger().log(level, s);
    }

    public static void registerCommands() {
        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            MinecraftServer.getServer().server.getCommandMap().register(entry.getKey(), "Purpur", entry.getValue());
        }
    }

    static void readConfig(Class<?> clazz, Object instance) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers())) {
                if (method.getParameterTypes().length == 0 && method.getReturnType() == Void.TYPE) {
                    try {
                        method.setAccessible(true);
                        method.invoke(instance);
                    } catch (InvocationTargetException ex) {
                        throw Throwables.propagate(ex.getCause());
                    } catch (Exception ex) {
                        Bukkit.getLogger().log(Level.SEVERE, "Error invoking " + method, ex);
                    }
                }
            }
        }

        try {
            config.save(CONFIG_FILE);
        } catch (IOException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Could not save " + CONFIG_FILE, ex);
        }
    }

    private static void set(String path, Object val) {
        config.addDefault(path, val);
        config.set(path, val);
    }

    private static String getString(String path, String def) {
        config.addDefault(path, def);
        return config.getString(path, config.getString(path));
    }

    private static boolean getBoolean(String path, boolean def) {
        config.addDefault(path, def);
        return config.getBoolean(path, config.getBoolean(path));
    }

    private static double getDouble(String path, double def) {
        config.addDefault(path, def);
        return config.getDouble(path, config.getDouble(path));
    }

    private static int getInt(String path, int def) {
        config.addDefault(path, def);
        return config.getInt(path, config.getInt(path));
    }

    private static <T> List<?> getList(String path, T def) {
        config.addDefault(path, def);
        return config.getList(path, config.getList(path));
    }

    static Map<String, Object> getMap(String path, Map<String, Object> def) {
        if (def != null && config.getConfigurationSection(path) == null) {
            config.addDefault(path, def);
            return def;
        }
        return toMap(config.getConfigurationSection(path));
    }

    private static Map<String, Object> toMap(ConfigurationSection section) {
        ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
        if (section != null) {
            for (String key : section.getKeys(false)) {
                Object obj = section.get(key);
                if (obj != null) {
                    builder.put(key, obj instanceof ConfigurationSection val ? toMap(val) : obj);
                }
            }
        }
        return builder.build();
    }

    public static String cannotRideMob = "<red>You cannot mount that mob";
    public static String afkBroadcastAway = "<yellow><italic>%s is now AFK";
    public static String afkBroadcastBack = "<yellow><italic>%s is no longer AFK";
    public static boolean afkBroadcastUseDisplayName = false;
    public static String afkTabListPrefix = "[AFK] ";
    public static String afkTabListSuffix = "";
    public static String creditsCommandOutput = "<green>%s has been shown the end credits";
    public static String demoCommandOutput = "<green>%s has been shown the demo screen";
    public static String pingCommandOutput = "<green>%s's ping is %sms";
    public static String ramCommandOutput = "<green>Ram Usage: <used>/<xmx> (<percent>)";
    public static String rambarCommandOutput = "<green>Rambar toggled <onoff> for <target>";
    public static String tpsbarCommandOutput = "<green>Tpsbar toggled <onoff> for <target>";
    public static String dontRunWithScissors = "<red><italic>Don't run with scissors!";
    public static String uptimeCommandOutput = "<green>Server uptime is <uptime>";
    public static String unverifiedUsername = "default";
    public static String sleepSkippingNight = "default";
    public static String sleepingPlayersPercent = "default";
    public static String sleepNotPossible = "default";
    private static void messages() {
        cannotRideMob = getString("settings.messages.cannot-ride-mob", cannotRideMob);
        afkBroadcastAway = getString("settings.messages.afk-broadcast-away", afkBroadcastAway);
        afkBroadcastBack = getString("settings.messages.afk-broadcast-back", afkBroadcastBack);
        afkBroadcastUseDisplayName = getBoolean("settings.messages.afk-broadcast-use-display-name", afkBroadcastUseDisplayName);
        afkTabListPrefix = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(getString("settings.messages.afk-tab-list-prefix", afkTabListPrefix)));
        afkTabListSuffix = MiniMessage.miniMessage().serialize(MiniMessage.miniMessage().deserialize(getString("settings.messages.afk-tab-list-suffix", afkTabListSuffix)));
        creditsCommandOutput = getString("settings.messages.credits-command-output", creditsCommandOutput);
        demoCommandOutput = getString("settings.messages.demo-command-output", demoCommandOutput);
        pingCommandOutput = getString("settings.messages.ping-command-output", pingCommandOutput);
        ramCommandOutput = getString("settings.messages.ram-command-output", ramCommandOutput);
        rambarCommandOutput = getString("settings.messages.rambar-command-output", rambarCommandOutput);
        tpsbarCommandOutput = getString("settings.messages.tpsbar-command-output", tpsbarCommandOutput);
        dontRunWithScissors = getString("settings.messages.dont-run-with-scissors", dontRunWithScissors);
        uptimeCommandOutput = getString("settings.messages.uptime-command-output", uptimeCommandOutput);
        unverifiedUsername = getString("settings.messages.unverified-username", unverifiedUsername);
        sleepSkippingNight = getString("settings.messages.sleep-skipping-night", sleepSkippingNight);
        sleepingPlayersPercent = getString("settings.messages.sleeping-players-percent", sleepingPlayersPercent);
        sleepNotPossible = getString("settings.messages.sleep-not-possible", sleepNotPossible);
    }

    public static String deathMsgRunWithScissors = "<player> slipped and fell on their shears";
    public static String deathMsgStonecutter = "<player> has sawed themself in half";
    private static void deathMessages() {
        deathMsgRunWithScissors = getString("settings.messages.death-message.run-with-scissors", deathMsgRunWithScissors);
        deathMsgStonecutter = getString("settings.messages.death-message.stonecutter", deathMsgStonecutter);
    }

    public static boolean advancementOnlyBroadcastToAffectedPlayer = false;
    public static boolean deathMessageOnlyBroadcastToAffectedPlayer = false;
    private static void broadcastSettings() {
        if (version < 13) {
            boolean oldValue = getBoolean("settings.advancement.only-broadcast-to-affected-player", false);
            set("settings.broadcasts.advancement.only-broadcast-to-affected-player", oldValue);
            set("settings.advancement.only-broadcast-to-affected-player", null);
        }
        advancementOnlyBroadcastToAffectedPlayer  = getBoolean("settings.broadcasts.advancement.only-broadcast-to-affected-player", advancementOnlyBroadcastToAffectedPlayer);
        deathMessageOnlyBroadcastToAffectedPlayer = getBoolean("settings.broadcasts.death.only-broadcast-to-affected-player", deathMessageOnlyBroadcastToAffectedPlayer);
    }

    public static String serverModName = io.papermc.paper.ServerBuildInfo.buildInfo().brandName();
    private static void serverModName() {
        serverModName = getString("settings.server-mod-name", serverModName);
    }

    public static double laggingThreshold = 19.0D;
    private static void tickLoopSettings() {
        laggingThreshold = getDouble("settings.lagging-threshold", laggingThreshold);
    }

    public static boolean useAlternateKeepAlive = false;
    private static void useAlternateKeepAlive() {
        useAlternateKeepAlive = getBoolean("settings.use-alternate-keepalive", useAlternateKeepAlive);
    }

    public static boolean disableGiveCommandDrops = false;
    private static void disableGiveCommandDrops() {
        disableGiveCommandDrops = getBoolean("settings.disable-give-dropping", disableGiveCommandDrops);
    }

    public static String commandRamBarTitle = "<gray>Ram<yellow>:</yellow> <used>/<xmx> (<percent>)";
    public static BossBar.Overlay commandRamBarProgressOverlay = BossBar.Overlay.NOTCHED_20;
    public static BossBar.Color commandRamBarProgressColorGood = BossBar.Color.GREEN;
    public static BossBar.Color commandRamBarProgressColorMedium = BossBar.Color.YELLOW;
    public static BossBar.Color commandRamBarProgressColorLow = BossBar.Color.RED;
    public static String commandRamBarTextColorGood = "<gradient:#55ff55:#00aa00><text></gradient>";
    public static String commandRamBarTextColorMedium = "<gradient:#ffff55:#ffaa00><text></gradient>";
    public static String commandRamBarTextColorLow = "<gradient:#ff5555:#aa0000><text></gradient>";
    public static int commandRamBarTickInterval = 20;
    public static String commandTPSBarTitle = "<gray>TPS<yellow>:</yellow> <tps> MSPT<yellow>:</yellow> <mspt> Ping<yellow>:</yellow> <ping>ms";
    public static BossBar.Overlay commandTPSBarProgressOverlay = BossBar.Overlay.NOTCHED_20;
    public static TPSBarTask.FillMode commandTPSBarProgressFillMode = TPSBarTask.FillMode.MSPT;
    public static BossBar.Color commandTPSBarProgressColorGood = BossBar.Color.GREEN;
    public static BossBar.Color commandTPSBarProgressColorMedium = BossBar.Color.YELLOW;
    public static BossBar.Color commandTPSBarProgressColorLow = BossBar.Color.RED;
    public static String commandTPSBarTextColorGood = "<gradient:#55ff55:#00aa00><text></gradient>";
    public static String commandTPSBarTextColorMedium = "<gradient:#ffff55:#ffaa00><text></gradient>";
    public static String commandTPSBarTextColorLow = "<gradient:#ff5555:#aa0000><text></gradient>";
    public static int commandTPSBarTickInterval = 20;
    public static String commandCompassBarTitle = "S  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  SW  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  W  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  NW  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  N  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  NE  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  E  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  SE  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  S  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  SW  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  W  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  NW  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  N  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  NE  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  E  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  SE  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  \u25C8  \u00B7  ";
    public static BossBar.Overlay commandCompassBarProgressOverlay = BossBar.Overlay.PROGRESS;
    public static BossBar.Color commandCompassBarProgressColor = BossBar.Color.BLUE;
    public static float commandCompassBarProgressPercent = 1.0F;
    public static int commandCompassBarTickInterval = 5;
    public static boolean commandGamemodeRequiresPermission = false;
    public static boolean hideHiddenPlayersFromEntitySelector = false;
    public static String uptimeFormat = "<days><hours><minutes><seconds>";
    public static String uptimeDay = "%02d day, ";
    public static String uptimeDays = "%02d days, ";
    public static String uptimeHour = "%02d hour, ";
    public static String uptimeHours = "%02d hours, ";
    public static String uptimeMinute = "%02d minute, and ";
    public static String uptimeMinutes = "%02d minutes, and ";
    public static String uptimeSecond = "%02d second";
    public static String uptimeSeconds = "%02d seconds";
    private static void commandSettings() {
        commandRamBarTitle = getString("settings.command.rambar.title", commandRamBarTitle);
        commandRamBarProgressOverlay = BossBar.Overlay.valueOf(getString("settings.command.rambar.overlay", commandRamBarProgressOverlay.name()));
        commandRamBarProgressColorGood = BossBar.Color.valueOf(getString("settings.command.rambar.progress-color.good", commandRamBarProgressColorGood.name()));
        commandRamBarProgressColorMedium = BossBar.Color.valueOf(getString("settings.command.rambar.progress-color.medium", commandRamBarProgressColorMedium.name()));
        commandRamBarProgressColorLow = BossBar.Color.valueOf(getString("settings.command.rambar.progress-color.low", commandRamBarProgressColorLow.name()));
        commandRamBarTextColorGood = getString("settings.command.rambar.text-color.good", commandRamBarTextColorGood);
        commandRamBarTextColorMedium = getString("settings.command.rambar.text-color.medium", commandRamBarTextColorMedium);
        commandRamBarTextColorLow = getString("settings.command.rambar.text-color.low", commandRamBarTextColorLow);
        commandRamBarTickInterval = getInt("settings.command.rambar.tick-interval", commandRamBarTickInterval);

        commandTPSBarTitle = getString("settings.command.tpsbar.title", commandTPSBarTitle);
        commandTPSBarProgressOverlay = BossBar.Overlay.valueOf(getString("settings.command.tpsbar.overlay", commandTPSBarProgressOverlay.name()));
        commandTPSBarProgressFillMode = TPSBarTask.FillMode.valueOf(getString("settings.command.tpsbar.fill-mode", commandTPSBarProgressFillMode.name()));
        commandTPSBarProgressColorGood = BossBar.Color.valueOf(getString("settings.command.tpsbar.progress-color.good", commandTPSBarProgressColorGood.name()));
        commandTPSBarProgressColorMedium = BossBar.Color.valueOf(getString("settings.command.tpsbar.progress-color.medium", commandTPSBarProgressColorMedium.name()));
        commandTPSBarProgressColorLow = BossBar.Color.valueOf(getString("settings.command.tpsbar.progress-color.low", commandTPSBarProgressColorLow.name()));
        commandTPSBarTextColorGood = getString("settings.command.tpsbar.text-color.good", commandTPSBarTextColorGood);
        commandTPSBarTextColorMedium = getString("settings.command.tpsbar.text-color.medium", commandTPSBarTextColorMedium);
        commandTPSBarTextColorLow = getString("settings.command.tpsbar.text-color.low", commandTPSBarTextColorLow);
        commandTPSBarTickInterval = getInt("settings.command.tpsbar.tick-interval", commandTPSBarTickInterval);

        commandCompassBarTitle = getString("settings.command.compass.title", commandCompassBarTitle);
        commandCompassBarProgressOverlay = BossBar.Overlay.valueOf(getString("settings.command.compass.overlay", commandCompassBarProgressOverlay.name()));
        commandCompassBarProgressColor = BossBar.Color.valueOf(getString("settings.command.compass.progress-color", commandCompassBarProgressColor.name()));
        commandCompassBarProgressPercent = (float) getDouble("settings.command.compass.percent", commandCompassBarProgressPercent);
        commandCompassBarTickInterval = getInt("settings.command.compass.tick-interval", commandCompassBarTickInterval);

        commandGamemodeRequiresPermission = getBoolean("settings.command.gamemode.requires-specific-permission", commandGamemodeRequiresPermission);
        hideHiddenPlayersFromEntitySelector = getBoolean("settings.command.hide-hidden-players-from-entity-selector", hideHiddenPlayersFromEntitySelector);
        uptimeFormat = getString("settings.command.uptime.format", uptimeFormat);
        uptimeDay = getString("settings.command.uptime.day", uptimeDay);
        uptimeDays = getString("settings.command.uptime.days", uptimeDays);
        uptimeHour = getString("settings.command.uptime.hour", uptimeHour);
        uptimeHours = getString("settings.command.uptime.hours", uptimeHours);
        uptimeMinute = getString("settings.command.uptime.minute", uptimeMinute);
        uptimeMinutes = getString("settings.command.uptime.minutes", uptimeMinutes);
        uptimeSecond = getString("settings.command.uptime.second", uptimeSecond);
        uptimeSeconds = getString("settings.command.uptime.seconds", uptimeSeconds);
    }

    public static int barrelRows = 3;
    public static boolean enderChestSixRows = false;
    public static boolean enderChestPermissionRows = false;
    public static boolean enderChestPersistHiddenRows = true;
    public static boolean cryingObsidianValidForPortalFrame = false;
    public static int beeInsideBeeHive = 3;
    public static boolean anvilCumulativeCost = true;
    public static int smoothSnowAccumulationStep = 0;
    public static int lightningRodRange = 128;
    public static Set<Enchantment> grindstoneIgnoredEnchants = new HashSet<>();
    public static boolean grindstoneRemoveAttributes = false;
    public static boolean grindstoneRemoveDisplay = false;
    public static int caveVinesMaxGrowthAge = 25;
    public static int kelpMaxGrowthAge = 25;
    public static int twistingVinesMaxGrowthAge = 25;
    public static int weepingVinesMaxGrowthAge = 25;
    public static boolean magmaBlockReverseBubbleColumnFlow = false;
    public static boolean soulSandBlockReverseBubbleColumnFlow = false;
    private static void blockSettings() {
        if (version < 3) {
            boolean oldValue = getBoolean("settings.barrel.packed-barrels", true);
            set("settings.blocks.barrel.six-rows", oldValue);
            set("settings.packed-barrels", null);
            oldValue = getBoolean("settings.large-ender-chests", true);
            set("settings.blocks.ender_chest.six-rows", oldValue);
            set("settings.large-ender-chests", null);
        }
        if (version < 20) {
            boolean oldValue = getBoolean("settings.blocks.barrel.six-rows", false);
            set("settings.blocks.barrel.rows", oldValue ? 6 : 3);
            set("settings.blocks.barrel.six-rows", null);
        }
        barrelRows = getInt("settings.blocks.barrel.rows", barrelRows);
        if (barrelRows < 1 || barrelRows > 6) {
            Bukkit.getLogger().severe("settings.blocks.barrel.rows must be 1-6, resetting to default");
            barrelRows = 3;
        }
        org.bukkit.event.inventory.InventoryType.BARREL.setDefaultSize(switch (barrelRows) {
            case 6 -> 54;
            case 5 -> 45;
            case 4 -> 36;
            case 2 -> 18;
            case 1 -> 9;
            default -> 27;
        });
        enderChestSixRows = getBoolean("settings.blocks.ender_chest.six-rows", enderChestSixRows);
        org.bukkit.event.inventory.InventoryType.ENDER_CHEST.setDefaultSize(enderChestSixRows ? 54 : 27);
        enderChestPermissionRows = getBoolean("settings.blocks.ender_chest.use-permissions-for-rows", enderChestPermissionRows);
        enderChestPersistHiddenRows = getBoolean("settings.blocks.ender_chest.persist-hidden-rows", enderChestPersistHiddenRows);
        cryingObsidianValidForPortalFrame = getBoolean("settings.blocks.crying_obsidian.valid-for-portal-frame", cryingObsidianValidForPortalFrame);
        beeInsideBeeHive = getInt("settings.blocks.beehive.max-bees-inside", beeInsideBeeHive);
        anvilCumulativeCost = getBoolean("settings.blocks.anvil.cumulative-cost", anvilCumulativeCost);
        smoothSnowAccumulationStep = getInt("settings.blocks.snow.smooth-accumulation-step", smoothSnowAccumulationStep);
        if (smoothSnowAccumulationStep > 7) {
            smoothSnowAccumulationStep = 7;
            log(Level.WARNING, "blocks.snow.smooth-accumulation-step is set to above maximum allowed value of 7");
            log(Level.WARNING, "Using value of 7 to prevent issues");
        } else if (smoothSnowAccumulationStep < 0) {
            smoothSnowAccumulationStep = 0;
            log(Level.WARNING, "blocks.snow.smooth-accumulation-step is set to below minimum allowed value of 0");
            log(Level.WARNING, "Using value of 0 to prevent issues");
        }
        lightningRodRange = getInt("settings.blocks.lightning_rod.range", lightningRodRange);
        ArrayList<String> defaultCurses = new ArrayList<>(){{
            add("minecraft:binding_curse");
            add("minecraft:vanishing_curse");
        }};
        if (version < 24 && !getBoolean("settings.blocks.grindstone.ignore-curses", true)) {
            defaultCurses.clear();
        }
        getList("settings.blocks.grindstone.ignored-enchants", defaultCurses).forEach(key -> {
            Registry<Enchantment> registry = MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            Enchantment enchantment = registry.getValue(Identifier.parse(key.toString()));
            if (enchantment == null) return;
            grindstoneIgnoredEnchants.add(enchantment);
        });
        grindstoneRemoveAttributes = getBoolean("settings.blocks.grindstone.remove-attributes", grindstoneRemoveAttributes);
        grindstoneRemoveDisplay = getBoolean("settings.blocks.grindstone.remove-name-and-lore", grindstoneRemoveDisplay);
        caveVinesMaxGrowthAge = getInt("settings.blocks.cave_vines.max-growth-age", caveVinesMaxGrowthAge);
        if (caveVinesMaxGrowthAge > 25) {
            caveVinesMaxGrowthAge = 25;
            log(Level.WARNING, "blocks.cave_vines.max-growth-age is set to above maximum allowed value of 25");
            log(Level.WARNING, "Using value of 25 to prevent issues");
        }
        kelpMaxGrowthAge = getInt("settings.blocks.kelp.max-growth-age", kelpMaxGrowthAge);
        if (kelpMaxGrowthAge > 25) {
            kelpMaxGrowthAge = 25;
            log(Level.WARNING, "blocks.kelp.max-growth-age is set to above maximum allowed value of 25");
            log(Level.WARNING, "Using value of 25 to prevent issues");
        }
        twistingVinesMaxGrowthAge = getInt("settings.blocks.twisting_vines.max-growth-age", twistingVinesMaxGrowthAge);
        if (twistingVinesMaxGrowthAge > 25) {
            twistingVinesMaxGrowthAge = 25;
            log(Level.WARNING, "blocks.twisting_vines.max-growth-age is set to above maximum allowed value of 25");
            log(Level.WARNING, "Using value of 25 to prevent issues");
        }
        weepingVinesMaxGrowthAge = getInt("settings.blocks.weeping_vines.max-growth-age", weepingVinesMaxGrowthAge);
        if (weepingVinesMaxGrowthAge > 25) {
            weepingVinesMaxGrowthAge = 25;
            log(Level.WARNING, "blocks.weeping_vines.max-growth-age is set to above maximum allowed value of 25");
            log(Level.WARNING, "Using value of 25 to prevent issues");
        }
        magmaBlockReverseBubbleColumnFlow = getBoolean("settings.blocks.magma-block.reverse-bubble-column-flow", magmaBlockReverseBubbleColumnFlow);
        soulSandBlockReverseBubbleColumnFlow = getBoolean("settings.blocks.soul-sand.reverse-bubble-column-flow", soulSandBlockReverseBubbleColumnFlow);
    }

    public static boolean allowInapplicableEnchants = false;
    public static boolean allowIncompatibleEnchants = false;
    public static boolean allowHigherEnchantsLevels = false;
    public static boolean allowUnsafeEnchantCommand = false;
    public static boolean replaceIncompatibleEnchants = false;
    public static boolean clampEnchantLevels = true;
    private static void enchantmentSettings() {
        if (version < 30) {
            boolean oldValue = getBoolean("settings.enchantment.allow-unsafe-enchants", false);
            set("settings.enchantment.anvil.allow-unsafe-enchants", oldValue);
            set("settings.enchantment.anvil.allow-inapplicable-enchants", true);
            set("settings.enchantment.anvil.allow-incompatible-enchants", true);
            set("settings.enchantment.anvil.allow-higher-enchants-levels", true);
            set("settings.enchantment.allow-unsafe-enchants", null);
        }
        if (version < 37) {
            boolean allowUnsafeEnchants = getBoolean("settings.enchantment.anvil.allow-unsafe-enchants", false);
            if (!allowUnsafeEnchants) {
                set("settings.enchantment.anvil.allow-inapplicable-enchants", false);
                set("settings.enchantment.anvil.allow-incompatible-enchants", false);
                set("settings.enchantment.anvil.allow-higher-enchants-levels", false);
            }
            set("settings.enchantment.anvil.allow-unsafe-enchants", null);
        }
        allowInapplicableEnchants = getBoolean("settings.enchantment.anvil.allow-inapplicable-enchants", allowInapplicableEnchants);
        allowIncompatibleEnchants = getBoolean("settings.enchantment.anvil.allow-incompatible-enchants", allowIncompatibleEnchants);
        allowHigherEnchantsLevels = getBoolean("settings.enchantment.anvil.allow-higher-enchants-levels", allowHigherEnchantsLevels);
        allowUnsafeEnchantCommand = getBoolean("settings.enchantment.allow-unsafe-enchant-command", allowUnsafeEnchantCommand);
        replaceIncompatibleEnchants = getBoolean("settings.enchantment.anvil.replace-incompatible-enchants", replaceIncompatibleEnchants);
        clampEnchantLevels = getBoolean("settings.enchantment.clamp-levels", clampEnchantLevels);
    }

    public static boolean endermanShortHeight = false;
    private static void entitySettings() {
        endermanShortHeight = getBoolean("settings.entity.enderman.short-height", endermanShortHeight);
        if (endermanShortHeight) EntityType.ENDERMAN.dimensions = EntityDimensions.scalable(0.6F, 1.9F);
    }

    public static boolean allowWaterPlacementInTheEnd = true;
    private static void allowWaterPlacementInEnd() {
        allowWaterPlacementInTheEnd = getBoolean("settings.allow-water-placement-in-the-end", allowWaterPlacementInTheEnd);
    }

    public static boolean beeCountPayload = false;
    private static void beeCountPayload() {
        beeCountPayload = getBoolean("settings.bee-count-payload", beeCountPayload);
    }

    public static boolean loggerSuppressInitLegacyMaterialError = false;
    public static boolean loggerSuppressIgnoredAdvancementWarnings = false;
    public static boolean loggerSuppressUnrecognizedRecipeErrors = false;
    public static boolean loggerSuppressSetBlockFarChunk = false;
    public static boolean loggerSuppressLibraryLoader = false;
    private static void loggerSettings() {
        loggerSuppressInitLegacyMaterialError = getBoolean("settings.logger.suppress-init-legacy-material-errors", loggerSuppressInitLegacyMaterialError);
        loggerSuppressIgnoredAdvancementWarnings = getBoolean("settings.logger.suppress-ignored-advancement-warnings", loggerSuppressIgnoredAdvancementWarnings);
        loggerSuppressUnrecognizedRecipeErrors = getBoolean("settings.logger.suppress-unrecognized-recipe-errors", loggerSuppressUnrecognizedRecipeErrors);
        loggerSuppressSetBlockFarChunk = getBoolean("settings.logger.suppress-setblock-in-far-chunk-errors", loggerSuppressSetBlockFarChunk);
        loggerSuppressLibraryLoader = getBoolean("settings.logger.suppress-library-loader", loggerSuppressLibraryLoader);
        org.bukkit.plugin.java.JavaPluginLoader.SuppressLibraryLoaderLogger = loggerSuppressLibraryLoader;
    }

    public static boolean tpsCatchup = true;
    private static void tpsCatchup() {
        tpsCatchup = getBoolean("settings.tps-catchup", tpsCatchup);
    }

    public static boolean useUPnP = false;
    public static boolean maxJoinsPerSecond = false;
    public static boolean kickForOutOfOrderChat = true;
    private static void networkSettings() {
        useUPnP = getBoolean("settings.network.upnp-port-forwarding", useUPnP);
        maxJoinsPerSecond = getBoolean("settings.network.max-joins-per-second", maxJoinsPerSecond);
        kickForOutOfOrderChat = getBoolean("settings.network.kick-for-out-of-order-chat", kickForOutOfOrderChat);
    }

    public static Pattern usernameValidCharactersPattern;
    private static void usernameValidationSettings() {
        String defaultPattern = "^[a-zA-Z0-9_.]*$";
        String setPattern = getString("settings.username-valid-characters", defaultPattern);
        usernameValidCharactersPattern = Pattern.compile(setPattern == null || setPattern.isBlank() ? defaultPattern : setPattern);
    }

    public static boolean fixProjectileLootingTransfer = false;
    private static void fixProjectileLootingTransfer() {
        fixProjectileLootingTransfer = getBoolean("settings.fix-projectile-looting-transfer", fixProjectileLootingTransfer);
    }

    public static boolean clampAttributes = true;
    private static void clampAttributes() {
        clampAttributes = getBoolean("settings.clamp-attributes", clampAttributes);
    }

    public static boolean limitArmor = true;
    private static void limitArmor() {
        limitArmor = getBoolean("settings.limit-armor", limitArmor);
    }

    private static void blastResistanceSettings() {
        getMap("settings.blast-resistance-overrides", Collections.emptyMap()).forEach((blockId, value) -> {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
            if (block == Blocks.AIR) {
                log(Level.SEVERE, "Invalid block for `settings.blast-resistance-overrides`: " + blockId);
                return;
            }
            if (!(value instanceof Number blastResistance)) {
                log(Level.SEVERE, "Invalid blast resistance for `settings.blast-resistance-overrides." + blockId + "`: " + value);
                return;
            }
            block.explosionResistance = blastResistance.floatValue();
        });
    }
    private static void blockFallMultiplierSettings() {
        getMap("settings.block-fall-multipliers", Map.ofEntries(
                Map.entry("minecraft:hay_block", Map.of("damage", 0.2F)),
                Map.entry("minecraft:white_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:light_gray_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:gray_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:black_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:brown_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:pink_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:red_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:orange_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:yellow_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:green_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:lime_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:cyan_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:light_blue_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:blue_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:purple_bed", Map.of("distance", 0.5F)),
                Map.entry("minecraft:magenta_bed", Map.of("distance", 0.5F))
        )).forEach((blockId, value) -> {
            Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockId));
            if (block == Blocks.AIR) {
                log(Level.SEVERE, "Invalid block for `settings.block-fall-multipliers`: " + blockId);
                return;
            }
            if (!(value instanceof Map<?, ?> map)) {
                log(Level.SEVERE, "Invalid fall multiplier for `settings.block-fall-multipliers." + blockId + "`: " + value
                        + ", expected a map with keys `damage` and `distance` to floats.");
                return;
            }
            Object rawFallDamageMultiplier = map.get("damage");
            if (rawFallDamageMultiplier == null) rawFallDamageMultiplier = 1F;
            if (!(rawFallDamageMultiplier instanceof Number fallDamageMultiplier)) {
                log(Level.SEVERE, "Invalid multiplier for `settings.block-fall-multipliers." + blockId + ".damage`: " + map.get("damage"));
                return;
            }
            Object rawFallDistanceMultiplier = map.get("distance");
            if (rawFallDistanceMultiplier == null) rawFallDistanceMultiplier = 1F;
            if (!(rawFallDistanceMultiplier instanceof Number fallDistanceMultiplier)) {
                log(Level.SEVERE, "Invalid multiplier for `settings.block-fall-multipliers." + blockId + ".distance`: " + map.get("distance"));
                return;
            }
            block.fallDamageMultiplier = fallDamageMultiplier.floatValue();
            block.fallDistanceMultiplier = fallDistanceMultiplier.floatValue();
        });
    }

    public static boolean playerDeathsAlwaysShowItem = false;
    private static void playerDeathsAlwaysShowItem() {
        playerDeathsAlwaysShowItem = getBoolean("settings.player-deaths-always-show-item", playerDeathsAlwaysShowItem);
    }

    public static boolean registerMinecraftDebugCommands = false;
    private static void registerMinecraftDebugCommands() {
        registerMinecraftDebugCommands = getBoolean("settings.register-minecraft-debug-commands", registerMinecraftDebugCommands);
    }

    public static boolean registerMinecraftDisabledCommands = false;
    private static void registerMinecraftDisabledCommands() {
        registerMinecraftDisabledCommands = getBoolean("settings.register-minecraft-disabled-commands", registerMinecraftDebugCommands);
    }

    public static List<String> startupCommands = new ArrayList<>();
    private static void startupCommands() {
        startupCommands.clear();
        getList("settings.startup-commands", new ArrayList<String>()).forEach(line -> {
            String command = line.toString();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            startupCommands.add(command);
        });
    }

    // Purpur start - protocol config (ported from Leaves)
    public static ModifyConfig modify = new ModifyConfig();
    public static ProtocolConfig protocol = new ProtocolConfig();

    public static class ModifyConfig {
        public boolean disableDistanceCheckForUseItem = false;
        public boolean disablePacketLimit = false;
    }

    private static void modifySettings() {
        modify.disableDistanceCheckForUseItem = getBoolean("settings.modify.disable-distance-check-for-use-item", modify.disableDistanceCheckForUseItem);
        modify.disablePacketLimit = getBoolean("settings.modify.disable-packet-limit", modify.disablePacketLimit);
    }

    public static class ProtocolConfig {
        public boolean strictMode = false;
        public CarpetConfig carpet = new CarpetConfig();
        public BladerenConfig bladeren = new BladerenConfig();
        public SyncmaticaConfig syncmatica = new SyncmaticaConfig();
        public PCAConfig pca = new PCAConfig();
        public AppleSkinConfig appleskin = new AppleSkinConfig();
        public ServuxConfig servux = new ServuxConfig();
        public boolean bborProtocol = false;
        public boolean jadeProtocol = false;
        public AlternativePlaceType alternativeBlockPlacement = AlternativePlaceType.NONE;
        public boolean xaeroMapProtocol = false;
        public int xaeroMapServerID = new java.util.Random().nextInt();
        public boolean leavesCarpetSupport = false;
        public boolean reiServerProtocol = false;

        public static class CarpetConfig {
            public boolean movableAmethyst = true;
            public boolean creativeNoClip = false;
            public boolean avoidAnvilTooExpensive = false;
            public boolean renewableCoral = false;
        }

        public static class BladerenConfig {
            public boolean enable = true;
            public boolean msptSyncProtocol = false;
            public int msptSyncTickInterval = 20;
        }

        public static class SyncmaticaConfig {
            public boolean enable = false;
            public boolean useQuota = false;
            public int quotaLimit = 40000000;
        }

        public static class PCAConfig {
            public boolean enable = false;
            public PcaPlayerEntityType syncPlayerEntity = PcaPlayerEntityType.OPS;
        }

        public enum PcaPlayerEntityType {
            NOBODY, BOT, OPS, OPS_AND_SELF, EVERYONE
        }

        public static class AppleSkinConfig {
            public boolean enable = false;
            public int syncTickInterval = 20;
        }

        public static class ServuxConfig {
            public boolean structureProtocol = false;
            public boolean entityProtocol = false;
            public boolean hudMetadataProtocol = false;
            public boolean hudLoggerProtocol = false;
            public List<DataLogger.Type> hudEnabledLoggers = List.of(DataLogger.Type.TPS, DataLogger.Type.MOB_CAPS);
            public int hudUpdateInterval = 1;
            public boolean hudMetadataShareSeed = true;
            public LitematicsConfig litematics = new LitematicsConfig();
        }

        public static class LitematicsConfig {
            public boolean enable = false;
            public long maxNbtSize = 2097152L;
        }

        public enum AlternativePlaceType {
            NONE, CARPET, CARPET_FIX, LITEMATICA
        }
    }

    private static void protocolSection() {
        protocol.strictMode = getBoolean("settings.protocol.strict-mode", protocol.strictMode);

        // carpet rules exposed through the carpet protocol
        protocol.carpet.movableAmethyst = getBoolean("settings.protocol.carpet.movable-amethyst", protocol.carpet.movableAmethyst);
        protocol.carpet.creativeNoClip = getBoolean("settings.protocol.carpet.creative-no-clip", protocol.carpet.creativeNoClip);
        protocol.carpet.avoidAnvilTooExpensive = getBoolean("settings.protocol.carpet.avoid-anvil-too-expensive", protocol.carpet.avoidAnvilTooExpensive);
        protocol.carpet.renewableCoral = getBoolean("settings.protocol.carpet.renewable-coral", protocol.carpet.renewableCoral);
        CarpetRules.register(CarpetRule.of("carpet", "movableAmethyst", protocol.carpet.movableAmethyst));
        CarpetRules.register(CarpetRule.of("carpet", "creativeNoClip", protocol.carpet.creativeNoClip));
        CarpetRules.register(CarpetRule.of("pca", "avoidAnvilTooExpensive", protocol.carpet.avoidAnvilTooExpensive));
        CarpetRules.register(CarpetRule.of("carpet", "renewableCoral", protocol.carpet.renewableCoral));

        protocol.bladeren.enable = getBoolean("settings.protocol.bladeren.protocol", protocol.bladeren.enable);
        protocol.bladeren.msptSyncTickInterval = getInt("settings.protocol.bladeren.mspt-sync-tick-interval", protocol.bladeren.msptSyncTickInterval);
        boolean oldMsptSync = protocol.bladeren.msptSyncProtocol;
        protocol.bladeren.msptSyncProtocol = getBoolean("settings.protocol.bladeren.mspt-sync-protocol", protocol.bladeren.msptSyncProtocol);
        if (oldMsptSync != protocol.bladeren.msptSyncProtocol) {
            PurpurFeatureSet.register(PurpurFeature.of("mspt_sync", protocol.bladeren.msptSyncProtocol));
        }

        boolean oldSyncmatica = protocol.syncmatica.enable;
        protocol.syncmatica.enable = getBoolean("settings.protocol.syncmatica.enable", protocol.syncmatica.enable);
        if (oldSyncmatica != protocol.syncmatica.enable) {
            SyncmaticaProtocol.init(protocol.syncmatica.enable);
        }
        protocol.syncmatica.useQuota = getBoolean("settings.protocol.syncmatica.quota", protocol.syncmatica.useQuota);
        protocol.syncmatica.quotaLimit = getInt("settings.protocol.syncmatica.quota-limit", protocol.syncmatica.quotaLimit);

        boolean oldPca = protocol.pca.enable;
        protocol.pca.enable = getBoolean("settings.protocol.pca.pca-sync-protocol", protocol.pca.enable);
        if (oldPca != protocol.pca.enable) {
            PcaSyncProtocol.onConfigModify(protocol.pca.enable);
        }
        protocol.pca.syncPlayerEntity = ProtocolConfig.PcaPlayerEntityType.valueOf(getString("settings.protocol.pca.pca-sync-player-entity", protocol.pca.syncPlayerEntity.name()));

        protocol.appleskin.enable = getBoolean("settings.protocol.appleskin.protocol", protocol.appleskin.enable);
        protocol.appleskin.syncTickInterval = getInt("settings.protocol.appleskin.sync-tick-interval", protocol.appleskin.syncTickInterval);

        protocol.servux.structureProtocol = getBoolean("settings.protocol.servux.structure-protocol", protocol.servux.structureProtocol);
        protocol.servux.entityProtocol = getBoolean("settings.protocol.servux.entity-protocol", protocol.servux.entityProtocol);
        protocol.servux.hudMetadataProtocol = getBoolean("settings.protocol.servux.hud-metadata-protocol", protocol.servux.hudMetadataProtocol);
        protocol.servux.hudLoggerProtocol = getBoolean("settings.protocol.servux.hud-logger-protocol", protocol.servux.hudLoggerProtocol);
        protocol.servux.hudUpdateInterval = getInt("settings.protocol.servux.hud-update-interval", protocol.servux.hudUpdateInterval);
        protocol.servux.hudMetadataShareSeed = getBoolean("settings.protocol.servux.hud-metadata-protocol-share-seed", protocol.servux.hudMetadataShareSeed);

        protocol.servux.hudEnabledLoggers = new ArrayList<>();
        for (Object obj : getList("settings.protocol.servux.hud-enabled-loggers", List.of("TPS", "MOB_CAPS"))) {
            DataLogger.Type type = DataLogger.Type.fromStringStatic(obj.toString());
            if (type != null) {
                protocol.servux.hudEnabledLoggers.add(type);
            }
        }

        boolean oldLitematics = protocol.servux.litematics.enable;
        protocol.servux.litematics.enable = getBoolean("settings.protocol.servux.litematics.enable", protocol.servux.litematics.enable);
        if (oldLitematics != protocol.servux.litematics.enable) {
            PluginManager pluginManager = Bukkit.getServer().getPluginManager();
            if (protocol.servux.litematics.enable) {
                if (pluginManager.getPermission("purpur.protocol.litematics") == null) {
                    pluginManager.addPermission(new Permission("purpur.protocol.litematics", PermissionDefault.OP));
                }
            } else {
                pluginManager.removePermission("purpur.protocol.litematics");
            }
        }
        protocol.servux.litematics.maxNbtSize = getLong("settings.protocol.servux.litematics.max-nbt-size", protocol.servux.litematics.maxNbtSize);

        protocol.bborProtocol = getBoolean("settings.protocol.bbor-protocol", protocol.bborProtocol);
        protocol.jadeProtocol = getBoolean("settings.protocol.jade-protocol", protocol.jadeProtocol);

        protocol.alternativeBlockPlacement = ProtocolConfig.AlternativePlaceType.valueOf(getString("settings.protocol.alternative-block-placement", protocol.alternativeBlockPlacement.name()));
        if (protocol.alternativeBlockPlacement != ProtocolConfig.AlternativePlaceType.NONE) {
            modify.disableDistanceCheckForUseItem = true;
        }

        protocol.xaeroMapProtocol = getBoolean("settings.protocol.xaero-map-protocol", protocol.xaeroMapProtocol);
        protocol.xaeroMapServerID = getInt("settings.protocol.xaero-map-server-id", protocol.xaeroMapServerID);
        protocol.leavesCarpetSupport = getBoolean("settings.protocol.leaves-carpet-support", protocol.leavesCarpetSupport);

        boolean oldRei = protocol.reiServerProtocol;
        protocol.reiServerProtocol = getBoolean("settings.protocol.rei-server-protocol", protocol.reiServerProtocol);
        if (oldRei != protocol.reiServerProtocol) {
            REIServerProtocol.onConfigModify(protocol.reiServerProtocol);
        }
    }

    private static long getLong(String path, long def) {
        config.addDefault(path, def);
        return config.getLong(path, config.getLong(path));
    }
    // Purpur end - protocol config
}
