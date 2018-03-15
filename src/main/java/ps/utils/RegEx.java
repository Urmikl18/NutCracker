package ps.utils;

/**
 * Collection of regular expression used in application.
 */
public class RegEx {
    private static final String author = "(?:[A-Z][A-Za-z'`-]+)";
    private static final String etal = "(?:et al.?)";
    private static final String additional = "(?:,? (?:(?:and |& )?" + author + "|" + etal + "))";
    private static final String year = "(?:,? ?\\(?(?:19|20)[0-9][0-9]\\)?)";
    private static final String page = "(?:,? ?p?.? ?[0-9]+)?";

    public static final String QUOTE1 = "\\[([A-Z]*?)([1-9][0-9]*?)\\]";
    public static final String QUOTE2 = "(\\(" + author + "*" + additional + "*" + year + "*" + page + "\\))";
    public static final String FORMAT_CHAR = "(\\s)+";
    public static final String TRIM_START = "^(\\.|,|!|\\?|\\s)";
    public static final String TRIM_END = "(\\.|,|!|\\?|\\s)$";
    public static final String SYMBOL = "\\W+";
    public static final String SENTENCE = "([a-zA-Z\\-\\'0-9]+(\\.|\\. |'(s |re |t |m |ll )|s' | )?)+";
}