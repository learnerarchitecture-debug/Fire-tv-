import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the OBAA Home launcher icon (all densities) and the TV
 * banner with plain Java2D — no Android toolchain needed for the art.
 */
public class GenIcon {

    private static final Color C1 = new Color(0xFFB45E);
    private static final Color C2 = new Color(0xA99BFF);
    private static final Color C_DEEP = new Color(0x120B08);

    public static void main(String[] args) throws Exception {
        File res = new File(args[0]);
        Object[][] sizes = {{48, "mdpi"}, {72, "hdpi"}, {96, "xhdpi"}, {144, "xxhdpi"}, {192, "xxxhdpi"}};
        for (Object[] s : sizes) {
            File f = new File(res, "mipmap-" + s[1] + "/ic_launcher.png");
            f.getParentFile().mkdirs();
            ImageIO.write(renderIcon((Integer) s[0]), "png", f);
        }
        File b = new File(res, "drawable-xhdpi/banner.png");
        b.getParentFile().mkdirs();
        ImageIO.write(renderBanner(), "png", b);
        System.out.println("OBAA icons generated in " + res);
    }

    private static BufferedImage renderIcon(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        float r = s * 0.23f;
        g.setClip(new RoundRectangle2D.Float(0, 0, s, s, r, r));
        g.setPaint(new GradientPaint(0, 0, C1, s * 0.55f, s, C2));
        g.fillRect(0, 0, s, s);
        g.setPaint(new GradientPaint(0, s * 0.30f, new Color(0x00120B08, true),
                0, s, new Color(0xD9120B08, true)));
        g.fillRect(0, 0, s, s);

        g.setColor(new Color(0x33FFFFFF, true));
        g.fillOval(Math.round(s * 0.55f), Math.round(-s * 0.18f),
                Math.round(s * 0.62f), Math.round(s * 0.62f));

        // Three nested aurora arcs: the OBAA mark.
        g.setColor(new Color(0xF5FFFFFF, true));
        g.setStroke(new BasicStroke(s * 0.072f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawArcs(g, s * 0.5f, s * 0.5f, s * 0.34f, s * 0.24f);
        g.dispose();
        return img;
    }

    private static void drawArcs(Graphics2D g, float cx, float cy, float w, float h) {
        g.drawArc(Math.round(cx - w), Math.round(cy - h), Math.round(w * 2), Math.round(h * 2),
                205, 130);
        float w2 = w * 0.76f, h2 = h * 0.76f;
        g.drawArc(Math.round(cx - w2), Math.round(cy - h2 + h * 0.18f),
                Math.round(w2 * 2), Math.round(h2 * 2), 205, 130);
        float w3 = w * 0.52f, h3 = h * 0.52f;
        g.drawArc(Math.round(cx - w3), Math.round(cy - h3 + h * 0.36f),
                Math.round(w3 * 2), Math.round(h3 * 2), 205, 130);
    }

    private static BufferedImage renderBanner() {
        int w = 320, h = 180;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, C1, w * 0.55f, h, C2));
        g.fillRect(0, 0, w, h);
        g.setPaint(new GradientPaint(0, h * 0.30f, new Color(0x00120B08, true),
                0, h, new Color(0xE6120B08, true)));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0x33FFFFFF, true));
        g.fillOval(Math.round(w * 0.72f), -40, 170, 170);

        g.setColor(new Color(0xF5FFFFFF, true));
        g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawArcs(g, 52f, 90f, 26f, 20f);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.drawString("OBAA", 92, 84);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(0xE6FFFFFF, true));
        g.drawString("HOME \u00B7 GLASS", 94, 110);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(0xB3FFFFFF, true));
        g.drawString("CURATED FOR FIRE TV", 94, 132);
        g.dispose();
        return img;
    }
}
