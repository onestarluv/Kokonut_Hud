import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Builds the currency HUD chips in the CoreUi book style (leather rim, parchment face) with the 9 x 9 mini icons.
 * Drawn at their final size (HUD scale 1.0) so every pixel stays square.
 *
 * java tools/HudChips.java art/icons common-resources/default/assets/currency
 * reads silver_mini.png, gold_mini.png, kokonut_mini.png
 * -> chip_silver.png (77 x 13), chip_gold.png, chip_kokonut.png (51 x 13)
 *
 * The chip is only as tall as the icon: 1 px outline + 1 px rim around a 9 px face. The balance (8 px font, 7 px ink)
 * sits at y 3 so it is centred on the face, and starts at x 15 after the icon well and divider.
 */
public class HudChips {
    static final int ICON = 9, H = ICON + 4;
    static final java.util.Map<String, Integer> WIDTH = java.util.Map.of("silver", 77, "gold", 51, "kokonut", 51);
    static final int OUTLINE = 0xFF1E0E06, RIM = 0xFF6B3A24, RIM_HI = 0xFF9A5A36, RIM_LO = 0xFF3E1E10,
            PAPER = 0xFFEADBB5, PAPER_SHADE = 0xFFCDB98A, PAPER_LIGHT = 0xFFF6ECD0, WELL = 0xFFD8C79C;

    public static void main(String[] args) throws Exception {
        File icons = new File(args[0]), out = new File(args[1]);
        out.mkdirs();
        for (String name : new String[]{"silver", "gold", "kokonut"}) {
            BufferedImage icon = ImageIO.read(new File(icons, name + "_mini.png"));
            if (icon.getWidth() != ICON || icon.getHeight() != ICON) throw new IllegalArgumentException(name + "_mini must be 9x9");
            ImageIO.write(chip(icon, WIDTH.get(name)), "png", new File(out, "chip_" + name + ".png"));
        }
        System.out.println("chips -> " + out);
    }

    static BufferedImage chip(BufferedImage icon, int W) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        fill(img, 0, 0, W, H, OUTLINE);
        // 1 px leather rim, lit from the top left like the dex book cover.
        fill(img, 1, 1, W - 2, H - 2, RIM);
        fill(img, 1, 1, W - 2, 1, RIM_HI);
        fill(img, 1, 1, 1, H - 2, RIM_HI);
        fill(img, 1, H - 2, W - 2, 1, RIM_LO);
        fill(img, W - 2, 1, 1, H - 2, RIM_LO);
        // Parchment face (inside 2..W-3 x 2..H-3) with a soft lower edge.
        fill(img, 2, 2, W - 4, H - 4, PAPER);
        fill(img, 2, H - 3, W - 4, 1, PAPER_SHADE);
        // Icon well on the left, then a thin divider before the number.
        fill(img, 2, 2, ICON, ICON, WELL);
        fill(img, 2 + ICON, 2, 1, H - 4, PAPER_SHADE);
        fill(img, 3 + ICON, 2, 1, H - 4, PAPER_LIGHT);
        for (int y = 0; y < ICON; y++)
            for (int x = 0; x < ICON; x++) {
                int argb = icon.getRGB(x, y);
                if ((argb >>> 24) != 0) img.setRGB(2 + x, 2 + y, argb);
            }
        return img;
    }

    static void fill(BufferedImage img, int x, int y, int w, int h, int argb) {
        for (int yy = y; yy < y + h; yy++)
            for (int xx = x; xx < x + w; xx++) img.setRGB(xx, yy, argb);
    }
}
