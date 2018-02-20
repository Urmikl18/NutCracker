package ps.utils;

/**
 * Collection of regular expression used in application.
 */
public class RegEx {
    private static final String author = "(?:[A-Z][A-Za-z'`-]+)";
    private static final String etal = "(?:et al.?)";
    private static final String additional = "(?:,? (?:(?:and |& )?" + author + "|" + etal + "))";
    private static final String year_num = "(?:19|20)[0-9][0-9]";
    private static final String page_num = "(?:, p.? [0-9]+)?";
    private static final String year = "(?:, *" + year_num + page_num + "| *\\(" + year_num + page_num + "\\))";

    public static final String QUOTE1 = "\\[([A-Z]*?)([1-9][0-9]*?)\\]";
    public static final String QUOTE2 = "(" + author + additional + "*" + year + ")";
    public static final String FORMAT_CHAR = "(\\s)+";
    public static final String TRIM_START = "^(\\.|,|!|\\?|\\s)";
    public static final String TRIM_END = "(\\.|,|!|\\?|\\s)$";
}