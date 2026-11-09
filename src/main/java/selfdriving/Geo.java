package selfdriving;

import java.util.ArrayList;
import java.util.List;

/** Small geometry helpers. A "polygon" is a list of {x, y} points. */
public final class Geo {
    private Geo() {}

    public static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Intersection of segment AB with segment CD. Returns {x, y, t} (t = distance along AB, 0..1) or null. */
    public static double[] intersection(double[] a, double[] b, double[] c, double[] d) {
        double tTop = (d[0] - c[0]) * (a[1] - c[1]) - (d[1] - c[1]) * (a[0] - c[0]);
        double uTop = (c[1] - a[1]) * (a[0] - b[0]) - (c[0] - a[0]) * (a[1] - b[1]);
        double bottom = (d[1] - c[1]) * (b[0] - a[0]) - (d[0] - c[0]) * (b[1] - a[1]);
        if (bottom != 0) {
            double t = tTop / bottom;
            double u = uTop / bottom;
            if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
                return new double[]{lerp(a[0], b[0], t), lerp(a[1], b[1], t), t};
            }
        }
        return null;
    }

    public static boolean polysIntersect(List<double[]> p1, List<double[]> p2) {
        for (int i = 0; i < p1.size(); i++) {
            for (int j = 0; j < p2.size(); j++) {
                if (intersection(p1.get(i), p1.get((i + 1) % p1.size()),
                                 p2.get(j), p2.get((j + 1) % p2.size())) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Corners of a w x h rectangle centred on (x, y), rotated by angle (radians, 0 = facing up). */
    public static List<double[]> carPolygon(double x, double y, double w, double h, double angle) {
        double rad = Math.hypot(w, h) / 2;
        double alpha = Math.atan2(w, h);
        List<double[]> pts = new ArrayList<>(4);
        pts.add(new double[]{x - Math.sin(angle - alpha) * rad, y - Math.cos(angle - alpha) * rad});
        pts.add(new double[]{x - Math.sin(angle + alpha) * rad, y - Math.cos(angle + alpha) * rad});
        pts.add(new double[]{x - Math.sin(Math.PI + angle - alpha) * rad, y - Math.cos(Math.PI + angle - alpha) * rad});
        pts.add(new double[]{x - Math.sin(Math.PI + angle + alpha) * rad, y - Math.cos(Math.PI + angle + alpha) * rad});
        return pts;
    }
}
