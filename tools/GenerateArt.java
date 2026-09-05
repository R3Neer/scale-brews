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
    static final Color GLASS = new Color(0xDDEEF2);
    static final Color GOLD = new Color(0xEEAA49);
    static final Color VIOLET = new Color(0xA288E8);

    static void rect(Graphics2D g, Color c, int x, int y, int w, int h) {
        g.setColor(c);
        g.fillRect(x, y, w, h);
    }

    static void bottle(Graphics2D g, int x, int y, int w, int h, Color liquid) {
        int neck = Math.max(2, w / 3);
        int nx = x + (w - neck) / 2;
        rect(g, OUTLINE, nx - 1, y, neck + 2, 5);
        rect(g, new Color(0xA16D45), nx, y, neck, 2);
        rect(g, GLASS, nx, y + 2, neck, 3);
        rect(g, OUTLINE, x + 1, y + 4, w - 2, h - 4);
        rect(g, OUTLINE, x, y + 5, w, h - 6);
        rect(g, GLASS, x + 1, y + 5, w - 2, h - 6);
        rect(g, liquid.darker(), x + 2, y + 7, w - 4, h - 8);
        rect(g, liquid, x + 2, y + 7, w - 4, Math.max(1, h - 10));
        rect(g, Color.WHITE, x + 2, y + 5, 1, 3);
    }

    static BufferedImage effect(boolean growth) {
        var image = new BufferedImage(18, 18, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        Color color = growth ? GOLD : VIOLET;
        bottle(g, growth ? 2 : 4, growth ? 2 : 6, growth ? 10 : 8, growth ? 14 : 10, color);
        // Direction and size distinguish effects even without color vision.
        int[] xs = {12, 15, 18, 16, 16, 14, 14};
        int[] ys = growth ? new int[]{6, 3, 6, 6, 12, 12, 6}
                          : new int[]{11, 14, 11, 11, 5, 5, 11};
        g.setColor(OUTLINE);
        g.drawPolygon(xs, ys, 7);
        g.setColor(color);
        g.fillPolygon(xs, ys, 7);
        g.dispose();
        return image;
    }

    static void save(BufferedImage image, Path path) throws Exception {
        Files.createDirectories(path.getParent());
        ImageIO.write(image, "png", path.toFile());
    }

    public static void main(String[] args) throws Exception {
        Path assets = Path.of("src/main/resources/assets/scalebrews");
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
        g.drawImage(growth, 8, 22, 72, 72, null);
        g.drawImage(shrinking, 69, 45, 54, 54, null);
        g.dispose();
        save(icon, assets.resolve("icon.png"));
    }
}
