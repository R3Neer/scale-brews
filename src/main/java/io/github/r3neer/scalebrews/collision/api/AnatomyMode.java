package io.github.r3neer.scalebrews.collision.api;

/**
 * Level/session ownership state, deliberately independent of a body's own
 * model binding. A player, boat or passenger can be owned while only a cow is
 * an exported support candidate.
 */
public enum AnatomyMode {
    /** Scale has no shared-anatomy ownership for this level/session. */
    DISABLED,
    /** Shared anatomy owns the route but catalog/session data is not usable yet. Fail closed. */
    BINDING,
    /** Catalog/session data is accepted; unsupported species/poses still return empty geometry. */
    READY
}
