package dev.codex.armageddon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.FileVisitResult;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.World.Environment;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.StringUtil;

public final class ArmageddonPlugin extends JavaPlugin implements Listener, TabCompleter {
    private static final String BUILD_USAGE =
            "Usage: /build <house|cage|tower|arena|bunker|skybox|lava_trap> [on <player>]";
    private static final String LOADMAP_USAGE =
            "Usage: /loadmap <clean_slate|fresh_survival|lobby|hogwarts|zombieapocalypse|greenfield>";
    private static final String JOINMAP_USAGE =
            "Usage: /joinmap <clean_slate|fresh_survival|lobby|hogwarts|zombieapocalypse|greenfield>";
    private static final String RESETMAP_USAGE =
            "Usage: /resetmap <map> [confirm] [final]";
    private static final String GAME_EFFECT_USAGE =
            "Usage: /game effect <true|false>";
    private static final String[] MANAGED_MAP_WORLDS = {
        "loadmap_clean_slate",
        "loadmap_fresh_survival",
        "loadmap_lobby",
        "loadmap_hogwarts",
        "loadmap_zombieapocalypse",
        "loadmap_greenfield"
    };
    private static final long FRESH_SURVIVAL_SEED = 733105244248291497L;
    private static final String TEMPLATE_ROOT = "map-templates";
    private static final String TEMPLATE_STAGE_ROOT = "map-staging";
    private static final String BLUEMAP_MAPS_DIR = "plugins/BlueMap/maps";
    private static final long LOADMAP_COOLDOWN_MILLIS = 30_000L;
    private static final String HOGWARTS_TEMPLATE = "hogwarts";
    private static final String ZOMBIE_TEMPLATE = "zombieapocalypse";
    private static final String GREENFIELD_TEMPLATE = "greenfield";
    private static final String PERSISTENT_WORLD_PREFIX = "persistent_";
    private static final List<String> GAME_EFFECT_IDS = List.of(
            "speed",
            "slowness",
            "haste",
            "mining_fatigue",
            "strength",
            "jump_boost",
            "regeneration",
            "resistance",
            "fire_resistance",
            "water_breathing",
            "invisibility",
            "blindness",
            "nausea",
            "night_vision",
            "health_boost",
            "absorption",
            "saturation",
            "slow_falling",
            "conduit_power",
            "dolphins_grace",
            "glowing",
            "darkness",
            "weakness",
            "luck",
            "unluck",
            "hero_of_the_village",
            "infested",
            "oozing",
            "weaving",
            "wind_charged"
    );
    private static final int GAME_EFFECT_HISTORY_SIZE = 30;
    private static final double MINECART_BASELINE_SPEED = 0.4D;
    private static final double MINECART_TARGET_MIN_SPEED = 1.25D;
    private static final double MINECART_MAX_HORIZONTAL_SPEED = 24.0D;
    private static final double MINECART_MIN_BOOST_MULTIPLIER = 1.08D;
    private static final double MINECART_MAX_BOOST_MULTIPLIER = 1.20D;
    private static final double MINECART_RAIL_LOOKAHEAD_DISTANCE = 0.75D;
    private static final double MINECART_CURVE_DAMPING_MULTIPLIER = 0.92D;
    private static final List<String> PERSISTENT_MAP_IDS = List.of(
            "greenfield",
            "hogwarts",
            "zombieapocalypse",
            "fresh_survival",
            "clean_slate",
            "lobby"
    );
    private static final Set<String> NUKE_BOOK_LOOT_TABLES = Set.of(
            "chests/abandoned_mineshaft",
            "chests/ancient_city",
            "chests/bastion_bridge",
            "chests/bastion_hoglin_stable",
            "chests/bastion_other",
            "chests/bastion_treasure",
            "chests/buried_treasure",
            "chests/desert_pyramid",
            "chests/end_city_treasure",
            "chests/igloo_chest",
            "chests/jungle_temple",
            "chests/nether_bridge",
            "chests/pillager_outpost",
            "chests/ruined_portal",
            "chests/shipwreck_treasure",
            "chests/simple_dungeon",
            "chests/stronghold_corridor",
            "chests/stronghold_crossing",
            "chests/stronghold_library",
            "chests/trial_chambers/reward_unique",
            "chests/underwater_ruin_big",
            "chests/underwater_ruin_small",
            "chests/woodland_mansion"
    );
    private static final double NUKE_BOOK_CHANCE = 0.0175D;
    private static final String NUKE_BOOK_NAME = "Nuke";

    private NamespacedKey nukeKey;
    private NamespacedKey fallingCreeperKey;
    private NamespacedKey nukeBookKey;
    private NamespacedKey nukeRodKey;
    private NamespacedKey cipherCompassKey;
    private final Set<UUID> activeFallingCreepers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> protectedRespawns = ConcurrentHashMap.newKeySet();
    private final Set<UUID> fallingNukes = ConcurrentHashMap.newKeySet();
    private final Random random = new Random();
    private volatile boolean renewScheduled = false;
    private volatile boolean loadMapBusy = false;
    private volatile long lastLoadMapStartedAtMs = 0L;
    private int currentNukeCode;
    private Location currentNukeCodeSign;
    private Location currentCipherCompassTarget;

    private final Map<Entity, Player> followTrackMap = new ConcurrentHashMap<>();
    private final Map<UUID, ResetMapRequest> pendingResetRequests = new HashMap<>();
    private final Map<UUID, String> playerGameEffects = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<String>> recentGameEffects = new ConcurrentHashMap<>();
    private final Set<UUID> pendingRespawnGameEffects = ConcurrentHashMap.newKeySet();
    private final Set<String> templateStageInProgress = ConcurrentHashMap.newKeySet();
    private boolean followTaskRunning = false;
    private boolean gameEffectModeEnabled = false;

