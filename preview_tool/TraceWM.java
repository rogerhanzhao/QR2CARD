import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class TraceWM {
    // args: <strokeR_targetPx>   e.g. 1.0  (smaller = thinner)
    public static void main(String[] a) throws Exception {
        double strokeTargetPx = a.length > 0 ? Double.parseDouble(a[0]) : 1.0;
        int SS = 3;                       // supersample
        int rWork = (int)Math.max(1, Math.round(strokeTargetPx * SS / 2.0)); // gradient half-width

        BufferedImage logo = ImageIO.read(new File("D:/QR2CARD/app/src/main/res/drawable/calb_logo.png"));
        int lw = logo.getWidth(), lh = logo.getHeight();

        // --- isolate big CALB letters: y in [0, 800) ---
        int yLimit = 800;
        int sx0=lw, sy0=lh, sx1=-1, sy1=-1;
        for (int y=0; y<yLimit; y++) for (int x=0; x<lw; x++) {
            if (((logo.getRGB(x,y)>>>24)&0xff) > 96) {
                if (x<sx0)sx0=x; if (x>sx1)sx1=x; if (y<sy0)sy0=y; if (y>sy1)sy1=y;
            }
        }
        System.out.printf("CALB src bbox: x[%d-%d] y[%d-%d]  aspect=%.3f%n",
            sx0,sx1,sy0,sy1,(sx1-sx0+1)/(double)(sy1-sy0+1));

        // --- target canvas (match current watermark png) ---
        int TW=1565, TH=953;
        int tx0=72, tx1=1497, ty0=655, ty1=882;   // letter bbox in target

        int WW=TW*SS, WH=TH*SS;
        // build binary coverage of letters in work canvas
        boolean[] bin = new boolean[WW*WH];
        int wtx0=tx0*SS, wtx1=(tx1+1)*SS, wty0=ty0*SS, wty1=(ty1+1)*SS;
        double srcW=(sx1-sx0+1), srcH=(sy1-sy0+1);
        for (int wy=wty0; wy<wty1; wy++) {
            double v=(wy-wty0)/(double)(wty1-wty0);
            double syf=sy0 + v*(srcH-1);
            for (int wx=wtx0; wx<wtx1; wx++) {
                double u=(wx-wtx0)/(double)(wtx1-wtx0);
                double sxf=sx0 + u*(srcW-1);
                int al = bilinearAlpha(logo, sxf, syf);
                if (al>128) bin[wy*WW+wx]=true;
            }
        }

        // morphological gradient: edge = dilate(r) AND NOT erode(r)
        boolean[] er = morph(bin, WW, WH, rWork, false);
        boolean[] di = morph(bin, WW, WH, rWork, true);
        boolean[] edge = new boolean[WW*WH];
        for (int i=0;i<edge.length;i++) edge[i]= di[i] && !er[i];

        // downscale SS->1 with box averaging => AA alpha
        int rgb = 0xcedbea;
        BufferedImage out = new BufferedImage(TW, TH, BufferedImage.TYPE_INT_ARGB);
        int ss2=SS*SS;
        for (int ty=0; ty<TH; ty++) {
            for (int tx=0; tx<TW; tx++) {
                int cnt=0;
                for (int dy=0; dy<SS; dy++) {
                    int wy=ty*SS+dy;
                    int base=wy*WW + tx*SS;
                    for (int dx=0; dx<SS; dx++) if (edge[base+dx]) cnt++;
                }
                int al = (int)Math.round(255.0*cnt/ss2);
                out.setRGB(tx,ty,(al<<24)|rgb);
            }
        }
        File dst=new File("D:/QR2CARD/app/src/main/res/drawable/calb_watermark_outline.png");
        ImageIO.write(out,"png",dst);
        System.out.println("WROTE "+dst+"  strokeTargetPx="+strokeTargetPx+" rWork="+rWork);
    }

    static int bilinearAlpha(BufferedImage img,double x,double y){
        int x0=(int)Math.floor(x), y0=(int)Math.floor(y);
        int x1=Math.min(x0+1,img.getWidth()-1), y1=Math.min(y0+1,img.getHeight()-1);
        x0=Math.max(0,x0); y0=Math.max(0,y0);
        double fx=x-x0, fy=y-y0;
        int a00=(img.getRGB(x0,y0)>>>24)&0xff, a10=(img.getRGB(x1,y0)>>>24)&0xff;
        int a01=(img.getRGB(x0,y1)>>>24)&0xff, a11=(img.getRGB(x1,y1)>>>24)&0xff;
        double top=a00*(1-fx)+a10*fx, bot=a01*(1-fx)+a11*fx;
        return (int)Math.round(top*(1-fy)+bot*fy);
    }

    // min(erode)/max(dilate) over disk radius r
    static boolean[] morph(boolean[] in,int w,int h,int r,boolean dilate){
        boolean[] out=new boolean[w*h];
        for (int y=0;y<h;y++) for (int x=0;x<w;x++){
            boolean res = dilate ? false : true;
            outer:
            for (int dy=-r;dy<=r;dy++){
                int yy=y+dy; if(yy<0||yy>=h){ if(!dilate){res=false; break;} else continue; }
                for (int dx=-r;dx<=r;dx++){
                    if (dx*dx+dy*dy>r*r) continue;
                    int xx=x+dx; if(xx<0||xx>=w){ if(!dilate){res=false; break outer;} else continue; }
                    boolean v=in[yy*w+xx];
                    if (dilate){ if(v){res=true; break outer;} }
                    else { if(!v){res=false; break outer;} }
                }
            }
            out[y*w+x]=res;
        }
        return out;
    }
}
