package com.arena.tools;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;

/**
 * Pinta tiles, props, efeitos e icones em pixel art (32 px = 1 metro), usando as paletas
 * extraidas (k-means) de cada painel da ilustracao de biomas.
 */
final class Painter {
    static final String[] BIOMES = {"crypt", "forest", "ruins"};
    // Paineis da imagem biomes.jpg: floresta | cripta | ruinas (x0, x1)
    private static final int[][] PANELS = {{668, 1333}, {0, 662}, {1338, 2000}};

    private static Random rnd = new Random(99);

    static void paintAll(Packer p, BufferedImage biomes) {
        for (int b = 0; b < 3; b++) {
            int[] pal = palette(biomes, PANELS[b][0], 75, PANELS[b][1], biomes.getHeight(), 10);
            System.out.println(BIOMES[b] + " paleta: " + hex(pal));
            String n = BIOMES[b];
            for (int v = 0; v < 4; v++) p.add("floor_" + n, v, floor(b, pal, v));
            for (int v = 0; v < 2; v++) p.add("wall_" + n, v, wall(b, pal, v));
            p.add("wallcap_" + n, wallCap(pal));
        }
        int[] crypt = palette(biomes, PANELS[0][0], 75, PANELS[0][1], biomes.getHeight(), 10);
        int[] forest = palette(biomes, PANELS[1][0], 75, PANELS[1][1], biomes.getHeight(), 10);
        int[] ruins = palette(biomes, PANELS[2][0], 75, PANELS[2][1], biomes.getHeight(), 10);

        // Cripta
        p.add("prop_pillar", pillar(crypt, false));
        p.add("prop_sarcophagus", sarcophagus(crypt));
        p.add("prop_bones", bones());
        p.add("decal_rug", 0, rug(Img.rgb(98, 30, 42), Img.rgb(150, 110, 60)));
        p.add("decal_rug", 1, rug(Img.rgb(66, 36, 88), Img.rgb(140, 120, 150)));
        p.add("decal_crack", crack(crypt[0], 32, 32, 3));
        for (int f = 0; f < 3; f++) p.add("prop_torch", f, torch(f));
        // Floresta
        p.add("prop_tree", deadTree());
        p.add("prop_mushroom", 0, mushrooms(Img.rgb(80, 190, 255), Img.rgb(30, 90, 160)));
        p.add("prop_mushroom", 1, mushrooms(Img.rgb(210, 100, 240), Img.rgb(110, 40, 140)));
        p.add("prop_rock", 0, rock(forest, true));
        p.add("decal_puddle", puddle());
        p.add("decal_roots", roots());
        // Ruinas
        p.add("prop_ruinwall", ruinWall(ruins));
        p.add("prop_rock", 1, rock(ruins, false));
        p.add("prop_column", pillar(ruins, true));
        p.add("prop_shrub", shrub());
        p.add("decal_rift", rift());
        for (int f = 0; f < 3; f++) p.add("prop_campfire", f, campfire(f));

        // Efeitos e objetos
        p.add("arrow", arrow());
        for (int f = 0; f < 2; f++) p.add("fireball", f, fireball(f));
        p.add("gem", gem());
        p.add("chest", 0, chest(false));
        p.add("chest", 1, chest(true));
        p.add("meteor", meteor());
        p.add("shadow", shadow());
        p.add("portal", portal());
        p.add("shield", shield());
        p.add("light", light(128));
        p.add("soft", light(16));
        p.add("pixel", solid(4, 4, 0xFFFFFFFF));
        p.add("ring", ring(64, 3));

        // Icones das habilidades especiais (no mesmo estilo de moldura dos icones extraidos)
        p.add("icon_sp_arrows", iconArrows());
        p.add("icon_sp_ice", iconIce());
        p.add("icon_sp_chain", iconChain());
        p.add("icon_sp_meteor", iconMeteor());
    }

    // =================================================================== paleta (k-means)

