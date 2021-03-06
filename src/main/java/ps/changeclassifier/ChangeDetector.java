package ps.changeclassifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.stream.Collectors;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;
import ps.models.Change;
import ps.models.PositionedDiff;
import ps.utils.LP;
import ps.utils.RegEx;

/**
 * Class for change detection in a plain-text document.
 * Uses google-diff-match-patch library (@see https://github.com/GerHobbelt/google-diff-match-patch for more information)
 */
public class ChangeDetector {
    private static diff_match_patch dmp = new diff_match_patch();

    private ChangeDetector() {
    }

    // public methods
    /**
     * <ol>
     * <li> Calculate the shortest edit script. </li>
     * <li> Cleanup the script to group some diffs. </li>
     * <li> Assign diffs their positions in text. </li>
     * </ol>
     * <br>
     * @param text1 Text before change.
     * @param text2 Text after change.
     * @return A list of changes with their positions in text before and after changes made.
     */
    public static ArrayList<Change> getChanges(String text1, String text2) {
        LinkedList<Diff> deltas = dmp.diff_main(text1, text2);
        dmp.diff_cleanupSemantic(deltas);
        Diff[] diffs = deltas.toArray(new Diff[deltas.size()]);
        ArrayList<PositionedDiff> pos_diffs = getDiffPositions(diffs);
        ArrayList<Change> before_after = getChangedText(pos_diffs, text1, text2);
        return before_after;
    }
    // public methods

    // protected methods
    // Extends a change to specified entity.
    // Options: 0 - citation, 1 - word, 2 - sentence.
    protected static Change extendChange(Change c, String text1, String text2, int option) {
        int pos1 = 0, pos2 = 0;
        String before = "", after = "";
        int[] extension = new int[2];
        switch (option) {
        case 0: {
            extension = LP.nearestCitation(text1, c.getPos1(), c.getPos1() + c.getBefore().length());
            if (extension == null) {
                return c;
            }
            pos1 = extension[0];
            before = text1.substring(extension[0], extension[1]).replaceAll(RegEx.TRIM_START, "")
                    .replaceAll(RegEx.TRIM_END, "");
            extension = LP.nearestCitation(text2, c.getPos2(), c.getPos2() + c.getAfter().length());
            if (extension == null) {
                return c;
            }
            pos2 = extension[0];
            after = text2.substring(extension[0], extension[1]).replaceAll(RegEx.TRIM_START, "")
                    .replaceAll(RegEx.TRIM_END, "");
            break;
        }
        case 1: {
            extension = LP.nearestWord(text1, c.getPos1(), c.getPos1() + c.getBefore().length());
            pos1 = extension[0];
            before = text1.substring(extension[0], extension[1]).replaceAll(RegEx.TRIM_START, "")
                    .replaceAll(RegEx.TRIM_END, "");
            extension = LP.nearestWord(text2, c.getPos2(), c.getPos2() + c.getAfter().length());
            pos2 = extension[0];
            after = text2.substring(extension[0], extension[1]).replaceAll(RegEx.TRIM_START, "")
                    .replaceAll(RegEx.TRIM_END, "");
            break;
        }
        case 2: {
            extension = LP.nearestSentence(text1, c.getPos1(), c.getPos1() + c.getBefore().length());
            pos1 = extension[0];
            before = text1.substring(extension[0], extension[1]).replaceAll(RegEx.TRIM_START, "")
                    .replaceAll(RegEx.TRIM_END, "");
            extension = LP.nearestSentence(text2, c.getPos2(), c.getPos2() + c.getAfter().length());
            pos2 = extension[0];
            after = text2.substring(extension[0], extension[1]).replaceAll(RegEx.TRIM_START, "")
                    .replaceAll(RegEx.TRIM_END, "");
            break;
        }
        default: {
            return c;
        }
        }
        return new Change(before, after, pos1, pos2);
    }
    // protected methods

    // private methods
    // Assigns each diff its positions in text before and after the changes took place.
    private static ArrayList<PositionedDiff> getDiffPositions(Diff[] diffs) {
        ArrayList<PositionedDiff> res = new ArrayList<PositionedDiff>(diffs.length);
        int it1 = 0, it2 = 0, pos1 = 0, pos2 = 0;
        for (int i = 0; i < diffs.length; ++i) {
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
            res.add(new PositionedDiff(diffs[i], pos1, pos2));
        }
        return res;
    }

    // Get the modified text between equal parts.
    private static ArrayList<Change> getChangedText(ArrayList<PositionedDiff> diffs, String text1, String text2) {
        ArrayList<PositionedDiff> commonParts = diffs.stream().filter(d -> (d.getDiff().operation == Operation.EQUAL))
                .collect(Collectors.toCollection(ArrayList::new));
        int length = commonParts.size() + 1;
        ArrayList<Change> changes = new ArrayList<>(length);
        String before = "";
        String after = "";
        int first1 = 0;
        int first2 = 0;
        int last1 = 0;
        int last2 = 0;
        int i = 0;

        for (i = 0; i < length; ++i) {
            first1 = i == 0 ? 0 : commonParts.get(i - 1).getPos1() + commonParts.get(i - 1).getDiff().text.length();
            first2 = i == 0 ? 0 : commonParts.get(i - 1).getPos2() + commonParts.get(i - 1).getDiff().text.length();
            last1 = i == length - 1 ? text1.length() : commonParts.get(i).getPos1();
            last2 = i == length - 1 ? text2.length() : commonParts.get(i).getPos2();
            before = text1.substring(first1, last1);
            after = text2.substring(first2, last2);
            changes.add(new Change(before, after, first1, first2));
        }

        if (changes.get(0).getAfter().equals("") && changes.get(0).getBefore().equals("")) {
            if (changes.get(changes.size() - 1).getBefore().equals("")
                    && changes.get(changes.size() - 1).getAfter().equals("") && changes.size() > 1) {
                return new ArrayList<Change>(changes.subList(1, changes.size() - 1));
            } else {
                return new ArrayList<Change>(changes.subList(1, changes.size()));
            }
        } else {
            if (changes.get(changes.size() - 1).getBefore().equals("")
                    && changes.get(changes.size() - 1).getAfter().equals("")) {
                return new ArrayList<Change>(changes.subList(0, changes.size() - 1));
            } else {
                return new ArrayList<Change>(changes.subList(0, changes.size()));
            }
        }
    }
    // private methods

}