package selfdriving;

import java.util.List;

/** An AI-driven car: sensor -> neural network -> one of four actions. */
public class Car {
    public static final double WIDTH = 30, HEIGHT = 50;
    public static final double MAX_SPEED = 3, ACCELERATION = 0.2, FRICTION = 0.05;
    public static final double TURN_RATE = Math.toRadians(1.0); // per frame

    public static final String[] ACTIONS = {"UP", "LEFT", "RIGHT", "DOWN"};

    public final int index;
    public final NeuralNetwork brain;
    public final Sensor sensor = new Sensor();

    public double x, y, angle, speed;
    public double reward;
    public boolean damaged;
    public String action = "UP";
    public List<double[]> polygon;

    private boolean up, left, right, down;

    // A car must keep making forward progress, otherwise it is removed (stops "parked" cars living forever).
    private static final int PROGRESS_CHECK_FRAMES = 120;
    private static final double MIN_PROGRESS = 270;  // px per check (2.25 px/frame: must out-run the traffic, which moves at 2)
    private double checkY;
    private int framesSinceCheck;

    public Car(int index, double x, double y, NeuralNetwork brain) {
        this.index = index;
        this.x = x;
        this.y = y;
        this.brain = brain;
        this.polygon = Geo.carPolygon(x, y, WIDTH, HEIGHT, angle);
        this.checkY = y;
    }

    public static NeuralNetwork newBrain() {
        return new NeuralNetwork(Sensor.RAY_COUNT, 8, ACTIONS.length);
    }

    public void update(Simulation sim) {
        if (damaged) return;

        sensor.update(x, y, angle, sim.borders, sim.traffic);
        double[] out = brain.feedForward(sensor.readings);

        int best = 0;
        for (int i = 1; i < out.length; i++) if (out[i] > out[best]) best = i;
        action = ACTIONS[best];
        up = best != 3;          // throttle is on unless the brain picks DOWN (brake)
        left = best == 1;
        right = best == 2;
        down = best == 3;

        move();
        polygon = Geo.carPolygon(x, y, WIDTH, HEIGHT, angle);
        damaged = sim.collides(polygon);
        // Reward = distance, but less of it when something is close in front. That gives evolution
        // a gradient to follow (steer early, keep clear) instead of only rewarding "crash later".
        double front = Math.max(sensor.readings[2], Math.max(sensor.readings[3], sensor.readings[4]));
        reward += speed * (1 - 0.6 * front);

        if (++framesSinceCheck >= PROGRESS_CHECK_FRAMES) {
            if (checkY - y < MIN_PROGRESS) damaged = true;
            checkY = y;
            framesSinceCheck = 0;
        }
    }

    private void move() {
        if (up) speed += ACCELERATION;
        if (down) speed -= ACCELERATION;
        speed = Math.max(-MAX_SPEED / 2, Math.min(MAX_SPEED, speed));
        if (speed > 0) speed -= FRICTION;
        if (speed < 0) speed += FRICTION;
        if (Math.abs(speed) < FRICTION) speed = 0;

        if (speed != 0) {
            double flip = speed > 0 ? 1 : -1;
            if (left) angle += TURN_RATE * flip;
            if (right) angle -= TURN_RATE * flip;
        }
        x -= Math.sin(angle) * speed;
        y -= Math.cos(angle) * speed;
    }

    public double angleDegrees() {
        return Math.toDegrees(angle);
    }
}
