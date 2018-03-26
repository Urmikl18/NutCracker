package ps.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.knowledgebooks.nlp.fasttag.FastTag;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.impl.JiangConrath;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

/**
 * Language processing module.
 */
public class LP {
    // Dictionary, imported from resources/dictionary/dict[size].txt
    private static final TreeSet<String> dictionary = new TreeSet<>();
    // WordNet database
    private static ILexicalDatabase db = new NictWordNet();

    // public methods
    /**
     * @param str string to be tested.
     * @return true, if string is a number, false, otherwise.
     */
    public static boolean isNumber(String str) {
        try {
            Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    /**
     * @param str string to be tested.
     * @return true, if string is a symbol.
     */
    public static boolean isSymbol(String str) {
        Pattern p = Pattern.compile(RegEx.SYMBOL);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    /**
     * @param str string to be tested.
     * @return true, if string is a formatting symbol, false, otherwise.
     */
    public static boolean isFormattingSymbol(String str) {
        Pattern p = Pattern.compile(RegEx.FORMAT_CHAR);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    /**
     * @param str string to be tested.
     * @return true, if string is an in-text citation, false, otherwise.
     */
    public static boolean isQuote(String str) {
        Pattern p1 = Pattern.compile(RegEx.QUOTE1);
        Pattern p2 = Pattern.compile(RegEx.QUOTE2);
        Matcher m1 = p1.matcher(str);
        Matcher m2 = p2.matcher(str);

        return m1.matches() || m2.matches() || str.equals("");
    }

    /**
     * @param str string to be tested.
     * @return true, if a word is in a dictionary, false, otherwise.
     */
    public static boolean inDictionary(String word) {
        if (dictionary.isEmpty()) {
            fillDictionary();
        }
        return dictionary.contains(word);
    }

    /**
     * @param word Word to be stemmed.
     * @param pos POS-tag to be considered while stemming and to be modified.
     * @return A pair [stem, pos]
     */
    public static ArrayList<String> stem(String word, String pos) {
        ArrayList<String> result = new ArrayList<>(2);
        result.add(word);
        result.add(pos);
        if (pos.startsWith("NN") || pos.startsWith("CD")) {
            if (pos.equals("NNS")) {
                if (word.endsWith("s"))
                    result.set(0, word.substring(0, word.length() - 1));
            } else if (pos.equals("NNP")) {
                result.set(0, word.substring(0, 1).toUpperCase() + word.substring(1));
            } else if (pos.equals("NNPS")) {
                String tmp = word;
                if (word.endsWith("s"))
                    tmp = word.substring(0, word.length() - 1);
                result.set(0, tmp.substring(0, 1).toUpperCase() + tmp.substring(1));
            } else {
                result.set(0, word);
            }
            result.set(1, "n");
        } else if (pos.startsWith("VB")) {
            String tmp = word;
            if (pos.equals("VBD") || pos.equals("VBN")) {
                if (word.endsWith("ed")) {
                    tmp = word.substring(0, word.length() - 2);
                    if (tmp.endsWith("i")) {
                        tmp = tmp.substring(0, tmp.length() - 1) + "y";
                    } else if (tmp.length() > 1 && tmp.charAt(tmp.length() - 1) == tmp.charAt(tmp.length() - 2)) {
                        tmp = tmp.substring(0, tmp.length() - 1);
                    } else if (addE(tmp)) {
                        tmp += "e";
                    }
                }
            } else if (pos.equals("VBG")) {
                if (word.endsWith("ing")) {
                    tmp = word.substring(0, word.length() - 3);
                    if (tmp.length() > 2) {
                        if (tmp.charAt(tmp.length() - 2) == tmp.charAt(tmp.length() - 3)) {
                            tmp = tmp.substring(0, tmp.length() - 1);
                        }
                    }
                }
            }
            result.set(0, tmp);
            result.set(1, "v");
        } else if (pos.startsWith("JJ"))

        {
            result.set(0, word);
            result.set(1, "a");
        } else if (pos.startsWith("RB")) {
            result.set(0, word);
            result.set(1, "r");
        } else if (pos.startsWith("MD")) {
            result.set(0, word);
            result.set(1, "v");
        } else {
            result.set(0, word);
            result.set(1, null);
        }
        return result;
    }

    /**
     * @param text initial text.
     * @param refPos1 left-most position.
     * @param refPos2 right-most position.
     * @return positions of the closest in-text citation that contains refPos1 and refPos2
     */
    public static int[] nearestCitation(String text, int refPos1, int refPos2) {
        int start = refPos1 - 20;
        start = start < 0 ? 0 : start;
        int end = refPos2 + 20;
        end = end > text.length() ? text.length() : end;
        String test_seq = text.substring(start, end);

        Pattern p1 = Pattern.compile(RegEx.QUOTE1);
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

        Pattern p2 = Pattern.compile(RegEx.QUOTE2);
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

    /**
     * @param text initial text.
     * @param refPos1 left-most position.
     * @param refPos2 right-most position.
     * @return positions of the closest word that contains refPos1 and refPos2
     */
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

    /**
     * @param text initial text.
     * @param refPos1 left-most position.
     * @param refPos2 right-most position.
     * @return positions of the closest sentence that contains refPos1 and refPos2
     */
    public static int[] nearestSentence(String text, int refPos1, int refPos2) {
        int i = refPos1;
        while (i > 0 && text.charAt(i) != '.' && text.charAt(i) != '?' && text.charAt(i) != '!'
                && text.charAt(i) != '\n') {
            --i;
        }
        int j = refPos2;
        while (j != text.length() && text.charAt(j) != '.' && text.charAt(j) != '?' && text.charAt(j) != '!'
                && text.charAt(j) != '\n') {
            ++j;
        }
        return new int[] { i, j };
    }

    /**
     * @param text initial text.
     * @param refPos1 left-most position.
     * @param refPos2 right-most position.
     * @return positions of the closest paragraph that contains refPos1 and refPos2
     */
    public static int[] nearestParagraph(String text, int refPos1, int refPos2) {
        int i = refPos1 >= text.length() ? text.length() - 1 : refPos1;
        int j = refPos2 >= text.length() ? text.length() - 1 : refPos2;
        while (i > 0 && text.charAt(i) != '\n' && text.charAt(i) != '\r') {
            --i;
        }
        while (j < text.length() && text.charAt(j) != '\n' && text.charAt(j) != '\r') {
            ++j;
        }
        return new int[] { i, j };
    }

    /**
     * @param s Text fragment to be analyzed.
     * @return Number of sentences in given fragment.
     */
    public static int numberOfSentences(String s) {
        int sentCount = 1;
        String tmp = s.replaceAll("\\.\\.\\.", ".");
        tmp = tmp.replaceAll("\\?!", "?");
        for (int i = 0; i < tmp.length(); ++i) {
            if (tmp.charAt(i) == '.' || tmp.charAt(i) == '?' || tmp.charAt(i) == '!') {
                ++sentCount;
            }
        }
        return sentCount;
    }

    /**
     * @param s Text fragment to be analyzed.
     * @return Number of paragraphs in given fragment.
     */
    public static int numberOfParagraphs(String s) {
        Pattern p = Pattern.compile(RegEx.PARAGRAPH);
        Matcher m = p.matcher(s);
        int paraCount = -1;
        while (m.find()) {
            ++paraCount;
        }
        return paraCount;
    }

    /**
     * @param sentence Sentence to be checked.
     * @return JSON in a form of string with a list of matched rules. For more info @see https://languagetool.org/http-api/swagger-ui/#!/default/post_check
     */
    public static String checkGrammar(String sentence) {
        HttpURLConnection connection = null;
        String urlParameters = "text=" + sentence + "&language=en-US";
        try {
            //Create connection
            URL url = new URL("http://localhost:8081/v2/check");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            //Send request
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(urlParameters);
            wr.close();

            //Get Response  
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuffer response = new StringBuffer(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * @param before First text fragment.
     * @param after Second text fragment.
     * @return Similarity score as defined by Fernando and Stevenson:
     * <ol>
     * <li>Extract words from text fragments.</li>
     * <li>Remove stop words.</li>
     * <li>Remove numbers.</li>
     * <li>Assign POS-tag to each word.</li>
     * <li>Stem words and modify POS-tags to be used in WS4J</li>
     * <li>Transform input fragments to word vectors.</li>
     * <li>Create normalized similarity matrix using Jiang-Conrath word similarity measure</li>
     * <li>Compute similarity score</li>
     * </ol>
     */
    public static double semanticSimilarity(String before, String after) {
        ArrayList<String> words1 = LP.tokenizeStop(before, true);
        ArrayList<String> words2 = LP.tokenizeStop(after, true);

        words1 = words1.stream().filter(w -> !isNumber(w)).collect(Collectors.toCollection(ArrayList::new));
        words2 = words2.stream().filter(w -> !isNumber(w)).collect(Collectors.toCollection(ArrayList::new));

        if ((words1.isEmpty() || words2.isEmpty()) || (words1.equals(words2))) {
            return -1;
        }

        // get POS-tags for all words
        List<String> tags1 = FastTag.tag(words1);
        List<String> tags2 = FastTag.tag(words2);

        // assign each word its tag for better similarity scores
        for (int i = 0; i < words1.size(); ++i) {
            ArrayList<String> wp = LP.stem(words1.get(i), tags1.get(i));
            words1.set(i, wp.get(0) + "#" + wp.get(1));
        }

        for (int i = 0; i < words2.size(); ++i) {
            ArrayList<String> wp = LP.stem(words2.get(i), tags2.get(i));
            words2.set(i, wp.get(0) + "#" + wp.get(1));
        }

        Set<String> wd1 = new TreeSet<String>(words1);
        Set<String> wd2 = new TreeSet<String>(words2);
        Set<String> w = new TreeSet<>(wd1);
        w.addAll(wd2);

        // create a sorted vector of words
        ArrayList<String> words = new ArrayList<>(w);
        words.sort((o1, o2) -> o1.compareTo(o2));

        // represent first sentence as a vector
        double[] a = words.stream().mapToDouble(word -> {
            if (wd1.contains(word)) {
                return 1.0;
            } else {
                return 0.0;
            }
        }).toArray();

        // represent second sentence as a vector
        double[] b = words.stream().mapToDouble(word -> {
            if (wd2.contains(word)) {
                return 1.0;
            } else {
                return 0.0;
            }
        }).toArray();

        // calculte similarity matrix
        WS4JConfiguration.getInstance().setMFS(false);
        double[][] W = new JiangConrath(db).getNormalizedSimilarityMatrix(w.toArray(new String[w.size()]),
                w.toArray(new String[w.size()]));

        for (int i = 0; i < W.length; ++i) {
            for (int j = 0; j < W[0].length; ++j) {
                if (W[i][j] < 0.8) {
                    W[i][j] = 0;
                }
            }
        }

        // calculate similarity score
        double sim = LP.fernandoSim(a, b, W);
        return sim;
    }

    /**
     * @param text to be parsed
     * @return A list of words without stop-words.
     */
    public static ArrayList<String> tokenizeStop(String text, boolean stop) {
        ArrayList<String> words = new ArrayList<>();
        Analyzer analyzer = new StandardAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
        if (stop) {
            tokenStream = new StopFilter(tokenStream, StandardAnalyzer.ENGLISH_STOP_WORDS_SET);
        }

        final CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        try {
            tokenStream.reset();

            while (tokenStream.incrementToken()) {
                words.add(charTermAttribute.toString());
            }
            tokenStream.end();
            tokenStream.close();
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            analyzer.close();
        }

        return words;
    }
    // public methods

    // private methods
    private static void fillDictionary() {
        URL dict = LP.class.getClassLoader().getResource("dict60.txt");
        try (Stream<String> stream = Files.lines(Paths.get(dict.getPath()))) {
            stream.forEach(w -> dictionary.add(w.toLowerCase()));
        } catch (IOException e) {
            System.out.println("Can't fill the dictionary");
        }
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static boolean addE(String tmp) {
        if (tmp.endsWith("g")) {
            return true;
        }
        if (tmp.length() > 3) {
            if ((tmp.charAt(tmp.length() - 1) == 't' || tmp.charAt(tmp.length() - 1) == 's'
                    || tmp.charAt(tmp.length() - 1) == 'z' || tmp.charAt(tmp.length() - 1) == 'v')
                    && isVowel(tmp.charAt(tmp.length() - 2))) {
                return true;
            } else if (tmp.charAt(tmp.length() - 1) == 'r'
                    && (tmp.charAt(tmp.length() - 2) == 'u' && tmp.charAt(tmp.length() - 3) != 'o')
                    || tmp.charAt(tmp.length() - 2) == 'i') {
                return true;
            } else if (tmp.charAt(tmp.length() - 1) == 's' && tmp.charAt(tmp.length() - 2) == 'a') {
                return true;
            } else if (tmp.charAt(tmp.length() - 1) == 'c' && tmp.charAt(tmp.length() - 2) == 'u') {
                return true;
            } else if (tmp.endsWith("rg")) {
                return true;
            } else if (tmp.endsWith("in") && !tmp.endsWith("oin")) {
                return true;
            }
        }
        return false;
    }

    private static double fernandoSim(double[] a, double[] b, double[][] W) {
        double norm = vecLength(a) * vecLength(b);
        double[][] aM = new double[a.length][a.length];
        aM[0] = a;
        double[][] bM = new double[b.length][b.length];
        for (int i = 0; i < bM.length; ++i) {
            bM[i][0] = b[i];
        }
        double[][] mult = matrixMult(matrixMult(aM, W), bM);
        return mult[0][0] / norm;
    }

    private static double vecLength(double[] vec) {
        double sum = 0;
        for (int i = 0; i < vec.length; ++i) {
            sum += vec[i] * vec[i];
        }
        return Math.sqrt(sum);
    }

    private static double[][] matrixMult(double[][] a, double[][] b) {
        double[][] res = new double[a.length][b[0].length];
        for (int i = 0; i < a.length; ++i) {
            for (int j = 0; j < b[0].length; ++j) {
                for (int k = 0; k < a[0].length; ++k) {
                    res[i][j] += a[i][k] * b[k][j];
                }
            }
        }
        return res;
    }
    // private methods
}