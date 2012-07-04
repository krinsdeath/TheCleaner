package net.krinsoft.thecleaner;

import org.bukkit.entity.Entity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @author krinsdeath
 */
public class Report {
    private Map<String, Integer> counts = new HashMap<String, Integer>();
    private Cleaner plugin;
    private File file;

    public Report(Cleaner instance) {
        plugin = instance;
    }

    /**
     * Increments the report counter by 1 for the specified entity.
     * @param e The entity we're incrementing a report count for.
     * @return The new count for the specified entity.
     */
    public int add(Entity e) {
        Integer count = counts.get(e.toString());
        if (count == null) {
            count = 0;
        }
        count++;
        counts.put(e.toString(), count);
        return count;
    }

    /**
     *
     */
    public void write() {
        try {
            int i = 1;
            file = new File(plugin.getDataFolder(), "report.txt");
            while (file.exists()) {
                file = new File(plugin.getDataFolder(), "report"+i+".txt");
                i++;
            }
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(file));
            out.write("# The following is a report of entities cleaned.\n");
            if (counts.size() == 0) {
                out.write("# No entities were cleaned.\n");
            }
            for (String key : counts.keySet()) {
                out.write(key + ": " + counts.get(key) + "\n");
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            plugin.getLogger().warning("An error occurred while writing the report.");
            e.printStackTrace();
        }
    }

    public String getFile() {
        return file.toString();
    }

}
