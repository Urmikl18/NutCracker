package ps.changeclassifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;
import ps.models.Change;
import ps.models.PositionedDiff;
import ps.utils.LangProc.Patterns;

/**
 * Singleton class for change detection in a plain-text document.
 * Uses google-diff-match-patch library (@see https://github.com/GerHobbelt/google-diff-match-patch for more information)
 */
public class ChangeDetector {
    private diff_match_patch dmp;
    public static final ChangeDetector INSTANCE = new ChangeDetector();

    private ChangeDetector() {
        this.dmp = new diff_match_patch();
    }

    /**
     * <ol>
     * <li> Calculate the shortest edit script. </li>
     * <li> Cleanup the script to group some diffs. </li>
     * <li> Assign diffs their positions in text. </li>
     * <li> Extend diffs to the nearest words. </li>
     * </ol>
     * <br>
     * @param text1 Text before change.
     * @param text2 Text after change.
     * @return A list of changes with their positions in text before and after changes made.
     */
    protected Change[] getChanges(String text1, String text2) {
        LinkedList<Diff> deltas = this.dmp.diff_main(text1, text2);
        this.dmp.diff_cleanupSemantic(deltas);
        Diff[] diffs = deltas.toArray(new Diff[deltas.size()]);
        PositionedDiff[] pos_diffs = getDiffPositions(diffs);
        Change[] before_after = getChangedText(pos_diffs, text1, text2);
        Change[] ext_changes = extendChanges(before_after, text1, text2);
        Change[] group_changes = groupChanges(ext_changes, text1, text2);
        return group_changes;
    }

    /*
        Assigns each diff its positions in text before and after the changes took place.
    */
    private PositionedDiff[] getDiffPositions(Diff[] diffs) {
        PositionedDiff[] res = new PositionedDiff[diffs.length];
        int it1 = 0, it2 = 0, pos1 = 0, pos2 = 0;
        for (int i = 0; i < res.length; ++i) {
            if (diffs[i].operation.equals(diff_match_patch.Operation.DELETE)) {
                pos1 = it1;
                pos2 = -1;
                it1 += diffs[i].text.length();
            } else if (diffs[i].operation.equals(diff_match_patch.Operation.INSERT)) {
                pos1 = -1;
                pos2 = it2;
                it2 += diffs[i].text.length();
            } else {
                pos1 = it1;
                pos2 = it2;
                it1 += diffs[i].text.length();
                it2 += diffs[i].text.length();
            }
            res[i] = new PositionedDiff(diffs[i], pos1, pos2);
        }
        return res;
    }

    /*
        Get the modified text between equal parts.
    */
    private Change[] getChangedText(PositionedDiff[] diffs, String text1, String text2) {
        Object[] _commonParts = Arrays.stream(diffs).filter(d -> (d.getDiff().operation == Operation.EQUAL)).toArray();
        PositionedDiff[] commonParts = new PositionedDiff[_commonParts.length];
        for (int i = 0; i < commonParts.length; ++i) {
            commonParts[i] = new PositionedDiff((PositionedDiff) _commonParts[i]);
        }
        Change[] changes = new Change[commonParts.length + 1];
        String before = "";
        String after = "";
        int first1 = 0;
        int first2 = 0;
        int last1 = 0;
        int last2 = 0;
        int i = 0;

        for (i = 0; i < changes.length; ++i) {
            first1 = i == 0 ? 0 : commonParts[i - 1].getPos1() + commonParts[i - 1].getDiff().text.length();
            first2 = i == 0 ? 0 : commonParts[i - 1].getPos2() + commonParts[i - 1].getDiff().text.length();
            last1 = i == changes.length - 1 ? text1.length() : commonParts[i].getPos1();
            last2 = i == changes.length - 1 ? text2.length() : commonParts[i].getPos2();
            before = text1.substring(first1, last1);
            after = text2.substring(first2, last2);
            changes[i] = new Change(before, after, first1, first2);
        }

        if (changes[0].getAfter().equals("") && changes[1].getBefore().equals("")) {
            if (changes[changes.length - 1].getBefore().equals("")
                    && changes[changes.length - 1].getAfter().equals("")) {
                return Arrays.copyOfRange(changes, 1, changes.length - 1);
            } else {
                return Arrays.copyOfRange(changes, 1, changes.length);
            }
        } else {
            if (changes[changes.length - 1].getBefore().equals("")
                    && changes[changes.length - 1].getAfter().equals("")) {
                return Arrays.copyOfRange(changes, 0, changes.length - 1);
            } else {
                return Arrays.copyOfRange(changes, 0, changes.length);
            }
        }
    }

