import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 다중 투척 스킬 칸(핫바 왼쪽). 재화 칩·영기 게이지와 같은 가죽 테두리.
 *
 * java tools/HudMultiCast.java common-resources/default/assets/fishing common-resources/default/fonts/galmuri11.ttf
 * -> multicast_frame.png    (20 x 20): 1px 외곽 + 1px 가죽 테두리, 안쪽 16x16 아이콘 칸(x 2, y 2)
 *    multicast_cooldown.png (16 x 16): 쿨타임 어둡게 덮개. HUD가 아래부터 잘라(split UP) 남은 비율만큼 그린다.
 *    multicast_reserved.png (20 x 20): 예약됨 표시용 금색 테두리(틀 위에 겹침)
 *    multicast_time.png     : 남은 시간 글꼴(Galmuri11, "0123456789ms", 높이 8px, 흰색). 표기: 1분 이상 "13m", 미만 "23s"
 *    multicast_icon.png     (16 x 16): 스킬 아이콘. 파일이 이미 있으면 덮어쓰지 않는다(직접 그린 아이콘 보존). 없을 때만 임시 아이콘 생성.
 */
public class HudMultiCast {
    static final int W = 20, ICON = 16;
    static final int OUTLINE = 0xFF1E0E06, RIM = 0xFF6B3A24, RIM_HI = 0xFF9A5A36, RIM_LO = 0xFF3E1E10, WELL = 0xFFD8C79C;
    static final int GOLD = 0xFFFFD84A, GOLD_LO = 0xFFC08A1E;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        ImageIO.write(frame(), "png", new File(out, "multicast_frame.png"));
        ImageIO.write(cooldown(), "png", new File(out, "multicast_cooldown.png"));
        ImageIO.write(reserved(), "png", new File(out, "multicast_reserved.png"));
        File icon = new File(out, "multicast_icon.png");
        if (!icon.exists()) ImageIO.write(placeholderIcon(), "png", icon);
        if (args.length > 1) {
            Font font = Font.createFont(Font.TRUETYPE_FONT, new File(args[1])).deriveFont(10f);
            ImageIO.write(timeFont(font), "png", new File(out, "multicast_time.png"));
        }
        System.out.println("multicast -> " + out);
    }

    static BufferedImage frame() {
        var img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < W; x++) for (int y = 0; y < W; y++) {
            boolean corner = (x == 0 || x == W - 1) && (y == 0 || y == W - 1);
            if (corner) continue;
            int c;
            if (x == 0 || y == 0 || x == W - 1 || y == W - 1) c = OUTLINE;
            else if (x == 1 || y == 1) c = RIM_HI;
            else if (x == W - 2 || y == W - 2) c = RIM_LO;
            else c = WELL;
            img.setRGB(x, y, c);
        }
        return img;
    }

    static BufferedImage cooldown() {
        var img = new BufferedImage(ICON, ICON, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < ICON; x++) for (int y = 0; y < ICON; y++) img.setRGB(x, y, 0xB0101010);
        return img;
    }

    static BufferedImage reserved() {
        var img = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < W; x++) for (int y = 0; y < W; y++) {
            boolean corner = (x == 0 || x == W - 1) && (y == 0 || y == W - 1);
            if (corner) continue;
            if (x == 0 || y == 0 || x == W - 1 || y == W - 1) img.setRGB(x, y, GOLD_LO);
            else if (x == 1 || y == 1 || x == W - 2 || y == W - 2) img.setRGB(x, y, GOLD);
        }
        return img;
    }

    /** 임시 아이콘: 낚시찌 3개. 직접 그린 16x16으로 바꾼다. */
    static BufferedImage placeholderIcon() {
        var img = new BufferedImage(ICON, ICON, BufferedImage.TYPE_INT_ARGB);
        int[][] at = {{1, 6}, {6, 2}, {11, 6}};
        for (int[] p : at) {
            int ox = p[0], oy = p[1];
            for (int x = 0; x < 4; x++) for (int y = 0; y < 8; y++) {
                boolean edge = (x == 0 || x == 3) && (y == 0 || y == 7);
                if (edge) continue;
                img.setRGB(ox + x, oy + y, y < 4 ? 0xFFE04040 : 0xFFF2F2F2);
            }
            img.setRGB(ox + 1, oy + 1, 0xFFFF9A9A);
        }
        return img;
    }

    static BufferedImage timeFont(Font font) {
        // 재화 숫자(HudChips.balanceFont)와 같은 8줄: 9px 숫자에서 반복 줄 두 개를 뺀다.
        int[] rows = {1, 2, 4, 5, 6, 8, 9, 10};
        String glyphs = "0123456789ms";
        int cellWidth = 8; // m 이 6px보다 넓다
        var atlas = new BufferedImage(cellWidth * glyphs.length(), rows.length, BufferedImage.TYPE_INT_ARGB);
        var context = new FontRenderContext(null, true, true);
        for (int i = 0; i < glyphs.length(); i++) {
            char[] glyph = {glyphs.charAt(i)};
            var nativeGlyph = new BufferedImage(10, 14, BufferedImage.TYPE_INT_ARGB);
            var graphics = nativeGlyph.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fill(font.createGlyphVector(context, glyph).getOutline(0, 10));
            graphics.dispose();
            int left = nativeGlyph.getWidth(), right = -1;
            for (int row : rows) for (int x = 0; x < nativeGlyph.getWidth(); x++)
                if ((nativeGlyph.getRGB(x, row) >>> 24) != 0) { left = Math.min(left, x); right = Math.max(right, x); }
            if (left > right || right - left >= cellWidth) throw new IllegalStateException("Unexpected glyph width: " + glyph[0]);
            for (int y = 0; y < rows.length; y++) for (int x = left; x <= right; x++)
                atlas.setRGB(i * cellWidth + x - left, y, nativeGlyph.getRGB(x, rows[y]));
        }
        return atlas;
    }
}
