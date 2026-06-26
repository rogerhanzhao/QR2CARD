import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Scale {
    // args: <src> <dst> <maxDim>
    public static void main(String[] a) throws Exception {
        BufferedImage src = ImageIO.read(new File(a[0]));
        int max = Integer.parseInt(a[2]);
        int w = src.getWidth(), h = src.getHeight();
        double s = Math.min(1.0, (double) max / Math.max(w, h));
        int nw = (int) Math.round(w * s), nh = (int) Math.round(h * s);
        Image scaled = src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
        BufferedImage out = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
        out.getGraphics().drawImage(scaled, 0, 0, null);
        ImageIO.write(out, "png", new File(a[1]));
        System.out.println("scaled " + w + "x" + h + " -> " + nw + "x" + nh);
    }
}
