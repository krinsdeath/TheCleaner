package net.krinsoft.thecleaner;

import org.bukkit.ChatColor;
import org.bukkit.Location;
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
            if (runtime > 1500 || ticks == 20) {
                CommandSender sender = getServer().getPlayer(runner);
                if (sender == null) {
                    sender = getServer().getConsoleSender();
                }
                if (ticks <= 10) {
                    sender.sendMessage("Server clock is almost stopped. Seriously consider removing any CPU intensive plugins.");
                } else if (ticks > 10 && ticks <= 15) {
                    sender.sendMessage("Server clock is running slow. Consider removing CPU intensive plugins.");
                } else if (ticks > 15 && ticks <=  18) {
                    sender.sendMessage("Server clock is running a bit slow. Consider optimizing plugin settings.");
                }
                String performance;
                if (runtime <= 1000) {
                    performance = ChatColor.GREEN + "Excellent";
                } else if (runtime > 1000 && runtime <= 1200) {
                    performance = ChatColor.GOLD + "Average";
                } else if (runtime > 1200 && runtime <= 1500) {
                    performance = ChatColor.RED + "Poor";
                } else {
                    performance = ChatColor.GRAY + "Terrible";
                }
                sender.sendMessage("Server performance: " + performance);
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
        clean_on_load_flags = getConfig().getString("startup.flags", "--monster --item");
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
        if (cmd.getName().equals("sysstat")) {
            if (!sender.hasPermission("thecleaner.system")) {
                sender.sendMessage(ChatColor.RED + "You can't use this command.");
                return true;
            }
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
            sender.sendMessage("Allocated (in use): " + (allocated / 1024L / 1024L) + "MB");
            sender.sendMessage("Free (available): " + (free / 1024L / 1024L) + "MB");
            try {
                clock.ID = getServer().getScheduler().scheduleSyncRepeatingTask(this, clock, 0L, 1L);
            } catch (RuntimeException e) {
                debug(e.getLocalizedMessage());
            }
        }
        if (cmd.getName().equals("cleanup")) {
            if (args.length == 0) {
                showHelp(sender, "help");
                return true;
            }
            List<String> arguments = new ArrayList<String>(Arrays.asList(args));
            List<String> worldList = new ArrayList<String>();
            Iterator<String> iterator = arguments.iterator();
            Set<Flag> flags = EnumSet.noneOf(Flag.class);
            int radius = 0;
            String topic = null;
            while (iterator.hasNext()) {
                String arg = iterator.next();
                if (arg.startsWith("--help")) {
                    iterator.remove();
                    flags.add(Flag.HELP);
                    try {
                        topic = arg.split("=")[1];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        topic = "help";
                    }
                    break;
                }
                if (!arg.startsWith("--")) {
                    iterator.remove();
                    worldList.add(arg);
                    continue;
                }
                if (arg.equals("--all") && check(sender, "all")) {
                    iterator.remove();
                    flags.add(Flag.ALL);
                    continue;
                }
                if (arg.equals("--debug") && check(sender, "debug")) {
                    iterator.remove();
                    flags.clear();
                    flags.add(Flag.DEBUG);
                    continue;
                }
                if (arg.equals("--broadcast") && check(sender, "broadcast")) {
                    iterator.remove();
                    flags.add(Flag.BROADCAST);
                    continue;
                }
                if (arg.equals("--info") && check(sender, "info")) {
                    iterator.remove();
                    flags.clear();
                    flags.add(Flag.INFO);
                    continue;
                }
                if (arg.equals("--verbose") && check(sender, "verbose")) {
                    iterator.remove();
                    flags.add(Flag.VERBOSE);
                    continue;
                }
                if (arg.equals("--force") && check(sender, "force")) {
                    iterator.remove();
                    flags.add(Flag.FORCE);
                    continue;
                }
                if (arg.equals("--all") && check(sender, "all")) {
                    iterator.remove();
                    flags.add(Flag.ALL);
                    continue;
                }
                if (arg.equals("--vehicle") && check(sender, "vehicle")) {
                    iterator.remove();
                    flags.add(Flag.VEHICLE);
                    continue;
                }
                if (arg.equals("--painting") && check(sender, "painting")) {
                    iterator.remove();
                    flags.add(Flag.PAINTING);
                    continue;
                }
                if (arg.equals("--monster") && check(sender, "monster")) {
                    iterator.remove();
                    flags.add(Flag.MONSTER);
                    continue;
                }
                if (arg.equals("--animal") && check(sender, "animal")) {
                    iterator.remove();
                    flags.add(Flag.ANIMAL);
                    continue;
                }
                if (arg.equals("--watermob") && check(sender, "watermob")) {
                    iterator.remove();
                    flags.add(Flag.WATERMOB);
                    continue;
                }
                if (arg.equals("--golem") && check(sender, "golem")) {
                    iterator.remove();
                    flags.add(Flag.GOLEM);
                    continue;
                }
                if (arg.equals("--pet") && check(sender, "pet")) {
                    iterator.remove();
                    flags.add(Flag.PET);
                    continue;
                }
                if (arg.equals("--villager") && check(sender, "villager")) {
                    iterator.remove();
                    flags.add(Flag.VILLAGER);
                    continue;
                }
                if (arg.equalsIgnoreCase("--dragon") && check(sender, "dragon")) {
                    iterator.remove();
                    flags.add(Flag.DRAGON);
                    continue;
                }
                if (arg.equals("--item") && check(sender, "item")) {
                    iterator.remove();
                    flags.add(Flag.ITEM);
                    continue;
                }
                if (arg.startsWith("--radius") && check(sender, "radius")) {
                    if (sender instanceof Player) {
                        try {
                            radius = Integer.parseInt(arg.split("=")[1]);
                            flags.add(Flag.RADIUS);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            sender.sendMessage(ChatColor.RED + "No radius setting specified.");
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Error parsing argument: expected number");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "You have to be a player to specify a radius.");
                    }
                    iterator.remove();
                    continue;
                }
                if (arg.equalsIgnoreCase("--projectile") && check(sender, "projectile")) {
                    iterator.remove();
                    flags.add(Flag.PROJECTILE);
                    continue;
                }
                if (arg.equalsIgnoreCase("--explosive") && check(sender, "explosive")) {
                    iterator.remove();
                    flags.add(Flag.EXPLOSIVE);
                    continue;
                }
                if (arg.equalsIgnoreCase("--report") && check(sender, "report")) {
                    iterator.remove();
                    flags.add(Flag.REPORT);
                }
            }
            if ((flags.isEmpty() && worldList.isEmpty()) || flags.contains(Flag.HELP)) {
                showHelp(sender, topic);
                return true;
            }
            if (flags.contains(Flag.DEBUG)) {
                debug = !debug;
                sender.sendMessage("Debug mode is: " + (debug ? "enabled" : "disabled"));
                getConfig().set("debug", debug);
                saveConfig();
                return true;
            }
            List<World> worlds = getServer().getWorlds();
            if (worldList.size() > 0 && !flags.contains(Flag.ALL)) {
                worlds.clear();
                for (String arg : worldList) {
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
                if (flags.contains(Flag.ALL)) {
                    worlds.addAll(getServer().getWorlds());
                } else {
                    if (sender instanceof Player) {
                        World w = ((Player)sender).getWorld();
                        if (check(sender, "world." + w.getName())) {
                            worlds.add(((Player)sender).getWorld());
                        }
                    } else {
                        worlds.addAll(getServer().getWorlds());
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
                    sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "Starting Cleaner" + (flags.contains(Flag.FORCE) ? " (" + ChatColor.RED + "Forced" + ChatColor.GOLD + ")" : "") + " - Flags: " + flags.toString() + ChatColor.GREEN + " ===");
                }
            }
            if (worlds.size() == 0) {
                sender.sendMessage(ChatColor.RED + "No worlds specified. Aborting.");
                return true;
            }
            Report report = new Report(this);
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
                        // check if the entity meets the radius requirement
                        if (flags.contains(Flag.RADIUS)) {
                            Location loc = ((Player)sender).getLocation();
                            if (!(e.getLocation().distanceSquared(loc) <= radius)) {
                                continue;
                            }
                        }
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
                        if (flags.contains(Flag.REPORT)) {
                            report.add(e);
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
            if (flags.contains(Flag.REPORT)) {
                report.write();
                sender.sendMessage("Report written to " + report.getFile());
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
        if (e instanceof Painting && flags.contains(Flag.PAINTING)) {
            // this is a painting, and the painting flag was added
            return true;
        }
        if (e instanceof Vehicle && flags.contains(Flag.VEHICLE)) {
            // this is a vehicle, and vehicle flag was explicitly added
            return true;
        }
        if (e instanceof Monster && flags.contains(Flag.MONSTER)) {
            if ((e instanceof EnderDragon || e instanceof EnderDragonPart) && !flags.contains(Flag.DRAGON)) {
                return false;
            }
            // this is a monster, and the monster flag was explicitly added
            return true;
        }
        if (e instanceof Animals && flags.contains(Flag.ANIMAL)) {
            if (e instanceof Tameable && ((Tameable) e).isTamed() && !flags.contains(Flag.PET)) {
                return false;
            }
            // this is an animal, and the animal flag was explicitly added
            return true;
        }
        if (e instanceof WaterMob && flags.contains(Flag.WATERMOB)) {
            // this is a water mob, and the water mob flag was explicitly added
            return true;
        }
        if (e instanceof Item && e.getTicksLived() > 1200 && flags.contains(Flag.ITEM)) {
            // this is an item, it's older than 1 minute, and item was specified
            return true;
        }
        if (e instanceof Golem && flags.contains(Flag.GOLEM)) {
            return true;
        }
        if (e instanceof Tameable && ((Tameable)e).isTamed() && flags.contains(Flag.PET)) {
            return true;
        }
        if (e instanceof Villager && flags.contains(Flag.VILLAGER)) {
            return true;
        }
        if ((e instanceof EnderDragon || e instanceof EnderDragonPart) && flags.contains(Flag.DRAGON)) {
            return true;
        }
        if (e instanceof Explosive && flags.contains(Flag.EXPLOSIVE)) {
            return true;
        }
        if (e instanceof Projectile && flags.contains(Flag.PROJECTILE)) {
            if (e.getTicksLived() > 1200 || ((Projectile)e).getShooter() == null) {
                return true;
            }
        }
        return false;
    }

    public String getEnvironment(World world) {
        switch (world.getEnvironment()) {
            case NORMAL: return ChatColor.GREEN + world.getName();
            case NETHER: return ChatColor.RED + world.getName();
            case THE_END: return ChatColor.GRAY + world.getName();
            default: return world.getName();
        }
    }

    public void showHelp(CommandSender sender, String f) {
        Flag flag = Flag.get(f);
        if (f != null && !f.equalsIgnoreCase("help") && !check(sender, flag.name())) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use that flag.");
        }
        sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "Help: " + ChatColor.AQUA + flag.name() + ChatColor.GREEN + " ===");
        sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.GOLD + flag.desc());
        sender.sendMessage(ChatColor.GREEN + "Usage: " + ChatColor.GOLD + flag.usage());
        if (flag.equals(Flag.HELP)) {
            sender.sendMessage("Topics:");
            StringBuilder help = new StringBuilder();
            for (Flag topic : Flag.values()) {
                if (check(sender, topic.name(), true)) {
                    help.append(topic.name()).append(", ");
                }
            }
            if (help.length() > 0) {
                sender.sendMessage(help.substring(0, help.length()-2));
            } else {
                sender.sendMessage("No topics available.");
            }
        }
    }

    public boolean check(CommandSender sender, String flag, boolean silent) {
        return sender instanceof ConsoleCommandSender || sender.hasPermission("thecleaner." + flag);
    }

}
