from pathlib import Path
p=Path('src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialPhysicsRuntime.java')
t=p.read_text()

def r(old,new,label):
    global t
    n=t.count(old)
    if n!=1: raise SystemExit(f'{label}: expected 1 match, got {n}')
    t=t.replace(old,new,1)

r('''        private final ServerLevel level;
        private final Map<GeometryProvider.MotionIntervalHandle,Prepared> prepared=new IdentityHashMap<>();
        Backend(ServerLevel level,List<Prepared> values){this.level=level;for(var value:values)prepared.put(value.pending().handle(),value);}
''','''        private final ServerLevel level;
        private final Map<GeometryProvider.MotionIntervalHandle,Prepared> prepared=new IdentityHashMap<>();
        private final Map<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot> derivedMotions=new IdentityHashMap<>();
        private record Applied(List<MaterialEventDispatcher.DerivedCarry<Entity>> derived,Set<MaterialEventDispatcher.EventId> invalidParents) {
            private Applied {derived=List.copyOf(derived);invalidParents=Set.copyOf(invalidParents);}
        }
        Backend(ServerLevel level,List<Prepared> values){this.level=level;for(var value:values)prepared.put(value.pending().handle(),value);}
''','backend fields')

r('''        @Override public MaterialEventDispatcher.Resolution<Entity> resolve(MaterialEventDispatcher.Event<Entity> event,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var motion=motion(event.interval().handle());
            if(motion==null)return new MaterialEventDispatcher.Resolution<>(fail(List.of(event),MaterialEventDispatcher.Reason.BACKEND_FAILURE),List.of());
            var pieces=scopedPieces(event,motion);
            var outcome=resolveAll(List.of(event),pieces,Map.of(event.interval().handle(),motion),candidates);
            return new MaterialEventDispatcher.Resolution<>(outcome,List.of());
        }
''','''        @Override public MaterialEventDispatcher.Resolution<Entity> resolve(MaterialEventDispatcher.Event<Entity> event,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
            var motion=motion(event.interval().handle());
            if(motion==null)return new MaterialEventDispatcher.Resolution<>(fail(List.of(event),MaterialEventDispatcher.Reason.BACKEND_FAILURE),List.of());
            var pieces=scopedPieces(event,motion);
            return resolveAll(List.of(event),pieces,Map.of(event.interval().handle(),motion),candidates);
        }
''','resolve wrapper')

r('''                var body=candidate.body();
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent())
                    return batchFailure(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                var pieces=new TreeMap<String,ConservativeSweep.Motion>();
''','''                var body=candidate.body();
                var pieces=new TreeMap<String,ConservativeSweep.Motion>();
''','joint active veto')

r('''                if(plan==null) {
                    suspendUncertainPairs(body,candidate.bounds(),events);
                    revalidateRetainedContacts(candidates,events);
                    return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
                }
                plans.add(plan);evaluations+=plan.evaluations();
''','''                if(plan==null) {
                    suspendUncertainPairs(body,candidate.bounds(),events);
                    revalidateRetainedContacts(candidates,events);
                    return batchFailure(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
                }
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent() && plan.transport()==null)
                    return batchFailure(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                plans.add(plan);evaluations+=plan.evaluations();
''','joint post-plan active gate')

r('''            apply(plans,events);record(level,events.size(),0,evaluations,0,0);
            var outcomes=new ArrayList<MaterialEventDispatcher.Outcome>(events.size());
            for(int i=0;i<events.size();i++)outcomes.add(MaterialEventDispatcher.Outcome.applied(1));
            return new MaterialEventDispatcher.BatchResolution<>(outcomes,List.of());
''','''            var applied=apply(plans,events);record(level,events.size(),0,evaluations,0,0);
            var outcomes=new ArrayList<MaterialEventDispatcher.Outcome>(events.size());
            for(var event:events)outcomes.add(applied.invalidParents().contains(event.id())
                ?MaterialEventDispatcher.Outcome.applied(1,MaterialEventDispatcher.Reason.INVALID_DERIVATION)
                :MaterialEventDispatcher.Outcome.applied(1));
            return new MaterialEventDispatcher.BatchResolution<>(outcomes,applied.derived());
''','joint apply result')

r('''        private GeometryProvider.MotionSnapshot motion(GeometryProvider.MotionIntervalHandle handle) {
            var value=prepared.get(handle);return value==null?null:value.motion();
        }
''','''        private GeometryProvider.MotionSnapshot motion(GeometryProvider.MotionIntervalHandle handle) {
            var value=prepared.get(handle);return value==null?derivedMotions.get(handle):value.motion();
        }
        private MaterialEventDispatcher.MaterialInterval derivedInterval(LivingEntity support,GeometryProvider.MotionIntervalHandle handle) {
            var motion=AnatomyRuntime.interval(support,handle).orElse(null);if(motion==null)return null;
            var bounds=envelope(motion);if(bounds==null || bounds.getXsize()>MAX_ENVELOPE_SPAN
                    || bounds.getYsize()>MAX_ENVELOPE_SPAN || bounds.getZsize()>MAX_ENVELOPE_SPAN)return null;
            try {
                var interval=new MaterialEventDispatcher.MaterialInterval(handle,bounds);
                derivedMotions.put(handle,motion);return interval;
            } catch(RuntimeException rejected){return null;}
        }
''','motion derived map')

