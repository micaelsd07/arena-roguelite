package com.arena.tools;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;

/** Utilitarios de imagem para extracao de sprites (ARGB). */
final class Img {
    static BufferedImage argb(int w, int h) { return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB); }

    static BufferedImage copy(BufferedImage src, int x, int y, int w, int h) {
        BufferedImage o = argb(w, h);
        for (int j = 0; j < h; j++)
            for (int i = 0; i < w; i++) {
                int sx = Math.min(src.getWidth() - 1, x + i), sy = Math.min(src.getHeight() - 1, y + j);
                o.setRGB(i, j, src.getRGB(sx, sy) | 0xFF000000);
            }
        return o;
    }

    static int r(int c) { return (c >> 16) & 255; }
    static int g(int c) { return (c >> 8) & 255; }
    static int b(int c) { return c & 255; }
    static int a(int c) { return (c >>> 24) & 255; }

    static int rgb(int r, int g, int b) { return 0xFF000000 | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b); }
    static int argb(int a, int r, int g, int b) { return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b); }
    static int clamp(int v) { return v < 0 ? 0 : Math.min(255, v); }

    static double dist(int c1, int c2) {
        int dr = r(c1) - r(c2), dg = g(c1) - g(c2), db = b(c1) - b(c2);
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    /** Cor mais comum na borda (quantizada) = cor de fundo da celula. */
    static int borderColor(BufferedImage im, int inset) {
        java.util.HashMap<Integer, Integer> count = new java.util.HashMap<>();
        java.util.HashMap<Integer, long[]> sum = new java.util.HashMap<>();
        int w = im.getWidth(), h = im.getHeight();
        for (int y = inset; y < h - inset; y++)
            for (int x = inset; x < w - inset; x++) {
                if (x > inset + 2 && x < w - inset - 3 && y > inset + 2 && y < h - inset - 3) continue;
                int c = im.getRGB(x, y);
                int key = (r(c) >> 3) << 10 | (g(c) >> 3) << 5 | (b(c) >> 3);
                count.merge(key, 1, Integer::sum);
                long[] s = sum.computeIfAbsent(key, k -> new long[3]);
                s[0] += r(c); s[1] += g(c); s[2] += b(c);
            }
        int best = -1, bestN = -1;
        for (var e : count.entrySet()) if (e.getValue() > bestN) { bestN = e.getValue(); best = e.getKey(); }
        long[] s = sum.get(best);
        return rgb((int) (s[0] / bestN), (int) (s[1] / bestN), (int) (s[2] / bestN));
    }

    /** Remove o fundo: flood fill a partir das bordas + passada global estreita. */
    static void removeBackground(BufferedImage im, int bg, double floodTol, double globalTol) {
        int w = im.getWidth(), h = im.getHeight();
        boolean[] seen = new boolean[w * h];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = 0; x < w; x++) { q.add(new int[]{x, 0}); q.add(new int[]{x, h - 1}); }
        for (int y = 0; y < h; y++) { q.add(new int[]{0, y}); q.add(new int[]{w - 1, y}); }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int x = p[0], y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h || seen[y * w + x]) continue;
            seen[y * w + x] = true;
            int c = im.getRGB(x, y);
            if (a(c) != 0 && dist(c, bg) > floodTol) continue;
            im.setRGB(x, y, 0);
            q.add(new int[]{x + 1, y}); q.add(new int[]{x - 1, y}); q.add(new int[]{x, y + 1}); q.add(new int[]{x, y - 1});
        }
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (a(im.getRGB(x, y)) != 0 && dist(im.getRGB(x, y), bg) < globalTol) im.setRGB(x, y, 0);
    }

    /** Mantem componentes conectados grandes; descarta ruido, numeros e a sombra eliptica achatada. */
    static void keepMainComponents(BufferedImage im, double minFraction, boolean dropFlatShadows) {
        int w = im.getWidth(), h = im.getHeight();
        int[] label = new int[w * h];
        java.util.ArrayList<int[]> comps = new java.util.ArrayList<>(); // area, minx, miny, maxx, maxy
        int next = 1;
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < w * h; i++) {
            if (label[i] != 0 || a(im.getRGB(i % w, i / w)) == 0) continue;
            int[] info = {0, w, h, 0, 0};
            label[i] = next;
            q.add(i);
            while (!q.isEmpty()) {
                int p = q.poll(), x = p % w, y = p / w;
                info[0]++;
                info[1] = Math.min(info[1], x); info[2] = Math.min(info[2], y);
                info[3] = Math.max(info[3], x); info[4] = Math.max(info[4], y);
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx, ny = y + dy;
                        if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                        int n = ny * w + nx;
                        if (label[n] == 0 && a(im.getRGB(nx, ny)) != 0) { label[n] = next; q.add(n); }
                    }
            }
            comps.add(info);
            next++;
        }
        int maxArea = 0;
        for (int[] c : comps) maxArea = Math.max(maxArea, c[0]);
        boolean[] keep = new boolean[next];
        for (int k = 0; k < comps.size(); k++) {
            int[] c = comps.get(k);
            int cw = c[3] - c[1] + 1, ch = c[4] - c[2] + 1;
            boolean flat = dropFlatShadows && cw > ch * 3.2 && c[2] > h * 0.6;
            keep[k + 1] = c[0] >= maxArea * minFraction && !flat;
        }
        for (int i = 0; i < w * h; i++) if (label[i] != 0 && !keep[label[i]]) im.setRGB(i % w, i / w, 0);
    }

    static int[] bounds(BufferedImage im) {
        int minx = im.getWidth(), miny = im.getHeight(), maxx = -1, maxy = -1;
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++)
                if (a(im.getRGB(x, y)) > 0) {
                    minx = Math.min(minx, x); miny = Math.min(miny, y);
                    maxx = Math.max(maxx, x); maxy = Math.max(maxy, y);
                }
        return maxx < 0 ? new int[]{0, 0, 1, 1} : new int[]{minx, miny, maxx - minx + 1, maxy - miny + 1};
    }

    /** Reducao por media de area (pre-multiplicada) seguida de alfa binario para bordas nitidas. */
    static BufferedImage downscale(BufferedImage src, double scale, boolean crispAlpha) {
        int w = Math.max(1, (int) Math.round(src.getWidth() * scale));
        int h = Math.max(1, (int) Math.round(src.getHeight() * scale));
        BufferedImage o = argb(w, h);
        double fx = src.getWidth() / (double) w, fy = src.getHeight() / (double) h;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                double sa = 0, sr = 0, sg = 0, sb = 0, n = 0;
                int x0 = (int) (x * fx), x1 = Math.max(x0 + 1, (int) ((x + 1) * fx));
                int y0 = (int) (y * fy), y1 = Math.max(y0 + 1, (int) ((y + 1) * fy));
                for (int sy = y0; sy < y1 && sy < src.getHeight(); sy++)
                    for (int sx = x0; sx < x1 && sx < src.getWidth(); sx++) {
                        int c = src.getRGB(sx, sy);
                        double al = a(c) / 255.0;
                        sa += al; sr += r(c) * al; sg += g(c) * al; sb += b(c) * al; n++;
                    }
                if (sa <= 0) continue;
                double alpha = sa / n;
                int rr = (int) (sr / sa), gg = (int) (sg / sa), bb = (int) (sb / sa);
                if (crispAlpha) { if (alpha < 0.45) continue; alpha = 1; }
                o.setRGB(x, y, argb((int) (alpha * 255), rr, gg, bb));
            }
        return o;
    }

    static BufferedImage trim(BufferedImage im) {
        int[] b = bounds(im);
        return im.getSubimage(b[0], b[1], b[2], b[3]);
    }

    static BufferedImage flipX(BufferedImage im) {
        BufferedImage o = argb(im.getWidth(), im.getHeight());
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++) o.setRGB(im.getWidth() - 1 - x, y, im.getRGB(x, y));
        return o;
    }

    private Img() {}
}
