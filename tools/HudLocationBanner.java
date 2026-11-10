import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Builds the location banner plates in the same CoreUi book style as the currency chips
 * (dark outline, lit leather rim, parchment face), dressed as a ribbon: swallow-tailed leather ends tucked behind the
 * plate, small gold studs either side of the title and leather corner brackets.
 *
 * java tools/HudLocationBanner.java common-resources/default/assets/location
 * -> banner.png (176 x 20, one line: region name or biome)
 * -> banner_tall.png (176 x 34, two lines: biome, then structure under a dotted rule)
 *
 * Drawn at final size (HUD scale 1.0). Text is Galmuri11 at its native 10 px grid (9 px Hangul ink):
 * line 1 ink rows 4..12 (layout y 2), line 2 ink rows 18..26 (layout y 16), centred by the layout.
 */
public class HudLocationBanner {
    static final int W = 176, TAIL = 8;
    static final int OUTLINE = 0xFF1E0E06, RIM = 0xFF6B3A24, RIM_HI = 0xFF9A5A36, RIM_LO = 0xFF3E1E10,
            TAIL_FACE = 0xFF55301C, TAIL_HI = 0xFF7A4429,
            PAPER = 0xFFEADBB5, PAPER_SHADE = 0xFFCDB98A, PAPER_LIGHT = 0xFFF6ECD0,
            GOLD = 0xFFD8A940, GOLD_HI = 0xFFF5DA8A, GOLD_LO = 0xFF9C6F1E;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        ImageIO.write(banner(17, false), "png", new File(out, "banner.png"));
        ImageIO.write(banner(31, true), "png", new File(out, "banner_tall.png"));
        System.out.println("banners -> " + out);
    }

    /** @param bodyH plate height including outline; the tails hang 3 px below it. */
    static BufferedImage banner(int bodyH, boolean twoLines) {
        int H = bodyH + 3;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        tail(img, 0, 4, H - 4, false);
        tail(img, W - TAIL - 6, 4, H - 4, true);

        int x0 = TAIL, x1 = W - TAIL - 1, bw = x1 - x0 + 1; // plate x range
        fill(img, x0, 0, bw, bodyH, OUTLINE);
        fill(img, x0 + 1, 1, bw - 2, bodyH - 2, RIM);
        fill(img, x0 + 1, 1, bw - 2, 1, RIM_HI);
        fill(img, x0 + 1, 1, 1, bodyH - 2, RIM_HI);
        fill(img, x0 + 1, bodyH - 2, bw - 2, 1, RIM_LO);
        fill(img, x1 - 1, 1, 1, bodyH - 2, RIM_LO);
        // Parchment face with a lit top edge and a soft lower edge.
        int fx0 = x0 + 2, fx1 = x1 - 2, fy0 = 2, fy1 = bodyH - 3;
        fill(img, fx0, fy0, fx1 - fx0 + 1, fy1 - fy0 + 1, PAPER);
        fill(img, fx0, fy0, fx1 - fx0 + 1, 1, PAPER_LIGHT);
        fill(img, fx0, fy1, fx1 - fx0 + 1, 1, PAPER_SHADE);
        // Leather corner brackets inside the face.
        bracket(img, fx0 + 1, fy0 + 1, 1, 1);
        bracket(img, fx1 - 1, fy0 + 1, -1, 1);
        bracket(img, fx0 + 1, fy1 - 1, 1, -1);
        bracket(img, fx1 - 1, fy1 - 1, -1, -1);
        // Gold studs level with the first text line (ink rows 4..12, centre 8).
        stud(img, fx0 + 7, 8);
        stud(img, fx1 - 7, 8);
        if (twoLines) {
            // Dotted rule between the lines with a small stud in the middle.
            int y = 15, cx = W / 2;
            for (int x = fx0 + 12; x <= fx1 - 12; x++) {
                if (Math.abs(x - cx) <= 3) continue;
                if ((x & 1) == 0) img.setRGB(x, y, PAPER_SHADE);
            }
            img.setRGB(cx, y - 1, GOLD_HI);
            img.setRGB(cx - 1, y, GOLD_LO);
            img.setRGB(cx, y, GOLD);
            img.setRGB(cx + 1, y, GOLD_LO);
            img.setRGB(cx, y + 1, GOLD_LO);
        }
        return img;
    }

    /** Ribbon end: dark leather strip with a swallow-tail notch on the outer side. */
    static void tail(BufferedImage img, int x, int y, int h, boolean right) {
        int w = TAIL + 6, mid = (h - 1) / 2;
        for (int r = 0; r < h; r++) {
            int notch = Math.max(0, 4 - Math.abs(r - mid));
            for (int c = 0; c < w; c++) {
                int outer = right ? w - 1 - c : c; // distance from the free end
                if (outer < notch) continue;
                boolean edge = r == 0 || r == h - 1 || outer == notch;
                int color = edge ? OUTLINE : (r == 1 ? TAIL_HI : TAIL_FACE);
                img.setRGB(x + c, y + r, color);
            }
        }
    }

    static void bracket(BufferedImage img, int x, int y, int dx, int dy) {
        img.setRGB(x, y, RIM);
        img.setRGB(x + dx, y, RIM);
        img.setRGB(x, y + dy, RIM);
    }

    /** 5 x 5 gold diamond, lit from the top left. */
    static void stud(BufferedImage img, int cx, int cy) {
        int[][] shape = {
                {0, 0, 1, 0, 0},
                {0, 1, 2, 1, 0},
                {1, 2, 3, 4, 1},
                {0, 1, 4, 1, 0},
                {0, 0, 1, 0, 0},
        };
        int[] colors = {0, OUTLINE, GOLD_HI, GOLD, GOLD_LO};
        for (int r = 0; r < 5; r++)
            for (int c = 0; c < 5; c++)
                if (shape[r][c] != 0) img.setRGB(cx - 2 + c, cy - 2 + r, colors[shape[r][c]]);
    }

    static void fill(BufferedImage img, int x, int y, int w, int h, int argb) {
        for (int yy = y; yy < y + h; yy++)
            for (int xx = x; xx < x + w; xx++) img.setRGB(xx, yy, argb);
    }
}
