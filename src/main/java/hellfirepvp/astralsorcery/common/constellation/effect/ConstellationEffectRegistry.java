package hellfirepvp.astralsorcery.common.constellation.effect;

import hellfirepvp.astralsorcery.common.constellation.IMajorConstellation;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectAevitas;
import hellfirepvp.astralsorcery.common.constellation.effect.aoe.CEffectDiscidia;
import hellfirepvp.astralsorcery.common.data.config.Config;
import hellfirepvp.astralsorcery.common.event.APIRegistryEvent;
import hellfirepvp.astralsorcery.common.lib.Constellations;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ConstellationEffectRegistry
 * Created by HellFirePvP
 * Date: 01.10.2016 / 15:49
 */
public class ConstellationEffectRegistry {

    private static Map<IMajorConstellation, ConstellationEffectProvider> providerMap = new HashMap<>();
    private static Map<IMajorConstellation, ConstellationEffect> singleRenderInstances = new HashMap<>();

    public static void init() {
        register(Constellations.aevitas,  CEffectAevitas::new);
        register(Constellations.discidia, CEffectDiscidia::new);

        MinecraftForge.EVENT_BUS.post(new APIRegistryEvent.ConstellationEffectRegister());
    }

    public static void addDynamicConfigEntries() {
        Config.addDynamicEntry(new CEffectAevitas());
        Config.addDynamicEntry(new CEffectDiscidia());
    }

    private static void register(IMajorConstellation c, ConstellationEffectProvider provider) {
        providerMap.put(c, provider);
        singleRenderInstances.put(c, provider.provideEffectInstance());
    }

    public static void registerFromAPI(IMajorConstellation c, Function<Void, ConstellationEffect> providerFunc) {
        providerMap.put(c, new APIConstellationProvider(providerFunc));
        singleRenderInstances.put(c, providerFunc.apply(null));
    }

    @Nullable
    public static ConstellationEffect clientRenderInstance(IMajorConstellation c) {
        return singleRenderInstances.get(c);
    }

    @Nullable
    public static ConstellationEffect getEffect(IMajorConstellation c) {
        ConstellationEffectProvider p = providerMap.get(c);
        if(p != null) {
            return p.provideEffectInstance();
        }
        return null;
    }

    public static interface ConstellationEffectProvider {

        public ConstellationEffect provideEffectInstance();

    }

    public static class APIConstellationProvider implements ConstellationEffectProvider {

        private final Function<Void, ConstellationEffect> providerFunc;

        public APIConstellationProvider(Function<Void, ConstellationEffect> providerFunc) {
            this.providerFunc = providerFunc;
        }

        @Override
        public ConstellationEffect provideEffectInstance() {
            return providerFunc.apply(null);
        }
    }

}
