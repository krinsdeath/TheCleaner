package net.krinsoft.thecleaner;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EnderDragonPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Explosive;
import org.bukkit.entity.Golem;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WaterMob;
import org.bukkit.entity.Wither;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author krinsdeath
 */
public class Cleaner extends JavaPlugin {
    private class Monitor implements Runnable {
        private int ID;
        private int ticks = 0;
        private int total = 0;
        private short iterations = 0;
        private long started;
        private long runtime;

        public Monitor() {
            started = System.currentTimeMillis();
        }

        @Override
        public void run() {
            ticks++;
            long run = System.currentTimeMillis() - started;
            if (ticks == 20 || run > 1000) {
                total += ticks;
                iterations++;
                if (iterations == 5) {
                    long time = System.currentTimeMillis();
                    int avg = total / 5;
                    try {
                        FileWriter out = new FileWriter(new File(getDataFolder(), "performance.txt"), true);
                        out.write(time + "," + avg + "\n");
                        out.close();
                        debug("Performance saved.");
                    } catch (IOException e) {
                        debug(e.getLocalizedMessage());
                    }
                    getServer().getScheduler().cancelTask(this.ID);
                } else {
                    runtime += run;
                    started = System.currentTimeMillis();
                    ticks = 0;
                }
            }
        }
    }

    private class Clock implements Runnable {
        private int ID = 0;
        private int iterations = 0;
        private int ticks = 0;
        private int total = 0;
        private long started;
        private long runtime;
        private String runner;

        public Clock(CommandSender sender) {
            runner = sender.getName();
            started = System.currentTimeMillis();
            sender.sendMessage("Calculating tick rate over 5 seconds...");
        }

        @Override
        public void run() {
            ticks++;
            long runtime = System.currentTimeMillis() - started;
            if (runtime > 1000 || ticks == 20) {
                iterations++;
                if (iterations == 5) {
                    CommandSender sender = getServer().getPlayer(runner);
                    if (sender == null) {
                        sender = getServer().getConsoleSender();
                    }
                    this.total += ticks;
                    this.runtime += runtime;
                    int t = this.total / iterations;
                    long r = this.runtime / iterations;
                    if (t <= 10) {
                        sender.sendMessage("Server clock is almost stopped. Seriously consider removing any CPU intensive plugins.");
                    } else if (t > 10 && t <= 15) {
                        sender.sendMessage("Server clock is running slow. Consider removing CPU intensive plugins.");
                    } else if (t > 15 && t <=  18) {
                        sender.sendMessage("Server clock is running a bit slow. Consider optimizing plugin settings.");
                    }
                    String performance;
                    if (r < 1049) {
                        performance = ChatColor.GREEN + "Excellent";
                    } else if (r >= 1050 && r < 1249) {
                        performance = ChatColor.GOLD + "Average";
                    } else if (r >= 1250 && r < 1549) {
                        performance = ChatColor.RED + "Poor";
                    } else {
                        performance = ChatColor.GRAY + "Terrible";
                    }
                    sender.sendMessage("Server performance: " + performance);
                    sender.sendMessage("Average ticks: " + t + " (Over an average of " + r + "ms)");
                    sender.sendMessage("Total ticks: " + this.total + " (Over a total of " + this.runtime + "ms)");
                    sender.sendMessage("Server tick rate: " + (20f - Math.abs(((float) this.runtime - 5000f) / 50f / 5f)));
                    getServer().getScheduler().cancelTask(ID);
                    if (getServer().getScheduler().isCurrentlyRunning(ID)) {
                        throw new RuntimeException("Clock time exceeded.");
                    }
                } else {
                    this.total += ticks;
                    this.runtime += runtime;
                    this.started = System.currentTimeMillis();
                    this.ticks = 0;
                }
            }
        }
    }

    private boolean debug = false;
    public boolean clean_on_overload = true;
    public int clean_on_overload_total = 5000;
    public boolean clean_on_load = true;
    public String clean_on_load_flags = "";
    public boolean chunk_recovery_mode = false;
    public int chunk_entity_cutoff = 100;

    public boolean monitor_performance = false;
    public int monitor_performance_period = 30;

