package ps.changeclassifier;

import java.io.IOException;
import java.util.LinkedList;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.LeacockChodorow;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;
import ps.models.Change;
import ps.models.ChangeTag;
import ps.models.ChangeTag.Tag;
import ps.utils.LangProc;

public class ChangeAnalyzer {

    private static ILexicalDatabase db = new NictWordNet();
    private diff_match_patch dmp;
    public static final ChangeAnalyzer INSTANCE = new ChangeAnalyzer();

    private ChangeAnalyzer() {
        this.dmp = new diff_match_patch();
    }

    protected ChangeTag[] getClassification(Change[] changes) {
        ChangeTag[] ch_class = new ChangeTag[changes.length];
        for (int i = 0; i < ch_class.length; ++i) {
            Tag tag = this.getTag(changes[i]);
            ch_class[i] = new ChangeTag(changes[i], tag);
        }
        return ch_class;
    }

    private Tag getTag(Change change) {
        LinkedList<Diff> diff = dmp.diff_main(change.getBefore(), change.getAfter());
        dmp.diff_cleanupSemantic(diff);
        Object[] tmp = diff.stream().filter(d -> (d.operation == Operation.DELETE || d.operation == Operation.INSERT))
                .toArray();
        Diff[] filt_diff = new Diff[tmp.length];
        for (int i = 0; i < filt_diff.length; ++i) {
            filt_diff[i] = (Diff) tmp[i];
        }

        boolean unclassified = isImpossibleToClassify(filt_diff);
        boolean formatting = isFormatting(filt_diff);
        boolean citation = isCitation(change.getBefore(), change.getAfter());
        boolean spelling = isSpelling(change.getBefore(), change.getAfter());
        boolean related_word = isRelatedWord(change.getBefore(), change.getAfter());

        if (formatting) {
            return Tag.FORMATTING;
        }
        if (citation) {
            return Tag.CITATION;
        }
        if (unclassified) {
            return Tag.IMPOSSIBLE_TO_CLASSIFY;
        }
        if (spelling) {
            return Tag.SPELLING;
        }
        if (related_word) {
            return Tag.RELATED_WORDS;
        }
        return Tag.UNDEFINED;
    }

    private boolean isImpossibleToClassify(Diff[] diffs) {
        for (Diff d : diffs) {
            if (!LangProc.isNumber(d.text)) {
                return false;
            }
        }
        return true;
    }

    private boolean isFormatting(Diff[] diffs) {
        for (Diff d : diffs) {
            if (!LangProc.isFormattingSymbol(d.text)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCitation(String before, String after) {
        if (LangProc.isQuote(before) && LangProc.isQuote(after)) {
            if (!before.equals(after)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpelling(String before, String after) {
        if (LangProc.isWord(before) && LangProc.isWord(after)) {
            boolean indict = false;
            try {
                indict = LangProc.inDictionary(before);

            } catch (IOException e) {
                System.out.println("Unable to open the WordNet dictionary");
                return false;
            }
            if (!indict) {
                LinkedList<Diff> diff = this.dmp.diff_main(before, after);
                this.dmp.diff_cleanupSemantic(diff);
                if (this.dmp.diff_levenshtein(diff) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRelatedWord(String before, String after) {
        if (LangProc.isWord(before) && LangProc.isWord(after)) {
            WS4JConfiguration.getInstance().setMFS(true);
            double sim = new LeacockChodorow(db).calcRelatednessOfWords(before, after);
            if (sim > 1) {
                return true;
            }
        }
        return false;
    }

}