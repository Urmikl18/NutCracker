package ps.changeclassifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.cmu.lti.jawjaw.JAWJAW;
import edu.cmu.lti.jawjaw.pobj.POS;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.HirstStOnge;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;
import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import ps.models.Change;
import ps.utils.LP;

import com.knowledgebooks.nlp.fasttag.FastTag;

/**
 * Class that provides tools for determining change's meaning.
 */
public class ChangeAnalyzer {

    // WordNet database
    private static ILexicalDatabase db = new NictWordNet();
    // Diff-Match-Patch tool
    private static diff_match_patch dmp = new diff_match_patch();

    private ChangeAnalyzer() {
    }

    // protected methods
    /*
    Checks if a change happened in a citation.
    Simple matching.
    */
    protected static boolean isCitation(Change change) {
        String cite1 = change.getBefore().trim();
        String cite2 = change.getAfter().trim();
        if (cite1.isEmpty() && cite2.isEmpty()) {
            return false;
        }
        if (LP.isQuote(cite1) && LP.isQuote(cite2)) {
            return true;
        }
        return false;
    }

    /*
    Checks if change was a simple case of formatting.
    Simple matching.
    */
    protected static boolean isFormatting(Change change, String text1, String text2) {
        if (LP.isFormattingSymbol(change.getBefore()) && LP.isFormattingSymbol(change.getAfter())) {
            return true;
        }
        if (LP.isFormattingSymbol(change.getAfter()) && change.getBefore().equals("")) {
            if (change.getPos1() == 0 || change.getPos1() + change.getBefore().length() == text1.length()) {
                return true;
            }
            char c1 = text1.charAt(change.getPos1() - 1);
            char c2 = text1.charAt(change.getPos1() + change.getBefore().length());
            if (!Character.isLetter(c1) || !Character.isLetter(c2)) {
                return true;
            }
        }
        if (LP.isFormattingSymbol(change.getBefore()) && change.getAfter().equals("")) {
            if (change.getPos2() == 0 || change.getPos2() + change.getAfter().length() == text2.length()) {
                return true;
            }
            char c1 = text2.charAt(change.getPos2() - 1);
            char c2 = text2.charAt(change.getPos2() + change.getAfter().length());
            if (!Character.isLetter(c1) || !Character.isLetter(c2)) {
                return true;
            }
        }
        return false;
    }

    /*
    1. Check if both changes are words.
    2. If both are not in dictionary return -1.
    3. If the first one is not in dictionary and has a distance of <= 2, but not 0, then SPELLING.
    4. If both words are correct, and are equal ignoring case, then it is spelling (e.g. north -> North).
    */
    protected static int isSpelling(Change change) {
        ArrayList<String> w1 = LP.tokenizeStop(change.getBefore(), false);
        ArrayList<String> w2 = LP.tokenizeStop(change.getAfter(), false);
        String before = "";
        String after = "";
        if (w1.size() == 1 && w2.size() == 1) {
            before = w1.get(0);
            after = w2.get(0);

            boolean misspelling = !LP.inDictionary(before);
            boolean correct = LP.inDictionary(after);
            if (misspelling && !correct) {
                return -1;
            }
            if (misspelling && correct) {
                LinkedList<Diff> diff = dmp.diff_main(before, after);
                int dist = dmp.diff_levenshtein(diff);
                if (dist <= 2 && dist != 0) {
                    return 1;
                }
            }
            if (!misspelling && correct) {
                if (before.equals(after)) {
                    return -1;
                }
                if (before.equalsIgnoreCase(after) && LP.inDictionary(before) && LP.inDictionary(after)) {
                    return 1;
                }
            }
        }
        return 0;
    }

