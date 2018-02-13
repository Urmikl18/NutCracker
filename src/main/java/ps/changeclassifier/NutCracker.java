package ps.changeclassifier;

import java.util.Map;

import ps.models.Change;
import ps.models.ChangeTag;

public class NutCracker {
    public NutCracker() {
    }

    public ChangeTag[] getChangeClassification(String text1, String text2) {
        // 0. Preprocess
        Preprocessor p = Preprocessor.INSTANCE;
        Map<String, Double> topic = p.calculateTopic(text1);
        // 1. Detect Changes
        ChangeDetector cd = new ChangeDetector();
        Change[] changes = cd.getChanges(text1, text2);
        // 2. Analyze Changes
        ChangeAnalyzer ca = new ChangeAnalyzer();
        ChangeTag[] class_changes = ca.getClassification(changes, text1, text2);
        // 3. Return Classification
        return class_changes;
    }

}
