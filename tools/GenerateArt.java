import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/** Original geometric pixel art, reproducible with: java tools/GenerateArt.java */
public class GenerateArt {
    static final Color OUTLINE = new Color(0x252237);
    static final Color GOLD = new Color(0xEEAA49);
    static final Color VIOLET = new Color(0xA288E8);

    static void rect(Graphics2D g, Color c, int x, int y, int w, int h) {
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    static BufferedImage effect(boolean growth) {
        var image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        // Hand-placed pixel clusters: a blocky person, separated from a directional
        // arrow by negative space. No resampling, noise or anti-aliasing.
        String[] person = growth ? new String[]{
            "...####...", "...####...", "...####...", "...####...",
            "....##....", ".########.", ".########.", ".########.",
            ".########.", ".########.", "...#####..", "...#####..",
            "...##.##..", "...##.##..", "...##.##.."
        } : new String[]{
            "...###....", "...###....", "...###....", "....#.....",
            "..#####...", "..#####...", "..#####...", "...###....",
            "...#.#....", "...#.#...."
        };
        String[] up = {"..#..", ".###.", "#####", "..#..", "..#..", "..#..", "..#..", "..#..", "..#.."};
        String[] down = {"..#..", "..#..", "..#..", "..#..", "..#..", "..#..", "#####", ".###.", "..#.."};
        int[] palette = growth ? new int[]{0x704226,0xD98C3F,0xFFE0A0}
                               : new int[]{0x40305F,0x9574DC,0xDFD0FF};
        stamp(image, person, 0, growth ? 1 : 6, palette);
        stamp(image, growth ? up : down, 11, growth ? 2 : 6, palette);
        return image;
    }

    static boolean filled(String[] mask, int x, int y) {
        return y >= 0 && y < mask.length && x >= 0 && x < mask[y].length() && mask[y].charAt(x) == '#';
    }

    static void stamp(BufferedImage image, String[] mask, int ox, int oy, int[] palette) {
        for (int y=0; y<mask.length; y++) for (int x=0; x<mask[y].length(); x++) {
            if (filled(mask,x,y)) image.setRGB(ox+x+1,oy+y+1,0xFF000000 | palette[0]);
        }
        for (int y=0; y<mask.length; y++) for (int x=0; x<mask[y].length(); x++) {
            if (!filled(mask,x,y)) continue;
            // Small top-left lit clusters; keep interiors flat and the silhouette readable.
            boolean lit = !filled(mask,x,y-1) || (!filled(mask,x-1,y) && filled(mask,x+1,y));
            image.setRGB(ox+x,oy+y,0xFF000000 | palette[lit ? 2 : 1]);
        }
    }

    static void save(BufferedImage image, Path path) throws Exception {
        Files.createDirectories(path.getParent());
        ImageIO.write(image, "png", path.toFile());
    }

    static BufferedImage saddle(boolean bee) {
        var image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        // Hand-painted UV islands: warm leather, dark binding, sparse cream stitches,
        // iron buckles. No copied Minecraft pixels, filters, or generated-image models.
        Color leather = new Color(0x995D35), light = new Color(0xC38A50), dark = new Color(0x583724);
        rect(g, dark, 0, 0, 20, 7);
        rect(g, leather, 1, 1, 18, 5);
        rect(g, light, 5, 0, 5, 5);
        rect(g, new Color(0xAD7040), 6, 1, 3, 3);
        rect(g, new Color(0xD9AF70), 5, 0, 5, 1);
        rect(g, dark, 0, 8, 24, 8);
        rect(g, new Color(bee ? 0xAA783C : 0x825339), 1, 9, 22, 6);
        for (int x = 2; x < 23; x += 3) rect(g, new Color(0xD1AF7A), x, 9, 1, 1);
        rect(g, dark, 32, 0, 4, 9);
        rect(g, leather, 33, 1, 2, 7);
        rect(g, new Color(0xD2B686), 33, 2, 1, 1);
        rect(g, dark, 40, 0, 6, 5);
        rect(g, new Color(0xB7B8AC), 40, 0, 6, 1);
        rect(g, new Color(0xE2E0CD), 41, 1, 1, 3);
        rect(g, new Color(0x85877D), 44, 1, 1, 3);
        rect(g, new Color(0xB7B8AC), 40, 4, 6, 1);
        g.dispose();
        return image;
    }

    static BufferedImage flower() {
        var image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        rect(g, new Color(0x335D27), 11, 8, 1, 6);
        rect(g, new Color(0x65853C), 10, 11, 1, 2);
        rect(g, new Color(0x91AA53), 12, 10, 2, 1);
        rect(g, new Color(0x713244), 9, 6, 5, 3);
        rect(g, new Color(0xB85262), 10, 5, 3, 5);
        rect(g, new Color(0xE28A89), 9, 6, 2, 2);
        rect(g, new Color(0xD57479), 12, 6, 2, 2);
        rect(g, new Color(0xF2C663), 11, 7, 1, 1);
        g.dispose();
        return image;
    }

    public static void main(String[] args) throws Exception {
        Path assets = Path.of("src/main/resources/assets/scalebrews");
        save(saddle(false), assets.resolve("textures/entity/saddle/chicken.png"));
        save(saddle(true), assets.resolve("textures/entity/saddle/bee.png"));
        save(saddle(false), assets.resolve("textures/entity/saddle/wolf.png"));
        save(flower(), assets.resolve("textures/item/flower_on_a_stick_overlay.png"));
        var growth = effect(true);
        var shrinking = effect(false);
        save(growth, assets.resolve("textures/gui/sprites/mob_effect/growth.png"));
        save(shrinking, assets.resolve("textures/gui/sprites/mob_effect/shrinking.png"));
        var icon = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        var g = icon.createGraphics();
        rect(g, OUTLINE, 0, 0, 128, 128);
        rect(g, new Color(0x36334A), 6, 6, 116, 116);
        rect(g, GOLD, 10, 10, 52, 3);
        rect(g, VIOLET, 66, 115, 52, 3);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(growth, 6, 30, 54, 54, null);
        g.drawImage(shrinking, 68, 30, 54, 54, null);
        g.dispose();
        save(icon, assets.resolve("icon.png"));
        if (args.length > 0) {
            var preview = new BufferedImage(560, 224, BufferedImage.TYPE_INT_RGB);
            var p = preview.createGraphics();
            p.setColor(new Color(0x202126)); p.fillRect(0,0,560,224);
            for (int i=0;i<2;i++) {
                var effect = i==0 ? growth : shrinking;
                int x=20+i*280;
                p.drawImage(effect,x,20,144,144,null);
                p.setColor(new Color(0x8B8B8B));p.fillRect(x+172,30,36,36);
                p.drawImage(effect,x+181,39,null);
                p.drawImage(effect,x+172,90,36,36,null);
                p.setColor(Color.WHITE);p.drawString(i==0 ? "Growth" : "Shrinking",x,194);
                p.drawString("1x / 2x",x+164,160);
            }
            p.dispose(); save(preview,Path.of(args[0]));
        }
    }
}
