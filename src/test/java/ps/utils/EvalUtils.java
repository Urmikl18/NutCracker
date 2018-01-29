package ps.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import ps.models.ChangeTag.Tag;

public class EvalUtils {
    public static String readFile(String path) throws IOException {
        Path p = Paths.get(path);
        List<String> lines = Files.readAllLines(p);
        String content = "";
        for (int i = 0; i < lines.size() - 1; ++i) {
            content += lines.get(i) + "\n";
        }
        return content + lines.get(lines.size() - 1);
    }

    public static double getAccuracy(Tag[] actual, Tag[] predicted) {
        int right = 0;
        int all = predicted.length;
        for (int i = 0; i < actual.length; ++i) {
            if (actual[i].equals(predicted[i])) {
                ++right;
            }
        }
        double accuracy = (double) right / (double) all;
        return accuracy;
    }

    public static double getPrecision(Tag[] actual, Tag[] predicted) {
        return -1;
    }

    public static double getRecall(Tag[] actual, Tag[] predicted) {
        return -1;
    }

    public static double getFMeasure(Tag[] actual, Tag[] predicted) {
        return -1;
    }

    public static double[] getConfusionMatrix(Tag[] actual, Tag[] predicted) {
        return new double[] { -1, -1, -1, -1 };
    }
}
