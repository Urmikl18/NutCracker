package ps.changeclassifier;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import ps.models.Change;
import ps.models.ChangeTag;
import ps.models.ChangeTag.Tag;
import ps.utils.CSVUtils;
import ps.utils.EvalUtils;
import ps.utils.Visualizer;

public class Evaluation {

    private static final String[] paths = { "../benchmark/angkorwat_1.txt", "../benchmark/angkorwat_2.txt",
            "../benchmark/antarctica_1.txt", "../benchmark/antarctica_2.txt", "../benchmark/atheism_1.txt",
            "../benchmark/atheism_2.txt", "../benchmark/brit_1.txt", "../benchmark/brit_2.txt",
            "../benchmark/dna_1.txt", "../benchmark/dna_2.txt", "../benchmark/han_1.txt", "../benchmark/han_2.txt",
            "../benchmark/influenza_1.txt", "../benchmark/influenza_2.txt", "../benchmark/japan_1.txt",
            "../benchmark/japan_2.txt", "../benchmark/mdd_1.txt", "../benchmark/mdd_2.txt",
            "../benchmark/supernova_1.txt", "../benchmark/supernova_2.txt" };

    private static final Tag[] tags = { Tag.FORMATTING, Tag.CITATION, Tag.SPELLING, Tag.GRAMMAR,

            Tag.RELATED_TERM, Tag.UNRELATED_TERM, Tag.SYNONYM,

            Tag.REPHRASING, Tag.MINOR_TOPIC_CHANGE, Tag.MAJOR_TOPIC_CHANGE,

            Tag.UNDEFINED };

    private static ArrayList<ChangeTag> getRandomClassification(ArrayList<Change> changes) {
        Random randomGen = new Random();
        ArrayList<ChangeTag> random_class = new ArrayList<ChangeTag>(changes.size());
        for (Change c : changes) {
            int index = randomGen.nextInt(11);
            random_class.add(new ChangeTag(c, tags[index]));
        }
        return random_class;
    }

    private static void runTest(int testNum) {
        String text1 = "";
        String text2 = "";
        try {
            text1 = EvalUtils.readFile(paths[2 * testNum]);
            text2 = EvalUtils.readFile(paths[2 * testNum + 1]);
        } catch (Exception err) {
            System.out.println("Could not read files for evaluation");
            return;
        }
        ArrayList<Change> changes = ChangeDetector.getChanges(text1, text2);
        ArrayList<ChangeTag> random_class = getRandomClassification(changes);
        ArrayList<ChangeTag> alg_class = ChangeClassifier.getClassification(changes, text1, text2);
        // Visualizer.visualize(alg_class, text1, text2);
        try {
            saveToFile(alg_class, random_class, testNum);
        } catch (Exception e) {
            System.out.println("Could not save evaluation results to files");
        }
    }

    private static void saveToFile(ArrayList<ChangeTag> alg, ArrayList<ChangeTag> random, int testNum)
            throws Exception {
        String csvFile = "../evaluation/test" + testNum + ".csv";
        FileWriter writer = new FileWriter(csvFile);

        //for header
        System.out.println("Started export to file");
        CSVUtils.writeLine(writer, Arrays.asList("Algorithm", "Random", "Actual"));

        for (int i = 0; i < alg.size(); ++i) {

            List<String> list = new ArrayList<>();
            list.add(alg.get(i).getTag().toString());
            list.add(random.get(i).getTag().toString());
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
        int testSize = 10;
        System.out.println("Start evaluation");
        runTests(testSize);
        System.out.println("Evaluation complete");

    }
}
