package ps.changeclassifier;

import java.util.ArrayList;

import ps.models.Change;
import ps.models.ChangeTag;
import ps.models.ChangeTag.Tag;

/**
 * Class that classifies changes in a plain-text document.
 * Showcases the algorithm proposed in the thesis.
 */
public class ChangeClassifier {
    private ChangeClassifier() {
    }

    /**
     * @param changes list of changes to be analyzed.
     * @param text1 initial version of the document.
     * @param text2 modified version of the document.
     * @return list of changes with tags describing the meaning of a change.
     */
    public static ArrayList<ChangeTag> getClassification(ArrayList<Change> changes, String text1, String text2) {
        ArrayList<ChangeTag> ch_class = new ArrayList<ChangeTag>(changes.size());
        for (int i = 0; i < changes.size(); ++i) {
            Tag tag = ChangeClassifier.getTag(changes.get(i), text1, text2);
            ch_class.add(new ChangeTag(changes.get(i), tag));
        }
        return ch_class;
    }

    /*
    Assigns each change a tag that describes change's meaning.
    */
    private static Tag getTag(Change change, String text1, String text2) {
        boolean citation = ChangeAnalyzer.isCitation(change);
        if (citation) {
            return Tag.CITATION;
        }

        boolean undefined = ChangeAnalyzer.isUndefined(change);
        if (undefined) {
            return Tag.UNDEFINED;
        }

        boolean spelling = ChangeAnalyzer.isSpelling(change);
        if (spelling) {
            return Tag.SPELLING;
        }

        boolean formatting = ChangeAnalyzer.isFormatting(change);
        if (formatting) {
            return Tag.FORMATTING;
        }

        int sub_sim = ChangeAnalyzer.substitutionSimilarity(change);
        switch (sub_sim) {
        case -1:
            break;
        case 0:
            return Tag.UNRELATED_TERM;
        case 1:
            return Tag.RELATED_TERM;
        case 2:
            return Tag.SYNONYM;
        }

        if (!change.getBefore().equals("") && !change.getAfter().equals("")) {
            Change changed_sent = ChangeDetector.extendChange(change, text1, text2, false);

            boolean grammar = ChangeAnalyzer.isGrammar(changed_sent);
            if (grammar) {
                return Tag.GRAMMAR;
            }

            boolean rephrasing = ChangeAnalyzer.isRephrasing(changed_sent);
            if (rephrasing) {
                return Tag.REPHRASING;
            }
        }

        boolean topic_sim = ChangeAnalyzer.relatedTopics(change, text1);
        if (topic_sim) {
            return Tag.MINOR_TOPIC_CHANGE;
        } else {
            return Tag.MAJOR_TOPIC_CHANGE;
        }
    }

}