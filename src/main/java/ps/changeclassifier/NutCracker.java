package ps.changeclassifier;

import java.util.Map;

import ps.models.*;

public class NutCracker {
    private ChangeDetector cd;
    private ChangeAnalyzer ca;

    public static final NutCracker INSTANCE = new NutCracker();

    private NutCracker() {
        this.cd = ChangeDetector.INSTANCE;
        this.ca = ChangeAnalyzer.INSTANCE;
    }

    public ChangeTag[] getChangeClassification(String text1, String text2) {
        // 0. Preprocess
        Preprocessor p = Preprocessor.INSTANCE;
        Map<String, Double> topic = p.calculateTopic(text1);
        // 1. Detect Changes
        Change[] changes = cd.getChanges(text1, text2);
        // 2. Analyze Changes
        ChangeTag[] class_changes = ca.getClassification(changes);
        // 3. Return Classification
        return class_changes;
    }

}
