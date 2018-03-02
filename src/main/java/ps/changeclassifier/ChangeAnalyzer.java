package ps.changeclassifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
import ps.utils.LP;

/**
 * Class that provides tools for determining change's meaning.
 * Uses google-diff-match-patch library (@see https://github.com/GerHobbelt/google-diff-match-patch for more information)
 */
public class ChangeAnalyzer {

    // WordNet database
    private static ILexicalDatabase db = new NictWordNet();
    // Diff-Match-Patch tool
    private static diff_match_patch dmp = new diff_match_patch();

    private ChangeAnalyzer() {
    }

    /*
    Checks if change was a simple case of formatting.
    Simple matching.
    */
    protected static boolean isFormatting(Change change) {
        LinkedList<Diff> diff = dmp.diff_main(change.getBefore(), change.getAfter());
        dmp.diff_cleanupSemantic(diff);
        Object[] tmp = diff.stream().filter(d -> (d.operation == Operation.DELETE || d.operation == Operation.INSERT))
                .toArray();
        Diff[] filt_diff = new Diff[tmp.length];
        for (int i = 0; i < filt_diff.length; ++i) {
            filt_diff[i] = (Diff) tmp[i];
        }
        for (Diff d : filt_diff) {
            if (!LP.isFormattingSymbol(d.text)) {
                return false;
            }
        }
        return true;
    }

    /*
    Checks if a change cannot be classified.
    Simple matching (as for now, only changes of numbers cannot be classified).
    */
    protected static boolean isUndefined(Change change) {
        LinkedList<Diff> diff = dmp.diff_main(change.getBefore(), change.getAfter());
        Object[] tmp = diff.stream().filter(d -> (d.operation == Operation.DELETE || d.operation == Operation.INSERT))
                .toArray();
        Diff[] filt_diff = new Diff[tmp.length];
        for (int i = 0; i < filt_diff.length; ++i) {
            filt_diff[i] = (Diff) tmp[i];
        }
        for (Diff d : filt_diff) {
            if (!LP.isNumber(d.text) || !LP.isSymbol(d.text)) {
                return false;
            }
        }
        return true;
    }

    /*
    Checks if a change happened in an in-text citation.
    Simple matching.
    */
    protected static boolean isCitation(Change change) {
        if (LP.isQuote(change.getBefore()) && LP.isQuote(change.getAfter())) {
            // Double checking, but should never happen
            if (!change.getBefore().equals(change.getAfter())) {
                return true;
            }
        }
        return false;
    }

    /*
    Checks if a change was spelling correction.
    Damerau-Levenshtein distance of misspelled words equals 2 in 80% of cases.
    */
    protected static boolean isSpelling(Change change) {
        ArrayList<String> w1 = LP.tokenizeStopStem(change.getBefore(), false, true);
        ArrayList<String> w2 = LP.tokenizeStopStem(change.getAfter(), false, true);
        String before = "";
        String after = "";
        if (w1.size() == 1 && w2.size() == 1) {
            before = w1.get(0);
            if (!change.getBefore().equals(change.getBefore().toLowerCase())) {
                before = before.substring(0, 1).toUpperCase() + before.substring(1);
            }
            after = w2.get(0);
            if (!change.getAfter().equals(change.getAfter().toLowerCase())) {
                after = after.substring(0, 1).toUpperCase() + after.substring(1);
            }
            boolean misspelling = false;
            boolean correct = false;
            try {
                misspelling = !LP.inDictionary(before);
                correct = LP.inDictionary(after);
            } catch (IOException e) {
                System.out.println("Unable to open the WordNet dictionary");
                return false;
            }
            if (misspelling && correct) {
                LinkedList<Diff> diff = dmp.diff_main(before, after);
                dmp.diff_cleanupSemantic(diff);
                if (dmp.diff_levenshtein(diff) <= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
    Computes the word similarity in change.
    Wu-Palmer Similarity Measure.
    */
    protected static int substitutionSimilarity(Change change) {
        ArrayList<String> w1 = LP.tokenizeStopStem(change.getBefore(), false, true);
        ArrayList<String> w2 = LP.tokenizeStopStem(change.getAfter(), false, true);
        if (w1.size() == 1 && w2.size() == 1) {
            String before = w1.get(0);
            String after = w2.get(0);
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

    /*
    Checks if a change was grammar correction.
    Rule-based approach by Daniel Naber.
    */
    protected static boolean isGrammar(Change change) {
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

    /*
    Checks if change was rephrasing.
    Uses Fernando and Stevenson semantic similarity measure.
    */
    protected static boolean isRephrasing(Change change) {
        ArrayList<String> words1 = LP.tokenizeStopStem(change.getBefore(), true, true);
        ArrayList<String> words2 = LP.tokenizeStopStem(change.getAfter(), true, true);
        Set<String> w1 = new TreeSet<String>(words1);
        Set<String> w2 = new TreeSet<String>(words2);
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
        double[][] W = new JiangConrath(db).getSimilarityMatrix(w.toArray(new String[w.size()]),
                w.toArray(new String[w.size()]));

        for (int i = 0; i < W.length; ++i) {
            for (int j = 0; j < W[0].length; ++j) {
                if (W[i][j] > 1) {
                    W[i][j] = 1;
                }
            }
        }

        double sim = LP.fernandoSim(a, b, W);

        return sim > 0.5;
    }

    /*
    Checks how a change influenced the topic of a text.
    Novel feature-based approach inspired by topic modelling and feature-based word similarity measures.
    */
    protected static int relatedTopics(Change change, String text) {
        String before = "", after = "";
        if (change.getBefore().length() < change.getAfter().length()) {
            before = text;
            after = change.getAfter();
        } else if (change.getAfter().equals("")) {
            before = text;
            after = text.substring(change.getPos1())
                    + text.substring(change.getPos1() + change.getBefore().length(), text.length());
        } else {
            before = text;
            after = text.substring(change.getPos1()) + change.getAfter()
                    + text.substring(change.getPos1() + change.getBefore().length(), text.length());
        }
        ArrayList<String> features = LP.getFeatures(before, after);
        if (features.isEmpty()) {
            return -1;
        }

        Map<String, Double> dist1 = LP.getDistribution(features, before);
        Map<String, Double> dist2 = LP.getDistribution(features, after);

        boolean emptymap = true;
        for (double d1 : dist1.values()) {
            if (d1 != 0.0) {
                emptymap = false;
                break;
            }
        }
        if (emptymap) {
            return -1;
        }

        emptymap = true;
        for (double d2 : dist2.values()) {
            if (d2 != 0.0) {
                emptymap = false;
                break;
            }
        }
        if (emptymap) {
            return -1;
        }

        double score = LP.jsd(dist1, dist2);
        return score > 0.5 ? 0 : 1;
    }

}