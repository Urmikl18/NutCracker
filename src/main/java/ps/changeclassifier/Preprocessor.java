package ps.changeclassifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.cmu.lti.ws4j.util.PorterStemmer;

public class Preprocessor {
    public static final Preprocessor INSTANCE = new Preprocessor();

    private PorterStemmer ps;

    private Preprocessor() {
        this.ps = new PorterStemmer();
    }

    public Map<String, Double> calculateTopic(String text) {
        Map<String, Double> topic = new HashMap<>();
        String str = text.toLowerCase();
        String word_regex = "\\b[a-z]+\\b";
        Pattern p = Pattern.compile(word_regex);
        Matcher m = p.matcher(str);
        ArrayList<String> words = new ArrayList<>();
        while (m.find()) {
            words.add(str.substring(m.start(), m.end()));
        }

        List<String> stem_words = words.stream().map((String word) -> {
            return ps.stemWord(word);
        }).collect(Collectors.toList());

        stem_words.sort((String w1, String w2) -> w1.compareTo(w2));
        String w = "";
        int count = 1;
        int word_number = stem_words.size();
        for (int i = 0; i < word_number - 1; ++i) {
            w = stem_words.get(i);
            if (w.equals(stem_words.get(i + 1))) {
                ++count;
            } else {
                topic.put(w, (double) (count));
                count = 1;
            }
        }
        w = stem_words.get(word_number - 1);
        if (w.equals(stem_words.get(word_number - 2))) {
            ++count;
        }
        topic.put(w, (double) count);
        return topic;
    }

}