    static int[] palette(BufferedImage im, int x0, int y0, int x1, int y1, int k) {
        int n = 0;
        int[] samples = new int[((x1 - x0) / 3 + 1) * ((y1 - y0) / 3 + 1)];
        for (int y = y0; y < y1; y += 3) for (int x = x0; x < x1; x += 3) samples[n++] = im.getRGB(x, y);
        samples = Arrays.copyOf(samples, n);
        double[][] c = new double[k][3];
        Random r = new Random(5);
        for (int i = 0; i < k; i++) { int s = samples[r.nextInt(n)]; c[i] = new double[]{Img.r(s), Img.g(s), Img.b(s)}; }
        int[] assign = new int[n];
        for (int it = 0; it < 20; it++) {
            double[][] sum = new double[k][4];
            for (int i = 0; i < n; i++) {
                int s = samples[i], best = 0; double bd = 1e18;
                for (int j = 0; j < k; j++) {
                    double dr = Img.r(s) - c[j][0], dg = Img.g(s) - c[j][1], db = Img.b(s) - c[j][2];
                    double d = dr * dr + dg * dg + db * db;
                    if (d < bd) { bd = d; best = j; }
                }
                assign[i] = best;
                sum[best][0] += Img.r(s); sum[best][1] += Img.g(s); sum[best][2] += Img.b(s); sum[best][3]++;
            }
            for (int j = 0; j < k; j++) if (sum[j][3] > 0) for (int q = 0; q < 3; q++) c[j][q] = sum[j][q] / sum[j][3];
        }
        // Cores muito saturadas (fogo, cogumelos) sao destaques, nao base: ficam de fora
        java.util.List<Integer> keep = new java.util.ArrayList<>();
        for (int i = 0; i < k; i++) {
            double mx = Math.max(c[i][0], Math.max(c[i][1], c[i][2])), mn = Math.min(c[i][0], Math.min(c[i][1], c[i][2]));
            if (mx <= 0 || (mx - mn) / mx < 0.45) keep.add(i);
        }
        keep.sort((a, b) -> Double.compare(lum(c[a]), lum(c[b])));
        int[] pal = new int[8];
        for (int i = 0; i < 8; i++) {
            int j = keep.get(Math.min(keep.size() - 1, i * keep.size() / 8));
            pal[i] = Img.rgb((int) c[j][0], (int) c[j][1], (int) c[j][2]);
        }
        return pal;
    }

    private static double lum(double[] c) { return c[0] * 0.3 + c[1] * 0.59 + c[2] * 0.11; }

    private static String hex(int[] pal) {
        StringBuilder sb = new StringBuilder();
        for (int c : pal) sb.append(String.format("#%06X ", c & 0xFFFFFF));
        return sb.toString();
    }

    // =================================================================== primitivas

    static int shade(int c, int d) { return Img.rgb(Img.r(c) + d, Img.g(c) + d, Img.b(c) + d); }

    static int mix(int a, int b, double t) {
        return Img.rgb((int) (Img.r(a) + (Img.r(b) - Img.r(a)) * t), (int) (Img.g(a) + (Img.g(b) - Img.g(a)) * t),
                (int) (Img.b(a) + (Img.b(b) - Img.b(a)) * t));
    }

    static int alpha(int c, int a) { return (Img.clamp(a) << 24) | (c & 0xFFFFFF); }

