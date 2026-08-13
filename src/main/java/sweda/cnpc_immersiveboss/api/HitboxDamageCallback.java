package sweda.cnpc_immersiveboss.api;

import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;

/** Nashorn-compatible callback invoked after successful collision damage. */
@FunctionalInterface
public interface HitboxDamageCallback {
    void onDamage(ICustomNpc attacker, IEntity target);
}
