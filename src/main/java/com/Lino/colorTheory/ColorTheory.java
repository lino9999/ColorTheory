package com.Lino.colorTheory;

import com.Lino.colorTheory.teams.BrownTeamMaterials;
import com.Lino.colorTheory.teams.GrayTeamMaterials;
import com.Lino.colorTheory.teams.YellowTeamMaterials;
import com.Lino.colorTheory.teams.RedTeamMaterials;
import com.Lino.colorTheory.teams.WhiteTeamMaterials;
import com.Lino.colorTheory.teams.PurpleTeamMaterials;
import com.Lino.colorTheory.teams.BlueTeamMaterials;
import com.Lino.colorTheory.teams.GreenTeamMaterials;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;

public class ColorTheory extends JavaPlugin implements Listener {

    private Connection connection;
    private Map<UUID, TeamColor> playerTeams = new HashMap<>();
    private Set<UUID> playersNeedingTeam = new HashSet<>();
    private Map<UUID, Material> blocksBrokenByPlayer = new HashMap<>();

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        setupDatabase();
        loadPlayerTeams();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("team").setExecutor(this);
        getCommand("colortheory").setExecutor(this);
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("team")) {
            if (args.length == 0) {
                openTeamSelectionGUI(player);
                return true;
            }

            if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
                playerTeams.remove(player.getUniqueId());
                removePlayerTeam(player.getUniqueId());
                openTeamSelectionGUI(player);
                player.sendMessage(ChatColor.YELLOW + "Your team has been reset. Please choose a new team!");
                return true;
            }
        }

        if (command.getName().equalsIgnoreCase("colortheory")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
                if (!player.hasPermission("colortheory.admin")) {
                    player.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                loadPlayerTeams();
                player.sendMessage(ChatColor.GREEN + "ColorTheory reloaded!");
                return true;
            }
        }

        return false;
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
        playerTeams.clear();
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

    private void removePlayerTeam(UUID uuid) {
        try {
            PreparedStatement ps = connection.prepareStatement("DELETE FROM player_teams WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!playerTeams.containsKey(uuid)) {
            playersNeedingTeam.add(uuid);

            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline() && playersNeedingTeam.contains(uuid)) {
                    openTeamSelectionGUI(player);
                    player.sendMessage(ChatColor.YELLOW + "Welcome! Please select your team color.");
                    player.sendMessage(ChatColor.YELLOW + "You can also use " + ChatColor.GREEN + "/team" + ChatColor.YELLOW + " to open the team selection menu.");
                }
            }, 40L);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (playersNeedingTeam.contains(uuid) && !playerTeams.containsKey(uuid)) {
            event.setCancelled(true);
            openTeamSelectionGUI(player);
            player.sendMessage(ChatColor.RED + "You must select a team before moving!");
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
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to join the " + team.getChatColor() + team.getDisplayName() + ChatColor.GRAY + " team!");
        lore.add(ChatColor.GRAY + "You will only be able to use " + team.getChatColor() + team.getDisplayName().toLowerCase() + ChatColor.GRAY + " items.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Select Your Team")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || !event.getCurrentItem().hasItemMeta()) return;

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
            playersNeedingTeam.remove(player.getUniqueId());
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
            player.sendMessage(ChatColor.RED + "You must select a team first! Use /team");
            return;
        }

        Material blockType = event.getBlock().getType();
        if (!team.isAllowedMaterial(blockType)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You can only place " + team.getDisplayName().toLowerCase() + " blocks!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        TeamColor team = playerTeams.get(player.getUniqueId());

        if (team == null) {
            event.setCancelled(true);
            openTeamSelectionGUI(player);
            player.sendMessage(ChatColor.RED + "You must select a team first! Use /team");
            return;
        }

        Material blockType = event.getBlock().getType();
        if (!team.isAllowedMaterial(blockType)) {
            blocksBrokenByPlayer.put(player.getUniqueId(), blockType);
            event.setDropItems(false);
            event.setExpToDrop(0);
            player.sendMessage(ChatColor.RED + "You broke a " + blockType.name().toLowerCase().replace("_", " ") + " which is not from your team. No drops!");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDropItem(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        Material brokenBlock = blocksBrokenByPlayer.remove(player.getUniqueId());

        if (brokenBlock != null) {
            event.setCancelled(true);
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
            player.sendMessage(ChatColor.RED + "You must select a team first! Use /team");
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
    BROWN("Brown", ChatColor.GOLD, BrownTeamMaterials.getMaterials()),
    GRAY("Gray", ChatColor.GRAY, GrayTeamMaterials.getMaterials()),
    YELLOW("Yellow", ChatColor.YELLOW, YellowTeamMaterials.getMaterials()),
    RED("Red", ChatColor.RED, RedTeamMaterials.getMaterials()),
    WHITE("White", ChatColor.WHITE, WhiteTeamMaterials.getMaterials()),
    PURPLE("Purple", ChatColor.DARK_PURPLE, PurpleTeamMaterials.getMaterials()),
    BLUE("Blue", ChatColor.BLUE, BlueTeamMaterials.getMaterials()),
    GREEN("Green", ChatColor.GREEN, GreenTeamMaterials.getMaterials());

    private final String displayName;
    private final ChatColor chatColor;
    private final Set<Material> allowedMaterials;

    TeamColor(String displayName, ChatColor chatColor, Set<Material> materials) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.allowedMaterials = materials;
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