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
    private boolean debug = false;
    public boolean clean_on_load = false;
    public String clean_on_load_flags = "";

    public void onEnable() {
        debug = getConfig().getBoolean("debug", false);
        clean_on_load = getConfig().getBoolean("startup.clean", false);
        clean_on_load_flags = getConfig().getString("startup.flags", "");
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
        getConfig().set("startup.clean", clean_on_load);
        getConfig().set("startup.flags", clean_on_load_flags);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("cleanup")) {
            Set<Flag> flags = EnumSet.noneOf(Flag.class);
            for (String arg : args) {
                if (arg.equals("--info") && check(sender, "info")) {
                    flags.clear();
                    flags.add(Flag.INFO); break;
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
                                sender.sendMessage(ChatColor.RED + "You do not have permission to clear all entities.");
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
            }
            if (flags.contains(Flag.INFO)) {
                sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "World information" + ChatColor.GREEN + " ===");
            }
            for (World world : worlds) {
                if (flags.contains(Flag.INFO)) {
                    sender.sendMessage(ChatColor.GREEN + world.getName() + ChatColor.WHITE + " - " + ChatColor.GOLD + world.getEntities().size() + " entities");
                    continue;
                }
                int ents = world.getEntities().size();
                Iterator<Entity> iter = world.getEntities().iterator();
                int cleaned = 0;
                while (iter.hasNext()) {
                    Entity e = iter.next();
                    if (cleanerCheck(e, flags)) {
                        // all the checks passed, so we'll remove the entity
                        e.remove();
                        iter.remove();
                        cleaned++;
                    }
                }
                String line = world.getName() + ": " + cleaned + "/" + ents + " entities removed";
                sender.sendMessage(line);
            }
            if (!flags.contains(Flag.INFO)) {
                sender.sendMessage(ChatColor.GOLD + "Entities " + (flags.contains(Flag.FORCE) ? "forcefully " : "") + "cleaned.");
                if (sender instanceof Player) { log(">> " + ChatColor.GREEN + sender.getName() + ChatColor.WHITE + ": " + ChatColor.GOLD + "Entities " + (flags.contains(Flag.FORCE) ? "forcefully " : "") + "cleaned."); }
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
