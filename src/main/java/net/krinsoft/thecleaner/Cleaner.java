package net.krinsoft.thecleaner;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    public boolean limits_enabled = true;
    public int limits_monsters = 100;
    public int limits_animals = 25;
    public int limits_water = 5;

    public void onEnable() {
        debug = getConfig().getBoolean("debug", false);
        clean_on_overload = getConfig().getBoolean("overload.clean", true);
        clean_on_overload_total = getConfig().getInt("overload.total", 5000);
        clean_on_load = getConfig().getBoolean("startup.clean", true);
        clean_on_load_flags = getConfig().getString("startup.flags", "");
        limits_enabled = getConfig().getBoolean("limits.enabled", true);
        limits_monsters = getConfig().getInt("limits.monsters", 100);
        limits_animals = getConfig().getInt("limits.animals", 25);
        limits_water = getConfig().getInt("limits.water", 5);
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
        // spawn limiting
        getConfig().set("limits.enabled", limits_enabled);
        getConfig().set("limits.monsters", limits_monsters);
        getConfig().set("limits.animals", limits_animals);
        getConfig().set("limits.water", limits_water);
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
            Set<Flag> flags = EnumSet.noneOf(Flag.class);
            for (String arg : args) {
                if (arg.equals("--info") && check(sender, "info")) {
                    flags.clear();
                    flags.add(Flag.INFO); break;
                }
                if (arg.equals("--verbose") && check(sender, "verbose")) {
                    flags.add(Flag.VERBOSE); continue;
                }
                if (arg.equals("--force") && check(sender, "force")) {
                    flags.clear();
                    flags.add(Flag.FORCE); break;
                }
                if (arg.equals("--all") && check(sender, "all")) {
                    flags.add(Flag.ALL); continue;
                }
                if (arg.equals("--vehicle") && check(sender, "vehicle")) {
                    flags.add(Flag.VEHICLE); continue;
                }
                if (arg.equals("--painting") && check(sender, "painting")) {
                    flags.add(Flag.PAINTING); continue;
                }
                if (arg.equals("--monster") && check(sender, "monster")) {
                    flags.add(Flag.MONSTER); continue;
                }
                if (arg.equals("--animal") && check(sender, "animal")) {
                    flags.add(Flag.ANIMAL); continue;
                }
                if (arg.equals("--watermob") && check(sender, "watermob")) {
                    flags.add(Flag.WATERMOB); continue;
                }
                if (arg.equals("--golem") && check(sender, "golem")) {
                    flags.add(Flag.GOLEM); continue;
                }
                if (arg.equals("--pet") && check(sender, "pet")) {
                    flags.add(Flag.PET); continue;
                }
                if (arg.equals("--villager") && check(sender, "villager")) {
                    flags.add(Flag.VILLAGER);
                }
            }
            if (args.length == 1 && args[0].equalsIgnoreCase("--debug")) {
                if (check(sender, "debug")) {
                    debug = !debug;
                    sender.sendMessage("Debug mode is: " + (debug ? "enabled" : "disabled"));
                    getConfig().set("debug", debug);
                    saveConfig();
                } else {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to toggle Debug mode.");
                }
                return true;
            }
            List<World> worlds = getServer().getWorlds();
            if (sender instanceof Player) {
                if (args.length >= 1) {
                    for (String arg : args) {
                        if (arg.equalsIgnoreCase("--all")) {
                            if (!check(sender, "all")) {
                                sender.sendMessage(ChatColor.RED + "You do not have permission to clear every world's entities.");
                                return true;
                            }
                            worlds.clear();
                            worlds.addAll(getServer().getWorlds());
                        } else {
                            if (arg.equalsIgnoreCase("--all") || arg.equalsIgnoreCase("--debug") || arg.equalsIgnoreCase("--force") || arg.equalsIgnoreCase("--vehicle") || arg.equalsIgnoreCase("--info")) { continue; }
                            World w = getServer().getWorld(arg);
                            if (w != null) {
                                if (!check(sender, "world." + w.getName())) {
                                    sender.sendMessage(ChatColor.RED + "You do not have permission to clear entities in that world.");
                                    return true;
                                }
                                worlds.clear();
                                worlds.add(w);
                            } else {
                                sender.sendMessage(ChatColor.RED + "Unknown world.");
                                return true;
                            }
                        }
                    }
                } else {
                    worlds.clear();
                    worlds.add(((Player)sender).getWorld());
                }
            }
            if (flags.contains(Flag.FORCE)) {
                sender.sendMessage(ChatColor.RED + "Warning! All entities other than players are being cleaned!");
            } else if (flags.contains(Flag.INFO)) {
                sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "World information" + ChatColor.GREEN + " ===");
            } else {
                sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "Starting Cleaner" + ChatColor.GREEN + " ===");
            }
            for (World world : worlds) {
                if (flags.contains(Flag.INFO)) {
                    sender.sendMessage(ChatColor.GREEN + world.getName() + ChatColor.WHITE + " - " + ChatColor.GOLD + world.getEntities().size() + " entities");
                    continue;
                }
                int ents = world.getEntities().size();
                Iterator<Entity> iter = world.getEntities().iterator();
                int cleaned = 0;
                int vehicle = 0, tamed = 0, golem = 0, painting = 0, villager = 0;
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
                        e.remove();
                        iter.remove();
                        cleaned++;
                    }
                }
                String line = world.getName() + ": " + cleaned + "/" + ents + " entities removed";
                sender.sendMessage(line);
                if (flags.contains(Flag.VERBOSE)) {
                    line = "Including: " + vehicle + " vehicles, " + tamed + " pets, " + painting + " paintings, " + villager + " villagers, and " + golem + " golems.";
                    sender.sendMessage(line);
                }
            }
            if (!flags.contains(Flag.INFO)) {
                String line = ChatColor.GOLD + "Entities " + (flags.contains(Flag.FORCE) ? ChatColor.RED + "forcefully " : "") + ChatColor.GOLD + "cleaned.";
                sender.sendMessage(line);
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
        return sender instanceof ConsoleCommandSender || sender.hasPermission("thecleaner." + val);
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

}
