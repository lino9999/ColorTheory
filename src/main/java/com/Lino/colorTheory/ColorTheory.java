package com.Lino.colorTheory;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class ColorTheory extends JavaPlugin implements Listener {

    private Connection connection;
    private Map<UUID, TeamColor> playerTeams = new HashMap<>();

    @Override
    public void onEnable() {
        setupDatabase();
        loadPlayerTeams();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + getDataFolder() + "/teams.db");
            Statement statement = connection.createStatement();
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_teams (uuid TEXT PRIMARY KEY, team TEXT NOT NULL)");
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadPlayerTeams() {
        try {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT * FROM player_teams");
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                TeamColor team = TeamColor.valueOf(rs.getString("team"));
                playerTeams.put(uuid, team);
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void savePlayerTeam(UUID uuid, TeamColor team) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT OR REPLACE INTO player_teams (uuid, team) VALUES (?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, team.name());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!playerTeams.containsKey(player.getUniqueId())) {
            openTeamSelectionGUI(player);
        }
    }

    private void openTeamSelectionGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 9, "Select Your Team");

        gui.setItem(0, createTeamItem(Material.BROWN_WOOL, TeamColor.BROWN));
        gui.setItem(1, createTeamItem(Material.GRAY_WOOL, TeamColor.GRAY));
        gui.setItem(2, createTeamItem(Material.YELLOW_WOOL, TeamColor.YELLOW));
        gui.setItem(3, createTeamItem(Material.RED_WOOL, TeamColor.RED));
        gui.setItem(4, createTeamItem(Material.WHITE_WOOL, TeamColor.WHITE));
        gui.setItem(5, createTeamItem(Material.PURPLE_WOOL, TeamColor.PURPLE));
        gui.setItem(6, createTeamItem(Material.BLUE_WOOL, TeamColor.BLUE));
        gui.setItem(7, createTeamItem(Material.GREEN_WOOL, TeamColor.GREEN));

        player.openInventory(gui);
    }

    private ItemStack createTeamItem(Material material, TeamColor team) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(team.getChatColor() + team.getDisplayName() + " Team");
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Select Your Team")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null) return;

        Player player = (Player) event.getWhoClicked();
        Material clickedMaterial = event.getCurrentItem().getType();

        TeamColor selectedTeam = null;

        switch (clickedMaterial) {
            case BROWN_WOOL:
                selectedTeam = TeamColor.BROWN;
                break;
            case GRAY_WOOL:
                selectedTeam = TeamColor.GRAY;
                break;
            case YELLOW_WOOL:
                selectedTeam = TeamColor.YELLOW;
                break;
            case RED_WOOL:
                selectedTeam = TeamColor.RED;
                break;
            case WHITE_WOOL:
                selectedTeam = TeamColor.WHITE;
                break;
            case PURPLE_WOOL:
                selectedTeam = TeamColor.PURPLE;
                break;
            case BLUE_WOOL:
                selectedTeam = TeamColor.BLUE;
                break;
            case GREEN_WOOL:
                selectedTeam = TeamColor.GREEN;
                break;
        }

        if (selectedTeam != null) {
            playerTeams.put(player.getUniqueId(), selectedTeam);
            savePlayerTeam(player.getUniqueId(), selectedTeam);
            player.closeInventory();
            player.sendMessage(ChatColor.GREEN + "You have joined the " + selectedTeam.getChatColor() + selectedTeam.getDisplayName() + ChatColor.GREEN + " team!");
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        TeamColor team = playerTeams.get(player.getUniqueId());

        if (team == null) {
            event.setCancelled(true);
            openTeamSelectionGUI(player);
            return;
        }

        Material blockType = event.getBlock().getType();
        if (!team.isAllowedMaterial(blockType)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only place " + team.getDisplayName().toLowerCase() + " blocks!");
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        TeamColor team = playerTeams.get(player.getUniqueId());

        if (team == null) {
            event.setCancelled(true);
            openTeamSelectionGUI(player);
            return;
        }

        ItemStack result = event.getRecipe().getResult();
        if (!team.isAllowedMaterial(result.getType())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only craft " + team.getDisplayName().toLowerCase() + " items!");
        }
    }
}

