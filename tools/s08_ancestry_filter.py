from pathlib import Path
p=Path('src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialPhysicsRuntime.java')
t=p.read_text()

def r(old,new,label):
    global t
    n=t.count(old)
    if n!=1: raise SystemExit(f'{label}: expected 1 match, got {n}')
    t=t.replace(old,new,1)

r('''        @Override public MaterialEventDispatcher.Candidates<Entity> capture(MaterialEventDispatcher.Event<Entity> event,int maximumBodies) {
            if(!(event.support() instanceof LivingEntity support))return new MaterialEventDispatcher.Candidates<>(List.of(),true);
            return capture(event.interval().envelope(),List.of(support),maximumBodies);
        }
''','''        @Override public MaterialEventDispatcher.Candidates<Entity> capture(MaterialEventDispatcher.Event<Entity> event,int maximumBodies) {
            if(!(event.support() instanceof LivingEntity support))return new MaterialEventDispatcher.Candidates<>(List.of(),true);
            return capture(event.interval().envelope(),List.of(support),event.ancestry(),maximumBodies);
        }
''','single capture ancestry')

r('''                var local=capture(envelope,List.of(support),maximumBodies);
''','''                var local=capture(envelope,List.of(support),event.ancestry(),maximumBodies);
''','joint local ancestry')

r('''        private MaterialEventDispatcher.Candidates<Entity> capture(AABB envelope,List<LivingEntity> supports,int maximumBodies) {
            var entities=new ArrayList<Entity>(Math.min(maximumBodies+1,256));
            level.getEntities(EntityTypeTest.forClass(Entity.class),envelope,body->{
                if(body.isRemoved())return false;
''','''        private MaterialEventDispatcher.Candidates<Entity> capture(AABB envelope,List<LivingEntity> supports,Set<Object> ancestry,int maximumBodies) {
            var entities=new ArrayList<Entity>(Math.min(maximumBodies+1,256));
            level.getEntities(EntityTypeTest.forClass(Entity.class),envelope,body->{
                if(body.isRemoved() || ancestry.contains(body.getUUID()))return false;
''','capture helper ancestry')

p.write_text(t)
