package sweda.cnpc_immersiveboss.api;

/** Script-facing collision-damage methods mixed into CNPC's NPC wrapper. */
public interface IHitboxDamageNpc {
    boolean activateHitboxDamage(String hitboxName, int startDelayTicks);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 HitboxDamageCallback callback);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, HitboxDamageCallback callback);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, float damage);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, float damage,
                                 HitboxDamageCallback callback);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, float damage, int repeatCount);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, float damage, int repeatCount,
                                 HitboxDamageCallback callback);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, float damage, int repeatCount,
                                 int repeatIntervalTicks);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, float damage, int repeatCount,
                                 int repeatIntervalTicks, HitboxDamageCallback callback);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, float damage, int repeatCount,
                                 int repeatIntervalTicks, int maxTargets);

    boolean activateHitboxDamage(String hitboxName, int startDelayTicks,
                                 int durationTicks, float damage, int repeatCount,
                                 int repeatIntervalTicks, int maxTargets,
                                 HitboxDamageCallback callback);

    boolean cancelHitboxDamageWindow(String hitboxName);

    int cancelAllHitboxDamageWindows();

    boolean isHitboxDamageWindowActive(String hitboxName);

    int getHitboxDamageWindowRemainingTicks(String hitboxName);

    String[] getActiveHitboxDamageWindows();
}