enum TeamColor {
    BROWN("Brown", ChatColor.GOLD, new Material[]{
            Material.BROWN_WOOL, Material.BROWN_CONCRETE, Material.BROWN_CONCRETE_POWDER,
            Material.BROWN_TERRACOTTA, Material.BROWN_GLAZED_TERRACOTTA, Material.BROWN_STAINED_GLASS,
            Material.BROWN_STAINED_GLASS_PANE, Material.BROWN_CARPET, Material.BROWN_BED,
            Material.BROWN_BANNER, Material.BROWN_SHULKER_BOX, Material.BROWN_CANDLE,
            Material.BROWN_DYE, Material.DIRT, Material.COARSE_DIRT, Material.PODZOL,
            Material.DARK_OAK_LOG, Material.DARK_OAK_PLANKS, Material.DARK_OAK_WOOD,
            Material.STRIPPED_DARK_OAK_LOG, Material.STRIPPED_DARK_OAK_WOOD,
            Material.DARK_OAK_LEAVES, Material.DARK_OAK_SAPLING, Material.DARK_OAK_SIGN,
            Material.DARK_OAK_DOOR, Material.DARK_OAK_TRAPDOOR, Material.DARK_OAK_FENCE,
            Material.DARK_OAK_FENCE_GATE, Material.DARK_OAK_BUTTON, Material.DARK_OAK_PRESSURE_PLATE,
            Material.DARK_OAK_SLAB, Material.DARK_OAK_STAIRS, Material.COCOA_BEANS,
            Material.SOUL_SAND, Material.SOUL_SOIL
    }),

    GRAY("Gray", ChatColor.GRAY, new Material[]{
            Material.GRAY_WOOL, Material.GRAY_CONCRETE, Material.GRAY_CONCRETE_POWDER,
            Material.GRAY_TERRACOTTA, Material.GRAY_GLAZED_TERRACOTTA, Material.GRAY_STAINED_GLASS,
            Material.GRAY_STAINED_GLASS_PANE, Material.GRAY_CARPET, Material.GRAY_BED,
            Material.GRAY_BANNER, Material.GRAY_SHULKER_BOX, Material.GRAY_CANDLE,
            Material.GRAY_DYE, Material.STONE, Material.COBBLESTONE, Material.STONE_BRICKS,
            Material.CRACKED_STONE_BRICKS, Material.CHISELED_STONE_BRICKS, Material.SMOOTH_STONE,
            Material.STONE_SLAB, Material.STONE_STAIRS, Material.COBBLESTONE_SLAB,
            Material.COBBLESTONE_STAIRS, Material.STONE_BRICK_SLAB, Material.STONE_BRICK_STAIRS,
            Material.ANDESITE, Material.POLISHED_ANDESITE, Material.GRAVEL, Material.FLINT,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, Material.RAW_IRON,
            Material.IRON_INGOT, Material.IRON_BLOCK, Material.IRON_NUGGET,
            Material.LIGHT_GRAY_WOOL, Material.LIGHT_GRAY_CONCRETE, Material.LIGHT_GRAY_CONCRETE_POWDER,
            Material.LIGHT_GRAY_TERRACOTTA, Material.LIGHT_GRAY_GLAZED_TERRACOTTA
    }),

