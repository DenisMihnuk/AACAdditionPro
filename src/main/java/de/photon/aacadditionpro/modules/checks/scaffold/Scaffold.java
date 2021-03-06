package de.photon.aacadditionpro.modules.checks.scaffold;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.world.BlockUtils;
import de.photon.aacadditionpro.util.world.LocationUtils;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Set;

public class Scaffold implements ListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 80L);

    private final AnglePattern anglePattern = new AnglePattern();
    private final AveragePattern averagePattern = new AveragePattern();
    private final PositionPattern positionPattern = new PositionPattern();
    private final RotationTypeOnePattern rotationTypeOne = new RotationTypeOnePattern();
    private final RotationTypeTwoPattern rotationTypeTwo = new RotationTypeTwoPattern();
    private final RotationTypeThreePattern rotationTypeThree = new RotationTypeThreePattern();
    private final SprintingPattern sprintingPattern = new SprintingPattern();
    private final SafewalkTypeOnePattern safewalkTypeOne = new SafewalkTypeOnePattern();
    private final SafewalkTypeTwoPattern safewalkTypeTwo = new SafewalkTypeTwoPattern();

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".parts.rotation.violation_threshold")
    private int rotationThreshold;

    // ------------------------------------------- BlockPlace Handling ---------------------------------------------- //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreBlockPlace(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // To prevent too fast scaffolding -> Timeout
        if (user.getScaffoldData().recentlyUpdated(0, timeout)) {
            event.setCancelled(true);
            InventoryUtils.syncUpdateInventory(user.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        final Block blockPlaced = event.getBlockPlaced();

        // Short distance between player and the block (at most 2 Blocks)
        if (LocationUtils.areLocationsInRange(user.getPlayer().getLocation(), blockPlaced.getLocation(), 4D) &&
            // Not flying
            !user.getPlayer().isFlying() &&
            // Above the block
            user.getPlayer().getLocation().getY() > blockPlaced.getY() &&
            // Check if this check applies to the block
            blockPlaced.getType().isSolid() &&
            // Check if the block is placed against one block face only, also implies no blocks above and below.
            // Only one block that is not a liquid is allowed (the one which the Block is placed against).
            BlockUtils.getBlocksAround(blockPlaced, false).stream().filter(block -> !BlockUtils.LIQUIDS.contains(block.getType())).count() == 1 &&
            // In between check to make sure it is somewhat a scaffold movement as the buffering does not work.
            BlockUtils.HORIZONTAL_FACES.contains(event.getBlock().getFace(event.getBlockAgainst())))
        {
            int vl = anglePattern.apply(user, event);
            vl += averagePattern.apply(user, event);
            vl += positionPattern.apply(user, event);

            // --------------------------------------------- Rotations ---------------------------------------------- //

            final float[] angleInformation = user.getLookPacketData().getAngleInformation();

            int rotationVl = rotationTypeOne.apply(user, event) +
                             rotationTypeTwo.apply(user, angleInformation[0]) +
                             rotationTypeThree.apply(user, angleInformation[1]);

            if (rotationVl > 0) {
                if (++user.getScaffoldData().rotationFails >= this.rotationThreshold) {
                    // Flag the player
                    vl += rotationVl;
                }
            } else if (user.getScaffoldData().rotationFails > 0) {
                user.getScaffoldData().rotationFails--;
            }

            vl += sprintingPattern.apply(user, event);
            vl += safewalkTypeOne.apply(user, event);
            vl += safewalkTypeTwo.apply(user, event);

            if (vl > 0) {
                vlManager.flag(event.getPlayer(), vl, cancelVl, () ->
                {
                    event.setCancelled(true);
                    user.getScaffoldData().updateTimeStamp(0);
                    InventoryUtils.syncUpdateInventory(user.getPlayer());
                }, () -> {});
            }
        }
    }

    @Override
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(anglePattern,
                               averagePattern,
                               positionPattern,
                               rotationTypeOne,
                               rotationTypeTwo,
                               rotationTypeThree,
                               sprintingPattern,
                               safewalkTypeOne,
                               safewalkTypeTwo);
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}