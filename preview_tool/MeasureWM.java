import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MeasureWM {
    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File("D:/QR2CARD/app/src/main/res/drawable/calb_watermark_outline.png"));
        int w = img.getWidth(), h = img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int alpha = (argb >>> 24) & 0xff;
                if (alpha > 16) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        System.out.println("PNG " + w + "x" + h);
        System.out.printf("bbox px: L=%d R=%d T=%d B=%d%n", minX, maxX, minY, maxY);
        System.out.printf("frac: fL=%.4f fR=%.4f fT=%.4f fB=%.4f%n",
            minX / (double) w, (maxX + 1) / (double) w, minY / (double) h, (maxY + 1) / (double) h);
    }
}
