package ps.changeclassifier;

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

public class ChangeDetector {
    private diff_match_patch dmp;
    public static final ChangeDetector INSTANCE = new ChangeDetector();

    private ChangeDetector() {
        this.dmp = new diff_match_patch();
    }

    protected Change[] getChanges(String text1, String text2) {
        LinkedList<Diff> deltas = this.dmp.diff_main(text1, text2);
        this.dmp.diff_cleanupSemantic(deltas);
        Diff[] diffs = deltas.toArray(new Diff[deltas.size()]);
        PositionedDiff[] pos_diffs = getDiffPositions(diffs);
        Change[] before_after = getChangedText(pos_diffs, text1, text2);
        Change[] ext_diffs = extendChanges(before_after, text1, text2);
        return ext_diffs;
    }

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

    private Change[] getChangedText(PositionedDiff[] diffs, String text1, String text2) {
        Object[] _commonParts = Arrays.stream(diffs).filter(d -> (d.getDiff().operation == Operation.EQUAL)).toArray();
        PositionedDiff[] commonParts = new PositionedDiff[_commonParts.length];
        for (int i = 0; i < commonParts.length; ++i) {
            commonParts[i] = new PositionedDiff((PositionedDiff) _commonParts[i]);
        }
        Change[] changes = new Change[commonParts.length - 1];
        String before = "";
        String after = "";
        int first1 = 0;
        int first2 = 0;
        int last1 = 0;
        int last2 = 0;
        int i = 0;
        for (i = 0; i < changes.length; ++i) {
            first1 = commonParts[i].getPos1() + commonParts[i].getDiff().text.length();
            first2 = commonParts[i].getPos2() + commonParts[i].getDiff().text.length();
            last1 = commonParts[i + 1].getPos1();
            last2 = commonParts[i + 1].getPos2();
            before = text1.substring(first1, last1);
            after = text2.substring(first2, last2);
            changes[i] = new Change(before, after, first1, first2);
        }
        return changes;
    }

    private Change[] extendChanges(Change[] diffs, String text1, String text2) {
        Change[] res = diffs.clone();
        int[] ind_ext = new int[2];
        String ext_text = "";

        Pattern p = Pattern.compile(Patterns.FORMAT_CHAR);
        Matcher m1, m2;

        for (int i = 0; i < res.length; ++i) {
            m1 = p.matcher(res[i].getBefore());
            m2 = p.matcher(res[i].getAfter());

            if ((m1.matches() || res[i].getBefore().equals("")) && (m2.matches() || res[i].getAfter().equals(""))) {
                continue;
            }

            ind_ext = getExtendedPosition(text1, res[i].getPos1(), res[i].getPos1() + res[i].getBefore().length());
            ext_text = text1.substring(ind_ext[0], ind_ext[1]);
            ext_text = ext_text.trim();
            ext_text = ext_text.replaceAll(Patterns.TRIM_START, "");
            ext_text = ext_text.replaceAll(Patterns.TRIM_END, "");
            res[i].setBefore(ext_text);

            ind_ext = getExtendedPosition(text2, res[i].getPos2(), res[i].getPos2() + res[i].getAfter().length());
            ext_text = text2.substring(ind_ext[0], ind_ext[1]);
            ext_text = ext_text.trim();
            ext_text = ext_text.replaceAll(Patterns.TRIM_START, "");
            ext_text = ext_text.replaceAll(Patterns.TRIM_END, "");
            res[i].setAfter(ext_text);
        }
        return res;
    }

    private int[] getExtendedPosition(String text, int refPos1, int refPos2) {
        int ind_space = 0;
        int ind_line = 0;
        int ind_ext1 = 0;
        int ind_ext2 = 0;

        int start = refPos1 - 20;
        start = start < 0 ? 0 : start;
        int end = refPos2 + 20;
        end = end > text.length() ? text.length() : end;
        String test_seq = text.substring(start, end);

        Pattern p1 = Pattern.compile(Patterns.QUOTE1);
        Matcher m1 = p1.matcher(test_seq);

        int ind_left_bracket = 0;
        int ind_right_bracket = 0;
        while (m1.find()) {
            ind_left_bracket = m1.start();
            ind_right_bracket = m1.end();
            if (refPos1 - start > ind_left_bracket && refPos2 - start < ind_right_bracket) {
                return new int[] { start + ind_left_bracket, start + ind_right_bracket };
            }
        }

        Pattern p2 = Pattern.compile(Patterns.QUOTE2);
        Matcher m2 = p2.matcher(test_seq);

        while (m2.find()) {
            ind_left_bracket = m2.start();
            ind_right_bracket = m2.end();
            if (refPos1 - start > ind_left_bracket && refPos2 - start < ind_right_bracket) {
                return new int[] { start + ind_left_bracket, start + ind_right_bracket };
            }
        }

        ind_space = text.lastIndexOf(" ", refPos1);
        ind_space = ind_space < 0 ? 0 : ind_space;
        ind_line = text.lastIndexOf("\n", refPos1);
        ind_line = ind_line < 0 ? 0 : ind_line;
        ind_ext1 = ind_line > ind_space ? ind_line : ind_space;

        ind_space = text.indexOf(" ", refPos2);
        ind_space = ind_space < 0 ? text.length() : ind_space;
        ind_line = text.indexOf("\n", refPos2);
        ind_line = ind_line < 0 ? text.length() : ind_line;
        ind_ext2 = ind_line < ind_space ? ind_line : ind_space;

        return new int[] { ind_ext1, ind_ext2 };
    }
}