from pathlib import Path
p=Path('src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialPhysicsRuntime.java')
t=p.read_text()
old='''            if(anchored.status()==AnchoredTransportPlanner.Status.COMPLETE)
                return new Plan(body,anchored.displacement(),anchored.evaluations(),Set.of(),anchored.evidence(),Set.of());
            if(anchored.status()==AnchoredTransportPlanner.Status.NOT_APPLICABLE)return plan(body,captured,pieces);

            var forbidden=retained.support();AnatomyMovement.clear(body);
'''
new='''            if(anchored.status()==AnchoredTransportPlanner.Status.COMPLETE)
                return new Plan(body,anchored.displacement(),anchored.evaluations(),Set.of(),anchored.evidence(),Set.of());
            if(anchored.status()==AnchoredTransportPlanner.Status.NOT_APPLICABLE)return plan(body,captured,pieces);
            if(anchored.status()==AnchoredTransportPlanner.Status.EXHAUSTED) {
                suspendUncertainPairs(body,captured,events);
                return null;
            }

            var forbidden=retained.support();AnatomyMovement.clear(body);
'''
if t.count(old)!=1: raise SystemExit(f'exhaustion gate match count={t.count(old)}')
p.write_text(t.replace(old,new,1))
