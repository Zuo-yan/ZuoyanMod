import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 生成空间锚点物品贴图（16x16 像素画，透明背景）。
 * 用法：java GenSpaceAnchorTexture.java <输出png路径>
 * 改配色只需要调 COLORS 里的 ARGB 值。
 */
public class GenSpaceAnchorTexture {

    // T = 青色传送门光环, S = 紫金亮部, D = 紫金暗部, '.' = 透明
    private static final String[] ROWS = {
            "................",
            "....TTTTTTTT....",
            "...T........T...",
            "..T...SSDD...T..",
            "..T..S....D..T..",
            ".T...S....D...T.",
            ".T.SSSSSSDDDD.T.",
            ".T.....SD.....T.",
            ".T.....SD.....T.",
            "..T....SD....T..",
            "...T...SD...T...",
            "....T.S..D.T....",
            "...T.S....D.T...",
            "...TS......DT...",
            "....S......D....",
            "................"
    };

    private static final int COLOR_T = 0xFF2BB7A8; // 青色光环
    private static final int COLOR_S = 0xFFF5D98A; // 紫金亮部
    private static final int COLOR_D = 0xFFB07A1E; // 紫金暗部
    private static final int COLOR_EMPTY = 0x00000000;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("usage: java GenSpaceAnchorTexture.java <output.png>");
            return;
        }
        int size = 16;
        for (int y = 0; y < size; y++) {
            if (ROWS[y].length() != size) {
                throw new IllegalStateException("row " + y + " length = " + ROWS[y].length() + ", expected " + size);
            }
        }

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < size; y++) {
            String row = ROWS[y];
            for (int x = 0; x < size; x++) {
                img.setRGB(x, y, switch (row.charAt(x)) {
                    case 'T' -> COLOR_T;
                    case 'S' -> COLOR_S;
                    case 'D' -> COLOR_D;
                    default -> COLOR_EMPTY;
                });
            }
        }

        File out = new File(args[0]);
        out.getParentFile().mkdirs();
        ImageIO.write(img, "png", out);
        System.out.println("written: " + out.getAbsolutePath());
    }
}
