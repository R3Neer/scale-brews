from pathlib import Path
p=Path('src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialPhysicsRuntime.java')
t=p.read_text()
old='''        private Plan planCandidate(Entity body,AABB captured,List<MaterialEventDispatcher.Event<Entity>> events,
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
'''
new='''        private Plan planCandidate(Entity body,AABB captured,List<MaterialEventDispatcher.Event<Entity>> events,
                Map<GeometryProvider.MotionIntervalHandle,GeometryProvider.MotionSnapshot> motions,
                Map<String,ConservativeSweep.Motion> pieces) {
            var retained=AnatomyMovement.contact(body);
            MaterialEventDispatcher.Event<Entity> retainedEvent=null;
            if(retained!=null)for(var event:events)if(event.support()==retained.support()
                    && event.support() instanceof LivingEntity support
                    && AnatomyRuntime.acceptsIntervalIdentity(support,event.interval().handle())) {
                if(retainedEvent!=null) {retainedEvent=null;break;}
                retainedEvent=event;
            }
            // The generic backend is also used by isolated kernel/holdout fixtures. Only a fully
            // live S06 identity may switch an existing retained relation into anchored carry.
            if(retainedEvent==null)return plan(body,captured,pieces);

            var anchored=AnchoredTransportPlanner.plan(level,body,captured,events,motions,clip(body));
            if(anchored.status()==AnchoredTransportPlanner.Status.COMPLETE)
                return new Plan(body,anchored.displacement(),anchored.evaluations(),Set.of(),anchored.evidence(),Set.of());
            if(anchored.status()==AnchoredTransportPlanner.Status.NOT_APPLICABLE)return plan(body,captured,pieces);

            var forbidden=retained.support();AnatomyMovement.clear(body);
            var remaining=new TreeMap<String,ConservativeSweep.Motion>();
            for(var event:events) {
                if(event.support()==forbidden)continue;
                String prefix=prefix(event);
                for(var entry:pieces.entrySet())if(entry.getKey().startsWith(prefix))remaining.put(entry.getKey(),entry.getValue());
            }
            if(remaining.isEmpty())return new Plan(body,Vec3.ZERO,anchored.evaluations(),Set.of(),null,Set.of(forbidden));
            var planned=plan(body,captured,remaining);
            return planned==null?null:planned.withForbidden(forbidden);
        }
'''
if t.count(old)!=1: raise SystemExit(f'planCandidate match count={t.count(old)}')
p.write_text(t.replace(old,new,1))
