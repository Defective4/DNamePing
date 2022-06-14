package net.defekt.paper.nameping;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

    private String pingFormat = "&a";
    private String continueFormat = "&7";
    private Sound pingSound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private float pingPitch = 1;
    private boolean enableSound = true;
    private boolean enableColor = true;

    private Map<Permission, String> formats = new ConcurrentHashMap<Permission, String>();

    @Override
    public void onDisable() {
        PluginManager mgr = getServer().getPluginManager();
        for (Permission perm : formats.keySet())
            mgr.removePermission(perm);
    }

    private void load() {
        saveDefaultConfig();
        reloadConfig();

        FileConfiguration config = getConfig();

        enableColor = config.getBoolean("enableColor");
        pingFormat = config.getString("pingFormat");
        continueFormat = config.getString("defaultContinueFormat");

        ConfigurationSection sec = config.getConfigurationSection("continuePerms");
        if (sec != null) {
            PluginManager mgr = getServer().getPluginManager();
            for (Entry<String, Object> val : sec.getValues(false).entrySet()) {
                if (val.getValue() instanceof ConfigurationSection) {
                    String format = ((ConfigurationSection) val.getValue()).getString("format");
                    if (format != null && !format.isEmpty()) {
                        Permission perm = new Permission(val.getKey().replace("_", "."), "", PermissionDefault.FALSE);
                        formats.put(perm, format);
                        mgr.addPermission(perm);
                        System.out.println(perm.getName());
                    }
                }
            }
        }

        enableSound = config.getBoolean("enableSound");
        String pingSoundS = config.getString("pingSound");
        Sound val = Sound.valueOf(pingSoundS.toUpperCase());
        if (val != null)
            pingSound = val;
        pingPitch = (float) config.getDouble("pingPitch");

    }

    @Override
    public void onEnable() {
        load();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("npreload").setExecutor(new CommandExecutor() {

            @Override
            public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
                load();
                arg0.sendMessage(ChatColor.GREEN + "Konfiguracja prze³adowana!");
                return true;
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        String message = e.getMessage();
        Player player = e.getPlayer();
        String format = continueFormat;

        for (Entry<Permission, String> perm : formats.entrySet()) {
            if (player.hasPermission(perm.getKey()))
                format = perm.getValue();
        }

        for (Player recipient : e.getRecipients().toArray(new Player[0])) {

            String recName = recipient.getName();
            if (message.toLowerCase().contains(recName.toLowerCase())) {
                if (enableColor) {
                    message = replace(message, recName,
                            ChatColor.translateAlternateColorCodes("&".charAt(0), pingFormat) + recName
                                    + ChatColor.translateAlternateColorCodes("&".charAt(0), format));
                }
                if (enableSound) {
                    recipient.playSound(recipient.getLocation(), pingSound, 2, pingPitch);
                }
            }
        }
        e.setMessage(message);

//        try {
//            for (Player recipient : e.getRecipients().toArray(new Player[0])) {
//                String recName = recipient.getName();
//                if (message.toLowerCase().contains(recName.toLowerCase())) {
//                    if (enableColor) {
//                        e.getRecipients().remove(recipient);
//                        String newMsg = replace(message, recName,
//                                ChatColor.translateAlternateColorCodes("&".charAt(0), pingFormat) + recName
//                                        + lastColor);
//                        recipient.sendMessage(String.format(e.getFormat(), displayName, newMsg));
//                    }
//                    if (enableSound) {
//                        recipient.playSound(recipient.getLocation(), pingSound, 2, pingPitch);
//                    }
//                }
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
    }

    public static String replace(String source, String target, String replacement) {
        StringBuilder sbSource = new StringBuilder(source);
        StringBuilder sbSourceLower = new StringBuilder(source.toLowerCase());
        String searchString = target.toLowerCase();

        int idx = 0;
        while ((idx = sbSourceLower.indexOf(searchString, idx)) != -1) {
            sbSource.replace(idx, idx + searchString.length(), replacement);
            sbSourceLower.replace(idx, idx + searchString.length(), replacement);
            idx += replacement.length();
        }
        sbSourceLower.setLength(0);
        sbSourceLower.trimToSize();
        sbSourceLower = null;

        return sbSource.toString();
    }

}
