package ps.changeclassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.languagetool.JLanguageTool;
import org.languagetool.language.AmericanEnglish;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;

import edu.cmu.lti.jawjaw.JAWJAW;
import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.JiangConrath;
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

    private ILexicalDatabase db;
    private diff_match_patch dmp;

    public ChangeAnalyzer() {
        this.dmp = new diff_match_patch();
        this.db = new NictWordNet();
    }

    protected ChangeTag[] getClassification(Change[] changes, String text1, String text2) {
        ChangeTag[] ch_class = new ChangeTag[changes.length];
        for (int i = 0; i < ch_class.length; ++i) {
            Tag tag = this.getTag(changes[i], text1, text2);
            ch_class[i] = new ChangeTag(changes[i], tag);
        }
        return ch_class;
    }

    private Tag getTag(Change change, String text1, String text2) {
        boolean citation = isCitation(change);
        if (citation) {
            return Tag.CITATION;
        }

        boolean spelling = isSpelling(change);
        if (spelling) {
            return Tag.SPELLING;
        }

        boolean formatting = isFormatting(change);
        if (formatting) {
            return Tag.FORMATTING;
        }

        int sub_sim = substitutionSimilarity(change);
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

            boolean grammar = isGrammar(changed_sent);
            if (grammar) {
                return Tag.GRAMMAR;
            }

            boolean rephrasing = isRephrasing(changed_sent);
            if (rephrasing) {
                return Tag.REPHRASING;
            }
        }

        return Tag.UNDEFINED;

    }

    private boolean isFormatting(Change change) {
        LinkedList<Diff> diff = dmp.diff_main(change.getBefore(), change.getAfter());
        dmp.diff_cleanupSemantic(diff);
        Object[] tmp = diff.stream().filter(d -> (d.operation == Operation.DELETE || d.operation == Operation.INSERT))
                .toArray();
        Diff[] filt_diff = new Diff[tmp.length];
        for (int i = 0; i < filt_diff.length; ++i) {
            filt_diff[i] = (Diff) tmp[i];
        }
        for (Diff d : filt_diff) {
            if (!LangProc.isFormattingSymbol(d.text)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCitation(Change change) {
        if (LangProc.isQuote(change.getBefore()) && LangProc.isQuote(change.getAfter())) {
            // Double checking, but should never happen
            if (!change.getBefore().equals(change.getAfter())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpelling(Change change) {
        if (LangProc.isWord(change.getBefore()) && LangProc.isWord(change.getAfter())) {
            boolean misspelling = false;
            boolean correct = false;
            try {
                misspelling = !LangProc.inDictionary(change.getBefore());
                correct = LangProc.inDictionary(change.getAfter());
            } catch (IOException e) {
                System.out.println("Unable to open the WordNet dictionary");
                return false;
            }
            if (misspelling && correct) {
                LinkedList<Diff> diff = this.dmp.diff_main(change.getBefore(), change.getAfter());
                this.dmp.diff_cleanupSemantic(diff);
                if (this.dmp.diff_levenshtein(diff) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private int substitutionSimilarity(Change change) {
        if (LangProc.isWord(change.getBefore()) && LangProc.isWord(change.getAfter())) {
            Set<String> s = JAWJAW.findSynonyms(change.getBefore(), POS.a);
            s.addAll(JAWJAW.findSynonyms(change.getBefore(), POS.n));
            s.addAll(JAWJAW.findSynonyms(change.getBefore(), POS.r));
            s.addAll(JAWJAW.findSynonyms(change.getBefore(), POS.v));

            for (String str : s) {
                if (str.equals(change.getAfter())) {
                    return 2;
                }
            }

            WS4JConfiguration.getInstance().setMFS(false);
            double sim = new WuPalmer(this.db).calcRelatednessOfWords(change.getBefore(), change.getAfter());
            if (sim > 0.5) {
                return 1;
            } else if (sim > 0) {
                return 0;
            }
        }
        return -1;
    }

    private boolean isGrammar(Change change) {
        boolean correct_before = false, correct_after = false;
        JLanguageTool langTool = new JLanguageTool(new AmericanEnglish());
        List<RuleMatch> matches = null;
        try {
            matches = langTool.check(change.getBefore());
            matches.removeIf(rule -> rule.getRule() instanceof SpellingCheckRule);
            correct_before = matches.isEmpty();

            matches = langTool.check(change.getAfter());
            matches.removeIf(rule -> rule.getRule() instanceof SpellingCheckRule);
            correct_after = matches.isEmpty();
        } catch (IOException e) {
            return false;
        }
        return !correct_before && correct_after;
    }

    private boolean isRephrasing(Change change) {
        Pattern p = Pattern.compile("(\\w|'|-)+");
        Matcher m = p.matcher(change.getBefore());
        Set<String> w1 = new TreeSet<>();
        Set<String> w2 = new TreeSet<>();
        while (m.find()) {
            w1.add(change.getBefore().substring(m.start(), m.end()).toLowerCase());
        }

        m = p.matcher(change.getAfter());
        while (m.find()) {
            w2.add(change.getAfter().substring(m.start(), m.end()).toLowerCase());
        }

        Set<String> w = new TreeSet<>(w1);
        w.addAll(w2);

        ArrayList<String> words = new ArrayList<>(w);
        words.sort((o1, o2) -> o1.compareTo(o2));

        double[] a = words.stream().mapToDouble(word -> {
            if (w1.contains(word)) {
                return 1.0;
            } else {
                return 0.0;
            }
        }).toArray();

        double[] b = words.stream().mapToDouble(word -> {
            if (w2.contains(word)) {
                return 1.0;
            } else {
                return 0.0;
            }
        }).toArray();

        WS4JConfiguration.getInstance().setMFS(false);
        double[][] W = new JiangConrath(this.db).getSimilarityMatrix(w.toArray(new String[w.size()]),
                w.toArray(new String[w.size()]));

        for (int i = 0; i < W.length; ++i) {
            for (int j = 0; j < W[0].length; ++j) {
                if (W[i][j] > 1) {
                    W[i][j] = 1;
                }
            }
        }

        double sim = fernandoSim(a, b, W);

        return sim > 0.5;
    }

    // sim(a,b) = a*W*b^t/|a|*|b|
    double fernandoSim(double[] a, double[] b, double[][] W) {
        double norm = vecLength(a) * vecLength(b);
        double[][] aM = new double[a.length][a.length];
        aM[0] = a;
        double[][] bM = new double[b.length][b.length];
        for (int i = 0; i < bM.length; ++i) {
            bM[i][0] = b[i];
        }
        double[][] mult = matrixMult(matrixMult(aM, W), bM);
        return mult[0][0] / norm;
    }

    double vecLength(double[] vec) {
        double sum = 0;
        for (int i = 0; i < vec.length; ++i) {
            sum += vec[i] * vec[i];
        }
        return Math.sqrt(sum);
    }

    double[][] matrixMult(double[][] a, double[][] b) {
        double[][] res = new double[a.length][b[0].length];
        for (int i = 0; i < a.length; ++i) {
            for (int j = 0; j < b[0].length; ++j) {
                for (int k = 0; k < a[0].length; ++k) {
                    res[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return res;
    }

}