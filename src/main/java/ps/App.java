package ps;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import ps.changeclassifier.NutCracker;
import ps.models.ChangeTag;

public class App {

    public static String readFile(String path) throws IOException {
        Path p = Paths.get(path);
        List<String> lines = Files.readAllLines(p);
        String content = "";
        for (int i = 0; i < lines.size() - 1; ++i) {
            content += lines.get(i) + "\n";
        }
        return content + lines.get(lines.size() - 1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Usage: java -jar nutcracker.jar -option source1 source2\n"
                    + "\nwhere options include:\n" + "\t -t\t analyze two strings (source1, source2)\n"
                    + "\t -f\t analyze content of two plain text files (source1, source2)\n"
                    + "\nwhere source1, source2 are either text snippets of paths to the text files to be analyzed.");
            return;
        }

        String text1 = "";
        String text2 = "";

        if (args[0].equals("-t")) {
            text1 = args[1];
            text2 = args[2];
        } else if (args[0].equals("-f")) {
            try {
                text1 = readFile(args[1]);
                text2 = readFile(args[2]);
            } catch (Exception err) {
                System.out.println("Could not read files");
                return;
            }
        } else {
            System.out.println(args[0] + " is an invalid option");
            return;
        }

        NutCracker cc = new NutCracker();
        ChangeTag[] changes_tag = cc.getChangeClassification(text1, text2);
        System.out.println();
        Arrays.stream(changes_tag).forEach(c_t -> System.out.println(c_t));
    }
}