    static void set(BufferedImage im, int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < im.getWidth() && y < im.getHeight()) im.setRGB(x, y, c);
    }

    static int get(BufferedImage im, int x, int y) {
        return x >= 0 && y >= 0 && x < im.getWidth() && y < im.getHeight() ? im.getRGB(x, y) : 0;
    }

    static void rect(BufferedImage im, int x, int y, int w, int h, int c) {
        for (int j = y; j < y + h; j++) for (int i = x; i < x + w; i++) set(im, i, j, c);
    }

    static void ellipse(BufferedImage im, double cx, double cy, double rx, double ry, int c) {
        for (int y = (int) (cy - ry); y <= cy + ry; y++)
            for (int x = (int) (cx - rx); x <= cx + rx; x++) {
                double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry;
                if (dx * dx + dy * dy <= 1) set(im, x, y, c);
            }
    }

    static void line(BufferedImage im, int x0, int y0, int x1, int y1, int c, int thick) {
        int dx = Math.abs(x1 - x0), dy = -Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1, err = dx + dy;
        while (true) {
            rect(im, x0 - thick / 2, y0 - thick / 2, thick, thick, c);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    /** Variacao de brilho pixel a pixel (textura). */
    static void noise(BufferedImage im, int amount) {
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++) {
                int c = im.getRGB(x, y);
                if (Img.a(c) == 0) continue;
                int d = rnd.nextInt(amount * 2 + 1) - amount;
                im.setRGB(x, y, alpha(shade(c, d), Img.a(c)));
            }
    }

    /** Contorno escuro de 1 px ao redor dos pixels opacos (estilo dos sprites de referencia). */
    static BufferedImage outline(BufferedImage im, int color) {
        BufferedImage o = Img.argb(im.getWidth(), im.getHeight());
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++) {
                int c = im.getRGB(x, y);
                if (Img.a(c) > 0) { o.setRGB(x, y, c); continue; }
                boolean edge = Img.a(get(im, x + 1, y)) > 128 || Img.a(get(im, x - 1, y)) > 128
                        || Img.a(get(im, x, y + 1)) > 128 || Img.a(get(im, x, y - 1)) > 128;
                if (edge) o.setRGB(x, y, color);
            }
        return o;
    }

    static BufferedImage solid(int w, int h, int c) {
        BufferedImage im = Img.argb(w, h);
        rect(im, 0, 0, w, h, c);
        return im;
    }

    private static final int OUTLINE = Img.rgb(14, 11, 20);

    // =================================================================== tiles

    static BufferedImage floor(int biome, int[] pal, int variant) {
        rnd = new Random(1000 * biome + variant);
        BufferedImage im = Img.argb(32, 32);
        if (biome == 0) { // cripta: lajotas de pedra 16x16 com rejunte
            for (int sy = 0; sy < 2; sy++)
                for (int sx = 0; sx < 2; sx++) {
                    int base = shade(mix(pal[5], pal[6], rnd.nextDouble()), rnd.nextInt(9) - 4);
                    rect(im, sx * 16, sy * 16, 16, 16, base);
                    rect(im, sx * 16 + 1, sy * 16 + 1, 14, 1, shade(base, 10));
                    rect(im, sx * 16 + 1, sy * 16 + 1, 1, 14, shade(base, 6));
                    rect(im, sx * 16 + 1, sy * 16 + 14, 14, 1, shade(base, -10));
                }
            noise(im, 4);
            for (int i = 0; i < 32; i++) { set(im, i, 0, pal[2]); set(im, i, 16, pal[2]); set(im, 0, i, pal[2]); set(im, 16, i, pal[2]); }
            if (variant >= 2) crackLines(im, pal[2], 2 + variant);
        } else if (biome == 1) { // floresta: musgo, terra e grama
            int moss = mix(pal[4], Img.rgb(46, 72, 50), 0.6);
            for (int y = 0; y < 32; y++)
                for (int x = 0; x < 32; x++) {
                    double n = Math.sin(x * 0.45 + variant) * Math.cos(y * 0.38 + variant * 2) + rnd.nextDouble() * 0.6;
                    set(im, x, y, n > 0.6 ? shade(moss, 10) : n < -0.5 ? shade(moss, -12) : moss);
                }
            if (variant == 3) // pequena mancha de terra, misturada ao musgo
                for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
                    double dx = (x - 16) / 9.0, dy = (y - 17) / 6.0;
                    if (dx * dx + dy * dy < 1 && rnd.nextDouble() < 0.75) set(im, x, y, mix(get(im, x, y), Img.rgb(58, 52, 44), 0.45));
                }
            noise(im, 5);
            for (int i = 0; i < 9 + variant * 2; i++) {
                int x = rnd.nextInt(32), y = rnd.nextInt(30) + 2;
                int g = rnd.nextBoolean() ? Img.rgb(72, 104, 74) : Img.rgb(58, 88, 66);
                set(im, x, y, g); set(im, x, y - 1, g); set(im, x + (rnd.nextBoolean() ? 1 : -1), y - 2, shade(g, 14));
            }
        } else { // ruinas: terra seca e cascalho
            int dirt = mix(pal[6], pal[5], 0.35);
            for (int y = 0; y < 32; y++)
                for (int x = 0; x < 32; x++) {
                    double n = Math.sin(x * 0.3 + variant * 3) + Math.cos(y * 0.27 + variant) + rnd.nextDouble() * 0.8;
                    set(im, x, y, n > 1.3 ? shade(dirt, 8) : n < -0.9 ? shade(dirt, -10) : dirt);
                }
            noise(im, 5);
            for (int i = 0; i < 5 + variant * 2; i++) {
                int x = rnd.nextInt(30), y = rnd.nextInt(30);
                int s = rnd.nextBoolean() ? pal[4] : pal[7];
                rect(im, x, y, 2, 2, s); set(im, x, y, shade(s, 18)); set(im, x + 1, y + 2, shade(dirt, -22));
            }
            if (variant == 2) crackLines(im, pal[2], 4);
        }
        return im;
    }

    private static void crackLines(BufferedImage im, int c, int len) {
        int x = 4 + rnd.nextInt(24), y = 4 + rnd.nextInt(24);
        for (int i = 0; i < len * 3; i++) {
            set(im, x, y, c);
            x += rnd.nextInt(3) - 1;
            y += rnd.nextInt(2);
        }
    }

    static BufferedImage wall(int biome, int[] pal, int variant) {
        rnd = new Random(2000 * biome + variant);
        BufferedImage im = Img.argb(32, 32);
        if (biome == 0 || biome == 2) { // tijolos (cripta) ou pedras irregulares (ruinas)
            int mortar = pal[1];
            rect(im, 0, 0, 32, 32, mortar);
            int bh = biome == 0 ? 8 : 10;
            for (int row = 0; row * bh < 32; row++) {
                int off = (row % 2) * (biome == 0 ? 8 : 6) - 8;
                for (int x = off; x < 32; x += biome == 0 ? 16 : 11 + rnd.nextInt(6)) {
                    int w = biome == 0 ? 15 : 10 + rnd.nextInt(5);
                    int base = shade(mix(pal[3], pal[5], rnd.nextDouble()), rnd.nextInt(10) - 6);
                    rect(im, x, row * bh, w, bh - 1, base);
                    rect(im, x, row * bh, w, 1, shade(base, 14));
                    rect(im, x, row * bh + bh - 2, w, 1, shade(base, -12));
                }
            }
            noise(im, 4);
            if (variant == 1 && biome == 0) { // corrente pendurada
                for (int y = 2; y < 26; y += 3) { rect(im, 15, y, 2, 2, Img.rgb(88, 88, 96)); set(im, 15, y, Img.rgb(130, 130, 140)); }
            }
            if (variant == 1 && biome == 2) ellipse(im, 20, 14, 5, 4, mortar);
        } else { // floresta: parede de arvores/folhagem escura
            rect(im, 0, 0, 32, 32, pal[0]);
            for (int i = 0; i < 26; i++) {
                int x = rnd.nextInt(36) - 2, y = rnd.nextInt(30);
                int c = mix(pal[1], Img.rgb(40, 62, 58), rnd.nextDouble());
                ellipse(im, x, y, 4 + rnd.nextInt(4), 3 + rnd.nextInt(3), c);
                ellipse(im, x - 1, y - 1, 2, 1.5, shade(c, 12));
            }
            rect(im, 6 + variant * 12, 18, 4, 14, Img.rgb(46, 34, 38));
            rect(im, 6 + variant * 12, 18, 1, 14, Img.rgb(62, 48, 50));
            noise(im, 4);
        }
        return im;
    }

    static BufferedImage wallCap(int[] pal) {
        BufferedImage im = solid(32, 32, pal[1]);
        rnd = new Random(7);
        noise(im, 3);
        rect(im, 0, 30, 32, 2, pal[3]);
        return im;
    }

    // =================================================================== props

    static BufferedImage pillar(int[] pal, boolean broken) {
        rnd = new Random(broken ? 31 : 30);
        int h = broken ? 56 : 76;
        BufferedImage im = Img.argb(32, h);
        int top = broken ? 14 : 8;
        for (int y = top; y < h - 6; y++)
            for (int x = 6; x < 26; x++) {
                double t = (x - 6) / 20.0;
                int c = mix(pal[6], pal[3], Math.abs(t - 0.32) * 1.5);
                if ((x - 6) % 5 == 0) c = shade(c, -10); // caneluras
                set(im, x, y, c);
            }
        rect(im, 3, h - 8, 26, 8, pal[4]); rect(im, 3, h - 8, 26, 2, pal[6]);
        if (!broken) { rect(im, 3, 2, 26, 7, pal[5]); rect(im, 3, 2, 26, 2, pal[7]); }
        else for (int x = 6; x < 26; x++) for (int y = top - (x * 7 % 6); y < top; y++) set(im, x, y, pal[5]);
        noise(im, 4);
        crackLines(im, pal[1], 4);
        return outline(im, OUTLINE);
    }

    static BufferedImage sarcophagus(int[] pal) {
        rnd = new Random(32);
        BufferedImage im = Img.argb(64, 44);
        rect(im, 2, 26, 60, 16, pal[3]);                 // face frontal
        rect(im, 2, 26, 60, 1, pal[5]);
        rect(im, 4, 4, 56, 23, pal[5]);                  // tampa
        rect(im, 4, 4, 56, 2, pal[7]);
        rect(im, 6, 8, 52, 15, pal[4]);
        rect(im, 30, 9, 4, 13, pal[6]); rect(im, 22, 13, 20, 4, pal[6]); // cruz gravada
        for (int x = 8; x < 58; x += 10) rect(im, x, 30, 6, 8, shade(pal[3], -8));
        noise(im, 4);
        return outline(im, OUTLINE);
    }

    static BufferedImage bones() {
        rnd = new Random(33);
        BufferedImage im = Img.argb(36, 22);
        int bone = Img.rgb(206, 196, 168), dark = Img.rgb(150, 138, 112);
        for (int i = 0; i < 9; i++) {
            int x = 4 + rnd.nextInt(26), y = 8 + rnd.nextInt(10), len = 5 + rnd.nextInt(6);
            boolean d = rnd.nextBoolean();
            line(im, x, y, x + len, y + (d ? -3 : 3), i % 3 == 0 ? dark : bone, 2);
            rect(im, x + len - 1, y + (d ? -4 : 2), 3, 3, bone);
        }
        ellipse(im, 15, 8, 6, 5, bone);                   // cranio
        rect(im, 12, 7, 2, 2, Img.rgb(30, 22, 26)); rect(im, 17, 7, 2, 2, Img.rgb(30, 22, 26));
        rect(im, 13, 11, 5, 2, dark);
        return outline(im, OUTLINE);
    }

    static BufferedImage rug(int base, int trim) {
        rnd = new Random(base);
        BufferedImage im = Img.argb(72, 44);
        rect(im, 2, 2, 68, 40, base);
        for (int x = 2; x < 70; x++) { set(im, x, 4, trim); set(im, x, 39, trim); }
        for (int y = 4; y < 40; y++) { set(im, 4, y, trim); set(im, 67, y, trim); }
        for (int x = 2; x < 70; x += 2) { set(im, x, 0, alpha(trim, 180)); set(im, x, 43, alpha(trim, 180)); }
        ellipse(im, 36, 22, 9, 7, shade(base, 18));
        ellipse(im, 36, 22, 5, 4, shade(base, -12));
        for (int i = 0; i < 8; i++) ellipse(im, 6 + rnd.nextInt(60), 6 + rnd.nextInt(32), 2 + rnd.nextInt(4), 1 + rnd.nextInt(2), 0);
        noise(im, 6);
        return im;
    }

    static BufferedImage crack(int c, int w, int h, int n) {
        rnd = new Random(34);
        BufferedImage im = Img.argb(w, h);
        for (int k = 0; k < n; k++) {
            int x = w / 2, y = h / 2;
            for (int i = 0; i < 14; i++) {
                set(im, x, y, alpha(c, 220));
                x += rnd.nextInt(3) - 1 + (k % 2 == 0 ? 1 : -1) * (i % 3 == 0 ? 1 : 0);
                y += rnd.nextInt(3) - 1 + (k == 2 ? 1 : 0);
            }
        }
        return im;
    }

    static BufferedImage torch(int frame) {
        BufferedImage im = Img.argb(16, 30);
        rect(im, 6, 16, 4, 12, Img.rgb(84, 58, 40));       // cabo
        rect(im, 6, 16, 1, 12, Img.rgb(120, 86, 58));
        rect(im, 4, 14, 8, 4, Img.rgb(70, 66, 74));         // suporte de ferro
        rect(im, 3, 24, 10, 2, Img.rgb(70, 66, 74));
        int[] h = {12, 14, 11};
        double sway = (frame - 1) * 1.2;
        ellipse(im, 8 + sway * 0.5, 14 - h[frame] / 2.0, 4, h[frame] / 2.0, Img.rgb(232, 96, 32));
        ellipse(im, 8 + sway * 0.8, 15 - h[frame] / 2.5, 2.6, h[frame] / 3.2, Img.rgb(255, 180, 60));
        ellipse(im, 8 + sway, 12, 1.4, 2.5, Img.rgb(255, 240, 180));
        return outline(im, OUTLINE);
    }

    static BufferedImage deadTree() {
        rnd = new Random(40);
        BufferedImage im = Img.argb(100, 116);
        int bark = Img.rgb(64, 46, 52), barkL = Img.rgb(92, 68, 72), barkD = Img.rgb(40, 28, 34);
        // raizes
        for (int i = 0; i < 6; i++) {
            int dir = i < 3 ? -1 : 1;
            line(im, 50, 104, 50 + dir * (10 + rnd.nextInt(26)), 110 + rnd.nextInt(5), barkD, 3);
        }
        // tronco retorcido
        for (int y = 40; y < 108; y++) {
            int cx = 50 + (int) (Math.sin(y * 0.09) * 4), w = 7 + (108 - y) / 9;
            for (int x = cx - w; x <= cx + w; x++) {
                double t = (x - cx + w) / (2.0 * w);
                set(im, x, y, t < 0.3 ? barkL : t > 0.75 ? barkD : bark);
            }
        }
        // galhos
        branch(im, 50, 48, -1.9, 30, 4, bark);
        branch(im, 50, 42, -1.2, 34, 4, bark);
        branch(im, 52, 56, -0.4, 30, 3, bark);
        branch(im, 48, 60, -2.7, 28, 3, bark);
        noise(im, 5);
        // musgo/luz roxa corrompida
        for (int i = 0; i < 18; i++) set(im, 40 + rnd.nextInt(20), 60 + rnd.nextInt(40), Img.rgb(130, 70, 150));
        ellipse(im, 50, 74, 4, 5, Img.rgb(24, 14, 22)); // buraco no tronco
        return outline(im, OUTLINE);
    }

    private static void branch(BufferedImage im, double x, double y, double ang, double len, int thick, int c) {
        if (len < 6 || thick < 1) return;
        double ex = x + Math.cos(ang) * len, ey = y + Math.sin(ang) * len;
        line(im, (int) x, (int) y, (int) ex, (int) ey, c, thick);
        branch(im, ex, ey, ang - 0.5 - rnd.nextDouble() * 0.3, len * 0.6, thick - 1, c);
        branch(im, ex, ey, ang + 0.4 + rnd.nextDouble() * 0.3, len * 0.55, thick - 1, c);
    }

    static BufferedImage mushrooms(int cap, int capDark) {
        rnd = new Random(cap);
        BufferedImage im = Img.argb(30, 26);
        int[][] m = {{9, 24, 7, 6}, {20, 24, 5, 4}, {25, 25, 3, 3}};
        for (int[] s : m) {
            rect(im, s[0] - 1, s[1] - s[2] - 2, 3, s[2] + 2, Img.rgb(200, 196, 186));
            ellipse(im, s[0] + 0.5, s[1] - s[2] - 2, s[3], s[3] * 0.65, capDark);
            ellipse(im, s[0] + 0.5, s[1] - s[2] - 3, s[3] - 1, s[3] * 0.5, cap);
            set(im, s[0] - 1, s[1] - s[2] - 5, 0xFFFFFFFF);
            set(im, s[0] + 2, s[1] - s[2] - 3, shade(cap, 60));
        }
        return outline(im, OUTLINE);
    }

    static BufferedImage rock(int[] pal, boolean mossy) {
        rnd = new Random(mossy ? 41 : 42);
        BufferedImage im = Img.argb(40, 32);
        ellipse(im, 20, 18, 17, 12, pal[4]);
        ellipse(im, 17, 15, 13, 9, pal[5]);
        ellipse(im, 14, 12, 7, 5, pal[7]);
        ellipse(im, 26, 22, 9, 6, pal[3]);
        if (mossy) for (int x = 6; x < 34; x++) for (int y = 6; y < 12 + rnd.nextInt(3); y++)
            if (Img.a(get(im, x, y)) > 0) set(im, x, y, mix(Img.rgb(70, 110, 66), Img.rgb(50, 84, 56), rnd.nextDouble()));
        noise(im, 5);
        return outline(im, OUTLINE);
    }

    static BufferedImage puddle() {
        BufferedImage im = Img.argb(64, 32);
        ellipse(im, 32, 16, 30, 14, Img.rgb(46, 90, 98));
        ellipse(im, 30, 15, 26, 11, Img.rgb(60, 118, 126));
        ellipse(im, 22, 11, 8, 2, Img.rgb(130, 190, 196));
        ellipse(im, 40, 19, 5, 1.5, Img.rgb(110, 170, 178));
        return im;
    }

    static BufferedImage roots() {
        rnd = new Random(43);
        BufferedImage im = Img.argb(56, 28);
        for (int i = 0; i < 7; i++)
            line(im, 28, 14, 4 + rnd.nextInt(48), 2 + rnd.nextInt(24), alpha(Img.rgb(54, 38, 44), 230), 2);
        return im;
    }

    static BufferedImage ruinWall(int[] pal) {
        rnd = new Random(44);
        BufferedImage im = Img.argb(72, 58);
        int[] heights = {40, 48, 34, 22, 14};
        for (int col = 0; col < 5; col++) {
            int x = 4 + col * 13, h = heights[col];
            for (int y = 56 - h; y < 56; y += 7) {
                int base = shade(mix(pal[4], pal[7], rnd.nextDouble()), rnd.nextInt(10) - 5);
                rect(im, x + ((y / 7) % 2) * 2, y, 12, 6, base);
                rect(im, x + ((y / 7) % 2) * 2, y, 12, 1, shade(base, 16));
            }
        }
        for (int i = 0; i < 5; i++) ellipse(im, 6 + rnd.nextInt(60), 52 + rnd.nextInt(4), 3, 2, pal[5]);
        noise(im, 4);
        return outline(im, OUTLINE);
    }

    static BufferedImage shrub() {
        rnd = new Random(45);
        BufferedImage im = Img.argb(34, 30);
        for (int i = 0; i < 9; i++) {
            double a = -Math.PI / 2 + (rnd.nextDouble() - 0.5) * 2.2;
            int ex = 17 + (int) (Math.cos(a) * (10 + rnd.nextInt(6))), ey = 28 + (int) (Math.sin(a) * (14 + rnd.nextInt(10)));
            line(im, 17, 28, ex, ey, Img.rgb(92, 70, 58), 1);
            line(im, ex, ey, ex + rnd.nextInt(7) - 3, ey - 3, Img.rgb(120, 96, 78), 1);
        }
        return outline(im, OUTLINE);
    }

    static BufferedImage rift() {
        rnd = new Random(46);
        BufferedImage im = Img.argb(72, 36);
        for (int k = 0; k < 3; k++) {
            int x = 8, y = 18 + k * 2 - 2;
            for (int i = 0; i < 60; i++) {
                int w = 1 + (int) (Math.sin(i / 60.0 * Math.PI) * (k == 1 ? 5 : 2));
                rect(im, x, y - w / 2, 1, w, Img.rgb(14, 8, 18));
                if (i % 7 == 0) set(im, x, y - w / 2 - 1, Img.rgb(150, 70, 200));
                x++;
                y += rnd.nextInt(3) - 1;
            }
        }
        return im;
    }

    static BufferedImage campfire(int frame) {
        BufferedImage im = Img.argb(34, 32);
        for (int i = 0; i < 8; i++) {
            double a = i / 8.0 * Math.PI * 2;
            ellipse(im, 17 + Math.cos(a) * 11, 25 + Math.sin(a) * 5, 3.5, 2.5, i % 2 == 0 ? Img.rgb(96, 92, 100) : Img.rgb(120, 116, 124));
        }
        line(im, 9, 27, 25, 22, Img.rgb(92, 60, 40), 3);
        line(im, 9, 22, 25, 27, Img.rgb(74, 48, 34), 3);
        int[] h = {16, 19, 15};
        double sway = (frame - 1) * 1.5;
        ellipse(im, 17 + sway * 0.4, 24 - h[frame] / 2.0, 6, h[frame] / 2.0, Img.rgb(230, 90, 30));
        ellipse(im, 17 + sway * 0.7, 25 - h[frame] / 2.6, 3.8, h[frame] / 3.0, Img.rgb(255, 176, 60));
        ellipse(im, 17 + sway, 22, 1.8, 3, Img.rgb(255, 238, 170));
        return outline(im, OUTLINE);
    }

    // =================================================================== efeitos

    static BufferedImage arrow() {
        BufferedImage im = Img.argb(24, 7);
        rect(im, 4, 3, 15, 1, Img.rgb(150, 108, 66));       // haste
        rect(im, 4, 2, 15, 1, Img.rgb(186, 140, 90));
        for (int i = 0; i < 4; i++) { rect(im, 19 + i, 3 - (3 - i) / 2, 1, 1 + (3 - i), Img.rgb(200, 206, 216)); }
        set(im, 22, 3, Img.rgb(240, 244, 250));
        rect(im, 0, 1, 5, 1, Img.rgb(220, 220, 228)); rect(im, 0, 5, 5, 1, Img.rgb(220, 220, 228)); // penas
        rect(im, 1, 2, 4, 1, Img.rgb(180, 60, 60)); rect(im, 1, 4, 4, 1, Img.rgb(180, 60, 60));
        return outline(im, OUTLINE);
    }

    static BufferedImage fireball(int frame) {
        BufferedImage im = Img.argb(20, 20);
        ellipse(im, 10, 10, 8 + frame, 7 + frame * 0.5, Img.rgb(200, 50, 30));
        ellipse(im, 10.5, 10, 5.5, 5, Img.rgb(250, 140, 40));
        ellipse(im, 11, 9.5, 2.8, 2.6, Img.rgb(255, 236, 160));
        return im;
    }

    static BufferedImage gem() {
        BufferedImage im = Img.argb(12, 16);
        int[][] rows = {{5, 2}, {4, 4}, {3, 6}, {2, 8}, {2, 8}, {2, 8}, {2, 8}, {3, 6}, {3, 6}, {4, 4}, {4, 4}, {5, 2}};
        for (int y = 0; y < rows.length; y++)
            for (int x = rows[y][0]; x < rows[y][0] + rows[y][1]; x++)
                set(im, x, y + 2, x < 6 ? Img.rgb(120, 230, 255) : Img.rgb(50, 160, 220));
        set(im, 4, 5, 0xFFFFFFFF); set(im, 4, 6, 0xFFFFFFFF); set(im, 5, 4, Img.rgb(220, 250, 255));
        return outline(im, Img.rgb(10, 30, 50));
    }

    static BufferedImage chest(boolean open) {
        BufferedImage im = Img.argb(34, 30);
        int wood = Img.rgb(120, 76, 44), woodD = Img.rgb(84, 50, 30), gold = Img.rgb(230, 180, 60);
        rect(im, 3, 13, 28, 15, wood);
        rect(im, 3, 13, 28, 1, shade(wood, 20));
        for (int x = 3; x < 31; x += 7) rect(im, x, 14, 1, 14, woodD);
        if (open) {
            rect(im, 3, 2, 28, 9, woodD);
            rect(im, 5, 10, 24, 4, Img.rgb(255, 230, 140));
            for (int i = 0; i < 6; i++) set(im, 8 + i * 4, 9 - (i % 2), Img.rgb(255, 250, 220));
        } else {
            rect(im, 3, 6, 28, 8, shade(wood, 8));
            rect(im, 3, 6, 28, 2, shade(wood, 26));
        }
        rect(im, 3, 12, 28, 2, gold); rect(im, 15, 11, 4, 6, gold); set(im, 17, 14, woodD);
        rect(im, 3, 26, 28, 2, gold);
        return outline(im, OUTLINE);
    }

    static BufferedImage meteor() {
        rnd = new Random(47);
        BufferedImage im = Img.argb(28, 28);
        ellipse(im, 14, 14, 13, 13, alpha(Img.rgb(250, 120, 30), 120));
        ellipse(im, 14, 14, 9, 9, Img.rgb(92, 60, 52));
        ellipse(im, 12, 12, 5, 4, Img.rgb(132, 90, 70));
        for (int i = 0; i < 6; i++) set(im, 8 + rnd.nextInt(12), 8 + rnd.nextInt(12), Img.rgb(255, 160, 40));
        return outline(im, OUTLINE);
    }

    static BufferedImage shadow() {
        BufferedImage im = Img.argb(32, 12);
        for (int y = 0; y < 12; y++)
            for (int x = 0; x < 32; x++) {
                double dx = (x + 0.5 - 16) / 16, dy = (y + 0.5 - 6) / 6, d = dx * dx + dy * dy;
                if (d < 1) set(im, x, y, Img.argb((int) (150 * (1 - d * d)), 0, 0, 0));
            }
        return im;
    }

    static BufferedImage portal() {
        BufferedImage im = Img.argb(48, 48);
        for (int y = 0; y < 48; y++)
            for (int x = 0; x < 48; x++) {
                double dx = x + 0.5 - 24, dy = y + 0.5 - 24, d = Math.sqrt(dx * dx + dy * dy), ang = Math.atan2(dy, dx);
                double swirl = Math.sin(ang * 3 + d * 0.5);
                if (d < 22) {
                    int a = (int) (255 * Math.min(1, (22 - d) / 4.0) * (d < 15 ? 0.55 + 0.25 * swirl : 1));
                    int c = d > 17 ? Img.rgb(190, 90, 255) : swirl > 0.3 ? Img.rgb(120, 40, 200) : Img.rgb(40, 10, 70);
                    set(im, x, y, alpha(c, a));
                }
            }
        return im;
    }

    static BufferedImage shield() {
        BufferedImage im = Img.argb(64, 64);
        for (int y = 0; y < 64; y++)
            for (int x = 0; x < 64; x++) {
                double dx = x + 0.5 - 32, dy = y + 0.5 - 32, d = Math.sqrt(dx * dx + dy * dy);
                if (d > 31) continue;
                int a = d > 27 ? 230 : (int) (50 + 60 * Math.pow(d / 27, 3));
                boolean hex = ((int) ((x + (y / 8 % 2) * 4) / 8) + y / 8) % 3 == 0 && d < 27;
                set(im, x, y, alpha(hex ? Img.rgb(200, 245, 255) : Img.rgb(120, 210, 255), hex ? a + 30 : a));
            }
        return im;
    }

    static BufferedImage light(int size) {
        BufferedImage im = Img.argb(size, size);
        double c = size / 2.0;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                double d = Math.hypot(x + 0.5 - c, y + 0.5 - c) / c;
                if (d < 1) { double v = Math.pow(1 - d, 1.6); set(im, x, y, Img.argb((int) (255 * v), 255, 255, 255)); }
            }
        return im;
    }

    static BufferedImage ring(int size, int thick) {
        BufferedImage im = Img.argb(size, size);
        double c = size / 2.0;
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                double d = Math.hypot(x + 0.5 - c, y + 0.5 - c);
                if (d < c - 0.5 && d > c - 0.5 - thick) set(im, x, y, 0xFFFFFFFF);
            }
        return im;
    }

    // =================================================================== icones das especiais

    private static BufferedImage iconFrame(int glow) {
        BufferedImage im = Img.argb(64, 64);
        rect(im, 2, 2, 60, 60, Img.rgb(22, 18, 30));
        for (int y = 2; y < 62; y++)
            for (int x = 2; x < 62; x++) {
                double d = Math.hypot(x - 32, y - 32) / 42;
                set(im, x, y, mix(glow, Img.rgb(22, 18, 30), Math.min(1, d + 0.35)));
            }
        for (int i = 2; i < 62; i++) {
            set(im, i, 2, Img.rgb(90, 80, 110)); set(im, i, 61, Img.rgb(40, 34, 52));
            set(im, 2, i, Img.rgb(90, 80, 110)); set(im, 61, i, Img.rgb(40, 34, 52));
        }
        return im;
    }

    static BufferedImage iconArrows() {
        BufferedImage im = iconFrame(Img.rgb(150, 110, 40));
        for (int i = 0; i < 8; i++) {
            double a = i / 8.0 * Math.PI * 2;
            int x0 = 32 + (int) (Math.cos(a) * 7), y0 = 32 + (int) (Math.sin(a) * 7);
            int x1 = 32 + (int) (Math.cos(a) * 24), y1 = 32 + (int) (Math.sin(a) * 24);
            line(im, x0, y0, x1, y1, Img.rgb(180, 130, 80), 2);
            ellipse(im, x1, y1, 2.5, 2.5, Img.rgb(225, 230, 240));
        }
        ellipse(im, 32, 32, 5, 5, Img.rgb(255, 220, 120));
        return im;
    }

    static BufferedImage iconIce() {
        BufferedImage im = iconFrame(Img.rgb(40, 120, 190));
        for (int y = 10; y < 56; y++) {
            int half = y < 40 ? 18 : 18 - (y - 40) * 18 / 16;
            for (int x = 32 - half; x <= 32 + half; x++)
                set(im, x, y, x < 32 ? Img.rgb(150, 220, 255) : Img.rgb(90, 170, 230));
        }
        line(im, 32, 14, 32, 50, Img.rgb(230, 250, 255), 2);
        line(im, 20, 26, 44, 26, Img.rgb(230, 250, 255), 2);
        return outline(im, Img.rgb(20, 40, 70));
    }

    static BufferedImage iconChain() {
        BufferedImage im = iconFrame(Img.rgb(150, 130, 30));
        int[][] pts = {{14, 8}, {30, 24}, {20, 30}, {40, 44}, {32, 48}, {50, 58}};
        for (int i = 0; i < pts.length - 1; i++) {
            line(im, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], Img.rgb(255, 220, 80), 4);
            line(im, pts[i][0], pts[i][1], pts[i + 1][0], pts[i + 1][1], Img.rgb(255, 255, 220), 2);
        }
        return im;
    }

    static BufferedImage iconMeteor() {
        BufferedImage im = iconFrame(Img.rgb(170, 60, 20));
        for (int i = 0; i < 18; i++) line(im, 14 + i, 10, 34 + i / 2, 34, alpha(Img.rgb(255, 140 + i * 5, 40), 200), 2);
        ellipse(im, 38, 38, 11, 11, Img.rgb(92, 60, 52));
        ellipse(im, 35, 35, 5, 4, Img.rgb(140, 96, 74));
        ellipse(im, 41, 42, 3, 2, Img.rgb(255, 150, 50));
        return im;
    }

    private Painter() {}
}