    YELLOW("Yellow", ChatColor.YELLOW, new Material[]{
            Material.YELLOW_WOOL, Material.YELLOW_CONCRETE, Material.YELLOW_CONCRETE_POWDER,
            Material.YELLOW_TERRACOTTA, Material.YELLOW_GLAZED_TERRACOTTA, Material.YELLOW_STAINED_GLASS,
            Material.YELLOW_STAINED_GLASS_PANE, Material.YELLOW_CARPET, Material.YELLOW_BED,
            Material.YELLOW_BANNER, Material.YELLOW_SHULKER_BOX, Material.YELLOW_CANDLE,
            Material.YELLOW_DYE, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.RAW_GOLD, Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.GOLD_NUGGET,
            Material.GOLDEN_APPLE, Material.GOLDEN_CARROT, Material.GOLDEN_HELMET,
            Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE,
            Material.GOLDEN_SHOVEL, Material.GOLDEN_HOE, Material.SAND, Material.SANDSTONE,
            Material.SMOOTH_SANDSTONE, Material.CUT_SANDSTONE, Material.CHISELED_SANDSTONE,
            Material.SANDSTONE_SLAB, Material.SANDSTONE_STAIRS, Material.SANDSTONE_WALL,
            Material.HAY_BLOCK, Material.SPONGE, Material.WET_SPONGE, Material.GLOWSTONE,
            Material.GLOWSTONE_DUST, Material.HONEYCOMB, Material.HONEYCOMB_BLOCK,
            Material.HONEY_BLOCK, Material.BEE_NEST, Material.BEEHIVE
    }),

    RED("Red", ChatColor.RED, new Material[]{
            Material.RED_WOOL, Material.RED_CONCRETE, Material.RED_CONCRETE_POWDER,
            Material.RED_TERRACOTTA, Material.RED_GLAZED_TERRACOTTA, Material.RED_STAINED_GLASS,
            Material.RED_STAINED_GLASS_PANE, Material.RED_CARPET, Material.RED_BED,
            Material.RED_BANNER, Material.RED_SHULKER_BOX, Material.RED_CANDLE,
            Material.RED_DYE, Material.REDSTONE, Material.REDSTONE_BLOCK, Material.REDSTONE_ORE,
            Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE_TORCH, Material.REDSTONE_WIRE,
            Material.REPEATER, Material.COMPARATOR, Material.REDSTONE_LAMP,
            Material.TNT, Material.NETHER_WART, Material.NETHER_WART_BLOCK,
            Material.RED_NETHER_BRICKS, Material.RED_NETHER_BRICK_SLAB,
            Material.RED_NETHER_BRICK_STAIRS, Material.RED_NETHER_BRICK_WALL,
            Material.CRIMSON_STEM, Material.CRIMSON_PLANKS, Material.CRIMSON_SLAB,
            Material.CRIMSON_STAIRS, Material.CRIMSON_FENCE, Material.CRIMSON_FENCE_GATE,
            Material.CRIMSON_DOOR, Material.CRIMSON_TRAPDOOR, Material.CRIMSON_SIGN,
            Material.CRIMSON_BUTTON, Material.CRIMSON_PRESSURE_PLATE,
            Material.RED_MUSHROOM, Material.RED_MUSHROOM_BLOCK, Material.APPLE,
            Material.ENCHANTED_GOLDEN_APPLE
    }),

