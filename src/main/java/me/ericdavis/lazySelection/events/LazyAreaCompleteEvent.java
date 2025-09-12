package me.ericdavis.lazySelection.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class LazyAreaCompleteEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Player player;
    private final Location point1;
    private final Location point2;

    private boolean isCancelled = false;

    public LazyAreaCompleteEvent(Player player, Location point1, Location point2) {
        this.player = player;
        this.point1 = point1.clone();
        this.point2 = point2.clone();
    }

    public Player getPlayer() {
        return player;
    }

    public Location getPoint1() {
        return point1.clone();
    }

    public Location getPoint2() {
        return point2.clone();
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }
}
