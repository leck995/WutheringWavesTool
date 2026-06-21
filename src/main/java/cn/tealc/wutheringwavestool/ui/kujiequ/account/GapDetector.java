package cn.tealc.wutheringwavestool.ui.kujiequ.account;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

/**
 * Pure-Java GeeTest V3 slide captcha gap detector.
 * <p>
 * Uses Sobel edge detection + template matching to locate the puzzle gap,
 * inspired by COOLasHEL/geetest-v3-solver (OpenCV) but with zero native deps.
 * Designed to replace the low-accuracy JS canvas pixel-diff approach.
 * </p>
 * <p>
 * Algorithm:
 * <ol>
 *   <li>Extract puzzle piece bounding box from slice image alpha channel</li>
 *   <li>Sobel edge detection on piece and background</li>
 *   <li>Zero out left-side edges (piece overlay area) from bg to avoid self-match</li>
 *   <li>Slide piece edges across bg edges, find best SAD (sum of absolute differences)</li>
 *   <li>Adjust for piece starting offset</li>
 * </ol>
 * </p>
 */
public class GapDetector {

    /**
     * Detect the horizontal gap position for a GeeTest V3 slide captcha.
     *
     * @param bgBase64    base64-encoded PNG of the background canvas (geetest_canvas_bg)
     * @param sliceBase64 base64-encoded PNG of the slice canvas (geetest_canvas_slice)
     * @return gap X position in pixels, or -1 if detection failed
     */
    public static int detect(String bgBase64, String sliceBase64) {
        BufferedImage bg = decodeBase64(bgBase64);
        BufferedImage slice = decodeBase64(sliceBase64);
        if (bg == null || slice == null) return -1;

        // 1. Get piece bounding box from alpha channel
        Rectangle pieceBounds = getPieceBounds(slice);
        if (pieceBounds == null || pieceBounds.width < 5 || pieceBounds.height < 5) {
            return -1;
        }

        // 2. Crop piece
        BufferedImage piece;
        try {
            piece = slice.getSubimage(pieceBounds.x, pieceBounds.y,
                    pieceBounds.width, pieceBounds.height);
        } catch (Exception e) {
            return -1;
        }

        // 3. Edge detection
        BufferedImage pieceEdges = sobelEdges(toGrayscale(piece));
        BufferedImage bgEdges = sobelEdges(toGrayscale(bg));

        // 4. Zero out edges where the piece overlay sits (left side),
        //    so we don't accidentally match the piece against itself
        int searchStart = Math.min(pieceBounds.x + pieceBounds.width + 5, bgEdges.getWidth() - 1);
        clearEdges(bgEdges, 0, searchStart);

        // 5. Template matching: find where piece edges best align with bg edges
        int matchX = matchTemplate(bgEdges, pieceEdges);
        if (matchX < 0) return -1;

        // 6. Final gap position = match position - piece starting offset
        //    The piece on the slice canvas starts at pieceBounds.x.
        //    So the actual drag distance is from the piece's current position
        //    to the matched gap position.
        int gapX = matchX - pieceBounds.x;
        if (gapX < 0) gapX = matchX;

        return gapX;
    }


    // ========================================================================
    // Image decode
    // ========================================================================

    private static BufferedImage decodeBase64(String dataUrl) {
        if (dataUrl == null || dataUrl.isEmpty()) return null;
        try {
            // Strip data:image/png;base64, prefix if present
            String base64 = dataUrl;
            int comma = dataUrl.indexOf(',');
            if (comma >= 0) {
                base64 = dataUrl.substring(comma + 1);
            }
            byte[] bytes = Base64.getDecoder().decode(base64);
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            return null;
        }
    }


    // ========================================================================
    // Grayscale
    // ========================================================================

