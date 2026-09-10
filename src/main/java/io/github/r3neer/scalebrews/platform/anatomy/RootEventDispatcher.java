package io.github.r3neer.scalebrews.platform.anatomy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;

/**
 * Delimits real root-transform mutations.  Mixins will supply exact before and
 * after frames later; this helper deliberately never reads a live entity itself.
 */
public final class RootEventDispatcher {
    public static final class GateViolation extends IllegalStateException {public GateViolation(String message){super(message);}}
    public enum Source {ENTITY_MOVE,DIRECT_SET_POS,REFRESH_DIMENSIONS,BODY_YAW,DISPATCH_APPLY}
    public record Mutation(Object support,Source source,AnatomyMovement.RootFrame before,AnatomyMovement.RootFrame after) {
        public Mutation {
            Objects.requireNonNull(support);Objects.requireNonNull(source);Objects.requireNonNull(before);Objects.requireNonNull(after);
        }
        public boolean changed() {return !before.equals(after);}
    }
    public final class Scope {
        private final Object support;
        private final Source source;
        private final AnatomyMovement.RootFrame before;
        private final boolean outer;
        private boolean closed,cancelled;
        private Scope(Object support,Source source,AnatomyMovement.RootFrame before,boolean outer) {
            this.support=support;this.source=source;this.before=before;this.outer=outer;
        }
        public boolean outer(){return outer;}
    }
    private final Deque<Scope> scopes=new ArrayDeque<>();

    /** Starts a mutation boundary. Nested setters never manufacture an extra segment. */
    public Scope begin(Object support,Source source,AnatomyMovement.RootFrame before) {
        Objects.requireNonNull(support);Objects.requireNonNull(source);Objects.requireNonNull(before);
        boolean sameOwner=scopes.stream().anyMatch(open->open.support==support);
        if(!scopes.isEmpty() && !sameOwner && source!=Source.DISPATCH_APPLY)
            throw new GateViolation("A different root mutation attempted to enter an active scope");
        // Dispatcher-owned carry reports a derived event only after confirmed setPos;
        // it must never look like an independent root mutation scope.
        var scope=new Scope(support,source,before,!sameOwner && source!=Source.DISPATCH_APPLY);scopes.push(scope);return scope;
    }
    /** Finishes one boundary and returns only its outer, materially changed mutation. */
    public Optional<Mutation> finish(Scope scope,AnatomyMovement.RootFrame after) {
        Objects.requireNonNull(scope);Objects.requireNonNull(after);
        if(scope.closed)throw new IllegalStateException("Root mutation scope already finished");
        if(scope.cancelled) {scope.closed=true;return Optional.empty();}
        if(scopes.peek()!=scope)throw new IllegalStateException("Root mutation scopes must finish in LIFO order");
        scopes.pop();scope.closed=true;
        if(!scope.outer || !new Mutation(scope.support,scope.source,scope.before,after).changed())return Optional.empty();
        return Optional.of(new Mutation(scope.support,scope.source,scope.before,after));
    }
    /** A lifecycle discontinuity cancels open scopes so their finally blocks remain harmless. */
    public void invalidate() {for(var scope:scopes)scope.cancelled=true;scopes.clear();}
    public boolean scoped(){return !scopes.isEmpty();}
}
