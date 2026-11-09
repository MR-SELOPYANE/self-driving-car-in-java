package selfdriving;

import java.util.Random;

/** Tiny fully-connected network (tanh everywhere) that can be copied, mutated and saved as text. */
public class NeuralNetwork {
    private final int[] sizes;
    private final double[][][] weights; // [layer][output][input]
    private final double[][] biases;    // [layer][output]

    public NeuralNetwork(int... sizes) {
        this.sizes = sizes.clone();
        weights = new double[sizes.length - 1][][];
        biases = new double[sizes.length - 1][];
        Random rng = new Random();
        for (int l = 0; l < sizes.length - 1; l++) {
            weights[l] = new double[sizes[l + 1]][sizes[l]];
            biases[l] = new double[sizes[l + 1]];
            for (int o = 0; o < sizes[l + 1]; o++) {
                biases[l][o] = rng.nextDouble() * 2 - 1;
                for (int i = 0; i < sizes[l]; i++) weights[l][o][i] = rng.nextDouble() * 2 - 1;
            }
        }
    }

    public double[] feedForward(double[] input) {
        double[] cur = input;
        for (int l = 0; l < weights.length; l++) {
            double[] next = new double[sizes[l + 1]];
            for (int o = 0; o < next.length; o++) {
                double sum = biases[l][o];
                for (int i = 0; i < cur.length; i++) sum += weights[l][o][i] * cur[i];
                next[o] = Math.tanh(sum);
            }
            cur = next;
        }
        return cur;
    }

    public NeuralNetwork copy() {
        NeuralNetwork n = new NeuralNetwork(sizes);
        for (int l = 0; l < weights.length; l++) {
            for (int o = 0; o < weights[l].length; o++) {
                System.arraycopy(weights[l][o], 0, n.weights[l][o], 0, weights[l][o].length);
            }
            System.arraycopy(biases[l], 0, n.biases[l], 0, biases[l].length);
        }
        return n;
    }

    /** Moves every weight/bias towards a random value by `amount` (0 = unchanged, 1 = fully random). */
    public void mutate(double amount, Random rng) {
        for (int l = 0; l < weights.length; l++) {
            for (int o = 0; o < weights[l].length; o++) {
                biases[l][o] = Geo.lerp(biases[l][o], rng.nextDouble() * 2 - 1, amount);
                for (int i = 0; i < weights[l][o].length; i++) {
                    weights[l][o][i] = Geo.lerp(weights[l][o][i], rng.nextDouble() * 2 - 1, amount);
                }
            }
        }
    }

    // ---- persistence: line 1 = layer sizes, line 2 = all numbers ----
    public String toText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sizes.length; i++) sb.append(i > 0 ? " " : "").append(sizes[i]);
        sb.append('\n');
        boolean first = true;
        for (int l = 0; l < weights.length; l++) {
            for (int o = 0; o < weights[l].length; o++) {
                for (double w : weights[l][o]) { sb.append(first ? "" : " ").append(w); first = false; }
                sb.append(' ').append(biases[l][o]);
            }
        }
        return sb.toString();
    }

    public static NeuralNetwork fromText(String text) {
        String[] lines = text.strip().split("\n");
        String[] s = lines[0].trim().split("\\s+");
        int[] sizes = new int[s.length];
        for (int i = 0; i < s.length; i++) sizes[i] = Integer.parseInt(s[i]);
        NeuralNetwork n = new NeuralNetwork(sizes);
        String[] nums = lines[1].trim().split("\\s+");
        int k = 0;
        for (int l = 0; l < n.weights.length; l++) {
            for (int o = 0; o < n.weights[l].length; o++) {
                for (int i = 0; i < n.weights[l][o].length; i++) n.weights[l][o][i] = Double.parseDouble(nums[k++]);
                n.biases[l][o] = Double.parseDouble(nums[k++]);
            }
        }
        return n;
    }
}
