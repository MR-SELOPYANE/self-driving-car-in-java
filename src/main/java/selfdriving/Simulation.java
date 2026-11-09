package selfdriving;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** All game logic (no JavaFX): road, traffic, population, evolution. Call step() once per frame. */
public class Simulation {
    public static final double ROAD_LEFT = 250, ROAD_RIGHT = 550;
    public static final double[] LANE_X = {300, 400, 500};
    public static final int CAR_COUNT = 50;
    private static final int FRESH_BRAINS = 5;            // random newcomers per generation
    public static final long TRAFFIC_SEED = 42;          // same road every generation
    private static final int STALL_FRAMES = 600;         // end generation if nobody makes progress
    private static final Path SAVE_FILE = Path.of("best_brain.txt");

    public final List<List<double[]>> borders = new ArrayList<>();
    public final List<Car> cars = new ArrayList<>();
    public final List<TrafficCar> traffic = new ArrayList<>();
    public int generation = 1;
    public Car leader;

    private NeuralNetwork globalBest;
    private double globalBestReward = Double.NEGATIVE_INFINITY;
    private final Random rng = new Random();
    private Random trafficRng;
    private double nextRowY;
    private int safeLane;
    private double bestY;
    private int framesSinceProgress;

    public Simulation() {
        borders.add(List.of(new double[]{ROAD_LEFT, -1e7}, new double[]{ROAD_LEFT, 1e7}));
        borders.add(List.of(new double[]{ROAD_RIGHT, -1e7}, new double[]{ROAD_RIGHT, 1e7}));
        load();
        startGeneration();
    }

    // ------------------------------------------------------------------ per-frame update
    public void step() {
        spawnTraffic(leader.y);
        for (TrafficCar t : traffic) t.update();
        for (Car c : cars) c.update(this);
        traffic.removeIf(t -> t.y > leader.y + 800);

        Car front = null;
        for (Car c : cars) if (!c.damaged && (front == null || c.y < front.y)) front = c;
        if (front == null) { nextGeneration(); return; }
        leader = front;

        // cars that fall far behind the pack are dropped
        for (Car c : cars) if (!c.damaged && c.y > leader.y + 600) c.damaged = true;

        if (leader.y < bestY - 1) { bestY = leader.y; framesSinceProgress = 0; }
        else if (++framesSinceProgress > STALL_FRAMES) nextGeneration();
    }

    public int aliveCount() {
        int n = 0;
        for (Car c : cars) if (!c.damaged) n++;
        return n;
    }

    public boolean collides(List<double[]> poly) {
        for (List<double[]> b : borders) if (Geo.polysIntersect(poly, b)) return true;
        double cy = poly.get(0).length > 0 ? (poly.get(0)[1] + poly.get(2)[1]) / 2 : 0;
        for (TrafficCar t : traffic) {
            if (Math.abs(t.y - cy) > 100) continue;
            if (Geo.polysIntersect(poly, t.polygon)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ evolution
    private void nextGeneration() {
        Car best = cars.get(0);
        for (Car c : cars) if (c.reward > best.reward) best = c;
        if (best.reward > globalBestReward) {
            globalBestReward = best.reward;
            globalBest = best.brain.copy();
        }
        generation++;
        startGeneration();
    }

    private void startGeneration() {
        traffic.clear();
        trafficRng = new Random(TRAFFIC_SEED);
        nextRowY = -300;
        safeLane = 1;
        bestY = 0;
        framesSinceProgress = 0;

        cars.clear();
        for (int i = 0; i < CAR_COUNT; i++) {
            NeuralNetwork brain;
            if (globalBest == null || i >= CAR_COUNT - FRESH_BRAINS) {
                brain = Car.newBrain();                                         // fresh blood keeps exploring
            } else {
                brain = globalBest.copy();
                if (i > 0) brain.mutate(0.02 + rng.nextDouble() * 0.4, rng);    // car 0 = untouched champion
            }
            cars.add(new Car(i, LANE_X[1], 0, brain));
        }
        leader = cars.get(0);
        spawnTraffic(0);
    }

    private void spawnTraffic(double leaderY) {
        while (nextRowY > leaderY - 1500) {
            // Every row keeps one "safe" lane open, and it is never more than one lane away
            // from the previous row's safe lane, so a winning path always exists.
            safeLane = Math.max(0, Math.min(2, safeLane + trafficRng.nextInt(3) - 1));
            List<Integer> blockable = new ArrayList<>(List.of(0, 1, 2));
            blockable.remove(Integer.valueOf(safeLane));
            Collections.shuffle(blockable, trafficRng);
            int count = 1 + trafficRng.nextInt(2);
            for (int k = 0; k < count; k++) traffic.add(new TrafficCar(LANE_X[blockable.get(k)], nextRowY));
            nextRowY -= 220 + trafficRng.nextInt(80);
        }
    }

    // ------------------------------------------------------------------ buttons
    public void save() {
        NeuralNetwork n = globalBest;
        double r = globalBestReward;
        for (Car c : cars) if (c.reward > r) { r = c.reward; n = c.brain; }
        if (n == null) return;
        try {
            Files.writeString(SAVE_FILE, n.toText());
            System.out.println("Saved best brain to " + SAVE_FILE.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("Could not save: " + e.getMessage());
        }
    }

    public void reset() {
        try { Files.deleteIfExists(SAVE_FILE); } catch (IOException ignored) {}
        globalBest = null;
        globalBestReward = Double.NEGATIVE_INFINITY;
        generation = 1;
        startGeneration();
    }

    private void load() {
        try {
            if (Files.exists(SAVE_FILE)) {
                globalBest = NeuralNetwork.fromText(Files.readString(SAVE_FILE));
                globalBestReward = 0;
                System.out.println("Loaded saved brain from " + SAVE_FILE.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Ignoring unreadable save file: " + e.getMessage());
        }
    }
}
