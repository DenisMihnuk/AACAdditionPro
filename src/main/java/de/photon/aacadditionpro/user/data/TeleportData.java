package de.photon.aacadditionpro.user.data;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.user.TimeData;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Used to store a player was teleported
 * The first index of this {@link TimeData} represents the last time a player was teleported.
 */
public class TeleportData extends TimeData
{
    static
    {
        AACAdditionPro.getInstance().registerListener(new TeleportDataUpdater());
    }

    public TeleportData(final User user)
    {
        // [0] = Teleport
        // [1] = World change
        // [2] = Respawn
        super(user, 0, 0, 0);
    }

    /**
     * A singleton class to reduce the reqired {@link Listener}s to a minimum.
     */
    private static class TeleportDataUpdater implements Listener
    {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onRespawn(final PlayerRespawnEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getTeleportData().nullifyTimeStamp(0);
                user.getTeleportData().nullifyTimeStamp(2);
            }
        }

        @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
        public void onTeleport(final PlayerTeleportEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getTeleportData().nullifyTimeStamp(0);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onWorldChange(final PlayerChangedWorldEvent event)
        {
            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user != null)
            {
                user.getTeleportData().nullifyTimeStamp(0);
                user.getTeleportData().nullifyTimeStamp(1);
            }
        }
    }
}
