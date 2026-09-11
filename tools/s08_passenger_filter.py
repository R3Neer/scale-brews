from pathlib import Path
p=Path('src/main/java/io/github/r3neer/scalebrews/collision/internal/MaterialPhysicsRuntime.java')
t=p.read_text()
old='''            level.getEntities(EntityTypeTest.forClass(Entity.class),envelope,body->{
                if(body.isRemoved() || supports.stream().anyMatch(s->s==body))return false;
                for(var support:supports)if(Platforms.eligible(body,support))return true;
                return false;
            },entities,maximumBodies+1);
'''
new='''            level.getEntities(EntityTypeTest.forClass(Entity.class),envelope,body->{
                if(body.isRemoved())return false;
                for(var support:supports) {
                    if(support==body)return false;
                    for(var passenger:support.getIndirectPassengers())if(passenger==body)return false;
                }
                for(var support:supports)if(Platforms.eligible(body,support))return true;
                return false;
            },entities,maximumBodies+1);
'''
if t.count(old)!=1: raise SystemExit(f'passenger filter match count={t.count(old)}')
p.write_text(t.replace(old,new,1))
