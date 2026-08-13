package sweda.cnpc_immersiveboss.api;

/** Short-lived context shared by multiple hurt calls belonging to one projectile hit. */
public interface IHitboxDamageContext {
    String cnpc_immersiveboss$getActiveHitboxName();

    boolean cnpc_immersiveboss$hasDamageEventResult();

    boolean cnpc_immersiveboss$getDamageEventResult();

    void cnpc_immersiveboss$setDamageEventResult(boolean result);
}
