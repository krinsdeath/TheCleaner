package net.krinsoft.thecleaner;

import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.WorldLoadEvent;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author krinsdeath
 */
@SuppressWarnings("unused")
public class WorldListener implements Listener {
    private Cleaner plugin;

    private boolean overloaded = false;

    public WorldListener(Cleaner instance) {
        plugin = instance;
    }

    @EventHandler(priority = EventPriority.LOW)
    void worldLoad(WorldLoadEvent event) {
        if (!plugin.clean_on_load) { return; }
        Set<Flag> flags = EnumSet.noneOf(Flag.class);
        String flagstring = plugin.clean_on_load_flags;
        for (String flag : flagstring.split(" ")) {
            if (flag.equals("--force")) {
                flags.add(Flag.FORCE);
            }
            if (flag.equals("--vehicle")) {
                flags.add(Flag.VEHICLE);
            }
            if (flag.equals("--painting")) {
                flags.add(Flag.PAINTING);
            }
            if (flag.equals("--villager")) {
                flags.add(Flag.VILLAGER);
            }
        }
        Iterator<Entity> iterator = event.getWorld().getEntities().iterator();
        int cleaned = 0;
        while (iterator.hasNext()) {
            Entity e = iterator.next();
            if (plugin.cleanerCheck(e, flags)) {
                iterator.remove();
                e.remove();
                cleaned++;
            }
        }
        plugin.log("Cleaned " + cleaned + " entities from " + event.getWorld().getName());
    }

    @EventHandler
    void worldExplosion(EntityExplodeEvent event) {
        if (overloaded) {
            event.setCancelled(true);
            return;
        }
        Set<Flag> flags = EnumSet.noneOf(Flag.class);
        if (event.getLocation().getWorld().getEntities().size() >= plugin.clean_on_overload_total && plugin.clean_on_overload) {
            event.setCancelled(true);
            overloaded = true;
            plugin.log("Detected massive explosion event. Automatically cleaning entities to protect world integrity!");
            Iterator<Entity> iterator = event.getLocation().getWorld().getEntities().iterator();
            int cleaned = 0;
            while (iterator.hasNext()) {
                Entity e = iterator.next();
                if (plugin.cleanerCheck(e, flags)) {
                    iterator.remove();
                    e.remove();
                    cleaned++;
                }
            }
            plugin.log("Cleaned " + cleaned + " entities from " + event.getLocation().getWorld().getName());
            overloaded = false;
        }
    }

    void creatureSpawn(CreatureSpawnEvent event) {
        if (plugin.limits_enabled) {
            World w = event.getEntity().getWorld();
            Entity e = event.getEntity();
            if (e instanceof Monster && w.getEntitiesByClass(Monster.class).size() >= plugin.limits_monsters) {
                plugin.debug("Monster spawn limit reached.");
                event.setCancelled(true);
            }
            if (e instanceof Animals && w.getEntitiesByClass(Animals.class).size() >= plugin.limits_animals) {
                plugin.debug("Animal spawn limit reached.");
                event.setCancelled(true);
            }
            if (e instanceof WaterMob && w.getEntitiesByClass(WaterMob.class).size() >= plugin.limits_water) {
                plugin.debug("Water animal spawn limit reached.");
                event.setCancelled(true);
            }
        }
    }

}
