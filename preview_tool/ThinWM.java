import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ThinWM {
    public static void main(String[] a) throws Exception {
        // args: <srcPng> <dstPng> <radiusPx>
        String src = a[0], dst = a[1];
        int r = Integer.parseInt(a[2]);
        BufferedImage img = ImageIO.read(new File(src));
        int w = img.getWidth(), h = img.getHeight();

        // extract alpha
        int[] alpha = new int[w * h];
        int rgbColor = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                alpha[y * w + x] = (argb >>> 24) & 0xff;
                if (((argb >>> 24) & 0xff) > 64) rgbColor = argb & 0xffffff;
            }
        }

        // erode alpha: min over disk of radius r
        int[] out = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int m = 255;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= h) { m = 0; break; }
                    for (int dx = -r; dx <= r; dx++) {
                        if (dx * dx + dy * dy > r * r) continue;
                        int xx = x + dx;
                        if (xx < 0 || xx >= w) { m = 0; break; }
                        int v = alpha[yy * w + xx];
                        if (v < m) m = v;
                    }
                    if (m == 0) break;
                }
                out[y * w + x] = m;
            }
        }

        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int al = out[y * w + x];
                res.setRGB(x, y, (al << 24) | rgbColor);
            }
        }
        ImageIO.write(res, "png", new File(dst));
        System.out.println("WROTE " + dst + " erode r=" + r);
    }
}
