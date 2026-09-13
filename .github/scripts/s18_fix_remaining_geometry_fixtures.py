from pathlib import Path

p = Path('src/gametest/java/io/github/r3neer/scalebrews/test/AnatomyGeometryTests.java')
text = p.read_text()
replacements = [
    (
        'PoseEngine poseProvider=switch(model.source()) {case "minecraft:cow"->new QuadrupedPose();case "alexsmobs:grizzly_bear"->new GrizzlyPose();default->new PlayerWalkingPose();};',
        'java.util.function.BiFunction<ModelGeometry,PoseEngine.Inputs,java.util.Optional<java.util.Map<String,Matrix4f>>> poseProvider=switch(model.source()) {case "minecraft:cow"->(g,i)->new QuadrupedPoseEngine().evaluate(g,i,java.util.Map.of());case "alexsmobs:grizzly_bear"->(g,i)->new GrizzlyPose().evaluate(g,new PoseProvider.Inputs(i.walkPhase(),i.walkAmount(),i.age(),i.headYaw(),i.headPitch(),i.ordinary(),i.channels()));default->(g,i)->new PlayerWalkingPoseEngine().evaluate(g,i,java.util.Map.of());};'
    ),
    (
        'new QuadrupedPose().evaluate(model,new PoseEngine.Inputs(tick*.37f,.5f,tick,10,5,true)).orElseThrow()',
        'new QuadrupedPoseEngine().evaluate(model,new PoseEngine.Inputs(tick*.37f,.5f,tick,10,5,true),java.util.Map.of()).orElseThrow()'
    ),
    (
        'new GrizzlyPose().evaluate(model,new PoseEngine.Inputs(tick*.37f,.5f,tick,10,5,true)).orElseThrow()',
        'new GrizzlyPose().evaluate(model,new PoseProvider.Inputs(tick*.37f,.5f,tick,10,5,true)).orElseThrow()'
    ),
    (
        'new PlayerWalkingPose().evaluate(model,new PoseEngine.Inputs(tick*.37f,.5f,tick,10,5,true)).orElseThrow()',
        'new PlayerWalkingPoseEngine().evaluate(model,new PoseEngine.Inputs(tick*.37f,.5f,tick,10,5,true),java.util.Map.of()).orElseThrow()'
    ),
]
for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'expected exactly one fixture seam, found {count}: {old[:100]}')
    text = text.replace(old, new, 1)
p.write_text(text)
