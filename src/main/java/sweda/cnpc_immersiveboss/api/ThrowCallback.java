package sweda.cnpc_immersiveboss.api;

import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.entity.IEntity;

/** Nashorn-compatible callback, invoked on the server after restoring the player. */
@FunctionalInterface
public interface ThrowCallback {
    void onEnd(ICustomNpc npc, IEntity player);
}
