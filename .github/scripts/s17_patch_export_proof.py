from pathlib import Path

p = Path('src/gametest/java/io/github/r3neer/scalebrews/test/AnatomyExportProof.java')
text = p.read_text()

def one(old, new, label):
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected 1 occurrence, found {count}')
    text = text.replace(old, new, 1)

one('import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;\n',
    'import io.github.r3neer.scalebrews.client.collision.preparation.GeometryExtractor;\nimport io.github.r3neer.scalebrews.client.collision.preparation.ModelPartGeometryEngine;\n', 'import')

one('''            var cow=CowModel.createBodyLayer().bakeRoot();
            verifyVanilla(cow,Set.of(),"minecraft:cow");
            verifyCowPoses();
            verifyVanillaFamilies();
            var skinLayers=Set.of("hat","jacket","left_sleeve","right_sleeve","left_pants","right_pants");
            for(boolean slim:new boolean[]{false,true}) {
                var root=LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot();
                verifyVanilla(root,skinLayers,"minecraft:player_"+(slim?"slim":"wide"));
                verifyPlayerPoses(root,slim,skinLayers);
            }
''', '''            var cow=CowModel.createBodyLayer().bakeRoot();
            verifyVanilla(cow,"minecraft:cow",()->CowModel.createBodyLayer().bakeRoot());
            verifyCowPoses();
            verifyVanillaFamilies();
            for(boolean slim:new boolean[]{false,true}) {
                var root=LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot();
                String source="minecraft:player_"+(slim?"slim":"wide");
                verifyVanilla(root,source,()->LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE,slim),64,64).bakeRoot());
                verifyPlayerPoses(root,slim);
            }
''', 'runTest vanilla sources')

one('''    private static void verifyPlayerPoses(ModelPart root,boolean slim,Set<String> skinLayers) {
        var model=new PlayerModel(root,slim);
        String source="minecraft:player_"+(slim?"slim":"wide");
        var geometry=GeometryExtractor.vanilla(source,"26.2",root,skinLayers);
''', '''    private static void verifyPlayerPoses(ModelPart root,boolean slim) {
        var model=new PlayerModel(root,slim);
        String source="minecraft:player_"+(slim?"slim":"wide");
        var geometry=modelPart(source);
''', 'player prepared geometry')

one('''                GeometryExtractor.vanilla(source,"26.2",root,skinLayers).evaluate(new Matrix4f(),Map.of(),new AnatomyFilter(0,0,0)),source+" tick "+tick);
''', '''                GeometryExtractor.vanilla(source,"26.2",root,Set.of()).evaluate(new Matrix4f(),Map.of(),new AnatomyFilter(0,0,0)),source+" tick "+tick);
''', 'player reference geometry')

one('''        var model=new CowModel(CowModel.createBodyLayer().bakeRoot());
        var geometry=GeometryExtractor.vanilla("minecraft:cow","26.2",model.root(),Set.of());
''', '''        var model=new CowModel(CowModel.createBodyLayer().bakeRoot());
        var geometry=modelPart("minecraft:cow");
''', 'cow prepared geometry')

old_verify = '''    private static void verifyVanilla(ModelPart root,Set<String> excluded,String source) {
        var result=GeometryExtractor.vanilla(source,"26.2",root,excluded);
        try {
            var dispatcher=net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher();
            var field=dispatcher.getClass().getDeclaredField(source.startsWith("minecraft:player_")?"playerRenderers":"renderers");field.setAccessible(true);
            var renderers=(Map<?,?>)field.get(dispatcher);
            Object renderer=source.startsWith("minecraft:player_")?renderers.values().iterator().next():renderers.get(net.minecraft.world.entity.EntityTypes.COW);
            Object state=source.startsWith("minecraft:player_")?new net.minecraft.client.renderer.entity.state.AvatarRenderState():new net.minecraft.client.renderer.entity.state.LivingEntityRenderState();
            result=result.withModelTransform(rendererRoot(renderer,state,false));
        }catch(ReflectiveOperationException e){throw new AssertionError("Original renderer transform extraction failed",e);}
'''
new_verify = '''    private static void verifyVanilla(ModelPart root,String source,java.util.function.Supplier<ModelPart> freshRoot) {
        final Matrix4f modelTransform;
        try {
            var dispatcher=net.minecraft.client.Minecraft.getInstance().getEntityRenderDispatcher();
            var field=dispatcher.getClass().getDeclaredField(source.startsWith("minecraft:player_")?"playerRenderers":"renderers");field.setAccessible(true);
            var renderers=(Map<?,?>)field.get(dispatcher);
            Object renderer=source.startsWith("minecraft:player_")?renderers.values().iterator().next():renderers.get(net.minecraft.world.entity.EntityTypes.COW);
            Object state=source.startsWith("minecraft:player_")?new net.minecraft.client.renderer.entity.state.AvatarRenderState():new net.minecraft.client.renderer.entity.state.LivingEntityRenderState();
            modelTransform=rendererRoot(renderer,state,false);
        }catch(ReflectiveOperationException e){throw new AssertionError("Original renderer transform extraction failed",e);}
        ModelPartGeometryEngine.registerSource(net.minecraft.resources.Identifier.parse(source),
            new ModelPartGeometryEngine.Source("26.2",freshRoot,()->new Matrix4f(modelTransform)));
        var result=modelPart(source);
'''
one(old_verify, new_verify, 'verifyVanilla engine route')

insert_before = '''    private static void save(ModelGeometry model) {
'''
helper = '''    private static ModelGeometry modelPart(String source) {
        var engine=CollisionEngines.geometry(BuiltInGeometryEngines.MODEL_PART)
            .orElseThrow(()->new AssertionError("Built-in ModelPart geometry engine is not registered"));
        return engine.prepare(new io.github.r3neer.scalebrews.collision.api.spi.GeometryEngine.Request(net.minecraft.resources.Identifier.parse(source)))
            .orElseThrow(()->new AssertionError("ModelPart source was not prepared: "+source));
    }
    private static void save(ModelGeometry model) {
'''
one(insert_before, helper, 'modelPart helper')

p.write_text(text)
