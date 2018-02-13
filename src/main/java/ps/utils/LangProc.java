package ps.utils;

import java.io.IOException;
import java.text.BreakIterator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.cmu.lti.jawjaw.JAWJAW;
import edu.cmu.lti.jawjaw.pobj.POS;

public class LangProc {
    public static class Patterns {
        private static final String author = "(?:[A-Z][A-Za-z'`-]+)";
        private static final String etal = "(?:et al.?)";
        private static final String additional = "(?:,? (?:(?:and |& )?" + author + "|" + etal + "))";
        private static final String year_num = "(?:19|20)[0-9][0-9]";
        private static final String page_num = "(?:, p.? [0-9]+)?";
        private static final String year = "(?:, *" + year_num + page_num + "| *\\(" + year_num + page_num + "\\))";

        public static final String QUOTE1 = "\\[([A-Z]*?)([1-9][0-9]*?)\\]";
        public static final String QUOTE2 = "(" + author + additional + "*" + year + ")";
        public static final String FORMAT_CHAR = "(\\s)+";
        public static final String WORD_LIMIT = "(\\w|'|\\-)*";
        public static final String TRIM_START = "^(\\.|,|!|\\?|\\s)";
        public static final String TRIM_END = "(\\.|,|!|\\?|\\s)$";
    }

    public static boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static boolean isFormattingSymbol(String str) {
        Pattern p = Pattern.compile(Patterns.FORMAT_CHAR);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    public static boolean isQuote(String str) {
        Pattern p1 = Pattern.compile(Patterns.QUOTE1);
        Pattern p2 = Pattern.compile(Patterns.QUOTE2);

        Matcher m1 = p1.matcher(str);
        Matcher m2 = p2.matcher(str);

        return m1.matches() || m2.matches() || str.equals("");
    }

    public static boolean isWord(String str) {
        String regex = "^(\\w|'|-)+$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    public static int[] nearestWord(String text, int refPos1, int refPos2) {
        int tmp, start, end;
        BreakIterator bi = BreakIterator.getWordInstance(Locale.ENGLISH);
        bi.setText(text);
        tmp = bi.preceding(refPos1);
        start = tmp == BreakIterator.DONE ? 0 : tmp;
        tmp = bi.following(refPos2);
        end = tmp == BreakIterator.DONE ? text.length() : tmp;
        return new int[] { start, end };
    }

    public static int[] nearestSentence(String text, int refPos1, int refPos2) {
        Pattern p = Pattern.compile("([a-zA-Z\\-\\'0-9]+(\\.|\\. |'(s |re |t |m |ll )|s' | )?)+");
        Matcher m = p.matcher(text);
        while (m.find()) {
            if (m.start() <= refPos1 && refPos2 <= m.end()) {
                return new int[] { m.start(), m.end() };
            }
        }
        return new int[] { refPos1, refPos2 };
    }

    public static int[] nearestCitation(String text, int refPos1, int refPos2) {
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
        return null;
    }

    public static boolean inDictionary(String word) throws IOException {
        Set<String> s1 = JAWJAW.findSynonyms(word, POS.a);
        Set<String> s2 = JAWJAW.findSynonyms(word, POS.n);
        Set<String> s3 = JAWJAW.findSynonyms(word, POS.r);
        Set<String> s4 = JAWJAW.findSynonyms(word, POS.v);

        return !(s1.isEmpty() && s2.isEmpty() && s3.isEmpty() && s4.isEmpty());
    }

}