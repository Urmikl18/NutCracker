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
            ch_class.add(ChangeClassifier.classifyChange(changes.get(i), text1, text2));
            System.out.println((i + 1) + "/" + changes.size() + ": " + ch_class.get(i));
        }
        return ch_class;
    }

    /*
    Assigns each change a tag that describes change's meaning.
    */
    private static ChangeTag classifyChange(Change change, String text1, String text2) {
        Change changed_citation = ChangeDetector.extendChange(change, text1, text2, 0, false);
        // boolean citation = ChangeAnalyzer.isCitation(changed_citation);
        // if (citation) {
        //     return new ChangeTag(changed_citation, Tag.CITATION);
        // }

        // boolean formatting = ChangeAnalyzer.isFormatting(changed_citation, text1, text2);
        // if (formatting) {
        //     return new ChangeTag(changed_citation, Tag.FORMATTING);
        // }

        Change changed_word = ChangeDetector.extendChange(changed_citation, text1, text2, 1, false);
        // int spelling = ChangeAnalyzer.isSpelling(changed_word);
        // switch (spelling) {
        // case -1:
        //     return new ChangeTag(changed_word, Tag.UNDEFINED);
        // case 0:
        //     break;
        // case 1:
        //     return new ChangeTag(changed_word, Tag.SPELLING);
        // }

        // int sub_sim = ChangeAnalyzer.substitutionSimilarity(changed_word);
        // switch (sub_sim) {
        // // check something else
        // case -2:
        //     break;
        // case -1:
        //     return new ChangeTag(changed_word, Tag.UNDEFINED);
        // case 0:
        //     return new ChangeTag(changed_word, Tag.UNRELATED_TERM);
        // case 1:
        //     return new ChangeTag(changed_word, Tag.RELATED_TERM);
        // case 2:
        //     return new ChangeTag(changed_word, Tag.INTERCHANGEABLE);
        // }

        Change changed_sent = ChangeDetector.extendChange(changed_word, text1, text2, 2, false);

        int grammar = ChangeAnalyzer.isGrammar(changed_sent);
        switch (grammar) {
        case -1:
            break;
        case 0:
            return new ChangeTag(change, Tag.UNDEFINED);
        case 1:
            return new ChangeTag(changed_sent, Tag.GRAMMAR);
        }

        // boolean rephrasing = ChangeAnalyzer.isRephrasing(changed_sent);
        // if (rephrasing) {
        //     return new ChangeTag(changed_sent, Tag.REPHRASING);
        // }
        changed_sent = ChangeDetector.extendChange(changed_word, text1, text2, 2, true);
        // int topic_sim = ChangeAnalyzer.relatedTopics(change, text1);
        // switch (topic_sim) {
        // case -1:
        //     break;
        // case 0:
        //     return new ChangeTag(change, Tag.MINOR_TOPIC_CHANGE);
        // case 1:
        //     return new ChangeTag(change, Tag.MAJOR_TOPIC_CHANGE);
        // }
        return new ChangeTag(change, Tag.UNDEFINED);
    }

}
