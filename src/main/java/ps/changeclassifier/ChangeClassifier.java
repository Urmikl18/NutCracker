package ps.changeclassifier;

import java.util.Map;

import ps.models.*;

public class ChangeClassifier {
    private ChangeDetector cd;
    private ChangeAnalyzer ca;

    public static final ChangeClassifier INSTANCE = new ChangeClassifier();

    private ChangeClassifier() {
        this.cd = ChangeDetector.INSTANCE;
        this.ca = ChangeAnalyzer.INSTANCE;
    }

    public ChangeTag[] getChangeClassification(String text1, String text2) {
        // 0. Preprocess
        Preprocessor p = Preprocessor.INSTANCE;
        Map<String, Double> topic = p.calculateTopic(text1);
        System.out.println("Preprocessing done");
        // 1. Detect Changes
        Change[] changes = cd.getChanges(text1, text2);
        System.out.println("Changes detection done");
        // 2. Analyze Changes
        ChangeTag[] class_changes = ca.getClassification(changes);
        System.out.println("Changes analysis done");
        // 3. Return Classification
        System.out.println("Change classification done");
        return class_changes;
    }

}
