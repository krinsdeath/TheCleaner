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
            boolean golem = false;
            for (String arg : args) {
                if (arg.equals("--force") && check(sender, "force")) {
                    force = true;
                }
                if (arg.equals("--vehicle") && check(sender, "vehicle")) {
                    vehicle = true;
                }
                if (arg.equals("--painting") && check(sender, "painting")) {
                    painting = true;
                }
                if (arg.equals("--info") && check(sender, "info")) {
                    info = true;
                }
                if (arg.equals("--golem") && check(sender, "golem")) {
                    golem = true;
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
                        // painting wasn't specified, so we're ignoring this entity
                        debug("Encountered a painting: " + ((Painting)e).getArt().toString());
                        continue;
                    }
                    if (!(e instanceof Painting) && painting) {
                        // this isn't a painting, and painting was specified
                        continue;
                    }
                    if (!(e instanceof Vehicle) && vehicle) {
                        // this isn't a vehicle, and vehicle was specified
                        continue;
                    }
                    if (e instanceof Golem) {
                        if (e instanceof IronGolem && ((IronGolem)e).isPlayerCreated() && !golem) {
                            if (!force) { continue; }
                        }
                    }
                    if (e instanceof Player) {
                        // ignore players!
                        debug("Encountered player while cleaning... " + ((Player)e).getName());
                        continue;
                    }
                    if (e instanceof Tameable) {
                        // this is a possible pet.. let's see
                        if (((Tameable)e).isTamed() && !force) {
                            // it's a pet! let's ignore since we don't have force specified
                            debug("Encountered player pet while cleaning... " + ((Tameable)e).getOwner());
                            continue;
                        }
                    }
                    if (e.getPassenger() != null && e.getPassenger() instanceof Player && !force) {
                        // this entity has a passenger, and we haven't specified force
                        debug("Encountered vehicle with passenger... " + ((Player)e.getPassenger()).getName());
                        continue;
                    }
                    // all the checks passed, so we'll remove the entity
                    e.remove();
                    iter.remove();
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
