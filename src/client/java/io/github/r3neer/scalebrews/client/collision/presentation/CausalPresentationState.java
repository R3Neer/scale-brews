package io.github.r3neer.scalebrews.client.collision.presentation;

/** One render-state token; the live receiver must revalidate it at each consumption boundary. */
public interface CausalPresentationState {
    CausalModelRender.Token scalebrews$causalToken();
    void scalebrews$causalToken(CausalModelRender.Token token);
    boolean scalebrews$causalSubmitted();
    void scalebrews$causalSubmitted(boolean submitted);
}
