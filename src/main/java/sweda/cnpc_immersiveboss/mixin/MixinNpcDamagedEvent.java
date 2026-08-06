package sweda.cnpc_immersiveboss.mixin;

import noppes.npcs.api.event.NpcEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import sweda.cnpc_immersiveboss.api.IMixinNpcDamagedEvent;

/**
 * Adds {@code hitboxName} to {@link NpcEvent.DamagedEvent} so CNPC scripts
 * can read {@code e.hitboxName} directly in the {@code damaged(e)} handler.
 * <p>
 * In Nashorn/JavaScript, {@code e.hitboxName} resolves to
 * {@code getHitboxName()} — no API call needed.
 */
@Mixin(NpcEvent.DamagedEvent.class)
public class MixinNpcDamagedEvent implements IMixinNpcDamagedEvent {

    @Unique
    private String cnpc_immersiveboss$hitboxName;

    @Override
    public String getHitboxName() {
        return cnpc_immersiveboss$hitboxName;
    }

    @Override
    public void setHitboxName(String name) {
        cnpc_immersiveboss$hitboxName = name;
    }
}
