package sweda.cnpc_immersiveboss.api;

/** Runtime turn-limiter state stored directly on a mixed-in CNPC entity. */
public interface INpcTurnState {
    boolean cnpc_immersiveboss$isTurnStateInitialized();

    void cnpc_immersiveboss$initializeTurnState(float entityYaw, float bodyYaw, float headYaw);

    float cnpc_immersiveboss$getLimitedEntityYaw();

    float cnpc_immersiveboss$getLimitedBodyYaw();

    float cnpc_immersiveboss$getLimitedHeadYaw();

    void cnpc_immersiveboss$setLimitedYaws(float entityYaw, float bodyYaw, float headYaw);

    int cnpc_immersiveboss$getRearTurnDirection();

    void cnpc_immersiveboss$setRearTurnDirection(int direction);

    long cnpc_immersiveboss$getNavigationTurnTick();

    float cnpc_immersiveboss$getNavigationTargetYaw();

    float cnpc_immersiveboss$getNavigationSpeedScale();

    void cnpc_immersiveboss$recordNavigationTurn(long gameTime, float targetYaw,
                                                  float speedScale);

    long cnpc_immersiveboss$getLastTurnLimitTick();

    void cnpc_immersiveboss$setLastTurnLimitTick(long gameTime);

    boolean cnpc_immersiveboss$hasRequestedRotation();

    float cnpc_immersiveboss$getRequestedRotation();

    void cnpc_immersiveboss$setRequestedRotation(float rotation);

    void cnpc_immersiveboss$clearRequestedRotation();

    void cnpc_immersiveboss$clearTurnState();
}
