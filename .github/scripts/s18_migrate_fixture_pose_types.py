from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly 1 occurrence, found {count}")
    return text.replace(old, new, 1)

# AnatomyGeometryTests: migrate explicit legacy variable types while retaining source-compatible wrappers.
p = Path("src/gametest/java/io/github/r3neer/scalebrews/test/AnatomyGeometryTests.java")
text = p.read_text()
if "import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;" not in text:
    text = replace_once(
        text,
        "import io.github.r3neer.scalebrews.collision.api.*;\n",
        "import io.github.r3neer.scalebrews.collision.api.*;\nimport io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;\n",
        "AnatomyGeometryTests PoseEngine import",
    )
text = replace_once(
    text,
    "PoseProvider pose=(geometry,inputs)->{calls[0]++;return java.util.Optional.of(java.util.Map.of());};",
    "PoseEngine pose=(geometry,inputs,parameters)->{calls[0]++;return java.util.Optional.of(java.util.Map.of());};",
    "AnatomyGeometryTests root-cache evaluator",
)
text = replace_once(
    text,
    "PoseProvider poseProvider=switch(model.source()) {case \"minecraft:cow\"->new QuadrupedPose();case \"alexsmobs:grizzly_bear\"->new GrizzlyPose();default->new PlayerWalkingPose();};",
    "PoseEngine poseProvider=switch(model.source()) {case \"minecraft:cow\"->new QuadrupedPose();case \"alexsmobs:grizzly_bear\"->new GrizzlyPose();default->new PlayerWalkingPose();};",
    "AnatomyGeometryTests prepared evaluator",
)
count = text.count("PoseProvider.Inputs")
if count < 1:
    raise SystemExit("AnatomyGeometryTests: expected at least one PoseProvider.Inputs fixture")
text = text.replace("PoseProvider.Inputs", "PoseEngine.Inputs")
p.write_text(text)

# Live original-model acceptance: the packet now carries canonical PoseEngine.Inputs and
# the comparison must use the canonical engine, not the deprecated wrapper signature.
p = Path("src/gametest/java/io/github/r3neer/scalebrews/test/AnatomyLivePoseAcceptanceTests.java")
text = p.read_text()
text = replace_once(
    text,
    "import io.github.r3neer.scalebrews.collision.pose.PoseProvider;\n",
    "import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;\n",
    "AnatomyLivePoseAcceptanceTests PoseEngine import",
)
text = replace_once(
    text,
    "import io.github.r3neer.scalebrews.collision.pose.QuadrupedPose;\n",
    "import io.github.r3neer.scalebrews.collision.pose.QuadrupedPoseEngine;\n",
    "AnatomyLivePoseAcceptanceTests canonical engine import",
)
text = replace_once(
    text,
    "private static void compareOriginalModel(Cow cow,PoseProvider.Inputs inputs) {",
    "private static void compareOriginalModel(Cow cow,PoseEngine.Inputs inputs) {",
    "AnatomyLivePoseAcceptanceTests canonical input type",
)
text = replace_once(
    text,
    "new QuadrupedPose().evaluate(baseline,inputs).orElseThrow()",
    "new QuadrupedPoseEngine().evaluate(baseline,inputs,Map.of()).orElseThrow()",
    "AnatomyLivePoseAcceptanceTests canonical pose evaluation",
)
p.write_text(text)

# Query-frame fixtures are purely causal DTO fixtures. Migrate them too so the S18 suite no
# longer grows fresh dependencies on the legacy input alias.
p = Path("src/gametest/java/io/github/r3neer/scalebrews/test/AnatomyQueryFrameTests.java")
text = p.read_text()
text = replace_once(
    text,
    "import io.github.r3neer.scalebrews.collision.pose.PoseProvider;\n",
    "import io.github.r3neer.scalebrews.collision.api.spi.PoseEngine;\n",
    "AnatomyQueryFrameTests PoseEngine import",
)
count = text.count("PoseProvider.Inputs")
if count < 1:
    raise SystemExit("AnatomyQueryFrameTests: expected legacy input fixtures")
text = text.replace("PoseProvider.Inputs", "PoseEngine.Inputs")
p.write_text(text)