    /*
    1. Checks if both changes are individual words.
    2. Both words must be in dictionary.
    3. Determine part-of-speech for both words.
    4. Both words must have the same part of speech.
    5. Modify tags to make them match to WS4J POS-class.
    6. Simple stemming (e.g. remove -s from plurals etc).
    6. Check if words are synonyms (they are if one of them in the synset of the other).
    7. Calculate similarity score (HirstStOnge allows to calculate the score between numerous POS).
    8. Threshold of 5 (min: 0, max: 16).
    */
    protected static int substitutionSimilarity(Change change) {
        // get words
        ArrayList<String> w1 = LP.tokenizeStop(change.getBefore(), false);
        ArrayList<String> w2 = LP.tokenizeStop(change.getAfter(), false);
        String before, after;
        String tag1, tag2;
        POS pos1 = null, pos2 = null;

        // substitution of single words
        if (w1.size() == 1 && w2.size() == 1) {
            before = w1.get(0);
            after = w2.get(0);
            if (before.equals(after)) {
                return -1;
            }
            // both words should be in dictionary
            if (!LP.inDictionary(before) || !LP.inDictionary(after)) {
                return -1;
            }
            // assing POS-tag using FastTag
            tag1 = FastTag.tag(w1).get(0);
            tag2 = FastTag.tag(w2).get(0);

            // similarity measures can compare only words with same POS
            boolean cond1 = tag1.substring(0, 2).equals(tag2.substring(0, 2));
            boolean cond2 = ((tag1.equals("MD") && tag2.startsWith("VB"))
                    || (tag1.startsWith("VB") && tag2.equals("MD")));
            boolean cond3 = ((tag1.equals("CD") && tag2.startsWith("NN"))
                    || (tag1.startsWith("NN") && tag2.equals("CD")));

            if (!cond1 && !cond2 && !cond3) {
                return -1;
            }

            // modify POS-tags to match the ones of WS4J and stem words if needed
            ArrayList<String> tmp1 = LP.stem(before, tag1);
            ArrayList<String> tmp2 = LP.stem(after, tag2);

            if (tmp1.get(1) == null || tmp2.get(1) == null) {
                return -1;
            }

            if (tmp1.get(0).equals(tmp2.get(0))) {
                return -1;
            }

            before = tmp1.get(0);
            pos1 = POS.valueOf(tmp1.get(1));
            after = tmp2.get(0);
            pos2 = POS.valueOf(tmp2.get(1));

            // if no POS-tag assigned, stop because no valid decision can be made
            if (pos1 == null || pos2 == null) {
                return -1;
            }

            // check if words are synonyms
            Set<String> s = JAWJAW.findSynonyms(before, pos1);
            for (String str : s) {
                if (str.equals(after)) {
                    return 2;
                }
            }

            // compute similarity score
            before = before + "#" + pos1;
            after = after + "#" + pos2;
            WS4JConfiguration.getInstance().setMFS(false);
            double sim = new HirstStOnge(db).calcRelatednessOfWords(before, after);
            if (sim >= 5) {
                return 1;
            } else {
                return 0;
            }
        }

        return -1;
    }

    /*
    Checks if change was rephrasing.
    1. Check is sentence contains any changes that have not been previously analyzed.
    2. Rephrasing is a property of sentences, but not more than two.
    3. Tokenize sentences, assign POS-tags, stem if needed.
    4. Compute binary word vectors.
    5. Compute similarity matrix using Jiang-Conrath similarity.
    6. Compute Fernando-Stevenson similarity score.
    */
    protected static boolean isRephrasing(Change changed_sent) {
        ArrayList<Change> localChanges = ChangeDetector.getChanges(changed_sent.getBefore(), changed_sent.getAfter());
        // things that can't be rephrasing
        boolean cond1, cond2, cond3, cond4, cond5, cond6;
        boolean other = true;
        for (Change c : localChanges) {
            cond1 = isCitation(c);
            cond2 = isFormatting(c, changed_sent.getBefore(), changed_sent.getAfter());
            cond4 = LP.isNumber(c.getBefore()) || LP.isSymbol(c.getBefore());
            cond5 = LP.isNumber(c.getAfter()) || LP.isSymbol(c.getAfter());
            Change cw = ChangeDetector.extendChange(c, changed_sent.getBefore(), changed_sent.getAfter(), 1);
            cond3 = isSpelling(cw) == 1;
            cond6 = substitutionSimilarity(cw) != -1;
            other = cond1 || cond2 || cond3 || (cond4 && cond5) || cond6;
        }
        if (other) {
            return false;
        }

        int sentCount1 = LP.numberOfSentences(changed_sent.getBefore());
        int sentCount2 = LP.numberOfSentences(changed_sent.getAfter());

        if ((sentCount1 != 1 && sentCount1 != 2) || (sentCount2 != 1 && sentCount2 != 2)) {
            return false;
        }

        double sim = LP.semanticSimilarity(changed_sent.getBefore(), changed_sent.getAfter());
        return sim >= 0.3;
    }