    private static BufferedImage toGrayscale(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        BufferedImage gray = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int luma = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                gray.setRGB(x, y, (luma << 16) | (luma << 8) | luma);
            }
        }
        return gray;
    }


    // ========================================================================
    // Sobel edge detection
    // ========================================================================

    private static BufferedImage sobelEdges(BufferedImage gray) {
        int w = gray.getWidth();
        int h = gray.getHeight();
        BufferedImage edges = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);

        // Build luminance array
        int[][] lum = new int[w][h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                lum[x][y] = gray.getRGB(x, y) & 0xFF;
            }
        }

        // Sobel operator (3x3)
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int gx = -lum[x - 1][y - 1] + lum[x + 1][y - 1]
                         - 2 * lum[x - 1][y] + 2 * lum[x + 1][y]
                         - lum[x - 1][y + 1] + lum[x + 1][y + 1];

                int gy = -lum[x - 1][y - 1] - 2 * lum[x][y - 1] - lum[x + 1][y - 1]
                         + lum[x - 1][y + 1] + 2 * lum[x][y + 1] + lum[x + 1][y + 1];

                int mag = (int) Math.sqrt(gx * gx + gy * gy);
                if (mag > 255) mag = 255;
                edges.setRGB(x, y, (mag << 16) | (mag << 8) | mag);
            }
        }
        return edges;
    }


    // ========================================================================
    // Piece bounding box from alpha channel
    // ========================================================================

    private static Rectangle getPieceBounds(BufferedImage slice) {
        int w = slice.getWidth();
        int h = slice.getHeight();
        int minX = w, maxX = 0, minY = h, maxY = 0;
        boolean found = false;

        // Check if slice has alpha by looking at type
        boolean hasAlpha = slice.getColorModel().hasAlpha();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgba = slice.getRGB(x, y);
                int alpha;
                if (hasAlpha) {
                    alpha = (rgba >> 24) & 0xFF;
                } else {
                    // No alpha: treat dark pixels as transparent
                    int r = (rgba >> 16) & 0xFF;
                    int g = (rgba >> 8) & 0xFF;
                    int b = rgba & 0xFF;
                    alpha = (r < 10 && g < 10 && b < 10) ? 0 : 255;
                }
                if (alpha > 20) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                    found = true;
                }
            }
        }

        if (!found) return null;
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }


    // ========================================================================
    // Edge clearing
    // ========================================================================

    private static void clearEdges(BufferedImage edges, int fromX, int toX) {
        int h = edges.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = fromX; x < toX && x < edges.getWidth(); x++) {
                edges.setRGB(x, y, 0); // black = no edge
            }
        }
    }


    // ========================================================================
    // Template matching (Sum of Absolute Differences on edge images)
    // ========================================================================

    private static int matchTemplate(BufferedImage bgEdges, BufferedImage pieceEdges) {
        int bw = bgEdges.getWidth();
        int bh = bgEdges.getHeight();
        int pw = pieceEdges.getWidth();
        int ph = pieceEdges.getHeight();

        if (pw >= bw || ph >= bh) return -1;

        int maxX = bw - pw;
        int bestX = -1;
        double bestScore = Double.MAX_VALUE;

        // Precompute edge strength for each pixel (0-255)
        int[][] bgVals = new int[bw][bh];
        for (int y = 0; y < bh; y++) {
            for (int x = 0; x < bw; x++) {
                bgVals[x][y] = bgEdges.getRGB(x, y) & 0xFF;
            }
        }

        int[][] pVals = new int[pw][ph];
        int edgePixelCount = 0;
        for (int y = 0; y < ph; y++) {
            for (int x = 0; x < pw; x++) {
                int v = pieceEdges.getRGB(x, y) & 0xFF;
                pVals[x][y] = v;
                if (v > 30) edgePixelCount++; // count non-trivial edge pixels
            }
        }

        if (edgePixelCount < 10) return -1; // piece has no edges

        // Slide across bg, compute SAD
        for (int x = 0; x <= maxX; x++) {
            double score = 0;
            int count = 0;

            for (int py = 0; py < ph; py++) {
                for (int px = 0; px < pw; px++) {
                    int pv = pVals[px][py];
                    if (pv < 30) continue; // skip non-edge pixels

                    int bv = bgVals[x + px][py];
                    score += Math.abs(pv - bv);
                    count++;
                }
            }

            if (count > 0) {
                double avg = score / count;
                if (avg < bestScore) {
                    bestScore = avg;
                    bestX = x;
                }
            }
        }

        return bestX;
    }
}
