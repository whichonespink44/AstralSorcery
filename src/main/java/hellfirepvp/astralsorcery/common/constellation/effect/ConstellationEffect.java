package hellfirepvp.astralsorcery.common.constellation.effect;

import hellfirepvp.astralsorcery.common.constellation.IMajorConstellation;
import hellfirepvp.astralsorcery.common.constellation.IMinorConstellation;
import hellfirepvp.astralsorcery.common.data.config.entry.ConfigEntry;
import hellfirepvp.astralsorcery.common.tile.TileRitualPedestal;
import hellfirepvp.astralsorcery.common.util.MiscUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ConstellationEffect
 * Created by HellFirePvP
 * Date: 01.10.2016 / 15:47
 */
public abstract class ConstellationEffect extends ConfigEntry {

    protected static final Random rand = new Random();

    private final IMajorConstellation constellation;

    public ConstellationEffect(IMajorConstellation constellation, String cfgName) {
        super(Section.RITUAL_EFFECTS, cfgName);
        this.constellation = constellation;
    }

    @Override
    public String getConfigurationSection() {
        return super.getConfigurationSection() + "." + getKey();
    }

    public IMajorConstellation getConstellation() {
        return constellation;
    }

    public boolean mayExecuteMultipleMain() {
        return false;
    }

    public boolean mayExecuteMultipleTrait() {
        return false;
    }

    //Once per TE client tick
    @SideOnly(Side.CLIENT)
    public void playClientEffect(World world, BlockPos pos, TileRitualPedestal pedestal,  float percEffectVisibility, boolean extendedEffects) {}

    //May be executed multiple times per tick
    //Even if this effect can handle multiple effects per tick, it is still possible that this method is called.
    public abstract boolean playMainEffect(World world, BlockPos pos, float percStrength, boolean mayDoTraitEffect, @Nullable IMinorConstellation possibleTraitEffect);

    //May be executed multiple times per tick
    public abstract boolean playTraitEffect(World world, BlockPos pos, IMinorConstellation traitType, float traitStrength);

    //Should handle multiple executions at once ('times' executions)
    public boolean playMainEffectMultiple(World world, BlockPos pos, int times, boolean mayDoTraitEffect, @Nullable IMinorConstellation possibleTraitEffect) {
        boolean changed = false;
        for (int i = 0; i < times; i++) {
            if(playMainEffect(world, pos, 1, mayDoTraitEffect, possibleTraitEffect)) changed = true;
        }
        return changed;
    }

    public boolean playTraitEffectMultiple(World world, BlockPos pos, IMinorConstellation traitType, int times) {
        boolean changed = false;
        for (int i = 0; i < times; i++) {
            if(playTraitEffect(world, pos, traitType, 1)) changed = true;
        }
        return changed;
    }

    @Nullable
    public TileRitualPedestal getPedestal(World world, BlockPos pos) {
        return MiscUtils.getTileAt(world, pos, TileRitualPedestal.class, false);
    }

    public void readFromNBT(NBTTagCompound cmp) {}

    public void writeToNBT(NBTTagCompound cmp) {}

    @Nullable
    public EntityPlayer getOwningPlayerInWorld(World world, BlockPos pos) {
        TileRitualPedestal pedestal = getPedestal(world, pos);
        if(pedestal != null) {
            return pedestal.getOwningPlayerInWorld(world);
        }
        return null;
    }

}