    WHITE("White", ChatColor.WHITE, new Material[]{
            Material.WHITE_WOOL, Material.WHITE_CONCRETE, Material.WHITE_CONCRETE_POWDER,
            Material.WHITE_TERRACOTTA, Material.WHITE_GLAZED_TERRACOTTA, Material.WHITE_STAINED_GLASS,
            Material.WHITE_STAINED_GLASS_PANE, Material.WHITE_CARPET, Material.WHITE_BED,
            Material.WHITE_BANNER, Material.WHITE_SHULKER_BOX, Material.WHITE_CANDLE,
            Material.WHITE_DYE, Material.BONE, Material.BONE_BLOCK, Material.BONE_MEAL,
            Material.QUARTZ, Material.QUARTZ_BLOCK, Material.QUARTZ_BRICKS,
            Material.QUARTZ_PILLAR, Material.CHISELED_QUARTZ_BLOCK, Material.SMOOTH_QUARTZ,
            Material.QUARTZ_SLAB, Material.QUARTZ_STAIRS, Material.NETHER_QUARTZ_ORE,
            Material.DIORITE, Material.POLISHED_DIORITE, Material.BIRCH_LOG,
            Material.BIRCH_PLANKS, Material.BIRCH_WOOD, Material.STRIPPED_BIRCH_LOG,
            Material.STRIPPED_BIRCH_WOOD, Material.BIRCH_LEAVES, Material.BIRCH_SAPLING,
            Material.BIRCH_SIGN, Material.BIRCH_DOOR, Material.BIRCH_TRAPDOOR,
            Material.BIRCH_FENCE, Material.BIRCH_FENCE_GATE, Material.BIRCH_BUTTON,
            Material.BIRCH_PRESSURE_PLATE, Material.BIRCH_SLAB, Material.BIRCH_STAIRS,
            Material.IRON_BARS, Material.SNOW, Material.SNOW_BLOCK, Material.SNOWBALL,
            Material.POWDER_SNOW_BUCKET, Material.STRING, Material.COBWEB
    }),

    PURPLE("Purple", ChatColor.DARK_PURPLE, new Material[]{
            Material.PURPLE_WOOL, Material.PURPLE_CONCRETE, Material.PURPLE_CONCRETE_POWDER,
            Material.PURPLE_TERRACOTTA, Material.PURPLE_GLAZED_TERRACOTTA, Material.PURPLE_STAINED_GLASS,
            Material.PURPLE_STAINED_GLASS_PANE, Material.PURPLE_CARPET, Material.PURPLE_BED,
            Material.PURPLE_BANNER, Material.PURPLE_SHULKER_BOX, Material.PURPLE_CANDLE,
            Material.PURPLE_DYE, Material.CHORUS_PLANT, Material.CHORUS_FLOWER,
            Material.CHORUS_FRUIT, Material.POPPED_CHORUS_FRUIT, Material.PURPUR_BLOCK,
            Material.PURPUR_PILLAR, Material.PURPUR_SLAB, Material.PURPUR_STAIRS,
            Material.END_STONE, Material.END_STONE_BRICKS, Material.END_STONE_BRICK_SLAB,
            Material.END_STONE_BRICK_STAIRS, Material.END_STONE_BRICK_WALL,
            Material.ENDER_PEARL, Material.ENDER_EYE, Material.END_CRYSTAL,
            Material.SHULKER_SHELL, Material.MAGENTA_WOOL, Material.MAGENTA_CONCRETE,
            Material.MAGENTA_CONCRETE_POWDER, Material.MAGENTA_TERRACOTTA,
            Material.MAGENTA_GLAZED_TERRACOTTA, Material.MAGENTA_STAINED_GLASS,
            Material.MAGENTA_STAINED_GLASS_PANE, Material.MAGENTA_CARPET,
            Material.MAGENTA_BED, Material.MAGENTA_BANNER, Material.MAGENTA_SHULKER_BOX,
            Material.MAGENTA_CANDLE, Material.MAGENTA_DYE, Material.OBSIDIAN,
            Material.CRYING_OBSIDIAN, Material.RESPAWN_ANCHOR, Material.AMETHYST_SHARD,
            Material.AMETHYST_BLOCK, Material.BUDDING_AMETHYST, Material.AMETHYST_CLUSTER
    }),

