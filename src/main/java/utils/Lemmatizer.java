package utils;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;

public class Lemmatizer {
    public static String root(String word){
        // Disabling logger messages
        RedwoodConfiguration.current().clear().apply();

        // Building a new sentence: in our case it consists of only one word
        Sentence s = new Sentence(word);

        return s.lemmas().get(0);
    }
}
