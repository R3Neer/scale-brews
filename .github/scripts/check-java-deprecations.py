#!/usr/bin/env python3
"""Reject obsolete runtime APIs while reporting intentional collision fixture adapters."""
import re
import sys
from pathlib import Path

# S18/S42: these adapters remain only for historical acceptance fixtures. This
# list is diagnostic-specific and test-source-only; it does not exempt a class
# from native/external API checks or suppress javac's diagnostics.
FIXTURE_DIAGNOSTICS = frozenset({
    "PoseProvider in io.github.r3neer.scalebrews.collision.pose has been deprecated",
    "Inputs in PoseProvider has been deprecated",
    "PlayerWalkingPose in io.github.r3neer.scalebrews.collision.pose has been deprecated",
    "QuadrupedPose in io.github.r3neer.scalebrews.collision.pose has been deprecated",
    "VanillaFamilyPose in io.github.r3neer.scalebrews.collision.pose has been deprecated",
    "PoseProviders in io.github.r3neer.scalebrews.collision.pose has been deprecated",
    "alex(String,String,Object,Set<String>) in GeometryExtractor has been deprecated",
    "vanilla(String,String,ModelPart,Set<String>) in GeometryExtractor has been deprecated",
    "tick() in AnatomyPosePayload has been deprecated",
    "AnatomyContactPayload(UUID,long,Identifier,int,UUID,long,long,long,int,UUID,String,int,Vec3,Vec3) in AnatomyContactPayload has been deprecated",
    "PlatformEligibility in io.github.r3neer.scalebrews.platform has been deprecated",
})
DIAGNOSTIC = re.compile(r"^(.+\.java):\d+: warning: \[deprecation\] (.+)$")

def check(log: str) -> int:
    accepted = 0
    rejected = []
    for line in log.splitlines():
        if "Some input files additionally use or override a deprecated API" in line:
            rejected.append("Compiler warning cap omitted deprecation diagnostics; increase -Xmaxwarns.")
        if "[deprecation]" not in line:
            continue
        match = DIAGNOSTIC.fullmatch(line)
        if (match and "/src/gametest/java/" in ("/" + match[1].replace("\\", "/"))
                and match[2] in FIXTURE_DIAGNOSTICS):
            accepted += 1
        else:
            rejected.append(line)
    if rejected:
        print("::error::Deprecated runtime/external API or unclassified diagnostic remains.")
        print("\n".join(rejected))
        return 1
    print(f"Deprecation audit passed; {accepted} intentional test-only collision adapter diagnostics reported.")
    return 0

if __name__ == "__main__":
    sys.exit(check(Path(sys.argv[1]).read_text(encoding="utf-8")))
