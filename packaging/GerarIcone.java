import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Gera o NightOut.ico usado pelo executavel e pelo instalador: quadrado
 * arredondado dividido na diagonal, dia (sol) em cima e noite (lua) embaixo.
 *
 * Uso (na raiz do projeto):
 *   java "-Djava.awt.headless=true" packaging/GerarIcone.java packaging/NightOut.ico [preview.png]
 * (as aspas no -D sao necessarias no PowerShell)
 *
 * O .ico guarda varios tamanhos (16 a 256 px) como PNG, formato aceito pelo
 * Windows desde o Vista; o Windows escolhe o tamanho certo para cada lugar.
 */
public class GerarIcone {

    private static final int[] TAMANHOS = {16, 24, 32, 48, 64, 128, 256};

    private static final Color CEU_DIA = new Color(0x8E, 0xC9, 0xFF);
    private static final Color CEU_NOITE = new Color(0x1E, 0x2A, 0x4A);
    private static final Color SOL = new Color(0xFF, 0xB8, 0x00);
    private static final Color LUA = new Color(0xE3, 0xE8, 0xEC);

    public static void main(String[] args) throws IOException {
        Path destino = Path.of(args.length > 0 ? args[0] : "packaging/NightOut.ico");
        List<byte[]> pngs = new ArrayList<>();
        for (int tamanho : TAMANHOS) {
            pngs.add(paraPng(desenhar(tamanho)));
        }
        Files.write(destino, montarIco(pngs));
        System.out.println("Icone gerado em " + destino.toAbsolutePath());

        // Opcional: salva um PNG de 256 px pra conferir o desenho.
        if (args.length > 1) {
            Files.write(Path.of(args[1]), pngs.get(pngs.size() - 1));
        }
    }

    static BufferedImage desenhar(int tamanho) {
        BufferedImage img = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double s = tamanho;

        Shape fundo = new RoundRectangle2D.Double(0, 0, s, s, s * 0.36, s * 0.36);
        g.setClip(fundo);

        // Metade de cima/esquerda: dia.
        g.setColor(CEU_DIA);
        g.fill(fundo);

        // Metade de baixo/direita: noite.
        Path2D noite = new Path2D.Double();
        noite.moveTo(s, 0);
        noite.lineTo(s, s);
        noite.lineTo(0, s);
        noite.closePath();
        g.setColor(CEU_NOITE);
        g.fill(noite);

        desenharSol(g, s * 0.32, s * 0.32, s * 0.13, s);
        desenharLua(img, s * 0.68, s * 0.68, s * 0.19);

        g.dispose();
        return img;
    }

    private static void desenharSol(Graphics2D g, double cx, double cy, double raio, double s) {
        g.setColor(SOL);
        g.setStroke(new BasicStroke((float) Math.max(1.0, s * 0.045), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        double inicio = raio * 1.45;
        double fim = raio * 2.05;
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45);
            g.draw(new java.awt.geom.Line2D.Double(
                    cx + Math.cos(a) * inicio, cy + Math.sin(a) * inicio,
                    cx + Math.cos(a) * fim, cy + Math.sin(a) * fim));
        }
        g.fill(new Ellipse2D.Double(cx - raio, cy - raio, raio * 2, raio * 2));
    }

    /** Lua crescente: circulo cheio menos um circulo deslocado (fica transparente e depois pintado de noite). */
    private static void desenharLua(BufferedImage img, double cx, double cy, double raio) {
        BufferedImage camada = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = camada.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(LUA);
        g.fill(new Ellipse2D.Double(cx - raio, cy - raio, raio * 2, raio * 2));
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT));
        double desloc = raio * 0.55;
        g.fill(new Ellipse2D.Double(cx - raio + desloc, cy - raio - desloc * 0.6, raio * 2, raio * 2));
        g.dispose();

        Graphics2D destino = img.createGraphics();
        destino.drawImage(camada, 0, 0, null);
        destino.dispose();
    }

    private static byte[] paraPng(BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    /** Formato ICO: cabecalho de 6 bytes + 16 bytes por imagem + os PNGs, tudo little-endian. */
    private static byte[] montarIco(List<byte[]> pngs) {
        int cabecalho = 6 + 16 * pngs.size();
        int total = cabecalho + pngs.stream().mapToInt(p -> p.length).sum();
        ByteBuffer buf = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort((short) 0);            // reservado
        buf.putShort((short) 1);            // tipo 1 = icone
        buf.putShort((short) pngs.size());

        int offset = cabecalho;
        for (int i = 0; i < pngs.size(); i++) {
            int tamanho = TAMANHOS[i];
            buf.put((byte) (tamanho >= 256 ? 0 : tamanho)); // 0 significa 256
            buf.put((byte) (tamanho >= 256 ? 0 : tamanho));
            buf.put((byte) 0);              // paleta
            buf.put((byte) 0);              // reservado
            buf.putShort((short) 1);        // planos
            buf.putShort((short) 32);       // bits por pixel
            buf.putInt(pngs.get(i).length);
            buf.putInt(offset);
            offset += pngs.get(i).length;
        }
        for (byte[] png : pngs) {
            buf.put(png);
        }
        return buf.array();
    }
}