r('''        private MaterialEventDispatcher.Outcome resolveAll(List<MaterialEventDispatcher.Event<Entity>> events,
                Map<String,ConservativeSweep.Motion> pieces,
                Map<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot> motions,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
''','''        private MaterialEventDispatcher.Resolution<Entity> resolveAll(List<MaterialEventDispatcher.Event<Entity>> events,
                Map<String,ConservativeSweep.Motion> pieces,
                Map<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot> motions,
                List<MaterialEventDispatcher.Candidate<Entity>> candidates) {
''','resolveAll signature')

r('''                var body=candidate.body();
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent())
                    return fail(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION);
                var plan=planCandidate(body,candidate.bounds(),events,motions,pieces);
                if(plan==null) {
                    suspendUncertainPairs(body,candidate.bounds(),events);
                    return fail(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
                }
                plans.add(plan);evaluations+=plan.evaluations();
''','''                var body=candidate.body();
                var plan=planCandidate(body,candidate.bounds(),events,motions,pieces);
                if(plan==null) {
                    suspendUncertainPairs(body,candidate.bounds(),events);
                    return new MaterialEventDispatcher.Resolution<>(fail(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED),List.of());
                }
                if(body instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent() && plan.transport()==null)
                    return new MaterialEventDispatcher.Resolution<>(fail(events,MaterialEventDispatcher.Reason.INVALID_DERIVATION),List.of());
                plans.add(plan);evaluations+=plan.evaluations();
''','resolveAll active and failure')

r('''            if(!conflicts.isEmpty()) {
                for(int index:conflicts)suspendUncertainPairs(plans.get(index).body(),candidates.get(index).bounds(),events);
                return fail(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED);
            }
            apply(plans,events);record(level,events.size(),0,evaluations,0,0);
            return MaterialEventDispatcher.Outcome.applied(1);
''','''            if(!conflicts.isEmpty()) {
                for(int index:conflicts)suspendUncertainPairs(plans.get(index).body(),candidates.get(index).bounds(),events);
                return new MaterialEventDispatcher.Resolution<>(fail(events,MaterialEventDispatcher.Reason.BACKEND_EXHAUSTED),List.of());
            }
            var applied=apply(plans,events);record(level,events.size(),0,evaluations,0,0);
            var parent=events.getFirst().id();
            var outcome=applied.invalidParents().contains(parent)
                ?MaterialEventDispatcher.Outcome.applied(1,MaterialEventDispatcher.Reason.INVALID_DERIVATION)
                :MaterialEventDispatcher.Outcome.applied(1);
            return new MaterialEventDispatcher.Resolution<>(outcome,applied.derived());
''','resolveAll end')

old='''        private void apply(List<Plan> plans,List<MaterialEventDispatcher.Event<Entity>> events) {
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
                    var allowed=contactPieces(event,plan.contactPieces());if(allowed.isEmpty())continue;
                    if(establish(plan.body(),support,event.interval().handle().after(),allowed))break;
                }
            }
        }
'''
new='''        private Applied apply(List<Plan> plans,List<MaterialEventDispatcher.Event<Entity>> events) {
            var derived=new ArrayList<MaterialEventDispatcher.DerivedCarry<Entity>>();
            var invalidParents=new java.util.HashSet<MaterialEventDispatcher.EventId>();
            for(var plan:plans) {
                LivingEntity childSupport=null;MaterialIntervalRuntime.RootCapture childBefore=null;
                if(plan.transport()!=null && plan.displacement().lengthSqr()>1e-20
                        && plan.body() instanceof LivingEntity living && AnatomyRuntime.authoritativeFrame(living).isPresent()) {
                    childSupport=living;childBefore=MaterialIntervalRuntime.captureRoot(living);
                }
                if(plan.displacement().lengthSqr()>1e-20) {
                    plan.body().setPos(plan.body().position().add(plan.displacement()));
                    if(plan.transport()!=null) {
                        var evidence=plan.transport();
                        if(!AnatomyMovement.recordCertifiedTransport(plan.body(),evidence.support(),evidence.surface(),evidence.root(),
                                plan.displacement(),evidence.materialBefore(),evidence.materialAfter()))
                            AnatomyMovement.clear(plan.body());
                    }
                }
                if(childSupport!=null) {
                    var parent=plan.transport().parent();
                    var handle=MaterialIntervalRuntime.deriveRoot(childSupport,childBefore).orElse(null);
                    var interval=handle==null?null:derivedInterval(childSupport,handle);
                    if(interval==null) {
                        AnatomyMovement.invalidateSupport(childSupport);invalidParents.add(parent);
                    } else derived.add(new MaterialEventDispatcher.DerivedCarry<>(parent,childSupport,interval));
                }
                AnatomyMovement.afterMove(plan.body());
                if(AnatomyMovement.contact(plan.body())!=null)continue;
                for(var event:events) {
                    if(!(event.support() instanceof LivingEntity support) || !Platforms.eligible(plan.body(),support)
                            || plan.forbiddenSupports().contains(support))continue;
                    var allowed=contactPieces(event,plan.contactPieces());if(allowed.isEmpty())continue;
                    if(establish(plan.body(),support,event.interval().handle().after(),allowed))break;
                }
            }
            return new Applied(derived,invalidParents);
        }
'''
if t.count(old)!=1: raise SystemExit(f'apply: expected 1 match, got {t.count(old)}')
t=t.replace(old,new,1)
p.write_text(t)