    /*
        Extend modified text to full entities:
        <ul>
        <li>group whitespace characters;</li>
        <li>retrieve the whole citation if part of it was changed;</li>
        <li>extend changes to full words.</li>
        </ul>
    */
    private Change[] extendChanges(Change[] diffs, String text1, String text2) {
        Change[] res = diffs.clone();
        int[] ind_ext = new int[2];
        String ext_text = "";

        for (int i = 0; i < res.length; ++i) {
            ind_ext = getExtendedPosition(text1, res[i].getBefore(), res[i].getPos1(),
                    res[i].getPos1() + res[i].getBefore().length());
            ext_text = text1.substring(ind_ext[0], ind_ext[1]);
            res[i].setBefore(ext_text);
            res[i].setPos1(ind_ext[0]);

            ind_ext = getExtendedPosition(text2, res[i].getAfter(), res[i].getPos2(),
                    res[i].getPos2() + res[i].getAfter().length());
            ext_text = text2.substring(ind_ext[0], ind_ext[1]);
            res[i].setAfter(ext_text);
            res[i].setPos2(ind_ext[0]);
        }
        return res;
    }

    /*
        Returns positions in text to which change has to be extended.
    */
    private int[] getExtendedPosition(String text, String match, int refPos1, int refPos2) {

        int start = refPos1 - 20;
        start = start < 0 ? 0 : start;
        int end = refPos2 + 20;
        end = end > text.length() ? text.length() : end;
        String test_seq = text.substring(start, end);

        // Detect the first type of citation
        Pattern p1 = Pattern.compile(Patterns.QUOTE1);
        Matcher m1 = p1.matcher(test_seq);

        int ind_left = 0;
        int ind_right = 0;
        while (m1.find()) {
            ind_left = m1.start();
            ind_right = m1.end();
            if (refPos1 - start >= ind_left && refPos2 - start <= ind_right) {
                return new int[] { start + ind_left, start + ind_right };
            }
        }

        // Detect the second type of citation
        Pattern p2 = Pattern.compile(Patterns.QUOTE2);
        Matcher m2 = p2.matcher(test_seq);

        while (m2.find()) {
            ind_left = m2.start();
            ind_right = m2.end();
            if (refPos1 - start >= ind_left && refPos2 - start <= ind_right) {
                return new int[] { start + ind_left, start + ind_right };
            }
        }

        // Detect everything else
        String regex = match.equals("") ? "(\\w|'|-)+" : Patterns.WORD_LIMIT + match + Patterns.WORD_LIMIT;
        Pattern p3 = Pattern.compile(regex);
        Matcher m3 = p3.matcher(test_seq);

        while (m3.find()) {
            ind_left = m3.start();
            ind_right = m3.end();
            if (refPos1 - start >= ind_left && refPos2 - start <= ind_right) {
                return new int[] { start + ind_left, start + ind_right };
            }
        }

        return new int[] { ind_left, ind_right };
    }

    private Change[] groupChanges(Change[] changes, String text1, String text2) {
        ArrayList<Change> result = new ArrayList<>();
        ArrayList<Change> tmp = new ArrayList<>();
        for (Change c : changes) {
            tmp.add(c);
        }
        int intercount = 1;
        while (intercount > 0) {
            intercount = 0;
            result = (ArrayList<Change>) tmp.clone();
            tmp.clear();
            for (int i = 0; i < result.size() - 1; ++i) {
                Change c1 = result.get(i);
                Change c2 = result.get(i + 1);
                if (intersect(c1, c2)) {
                    ++intercount;
                    int pos1 = Math.min(c1.getPos1(), c2.getPos1());
                    int pos2 = Math.min(c1.getPos2(), c2.getPos2());
                    int limit1 = Math.max(c1.getPos1() + c1.getBefore().length(),
                            c2.getPos1() + c2.getBefore().length());
                    int limit2 = Math.max(c1.getPos2() + c1.getAfter().length(), c2.getPos2() + c2.getAfter().length());
                    Change c = new Change(text1.substring(pos1, limit1), text2.substring(pos2, limit2), pos1, pos2);
                    tmp.add(c);
                }
            }
        }
        return result.toArray(new Change[result.size()]);
    }

    private boolean intersect(Change c1, Change c2) {
        return c1.getPos1() + c1.getBefore().length() > c2.getPos1()
                || c1.getPos2() + c1.getAfter().length() > c2.getPos2();
    }
}