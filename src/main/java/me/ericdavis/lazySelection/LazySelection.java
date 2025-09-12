package me.ericdavis.lazySelection;

import me.ericdavis.lazySelection.events.LazyAreaCompleteEvent;
import me.ericdavis.lazySelection.events.LazyPointsCompleteEvent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public final class LazySelection implements Listener {

    public LazySelection(JavaPlugin plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // stuff for setting points
    private final static HashMap<UUID, List<Location>> tempPoints = new HashMap<>();
    private final static HashMap<UUID, Collection<Location>> listToConfirm = new HashMap<>();

    // stuff for setting areas
    private static final HashMap<UUID, Location> tempFirstClicks = new HashMap<>();
    private static final HashMap<UUID, Location> playerLoc1 = new HashMap<>();
    private static final HashMap<UUID, Location> playerLoc2 = new HashMap<>();

    // track last click times
    private static final Map<UUID, Long> lastClickTimes = new HashMap<>();

    /**
     * The provided list must be mutable (e.g. an ArrayList).
     */
    public static void setPoints(Player player, Collection<Location> points) {
        UUID id = player.getUniqueId();

        removeFromPoints(id);
        removeFromArea(id);

        tempPoints.put(id, new ArrayList<>());
        listToConfirm.put(id, points);
        player.sendMessage(startPointsMessage);
    }

    public static void setArea(Player player, Location loc1, Location loc2) {
        UUID id = player.getUniqueId();

        removeFromPoints(id);
        removeFromArea(id);

        tempFirstClicks.put(id, null);
        playerLoc1.put(id, loc1);
        playerLoc2.put(id, loc2);
        player.sendMessage(startAreaMessage);
    }

    private static void confirmArea(Player player, Location loc2) {
        UUID id = player.getUniqueId();

        if (!tempFirstClicks.containsKey(id) || tempFirstClicks.get(id) == null) {
            Bukkit.getLogger().warning("[LazySelection] Attempted to confirm an area selection that does not exist for player: " + player.getName());
            return;
        }

        LazyAreaCompleteEvent event = new LazyAreaCompleteEvent(player, tempFirstClicks.get(id), loc2);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            removeFromArea(id);
            return;
        }

        if (!setLocationValues(playerLoc1.get(id), tempFirstClicks.get(id))) {
            Bukkit.getLogger().warning("Could not set Area because first location given is null");
            return;
        }
        if (!setLocationValues(playerLoc2.get(id), loc2)) {
            Bukkit.getLogger().warning("Could not set Area because second location given is null");
            return;
        }

        // show particles
        createParticleOutline(tempFirstClicks.get(id), loc2);

        player.sendMessage(areaCompletedMessage);

        removeFromArea(id);
    }

    private static void confirmPoints(Player player) {
        UUID id = player.getUniqueId();

        if (!listToConfirm.containsKey(id) || !tempPoints.containsKey(id)) {
            Bukkit.getLogger().warning("[LazySelection] Attempted to confirm a points selection that does not exist for player: " + player.getName());
            return;
        }

        List<Location> selectedPoints = tempPoints.get(id);

        if (selectedPoints.isEmpty()) {
            Bukkit.getLogger().warning("[LazySelection] Attempted to confirm a points selection that is empty for player: " + player.getName());
            removeFromPoints(id);
            player.sendMessage(stopPointsMessage);
            return;
        }

        LazyPointsCompleteEvent event = new LazyPointsCompleteEvent(player, selectedPoints);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            removeFromPoints(id);
            return;
        }

        // Use the potentially modified list from the event
        listToConfirm.get(id).addAll(event.getPoints());

        player.sendMessage(pointsCompletedMessage.replace("%count%", String.valueOf(tempPoints.get(id).size())));
        removeFromPoints(id);
    }

    private static void removeFromPoints(UUID id) {
        tempPoints.remove(id);
        listToConfirm.remove(id);
    }

    private static boolean isPlayerSettingPoints(UUID id) {
        return tempPoints.containsKey(id) || listToConfirm.containsKey(id);
    }

    private static boolean isPlayerSettingArea(UUID id) {
        return tempFirstClicks.containsKey(id);
    }

    private static void removeFromArea(UUID id) {
        tempFirstClicks.remove(id);
        playerLoc1.remove(id);
        playerLoc2.remove(id);
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        UUID id = player.getUniqueId();

        // Only proceed if this event is from the MAIN hand.
        if (e.getHand() != null && e.getHand() != EquipmentSlot.HAND) return;

        boolean inAreaMode = isPlayerSettingArea(id);
        boolean inPointsMode = isPlayerSettingPoints(id);

        if (!inAreaMode && !inPointsMode) return;

        e.setCancelled(true);

        if (inAreaMode) {
            handleAreaClick(player, id, e);
        } else {
            handlePointsClick(player, id, e);
        }
    }

    private void handlePointsClick(Player player, UUID id, PlayerInteractEvent e) {
        List<Location> playerTempPoints = tempPoints.get(id);
        Collection<Location> pointsToAddTo = listToConfirm.get(id);

        if (player.isSneaking()) {
            if (rightClicked(e)) {
                // Shift-right-click cancels entire point selection
                tempPoints.remove(id);
                player.sendMessage(stopPointsMessage);
            } else {
                // Shift-left-click confirms point selection
                confirmPoints(player);
            }
            return;
        }

        if (e.getClickedBlock() == null) return;
        Location clicked = e.getClickedBlock().getLocation();

        if (isOnCooldown(player)) {
            player.sendMessage(ChatColor.GRAY + "Please wait before clicking again!");
            return;
        }

        if (rightClicked(e)) {
            // Normal right-click undoes last point
            if (playerTempPoints != null && !playerTempPoints.isEmpty()) {
                showDeletionParticles(player, playerTempPoints.getLast());
                playerTempPoints.removeLast();
                player.sendMessage(lastPointRemovedMessage.replace("%count%", String.valueOf(playerTempPoints.size())));
            } else {
                player.sendMessage(noPointsToUndoMessage);
            }
            return;
        }

        if (!leftClickedBlock(e)) return;

        if (playerTempPoints.contains(clicked) || pointsToAddTo.contains(clicked)) {
            player.sendMessage(pointAlreadyExistsMessage);
            return;
        }

        playerTempPoints.add(clicked);
        showSelectionParticles(player, clicked); // show particles
        player.sendMessage(pointAddedMessage.replace("%count%", String.valueOf(playerTempPoints.size())));
    }

    private void handleAreaClick(Player player, UUID id, PlayerInteractEvent e) {
        if (rightClicked(e)) {
            removeFromArea(id);
            player.sendMessage(stopAreaMessage);
            return;
        }

        if (!leftClickedBlock(e)) return;
        if (e.getClickedBlock() == null) return;

        if (isOnCooldown(player)) {
            player.sendMessage(ChatColor.GRAY + "Please wait before clicking again!");
            return;
        }

        Location clicked = e.getClickedBlock().getLocation();
        tempFirstClicks.putIfAbsent(id, null);

        // FIRST CLICK
        if (tempFirstClicks.get(id) == null) {
            tempFirstClicks.put(id, clicked);
            showSelectionParticles(player, clicked); // show particles
            player.sendMessage(firstBlockMessage);
            return;
        }

        confirmArea(player, clicked);
    }

    private boolean rightClicked(PlayerInteractEvent e) {
        return e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR;
    }

    private boolean leftClickedBlock(PlayerInteractEvent e) {
        return e.getAction() == Action.LEFT_CLICK_BLOCK;
    }

    private static boolean setLocationValues(Location receiving, Location giving) {
        if (receiving == null || giving == null) return false;

        receiving.setWorld(giving.getWorld());
        receiving.setX(giving.getX());
        receiving.setY(giving.getY());
        receiving.setZ(giving.getZ());
        receiving.setYaw(giving.getYaw());
        receiving.setPitch(giving.getPitch());

        return true;
    }

    private static void showSelectionParticles(Player player, Location loc) {
        if (loc == null) return;
        for (double y = 1; y >= -1; y -= 0.5) {
            Location particleLoc = loc.clone().add(0.5, y, 0.5); // center on block
            player.spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 5, 0.1, 1.0, 0.1, 0);
        }
    }

    /**
     *
     * @param loc1 Corner 1
     * @param loc2 Corner 2
     * @implNote Creates particles to visualize the volume created by the two locations
     */
    private static void createParticleOutline(Location loc1, Location loc2) {
        World world = loc1.getWorld();
        if (world == null || !world.equals(loc2.getWorld())) {
            Bukkit.getLogger().warning("[MCOHexRoyale -- createParticleOutline] Locations must be in the same world.");
            return;
        }

        int minX = Math.min(loc1.getBlockX(), loc2.getBlockX());
        int minY = Math.min(loc1.getBlockY(), loc2.getBlockY());
        int minZ = Math.min(loc1.getBlockZ(), loc2.getBlockZ());
        int maxX = Math.max(loc1.getBlockX(), loc2.getBlockX());
        int maxY = Math.max(loc1.getBlockY(), loc2.getBlockY());
        int maxZ = Math.max(loc1.getBlockZ(), loc2.getBlockZ());

        // Increased particle count and persistence
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean isEdge = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                    if (isEdge) {
                        Location blockLoc = new Location(world, x + 0.5, y + 0.5, z + 0.5);

                        // Increase the number of particles and density
                        world.spawnParticle(
                                Particle.HAPPY_VILLAGER, // Choose your preferred particle
                                blockLoc,
                                10,    // Particle count
                                0.1,   // X-offset for spread
                                0.1,   // Y-offset for spread
                                0.1,   // Z-offset for spread
                                0.05   // Speed/extra parameter for density
                        );
                    }
                }
            }
        }
    }

    private static void showDeletionParticles(Player player, Location loc) {
        if (loc == null) return;
        for (double y = 1; y >= -1; y -= 0.5) {
            Location particleLoc = loc.clone().add(0.5, y, 0.5); // center on block
            player.spawnParticle(Particle.DUST, particleLoc, 5, 0.1, 1.0, 0.1, new Particle.DustOptions(Color.RED, 2));
        }
    }

    /**
     * Checks if player is on cooldown (1s).
     * Returns true if they must wait longer.
     */
    private boolean isOnCooldown(Player player) {
        long now = System.currentTimeMillis();
        long last = lastClickTimes.getOrDefault(player.getUniqueId(), 0L);

        if (now - last < 500) { // less than 0.5s passed
            return true;
        }

        lastClickTimes.put(player.getUniqueId(), now);
        return false;
    }

    // Area Messages
    public static String startAreaMessage = ChatColor.AQUA + "Started Area Selection\n -- Left-Click to Select\n -- Right-Click to Cancel";
    public static String stopAreaMessage = ChatColor.AQUA + "Cancelled Area Selection";
    public static String firstBlockMessage = ChatColor.AQUA + "First Location Set -- Select Second Location";
    public static String areaCompletedMessage = ChatColor.AQUA + "Second Location Set -- Area Selection Completed";

    // Points Messages
    public static String startPointsMessage = ChatColor.YELLOW + "Started Point Selection\n" + ChatColor.AQUA + " -- Left-Click to Add\n -- Right-Click to Undo\n -- Shift-Right-Click to Cancel\n" + ChatColor.YELLOW + " -- Shift-Left-Click to Complete";
    public static String stopPointsMessage = ChatColor.AQUA + "Cancelled Point Selection";
    public static String pointsCompletedMessage = ChatColor.AQUA + "Point Selection Completed with %count% points";
    public static String pointAddedMessage = ChatColor.AQUA + "Added Point #%count%";
    public static String lastPointRemovedMessage = ChatColor.AQUA + "Removed Last Point -- %count% Remaining";
    public static String noPointsToUndoMessage = ChatColor.RED + "No points to undo!";
    public static String pointAlreadyExistsMessage = ChatColor.RED + "Point already exists!";
}