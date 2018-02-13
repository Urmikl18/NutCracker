package ps.changeclassifier;

import java.util.Arrays;
import java.util.LinkedList;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;
import ps.models.Change;
import ps.models.PositionedDiff;
import ps.utils.LangProc;
import ps.utils.LangProc.Patterns;

/**
 * Singleton class for change detection in a plain-text document.
 * Uses google-diff-match-patch library (@see https://github.com/GerHobbelt/google-diff-match-patch for more information)
 */
public class ChangeDetector {
    private diff_match_patch dmp;

    public ChangeDetector() {
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
        return ext_changes;
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

        if (changes[0].getAfter().equals("") && changes[0].getBefore().equals("")) {
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
        <li>retrieve the whole citation if part of it was changed;</li>
        <li>extend changes to full words;</li>
        <li>extend changes to full sentences.</li>
        </ul>
    */
    private Change[] extendChanges(Change[] diffs, String text1, String text2) {
        Change[] res = new Change[diffs.length];
        for (int i = 0; i < diffs.length; ++i) {
            res[i] = extendChange(diffs[i], text1, text2, true);
        }
        return res;
    }

    protected static Change extendChange(Change c, String text1, String text2, boolean word) {
        int pos1 = 0, pos2 = 0;
        String before = "", after = "";
        int[] extension = new int[2];
        extension = LangProc.nearestCitation(text1, c.getPos1(), c.getPos1() + c.getBefore().length());
        if (extension == null) {
            if (word) {
                extension = LangProc.nearestWord(text1, c.getPos1(), c.getPos1() + c.getBefore().length());
            } else {
                extension = LangProc.nearestSentence(text1, c.getPos1(), c.getPos1() + c.getBefore().length());
            }
        }
        pos1 = extension[0];
        before = text1.substring(extension[0], extension[1]).replaceAll(Patterns.TRIM_START, "")
                .replaceAll(Patterns.TRIM_END, "");

        extension = LangProc.nearestCitation(text2, c.getPos2(), c.getPos2() + c.getAfter().length());
        if (extension == null) {
            if (word) {
                extension = LangProc.nearestWord(text2, c.getPos2(), c.getPos2() + c.getAfter().length());
            } else {
                extension = LangProc.nearestSentence(text2, c.getPos2(), c.getPos2() + c.getAfter().length());
            }
        }

        pos2 = extension[0];
        after = text2.substring(extension[0], extension[1]).replaceAll(Patterns.TRIM_START, "")
                .replaceAll(Patterns.TRIM_END, "");

        return new Change(before, after, pos1, pos2);
    }

}