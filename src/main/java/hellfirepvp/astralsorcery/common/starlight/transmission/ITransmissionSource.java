package hellfirepvp.astralsorcery.common.starlight.transmission;

import hellfirepvp.astralsorcery.common.starlight.IIndependentStarlightSource;
import hellfirepvp.astralsorcery.common.starlight.IStarlightSource;

/**
 * This class is part of the Astral Sorcery Mod
 * The complete source code for this mod can be found on github.
 * Class: ITransmissionSource
 * Created by HellFirePvP
 * Date: 03.08.2016 / 11:06
 */
public interface ITransmissionSource extends IPrismTransmissionNode {

    public IIndependentStarlightSource provideNewIndependentSource(IStarlightSource source);

}
