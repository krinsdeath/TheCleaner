package net.krinsoft.thecleaner;

import java.util.EnumSet;
import java.util.Set;

/**
 * @author krinsdeath
 */
public enum Flag {
    HELP("Specifies help message for the provided flag.", "--help=[topic]", true),
    ALL("Cleans entities on every world.", "--all", false),
    WORLD("Cleans up entities on the specified world.", "--world=[world name]", true),
    DEBUG("Enables debug mode.", "--debug", false),
    BROADCAST("Broadcasts that entities are being cleaned.", "--broadcast", false),
    INFO("Outputs information about the loaded worlds.", "--info", false),
    VERBOSE("Prints detailed entity cleanup information.", "--verbose", false),
    FORCE("Forces all entities to be cleaned.", "--force", false),
    ENTITY("Cleans up the specified entity.", "--entity=[name]", true),
    VEHICLE("Cleans up vehicles.", "--vehicle", false),
    PAINTING("Cleans up paintings.", "--painting", false),
    MONSTER("Cleans up monsters.", "--monster", false),
    ANIMAL("Cleans up animals.", "--animal", false),
    WATERMOB("Cleans up water mobs.", "--watermob", false),
    GOLEM("Cleans up golems.", "--golem", false),
    PET("Cleans up pets.", "--pet", false),
    VILLAGER("Cleans up villagers.", "--villager", false),
    NPC("Cleans up NPCS (e.g. from Citizens).", "--npc", false),
    DRAGON("Kills the Ender Dragon.", "--dragon", false),
    ITEM("Cleans up items older than 60 seconds.", "--item", false),
    RADIUS("Cleans entities in a radius around the player.", "--radius=[num]", true),
    EXPLOSIVE("Cleans up left-over explosives.", "--explosive", false),
    PROJECTILE("Cleans up orphaned projectiles.", "--projectile", false),
    REPORT("Creates a detailed report of all entities cleaned.", "--report", false);
    private String desc;
    private String usage;
    private boolean option;

    Flag(String desc, String usage, boolean option) {
        this.desc = desc;
        this.usage = usage;
        this.option = option;
    }

    public String desc() {
        return this.desc;
    }

    public String usage() {
        return this.usage;
    }

    public static Flag forName(String flag) {
        for (Flag f : values()) {
            if (f.name().equalsIgnoreCase(flag)) {
                return f;
            }
        }
        return HELP;
    }

    /**
     * Fetches a set of Flags that match whether or not they allow an option, based on the passed argument
     * @param option Whether or not the set of flags should allow options or not
     * @return if true, a set of flags which allow options, otherwise a set of flags that do not allow options
     */
    public static EnumSet<Flag> hasOption(boolean option) {
        EnumSet<Flag> flags = EnumSet.noneOf(Flag.class);
        for (Flag f : values()) {
            if (f.option == option) {
                flags.add(f);
            }
        }
        return flags;
    }
}
