from pathlib import Path


def replace1(text, old, new, label):
    n=text.count(old)
    if n!=1:
        raise SystemExit(f"{label}: expected 1 match, got {n}")
    return text.replace(old,new,1)

p=Path('src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialPhysicsRuntime.java')
t=p.read_text()
t=replace1(t,
'''    private record Plan(Entity body,Vec3 displacement,int evaluations,Set<String> contactPieces) {
        private Plan {contactPieces=Set.copyOf(contactPieces);}
    }
''',
'''    private record Plan(Entity body,Vec3 displacement,int evaluations,Set<String> contactPieces,
            AnchoredTransportPlanner.Evidence transport,Set<LivingEntity> forbiddenSupports) {
        private Plan {
            contactPieces=Set.copyOf(contactPieces);forbiddenSupports=Set.copyOf(forbiddenSupports);
        }
        private Plan(Entity body,Vec3 displacement,int evaluations,Set<String> contactPieces) {
            this(body,displacement,evaluations,contactPieces,null,Set.of());
        }
        private Plan withForbidden(LivingEntity support) {
            var forbidden=new java.util.LinkedHashSet<>(forbiddenSupports);forbidden.add(support);
            return new Plan(body,displacement,evaluations,contactPieces,transport,forbidden);
        }
    }
''','plan record')

t=replace1(t,
'''            var pieces=scopedPieces(event,motion);
            var outcome=resolveAll(List.of(event),pieces,candidates);
            return new MaterialEventDispatcher.Resolution<>(outcome,List.of());
''',
'''            var pieces=scopedPieces(event,motion);
            var outcome=resolveAll(List.of(event),pieces,Map.of(event.interval().handle(),motion),candidates);
            return new MaterialEventDispatcher.Resolution<>(outcome,List.of());
''','single resolve')

t=replace1(t,
'''                if(pieces.isEmpty()) {plans.add(new Plan(body,Vec3.ZERO,0,Set.of()));continue;}
                var plan=plan(body,candidate.bounds(),pieces);
                if(plan==null) {
''',
'''                var plan=planCandidate(body,candidate.bounds(),events,motions,pieces);
                if(plan==null) {
''','joint plan candidate')

t=replace1(t,
'''        private MaterialEventDispatcher.Outcome resolveAll(List<MaterialEventDispatcher.Event<Entity>> events,
                Map<String,ConservativeSweep.Motion> pieces,List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var plans=new ArrayList<Plan>();int evaluations=0;
            for(var candidate:candidates) {
                var body=candidate.body();
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent())
                    return fail(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                var plan=plan(body,candidate.bounds(),pieces);
                if(plan==null) {
''',
'''        private MaterialEventDispatcher.Outcome resolveAll(List<MaterialEventDispatcher.Event<Entity>> events,
                Map<String,ConservativeSweep.Motion> pieces,
                Map<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot> motions,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var plans=new ArrayList<Plan>();int evaluations=0;
            for(var candidate:candidates) {
                var body=candidate.body();
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent())
                    return fail(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                var plan=planCandidate(body,candidate.bounds(),events,motions,pieces);
                if(plan==null) {
''','resolveAll signature')

marker='''        private Plan plan(Entity body,AABB captured,Map<String,ConservativeSweep.Motion> pieces) {
'''
insert='''        private Plan planCandidate(Entity body,AABB captured,List<MaterialEventDispatcher.Event<Entity>> events,
                Map<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot> motions,
                Map<String,ConservativeSweep.Motion> pieces) {
            var retained=AnatomyMovement.contact(body);
            var anchored=AnchoredTransportPlanner.plan(level,body,captured,events,motions,clip(body));
            if(anchored.status()==AnchoredTransportPlanner.Status.COMPLETE)
                return new Plan(body,anchored.displacement(),anchored.evaluations(),Set.of(),anchored.evidence(),Set.of());
            LivingEntity forbidden=null;
            if(anchored.status()==AnchoredTransportPlanner.Status.RELEASE && retained!=null) {
                forbidden=retained.support();AnatomyMovement.clear(body);
            }
            if(pieces.isEmpty())return new Plan(body,Vec3.ZERO,anchored.evaluations(),Set.of(),null,
                forbidden==null?Set.of():Set.of(forbidden));
            var planned=plan(body,captured,pieces);
            return planned==null || forbidden==null?planned:planned.withForbidden(forbidden);
        }
'''+marker
t=replace1(t,marker,insert,'planCandidate insertion')

