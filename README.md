# Self-Driving Car: Neuroevolution in Java

A 2D self-driving car simulation built with **Java and JavaFX**. A population of 50 cars with small neural-network "brains" learns to drive through traffic using a **genetic algorithm**, with no training data and no machine-learning libraries. Each generation inherits from the best driver of the previous one, and the cars get better at dodging traffic over time.

![Demo](docs/demo.gif)
<!-- Record a 10-20 second GIF of the simulation (e.g. with ScreenToGif) and save it as docs/demo.gif -->

## Features

- Real-time 2D simulation with a camera that follows the leading car
- Ray-cast **distance sensors** (7 rays) that detect road borders and traffic
- A **neural network written from scratch** (no external ML libraries)
- **Genetic algorithm**: elitism, mutation, and fresh random brains for diversity
- Live HUD: generation, cars alive, speed, position, angle, chosen action and reward
- **Save / load** the best brain to disk, so training survives restarts
- Fast-forward button (1x / 5x / 20x) to speed up training
- Simulation logic fully separated from the UI, so it can be run and tested headlessly

## How it works

### 1. Perception
Each car casts 7 rays in a 135° fan, each 150 px long. A ray's reading is how close the nearest obstacle is (0 = nothing seen, 1 = touching). The rays are found by segment-intersection tests against the road borders and the edges of nearby traffic cars.

### 2. Decision
The 7 readings go into a feed-forward neural network, **7 inputs → 8 hidden neurons → 4 outputs** (tanh activations). The strongest output is the chosen action: `UP`, `LEFT`, `RIGHT` or `DOWN` (brake).

### 3. Learning (genetic algorithm)
1. Spawn 50 cars. Generation 1 has random brains.
2. Every car drives until it crashes, stops making progress, or falls far behind the leader.
3. When the generation ends, the best brain so far becomes the parent of the next one.
4. The next generation is made up of:
   - 1 unchanged copy of the champion (elitism)
   - 44 mutated copies, with a randomly chosen mutation strength
   - 5 fresh random brains, to keep exploring
5. Repeat.

### 4. Fitness function
Reward is distance travelled, scaled down when an obstacle is close in front. That gives evolution a gradient to follow (steer early, keep clear) instead of only rewarding "crash later". Cars must also keep a minimum average speed (faster than the traffic), so they can't survive by parking or tailing another car forever.

## Tech stack

| | |
|---|---|
| Language | Java 17 |
| UI / rendering | JavaFX 21 (Canvas, AnimationTimer) |
| Build | Maven |
| ML | Custom neural network and genetic algorithm, no libraries |

## Project structure

```
src/main/java/selfdriving/
├── Main.java            JavaFX window, rendering, buttons
├── Launcher.java        Plain entry point (avoids the JavaFX classpath issue)
├── Simulation.java      Road, traffic, population, evolution, save/load (no JavaFX)
├── Car.java             Car physics, sensor -> brain -> action loop, fitness
├── Sensor.java          Ray casting
├── NeuralNetwork.java   Feed-forward network, mutation, text serialization
├── TrafficCar.java      Obstacle cars
└── Geo.java             Segment intersection, polygon collision
```

## Getting started

**Requirements:** JDK 17+ and Maven.

```bash
git clone https://github.com/[your-username]/[your-repo].git
cd [your-repo]
mvn javafx:run
```

**NetBeans:** open the folder as a Maven project, run *Clean and Build*, then press **Run**.

### Controls

| Button | What it does |
|---|---|
| **Reset** | Deletes the saved brain and restarts training from scratch |
| **Save** | Saves the best brain so far to `best_brain.txt` (loaded automatically next launch) |
| **Speed** | Cycles the simulation speed: 1x, 5x, 20x |

## Configuration

| Constant | File | Default |
|---|---|---|
| Population size `CAR_COUNT` | `Simulation.java` | 50 |
| Fresh random brains per generation `FRESH_BRAINS` | `Simulation.java` | 5 |
| Traffic seed `TRAFFIC_SEED` | `Simulation.java` | 42 |
| Max speed / acceleration / friction | `Car.java` | 3 / 0.2 / 0.05 |
| Steering rate `TURN_RATE` | `Car.java` | 1° per frame |
| Rays, length, spread | `Sensor.java` | 7, 150 px, 135° |
| Network shape | `Car.newBrain()` | 7 → 8 → 4 |

## Challenges and what I learned

- **Degenerate strategies.** Early versions let cars survive forever by stopping, or by following a traffic car at its own speed. I fixed this with a minimum-progress rule, so cars have to overtake.
- **Weak learning signal.** With a pure distance fitness, all straight-driving cars tied. Adding a proximity-based penalty to the reward gave the evolution something to improve on.
- **Fair environment.** Random traffic sometimes created unwinnable patterns. The generator now guarantees that every row has an open lane reachable from the previous row's open lane.
- **Separation of concerns.** Keeping `Simulation` free of JavaFX let me run headless experiments to measure learning speed.

## Possible improvements

- Crossover between the best brains, instead of mutation only
- Different traffic each generation, for better generalisation
- Plotting fitness per generation
- Configurable network size and sensor setup from the UI
