package sweda.cnpc_immersiveboss.api;

import sweda.cnpc_immersiveboss.hitbox.OBB;
import java.util.Map;

/** Per-entity OBB access and hitbox damage tracking. */
public interface IOBBHolder {
    Map<String, OBB> cnpc_immersiveboss$getBoneOBBs();
    void cnpc_immersiveboss$setBoneOBBs(Map<String, OBB> obbs);

    /** Sets the bone name of the last hitbox that was struck (used for damage attribution). */
    void cnpc_immersiveboss$setLastHitboxName(String name);
    /** Returns the bone name of the last hitbox struck, or null if none. */
    String cnpc_immersiveboss$getLastHitboxName();
}