    BLUE("Blue", ChatColor.BLUE, new Material[]{
            Material.BLUE_WOOL, Material.BLUE_CONCRETE, Material.BLUE_CONCRETE_POWDER,
            Material.BLUE_TERRACOTTA, Material.BLUE_GLAZED_TERRACOTTA, Material.BLUE_STAINED_GLASS,
            Material.BLUE_STAINED_GLASS_PANE, Material.BLUE_CARPET, Material.BLUE_BED,
            Material.BLUE_BANNER, Material.BLUE_SHULKER_BOX, Material.BLUE_CANDLE,
            Material.BLUE_DYE, Material.LAPIS_LAZULI, Material.LAPIS_BLOCK,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.WATER_BUCKET,
            Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.PRISMARINE,
            Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE, Material.PRISMARINE_SLAB,
            Material.PRISMARINE_STAIRS, Material.PRISMARINE_BRICK_SLAB,
            Material.PRISMARINE_BRICK_STAIRS, Material.DARK_PRISMARINE_SLAB,
            Material.DARK_PRISMARINE_STAIRS, Material.PRISMARINE_WALL,
            Material.PRISMARINE_SHARD, Material.PRISMARINE_CRYSTALS,
            Material.WARPED_STEM, Material.WARPED_PLANKS, Material.WARPED_SLAB,
            Material.WARPED_STAIRS, Material.WARPED_FENCE, Material.WARPED_FENCE_GATE,
            Material.WARPED_DOOR, Material.WARPED_TRAPDOOR, Material.WARPED_SIGN,
            Material.WARPED_BUTTON, Material.WARPED_PRESSURE_PLATE,
            Material.LIGHT_BLUE_WOOL, Material.LIGHT_BLUE_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE_POWDER, Material.LIGHT_BLUE_TERRACOTTA,
            Material.LIGHT_BLUE_GLAZED_TERRACOTTA, Material.CYAN_WOOL,
            Material.CYAN_CONCRETE, Material.CYAN_CONCRETE_POWDER,
            Material.CYAN_TERRACOTTA, Material.CYAN_GLAZED_TERRACOTTA
    }),

    GREEN("Green", ChatColor.GREEN, new Material[]{
            Material.GREEN_WOOL, Material.GREEN_CONCRETE, Material.GREEN_CONCRETE_POWDER,
            Material.GREEN_TERRACOTTA, Material.GREEN_GLAZED_TERRACOTTA, Material.GREEN_STAINED_GLASS,
            Material.GREEN_STAINED_GLASS_PANE, Material.GREEN_CARPET, Material.GREEN_BED,
            Material.GREEN_BANNER, Material.GREEN_SHULKER_BOX, Material.GREEN_CANDLE,
            Material.GREEN_DYE, Material.EMERALD, Material.EMERALD_BLOCK,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE, Material.GRASS_BLOCK,
            Material.MOSS_BLOCK, Material.MOSS_CARPET, Material.OAK_LEAVES,
            Material.SPRUCE_LEAVES, Material.JUNGLE_LEAVES, Material.ACACIA_LEAVES,
            Material.MANGROVE_LEAVES, Material.OAK_SAPLING, Material.SPRUCE_SAPLING,
            Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING, Material.MANGROVE_PROPAGULE,
            Material.CACTUS, Material.SUGAR_CANE, Material.BAMBOO, Material.KELP,
            Material.SEA_PICKLE, Material.LILY_PAD, Material.VINE, Material.FERN,
            Material.LARGE_FERN, Material.SEAGRASS, Material.TALL_SEAGRASS,
            Material.LIME_WOOL, Material.LIME_CONCRETE, Material.LIME_CONCRETE_POWDER,
            Material.LIME_TERRACOTTA, Material.LIME_GLAZED_TERRACOTTA,
            Material.LIME_STAINED_GLASS, Material.LIME_STAINED_GLASS_PANE,
            Material.SLIME_BALL, Material.SLIME_BLOCK, Material.EXPERIENCE_BOTTLE,
            Material.TURTLE_EGG, Material.TURTLE_SCUTE, Material.CREEPER_HEAD
    });

    private final String displayName;
    private final ChatColor chatColor;
    private final Set<Material> allowedMaterials;

    TeamColor(String displayName, ChatColor chatColor, Material[] materials) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.allowedMaterials = new HashSet<>(Arrays.asList(materials));
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public boolean isAllowedMaterial(Material material) {
        return allowedMaterials.contains(material);
    }
}