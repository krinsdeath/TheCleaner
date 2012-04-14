package net.krinsoft.thecleaner;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Iterator;
import java.util.List;

/**
 * @author krinsdeath
 */
public class Cleaner extends JavaPlugin {
    private boolean debug = false;

    public void onEnable() {
        debug = getConfig().getBoolean("debug", false);
        log("Debug mode is: " + (debug ? "enabled" : "disabled"));
        saveConfig();
        log("Plugin enabled successfully.");
    }

    public void onDisable() {
        log("Plugin disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equals("cleanup")) {
            boolean force = false;
            boolean vehicle = false;
            boolean info = false;
            boolean painting = false;
            for (String arg : args) {
                if (arg.equals("--force") && check(sender, "force")) {
                    force = true;
                }
                if (arg.equals("--vehicle") && check(sender, "vehicle")) {
                    vehicle = true;
                }
                if (arg.equals("--info") && check(sender, "info")) {
                    info = true;
                }
                if (arg.equals("--painting") && check(sender, "painting")) {
                    painting = true;
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
                                if (!check(sender, w.getName())) {
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
            if (info) {
                sender.sendMessage(ChatColor.GREEN + "=== " + ChatColor.GOLD + "World information" + ChatColor.GREEN + " ===");
            }
            for (World world : worlds) {
                if (info) {
                    sender.sendMessage(ChatColor.GREEN + world.getName() + ChatColor.WHITE + " - " + ChatColor.GOLD + world.getEntities().size() + " entities");
                    continue;
                }
                int ents = world.getEntities().size();
                Iterator<Entity> iter = world.getEntities().iterator();
                int cleaned = 0;
                while (iter.hasNext()) {
                    Entity e = iter.next();
                    if (e instanceof Painting && !painting) {
                        debug("Encountered a painting: " + ((Painting)e).getArt().toString());
                        continue;
                    }
                    if (!(e instanceof Painting) && painting) {
                        continue;
                    }
                    if (!(e instanceof Vehicle) && vehicle) {
                        continue;
                    }
                    if (e instanceof Player) {
                        debug("Encountered player while cleaning... " + ((Player)e).getName());
                        continue;
                    }
                    if (e instanceof Tameable) {
                        if (((Tameable)e).isTamed() && !force) {
                            debug("Encountered player pet while cleaning... " + ((Tameable)e).getOwner());
                            continue;
                        }
                    }
                    if (e.getPassenger() != null && e.getPassenger() instanceof Player && !force) {
                        debug("Encountered vehicle with passenger... " + ((Player)e.getPassenger()).getName());
                        continue;
                    }
                    e.remove();
                    world.getEntities().remove(e);
                    cleaned++;
                }
                String line = world.getName() + ": " + cleaned + "/" + ents + " entities removed";
                sender.sendMessage(line);
            }
            if (!info) {
                sender.sendMessage("Entities cleaned.");
                log(">> " + sender.getName() + ": Entities cleaned.");
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

}
