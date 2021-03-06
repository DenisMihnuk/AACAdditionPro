package de.photon.aacadditionpro.events;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class PlayerAdditionViolationEvent extends ClientControlEvent
{
    private static final HandlerList handlers = new HandlerList();

    //Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    private final int vl;

    public static PlayerAdditionViolationEvent build(final Player p, final ModuleType moduleType, final int i, final String message)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC113:
                return new PlayerAdditionViolationEvent(p, moduleType, i, message, true);
            case MC114:
            case MC115:
                return new PlayerAdditionViolationEvent(p, moduleType, i, message);
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    public static PlayerAdditionViolationEvent build(final Player p, final ModuleType moduleType, final int i)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC113:
                return new PlayerAdditionViolationEvent(p, moduleType, i, moduleType.getViolationMessage(), true);
            case MC114:
            case MC115:
                return new PlayerAdditionViolationEvent(p, moduleType, i, moduleType.getViolationMessage());
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    @Override
    public PlayerAdditionViolationEvent call()
    {
        Bukkit.getPluginManager().callEvent(this);
        return this;
    }

    /**
     * Constructor for 1.14 and onwards.
     */
    public PlayerAdditionViolationEvent(final Player p, final ModuleType moduleType, final int i, final String message)
    {
        super(p, moduleType, message);
        this.vl = i;
    }

    /**
     * Dummy constructor for legacy minecraft versions before 1.14.
     */
    public PlayerAdditionViolationEvent(final Player p, final ModuleType moduleType, final int i, final String message, final boolean legacy)
    {
        super(p, moduleType, message, legacy);
        this.vl = i;
    }

    /**
     * Used to get the current vl of a player in a certain check. The checks are distinguished by their {@link ModuleType}
     *
     * @return the new vl of the player in the {@link ModuleType}
     */
    public int getVl()
    {
        return vl;
    }

    @NotNull
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}