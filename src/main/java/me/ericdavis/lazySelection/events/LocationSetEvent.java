package me.ericdavis.lazySelection.events;

import me.ericdavis.lazySelection.LocationType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LocationSetEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private Location location;
    private final LocationType locationType;
    /***
     * @implNote pointsCollection does not contain the most recent point (the point to be added in this event)
     */
    private final List<Location> pointsCollection;
    private boolean cancelled;
    private boolean finishSelection;

    public LocationSetEvent(Player player, Location location, LocationType locationType) {
        this.player = player;
        this.location = location;
        this.locationType = locationType;
        pointsCollection = null;
    }

    public LocationSetEvent(Player player, List<Location> pointsCollection, Location location, LocationType locationType) {
        this.player = player;
        this.pointsCollection = pointsCollection;
        this.location = location;
        this.locationType = locationType;
    }

    public Player getPlayer() {
        return player;
    }

    public @Nullable Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public @Nullable List<Location> getCollection() {
        return pointsCollection;
    }

    public LocationType getLocationType() { return locationType; }

    public boolean finishSelection() { return finishSelection; }
    public void setFinishSelection(boolean finishSelection) { this.finishSelection = finishSelection; }

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
