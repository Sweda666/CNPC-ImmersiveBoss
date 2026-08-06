package sweda.cnpc_immersiveboss.api;

import net.minecraft.world.entity.Entity;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.event.NpcEvent;

/**
 * Extended collide event that includes the names of both hitboxes involved in the collision.
 * Fired from EntityCollisionListener when OBB-based hitbox overlap is detected.
 *
 * In CNPC scripts, accessible as:
 * - e.npc          (ICustomNpc)  — the NPC
 * - e.entity       (IEntity)     — the other entity
 * - e.hitboxAName  (String)      — NPC's hitbox bone name (e.g. "hb_head")
 * - e.hitboxBName  (String)      — other entity's hitbox bone name (or "AABB" if vanilla)
 */
public class HitboxCollideEvent extends NpcEvent.CollideEvent {
    public final String hitboxAName;
    public final String hitboxBName;

    public HitboxCollideEvent(ICustomNpc npc, Entity entity, String hitboxAName, String hitboxBName) {
        super(npc, entity);
        this.hitboxAName = hitboxAName;
        this.hitboxBName = hitboxBName;
    }
}
