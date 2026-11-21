import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 영기 축적 세로 게이지(핫바 오른쪽). 재화 칩과 같은 가죽 테두리·양피지 바탕, 폭 13px.
 *
 * java tools/HudAuraGauge.java common-resources/default/assets/fishing
 * -> aura_frame.png (13 x 52): 위쪽 9x9 기포 아이콘 칸 + 구분선 + 세로 홈(x 3..9, y 15..49)
 *    aura_fill.png  (5 x 33) : 게이지 채움. HUD가 아래부터 잘라(split UP) 비율만큼 그린다. 레이아웃에서 x 4, y 16.
 */
public class HudAuraGauge {
    static final int W = 13, H = 52, FILL_W = 5, FILL_H = 33, FILL_X = 4, FILL_Y = 16;
    static final int OUTLINE = 0xFF1E0E06, RIM = 0xFF6B3A24, RIM_HI = 0xFF9A5A36, RIM_LO = 0xFF3E1E10,
            PAPER = 0xFFEADBB5, PAPER_SHADE = 0xFFCDB98A, PAPER_LIGHT = 0xFFF6ECD0, WELL = 0xFFD8C79C,
            TRACK = 0xFF3E2A1A, TRACK_LO = 0xFF2A1C10;
    static final int AQUA = 0xFF4FD6E8, AQUA_HI = 0xFFB8F4FF, AQUA_LO = 0xFF2A93B8;

    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        ImageIO.write(frame(), "png", new File(out, "aura_frame.png"));
        ImageIO.write(fill(), "png", new File(out, "aura_fill.png"));
        System.out.println("aura gauge -> " + out);
    }

    static BufferedImage frame() {
        var img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < W; x++) for (int y = 0; y < H; y++) {
            boolean corner = (x == 0 || x == W - 1) && (y == 0 || y == H - 1);
            if (corner) continue;
            int c;
            if (x == 0 || y == 0 || x == W - 1 || y == H - 1) c = OUTLINE;
            else if (x == 1 || y == 1) c = RIM_HI;
            else if (x == W - 2 || y == H - 2) c = RIM_LO;
            else c = x == 2 ? PAPER_LIGHT : x == W - 3 ? PAPER_SHADE : PAPER;
            img.setRGB(x, y, c);
        }
        // 아이콘 칸(2..10) + 가로 구분선(y 12)
        for (int x = 2; x <= 10; x++) for (int y = 2; y <= 10; y++) img.setRGB(x, y, WELL);
        for (int x = 2; x <= 10; x++) img.setRGB(x, 12, RIM);
        bubble(img, 2, 2);
        // 세로 홈: 바깥 3..9 x 15..49, 안쪽 채움 영역 4..8 x 16..48 (5 x 33)
        for (int x = FILL_X - 1; x <= FILL_X + FILL_W; x++) for (int y = FILL_Y - 1; y <= FILL_Y + FILL_H; y++) {
            boolean edge = x == FILL_X - 1 || x == FILL_X + FILL_W || y == FILL_Y - 1 || y == FILL_Y + FILL_H;
            img.setRGB(x, y, edge ? RIM_LO : (x == FILL_X ? TRACK_LO : TRACK));
        }
        // 25% 눈금
        for (int i = 1; i < 4; i++) {
            int y = FILL_Y + FILL_H - Math.round(FILL_H * i / 4f);
            img.setRGB(FILL_X - 1, y, RIM_HI); img.setRGB(FILL_X + FILL_W, y, RIM_HI);
        }
        return img;
    }

    /** 9x9 기포 픽셀 아트. */
    static void bubble(BufferedImage img, int ox, int oy) {
        String[] rows = {
                "..#####..",
                ".#ooooo#.",
                "#oHHoooo#",
                "#oHooooo#",
                "#ooooooo#",
                "#ooooooo#",
                "#oooooLo#",
                ".#ooLLo#.",
                "..#####.."};
        for (int y = 0; y < 9; y++) for (int x = 0; x < 9; x++) {
            int c = switch (rows[y].charAt(x)) {
                case '#' -> AQUA_LO; case 'o' -> AQUA; case 'H' -> AQUA_HI; case 'L' -> 0xFF7FE4F2; default -> 0;
            };
            if (c != 0) img.setRGB(ox + x, oy + y, c);
        }
    }

    static BufferedImage fill() {
        var img = new BufferedImage(FILL_W, FILL_H, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < FILL_W; x++) for (int y = 0; y < FILL_H; y++)
            img.setRGB(x, y, x == 0 ? AQUA_HI : x == FILL_W - 1 ? AQUA_LO : AQUA);
        return img;
    }
}
