"""Create test-only S47 shadow sources; never modifies production."""
import argparse
import hashlib
import json
from pathlib import Path

CASES = [
    ("instrument-hash", "main", "collision/internal/CollisionBindingIdentity", "public static Identifier of(CollisionBinding binding) {", "public static final java.util.concurrent.atomic.AtomicLong ADVERSARIAL_HASH_COUNT=new java.util.concurrent.atomic.AtomicLong();\n    public static Identifier of(CollisionBinding binding) {\n        ADVERSARIAL_HASH_COUNT.incrementAndGet();"),
    ("mutant-history", "main", "collision/internal/AnatomyFrameHistory", "&& java.util.Objects.equals(current.bindingIdentity(),next.bindingIdentity())", "&& true"),
    ("mutant-type", "main", "collision/internal/WorldAnatomyCatalog", "prepared.resolveIdentity(entityType, packet.bindingIdentity())", "prepared.resolveIdentity(packet.bindingIdentity())"),
    ("mutant-budget", "main", "collision/internal/AnatomyPosePayload", "b.readUtf(CollisionBindingIdentity.TOKEN_LENGTH)", "b.readUtf(32767)"),
    ("mutant-fence", "main", "collision/internal/TrackingReplayFence", "&& rootProvider.equals(packet.rootProvider()) && Objects.equals(bindingIdentity,packet.bindingIdentity())", "&& rootProvider.equals(packet.rootProvider())"),
    ("mutant-early", "client", "client/collision/network/AnatomyClientNetworking", "transfer.snapshot().resolvePose(null,packet).isEmpty()", "!transfer.snapshot().models().containsKey(packet.model().toString())"),
]

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[1]
    receipt = []
    for name, environment, owner, old, new in CASES:
        relative = Path("src") / environment / "java/io/github/r3neer/scalebrews" / (owner + ".java")
        source = (repo / relative).read_text(encoding="utf-8")
        if source.count(old) != 1:
            raise ValueError("Mutation seam changed: " + str(relative))
        mutated = source.replace(old, new)
        if name == "mutant-history":
            mutated = mutated.replace("|| !java.util.Objects.equals(current.bindingIdentity(),next.bindingIdentity())", "|| false")
        destination = args.output / name / "io/github/r3neer/scalebrews" / (owner + ".java")
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(mutated, encoding="utf-8")
        receipt.append({"case": name, "requireHashCounter": name == "instrument-hash", "environment": environment, "original": relative.as_posix(), "shadow": str(destination), "originalSha256": hashlib.sha256((repo / relative).read_bytes()).hexdigest(), "shadowSha256": hashlib.sha256(destination.read_bytes()).hexdigest(), "replace": [old, new]})
    (args.output / "recipes.json").write_text(json.dumps(receipt, indent=2), encoding="utf-8")
    print(json.dumps(receipt, indent=2))

if __name__ == "__main__":
    main()
