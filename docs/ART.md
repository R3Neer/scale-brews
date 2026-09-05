# Art direction and provenance

The first bottle-shaped effect icons were rejected by the owner. The replacement is a blocky human silhouette with an upward arrow for Growth and a smaller silhouette with a downward arrow for Shrinking. Direction and figure size distinguish them without relying on color alone. Amber and violet echo the registered potion colors.

The icons are hand-designed 18x18 pixel masks in `tools/GenerateArt.java`: restricted hue-shifted palettes, small top-left highlights, lower-right colored shadows, opaque pixel clusters and negative space. No image-generation model, smooth scaling or random texture noise is used. The generator also builds the mod icon. Supply an output PNG argument to generate a comparison at native, 2x and 8x scale.

References inspected:

- [Blockbench Minecraft Style Guide](https://blockbench.net/wiki/guides/minecraft-style-guide/): restricted palettes, deliberate pixels, readable shapes and avoiding mixed pixel resolutions.
- [Mojang's interview about the Texture Update](https://www.minecraft.net/en-us/article/try-new-minecraft-textures): readability and avoiding excessive smoothing at low resolution.
- Installed Minecraft 26.2 Strength, Speed, Jump Boost and Invisibility icons, reviewed enlarged and in the beacon UI.

The replacement pixel masks are original and GPL-3.0-or-later, like this repository. Minecraft's own bottle models, tinting, particles and sounds are reused through their existing resource identifiers; their artwork is not copied into the repository or relicensed. This preserves the freely redistributable status of this mod's own files. Minecraft itself remains required and separately licensed. See [Minecraft Usage Guidelines](https://www.minecraft.net/en-us/usage-guidelines).

The beacon and inventory use the same effect sprite from the GUI atlas. Screenshots from the client GameTest are reviewed at actual GUI scale to check shape, spacing and contrast. The final aesthetic judgement remains the owner's; automated loading checks cannot judge artistic quality.
