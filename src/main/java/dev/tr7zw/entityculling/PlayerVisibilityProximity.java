package dev.tr7zw.entityculling;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerVisibilityProximity implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joinedPlayer = event.getPlayer();
        updatePlayerVisibility(joinedPlayer);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player movedPlayer = event.getPlayer();
        updatePlayerVisibility(movedPlayer);
    }
    @EventHandler
    public void onChangeWorld(PlayerChangedWorldEvent event) {
        Player movedPlayer = event.getPlayer();
        updatePlayerVisibility(movedPlayer);
    }

    private void updatePlayerVisibility(Player player) {
        for (Player onlinePlayer : Bukkit.getServer().getOnlinePlayers()) {
            if (!player.equals(onlinePlayer) && player.getWorld().equals(onlinePlayer.getWorld())) {
                double distance = player.getLocation().distance(onlinePlayer.getLocation());
                boolean isVisible = distance <= CullingPlugin.instance.config.getInt("player-visibility-range");

                if (isVisible) {
                    player.showPlayer(CullingPlugin.instance, onlinePlayer);
                } else {
                    player.hidePlayer(CullingPlugin.instance, onlinePlayer);
                }
            }
        }
    }


}
