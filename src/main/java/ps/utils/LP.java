package ps.utils;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishMinimalStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import edu.cmu.lti.jawjaw.JAWJAW;
import edu.cmu.lti.jawjaw.pobj.POS;

/**
 * Language processing module.
 */
public class LP {

    private static final TreeSet<String> dictionary = new TreeSet<>();

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
     * @return true, if string is a in-text citation, false, otherwise.
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
        return dictionary.contains(word);
    }

    public static void fillDictionary(String dict) {
        try (Stream<String> stream = Files.lines(Paths.get(dict))) {
            stream.forEach(w -> dictionary.add(w.toLowerCase()));
        } catch (IOException e) {
            System.out.println("Can't fill the dictionary");
        }
    }

    private static boolean isVowel(char c) {
        return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
    }

    private static boolean addE(String tmp) {
        if ((tmp.charAt(tmp.length() - 1) == 't' || tmp.charAt(tmp.length() - 1) == 's'
                || tmp.charAt(tmp.length() - 1) == 'z') && isVowel(tmp.charAt(tmp.length() - 2))) {
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
        }
        return false;
    }

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
                    } else if (tmp.charAt(tmp.length() - 1) == tmp.charAt(tmp.length() - 2)) {
                        tmp = tmp.substring(0, tmp.length() - 1);
                    } else if (addE(tmp)) {
                        tmp += "e";
                    }
                }
            } else if (pos.equals("VBG")) {
                if (word.endsWith("ing")) {
                    tmp = word.substring(0, word.length() - 3);
                    if (tmp.charAt(tmp.length() - 2) == tmp.charAt(tmp.length() - 3)) {
                        tmp = tmp.substring(0, tmp.length() - 1);
                    }
                }
            }
            result.set(0, tmp);
            result.set(1, "v");
        } else if (pos.startsWith("JJ")) {
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
        Pattern p = Pattern.compile(RegEx.SENTENCE);
        Matcher m = p.matcher(text);
        while (m.find()) {
            if (m.start() <= refPos1 && refPos2 <= m.end()) {
                return new int[] { m.start(), m.end() };
            }
        }
        return new int[] { refPos1, refPos2 };
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

    public static int numberOfSentences(String s) {
        Pattern p = Pattern.compile("^" + RegEx.SENTENCE + "$");
        Matcher m = p.matcher(s);
        int sentCount = 0;
        while (m.find()) {
            ++sentCount;
        }
        return sentCount;
    }

    /**
     * @param a vector representation of the first sentence.
     * @param b vector representation of the second sentence.
     * @param W similarity matrix between words.
     * @return value of Fernando-Stevenson similarity measure.
     */
    public static double fernandoSim(double[] a, double[] b, double[][] W) {
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

    /**
     * @param text1 initial version of the document
     * @param text2 modified version of the document
     * @return a list of features (covered WordNet domains) in texts
     */
    public static ArrayList<String> getFeatures(String text1, String text2) {
        Set<String> w1 = new TreeSet<String>(tokenizeStopStem(text1, true, true));
        Set<String> w2 = new TreeSet<String>(tokenizeStopStem(text2, true, true));
        Set<String> w = new TreeSet<>(w1);
        w.addAll(w2);
        Set<String> d = new TreeSet<>();
        for (String word : w) {
            for (POS pos : POS.values()) {
                try {
                    d.addAll(JAWJAW.findInDomains(word, pos));
                } catch (Exception e) {
                    continue;
                }
            }
        }
        ArrayList<String> features = new ArrayList<String>(d);
        features.sort((d1, d2) -> d1.compareTo(d2));
        return features;
    }

    /**
     * @param features whose distribution is to be found
     * @param text to be analyzed
     */
    public static Map<String, Double> getDistribution(ArrayList<String> features, String text) {
        Map<String, Double> dist = new TreeMap<>();
        ArrayList<String> words = tokenizeStopStem(text, true, true);
        Set<String> domains = new TreeSet<>();
        for (String f : features) {
            dist.put(f, 0.0);
        }
        int totalCount = 0;
        for (String word : words) {
            for (POS pos : POS.values()) {
                try {
                    domains = JAWJAW.findInDomains(word, pos);
                } catch (Exception e) {
                    domains = new TreeSet<>();
                }
                for (String domain : domains) {
                    if (dist.containsKey(domain)) {
                        ++totalCount;
                        double freq = dist.get(domain);
                        dist.put(domain, freq + 1);
                    }
                }
            }
        }

        if (totalCount != 0) {
            for (String key : dist.keySet()) {
                double newFreq = dist.get(key) / totalCount;
                dist.put(key, newFreq);
            }
        }
        return dist;
    }

    /**
     * @param dist1 first distribution
     * @param dist2 second distribution
     * @return Jensen-Shannon divergence for these distributions
     */
    public static double jsd(Map<String, Double> dist1, Map<String, Double> dist2) {
        Map<String, Double> mid = midDist(dist1, dist2);
        ArrayList<Double> p = new ArrayList<>(dist1.values());
        ArrayList<Double> q = new ArrayList<>(dist2.values());
        ArrayList<Double> m = new ArrayList<>(mid.values());
        return (dkl(p, m) + dkl(q, m)) / 2;
    }

    /*
    Calculates Kullback-Leibler divergence of two probability distributions.
    */
    private static double dkl(ArrayList<Double> p, ArrayList<Double> q) {
        double score = 0.0;
        for (int i = 0; i < p.size(); ++i) {
            double cont = q.get(i) == 0 || p.get(i) == 0 ? 0 : p.get(i) * Math.log(p.get(i) / q.get(i));
            score += cont;
        }
        return score;
    }

    /*
    Calculates the point-wise average of two distributions.
    */
    public static Map<String, Double> midDist(Map<String, Double> dist1, Map<String, Double> dist2) {
        Map<String, Double> mid = new TreeMap<>();
        for (String key : dist1.keySet()) {
            double freq = (dist1.get(key) + dist2.get(key)) / 2;
            mid.put(key, freq);
        }
        return mid;
    }

    /**
     * @param text to be parsed
     * @return A list of stemmed words without stop-words
     */
    public static ArrayList<String> tokenizeStopStem(String text, boolean stop, boolean stem) {
        ArrayList<String> words = new ArrayList<>();
        Analyzer analyzer = new StandardAnalyzer();
        TokenStream tokenStream = analyzer.tokenStream(null, new StringReader(text));
        if (stop) {
            tokenStream = new StopFilter(tokenStream, StandardAnalyzer.ENGLISH_STOP_WORDS_SET);
        }
        if (stem) {
            tokenStream = new EnglishMinimalStemFilter(tokenStream);
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

}