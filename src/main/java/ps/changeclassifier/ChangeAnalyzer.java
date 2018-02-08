package ps.changeclassifier;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;

import edu.cmu.lti.jawjaw.JAWJAW;
import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.WuPalmer;
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

        boolean citation = isCitation(change.getBefore(), change.getAfter());
        if (citation) {
            return Tag.CITATION;
        }

        boolean spelling = isSpelling(change.getBefore(), change.getAfter());
        if (spelling) {
            return Tag.SPELLING;
        }

        boolean formatting = isFormatting(filt_diff);
        if (formatting) {
            return Tag.FORMATTING;
        }

        int sub_sim = substitutionSimilarity(change.getBefore(), change.getAfter());
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
        return Tag.UNDEFINED;
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
            // Double checking, but should never happen
            if (!before.equals(after)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpelling(String before, String after) {
        if (LangProc.isWord(before) && LangProc.isWord(after)) {
            boolean misspelling = false;
            boolean correct = false;
            try {
                misspelling = !LangProc.inDictionary(before);
                correct = LangProc.inDictionary(after);
            } catch (IOException e) {
                System.out.println("Unable to open the WordNet dictionary");
                return false;
            }
            if (misspelling && correct) {
                LinkedList<Diff> diff = this.dmp.diff_main(before, after);
                this.dmp.diff_cleanupSemantic(diff);
                if (this.dmp.diff_levenshtein(diff) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private int substitutionSimilarity(String before, String after) {
        if (LangProc.isWord(before) && LangProc.isWord(after)) {
            Set<String> s = JAWJAW.findSynonyms(before, POS.a);
            s.addAll(JAWJAW.findSynonyms(before, POS.n));
            s.addAll(JAWJAW.findSynonyms(before, POS.r));
            s.addAll(JAWJAW.findSynonyms(before, POS.v));

            for (String str : s) {
                if (str.equals(after)) {
                    return 2;
                }
            }

            WS4JConfiguration.getInstance().setMFS(false);
            double sim = new WuPalmer(db).calcRelatednessOfWords(before, after);
            if (sim > 0.5) {
                return 1;
            } else if (sim > 0) {
                return 0;
            }
        }
        return -1;
    }

}