package sweda.cnpc_immersiveboss.api;

/** Script-facing turn-speed controls mixed into CNPC's NPC wrapper. */
public interface INpcTurnControl {
    boolean setTurnSpeedLimit(float degreesPerTick);

    float getTurnSpeedLimit();

    boolean hasTurnSpeedLimit();

    boolean setTurnSpeedLimitEnabled(boolean enabled);

    boolean clearTurnSpeedLimit();

    float getMinimumNavigationSpeedScale();

    boolean setMinimumNavigationSpeedScale(float scale);

    void setRotationImmediate(float rotation);
}
