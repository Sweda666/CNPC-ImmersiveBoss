package sweda.cnpc_immersiveboss.api;

/** Accessor for the {@code hitboxName} field mixed into {@code NpcEvent.DamagedEvent}. */
public interface IMixinNpcDamagedEvent {
    String getHitboxName();
    void setHitboxName(String name);
}
