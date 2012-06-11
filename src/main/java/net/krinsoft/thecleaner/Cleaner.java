package net.krinsoft.thecleaner;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/**
 * @author krinsdeath
 */
public class Cleaner extends JavaPlugin {
    private class Clock implements Runnable {
        private int ID = 0;
        private int ticks = 0;
        private long started;
        private String runner;

        public Clock(CommandSender sender) {
            runner = sender.getName();
            started = System.currentTimeMillis();
        }

        @Override
        public void run() {
            ticks++;
            long runtime = System.currentTimeMillis() - started;
            if (runtime >= 1000 || ticks == 20) {
                CommandSender sender = getServer().getPlayer(runner);
                if (sender == null) {
                    sender = getServer().getConsoleSender();
                }
                if (ticks < 15) {
                    sender.sendMessage("Server clock is running slow. Consider removing some plugins.");
                }
                if (runtime <= 1000) {
                    sender.sendMessage("Server performance: " + ChatColor.GREEN + "Excellent");
                } else if (runtime > 1000 && runtime < 1200) {
                    sender.sendMessage("Server performance: " + ChatColor.GOLD + "Average");
                } else if (runtime > 1200 && runtime < 1500) {
                    sender.sendMessage("Server performance: " + ChatColor.RED + "Poor");
                } else {
                    sender.sendMessage("Server performance: " + ChatColor.GRAY + "Terrible");
                }
                sender.sendMessage("Expected 20 ticks, got " + ticks + ". (" + runtime + "ms)");
                getServer().getScheduler().cancelTask(ID);
                if (getServer().getScheduler().isCurrentlyRunning(ID)) {
                    throw new RuntimeException("Clock time exceeded.");
                }
            }
        }
    }

    private boolean debug = false;
    public boolean clean_on_overload = true;
    public int clean_on_overload_total = 5000;
    public boolean clean_on_load = true;
    public String clean_on_load_flags = "";