    public void onEnable() {
        debug = getConfig().getBoolean("debug", false);
        clean_on_overload = getConfig().getBoolean("overload.clean", true);
        clean_on_overload_total = getConfig().getInt("overload.total", 5000);
        clean_on_load = getConfig().getBoolean("startup.clean", true);
        clean_on_load_flags = getConfig().getString("startup.flags", "--monster --item --explosive --projectile");
        chunk_recovery_mode = getConfig().getBoolean("startup.chunk_recovery", false);
        chunk_entity_cutoff = getConfig().getInt("info.problem_chunk_entity_cutoff", 100);
        monitor_performance = getConfig().getBoolean("info.monitor_performance", false);
        monitor_performance_period = getConfig().getInt("info.monitor_performance_period", 30);
        if (monitor_performance) {
            scheduleMonitor();
        }
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
        getConfig().options().header("Debug turns extra log messages on for finding the location and cause of bugs.\n" +
                "Overload settings determine the threshold for cleaning entities on huge explosions.\n" +
                "Startup settings determine the flags specified when cleaning entities when worlds are loaded.\n" +
                "Chunk recovery causes a force clean on all chunks as they are loaded, to try to fix possible chunk corruption.\n" +
                "Problem chunks contain entities more than the specified cutoff. These will be warned in the server log and cleaned.\n" +
                "Performance monitor specifies the sample period in minutes between each tick measure."
        );
        getConfig().set("debug", debug);
        // cleanup on entity overload
        getConfig().set("overload.clean", clean_on_overload);
        getConfig().set("overload.total", clean_on_overload_total);
        // cleanup at world loading
        getConfig().set("startup.clean", clean_on_load);
        getConfig().set("startup.flags", clean_on_load_flags);
        getConfig().set("startup.chunk_recovery", chunk_recovery_mode);
        // info options
        getConfig().set("info.problem_chunk_entity_cutoff", chunk_entity_cutoff);
        getConfig().set("info.monitor_performance", monitor_performance);
        getConfig().set("info.monitor_performance_period", monitor_performance_period);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("performance")) {
            if (!sender.hasPermission("thecleaner.performance")) {
                sender.sendMessage(ChatColor.RED + "You can't use this command.");
                return true;
            }
            long days = 1;
            if (args.length >= 1) {
                try {
                    days = Long.parseLong(args[0]);
                } catch (NumberFormatException e) {
                    if (args[0].equalsIgnoreCase("--reset") || args[0].equalsIgnoreCase("-r")) {
                        if (new File(getDataFolder(), "performance.txt").delete()) {
                            sender.sendMessage("Performance data successfully reset.");
                        }
                        return true;
                    }
                    if (args[0].equalsIgnoreCase("--help") || args[0].equalsIgnoreCase("-h")) {
                        return false;
                    }
                    debug("Expected a number: " + e.getLocalizedMessage());
                    return false;
                }
            }
            showPerformance(sender, days);
            return true;
        }
        if (cmd.getName().equals("sysstat")) {
            if (!sender.hasPermission("thecleaner.system")) {
                sender.sendMessage(ChatColor.RED + "You can't use this command.");
                return true;
            }
            if (args.length > 0 && (args[0].equalsIgnoreCase("--verbose") || args[0].equalsIgnoreCase("-v"))) {
                String os = System.getProperty("os.name") + " (" + System.getProperty("os.version") + ") - " + System.getProperty("os.arch");
                String version = System.getProperty("java.vendor") + " " + System.getProperty("java.version");
                sender.sendMessage("OS: " + os);
                sender.sendMessage("Java Version: " + version);
            }
            showMemory(sender);
            showTicks(sender);
            return true;
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
                if (arg.equals("--vitals") && check(sender, "vitals")) {
                    iterator.remove();
                    flags.add(Flag.VITALS);
                    flags.add(Flag.INFO);
                    flags.add(Flag.ALL);
                    break;
                }
                if (arg.equals("--broadcast") && check(sender, "broadcast")) {
                    iterator.remove();
                    flags.add(Flag.BROADCAST);
                    continue;
                }
                if (arg.equals("--info") && check(sender, "info")) {
                    iterator.remove();
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
                if (arg.equals("--npc") && check(sender, "npc")) {
                    iterator.remove();
                    flags.add(Flag.NPC);
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
                if (arg.equals("--frame") && check(sender, "frame")) {
                    iterator.remove();
                    flags.add(Flag.FRAME);
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
                    if (flags.contains(Flag.VERBOSE)) {
                        StringBuilder chunks = new StringBuilder();
                        for (Chunk chunk : world.getLoadedChunks()) {
                            int ents = chunk.getEntities().length;
                            if (ents > chunk_entity_cutoff) {
                                if (chunks.length() > 0) {
                                    chunks.append("\n");
                                }
                                chunks.append("Problem Chunk [").append(chunk.getWorld().getName()).append(",").append(chunk.getX()).append(",").append(chunk.getZ()).append("]: ");
                                chunks.append(ents).append(" entities");
                            }
                        }
                        if (chunks.toString().length() > 0) {
                            sender.sendMessage(chunks.toString());
                        } else {
                            sender.sendMessage("No problem chunks detected.");
                        }
                    }
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
            if (flags.contains(Flag.VITALS)) {
                showMemory(sender);
                showTicks(sender);
            }
            return true;
        }
        return false;
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
        if (e instanceof LivingEntity) {
            if (e instanceof Monster) {
                if ((e instanceof EnderDragon || e instanceof EnderDragonPart)) {
                    return flags.contains(Flag.DRAGON);
                }
                if (e instanceof Wither) {
                    return flags.contains(Flag.WITHER);
                }
                // this is a monster, and the monster flag was explicitly added
                return flags.contains(Flag.MONSTER);
            }
            if (e instanceof Animals) {
                if (e instanceof Tameable && ((Tameable) e).isTamed()) {
                    return flags.contains(Flag.PET);
                }
                return flags.contains(Flag.ANIMAL);
            }
            if (e instanceof WaterMob) {
                return flags.contains(Flag.WATERMOB);
            }
            if (e instanceof Golem) {
                return flags.contains(Flag.GOLEM);
            }
            if (e instanceof NPC) {
                if (e instanceof Villager) {
                    return flags.contains(Flag.VILLAGER);
                }
                return flags.contains(Flag.NPC);
            }
        } else {
            if (e instanceof Painting) {
                // this is a painting, and the painting flag was added
                return flags.contains(Flag.PAINTING);
            }
            if (e instanceof Vehicle) {
                // this is a vehicle, and vehicle flag was explicitly added
                return flags.contains(Flag.VEHICLE);
            }
            if (e instanceof Item && e.getTicksLived() > 1200) {
                // this is an item, it's older than 1 minute, and item was specified
                return flags.contains(Flag.ITEM);
            }
            if (e instanceof ItemFrame) {
                return flags.contains(Flag.FRAME);
            }
            if (e instanceof Explosive) {
                return flags.contains(Flag.EXPLOSIVE);
            }
            if (e instanceof Projectile) {
                if (e.getTicksLived() > 1200 || ((Projectile)e).getShooter() == null) {
                    return flags.contains(Flag.PROJECTILE);
                }
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
        Flag flag = Flag.forName(f);
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

    public void showPerformance(CommandSender sender, long days) {
        if (days < 1) { days = 1; }
        try {
            FileInputStream reader = new FileInputStream(new File(getDataFolder(), "performance.txt"));
            BufferedReader in = new BufferedReader(new InputStreamReader(reader));
            long period = System.currentTimeMillis() - (1000L * 60L * 60L * 24L * days);
            String line;
            int average = 0;
            int iterations = 0;
            while ((line = in.readLine()) != null) {
                try {
                    long epoch = Long.parseLong(line.substring(0, line.indexOf(',')));
                    if (epoch >= period) {
                        int avg = Integer.parseInt(line.substring(line.indexOf(',')+1));
                        average += avg;
                        iterations++;
                    }
                } catch (NumberFormatException e) {
                    debug("Something went wrong: " + e.getLocalizedMessage());
                }
            }
            if (average > 0) {
                int avg = average / iterations;
                sender.sendMessage("Average tick rate over the last " + ChatColor.AQUA + (days > 1 ? days + " days": "day") + ChatColor.RESET + " was " + ChatColor.GREEN + avg + ChatColor.RESET + ".");
                sender.sendMessage("Sample period is every " + ChatColor.GREEN + monitor_performance_period + " minute" + (monitor_performance_period > 1 ? "s":"") + ChatColor.RESET + ".");
                if (avg >= 20) {
                    sender.sendMessage("Tick rate is excellent.");
                } else if (average >= 17) {
                    sender.sendMessage("Tick rate is average.");
                } else if (average >= 14) {
                    sender.sendMessage("Tick rate is below average.");
                } else if (average <= 13) {
                    sender.sendMessage("Tick rate is low.");
                }
            } else {
                sender.sendMessage("No performance has been measured.");
            }
        } catch (FileNotFoundException e) {
            sender.sendMessage("No performance has been measured yet.");
        } catch (IOException e) {
            debug(e.getLocalizedMessage());
        }
    }

    public void showMemory(CommandSender sender) {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory  = runtime.maxMemory();
        long allocated  = runtime.totalMemory();
        long free       = runtime.freeMemory();
        sender.sendMessage("Maximum memory: " + (maxMemory / 1024L / 1024L) + "MB");
        sender.sendMessage("Allocated (in use): " + (allocated / 1024L / 1024L) + "MB");
        sender.sendMessage("Free (available): " + (free / 1024L / 1024L) + "MB");
    }

    public void showTicks(CommandSender sender) {
        try {
            Clock clock = new Clock(sender);
            clock.ID = getServer().getScheduler().scheduleSyncRepeatingTask(this, clock, 1L, 1L);
        } catch (RuntimeException e) {
            debug(e.getLocalizedMessage());
        }
    }

    private void scheduleMonitor() {
        try {
            getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
                @Override
                public void run() {
                    try {
                        Monitor mon = new Monitor();
                        mon.ID = getServer().getScheduler().scheduleSyncRepeatingTask(Cleaner.this, mon, 1L, 1L);
                    } catch (RuntimeException e) {
                        debug(e.getLocalizedMessage());
                    }
                }
            }, 1L, monitor_performance_period * 20L * 60L);
        } catch (RuntimeException e) {
            debug(e.getLocalizedMessage());
        }
    }

    public boolean check(CommandSender sender, String flag, boolean silent) {
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender || sender.hasPermission("thecleaner." + flag);
    }

}
