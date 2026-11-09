package selfdriving;

import java.util.List;

/** A fan of rays that measures the distance to the nearest road border / traffic car. */
public class Sensor {
    public static final int RAY_COUNT = 7;
    public static final double RAY_LENGTH = 150;
    public static final double RAY_SPREAD = Math.toRadians(135);

    public final double[][][] rays = new double[RAY_COUNT][2][];
    public final double[][] hits = new double[RAY_COUNT][]; // {x, y, t} or null
    public final double[] readings = new double[RAY_COUNT]; // 0 = nothing seen, 1 = touching

    public void update(double x, double y, double angle,
                       List<List<double[]>> borders, List<TrafficCar> traffic) {
        for (int i = 0; i < RAY_COUNT; i++) {
            double a = Geo.lerp(RAY_SPREAD / 2, -RAY_SPREAD / 2, (double) i / (RAY_COUNT - 1)) + angle;
            double[] start = {x, y};
            double[] end = {x - Math.sin(a) * RAY_LENGTH, y - Math.cos(a) * RAY_LENGTH};
            rays[i][0] = start;
            rays[i][1] = end;

            double[] best = null;
            for (List<double[]> border : borders) best = nearest(start, end, border, best);
            for (TrafficCar t : traffic) {
                if (Math.abs(t.y - y) > RAY_LENGTH + TrafficCar.HEIGHT) continue;
                best = nearest(start, end, t.polygon, best);
            }
            hits[i] = best;
            readings[i] = best == null ? 0 : 1 - best[2];
        }
    }

    private static double[] nearest(double[] start, double[] end, List<double[]> poly, double[] best) {
        for (int j = 0; j < poly.size(); j++) {
            double[] touch = Geo.intersection(start, end, poly.get(j), poly.get((j + 1) % poly.size()));
            if (touch != null && (best == null || touch[2] < best[2])) best = touch;
        }
        return best;
    }
}
