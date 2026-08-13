package sweda.cnpc_immersiveboss.api;

/** Accessor for the {@code hitboxName} field mixed into {@code NpcEvent.InteractEvent}. */
public interface IMixinNpcInteractEvent {
    String getHitboxName();
    void setHitboxName(String name);
}
