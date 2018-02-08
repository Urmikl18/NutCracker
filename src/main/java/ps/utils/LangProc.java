package ps.utils;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.POS;

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
        public static final String TRIM_START = "^(\\.|,|!|\\?)";
        public static final String TRIM_END = "(\\.|,|!|\\?)$";
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
        String regex = "^[A-Za-z]+$";
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    public static boolean inDictionary(String word) throws IOException {
        String wnpath = "src/main/resources/wordnet/dict";
        URL url = new URL("file", null, wnpath);
        IDictionary dict = new Dictionary(url);
        dict.open();

        IIndexWord idxWord1 = dict.getIndexWord(word, POS.ADJECTIVE);
        IIndexWord idxWord2 = dict.getIndexWord(word, POS.ADVERB);
        IIndexWord idxWord3 = dict.getIndexWord(word, POS.NOUN);
        IIndexWord idxWord4 = dict.getIndexWord(word, POS.VERB);

        dict.close();
        return idxWord1 != null || idxWord2 != null || idxWord3 != null || idxWord4 != null;
    }

}