package ps.changeclassifier;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ps.models.Change;
import ps.models.ChangeTag;
import ps.utils.CSVUtils;
import ps.utils.EvalUtils;
import ps.utils.Visualizer;

public class Evaluation {

    private static final String[] paths = { "angkorwat_1.txt", "angkorwat_2.txt", "antarctica_1.txt",
            "antarctica_2.txt", "atheism_1.txt", "atheism_2.txt", "brit_1.txt", "brit_2.txt", "dna_1.txt",
            "dna_2.txt" };

    private static void runTest(int testNum) {
        String text1 = "";
        String text2 = "";
        System.out.println("Test " + (testNum + 1) + ": " + paths[2 * testNum]);
        try {
            String path1 = Evaluation.class.getClassLoader().getResource(paths[2 * testNum]).getPath();
            String path2 = Evaluation.class.getClassLoader().getResource(paths[2 * testNum + 1]).getPath();
            text1 = EvalUtils.readFile(path1);
            text2 = EvalUtils.readFile(path2);
        } catch (Exception err) {
            System.out.println("Could not read files for evaluation");
            return;
        }
        ArrayList<Change> changes = ChangeDetector.getChanges(text1, text2);
        ArrayList<ChangeTag> alg_class = ChangeClassifier.getClassification(changes, text1, text2);
        Visualizer.visualize(alg_class, text1, text2);
        try {
            saveToFile(alg_class, testNum);
        } catch (Exception e) {
            System.out.println("Could not save evaluation results to files");
        }
    }

    private static void saveToFile(ArrayList<ChangeTag> alg, int testNum) throws Exception {
        String csvFile = "./evaluation" + testNum + ".csv";
        FileWriter writer = new FileWriter(csvFile);

        //for header
        System.out.println("Started export to file");
        CSVUtils.writeLine(writer, Arrays.asList("Algorithm", "Actual"));

        for (int i = 0; i < alg.size(); ++i) {

            List<String> list = new ArrayList<>();
            list.add(alg.get(i).getTag().toString());
            list.add("");

            CSVUtils.writeLine(writer, list);
        }

        writer.flush();
        writer.close();
        System.out.println("Ended export to file");

    }

    private static void runTests(int testSize) {
        int evalSize = Math.min(testSize, paths.length / 2);
        for (int i = 0; i < evalSize; ++i) {
            System.out.println("  Started evaluation #" + (i + 1) + " of " + testSize);
            runTest(i);
            System.out.println("  Finished evaluation #" + (i + 1) + " of " + testSize);
        }
    }

    public static void main(String[] args) {
        int testSize = 5;
        System.out.println("Start evaluation");
        runTests(testSize);
        System.out.println("Evaluation complete");

    }
}
