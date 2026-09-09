package com.filipelc12.nightout;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import com.filipelc12.nightout.WindowsThemeManager.Theme;

/** Desenha o icone da bandeja em runtime: sol (tema claro) ou lua (tema escuro). */
public final class TrayIconFactory {

    private TrayIconFactory() {
    }

    public static BufferedImage create(Theme theme) {
        int size = 32;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (theme == Theme.LIGHT) {
            drawSun(g, size);
        } else {
            drawMoon(g, size);
        }
        g.dispose();
        return image;
    }

    private static void drawSun(Graphics2D g, int size) {
        Color yellow = new Color(0xFF, 0xB8, 0x00);
        g.setColor(yellow);
        int margin = size / 5;
        int rayLen = margin - 2;
        int cx = size / 2;
        int cy = size / 2;
        int coreRadius = size / 2 - margin;
        g.setStroke(new java.awt.BasicStroke(2f));
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            int x1 = (int) (cx + Math.cos(angle) * (coreRadius + 2));
            int y1 = (int) (cy + Math.sin(angle) * (coreRadius + 2));
            int x2 = (int) (cx + Math.cos(angle) * (coreRadius + 2 + rayLen));
            int y2 = (int) (cy + Math.sin(angle) * (coreRadius + 2 + rayLen));
            g.drawLine(x1, y1, x2, y2);
        }
        g.fill(new Ellipse2D.Double(cx - coreRadius, cy - coreRadius, coreRadius * 2.0, coreRadius * 2.0));
    }

    private static void drawMoon(Graphics2D g, int size) {
        Color silver = new Color(0xCF, 0xD8, 0xDC);
        g.setColor(silver);
        int radius = size / 2 - 3;
        int cx = size / 2;
        int cy = size / 2;
        Ellipse2D full = new Ellipse2D.Double(cx - radius, cy - radius, radius * 2.0, radius * 2.0);
        g.fill(full);
        // "morde" um pedaco pra formar a lua crescente, deixando transparente.
        g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.DST_OUT));
        int biteRadius = radius;
        int offset = radius / 2;
        g.fill(new Ellipse2D.Double(cx - biteRadius + offset, cy - biteRadius - offset / 3.0,
                biteRadius * 2.0, biteRadius * 2.0));
    }
}
