from pathlib import Path

p = Path('src/gametest/java/io/github/r3neer/scalebrews/collision/internal/S16CanonicalCatalogAuthorityTests.java')
text = p.read_text()
replacements = [
    (
        'public void incompatibleCanonicalBundleSchemaOwnsProtocolV4(GameTestHelper h) {\n        h.assertTrue(AnatomyApi.PROTOCOL_VERSION == 4,\n            "Replacing legacy profiles with canonical bindings inside the authoritative bundle is an incompatible wire change and must own protocol v4");',
        'public void canonicalBundleNeverRegressesToLegacyProfileProtocol(GameTestHelper h) {\n        h.assertTrue(AnatomyApi.PROTOCOL_VERSION >= 4,\n            "Canonical binding bundles must never regress to the pre-S16 profile-era wire protocol; later incompatible catalog features may advance the version");'
    ),
    ('"Protocol-v4 catalog bundle must not carry legacy PlatformDefinition profiles"',
     '"Canonical post-profile catalog bundle must not carry legacy PlatformDefinition profiles"'),
    ('"A complete protocol-v4 variant catalog must publish the announced revision atomically"',
     '"A complete canonical variant catalog must publish the announced revision atomically"'),
    ('"A complete but invalid protocol-v4 replacement must fail during authoritative candidate validation"',
     '"A complete but invalid canonical replacement must fail during authoritative candidate validation"'),
]
for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'expected one historical S16 protocol oracle occurrence, found {count}: {old[:80]}')
    text = text.replace(old, new, 1)
p.write_text(text)
