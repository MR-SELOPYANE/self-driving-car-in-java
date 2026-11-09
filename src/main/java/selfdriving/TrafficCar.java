package selfdriving;

import java.util.List;

/** A blue obstacle car that cruises slowly up the road. */
public class TrafficCar {
    public static final double WIDTH = 30, HEIGHT = 50, SPEED = 2;

    public double x, y;
    public List<double[]> polygon;

    public TrafficCar(double x, double y) {
        this.x = x;
        this.y = y;
        this.polygon = Geo.carPolygon(x, y, WIDTH, HEIGHT, 0);
    }

    public void update() {
        y -= SPEED;
        polygon = Geo.carPolygon(x, y, WIDTH, HEIGHT, 0);
    }
}
