package sweda.cnpc_immersiveboss.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import noppes.npcs.api.entity.ICustomNpc;
import noppes.npcs.api.event.NpcEvent;

/**
 * Fired when an NPC takes damage to a specific hitbox bone.
 * Extends NpcEvent.DamagedEvent so scripts can use the same handler
 * and check e.hitboxName to determine which body part was hit.
 *
 * In CNPC scripts:
 *   function damaged(e) {
 *     if (e.hitboxName == "hb_head") {
 *       // headshot logic
 *     }
 *   }
 */
public class HitboxDamagedEvent extends NpcEvent.DamagedEvent {
    /** The bone name of the hitbox that was struck (e.g. "hb_head", "hadb_upbody"). */
    public final String hitboxName;
    /** The source entity that dealt the damage, if applicable. */
    public final ICustomNpc source;

    public HitboxDamagedEvent(ICustomNpc npc, Entity damageSourceEntity, DamageSource damageSource, float damage,
                              ICustomNpc source, String hitboxName) {
        super(npc, damageSourceEntity, damage, damageSource);
        this.source = source;
        this.hitboxName = hitboxName;
    }
}
