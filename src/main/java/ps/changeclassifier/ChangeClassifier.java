package ps.changeclassifier;

import java.util.ArrayList;
import java.util.stream.Collectors;

import ps.models.Change;
import ps.models.ChangeTag;
import ps.models.ChangeTag.Tag;
import ps.utils.LP;

/**
 * Class that classifies changes in a plain-text document.
 * Showcases the algorithm proposed in the thesis.
 */
public class ChangeClassifier {
    private ChangeClassifier() {
    }

    // public methods
    /**
     * @param changes list of changes to be analyzed.
     * @param text1 initial version of the document.
     * @param text2 modified version of the document.
     * @return list of changes with tags describing the meaning of a change.
     */
    public static ArrayList<ChangeTag> getClassification(ArrayList<Change> changes, String text1, String text2) {
        ArrayList<ChangeTag> ch_class = new ArrayList<ChangeTag>(changes.size());
        for (int i = 0; i < changes.size(); ++i) {
            ChangeTag ct = ChangeClassifier.classifyChange(changes.get(i), text1, text2);
            ch_class.add(ct);
            System.out.println((i + 1) + "/" + changes.size() + ": " + ct);
        }
        return ch_class;
    }
    // public methods

    // private methods
    // Assigns each change a tag that describes change's meaning.
    private static ChangeTag classifyChange(Change change, String text1, String text2) {
        Change changed_citation = ChangeDetector.extendChange(change, text1, text2, 0);
        boolean citation = ChangeAnalyzer.isCitation(changed_citation);
        if (citation) {
            return new ChangeTag(changed_citation, Tag.CITATION);
        }

        boolean formatting = ChangeAnalyzer.isFormatting(changed_citation, text1, text2);
        if (formatting) {
            return new ChangeTag(changed_citation, Tag.FORMATTING);
        }

        Change changed_word = ChangeDetector.extendChange(changed_citation, text1, text2, 1);
        int spelling = ChangeAnalyzer.isSpelling(changed_word);
        switch (spelling) {
        case -1:
            return new ChangeTag(changed_word, Tag.UNDEFINED);
        case 0:
            break;
        case 1:
            return new ChangeTag(changed_word, Tag.SPELLING);
        }

        int sub_sim = ChangeAnalyzer.substitutionSimilarity(changed_word);
        switch (sub_sim) {
        case -1:
            break;
        case 0:
            return new ChangeTag(changed_word, Tag.UNRELATED_TERM);
        case 1:
            return new ChangeTag(changed_word, Tag.RELATED_TERM);
        case 2:
            return new ChangeTag(changed_word, Tag.INTERCHANGEABLE);
        }

        Change changed_sent = ChangeDetector.extendChange(changed_word, text1, text2, 2);

        ArrayList<String> w1 = LP.tokenizeStop(changed_word.getBefore(), false);
        ArrayList<String> w2 = LP.tokenizeStop(changed_word.getAfter(), false);
        w1 = w1.stream().filter(w -> !LP.isNumber(w)).collect(Collectors.toCollection(ArrayList::new));
        w2 = w2.stream().filter(w -> !LP.isNumber(w)).collect(Collectors.toCollection(ArrayList::new));
        if (w1.size() > 1 || w2.size() > 1) {
            boolean rephrasing = ChangeAnalyzer.isRephrasing(changed_sent);
            if (rephrasing) {
                return new ChangeTag(changed_sent, Tag.REPHRASING);
            }
        }

        int grammar = ChangeAnalyzer.isGrammar(changed_sent);
        switch (grammar) {
        case -1:
            break;
        case 0:
            return new ChangeTag(changed_sent, Tag.UNDEFINED);
        case 1:
            return new ChangeTag(changed_sent, Tag.GRAMMAR);
        }

        w1 = LP.tokenizeStop(changed_word.getBefore(), false);
        w2 = LP.tokenizeStop(changed_word.getAfter(), false);
        w1 = w1.stream().filter(w -> !LP.isNumber(w)).collect(Collectors.toCollection(ArrayList::new));
        w2 = w2.stream().filter(w -> !LP.isNumber(w)).collect(Collectors.toCollection(ArrayList::new));
        if (w1.size() > 2 || w2.size() > 2) {
            int topic_sim = ChangeAnalyzer.relatedTopics(changed_sent, text1);
            switch (topic_sim) {
            case -1:
                break;
            case 0:
                return new ChangeTag(changed_sent, Tag.MINOR_TOPIC_CHANGE);
            case 1:
                return new ChangeTag(changed_sent, Tag.MAJOR_TOPIC_CHANGE);
            }
        }
        return new ChangeTag(change, Tag.UNDEFINED);
    }
    // private methods
}
