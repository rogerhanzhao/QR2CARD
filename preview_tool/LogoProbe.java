import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class LogoProbe {
    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File("D:/QR2CARD/app/src/main/res/drawable/calb_logo.png"));
        int w = img.getWidth(), h = img.getHeight();
        boolean hasAlpha = img.getColorModel().hasAlpha();
        System.out.println("dims " + w + "x" + h + " hasAlpha=" + hasAlpha);
        // sample corners
        for (int[] p : new int[][]{{0,0},{w-1,0},{0,h-1},{w-1,h-1},{w/2,h/2}}) {
            int argb = img.getRGB(p[0], p[1]);
            System.out.printf("px(%d,%d) a=%d r=%d g=%d b=%d%n", p[0], p[1],
                (argb>>>24)&0xff, (argb>>16)&0xff, (argb>>8)&0xff, argb&0xff);
        }
        // define "ink": dark-ish and visible
        // print row ink counts (downsampled to 60 bands) to find letters/tagline gap
        int bands = 60;
        for (int b = 0; b < bands; b++) {
            int y0 = b*h/bands, y1 = (b+1)*h/bands;
            System.out.printf("band %2d y[%4d-%4d] ink=%d%n", b, y0, y1, rowInk(img,y0,y1,w,hasAlpha));
        }
    }
    static long rowInk(BufferedImage img,int y0,int y1,int w,boolean hasAlpha){
        long c=0;
        for(int y=y0;y<y1;y++)for(int x=0;x<w;x++){
            int argb=img.getRGB(x,y); int al=(argb>>>24)&0xff;
            int r=(argb>>16)&0xff,g=(argb>>8)&0xff,b=argb&0xff;
            boolean ink= hasAlpha? al>96 : (r+g+b)<600;
            if(ink)c++;
        }
        return c;
    }
}
