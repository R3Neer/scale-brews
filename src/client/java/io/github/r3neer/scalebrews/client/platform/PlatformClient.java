package io.github.r3neer.scalebrews.client.platform;

import io.github.r3neer.scalebrews.platform.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.Map;

/** Server confirmations; local movement remains predicted with the shared geometry. */
public final class PlatformClient {
    private static final Map<Integer,PlatformPayload> PENDING=new HashMap<>();
    private static Object world;
    private PlatformClient() {}
    public static void initialize() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents.ENTITY_LOAD.register((e,level)->Platforms.noteSupport(e));
        ClientPlayNetworking.registerGlobalReceiver(PlatformPayload.TYPE,(packet,context)->{
            context.client().execute(()->{
                if(world!=context.client().level) { PENDING.clear(); world=context.client().level; }
                PENDING.merge(packet.body(),packet,(old,next)->next.sequence()>=old.sequence()?next:old);
                apply(context.client());
            });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(world!=client.level) { PENDING.clear(); world=client.level; PlatformVisuals.reset(); }
            if(client.level==null) return;
            apply(client);
            for(var e:client.level.entitiesForRendering()) {
                Platforms.noteSupport(e);
                if(e.isLocalInstanceAuthoritative() && Platforms.state(e).support!=null) PlatformPhysics.carry(e);
            }
        });
    }
    private static void apply(Minecraft client) {
        if(client.level==null) return;
        var it=PENDING.values().iterator();
        while(it.hasNext()) {
            var p=it.next(); var body=client.level.getEntity(p.body());
            if(body==null) { if(client.level.getGameTime()-p.sequence()>100) it.remove(); continue; }
            var s=Platforms.state(body);
            if(p.sequence()<s.sequence) { it.remove(); continue; }
            if(p.support()<0) { s.clear(); s.sequence=p.sequence(); it.remove(); continue; }
            if(!(client.level.getEntity(p.support()) instanceof LivingEntity support)) {
                if(client.level.getGameTime()-p.sequence()>100) it.remove();
                continue;
            }
            var d=Platforms.definition(support);
            if(d==null) { it.remove(); continue; }
            var surface=d.surfaces().stream().filter(v->v.id().equals(p.surface())).findFirst().orElse(null);
            if(surface==null || !Platforms.eligible(body,support)) { s.clear(); it.remove(); continue; }
            if(s.support!=support || s.surface!=surface || !body.isLocalInstanceAuthoritative()) {
                s.support=support; s.surface=surface; s.frame=PlatformGeometry.frame(support);
                s.contact=body.isLocalInstanceAuthoritative()?s.frame.local(body.position()):p.contact();
            }
            s.sequence=p.sequence(); it.remove();
        }
    }
}
