package ps.changeclassifier;

import java.util.Arrays;

import ps.models.ChangeTag;
import ps.models.ChangeTag.Tag;
import ps.utils.EvalUtils;

public class AppTest {

    private static final String[] paths = { "src/main/resources/benchmark/f1_1.txt",
            "src/main/resources/benchmark/f1_2.txt", "src/main/resources/benchmark/f2_1.txt",
            "src/main/resources/benchmark/f2_2.txt", "src/main/resources/benchmark/f3_1.txt",
            "src/main/resources/benchmark/f3_2.txt", "src/main/resources/benchmark/f4_1.txt",
            "src/main/resources/benchmark/f4_2.txt", "src/main/resources/benchmark/f5_1.txt",
            "src/main/resources/benchmark/f5_2.txt" };

    private static final Tag[][] predicted = { { Tag.FORMATTING, Tag.RELATED_TERM, Tag.SPELLING, Tag.RELATED_IDEA,
            Tag.RELATED_TERM, Tag.CITATION, Tag.RELATED_TERM, Tag.CITATION, Tag.CITATION, Tag.CITATION, Tag.CITATION,
            Tag.CITATION, Tag.RELATED_IDEA, Tag.RELATED_IDEA, Tag.RELATED_IDEA }, { Tag.RELATED_TERM } };

    public static void runTest(int testNum) {
        NutCracker nc = NutCracker.INSTANCE;
        String text1 = "";
        String text2 = "";
        try {
            text1 = "~PS~\n" + EvalUtils.readFile(paths[2 * testNum]) + "\n~PS~";
            text2 = "~PS~\n" + EvalUtils.readFile(paths[2 * testNum + 1]) + "\n~PS~";
        } catch (Exception err) {
            System.out.println("Could not read files");
            return;
        }
        ChangeTag[] classification = nc.getChangeClassification(text1, text2);
        Object[] tmp = Arrays.stream(classification).map(ct -> ct.getTag()).toArray();
        Tag[] actual = new Tag[tmp.length];
        for (int i = 0; i < actual.length; ++i) {
            actual[i] = (Tag) tmp[i];
        }
        double accuracy = EvalUtils.getAccuracy(actual, predicted[testNum]);
        double fmeasure = EvalUtils.getFMeasure(actual, predicted[testNum]);
        double[] confusion = EvalUtils.getConfusionMatrix(actual, predicted[testNum]);
        System.out.println("    Accuracy: " + accuracy + "\n    F-Measure: " + fmeasure + "\n    Confusion Matrix: "
                + Arrays.toString(confusion));
    }

    public static void runTests(int testSize) {
        int evalSize = Math.min(testSize, paths.length / 2);
        for (int i = 0; i < evalSize; ++i) {
            System.out.println("  Started evaluation #" + (i + 1) + " of " + testSize);
            runTest(i);
            System.out.println("  Finished evaluation #" + (i + 1) + " of " + testSize);
        }
    }

    public static void main(String[] args) {
        int testSize = 2;
        System.out.println("Start evaluation");
        runTests(testSize);
        System.out.println("Evaluation complete");

    }
}