    /*
    Checks if a change was grammar correction.
    1. Check is sentence contains any changes that have not been previously analyzed.
    2. Grammar is a property of a sentence.
    3. Check grammar in both sentences.
    4. Ignore spelling errors (they are analyzed using other method).
    5. If both are correct, it may be rephrasing or topic changes.
    6. If both are incorrect or correct sentence was turned into incorrect, no decision can be made.
    7. If original version was incorrect, and modified is correct, then it was grammar change.
    */
    protected static int isGrammar(Change changed_sent) {
        ArrayList<Change> localChanges = ChangeDetector.getChanges(changed_sent.getBefore(), changed_sent.getAfter());
        // things that can't be rephrasing
        boolean cond1, cond2, cond3, cond4, cond5, cond6;
        boolean other = true;
        for (Change c : localChanges) {
            cond1 = isCitation(c);
            cond2 = isFormatting(c, changed_sent.getBefore(), changed_sent.getAfter());
            cond4 = LP.isNumber(c.getBefore()) || LP.isSymbol(c.getBefore());
            cond5 = LP.isNumber(c.getAfter()) || LP.isSymbol(c.getAfter());
            Change cw = ChangeDetector.extendChange(c, changed_sent.getBefore(), changed_sent.getAfter(), 1);
            cond3 = isSpelling(cw) == 1;
            cond6 = substitutionSimilarity(cw) != -1;
            other = cond1 || cond2 || cond3 || (cond4 && cond5) || cond6;
        }
        if (other) {
            return -1;
        }

        int sentCount1 = LP.numberOfSentences(changed_sent.getBefore());
        int sentCount2 = LP.numberOfSentences(changed_sent.getAfter());

        if (sentCount1 != 1 || sentCount2 != 1) {
            return -1;
        }
        String check1 = LP.checkGrammar(changed_sent.getBefore());
        String check2 = LP.checkGrammar(changed_sent.getAfter());
        if (check1 == null || check2 == null) {
            return -1;
        }
        JSONObject grammar1 = new JSONObject(check1);
        JSONObject grammar2 = new JSONObject(check2);
        JSONArray matches1 = (JSONArray) grammar1.get("matches");
        JSONArray matches2 = (JSONArray) grammar2.get("matches");
        boolean onlyMisspelling = true;
        for (int i = 0; i < matches1.length(); ++i) {
            JSONObject match = matches1.getJSONObject(i);
            JSONObject rule = match.getJSONObject("rule");
            String issueType = rule.getString("issueType");
            if (!issueType.equals("misspelling")) {
                onlyMisspelling = false;
            }
        }
        if (onlyMisspelling) {
            return -1;
        }
        boolean correct_before = matches1.length() == 0 || onlyMisspelling;

        for (int i = 0; i < matches2.length(); ++i) {
            JSONObject match = matches2.getJSONObject(i);
            JSONObject rule = match.getJSONObject("rule");
            String issueType = rule.getString("issueType");
            if (!issueType.equals("misspelling")) {
                onlyMisspelling = false;
            }
        }
        if (onlyMisspelling) {
            return -1;
        }
        boolean correct_after = matches2.length() == 0 || onlyMisspelling;
        if (!correct_before && !correct_after) {
            return 0;
        } else if (correct_before && !correct_after) {
            return 0;
        } else if (correct_before && correct_after) {
            return -1;
        } else {
            return 1;
        }
    }

    /*
    Checks how a change influenced the topic of a text.
    1. Performed if sentences are not grammar correction of paraphrases.
    2. There should be at least one sentence.
    3. Extend change to its context (here neighbouring paragraphs).
    4. Compute Fernando and Stevenson similarity score between text fragments before and after changes.
    */
    protected static int relatedTopics(Change changed_sent, String text) {
        // things that can't be rephrasing
        boolean cond1, cond2;
        cond1 = isRephrasing(changed_sent);
        cond2 = isGrammar(changed_sent) == 1;
        boolean other = cond1 || cond2;
        if (other) {
            return -1;
        }

        int sentCount1 = LP.numberOfSentences(changed_sent.getBefore());
        int sentCount2 = LP.numberOfSentences(changed_sent.getAfter());

        // too short of a change to be something important
        if (sentCount1 == 0 && sentCount2 == 0) {
            return -1;
        }

        // pick text fragments that need to be compared
        String before = "", after = "";
        int[] para = LP.nearestParagraph(text, changed_sent.getPos1(),
                changed_sent.getPos1() + changed_sent.getBefore().length());
        before = text.substring(para[0], para[1]);
        after = text.substring(para[0], changed_sent.getPos1()) + changed_sent.getAfter()
                + text.substring(changed_sent.getPos1() + changed_sent.getBefore().length(), para[1]);

        double score = LP.semanticSimilarity(before, after);
        return score >= 0.5 ? 0 : 1;
    }
    // protected methods

}