import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class RenderCard {
    static final float CW = 92f, CH = 56f;     // mm
    static final float S = 8f;                 // px per mm (preview scale)
    static int W = Math.round(CW * S);
    static int H = Math.round(CH * S);

    static Font manReg, manBold;

    // mm -> px
    static float x(float mm) { return mm * S; }
    static float y(float mm) { return mm * S; }
    // pt font size -> px : 1pt = 1/72 inch, 1mm = 1/25.4 inch => pt*25.4/72 mm * S px
    static float ptPx(float pt) { return pt * 25.4f / 72f * S; }

    public static void main(String[] args) throws Exception {
        String base = "D:/QR2CARD/app/src/main/assets/fonts/";
        manReg = Font.createFont(Font.TRUETYPE_FONT, new File(base + "Manrope-Regular.otf"));
        manBold = Font.createFont(Font.TRUETYPE_FONT, new File(base + "Manrope-Bold.otf"));

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        Color BLUE = new Color(0x23, 0x49, 0x6b);
        Color WG = new Color(0xce, 0xdb, 0xea);

        // paper background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, W, H);
        // light speckle
        g.setColor(new Color(0xce, 0xdb, 0xea, 16));
        int row = 0;
        for (float yy = y(2.2f); yy < H; yy += y(5.8f), row++) {
            for (float px = x(row % 2 == 0 ? 1.4f : 4.8f); px < W; px += x(8.6f)) {
                g.fill(new Ellipse2D.Float(px, yy, 1.3f, 1.3f));
            }
        }

        // ===== NEW watermark: stroke "CALB" filling card width =====
        {
            float marginMm = 2.5f;
            float targetW = x(CW - marginMm * 2);
            // find font size so "CALB" width == targetW
            float fs = 10f;
            Font f = manBold.deriveFont(fs);
            FontRenderContext frc = g.getFontRenderContext();
            float w = (float) manBold.deriveFont(fs).getStringBounds("CALB", frc).getWidth();
            fs = fs * targetW / w;
            f = manBold.deriveFont(fs);
            GlyphVector gv = f.createGlyphVector(frc, "CALB");
            // baseline near bottom
            Shape outline = gv.getOutline(x(marginMm), y(CH) - y(1.5f));
            g.setColor(WG);
            g.setStroke(new BasicStroke(ptPx(0.7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(outline);
        }

        // ===== Logo (real PNG) =====
        BufferedImage logo = ImageIO.read(new File("D:/QR2CARD/app/src/main/res/drawable/calb_logo.png"));
        drawFit(g, logo, x(8.3f), y(7.35f), x(22.7f), y(7.1f));

        // ===== Company top tracked text =====
        g.setColor(BLUE);
        drawTracked(g, manReg.deriveFont(ptPx(9.2f)), "CALB Group Co., Ltd.", x(46.8f), y(11.5f), x(0.14f));

        // ===== Name (bold) =====
        g.setFont(manBold.deriveFont(ptPx(12.6f)));
        g.drawString("Alex Zhao", x(9f), y(24.2f));

        // ===== Title =====
        g.setFont(manReg.deriveFont(ptPx(6.3f)));
        g.drawString("Director of Pre-sale & Solution", x(9f), y(27.7f));
        // ===== Company line =====
        g.drawString("CALB Americas Inc", x(9f), y(31.5f));

        // ===== Contact block =====
        String[] labels = {"Mobile", "Mail", "Postcode", "Address"};
        String[][] values = {
            {"+14015927928 (US)"},
            {"alex.zhao@calb-tech.com"},
            {"77423"},
            {"839 FM 1489 Rd,", "Brookshire, TX 77423,US"}
        };
        Font lf = manReg.deriveFont(ptPx(6.1f));
        float rowGap = y(3.75f);
        float cy = y(29.95f);
        for (int i = 0; i < labels.length; i++) {
            g.setFont(lf);
            g.drawString(labels[i], x(46.9f), cy);
            for (int j = 0; j < values[i].length; j++) {
                g.drawString(values[i][j], x(57.4f), cy + rowGap * j);
            }
            cy += rowGap;
        }

        g.dispose();
        File out = new File("D:/QR2CARD/preview_tool/card_front.png");
        ImageIO.write(img, "png", out);
        System.out.println("WROTE " + out.getAbsolutePath() + " " + W + "x" + H);
    }

    static void drawTracked(Graphics2D g, Font f, String s, float x, float y, float tracking) {
        g.setFont(f);
        FontRenderContext frc = g.getFontRenderContext();
        float cursor = x;
        for (char c : s.toCharArray()) {
            String cs = String.valueOf(c);
            g.drawString(cs, cursor, y);
            cursor += (float) f.getStringBounds(cs, frc).getWidth() + tracking;
        }
    }

    static void drawFit(Graphics2D g, BufferedImage bmp, float tx, float ty, float tw, float th) {
        float sr = (float) bmp.getWidth() / bmp.getHeight();
        float tr = tw / th;
        float fw = tw, fh = th, fx = tx, fy = ty;
        if (sr > tr) { fh = tw / sr; fy = ty + (th - fh) / 2f; }
        else { fw = th * sr; fx = tx + (tw - fw) / 2f; }
        g.drawImage(bmp, Math.round(fx), Math.round(fy), Math.round(fw), Math.round(fh), null);
    }
}
