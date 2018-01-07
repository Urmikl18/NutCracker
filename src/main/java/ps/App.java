package ps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import ps.changeclassifier.ChangeClassifier;
import ps.models.ChangeTag;

public class App {

    public static final String FILE1 = "src/main/resources/benchmark/f1_1.txt";
    public static final String FILE2 = "src/main/resources/benchmark/f1_2.txt";

    public static String readFile(String path) throws IOException {
        Path p = Paths.get(path);
        List<String> lines = Files.readAllLines(p);
        String content = "";
        for (String line : lines) {
            content += line + "\n";
        }
        return content;
    }

    public static void main(String[] args) throws IOException {
        String text1 = "";
        String text2 = "";
        try {
            text1 = readFile(FILE1);
            text2 = readFile(FILE2);
        } catch (Exception err) {
            System.out.println("Could not read files");
        }

        ChangeClassifier cc = ChangeClassifier.INSTANCE;
        ChangeTag[] changes_tag = cc.getChangeClassification(text1, text2);
        Arrays.stream(changes_tag).forEach(c_t -> System.out.println(c_t));
    }
}