    @Override
    public void onEnable() {
        this.nukeKey = new NamespacedKey(this, "nuke");
        this.fallingCreeperKey = new NamespacedKey(this, "falling_creeper");
        this.nukeBookKey = new NamespacedKey(this, "nuke_book");
        this.nukeRodKey = new NamespacedKey(this, "nuke_rod");
        this.cipherCompassKey = new NamespacedKey(this, "cipher_compass");
        this.currentNukeCode = startupNukeCode();
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("joinmap") != null) {
            getCommand("joinmap").setTabCompleter(this);
        }
        if (getCommand("resetmap") != null) {
            getCommand("resetmap").setTabCompleter(this);
        }
        if (getCommand("followme") != null) {
            getCommand("followme").setTabCompleter(this);
        }
        if (getCommand("game") != null) {
            getCommand("game").setTabCompleter(this);
        }
        startFallingCreeperWatcher();
        startNukeWatcher();
        scheduleTemplatePrestage(HOGWARTS_TEMPLATE);
        scheduleTemplatePrestage(ZOMBIE_TEMPLATE);
        scheduleTemplatePrestage(GREENFIELD_TEMPLATE);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("joinmap") || name.equals("resetmap")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[0], PERSISTENT_MAP_IDS, completions);
                return completions;
            }
            if (name.equals("resetmap")) {
                if (args.length == 2) {
                    List<String> completions = new ArrayList<>();
                    StringUtil.copyPartialMatches(args[1], List.of("confirm"), completions);
                    return completions;
                }
                if (args.length == 3 && args[1].equalsIgnoreCase("confirm")) {
                    List<String> completions = new ArrayList<>();
                    StringUtil.copyPartialMatches(args[2], List.of("final"), completions);
                    return completions;
                }
            }
            return List.of();
        } else if (name.equals("followme")) {
            if (args.length == 1) {
                List<String> options = List.of("horse", "pig", "chicken");
                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[0], options, completions);
                return completions;
            } else if (args.length == 2) {
                List<String> options = List.of("on");
                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[1], options, completions);
                return completions;
            } else if (args.length == 3 && args[1].equalsIgnoreCase("on")) {
                List<String> completions = new ArrayList<>();
                for (Player p : getServer().getOnlinePlayers()) {
                    String pname = p.getName();
                    if (StringUtil.startsWithIgnoreCase(pname, args[2])) {
                        completions.add(pname);
                    }
                }
                return completions;
            } else {
                return List.of();
            }
        } else if (name.equals("game")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[0], List.of("effect"), completions);
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("effect")) {
                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[1], List.of("true", "false"), completions);
                return completions;
            }
            return List.of();
        }
        return List.of();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);

        if (name.equals("build")) {
            return handleBuildCommand(sender, args);
        }

        if (name.equals("maps")) {
            return handleMapsCommand(sender, args);
        }

        if (name.equals("joinmap")) {
            return handleJoinMapCommand(sender, args);
        }

        if (name.equals("resetmap")) {
            return handleResetMapCommand(sender, args);
        }

        if (name.equals("renew")) {
            return handleRenewCommand(sender, args);
        }

        if (name.equals("followme")) {
            return handleFollowMeCommand(sender, args);
        }

        if (name.equals("game")) {
            return handleGameCommand(sender, args);
        }

        if (name.equals("nuke")) {
            return handleNukeCommand(sender, args);
        }

        if (name.equals("spawnnukecode")) {
            return handleSpawnNukeCodeCommand(sender, args);
        }

        Player target = resolveTarget(sender, args, name);
        if (target == null) {
            return true;
        }

        protectedRespawns.add(target.getUniqueId());

        broadcastArmageddonAlert(sender.getName(), target.getName());
        unleashArmageddon(target);
        sender.sendMessage("Cataclysm unleashed on " + target.getName() + ".");
        if (sender != target) {
            target.sendMessage("Cataclysm has been unleashed on you.");
        }
        return true;
    }

    private boolean handleNukeCommand(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage("Usage: /nuke <code> [on <player>]");
            return true;
        }

        if (!args[0].matches("\\d{3}")) {
            sender.sendMessage("Nuke launch code must be exactly 3 digits.");
            return true;
        }

        int enteredCode = Integer.parseInt(args[0]);
        if (enteredCode != currentNukeCode) {
            sender.sendMessage("Invalid nuke launch code.");
            return true;
        }

        Player target = resolveTarget(sender, args, "nuke", 1);
        if (target == null) {
            return true;
        }

        final UUID targetId = target.getUniqueId();
        final String targetName = target.getName();
        final String callerName = sender.getName();
        currentNukeCode = nextNukeCode();

        sender.sendMessage("Launch code accepted.");
        if (sender != target) {
            target.sendMessage("A nuke has been authorized for your position.");
        }

        broadcastNukeAlert(callerName, targetName);
        runNukeCountdown(targetId, targetName, callerName, sender);
        return true;
    }

    private boolean handleSpawnNukeCodeCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage("Usage: /spawnnukecode");
            return true;
        }

        World world;
        if (sender instanceof Player player) {
            world = player.getWorld();
        } else {
            world = getServer().getWorlds().getFirst();
        }

        spawnCurrentNukeCodeSite(world);
        sender.sendMessage("A new nuclear code relic has been hidden somewhere in " + world.getName() + ".");
        return true;
    }

    private void broadcastNukeAlert(String callerName, String targetName) {
        getServer().broadcastMessage("§8[§4Oblivion§8] §4" + callerName
                + " has split the heavens and called down a nuke upon " + targetName
                + "! §8Have mercy on your soul!!");
        for (Player online : getServer().getOnlinePlayers()) {
            online.sendTitle("§4NUCLEAR OBLIVION", "§8" + callerName + " has doomed " + targetName, 10, 70, 20);
            online.playSound(online.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 0.6f);
            online.playSound(online.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.7f);
        }
    }

    private void runNukeCountdown(UUID targetId, String targetName, String callerName, CommandSender sender) {
        new BukkitRunnable() {
            int seconds = 5;

            @Override
            public void run() {
                if (seconds > 0) {
                    getServer().broadcastMessage("§8[§4Oblivion§8] §cImpact on " + targetName + " in " + seconds + "...");
                    for (Player online : getServer().getOnlinePlayers()) {
                        online.sendTitle("§4NUKE INBOUND", "§8" + targetName + " in " + seconds, 0, 20, 5);
                        online.playSound(online.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 0.8f, 0.5f + (seconds * 0.08f));
                    }
                    seconds--;
                    return;
                }

                Player liveTarget = getServer().getPlayer(targetId);
                if (liveTarget == null || !liveTarget.isOnline()) {
                    getServer().broadcastMessage("§8[§4Oblivion§8] §7Nuclear lock on " + targetName + " was lost before impact.");
                    sender.sendMessage("Nuke aborted because " + targetName + " is no longer online.");
                    spawnCurrentNukeCodeSite(getServer().getWorlds().getFirst());
                    cancel();
                    return;
                }

                protectedRespawns.add(liveTarget.getUniqueId());
                launchNuke(liveTarget);
                sender.sendMessage("Nuke launched toward " + targetName + ".");
                if (!callerName.equals(targetName)) {
                    liveTarget.sendMessage("A nuke is falling toward you.");
                }
                cancel();
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private int startupNukeCode() {
        return 100 + (LocalTime.now().toSecondOfDay() % 900);
    }

    private int nextNukeCode() {
        return 100 + random.nextInt(900);
    }

    private String formatNukeCode(int code) {
        return String.format(Locale.ROOT, "%03d", code);
    }

    private Location spawnCurrentNukeCodeSite(World world) {
        expirePreviousNukeCodeSign();
        Location center = findNukeCodeLocation(world);
        int x = center.getBlockX();
        int y = center.getBlockY();
        int z = center.getBlockZ();

        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                setBlock(world, x + dx, y, z + dz, Material.CRYING_OBSIDIAN);
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                    setBlock(world, x + dx, y + 1, z + dz, Material.NETHER_BRICK_WALL);
                }
            }
        }

        setBlock(world, x, y + 1, z, Material.CHISELED_POLISHED_BLACKSTONE);
        setBlock(world, x, y + 2, z, Material.DARK_OAK_SIGN);
        decorateNukeCodeSite(world, x, y, z);
        Block signBlock = world.getBlockAt(x, y + 2, z);
        if (signBlock.getState() instanceof Sign sign) {
            sign.setLine(0, "NUKE");
            sign.setLine(1, "CODE");
            sign.setLine(2, formatNukeCode(currentNukeCode));
            sign.setLine(3, "DOOM");
            sign.update();
        }
        currentNukeCodeSign = signBlock.getLocation();
        currentCipherCompassTarget = createCipherCompassTarget(center);

        spawnNukeCodeGuardians(center.clone().add(0.5, 1.0, 0.5));
        getLogger().info("Nuke code " + formatNukeCode(currentNukeCode) + " hidden at "
                + world.getName() + " "
                + signBlock.getX() + ", " + signBlock.getY() + ", " + signBlock.getZ());
        refreshCipherCompasses();
        getServer().broadcastMessage("§8[§2Cipher§8] §aA new nuclear launch code has been hidden somewhere in "
                + world.getName() + ". §7Your Cipher Compass has been retuned.");
        for (Player online : getServer().getOnlinePlayers()) {
            online.sendTitle("§2CIPHER SHIFT", "§8New code hidden. Compass retuned.", 10, 60, 20);
            online.playSound(online.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 0.8f);
            online.playSound(online.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 0.5f);
        }
        return signBlock.getLocation();
    }

    private void expirePreviousNukeCodeSign() {
        if (currentNukeCodeSign == null) {
            return;
        }

        Block block = currentNukeCodeSign.getBlock();
        if (block.getState() instanceof Sign sign) {
            sign.setLine(0, "EXPIRED");
            sign.setLine(1, "CODE");
            sign.setLine(2, "---");
            sign.setLine(3, "VOID");
            sign.update();
        }
    }

    private Location findNukeCodeLocation(World world) {
        Location spawn = world.getSpawnLocation();
        int radius = 1800;
        for (int attempt = 0; attempt < 40; attempt++) {
            int x = spawn.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = spawn.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            int y = world.getHighestBlockYAt(x, z);
            Material surface = world.getBlockAt(x, y - 1, z).getType();
            if (surface.isAir() || surface == Material.WATER || surface == Material.LAVA) {
                continue;
            }
            return new Location(world, x, y, z);
        }
        int y = world.getHighestBlockYAt(spawn);
        return new Location(world, spawn.getBlockX(), y, spawn.getBlockZ());
    }

    private Location createCipherCompassTarget(Location actualSite) {
        World world = actualSite.getWorld();
        if (world == null) {
            return actualSite;
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            int distance = 80 + random.nextInt(81);
            int x = actualSite.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
            int z = actualSite.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = world.getHighestBlockYAt(x, z);
            Material surface = world.getBlockAt(x, y - 1, z).getType();
            if (surface.isAir() || surface == Material.WATER || surface == Material.LAVA) {
                continue;
            }
            return new Location(world, x + 0.5, y, z + 0.5);
        }

        return actualSite.clone().add(0.5, 0, 0.5);
    }

    private void refreshCipherCompasses() {
        for (Player player : getServer().getOnlinePlayers()) {
            giveCipherCompass(player);
        }
    }

    private void giveCipherCompass(Player player) {
        removeCipherCompasses(player);
        ItemStack compass = createCipherCompass();
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(compass);
        for (ItemStack leftover : leftovers.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private void removeCipherCompasses(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isCipherCompass(contents[i])) {
                contents[i] = null;
            }
        }
        player.getInventory().setContents(contents);

        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (isCipherCompass(offhand)) {
            player.getInventory().setItemInOffHand(null);
        }
    }

    private boolean isCipherCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(this.cipherCompassKey, PersistentDataType.BYTE);
    }

    private ItemStack createCipherCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        CompassMeta meta = (CompassMeta) item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§2Cipher Compass");
        List<String> lore = new ArrayList<>();
        lore.add("§8An unstable relic tuned to");
        lore.add("§8the approximate pulse of");
        lore.add("§8the current nukecode.");

        if (currentCipherCompassTarget != null) {
            meta.setLodestone(currentCipherCompassTarget);
            meta.setLodestoneTracked(false);
            lore.add("§7Search the marked region.");
            lore.add("§7Expect error: about 80-160 blocks.");
        } else {
            lore.add("§7No active signal yet.");
        }

        meta.setLore(lore);
        meta.getPersistentDataContainer().set(this.cipherCompassKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        getServer().getScheduler().runTaskLater(this, () -> {
            Player player = event.getPlayer();
            giveCipherCompass(player);
            ensurePlayerOnPersistentMap(player, getLastMap(player.getUniqueId()));
            if (gameEffectModeEnabled && playerGameEffects.containsKey(player.getUniqueId())) {
                reapplyTrackedGameEffect(player, true);
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        if (worldName.startsWith(PERSISTENT_WORLD_PREFIX)) {
            setLastMap(player.getUniqueId(), worldName.substring(PERSISTENT_WORLD_PREFIX.length()));
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            if (gameEffectModeEnabled && getServer().getOnlinePlayers().isEmpty()) {
                disableGameEffectMode(false);
                getLogger().info("Game effect mode disabled automatically because all players left the server.");
            }
        }, 1L);
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        World world = player.getWorld();
        String worldName = world.getName();
        if (!(worldName.startsWith(PERSISTENT_WORLD_PREFIX) || worldName.startsWith("loadmap_"))) {
            return;
        }

        String message = event.getMessage();
        if (!message.regionMatches(true, 0, "/time ", 0, 6)) {
            return;
        }

        String remainder = message.substring(6).trim();
        if (remainder.isEmpty()) {
            return;
        }

        if (handlePersistentWorldTimeCommand(player, world, remainder)) {
            event.setCancelled(true);
            return;
        }

        String worldKey = world.getKey().toString();
        boolean ok = getServer().dispatchCommand(
                getServer().getConsoleSender(),
                "execute in " + worldKey + " run time " + remainder
        );
        if (ok) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameEffectModeEnabled) {
            return;
        }

        rerollGameEffects();
    }

    private void decorateNukeCodeSite(World world, int x, int y, int z) {
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }

                double distance = Math.sqrt((dx * dx) + (dz * dz));
                if (distance > 4.5) {
                    continue;
                }

                Block ground = world.getBlockAt(x + dx, y + 1, z + dz);
                if (!ground.getType().isAir()) {
                    continue;
                }

                if (distance <= 1.5) {
                    if (random.nextBoolean()) {
                        ground.setType(Material.SHORT_GRASS, false);
                    }
                    continue;
                }

                double roll = random.nextDouble();
                if (roll < 0.35) {
                    ground.setType(random.nextBoolean() ? Material.OAK_LEAVES : Material.SPRUCE_LEAVES, false);
                } else if (roll < 0.55) {
                    ground.setType(Material.TALL_GRASS, false);
                } else if (roll < 0.68) {
                    ground.setType(Material.FERN, false);
                }
            }
        }

        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    continue;
                }
                if (random.nextDouble() > 0.18) {
                    continue;
                }

                int height = 1 + random.nextInt(3);
                for (int dy = 2; dy < 2 + height; dy++) {
                    Block leafBlock = world.getBlockAt(x + dx, y + dy, z + dz);
                    if (leafBlock.getType().isAir()) {
                        leafBlock.setType(random.nextBoolean() ? Material.OAK_LEAVES : Material.SPRUCE_LEAVES, false);
                    }
                }
            }
        }
    }

    private boolean handleMapsCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage("Usage: /maps");
            return true;
        }

        sender.sendMessage("Available persistent maps:");
        for (String mapId : PERSISTENT_MAP_IDS) {
            sender.sendMessage("- " + mapId + " (" + (isPersistentWorldLoaded(mapId) ? "loaded" : "stored") + ")");
        }
        return true;
    }

    private boolean handleJoinMapCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /joinmap.");
            return true;
        }
        if (args.length != 1) {
            sender.sendMessage(JOINMAP_USAGE);
            return true;
        }

        String mapId = normalizeMapId(args[0]);
        if (!isSupportedMapId(mapId)) {
            sender.sendMessage("Unknown map: " + args[0]);
            return true;
        }

        World world = ensurePersistentWorldLoaded(mapId);
        if (world == null) {
            sender.sendMessage("Failed to load map: " + mapId);
            return true;
        }

        player.teleport(playerSafeLocation(player, world));
        setLastMap(player.getUniqueId(), mapId);
        sender.sendMessage("Joined map: " + mapId);
        return true;
    }

    private boolean handleResetMapCommand(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 3) {
            sender.sendMessage(RESETMAP_USAGE);
            return true;
        }

        String mapId = normalizeMapId(args[0]);
        if (!isSupportedMapId(mapId)) {
            sender.sendMessage("Unknown map: " + args[0]);
            return true;
        }

        UUID requester = sender instanceof Player player
                ? player.getUniqueId()
                : UUID.nameUUIDFromBytes(("console:" + mapId).getBytes());
        ResetMapRequest request = pendingResetRequests.get(requester);

        if (args.length == 1) {
            pendingResetRequests.put(requester, new ResetMapRequest(mapId, 1));
            sender.sendMessage("WARNING: /resetmap " + mapId + " will permanently destroy all saved changes for that map.");
            sender.sendMessage("Run /resetmap " + mapId + " confirm to continue.");
            return true;
        }

        if (!args[1].equalsIgnoreCase("confirm") || request == null || !request.mapId().equals(mapId)) {
            sender.sendMessage("Reset confirmation sequence not started. Run /resetmap " + mapId + " first.");
            return true;
        }

        if (args.length == 2) {
            pendingResetRequests.put(requester, new ResetMapRequest(mapId, 2));
            sender.sendMessage("FINAL WARNING: resetting " + mapId + " restores the pristine version and current changes will be lost forever.");
            sender.sendMessage("Run /resetmap " + mapId + " confirm final to perform the reset.");
            return true;
        }

        if (!args[2].equalsIgnoreCase("final") || request.stage() != 2) {
            sender.sendMessage("Final confirmation missing. Run /resetmap " + mapId + " confirm first.");
            return true;
        }

        pendingResetRequests.remove(requester);
        if (!resetPersistentMap(mapId, sender)) {
            sender.sendMessage("Failed to reset map: " + mapId);
            return true;
        }

        sender.sendMessage("Map reset to pristine state: " + mapId);
        return true;
    }

    private void spawnNukeCodeGuardians(Location center) {
        spawnGuardian(center.clone().add(6, 0, 0), EntityType.VINDICATOR);
        spawnGuardian(center.clone().add(-6, 0, 0), EntityType.VINDICATOR);
        spawnGuardian(center.clone().add(0, 0, 6), EntityType.VINDICATOR);
        spawnGuardian(center.clone().add(0, 0, -6), EntityType.VINDICATOR);
        spawnGuardian(center.clone().add(4, 0, 4), EntityType.WITCH);
        spawnGuardian(center.clone().add(-4, 0, 4), EntityType.WITCH);
        spawnGuardian(center.clone().add(4, 0, -4), EntityType.WITCH);
        spawnGuardian(center.clone().add(-4, 0, -4), EntityType.WITCH);
        spawnGuardian(center.clone().add(8, 0, 2), EntityType.SPIDER);
        spawnGuardian(center.clone().add(-8, 0, -2), EntityType.SPIDER);
        spawnGuardian(center.clone().add(2, 0, 8), EntityType.SPIDER);
        spawnGuardian(center.clone().add(-2, 0, -8), EntityType.SPIDER);
        spawnGuardian(center.clone().add(9, 0, 0), EntityType.ENDERMAN);
        spawnGuardian(center.clone().add(-9, 0, 0), EntityType.ENDERMAN);
        spawnGuardian(center.clone().add(0, 0, 9), EntityType.ENDERMAN);
        spawnGuardian(center.clone().add(0, 0, -9), EntityType.ENDERMAN);
    }

    private void spawnGuardian(Location location, EntityType type) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        int y = world.getHighestBlockYAt(location);
        Location spawn = new Location(world, location.getX(), y + 1.0, location.getZ());
        LivingEntity entity = (LivingEntity) world.spawnEntity(spawn, type);
        entity.setPersistent(true);
        entity.setCustomName("Code Guardian");
        entity.setCustomNameVisible(true);
    }

    private void broadcastArmageddonAlert(String callerName, String targetName) {
        getServer().broadcastMessage("§8[§4Harbinger§8] §4" + callerName
                + " has called down armageddon upon " + targetName
                + "! §8Have mercy on your soul!!");
        for (Player online : getServer().getOnlinePlayers()) {
            online.sendTitle("§4ARMAGEDDON", "§8" + callerName + " has marked " + targetName, 10, 70, 20);
            online.playSound(online.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.9f, 0.7f);
            online.playSound(online.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.6f);
        }
    }

    private boolean handleRenewCommand(CommandSender sender, String[] args) {
        if (args.length != 0) {
            sender.sendMessage("Usage: /renew");
            return true;
        }

        if (renewScheduled) {
            sender.sendMessage("Renew is already scheduled.");
            return true;
        }

        renewScheduled = true;
        sender.sendMessage("Renew countdown started.");
        getServer().broadcastMessage("[Server] World renew has been triggered.");
        getServer().broadcastMessage("[Server] The server is going down to rebuild the world to a fresh state.");

        new BukkitRunnable() {
            int seconds = 10;

            @Override
            public void run() {
                if (seconds > 0) {
                    getServer().broadcastMessage("[Server] Renew in " + seconds + " seconds...");
                    seconds--;
                    return;
                }

                getServer().broadcastMessage("[Server] Rebuilding the world now. See you in a moment.");
                try {
                    writeRenewMarker();
                } catch (IOException e) {
                    renewScheduled = false;
                    sender.sendMessage("Failed to create renew marker: " + e.getMessage());
                    getLogger().severe("Failed to create renew marker");
                    e.printStackTrace();
                    cancel();
                    return;
                }

                cancel();
                getServer().shutdown();
            }
        }.runTaskTimer(this, 0L, 20L);

        return true;
    }

    private boolean handleGameCommand(CommandSender sender, String[] args) {
        if (args.length != 2 || !args[0].equalsIgnoreCase("effect")) {
            sender.sendMessage(GAME_EFFECT_USAGE);
            return true;
        }

        if (args[1].equalsIgnoreCase("true")) {
            gameEffectModeEnabled = true;
            sender.sendMessage("Game effect mode enabled. Effects will reroll for online players whenever any player dies.");
            return true;
        }

        if (args[1].equalsIgnoreCase("false")) {
            disableGameEffectMode(true);
            sender.sendMessage("Game effect mode disabled.");
            return true;
        }

        sender.sendMessage(GAME_EFFECT_USAGE);
        return true;
    }

    private boolean handleFollowMeCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use /followme");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("Usage: /followme <horse|pig|chicken> [on <player>]");
            return true;
        }

        String animal = args[0].toLowerCase(Locale.ROOT);
        if (!animal.equals("horse") && !animal.equals("pig") && !animal.equals("chicken")) {
            sender.sendMessage("Invalid animal: " + animal + ". Choose horse, pig, or chicken.");
            return true;
        }

        Player target = player;
        if (args.length >= 3 && args[1].equalsIgnoreCase("on")) {
            Player t = getServer().getPlayerExact(args[2]);
            if (t == null) {
                sender.sendMessage("Player not found: " + args[2]);
                return true;
            }
            target = t;
        }

        World world = target.getWorld();
        EntityType type;
        switch (animal) {
            case "horse" -> type = EntityType.HORSE;
            case "pig" -> type = EntityType.PIG;
            case "chicken" -> type = EntityType.CHICKEN;
            default -> type = EntityType.PIG;
        }

        Location baseLoc = target.getLocation();
        Location spawnLoc = new Location(world,
                baseLoc.getX() + 5.0,
                baseLoc.getY(),
                baseLoc.getZ());
        Entity entity = world.spawnEntity(spawnLoc, type);
        entity.setPersistent(true);

        followTrackMap.put(entity, target);
        startFollowTaskIfNeeded();

        String msg = "Spawned a " + animal + " to follow " + target.getName() + ".";
        sender.sendMessage(msg);
        if (target != sender) {
            target.sendMessage("A " + animal + " is following you (spawned by " + sender.getName() + ").");
        }

        return true;
    }

    private void startFollowTaskIfNeeded() {
        if (followTaskRunning) return;
        followTaskRunning = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (followTrackMap.isEmpty()) {
                    followTaskRunning = false;
                    cancel();
                    return;
                }

                for (Iterator<Map.Entry<Entity, Player>> it = followTrackMap.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry<Entity, Player> entry = it.next();
                    Entity entity = entry.getKey();
                    Player player = entry.getValue();
                    if (!entity.isValid() || !player.isOnline()) {
                        if (entity.isValid()) {
                            entity.remove();
                        }
                        it.remove();
                        continue;
                    }

                    Location base = player.getLocation();
                    Location followLoc = new Location(
                            player.getWorld(),
                            base.getX() + 5.0,
                            base.getY(),
                            base.getZ());
                    followLoc.setYaw(base.getYaw());
                    followLoc.setPitch(base.getPitch());

                    // Use natural movement when possible, but hard-catch-up if the follower falls behind,
                    // the player is flying, or the target changed worlds.
                    if (entity.getWorld() != player.getWorld()) {
                        entity.teleport(followLoc);
                        continue;
                    }

                    double distance = entity.getLocation().distance(followLoc);
                    boolean playerFlying = player.isFlying() || !player.isOnGround();

                    if (distance > 12.0 || playerFlying) {
                        entity.teleport(followLoc);
                        continue;
                    }

                    if (distance < 1.5) {
                        entity.setVelocity(entity.getVelocity().multiply(0.5));
                        continue;
                    }

                    Vector motion = followLoc.toVector().subtract(entity.getLocation().toVector());
                    double y = motion.getY();
                    motion.setY(0);
                    if (motion.lengthSquared() > 0.0001) {
                        motion.normalize().multiply(0.35);
                    }

                    if (Math.abs(y) > 1.0) {
                        motion.setY(Math.signum(y) * 0.2);
                    }

                    entity.setVelocity(motion);
                }
            }
        }.runTaskTimer(this, 0L, 1L);
    }

    private boolean handleLoadMapCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sendLoadMapList(sender);
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(LOADMAP_USAGE);
            sendLoadMapList(sender);
            return true;
        }

        String mapId = args[0].toLowerCase(Locale.ROOT).replace("-", "_");
        if (!isSupportedMapId(mapId)) {
            sender.sendMessage("Unknown map: " + args[0]);
            sendLoadMapList(sender);
            return true;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastLoadMapStartedAtMs;
        if (elapsed < LOADMAP_COOLDOWN_MILLIS) {
            long remainingMs = LOADMAP_COOLDOWN_MILLIS - elapsed;
            long remainingSeconds = (remainingMs + 999L) / 1000L;
            sender.sendMessage("Please wait " + remainingSeconds + "s before using /loadmap again.");
            return true;
        }

        if (loadMapBusy) {
            sender.sendMessage("A map load is already in progress.");
            return true;
        }

        loadMapBusy = true;
        lastLoadMapStartedAtMs = now;
        try {
            World fallbackWorld = getMainWorld();
            Location fallbackSpawn = fallbackWorld.getSpawnLocation().add(0.5, 1.0, 0.5);
            for (Player player : getServer().getOnlinePlayers()) {
                player.teleport(fallbackSpawn);
            }

            unloadManagedMapWorlds();

            World loaded = createManagedMapWorld(mapId);
            if (loaded == null) {
                if (isTemplateBackedMap(mapId) && !templateExists(mapId)) {
                    sender.sendMessage("Template for " + mapId + " is not installed yet. Expected extracted world at "
                            + getTemplateDirectory(mapId) + ".");
                } else {
                    sender.sendMessage("Failed to load map: " + mapId);
                }
                return true;
            }

            postCreateMapSetup(mapId, loaded);
            syncBlueMapManagedWorld(mapId, loaded);

            Location targetSpawn = loaded.getSpawnLocation().add(0.5, 1.0, 0.5);
            for (Player player : getServer().getOnlinePlayers()) {
                player.teleport(targetSpawn);
                player.sendMessage("Loaded map: " + mapId);
            }

            sender.sendMessage("Map loaded: " + mapId);
            return true;
        } finally {
            loadMapBusy = false;
            if (isTemplateBackedMap(mapId)) {
                scheduleTemplatePrestage(mapId);
            }
        }
    }

    private void sendLoadMapList(CommandSender sender) {
        sender.sendMessage("Available maps:");
        sender.sendMessage("- clean_slate");
        sender.sendMessage("- fresh_survival");
        sender.sendMessage("- lobby");
        sender.sendMessage("- hogwarts");
        sender.sendMessage("- zombieapocalypse");
        sender.sendMessage("- greenfield");
        sender.sendMessage("Use /loadmap <name> to load one.");
    }

    private void writeRenewMarker() throws IOException {
        Path marker = getServer().getWorldContainer().toPath().resolve(".renew_pending");
        Files.writeString(marker, Long.toString(System.currentTimeMillis()));
    }

    private World getMainWorld() {
        World world = getServer().getWorld("world");
        if (world != null) {
            return world;
        }
        return getServer().getWorlds().get(0);
    }

    private String normalizeMapId(String input) {
        return input.toLowerCase(Locale.ROOT).replace("-", "_");
    }

    private boolean isSupportedMapId(String mapId) {
        return PERSISTENT_MAP_IDS.contains(mapId);
    }

    private boolean isTemplateBackedMap(String mapId) {
        return mapId.equals(HOGWARTS_TEMPLATE)
                || mapId.equals(ZOMBIE_TEMPLATE)
                || mapId.equals(GREENFIELD_TEMPLATE);
    }

    private void unloadManagedMapWorlds() {
        removeBlueMapManagedMapConfigs();
        for (String worldName : MANAGED_MAP_WORLDS) {
            World world = getServer().getWorld(worldName);
            if (world != null) {
                getServer().unloadWorld(world, false);
            }
            deleteManagedWorldArtifacts(worldName);
        }
    }

    private World createManagedMapWorld(String mapId) {
        String worldName = "loadmap_" + mapId;

        if (isTemplateBackedMap(mapId)) {
            return createTemplateBackedWorld(mapId, worldName);
        }

        WorldCreator creator = new WorldCreator(worldName);

        switch (mapId) {
            case "clean_slate" -> {
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
            }
            case "fresh_survival" -> {
                creator.environment(Environment.NORMAL);
                creator.seed(FRESH_SURVIVAL_SEED);
                creator.generateStructures(true);
            }
            case "lobby" -> {
                creator.environment(Environment.NORMAL);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
            }
            default -> {
                return null;
            }
        }

        return creator.createWorld();
    }

    private String getPersistentWorldName(String mapId) {
        return PERSISTENT_WORLD_PREFIX + mapId;
    }

    private boolean isPersistentWorldLoaded(String mapId) {
        return getServer().getWorld(getPersistentWorldName(mapId)) != null;
    }

    private World ensurePersistentWorldLoaded(String mapId) {
        String worldName = getPersistentWorldName(mapId);
        World existing = getServer().getWorld(worldName);
        if (existing != null) {
            return existing;
        }

        Path worldPath = getServer().getWorldContainer().toPath().resolve(worldName);
        if (!Files.exists(worldPath)) {
            World created = createPersistentWorld(mapId);
            if (created != null) {
                syncBlueMapPersistentWorld(mapId, created);
            }
            return created;
        }

        World loaded = new WorldCreator(worldName).createWorld();
        if (loaded != null) {
            postCreateMapSetup(mapId, loaded);
            syncBlueMapPersistentWorld(mapId, loaded);
        }
        return loaded;
    }

    private World createPersistentWorld(String mapId) {
        String worldName = getPersistentWorldName(mapId);

        if (isTemplateBackedMap(mapId)) {
            World world = createTemplateBackedWorld(mapId, worldName);
            if (world != null) {
                postCreateMapSetup(mapId, world);
            }
            return world;
        }

        WorldCreator creator = new WorldCreator(worldName);
        switch (mapId) {
            case "clean_slate" -> {
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
            }
            case "fresh_survival" -> {
                creator.environment(Environment.NORMAL);
                creator.seed(FRESH_SURVIVAL_SEED);
                creator.generateStructures(true);
            }
            case "lobby" -> {
                creator.environment(Environment.NORMAL);
                creator.type(WorldType.FLAT);
                creator.generateStructures(false);
            }
            default -> {
                return null;
            }
        }

        World world = creator.createWorld();
        if (world != null) {
            postCreateMapSetup(mapId, world);
        }
        return world;
    }

    private World createTemplateBackedWorld(String mapId, String worldName) {
        Path templateDir = getTemplateDirectory(mapId);
        if (!Files.isDirectory(templateDir)) {
            getLogger().warning("Map template missing for " + mapId + ": " + templateDir);
            return null;
        }

        deleteManagedWorldArtifacts(worldName);

        Path targetDir = getServer().getWorldContainer().toPath().resolve(worldName);
        Path stageDir = getTemplateStageDirectory(mapId);
        try {
            boolean usedStagedCopy = false;
            if (Files.isDirectory(stageDir)) {
                try {
                    Files.move(stageDir, targetDir, StandardCopyOption.ATOMIC_MOVE);
                    usedStagedCopy = true;
                } catch (IOException atomicMoveFailure) {
                    try {
                        Files.move(stageDir, targetDir);
                        usedStagedCopy = true;
                    } catch (IOException moveFailure) {
                        getLogger().warning("Failed to use staged template for " + mapId + ": "
                                + moveFailure.getMessage() + ". Falling back to direct copy.");
                    }
                }
            }

            if (!usedStagedCopy) {
                copyDirectory(templateDir, targetDir);
            }
            Files.deleteIfExists(targetDir.resolve("uid.dat"));
            Files.deleteIfExists(targetDir.resolve("session.lock"));
        } catch (IOException e) {
            getLogger().warning("Failed to copy map template " + templateDir + " -> " + targetDir + ": "
                    + e.getMessage());
            return null;
        }

        scheduleTemplatePrestage(mapId);
        return new WorldCreator(worldName).createWorld();
    }

    private void deleteManagedWorldArtifacts(String worldName) {
        Path worldPath = getServer().getWorldContainer().toPath().resolve(worldName);
        deleteWorldDirectory(worldPath);

        Path migratedDimensionPath = getServer().getWorldContainer().toPath()
                .resolve("world")
                .resolve("dimensions")
                .resolve("minecraft")
                .resolve(worldName);
        deleteWorldDirectory(migratedDimensionPath);
    }

    private boolean resetPersistentMap(String mapId, CommandSender sender) {
        String worldName = getPersistentWorldName(mapId);
        World target = getServer().getWorld(worldName);
        World fallback = getMainWorld();
        Location fallbackSpawn = fallback.getSpawnLocation().add(0.5, 1.0, 0.5);

        for (Player online : getServer().getOnlinePlayers()) {
            if (online.getWorld().getName().equals(worldName)) {
                online.teleport(fallbackSpawn);
                online.sendMessage("Map " + mapId + " is being reset to pristine state.");
                setLastMap(online.getUniqueId(), null);
            }
        }

        if (target != null && !getServer().unloadWorld(target, true)) {
            sender.sendMessage("Could not unload map world: " + mapId);
            return false;
        }

        deleteManagedWorldArtifacts(worldName);
        removeBlueMapConfig(worldName);

        World recreated = createPersistentWorld(mapId);
        if (recreated == null) {
            return false;
        }
        syncBlueMapPersistentWorld(mapId, recreated);
        return true;
    }

    private void syncBlueMapManagedWorld(String mapId, World world) {
        if (world == null) {
            return;
        }

        Path mapsDir = getServer().getWorldContainer().toPath().resolve(BLUEMAP_MAPS_DIR);
        String worldName = world.getName();
        Path mapConfig = mapsDir.resolve(worldName + ".conf");

        try {
            Files.createDirectories(mapsDir);
            Files.writeString(mapConfig, buildBlueMapConfig(worldName, mapId, world));
            getServer().dispatchCommand(getServer().getConsoleSender(), "bluemap reload light");
        } catch (IOException e) {
            getLogger().warning("Failed to write BlueMap config for " + worldName + ": " + e.getMessage());
        }
    }

    private void syncBlueMapPersistentWorld(String mapId, World world) {
        if (world == null) {
            return;
        }

        Path mapsDir = getServer().getWorldContainer().toPath().resolve(BLUEMAP_MAPS_DIR);
        String worldName = world.getName();
        Path mapConfig = mapsDir.resolve(worldName + ".conf");

        try {
            Files.createDirectories(mapsDir);
            Files.writeString(mapConfig, buildBlueMapConfig(worldName, mapId, world));
            getServer().dispatchCommand(getServer().getConsoleSender(), "bluemap reload");
        } catch (IOException e) {
            getLogger().warning("Failed to write BlueMap config for " + worldName + ": " + e.getMessage());
        }
    }

    private String buildBlueMapConfig(String worldName, String mapId, World world) {
        Location spawn = world.getSpawnLocation();
        return """
                world: "world/dimensions/minecraft/%s"
                dimension: "minecraft:overworld"
                name: "%s (managed)"
                sorting: -10
                start-pos: { x: %d, z: %d }
                sky-color: "#7dabff"
                void-color: "#000000"
                sky-light: 1
                ambient-light: 0.1
                remove-caves-below-y: 55
                cave-detection-ocean-floor: -5
                cave-detection-uses-block-light: false
                min-inhabited-time: 0
                render-mask: [ { } ]
                render-edges: true
                edge-light-strength: 8
                enable-perspective-view: true
                enable-flat-view: true
                enable-free-flight-view: true
                enable-hires: true
                storage: "file"
                ignore-missing-light-data: false
                marker-sets: { }
                """.formatted(worldName, mapId, spawn.getBlockX(), spawn.getBlockZ());
    }

    private void removeBlueMapManagedMapConfigs() {
        for (String worldName : MANAGED_MAP_WORLDS) {
            removeBlueMapConfig(worldName);
        }
    }

    private void removeBlueMapConfig(String worldName) {
        Path mapsDir = getServer().getWorldContainer().toPath().resolve(BLUEMAP_MAPS_DIR);
        try {
            Files.deleteIfExists(mapsDir.resolve(worldName + ".conf"));
        } catch (IOException e) {
            getLogger().warning("Failed to remove BlueMap config for " + worldName + ": " + e.getMessage());
        }
    }

    private void postCreateMapSetup(String mapId, World world) {
        if (world == null) {
            return;
        }

        world.setAutoSave(true);
        world.setPVP(true);
        world.setSpawnFlags(true, true);

        switch (mapId) {
            case "clean_slate" -> configureCleanSlate(world);
            case "fresh_survival" -> configureFreshSurvival(world);
            case "lobby" -> configureLobby(world);
            case "hogwarts" -> configureHogwarts(world);
            case "zombieapocalypse" -> configureZombieApocalypse(world);
            case "greenfield" -> configureGreenfield(world);
            default -> {
            }
        }
    }

    private void configureCleanSlate(World world) {
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setSpawnLocation(0, world.getHighestBlockYAt(0, 0) + 1, 0);
    }

    private void configureFreshSurvival(World world) {
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
        world.setSpawnLocation(0, world.getHighestBlockYAt(0, 0) + 1, 0);
    }

    private void configureLobby(World world) {
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);

        int centerX = 0;
        int centerZ = 0;
        int baseY = Math.max(world.getHighestBlockYAt(centerX, centerZ), 4);

        fillBox(world, centerX - 5, baseY, centerZ - 5, centerX + 5, baseY, centerZ + 5, Material.POLISHED_ANDESITE);
        fillBox(world, centerX - 2, baseY + 1, centerZ - 2, centerX + 2, baseY + 1, centerZ + 2, Material.SMOOTH_STONE);

        for (int x = -6; x <= 6; x++) {
            setBlock(world, centerX + x, baseY, centerZ - 6, Material.STONE_BRICK_WALL);
            setBlock(world, centerX + x, baseY, centerZ + 6, Material.STONE_BRICK_WALL);
        }
        for (int z = -6; z <= 6; z++) {
            setBlock(world, centerX - 6, baseY, centerZ + z, Material.STONE_BRICK_WALL);
            setBlock(world, centerX + 6, baseY, centerZ + z, Material.STONE_BRICK_WALL);
        }

        setBlock(world, centerX - 3, baseY + 1, centerZ - 3, Material.LANTERN);
        setBlock(world, centerX + 3, baseY + 1, centerZ - 3, Material.LANTERN);
        setBlock(world, centerX - 3, baseY + 1, centerZ + 3, Material.LANTERN);
        setBlock(world, centerX + 3, baseY + 1, centerZ + 3, Material.LANTERN);

        world.setSpawnLocation(centerX, baseY + 2, centerZ);
    }

    private void configureHogwarts(World world) {
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
    }

    private void configureZombieApocalypse(World world) {
        world.setTime(18000L);
        world.setStorm(true);
        world.setThundering(true);
    }

    private void configureGreenfield(World world) {
        world.setTime(6000L);
        world.setStorm(false);
        world.setThundering(false);
    }

    private void ensurePlayerOnPersistentMap(Player player, String preferredMapId) {
        String currentWorld = player.getWorld().getName();
        if (currentWorld.startsWith(PERSISTENT_WORLD_PREFIX)) {
            setLastMap(player.getUniqueId(), currentWorld.substring(PERSISTENT_WORLD_PREFIX.length()));
            return;
        }

        if (preferredMapId == null || preferredMapId.isBlank()) {
            return;
        }

        if (!isSupportedMapId(preferredMapId)) {
            return;
        }

        String mapId = preferredMapId;
        World world = ensurePersistentWorldLoaded(mapId);
        if (world == null) {
            return;
        }

        player.teleport(playerSafeLocation(player, world));
        setLastMap(player.getUniqueId(), mapId);
        player.sendMessage("Joined map: " + mapId);
    }

    private Location playerSafeLocation(Player player, World world) {
        Location current = player.getLocation();
        if (current.getWorld() != null && current.getWorld().getName().equals(world.getName())) {
            return current;
        }
        return world.getSpawnLocation().add(0.5, 1.0, 0.5);
    }

    private boolean handlePersistentWorldTimeCommand(Player player, World world, String remainder) {
        String[] parts = remainder.split("\\s+");
        if (parts.length < 2 || !parts[0].equalsIgnoreCase("set")) {
            return false;
        }

        Long mappedTime = mapTimeValue(parts[1]);
        if (mappedTime == null) {
            return false;
        }

        world.setTime(mappedTime);
        player.sendMessage("Set " + world.getKey() + " time to " + parts[1] + ".");
        return true;
    }

    private Long mapTimeValue(String token) {
        String normalized = token.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "day", "minecraft:day" -> 1000L;
            case "noon", "minecraft:noon" -> 6000L;
            case "night", "minecraft:night" -> 13000L;
            case "midnight", "minecraft:midnight" -> 18000L;
            default -> {
                try {
                    yield Long.parseLong(normalized);
                } catch (NumberFormatException ex) {
                    yield null;
                }
            }
        };
    }

    private Location worldSafeSpawn(World world) {
        Location base = world.getSpawnLocation();
        int safeY = world.getHighestBlockYAt(base);
        return new Location(world, base.getX() + 0.5, safeY + 1.0, base.getZ() + 0.5, base.getYaw(), base.getPitch());
    }

    private void setLastMap(UUID playerId, String mapId) {
        if (mapId == null || mapId.isBlank()) {
            getConfig().set("player-last-map." + playerId, null);
        } else {
            getConfig().set("player-last-map." + playerId, mapId);
        }
        saveConfig();
    }

    private String getLastMap(UUID playerId) {
        return getConfig().getString("player-last-map." + playerId);
    }

    private record ResetMapRequest(String mapId, int stage) {}

    private Path getTemplateDirectory(String mapId) {
        return getServer().getWorldContainer().toPath().resolve(TEMPLATE_ROOT).resolve(mapId);
    }

    private Path getTemplateStageDirectory(String mapId) {
        return getServer().getWorldContainer().toPath().resolve(TEMPLATE_STAGE_ROOT).resolve(mapId);
    }

    private void scheduleTemplatePrestage(String mapId) {
        if (!isTemplateBackedMap(mapId)) {
            return;
        }
        if (!templateStageInProgress.add(mapId)) {
            return;
        }

        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            Path templateDir = getTemplateDirectory(mapId);
            Path stageRoot = getServer().getWorldContainer().toPath().resolve(TEMPLATE_STAGE_ROOT);
            Path stageDir = getTemplateStageDirectory(mapId);
            Path tempStageDir = stageRoot.resolve(mapId + ".tmp");

            try {
                if (!Files.isDirectory(templateDir)) {
                    return;
                }

                Files.createDirectories(stageRoot);
                deleteWorldDirectory(tempStageDir);
                deleteWorldDirectory(stageDir);

                copyDirectory(templateDir, tempStageDir);
                Files.deleteIfExists(tempStageDir.resolve("uid.dat"));
                Files.deleteIfExists(tempStageDir.resolve("session.lock"));

                try {
                    Files.move(tempStageDir, stageDir, StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException atomicMoveFailure) {
                    Files.move(tempStageDir, stageDir);
                }
            } catch (IOException e) {
                getLogger().warning("Failed to prepare staged template for " + mapId + ": " + e.getMessage());
                deleteWorldDirectory(tempStageDir);
            } finally {
                templateStageInProgress.remove(mapId);
            }
        });
    }

    private boolean templateExists(String mapId) {
        return Files.isDirectory(getTemplateDirectory(mapId));
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                Files.createDirectories(target.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Files.copy(file, target.resolve(relative));
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteWorldDirectory(Path worldPath) {
        if (!Files.exists(worldPath)) {
            return;
        }

        try {
            Files.walkFileTree(worldPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            getLogger().warning("Failed to delete world directory " + worldPath + ": " + e.getMessage());
        }
    }

    private boolean handleBuildCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(BUILD_USAGE);
            return true;
        }

        String structure = normalizeStructureName(args[0]);
        if (structure == null) {
            sender.sendMessage(
                    "Unknown structure: " + args[0] + ". Use house, cage, tower, arena, bunker, skybox, or lava_trap.");
            return true;
        }

        Player target = resolveTarget(sender, args, "build", 1);
        if (target == null) {
            return true;
        }

        switch (structure) {
            case "house" -> buildHouse(target);
            case "cage" -> buildCage(target);
            case "tower" -> buildTower(target);
            case "arena" -> buildArena(target);
            case "bunker" -> buildBunker(target);
            case "skybox" -> buildSkybox(target);
            case "lava_trap" -> buildLavaTrap(target);
            default -> {
                sender.sendMessage("Unknown structure: " + args[0]);
                return true;
            }
        }

        sender.sendMessage("Built a " + structure + " for " + target.getName() + ".");
        if (sender != target) {
            target.sendMessage("A " + structure + " appeared around you.");
        }
        return true;
    }

    private String normalizeStructureName(String raw) {
        String value = raw.toLowerCase(Locale.ROOT).replace("-", "_");
        return switch (value) {
            case "house", "cage", "tower", "arena", "bunker", "skybox", "lava_trap" -> value;
            case "lavatrap", "lava" -> "lava_trap";
            default -> null;
        };
    }

    private Player resolveTarget(CommandSender sender, String[] args, String commandName) {
        return resolveTarget(sender, args, commandName, 0);
    }

    private Player resolveTarget(CommandSender sender, String[] args, String commandName, int offset) {
        if (args.length == offset) {
            if (sender instanceof Player player) {
                return player;
            }
            sender.sendMessage("Console usage: /" + commandName
                    + (commandName.equals("build")
                            ? " <house|cage|tower|arena|bunker|skybox|lava_trap> on <player>"
                            : " on <player>"));
            return null;
        }

        if (args.length == offset + 2 && args[offset].equalsIgnoreCase("on")) {
            Player target = getServer().getPlayerExact(args[offset + 1]);
            if (target == null) {
                sender.sendMessage("Player not found: " + args[offset + 1]);
                return null;
            }
            return target;
        }

        if (commandName.equals("build")) {
            sender.sendMessage(BUILD_USAGE);
        } else if (commandName.equals("nuke")) {
            sender.sendMessage("Usage: /nuke <code> [on <player>]");
        } else {
            sender.sendMessage("Usage: /" + commandName + " [on <player>]");
        }
        return null;
    }

    private void buildHouse(Player target) {
        World world = target.getWorld();
        Location center = getGroundAnchor(target.getLocation());
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                setBlock(world, cx + x, cy, cz + z, Material.SPRUCE_PLANKS);
            }
        }

        for (int y = 1; y <= 4; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    boolean edge = Math.abs(x) == 3 || Math.abs(z) == 3;
                    if (!edge) {
                        continue;
                    }
                    Material material = (Math.abs(x) == 3 && Math.abs(z) == 3) ? Material.OAK_LOG : Material.BIRCH_PLANKS;
                    setBlock(world, cx + x, cy + y, cz + z, material);
                }
            }
        }

        for (int y = 1; y <= 3; y++) {
            carveInterior(world, cx, cy + y, cz, 2, Material.AIR);
        }

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (Math.abs(x) + Math.abs(z) > 6) {
                    continue;
                }
                setBlock(world, cx + x, cy + 5, cz + z, Material.DARK_OAK_PLANKS);
            }
        }

        setBlock(world, cx, cy + 1, cz - 3, Material.AIR);
        setBlock(world, cx, cy + 2, cz - 3, Material.AIR);
        setBlock(world, cx - 2, cy + 2, cz - 3, Material.GLASS_PANE);
        setBlock(world, cx + 2, cy + 2, cz - 3, Material.GLASS_PANE);
        setBlock(world, cx - 3, cy + 2, cz, Material.GLASS_PANE);
        setBlock(world, cx + 3, cy + 2, cz, Material.GLASS_PANE);
        setBlock(world, cx - 2, cy + 1, cz + 2, Material.CHEST);
        setBlock(world, cx + 2, cy + 1, cz + 2, Material.CRAFTING_TABLE);
        setBlock(world, cx, cy + 1, cz + 2, Material.RED_BED);
        setBlock(world, cx, cy + 1, cz + 1, Material.RED_BED);
        teleportTo(world, target, cx + 0.5, cy + 1.0, cz + 0.5);
    }

    private void buildCage(Player target) {
        World world = target.getWorld();
        Location base = target.getLocation().getBlock().getLocation();
        int cx = base.getBlockX();
        int cy = base.getBlockY();
        int cz = base.getBlockZ();

        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = -2; z <= 2; z++) {
                    boolean shell = Math.abs(x) == 2 || Math.abs(z) == 2 || y == 0 || y == 4;
                    if (!shell) {
                        setBlock(world, cx + x, cy + y, cz + z, Material.AIR);
                        continue;
                    }
                    Material material = (y == 0) ? Material.OBSIDIAN : Material.TINTED_GLASS;
                    setBlock(world, cx + x, cy + y, cz + z, material);
                }
            }
        }

        teleportTo(world, target, cx + 0.5, cy + 1.0, cz + 0.5);
    }

    private void buildTower(Player target) {
        World world = target.getWorld();
        Location center = getGroundAnchor(target.getLocation());
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        fillBox(world, cx - 2, cy, cz - 2, cx + 2, cy, cz + 2, Material.STONE_BRICKS);

        for (int y = 1; y <= 12; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2;
                    if (!wall) {
                        setBlock(world, cx + x, cy + y, cz + z, Material.AIR);
                        continue;
                    }
                    Material material = (Math.abs(x) == 2 && Math.abs(z) == 2) ? Material.POLISHED_ANDESITE : Material.STONE_BRICKS;
                    setBlock(world, cx + x, cy + y, cz + z, material);
                }
            }
        }

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    continue;
                }
                setBlock(world, cx + x, cy + 13, cz + z, Material.DEEPSLATE_TILES);
            }
        }

        for (int y = 1; y <= 12; y++) {
            setBlock(world, cx, cy + y, cz + 1, Material.LADDER);
        }
        setBlock(world, cx, cy + 1, cz - 2, Material.AIR);
        setBlock(world, cx, cy + 2, cz - 2, Material.AIR);
        teleportTo(world, target, cx + 0.5, cy + 1.0, cz + 0.5);
    }

    private void buildArena(Player target) {
        World world = target.getWorld();
        Location center = getGroundAnchor(target.getLocation());
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        fillBox(world, cx - 10, cy - 1, cz - 10, cx + 10, cy - 1, cz + 10, Material.SMOOTH_STONE);
        fillBox(world, cx - 8, cy, cz - 8, cx + 8, cy, cz + 8, Material.CUT_SANDSTONE);

        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                boolean outer = Math.abs(x) == 10 || Math.abs(z) == 10;
                boolean inner = Math.abs(x) == 8 || Math.abs(z) == 8;
                if (outer) {
                    setBlock(world, cx + x, cy, cz + z, Material.POLISHED_BLACKSTONE_BRICKS);
                    setBlock(world, cx + x, cy + 1, cz + z, Material.POLISHED_BLACKSTONE_BRICK_WALL);
                } else if (inner && ((Math.abs(x) == 8) ^ (Math.abs(z) == 8))) {
                    setBlock(world, cx + x, cy, cz + z, Material.CHISELED_STONE_BRICKS);
                }
            }
        }

        for (int y = 1; y <= 4; y++) {
            setBlock(world, cx - 10, cy + y, cz - 10, Material.LANTERN);
            setBlock(world, cx + 10, cy + y, cz - 10, Material.LANTERN);
            setBlock(world, cx - 10, cy + y, cz + 10, Material.LANTERN);
            setBlock(world, cx + 10, cy + y, cz + 10, Material.LANTERN);
        }

        teleportTo(world, target, cx + 0.5, cy + 1.0, cz + 0.5);
    }

    private void buildBunker(Player target) {
        World world = target.getWorld();
        Location center = getGroundAnchor(target.getLocation());
        int cx = center.getBlockX();
        int surfaceY = center.getBlockY();
        int baseY = surfaceY - 6;
        int cz = center.getBlockZ();

        fillBox(world, cx - 4, baseY, cz - 4, cx + 4, baseY + 5, cz + 4, Material.OBSIDIAN);
        fillBox(world, cx - 3, baseY + 1, cz - 3, cx + 3, baseY + 4, cz + 3, Material.AIR);
        fillBox(world, cx - 1, baseY + 5, cz - 1, cx + 1, surfaceY, cz + 1, Material.OBSIDIAN);
        fillBox(world, cx, baseY + 1, cz, cx, surfaceY - 1, cz, Material.LADDER);
        setBlock(world, cx, surfaceY, cz, Material.IRON_TRAPDOOR);
        setBlock(world, cx - 2, baseY + 1, cz - 2, Material.CHEST);
        setBlock(world, cx + 2, baseY + 1, cz - 2, Material.CRAFTING_TABLE);
        setBlock(world, cx - 2, baseY + 1, cz + 2, Material.FURNACE);
        setBlock(world, cx + 2, baseY + 1, cz + 2, Material.RED_BED);
        setBlock(world, cx + 2, baseY + 1, cz + 1, Material.RED_BED);
        teleportTo(world, target, cx + 0.5, baseY + 1.0, cz + 0.5);
    }

    private void buildSkybox(Player target) {
        World world = target.getWorld();
        Location base = target.getLocation();
        int cx = base.getBlockX();
        int cy = Math.max(base.getBlockY() + 18, 160);
        int cz = base.getBlockZ();

        fillBox(world, cx - 3, cy, cz - 3, cx + 3, cy, cz + 3, Material.GLASS);
        for (int y = 1; y <= 5; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    boolean shell = Math.abs(x) == 3 || Math.abs(z) == 3 || y == 5;
                    setBlock(world, cx + x, cy + y, cz + z, shell ? Material.TINTED_GLASS : Material.AIR);
                }
            }
        }
        setBlock(world, cx, cy + 1, cz, Material.GLOWSTONE);
        teleportTo(world, target, cx + 0.5, cy + 1.0, cz + 0.5);
    }

    private void buildLavaTrap(Player target) {
        World world = target.getWorld();
        Location base = target.getLocation().getBlock().getLocation();
        int cx = base.getBlockX();
        int cy = base.getBlockY();
        int cz = base.getBlockZ();

        fillBox(world, cx - 3, cy - 1, cz - 3, cx + 3, cy - 1, cz + 3, Material.BLACKSTONE);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean rim = Math.abs(x) == 3 || Math.abs(z) == 3;
                if (rim) {
                    setBlock(world, cx + x, cy, cz + z, Material.OBSIDIAN);
                    setBlock(world, cx + x, cy + 1, cz + z, Material.IRON_BARS);
                } else if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    setBlock(world, cx + x, cy, cz + z, Material.LAVA);
                } else {
                    setBlock(world, cx + x, cy, cz + z, Material.MAGMA_BLOCK);
                }
            }
        }
        teleportTo(world, target, cx + 2.5, cy + 1.0, cz + 2.5);
    }

    private Location getGroundAnchor(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int z = location.getBlockZ();
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y, z);
    }

    private void teleportTo(World world, Player target, double x, double y, double z) {
        target.teleport(new Location(world, x, y, z, target.getLocation().getYaw(), target.getLocation().getPitch()));
    }

    private void carveInterior(World world, int cx, int y, int cz, int radius, Material material) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                setBlock(world, cx + x, y, cz + z, material);
            }
        }
    }

    private void fillBox(World world, int x1, int y1, int z1, int x2, int y2, int z2, Material material) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    setBlock(world, x, y, z, material);
                }
            }
        }
    }

    private void setBlock(World world, int x, int y, int z, Material material) {
        Block block = world.getBlockAt(x, y, z);
        if (material == Material.AIR) {
            block.setType(Material.AIR, false);
            return;
        }
        block.setType(material, false);
    }

    private void unleashArmageddon(Player target) {
        World world = target.getWorld();
        Location base = target.getLocation();

        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 10, 0, true, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 4, 0, true, false, true));

        for (int i = 0; i < 6; i++) {
            world.strikeLightning(base);
        }

        double[][] phantomOffsets = {
            {8, 14, 0}, {0, 16, 8}, {-8, 14, 0}, {0, 16, -8},
            {6, 18, 6}, {-6, 18, 6}, {-6, 18, -6}, {6, 18, -6},
            {12, 20, 0}, {0, 22, 12}, {-12, 20, 0}, {0, 22, -12}
        };
        for (double[] offset : phantomOffsets) {
            world.spawnEntity(base.clone().add(offset[0], offset[1], offset[2]), EntityType.PHANTOM);
        }

        double[][] ghastOffsets = {
            {18, 12, 0}, {-18, 12, 0}, {0, 14, 18}, {0, 14, -18},
            {24, 18, 10}, {-24, 18, -10}, {-24, 18, 10}, {24, 18, -10}
        };
        for (double[] offset : ghastOffsets) {
            world.spawnEntity(base.clone().add(offset[0], offset[1], offset[2]), EntityType.GHAST);
        }

        spawnFallingCreeperWave(world, base, 36, 18.0, 28.0, 44.0);
        spawnFallingCreeperWave(world, base, 54, 22.0, 40.0, 58.0);
        spawnFallingCreeperWave(world, base, 72, 26.0, 54.0, 78.0);

        for (int i = 0; i < 20; i++) {
            double angle = (Math.PI * 2 * i) / 20.0;
            double radius = 5.0 + (i % 3);
            Location spawn = base.clone().add(Math.cos(angle) * radius, 18 + (i % 4), Math.sin(angle) * radius);
            TNTPrimed tnt = world.spawn(spawn, TNTPrimed.class);
            tnt.setFuseTicks(18);
            tnt.setVelocity(new Vector(0, -0.45, 0));
        }
    }

    private void spawnFallingCreeperWave(World world, Location base, int count, double horizontalRadius,
            double minHeight, double maxHeight) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double distance = Math.sqrt(random.nextDouble()) * horizontalRadius;
            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            double y = minHeight + (random.nextDouble() * (maxHeight - minHeight));
            Location spawn = base.clone().add(x, y, z);
            Creeper creeper = (Creeper) world.spawnEntity(spawn, EntityType.CREEPER);
            creeper.setPowered(true);
            creeper.setInvulnerable(true);
            creeper.setSilent(true);
            creeper.getPersistentDataContainer().set(this.fallingCreeperKey, PersistentDataType.BYTE, (byte) 1);
            activeFallingCreepers.add(creeper.getUniqueId());
        }
    }

    private void startFallingCreeperWatcher() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeFallingCreepers.isEmpty()) {
                    return;
                }

                List<UUID> finished = new ArrayList<>();
                for (UUID id : activeFallingCreepers) {
                    Entity entity = findEntity(id);
                    if (!(entity instanceof Creeper creeper) || !creeper.isValid()) {
                        finished.add(id);
                        continue;
                    }

                    if (!creeper.getPersistentDataContainer().has(fallingCreeperKey, PersistentDataType.BYTE)) {
                        finished.add(id);
                        continue;
                    }

                    if (!creeper.isOnGround()) {
                        continue;
                    }

                    creeper.getPersistentDataContainer().remove(fallingCreeperKey);
                    creeper.setInvulnerable(false);
                    creeper.setSilent(false);
                    creeper.setMaxFuseTicks(20);
                    creeper.setFuseTicks(1);
                    finished.add(id);
                }

                activeFallingCreepers.removeAll(finished);
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    private void launchNuke(Player target) {
        launchNuke(target.getLocation());
    }

    private void startNukeWatcher() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fallingNukes.isEmpty()) {
                    return;
                }

                List<UUID> finished = new ArrayList<>();
                for (UUID id : fallingNukes) {
                    Entity entity = findEntity(id);
                    if (!(entity instanceof TNTPrimed nuke) || !nuke.isValid()) {
                        finished.add(id);
                        continue;
                    }

                    if (!nuke.isOnGround()) {
                        continue;
                    }

                    Location center = nuke.getLocation();
                    nuke.remove();
                    triggerNuke(center);
                    finished.add(id);
                }

                fallingNukes.removeAll(finished);
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    private Entity findEntity(UUID id) {
        for (World world : getServer().getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof TNTPrimed tnt)) {
            return;
        }
        Byte marker = tnt.getPersistentDataContainer().get(this.nukeKey, PersistentDataType.BYTE);
        if (marker == null || marker != (byte) 1) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        World currentWorld = player.getWorld();
        String worldName = currentWorld.getName();
        boolean shouldReapplyGameEffect = gameEffectModeEnabled
                && (playerGameEffects.containsKey(playerId) || pendingRespawnGameEffects.contains(playerId));
        Location personalRespawn = player.getRespawnLocation();

        if (personalRespawn != null) {
            event.setRespawnLocation(personalRespawn);
            if (shouldReapplyGameEffect) {
                scheduleGameEffectReapply(player);
            }
            return;
        }

        if (protectedRespawns.remove(playerId)) {
            event.setRespawnLocation(worldSafeSpawn(currentWorld));
            if (shouldReapplyGameEffect) {
                scheduleGameEffectReapply(player);
            }
            return;
        }

        if (worldName.startsWith(PERSISTENT_WORLD_PREFIX) || worldName.startsWith("loadmap_")) {
            event.setRespawnLocation(worldSafeSpawn(currentWorld));
            if (worldName.startsWith(PERSISTENT_WORLD_PREFIX)) {
                setLastMap(playerId, worldName.substring(PERSISTENT_WORLD_PREFIX.length()));
            }
            if (shouldReapplyGameEffect) {
                scheduleGameEffectReapply(player);
            }
            return;
        }

        String lastMap = getLastMap(playerId);
        if (lastMap != null && !lastMap.isBlank()) {
            World persistentWorld = ensurePersistentWorldLoaded(lastMap);
            if (persistentWorld != null) {
                event.setRespawnLocation(worldSafeSpawn(persistentWorld));
            }
        }

        if (shouldReapplyGameEffect) {
            scheduleGameEffectReapply(player);
        }
    }

    private void scheduleGameEffectReapply(Player player) {
        UUID playerId = player.getUniqueId();
        long[] delays = {5L, 20L, 40L};
        for (long delay : delays) {
            getServer().getScheduler().runTaskLater(this, () -> {
                Player online = getServer().getPlayer(playerId);
                if (online == null || !online.isOnline() || online.isDead()) {
                    return;
                }
                reapplyTrackedGameEffect(online, true);
                PotionEffectType appliedType = resolvePotionEffectType(playerGameEffects.get(playerId));
                if (appliedType != null && online.hasPotionEffect(appliedType)) {
                    pendingRespawnGameEffects.remove(playerId);
                }
            }, delay);
        }
    }

    private void rerollGameEffects() {
        Map<UUID, String> previousEffects = new HashMap<>(playerGameEffects);
        for (Player player : getServer().getOnlinePlayers()) {
            clearTrackedGameEffect(player);
            playerGameEffects.remove(player.getUniqueId());
            pendingRespawnGameEffects.remove(player.getUniqueId());
        }

        for (Player player : getServer().getOnlinePlayers()) {
            List<String> candidates = new ArrayList<>();
            for (String effectId : GAME_EFFECT_IDS) {
                PotionEffectType type = resolvePotionEffectType(effectId);
                if (type == null || player.hasPotionEffect(type)) {
                    continue;
                }
                candidates.add(effectId);
            }

            if (candidates.isEmpty()) {
                player.sendMessage("No game mode effect could be assigned because you already have all available effects from other sources.");
                continue;
            }

            Deque<String> recentEffects = recentGameEffects.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
            if (!recentEffects.isEmpty()) {
                HashSet<String> recentSet = new HashSet<>(recentEffects);
                if (candidates.size() > recentSet.size()) {
                    candidates.removeIf(recentSet::contains);
                }
            } else {
                String previousEffect = previousEffects.get(player.getUniqueId());
                if (previousEffect != null && candidates.size() > 1) {
                    candidates.remove(previousEffect);
                }
            }

            String chosenEffect = candidates.get(random.nextInt(candidates.size()));
            playerGameEffects.put(player.getUniqueId(), chosenEffect);
            recentEffects.remove(chosenEffect);
            recentEffects.addLast(chosenEffect);
            while (recentEffects.size() > GAME_EFFECT_HISTORY_SIZE) {
                recentEffects.removeFirst();
            }
            if (!player.isDead()) {
                reapplyTrackedGameEffect(player, false);
            } else {
                pendingRespawnGameEffects.add(player.getUniqueId());
            }
        }
    }

    private void reapplyTrackedGameEffect(Player player) {
        reapplyTrackedGameEffect(player, false);
    }

    private void reapplyTrackedGameEffect(Player player, boolean force) {
        if (!gameEffectModeEnabled) {
            return;
        }

        String effectId = playerGameEffects.get(player.getUniqueId());
        if (effectId == null) {
            return;
        }

        PotionEffectType type = resolvePotionEffectType(effectId);
        if (type == null) {
            return;
        }

        if (force) {
            player.removePotionEffect(type);
        } else if (player.hasPotionEffect(type)) {
            return;
        }

        player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, amplifierForGameEffect(effectId), false, true, true));
        if (!player.isDead()) {
            player.sendMessage("Game effect: " + friendlyEffectName(effectId));
        }
    }

    private void clearTrackedGameEffect(Player player) {
        String effectId = playerGameEffects.get(player.getUniqueId());
        if (effectId == null) {
            return;
        }

        PotionEffectType type = resolvePotionEffectType(effectId);
        if (type != null) {
            player.removePotionEffect(type);
        }
    }

    private void disableGameEffectMode(boolean clearOnlinePlayers) {
        gameEffectModeEnabled = false;
        if (clearOnlinePlayers) {
            for (Player player : getServer().getOnlinePlayers()) {
                clearTrackedGameEffect(player);
            }
        }
        playerGameEffects.clear();
        pendingRespawnGameEffects.clear();
        recentGameEffects.clear();
    }

    private PotionEffectType resolvePotionEffectType(String effectId) {
        return PotionEffectType.getByKey(NamespacedKey.minecraft(effectId));
    }

    private int amplifierForGameEffect(String effectId) {
        return switch (effectId) {
            case "speed", "slowness", "haste", "strength", "jump_boost", "regeneration",
                    "health_boost", "absorption", "saturation", "weakness", "luck", "unluck" -> 254;
            case "mining_fatigue", "resistance", "hero_of_the_village" -> 4;
            case "fire_resistance", "water_breathing", "invisibility", "blindness", "nausea",
                    "night_vision", "slow_falling", "conduit_power", "dolphins_grace", "glowing",
                    "darkness", "infested", "oozing", "weaving", "wind_charged" -> 0;
            default -> 0;
        };
    }

    private String friendlyEffectName(String effectId) {
        String[] parts = effectId.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Entity vehicle = event.getVehicle();
        if (!(vehicle instanceof org.bukkit.entity.Minecart minecart)) {
            return;
        }

        Location loc = minecart.getLocation();
        Block railBlock = loc.getBlock();
        if (!isRail(railBlock.getType())) {
            railBlock = loc.getBlock().getRelative(BlockFace.DOWN);
        }
        if (!isRail(railBlock.getType()) || railBlock.getType() == Material.POWERED_RAIL) {
            return;
        }

        minecart.setMaxSpeed(MINECART_MAX_HORIZONTAL_SPEED);
        minecart.setSlowWhenEmpty(false);

        Vector velocity = minecart.getVelocity();
        double horizontal = Math.hypot(velocity.getX(), velocity.getZ());
        if (horizontal < 0.01D || horizontal >= MINECART_MAX_HORIZONTAL_SPEED) {
            return;
        }

        Vector horizontalDirection = velocity.clone().setY(0);
        if (horizontalDirection.lengthSquared() < 1.0E-6D) {
            return;
        }
        horizontalDirection.normalize();

        if (!hasRailAhead(loc, horizontalDirection)) {
            return;
        }

        double maxAllowedSpeed = hasStraightRailAhead(loc, horizontalDirection)
                ? MINECART_MAX_HORIZONTAL_SPEED
                : Math.max(MINECART_TARGET_MIN_SPEED, MINECART_MAX_HORIZONTAL_SPEED * MINECART_CURVE_DAMPING_MULTIPLIER);
        if (horizontal > maxAllowedSpeed) {
            double dampScale = maxAllowedSpeed / horizontal;
            minecart.setVelocity(new Vector(velocity.getX() * dampScale, velocity.getY(), velocity.getZ() * dampScale));
            return;
        }

        double targetSpeed = Math.max(MINECART_TARGET_MIN_SPEED, horizontal * MINECART_MIN_BOOST_MULTIPLIER);
        targetSpeed = Math.min(targetSpeed, Math.max(horizontal * MINECART_MAX_BOOST_MULTIPLIER, MINECART_TARGET_MIN_SPEED));
        targetSpeed = Math.min(targetSpeed, maxAllowedSpeed);

        if (targetSpeed <= horizontal + 0.01D) {
            return;
        }

        double scale = targetSpeed / Math.max(horizontal, 0.01D);
        Vector boosted = velocity.clone();
        boosted.setX(boosted.getX() * scale);
        boosted.setZ(boosted.getZ() * scale);

        double boostedHorizontal = Math.hypot(boosted.getX(), boosted.getZ());
        if (boostedHorizontal > maxAllowedSpeed) {
            double capScale = maxAllowedSpeed / boostedHorizontal;
            boosted.setX(boosted.getX() * capScale);
            boosted.setZ(boosted.getZ() * capScale);
        }

        minecart.setVelocity(boosted);
    }

    private boolean isRail(Material material) {
        return material == Material.RAIL || material == Material.POWERED_RAIL || material == Material.DETECTOR_RAIL || material == Material.ACTIVATOR_RAIL;
    }

    private boolean hasRailAhead(Location loc, Vector direction) {
        return isRailAt(sampleRailLocation(loc, direction, MINECART_RAIL_LOOKAHEAD_DISTANCE))
                || isRailAt(sampleRailLocation(loc, direction, MINECART_RAIL_LOOKAHEAD_DISTANCE * 1.5D));
    }

    private boolean hasStraightRailAhead(Location loc, Vector direction) {
        Vector sideways = new Vector(-direction.getZ(), 0, direction.getX());
        return isRailAt(sampleRailLocation(loc, direction, MINECART_RAIL_LOOKAHEAD_DISTANCE))
                && !isRailAt(sampleRailLocation(loc, sideways, 0.6D))
                && !isRailAt(sampleRailLocation(loc, sideways.multiply(-1), 0.6D));
    }

    private Location sampleRailLocation(Location origin, Vector direction, double distance) {
        return origin.clone().add(direction.clone().multiply(distance));
    }

    private boolean isRailAt(Location location) {
        Block block = location.getBlock();
        if (isRail(block.getType())) {
            return true;
        }
        return isRail(block.getRelative(BlockFace.DOWN).getType());
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack first = event.getInventory().getFirstItem();
        ItemStack second = event.getInventory().getSecondItem();
        ItemStack rod = null;
        ItemStack book = null;

        if (isFishingRod(first) && isNukeBook(second)) {
            rod = first;
            book = second;
        } else if (isFishingRod(second) && isNukeBook(first)) {
            rod = second;
            book = first;
        }

        if (rod == null || book == null) {
            return;
        }

        event.setResult(createNukeRod(rod));
        event.getInventory().setRepairCost(18);
    }

    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getLootTable() == null || event.getLootTable().getKey() == null) {
            return;
        }

        if (!NUKE_BOOK_LOOT_TABLES.contains(event.getLootTable().getKey().getKey())) {
            return;
        }

        if (random.nextDouble() >= NUKE_BOOK_CHANCE) {
            return;
        }

        event.getLoot().add(createNukeBook());
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (!isNukeRod(event.getPlayer().getInventory().getItemInMainHand())) {
            return;
        }

        PlayerFishEvent.State state = event.getState();
        if (state != PlayerFishEvent.State.REEL_IN
                && state != PlayerFishEvent.State.CAUGHT_FISH
                && state != PlayerFishEvent.State.CAUGHT_ENTITY
                && state != PlayerFishEvent.State.FAILED_ATTEMPT
                && state != PlayerFishEvent.State.IN_GROUND) {
            return;
        }

        FishHook hook = event.getHook();
        if (hook == null || !hook.isValid()) {
            return;
        }

        protectedRespawns.add(event.getPlayer().getUniqueId());
        launchNuke(hook.getLocation());
        event.getPlayer().sendMessage("Nuclear line engaged.");
    }

    private boolean isFishingRod(ItemStack item) {
        return item != null && item.getType() == Material.FISHING_ROD;
    }

    private boolean isNukeBook(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTED_BOOK || !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta().getPersistentDataContainer().has(this.nukeBookKey, PersistentDataType.BYTE);
    }

    private boolean isNukeRod(ItemStack item) {
        if (!isFishingRod(item) || !item.hasItemMeta()) {
            return false;
        }

        return item.getItemMeta().getPersistentDataContainer().has(this.nukeRodKey, PersistentDataType.BYTE);
    }

    private ItemStack createNukeBook() {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
        meta.setDisplayName(NUKE_BOOK_NAME);
        meta.setLore(List.of(
                "Apply in an anvil to a fishing rod.",
                "Reel in the bobber to call down a nuke.",
                "Extremely rare treasure enchantment."
        ));
        meta.getPersistentDataContainer().set(this.nukeBookKey, PersistentDataType.BYTE, (byte) 1);
        meta.addStoredEnchant(Enchantment.LURE, 1, true);
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack createNukeRod(ItemStack baseRod) {
        ItemStack result = baseRod.clone();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }

        List<String> lore = meta.hasLore() && meta.getLore() != null
                ? new ArrayList<>(meta.getLore())
                : new ArrayList<>();
        if (!lore.contains("Nuke")) {
            lore.add("Nuke");
        }
        if (!lore.contains("Reel in to drop a nuke at the bobber.")) {
            lore.add("Reel in to drop a nuke at the bobber.");
        }
        meta.setLore(lore);
        meta.getPersistentDataContainer().set(this.nukeRodKey, PersistentDataType.BYTE, (byte) 1);
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        result.setItemMeta(meta);
        return result;
    }

    private void triggerNuke(Location center) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        double radius = 220.0;
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living) {
                living.damage(10000.0);
            } else {
                entity.remove();
            }
        }

        world.strikeLightningEffect(center);
        world.strikeLightningEffect(center.clone().add(6, 0, 0));
        world.strikeLightningEffect(center.clone().add(-6, 0, 0));
        world.strikeLightningEffect(center.clone().add(0, 0, 6));
        world.strikeLightningEffect(center.clone().add(0, 0, -6));
        world.createExplosion(center, 46.0f, false, true);
        world.createExplosion(center.clone().add(0, 18, 0), 22.0f, false, true);
        world.createExplosion(center.clone().subtract(0, 14, 0), 18.0f, false, true);
        world.createExplosion(center.clone().add(12, 0, 0), 14.0f, false, true);
        world.createExplosion(center.clone().add(-12, 0, 0), 14.0f, false, true);

        List<Location> blastPoints = new ArrayList<>();
        int step = 28;
        double centerY = center.getY();
        for (int x = -220; x <= 220; x += step) {
            for (int z = -220; z <= 220; z += step) {
                if ((x * x) + (z * z) > (radius * radius)) {
                    continue;
                }
                blastPoints.add(new Location(world, center.getX() + x, centerY, center.getZ() + z));
            }
        }

        new BukkitRunnable() {
            int index = 0;

            @Override
            public void run() {
                int batch = 5;
                for (int i = 0; i < batch && index < blastPoints.size(); i++, index++) {
                    Location point = blastPoints.get(index);
                    world.strikeLightningEffect(point);
                    if ((index % 2) == 0) {
                        world.strikeLightningEffect(point.clone().add(0, 0, 4));
                    }
                    world.createExplosion(point, 17.0f, false, true);
                    world.createExplosion(point.clone().add(0, 12, 0), 8.0f, false, true);
                    world.createExplosion(point.clone().subtract(0, 12, 0), 11.0f, false, true);
                }
                if (index >= blastPoints.size()) {
                    spawnCurrentNukeCodeSite(world);
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 3L);
    }

    private void launchNuke(Location targetLocation) {
        World world = targetLocation.getWorld();
        if (world == null) {
            return;
        }

        Location spawn = targetLocation.clone().add(0, 120, 0);
        TNTPrimed nuke = world.spawn(spawn, TNTPrimed.class);
        nuke.setGlowing(true);
        nuke.setFuseTicks(1200);
        nuke.setVelocity(new Vector(0, -1.6, 0));
        nuke.getPersistentDataContainer().set(this.nukeKey, PersistentDataType.BYTE, (byte) 1);
        fallingNukes.add(nuke.getUniqueId());

        world.strikeLightning(spawn);
    }
}
