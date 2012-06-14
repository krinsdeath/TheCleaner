package net.krinsoft.thecleaner;

/**
 * @author krinsdeath
 */
public enum Flag {
    HELP("Specifies help message for the provided flag.", "--help=[topic]"),
    ALL("Cleans entities on every world.", "--all"),
    DEBUG("Enables debug mode.", "--debug"),
    BROADCAST("Broadcasts that entities are being cleaned.", "--broadcast"),
    INFO("Outputs information about the loaded worlds.", "--info"),
    VERBOSE("Prints detailed entity cleanup information.", "--verbose"),
    FORCE("Forces all entities to be cleaned.", "--force"),
    VEHICLE("Cleans up vehicles.", "--vehicle"),
    PAINTING("Cleans up paintings.", "--painting"),
    MONSTER("Cleans up monsters.", "--monster"),
    ANIMAL("Cleans up animals.", "--animal"),
    WATERMOB("Cleans up water mobs.", "--watermob"),
    GOLEM("Cleans up golems.", "--golem"),
    PET("Cleans up pets.", "--pet"),
    VILLAGER("Cleans up villagers.", "--villager"),
    ITEM("Cleans up items older than 60 seconds.", "--item"),
    RADIUS("Cleans entities in a radius around the player.", "--radius=[num]");
    private String desc;
    private String usage;

    Flag(String desc, String usage) {
        this.desc = desc;
        this.usage = usage;
    }

    public String desc() {
        return this.desc;
    }

    public String usage() {
        return this.usage;
    }

    public static Flag get(String flag) {
        for (Flag f : values()) {
            if (f.name().equalsIgnoreCase(flag)) {
                return f;
            }
        }
        return HELP;
    }
}