    public void onEnable() {
        debug = getConfig().getBoolean("debug", false);
        clean_on_overload = getConfig().getBoolean("overload.clean", true);
        clean_on_overload_total = getConfig().getInt("overload.total", 5000);
        clean_on_load = getConfig().getBoolean("startup.clean", true);
        clean_on_load_flags = getConfig().getString("startup.flags", "");
        if (getConfig().get("limits.enabled") != null) {
            getConfig().set("limits", null);
        }
        dumpConfig();
        saveConfig();
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);
        log("Debug mode is: " + (debug ? "enabled" : "disabled"));
        log("Plugin enabled successfully.");
    }

    public void onDisable() {
        log("Plugin disabled.");
    }

    private void dumpConfig() {
        getConfig().set("debug", debug);
        // cleanup on entity overload
        getConfig().set("overload.clean", clean_on_overload);
        getConfig().set("overload.total", clean_on_overload_total);
        // cleanup at world loading
        getConfig().set("startup.clean", clean_on_load);
        getConfig().set("startup.flags", clean_on_load_flags);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("sysstat") && check(sender, "thecleaner.system")) {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory  = runtime.maxMemory();
            long allocated  = runtime.totalMemory();
            long free       = runtime.freeMemory();
            Clock clock = new Clock(sender);
            if (args.length > 0 && (args[0].equalsIgnoreCase("--verbose") || args[0].equalsIgnoreCase("-v"))) {
                String os = System.getProperty("os.name") + " (" + System.getProperty("os.version") + ") - " + System.getProperty("os.arch");
                String version = System.getProperty("java.vendor") + " " + System.getProperty("java.version");
                sender.sendMessage("OS: " + os);
                sender.sendMessage("Java Version: " + version);
            }
            sender.sendMessage("Maximum memory: " + (maxMemory / 1024L / 1024L) + "MB");
            sender.sendMessage("Allocated: " + (allocated / 1024L / 1024L) + "MB");
            sender.sendMessage("Free: " + (free / 1024L / 1024L) + "MB");
            try {
                clock.ID = getServer().getScheduler().scheduleSyncRepeatingTask(this, clock, 0L, 1L);
            } catch (RuntimeException e) {
                debug(e.getLocalizedMessage());
            }
        }
        if (cmd.getName().equals("cleanup")) {
            List<String> arguments = new ArrayList<String>(Arrays.asList(args));
            Iterator<String> iterator = arguments.iterator();
            Set<Flag> flags = EnumSet.noneOf(Flag.class);
            while (iterator.hasNext()) {
                String arg = iterator.next();
                if (arg.equals("--all") && check(sender, "all")) {
                    iterator.remove();
                    flags.add(Flag.ALL); continue;
                }
                if (arg.equals("--debug") && check(sender, "debug")) {
                    iterator.remove();
                    flags.clear();
                    flags.add(Flag.DEBUG); continue;
                }
                if (arg.equals("--broadcast") && check(sender, "broadcast")) {
                    iterator.remove();
                    flags.add(Flag.BROADCAST); continue;
                }
                if (arg.equals("--info") && check(sender, "info")) {
                    iterator.remove();
                    flags.clear();
                    flags.add(Flag.INFO); break;
                }
                if (arg.equals("--verbose") && check(sender, "verbose")) {
                    iterator.remove();
                    flags.add(Flag.VERBOSE); continue;
                }
                if (arg.equals("--force") && check(sender, "force")) {
                    iterator.remove();
                    flags.clear();
                    flags.add(Flag.FORCE); break;
                }
                if (arg.equals("--all") && check(sender, "all")) {
                    iterator.remove();
                    flags.add(Flag.ALL); continue;
                }
                if (arg.equals("--vehicle") && check(sender, "vehicle")) {
                    iterator.remove();
                    flags.add(Flag.VEHICLE); continue;
                }
                if (arg.equals("--painting") && check(sender, "painting")) {
                    iterator.remove();
                    flags.add(Flag.PAINTING); continue;
                }
                if (arg.equals("--monster") && check(sender, "monster")) {
                    iterator.remove();
                    flags.add(Flag.MONSTER); continue;
                }
                if (arg.equals("--animal") && check(sender, "animal")) {
                    iterator.remove();
                    flags.add(Flag.ANIMAL); continue;
                }
                if (arg.equals("--watermob") && check(sender, "watermob")) {
                    iterator.remove();
                    flags.add(Flag.WATERMOB); continue;
                }
                if (arg.equals("--golem") && check(sender, "golem")) {
                    iterator.remove();
                    flags.add(Flag.GOLEM); continue;
                }
                if (arg.equals("--pet") && check(sender, "pet")) {
                    iterator.remove();
                    flags.add(Flag.PET); continue;
                }
                if (arg.equals("--villager") && check(sender, "villager")) {
                    iterator.remove();
                    flags.add(Flag.VILLAGER); continue;
                }
                if (arg.equals("--item") && check(sender, "item")) {
                    iterator.remove();
                    flags.add(Flag.ITEM);
                }
            }
            if (flags.contains(Flag.DEBUG)) {
                debug = !debug;
                sender.sendMessage("Debug mode is: " + (debug ? "enabled" : "disabled"));
                getConfig().set("debug", debug);
                saveConfig();
                return true;
            }
            List<World> worlds = getServer().getWorlds();
            if (sender instanceof Player) {
                if (flags.contains(Flag.ALL)) {
                    worlds.clear();
                    worlds.addAll(getServer().getWorlds());
                }
                if (arguments.size() > 0) {
                    worlds.clear();
                    for (String arg : arguments) {
                        World w = getServer().getWorld(arg);
                        if (w == null) {
                            sender.sendMessage(ChatColor.RED + "Unknown world: " + ChatColor.DARK_RED + arg);
                            continue;
                        }
                        if (check(sender, "world." + w.getName())) {
                            worlds.add(w);
                        }
                    }
                } else {
                    worlds.clear();
                    World w = ((Player)sender).getWorld();
                    if (check(sender, "world." + w.getName())) {
                        worlds.add(((Player)sender).getWorld());
                    }
                }
            }
            if (flags.contains(Flag.FORCE)) {
                sender.sendMessage(ChatColor.RED + "Warning! All entities other than players are being cleaned!");
            } else if (flags.contains(Flag.INFO)) {
                sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "World information" + ChatColor.GREEN + " ===");
            }
            if (flags.contains(Flag.BROADCAST)) {
                getServer().broadcastMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "Starting Cleaner" + (flags.contains(Flag.FORCE) ? " (" + ChatColor.RED + "Forced" + ChatColor.GOLD + ")" : "") + ChatColor.GREEN + " ===");
            } else {
                if (!flags.contains(Flag.INFO)) {
                    sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "Starting Cleaner" + (flags.contains(Flag.FORCE) ? " (" + ChatColor.RED + "Forced" + ChatColor.GOLD + ")" : "") + ChatColor.GREEN + " ===");
                }
            }
            if (worlds.size() == 0) {
                sender.sendMessage(ChatColor.RED + "No worlds specified. Aborting.");
                return true;
            }
            for (World world : worlds) {
                if (flags.contains(Flag.INFO)) {
                    String line = getEnvironment(world) + ChatColor.WHITE + " - " + ChatColor.GOLD + world.getEntities().size() + ChatColor.WHITE + " entities";
                    line += ChatColor.WHITE + " in " + ChatColor.GOLD + world.getLoadedChunks().length + ChatColor.WHITE + " chunks.";
                    sender.sendMessage(line);
                    continue;
                }
                int ents = world.getEntities().size();
                Iterator<Entity> iter = world.getEntities().iterator();
                int cleaned = 0;
                int vehicle = 0, tamed = 0, golem = 0, painting = 0, villager = 0, monster = 0, animal = 0, watermob = 0, item = 0;
                while (iter.hasNext()) {
                    Entity e = iter.next();
                    if (cleanerCheck(e, flags)) {
                        // all the checks passed, so we'll remove the entity
                        if (e instanceof Vehicle) {
                            vehicle++;
                        }
                        if (e instanceof Tameable && ((Tameable)e).isTamed()) {
                            tamed++;
                        }
                        if (e instanceof Painting) {
                            painting++;
                        }
                        if (e instanceof IronGolem && ((IronGolem)e).isPlayerCreated()) {
                            golem++;
                        }
                        if (e instanceof Villager) {
                            villager++;
                        }
                        if (e instanceof Monster) {
                            monster++;
                        }
                        if (e instanceof Animals) {
                            animal++;
                        }
                        if (e instanceof WaterMob) {
                            watermob++;
                        }
                        if (e instanceof Item) {
                            item++;
                        }
                        e.remove();
                        iter.remove();
                        cleaned++;
                    }
                }
                String line = ChatColor.AQUA + world.getName() + ChatColor.WHITE + ": " + cleaned + "/" + ents + " entities removed";
                sender.sendMessage(line);
                if (flags.contains(Flag.VERBOSE)) {
                    sender.sendMessage("Explicits: " + vehicle + " vehicles, " + painting + " paintings, " + villager + " villagers, ");
                    sender.sendMessage("Owned: " + tamed + " pets, " + golem + " golems,");
                    sender.sendMessage("General: " + monster + " monsters, " + animal + " animals, " + watermob + " water mobs, and " + item + " items.");
                }
            }
            if (!flags.contains(Flag.INFO)) {
                String line = ChatColor.GOLD + "Entities " + (flags.contains(Flag.FORCE) ? ChatColor.RED + "forcefully " : "") + ChatColor.GOLD + "cleaned.";
                if (flags.contains(Flag.BROADCAST)) {
                    getServer().broadcastMessage(line);
                } else {
                    sender.sendMessage(line);
                }
                if (sender instanceof Player) { log(">> " + sender.getName() + ": " + ChatColor.stripColor(line)); }
            }
        }
        return true;
    }

    // logging!
    public void log(String message) {
        getLogger().info(message);
    }

    // debugging
    public void debug(String message) {
        if (debug) {
            getLogger().info("[Debug] " + message);
        }
    }

    public boolean check(CommandSender sender, String val) {
        boolean checked = sender instanceof ConsoleCommandSender || sender.hasPermission("thecleaner." + val);
        if (!checked) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use the " + ChatColor.DARK_GREEN + val + ChatColor.RED + " flag.");
        }
        return checked;
    }

    public boolean cleanerCheck(Entity e, Set<Flag> flags) {
        if (flags.contains(Flag.FORCE) && !(e instanceof Player)) {
            return true;
        }
        if (e instanceof Painting && !flags.contains(Flag.PAINTING)) {
            // painting wasn't specified, so we're ignoring this entity
            if (!flags.contains(Flag.FORCE)) { return false; }
        }
        if (!(e instanceof Painting) && flags.contains(Flag.PAINTING)) {
            // this isn't a painting, and painting was specified
            return false;
        }
        if (!(e instanceof Monster) && flags.contains(Flag.MONSTER)) {
            // monster was specified, but this isn't a monster
            return false;
        }
        if (!(e instanceof Animals) && flags.contains(Flag.ANIMAL)) {
            // animal was specified, but this isn't an animal
            return false;
        }
        if (!(e instanceof WaterMob) && flags.contains(Flag.WATERMOB)) {
            // watermob was specified, but this isn't a watermob
            return false;
        }
        if (!(e instanceof Vehicle) && flags.contains(Flag.VEHICLE)) {
            // this isn't a vehicle, and vehicle was specified
            return false;
        }
        if (!(e instanceof Item) && flags.contains(Flag.ITEM)) {
            // this isn't an item, and item was specified
            return false;
        }
        if (e instanceof Item && e.getTicksLived() < 1200 && flags.contains(Flag.ITEM)) {
            // this is an item, it's younger than 1 minute, and item was specified
            return false;
        }
        if (e instanceof Golem) {
            if (e instanceof IronGolem && ((IronGolem)e).isPlayerCreated() && !flags.contains(Flag.GOLEM)) {
                if (!flags.contains(Flag.FORCE)) { return false; }
            }
        }
        if (e instanceof Villager && !flags.contains(Flag.VILLAGER)) {
            if (!flags.contains(Flag.FORCE)) { return false; }
        }
        if (e instanceof Player) {
            // ignore players!
            debug("Encountered player while cleaning... " + ((Player)e).getName());
            return false;
        }
        if (e instanceof Tameable) {
            // this is a possible pet.. let's see
            if (((Tameable)e).isTamed() && !flags.contains(Flag.PET)) {
                // it's a pet! let's ignore it if we don't have force specified
                if (!flags.contains(Flag.FORCE)) { return false; }
            }
        }
        if (e.getPassenger() != null && e.getPassenger() instanceof Player && !flags.contains(Flag.FORCE)) {
            // this entity has a passenger, and we haven't specified force
            return false;
        }
        return true;
    }

    public String getEnvironment(World world) {
        switch (world.getEnvironment()) {
            case NORMAL: return ChatColor.GREEN + world.getName();
            case NETHER: return ChatColor.RED + world.getName();
            case THE_END: return ChatColor.GRAY + world.getName();
            default: return world.getName();
        }
    }

}
