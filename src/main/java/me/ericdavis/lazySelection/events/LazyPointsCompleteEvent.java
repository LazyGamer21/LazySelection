package me.ericdavis.lazySelection.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.List;

public class LazyPointsCompleteEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final List<Location> points;
    private boolean cancelled;

    public LazyPointsCompleteEvent(Player player, List<Location> points) {
        this.player = player;
        // Wrap in unmodifiable list so plugins can't mutate directly
        this.points = points;
    }

    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the points the player selected (immutable view).
     */
    public List<Location> getPoints() {
        return points;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
