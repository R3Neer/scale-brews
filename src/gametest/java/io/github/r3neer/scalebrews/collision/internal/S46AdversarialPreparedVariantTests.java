package io.github.r3neer.scalebrews.collision.internal;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import io.github.r3neer.scalebrews.collision.catalog.CollisionBindingCatalog;
import io.github.r3neer.scalebrews.collision.data.*;
import io.github.r3neer.scalebrews.collision.geometry.*;

/** Independent S46 preparation/identity oracles; no runtime variant selection claim. */
public final class S46AdversarialPreparedVariantTests {
 static Identifier id(String p){return Identifier.fromNamespaceAndPath("scalebrews",p);}
 static final Identifier ENTITY=Identifier.fromNamespaceAndPath("minecraft","cow");
 static final Identifier MODEL=id("s46_adversary");
 static CollisionBinding row(Map<String,String> variant){return new CollisionBinding(1,ENTITY,variant,
  new CollisionBinding.Geometry(BuiltInGeometryEngines.MODEL_PART,MODEL,Map.of(),AnatomyFilter.DEFAULT),
  new CollisionBinding.Pose(id("static"),Map.of(),Set.of()),BuiltInRootTransformProviders.ENTITY_ROOT,CollisionPolicy.Patch.EMPTY,Set.of());}
 static CollisionBinding change(CollisionBinding b,CollisionBinding.Geometry g,CollisionBinding.Pose p,CollisionPolicy.Patch policy,Set<String> states){return new CollisionBinding(1,b.entity(),b.variant(),g,p,b.rootTransform(),policy,states);}
 static ModelGeometry model(){return new ModelGeometry(1,"adversarial","1",List.of(new ModelGeometry.Part("root",null,ModelGeometry.values(new Matrix4f()))),List.of(new ModelGeometry.Piece("solid","root",List.of(0d,0d,0d),List.of(1d,1d,1d),null)));}
 static void require(boolean x,String why){if(!x)throw new AssertionError(why);}
 static void rejects(Runnable r,String why){boolean rejected=false;try{r.run();}catch(IllegalArgumentException x){rejected=true;}require(rejected,why);}
 @GameTest public void selectTransferAndMissingMaterial(GameTestHelper h){
  var base=row(Map.of()); var a=row(Map.of("coat","a")); var b=row(Map.of("pose","b"));
  var missing=new CollisionBinding(1,ENTITY,Map.of("coat","missing"),new CollisionBinding.Geometry(base.geometry().engine(),id("absent"),Map.of(),AnatomyFilter.DEFAULT),base.pose(),base.rootTransform(),base.policy(),Set.of());
  var world=new WorldAnatomyCatalog();var accepted=world.replace(Map.of(MODEL.toString(),model()),List.of(b,missing,base,a));
  require(accepted.prepared().size()==3,"All executable selections retained");
  require(accepted.resolve(ENTITY,Map.of("coat","a")).orElseThrow().selection()==a,"Exact canonical variant");
  require(accepted.resolve(ENTITY,Map.of("coat","a","pose","b")).isEmpty(),"Ambiguity cannot default");
  require(accepted.resolve(ENTITY,Map.of("coat","missing")).isEmpty(),"Unavailable more-specific cannot default");
  var wire=new AnatomyCatalogTransfer(); for(var packet:world.preparedPackets(UUID.fromString("12345678-1234-1234-1234-123456789012")))wire.accept(packet);
  require(wire.ready()&&wire.snapshot().prepared().size()==3,"Reconstructed executable population");
  for(var facts:List.of(Map.<String,String>of(),Map.of("coat","a"),Map.of("pose","b"))){var original=accepted.resolve(ENTITY,facts).orElseThrow();var decoded=wire.snapshot().resolve(ENTITY,facts).orElseThrow();require(accepted.bindingIdentity(original).equals(wire.snapshot().bindingIdentity(decoded)),"Content identity roundtrip");}
  var only=world.replace(Map.of(MODEL.toString(),model()),List.of(a));require(only.bindings().isEmpty()&&only.resolve(ENTITY,Map.of("coat","a")).isPresent(),"Variant-only no invented default");
  h.succeed();
 }
 @GameTest public void identityFieldsAndText(GameTestHelper h){
  var base=row(Map.of());var expected=CollisionBindingIdentity.of(base);var variants=new ArrayList<CollisionBinding>();
  variants.add(row(Map.of("coat","a")));
  variants.add(new CollisionBinding(1,Identifier.fromNamespaceAndPath("minecraft","pig"),base.variant(),base.geometry(),base.pose(),base.rootTransform(),base.policy(),Set.of()));
  variants.add(change(base,new CollisionBinding.Geometry(BuiltInGeometryEngines.ADVANCED_MODEL_BOX,MODEL,Map.of(),AnatomyFilter.DEFAULT),base.pose(),base.policy(),Set.of()));
  variants.add(change(base,base.geometry(),new CollisionBinding.Pose(id("chicken"),Map.of(),Set.of()),base.policy(),Set.of()));
  variants.add(new CollisionBinding(1,ENTITY,base.variant(),base.geometry(),base.pose(),id("alternate_root"),base.policy(),Set.of()));
  variants.add(change(base,new CollisionBinding.Geometry(base.geometry().engine(),id("other"),Map.of(),AnatomyFilter.DEFAULT),base.pose(),base.policy(),Set.of()));
  variants.add(change(base,new CollisionBinding.Geometry(base.geometry().engine(),MODEL,Map.of("x","a\nb"),AnatomyFilter.DEFAULT),base.pose(),base.policy(),Set.of()));
  variants.add(change(base,new CollisionBinding.Geometry(base.geometry().engine(),MODEL,Map.of(),new AnatomyFilter(.1,.02,.001,Set.of("root"),Set.of())),base.pose(),base.policy(),Set.of()));
  variants.add(change(base,base.geometry(),new CollisionBinding.Pose(base.pose().engine(),Map.of("x","a"),Set.of()),base.policy(),Set.of()));
  variants.add(change(base,base.geometry(),new CollisionBinding.Pose(base.pose().engine(),Map.of(),Set.of("custom")),base.policy(),Set.of()));
  variants.add(change(base,base.geometry(),base.pose(),new CollisionPolicy.Patch(Optional.of(false),Optional.empty(),Optional.empty()),Set.of()));
  variants.add(change(base,base.geometry(),base.pose(),new CollisionPolicy.Patch(Optional.empty(),Optional.of(.4),Optional.empty()),Set.of()));
  variants.add(change(base,base.geometry(),base.pose(),new CollisionPolicy.Patch(Optional.empty(),Optional.empty(),Optional.of(.2)),Set.of()));
  variants.add(change(base,base.geometry(),base.pose(),base.policy(),Set.of("sleeping")));
  for(var filter:List.of(new AnatomyFilter(.11,AnatomyFilter.DEFAULT.minAspect(),AnatomyFilter.DEFAULT.minVolumeRatio()),new AnatomyFilter(AnatomyFilter.DEFAULT.minThickness(),.12,AnatomyFilter.DEFAULT.minVolumeRatio()),new AnatomyFilter(AnatomyFilter.DEFAULT.minThickness(),AnatomyFilter.DEFAULT.minAspect(),.13),new AnatomyFilter(AnatomyFilter.DEFAULT.minThickness(),AnatomyFilter.DEFAULT.minAspect(),AnatomyFilter.DEFAULT.minVolumeRatio(),Set.of("root"),Set.of()),new AnatomyFilter(AnatomyFilter.DEFAULT.minThickness(),AnatomyFilter.DEFAULT.minAspect(),AnatomyFilter.DEFAULT.minVolumeRatio(),Set.of(),Set.of("root"))))variants.add(change(base,new CollisionBinding.Geometry(base.geometry().engine(),MODEL,Map.of(),filter),base.pose(),base.policy(),Set.of()));
  var ids=new HashSet<Identifier>();ids.add(expected);for(var r:variants)require(ids.add(CollisionBindingIdentity.of(r)),"Semantic field identity sensitivity");
  for(String broken:List.of("\uD800","\uDC00","x\uD800y")){var bad=change(base,new CollisionBinding.Geometry(base.geometry().engine(),MODEL,Map.of("x",broken),AnatomyFilter.DEFAULT),base.pose(),base.policy(),Set.of());rejects(()->CollisionBindingIdentity.of(bad),"Malformed text rejected");}
  var malformedFilter=change(base,new CollisionBinding.Geometry(base.geometry().engine(),MODEL,Map.of(),new AnatomyFilter(.1,.02,.001,Set.of("x\uD800"),Set.of())),base.pose(),base.policy(),Set.of());rejects(()->CollisionBindingIdentity.of(malformedFilter),"Malformed filter text rejected");
  var filterA=new AnatomyFilter(.1,.02,.001,new LinkedHashSet<>(List.of("a","b","c")),Set.of());var filterB=new AnatomyFilter(.1,.02,.001,new LinkedHashSet<>(List.of("c","a","b")),Set.of());require(CollisionBindingIdentity.of(change(base,new CollisionBinding.Geometry(base.geometry().engine(),MODEL,Map.of(),filterA),base.pose(),base.policy(),Set.of())).equals(CollisionBindingIdentity.of(change(base,new CollisionBinding.Geometry(base.geometry().engine(),MODEL,Map.of(),filterB),base.pose(),base.policy(),Set.of()))),"Filter set permutation deterministic");
  var first=change(base,base.geometry(),new CollisionBinding.Pose(base.pose().engine(),new LinkedHashMap<>(Map.of("a","v","b","w")),Set.of("alpha","beta")),base.policy(),Set.of("x","y"));
  var reverse=new LinkedHashMap<String,String>();reverse.put("b","w");reverse.put("a","v");var second=change(base,base.geometry(),new CollisionBinding.Pose(base.pose().engine(),reverse,new LinkedHashSet<>(List.of("beta","alpha"))),base.policy(),new LinkedHashSet<>(List.of("y","x")));
  require(CollisionBindingIdentity.of(first).equals(CollisionBindingIdentity.of(second)),"Permutation deterministic"); h.succeed();
 }
 @GameTest public void ownershipAndProjection(GameTestHelper h){
  var base=row(Map.of());var world=new WorldAnatomyCatalog();var s=world.replace(Map.of(MODEL.toString(),model()),List.of(base));var owned=s.bindings().get(ENTITY);
  var foreign=new WorldAnatomyCatalog.Binding(owned.selection(),owned.model(),owned.poses(),owned.root(),owned.legacyPoseProvider());require(owned.equals(foreign)&&owned!=foreign,"Fixture equal different identity");
  require(s.bindingIdentity(foreign).isEmpty(),"Foreign equal binding does not confer ownership");
  rejects(()->new WorldAnatomyCatalog.Snapshot(s.revision(),s.models(),s.posePrograms(),s.citadelPosePrograms(),s.catalog(),Map.of(ENTITY,foreign),s.prepared()),"Default projection must own exact instances");
  var id=s.bindingIdentity(owned).orElseThrow();require(s.resolveIdentity(Identifier.fromNamespaceAndPath("minecraft","pig"),id).isEmpty(),"Identity bound to entity");
  var incoming=new ArrayList<>(List.of(owned));var index=new PreparedBindingIndex(s.catalog(),incoming);incoming.clear();require(index.size()==1&&index.resolve(ENTITY,Map.of()).orElseThrow()==owned,"Input collection defensively retained");
  var defaults=new HashMap<Identifier,WorldAnatomyCatalog.Binding>();defaults.put(ENTITY,owned);var compat=new WorldAnatomyCatalog.Snapshot(s.revision(),s.models(),s.posePrograms(),s.catalog(),defaults);defaults.clear();require(compat.bindings().size()==1,"Projection immutable ownership");h.succeed();
 }
 @GameTest public void preparedLookupDoesNotExecuteOrPrepare(GameTestHelper h){
  var binds=new java.util.concurrent.atomic.AtomicInteger();var evaluations=new java.util.concurrent.atomic.AtomicInteger();var roots=new java.util.concurrent.atomic.AtomicInteger();var engine=id("s46_adversary_counted_pose");var root=id("s46_adversary_counted_root");
  io.github.r3neer.scalebrews.collision.api.CollisionEngines.registerPose(engine,new io.github.r3neer.scalebrews.collision.api.spi.PoseEngine(){
   public Optional<Map<String,Matrix4f>> evaluate(ModelGeometry g,Inputs in,Map<String,String> p){evaluations.incrementAndGet();return Optional.empty();}
   public Optional<Bound> bind(ModelGeometry g,Map<String,String> p,Set<String> c,Resources r){binds.incrementAndGet();return Optional.of(in->{evaluations.incrementAndGet();return Optional.empty();});}
  });
  io.github.r3neer.scalebrews.collision.api.CollisionEngines.registerRootTransform(root,e->{roots.incrementAndGet();return Optional.empty();});
  var params=new HashMap<String,String>();for(int i=0;i<128;i++)params.put("key"+i,"v".repeat(1024));
  var rows=new ArrayList<CollisionBinding>();for(int i=0;i<3;i++){var b=row(i==0?Map.of():Map.of("coat","v"+i));rows.add(new CollisionBinding(1,b.entity(),b.variant(),b.geometry(),new CollisionBinding.Pose(engine,params,Set.of()),root,b.policy(),b.excludedStates()));}
  var world=new WorldAnatomyCatalog();var s=world.replace(Map.of(MODEL.toString(),model()),rows);require(binds.get()>=3,"Counter observes real preparation for all executable rows");
  var packets=new ArrayList<>(world.preparedPackets(UUID.fromString("12345678-1234-1234-1234-123456789012")));require(packets.size()>1,"Real multipart canonical bundle");
  Collections.reverse(packets);var transfer=new AnatomyCatalogTransfer();for(int n=0;n<packets.size()-1;n++){transfer.accept(packets.get(n));require(!transfer.ready(),"Partial variant bundle cannot publish");}transfer.accept(packets.getLast());require(transfer.ready()&&transfer.snapshot().prepared().size()==3,"Out-of-order multipart reconstruction");
  for(var fact:List.of(Map.<String,String>of(),Map.of("coat","v1"),Map.of("coat","v2"))){var original=s.resolve(ENTITY,fact).orElseThrow();var received=transfer.snapshot().resolve(ENTITY,fact).orElseThrow();require(s.bindingIdentity(original).equals(transfer.snapshot().bindingIdentity(received)),"Large parameter identity survives wire");}
  int preparationBinds=binds.get();var lookup=s.resolve(ENTITY,Map.of("coat","v2")).orElseThrow();var identity=s.bindingIdentity(lookup).orElseThrow();long started=System.nanoTime();
  for(int i=0;i<100000;i++){require(s.resolve(ENTITY,Map.of("coat","v2")).orElseThrow()==lookup,"Stable exact prepared object");require(s.resolveIdentity(ENTITY,identity).orElseThrow()==lookup,"Stable exact ID lookup");require(s.bindingIdentity(lookup).orElseThrow()==identity,"Prepared ID object reused");}
  require(binds.get()==preparationBinds&&evaluations.get()==0&&roots.get()==0,"Lookups do not bind/sample/evaluate");System.out.println("S46_ADV_LOOKUP calls=300000 preparationBinds="+preparationBinds+" additionalBinds=0 evaluations=0 rootSamples=0 elapsedNs="+(System.nanoTime()-started));
  var all=new ArrayList<CollisionBinding>();for(int i=0;i<4096;i++)all.add(row(Map.of("slot","v"+i)));var catalog=new CollisionBindingCatalog(all);var prototype=lookup;
  var prepared=new ArrayList<WorldAnatomyCatalog.Binding>();for(var b:all)prepared.add(new WorldAnatomyCatalog.Binding(b,prototype.model(),prototype.poses(),prototype.root(),null));var index=new PreparedBindingIndex(catalog,prepared);require(index.size()==4096&&index.resolve(ENTITY,Map.of("slot","v4095")).isPresent(),"Maximum executable population retained");
  rejects(()->new PreparedBindingIndex(catalog,new ArrayList<>(Collections.nCopies(4097,prepared.getFirst()))),"Maximum plus one rejected");h.succeed();
 }
 @GameTest public void atomicInvalidNondefault(GameTestHelper h){
  var world=new WorldAnatomyCatalog();var base=row(Map.of());var prior=world.replace(Map.of(MODEL.toString(),model()),List.of(base));var epoch=UUID.fromString("12345678-1234-1234-1234-123456789012");var before=world.preparedPackets(epoch);
  var malformed=row(Map.of("coat","\uD800"));rejects(()->world.replace(Map.of(MODEL.toString(),model()),List.of(base,malformed)),"Invalid nondefault text rejected");
  require(world.snapshot()==prior,"Exact snapshot retained");require(world.preparedPackets(epoch).equals(before),"Exact wire pair retained");
  var nondefault=row(Map.of("coat","invalid_filter"));var invalidFilter=change(nondefault,new CollisionBinding.Geometry(nondefault.geometry().engine(),MODEL,Map.of(),new AnatomyFilter(.1,.02,.001,Set.of("not_a_part"),Set.of())),nondefault.pose(),nondefault.policy(),Set.of());rejects(()->world.replace(Map.of(MODEL.toString(),model()),List.of(base,invalidFilter)),"Invalid executable nondefault rejected");require(world.snapshot()==prior&&world.preparedPackets(epoch).equals(before),"Executable rejection keeps exact accepted pair");
  var absent=row(Map.of("coat","unknown"));var missing=change(absent,new CollisionBinding.Geometry(absent.geometry().engine(),id("absent"),Map.of("x","\uDC00"),AnatomyFilter.DEFAULT),absent.pose(),absent.policy(),Set.of());
  rejects(()->world.replace(Map.of(MODEL.toString(),model()),List.of(base,missing)),"Catalog-only text cannot bypass validation");require(world.snapshot()==prior,"Catalog-only rejection atomic");h.succeed();
 }
}
