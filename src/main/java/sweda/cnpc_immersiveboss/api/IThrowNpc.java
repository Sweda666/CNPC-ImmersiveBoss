package sweda.cnpc_immersiveboss.api;

import noppes.npcs.api.entity.IEntity;

/** Script-facing throw-animation shortcuts mixed into CNPC's NPC wrapper. */
public interface IThrowNpc {
    /** Starts a throw against a player using the NPC's GeckoLib animation. */
    boolean startThrow(IEntity target, String animation, int durationTicks);
    boolean startThrow(IEntity target, String animation, int durationTicks, int struggleMode);
    boolean startThrow(IEntity target, String animation, int durationTicks, int struggleMode, int difficulty);
    boolean startThrow(IEntity target, String animation, int durationTicks, int struggleMode, int difficulty,
                       boolean returnToStart);
    boolean startThrow(IEntity target, String animation, int durationTicks, boolean returnToStart,
                       int struggleMode, int difficulty);
    boolean startThrow(IEntity target, String animation, int durationTicks, int struggleMode,
                       int difficulty, ThrowCallback onEscape);
    boolean startThrow(IEntity target, String animation, int durationTicks, int struggleMode,
                       int difficulty, ThrowCallback onEscape, ThrowCallback onFinish);
    boolean startThrow(IEntity target, String animation, int durationTicks, int struggleMode,
                       int difficulty, boolean returnToStart, ThrowCallback onEscape,
                       ThrowCallback onFinish);

    boolean startThrow(IEntity target, String animation, int durationTicks, String struggleMode);

    boolean startThrow(IEntity target, String animation, int durationTicks, String struggleMode,
                       Integer difficulty);

    boolean startThrow(IEntity target, String animation, int durationTicks, String struggleMode,
                       Integer difficulty, ThrowCallback onEscape);

    boolean startThrow(IEntity target, String animation, int durationTicks, String struggleMode,
                       Integer difficulty, ThrowCallback onEscape, ThrowCallback onFinish);
    boolean startThrow(IEntity target, String animation, int durationTicks, String struggleMode,
                       Integer difficulty, boolean returnToStart, ThrowCallback onEscape,
                       ThrowCallback onFinish);
    boolean startThrow(IEntity target, String animation, int durationTicks, boolean returnToStart,
                       int struggleMode, Integer difficulty, ThrowCallback onEscape,
                       ThrowCallback onFinish);

    /** Stops the throw currently controlled by the supplied player. */
    boolean stopThrow(IEntity target);

    /** Returns whether the supplied player is currently controlled by a throw. */
    boolean isThrowActive(IEntity target);
}
