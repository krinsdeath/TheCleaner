package net.krinsoft.thecleaner;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

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
        Permission perm = new Permission("thecleaner.world." + event.getWorld().getName());
        perm.setDescription("Allows the attached user to clean entities on the indicated world: " + event.getWorld().getName());
        perm.setDefault(PermissionDefault.OP);
        perm.addParent("thecleaner.world.*", true);
        if (plugin.getServer().getPluginManager().getPermission(perm.getName()) == null) {
            plugin.getServer().getPluginManager().addPermission(perm);
        }
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
            if (flag.equals("--monster")) {
                flags.add(Flag.MONSTER);
            }
            if (flag.equals("--animal")) {
                flags.add(Flag.ANIMAL);
            }
            if (flag.equals("--watermob")) {
                flags.add(Flag.WATERMOB);
            }
            if (flag.equals("--golem")) {
                flags.add(Flag.GOLEM);
            }
            if (flag.equals("--villager")) {
                flags.add(Flag.VILLAGER);
            }
            if (flag.equals("--item")) {
                flags.add(Flag.ITEM);
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
        flags.add(Flag.ITEM);
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

    @EventHandler
    void chunkLoad(ChunkLoadEvent event) {
        // clean up frozen projectiles
        int cleaned = 0;
        for (Entity e : event.getChunk().getEntities()) {
            if (e instanceof Projectile && (((Projectile)e).getShooter() == null || e.getVelocity().length() == 0)) {
                // this projectile's shooter is gone or has no velocity
                e.remove();
                cleaned++;
            }
            if (e instanceof Item && e.getTicksLived() > 1200) {
                // this item has been on the ground for more than 60 seconds
                e.remove();
                cleaned++;
            }
        }
        if (cleaned > 0) {
            plugin.debug("Cleaned " + cleaned + " entities from '" + event.getChunk().getWorld().getName() + "'");
        }
    }

}
