package selfdriving;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;

public class Main extends Application {
    private static final int W = 800, H = 600;
    private static final int[] SPEEDS = {1, 5, 20};

    private final Simulation sim = new Simulation();
    private int speedIdx = 0;

    @Override
    public void start(Stage stage) {
        Canvas canvas = new Canvas(W, H);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Button reset = new Button("Reset");
        Button save = new Button("Save");
        Button speed = new Button("Speed: 1x");
        reset.setOnAction(e -> sim.reset());
        save.setOnAction(e -> sim.save());
        speed.setOnAction(e -> {
            speedIdx = (speedIdx + 1) % SPEEDS.length;
            speed.setText("Speed: " + SPEEDS[speedIdx] + "x");
        });

        VBox buttons = new VBox(6, reset, save, speed);
        buttons.setPadding(new Insets(10));
        buttons.setAlignment(Pos.TOP_RIGHT);
        StackPane root = new StackPane(canvas, buttons);
        StackPane.setAlignment(buttons, Pos.TOP_RIGHT);

        stage.setTitle("Self Driving Car");
        stage.setScene(new Scene(root, W, H));
        stage.setResizable(false);
        stage.show();

        new AnimationTimer() {
            @Override public void handle(long now) {
                for (int i = 0; i < SPEEDS[speedIdx]; i++) sim.step();
                render(gc);
            }
        }.start();
    }

    private void render(GraphicsContext gc) {
        Car lead = sim.leader;
        gc.setFill(Color.web("#f4f4f4"));
        gc.fillRect(0, 0, W, H);

        gc.save();
        gc.translate(0, H * 0.7 - lead.y);   // camera follows the leader
        double top = lead.y - H, bottom = lead.y + H;

        // road + borders
        gc.setFill(Color.GRAY);
        gc.fillRect(Simulation.ROAD_LEFT - 15, top, Simulation.ROAD_RIGHT - Simulation.ROAD_LEFT + 30, bottom - top);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(5);
        gc.strokeLine(Simulation.ROAD_LEFT, top, Simulation.ROAD_LEFT, bottom);
        gc.strokeLine(Simulation.ROAD_RIGHT, top, Simulation.ROAD_RIGHT, bottom);

        // dashed lane dividers (anchored to world coordinates so they scroll correctly)
        double dashTop = Math.floor(top / 40) * 40;
        gc.setLineDashes(20, 20);
        gc.setLineWidth(4);
        for (int i = 1; i < 3; i++) {
            double lx = Simulation.ROAD_LEFT + i * (Simulation.ROAD_RIGHT - Simulation.ROAD_LEFT) / 3;
            gc.strokeLine(lx, dashTop, lx, bottom);
        }
        gc.setLineDashes(null);

        // traffic
        gc.setFill(Color.BLUE);
        for (TrafficCar t : sim.traffic) fillPoly(gc, t.polygon);

        // other cars first, leader last (opaque, with sensors)
        for (Car c : sim.cars) {
            if (c == lead) continue;
            gc.setGlobalAlpha(c.damaged ? 0.25 : 0.2);
            gc.setFill(c.damaged ? Color.BLACK : Color.RED);
            fillPoly(gc, c.polygon);
        }
        gc.setGlobalAlpha(1);
        gc.setFill(lead.damaged ? Color.BLACK : Color.RED);
        fillPoly(gc, lead.polygon);
        drawSensors(gc, lead);
        gc.restore();

        // HUD
        gc.setFill(Color.BLACK);
        gc.setFont(Font.font("System", 18));
        gc.fillText("Generation: " + sim.generation, 20, 32);
        gc.setFont(Font.font("System", 14));
        gc.fillText("Cars: " + sim.aliveCount() + "/" + Simulation.CAR_COUNT, 20, 62);
        gc.setFont(Font.font("Monospaced", 11));
        String[] lines = {
            "Car index: " + lead.index,
            String.format("Speed: %.2f", lead.speed),
            String.format("X: %.2f", lead.x),
            String.format("Y: %.2f", lead.y),
            String.format("Angle: %.2f", lead.angleDegrees()),
            "Predicted Action: " + lead.action,
            String.format("Reward Points: %.2f", lead.reward)
        };
        for (int i = 0; i < lines.length; i++) gc.fillText(lines[i], 20, 90 + i * 16);
    }

    private void drawSensors(GraphicsContext gc, Car car) {
        gc.setLineWidth(2);
        for (int i = 0; i < Sensor.RAY_COUNT; i++) {
            double[] start = car.sensor.rays[i][0], end = car.sensor.rays[i][1];
            if (start == null) continue;
            double[] hit = car.sensor.hits[i];
            double[] mid = hit != null ? hit : end;
            gc.setStroke(Color.YELLOW);
            gc.strokeLine(start[0], start[1], mid[0], mid[1]);
            gc.setStroke(Color.BLACK);
            gc.strokeLine(mid[0], mid[1], end[0], end[1]);
        }
    }

    private static void fillPoly(GraphicsContext gc, List<double[]> poly) {
        double[] xs = new double[poly.size()], ys = new double[poly.size()];
        for (int i = 0; i < xs.length; i++) { xs[i] = poly.get(i)[0]; ys[i] = poly.get(i)[1]; }
        gc.fillPolygon(xs, ys, xs.length);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
