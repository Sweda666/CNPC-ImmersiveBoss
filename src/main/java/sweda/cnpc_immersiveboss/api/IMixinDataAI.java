package sweda.cnpc_immersiveboss.api;

/** Shared rotation-limit configuration stored on CNPC's DataAI object. */
public interface IMixinDataAI {
    boolean cnpc_immersiveboss$isRotationLimitEnabled();

    void cnpc_immersiveboss$setRotationLimitEnabled(boolean enabled);

    float cnpc_immersiveboss$getRotationSpeed();

    void cnpc_immersiveboss$setRotationSpeed(float degreesPerTick);

    float cnpc_immersiveboss$getMinimumNavigationSpeedScale();

    void cnpc_immersiveboss$setMinimumNavigationSpeedScale(float scale);
}