t=replace1(t,
'''        private void apply(List<Plan> plans,List<MaterialEventDispatcher.Event<Entity>> events) {
            for(var plan:plans) {
                if(plan.displacement().lengthSqr()>1e-20)plan.body().setPos(plan.body().position().add(plan.displacement()));
                AnatomyMovement.afterMove(plan.body());
                if(AnatomyMovement.contact(plan.body())!=null)continue;
                for(var event:events) {
                    if(!(event.support() instanceof LivingEntity support) || !Platforms.eligible(plan.body(),support))continue;
''',
'''        private void apply(List<Plan> plans,List<MaterialEventDispatcher.Event<Entity>> events) {
            for(var plan:plans) {
                if(plan.displacement().lengthSqr()>1e-20) {
                    plan.body().setPos(plan.body().position().add(plan.displacement()));
                    if(plan.transport()!=null) {
                        var evidence=plan.transport();
                        if(!AnatomyMovement.recordCertifiedTransport(plan.body(),evidence.support(),evidence.surface(),evidence.root(),
                                plan.displacement(),evidence.materialBefore(),evidence.materialAfter()))
                            AnatomyMovement.clear(plan.body());
                    }
                }
                AnatomyMovement.afterMove(plan.body());
                if(AnatomyMovement.contact(plan.body())!=null)continue;
                for(var event:events) {
                    if(!(event.support() instanceof LivingEntity support) || !Platforms.eligible(plan.body(),support)
                            || plan.forbiddenSupports().contains(support))continue;
''','apply transport')
p.write_text(t)

p=Path('src/main/java/io/github/r3neer/scalebrews/collision/internal/AnatomyMovement.java')
t=p.read_text()
marker='''    public static void carry(Entity body){carry(body,Collections.newSetFromMap(new IdentityHashMap<>()));}
'''
insert='''    /** Record one displacement already certified/applied by the S08 material dispatcher. */
    static boolean recordCertifiedTransport(Entity body,LivingEntity support,SurfaceContact surface,RootFrame root,
            Vec3 applied,ConvexBox materialBefore,ConvexBox materialAfter) {
        var contact=contact(body);
        if(body==null || support==null || surface==null || root==null || applied==null || materialBefore==null || materialAfter==null
                || contact==null || contact.support()!=support || !surface.support().equals(support.getUUID())
                || contact.revision()!=surface.revision() || !contact.piece().equals(surface.piece())
                || !Double.isFinite(applied.lengthSqr()))return false;
        if(applied.lengthSqr()<=1e-20)return true;
        positionPassengers(body);
        if(body instanceof net.minecraft.server.level.ServerPlayer player)
            ((io.github.r3neer.scalebrews.platform.PlatformConnection)player.connection).scalebrews$transportBaseline(body,applied);
        for(var passenger:body.getIndirectPassengers())
            if(passenger instanceof net.minecraft.server.level.ServerPlayer player)
                ((io.github.r3neer.scalebrews.platform.PlatformConnection)player.connection).scalebrews$transportBaseline(body,applied);
        var before=TRANSPORT.get(body);long tick=body.level().getGameTime();
        var transport=new SupportTransport(tick,before==null?1:before.sequence()+1,root.sequence(),
            before!=null && before.tick()==tick?before.displacement().add(applied):applied,applied);
        TRANSPORT.put(body,transport);rememberTransport(body,transport);
        AnatomyTransportReceipts.record(body,contact,surface,root,transport,materialBefore,materialAfter);
        return true;
    }
'''+marker
t=replace1(t,marker,insert,'record transport insertion')
t=replace1(t,
'''        var c=contact(body);var anchor=ANCHORS.get(body);
        if(c==null || anchor==null)return;
''',
'''        var c=contact(body);var anchor=ANCHORS.get(body);
        if(c==null || anchor==null)return;
        // Server runtime material intervals own carry. Keeping this endpoint path active in
        // parallel would double-apply ROOT/JOINT work before the causal dispatcher drains it.
        if(!body.level().isClientSide() && AnatomyRuntime.owns(c.support()))return;
''','legacy carry gate')
p.write_text(t)
