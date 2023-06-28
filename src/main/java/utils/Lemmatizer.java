package utils;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.util.logging.RedwoodConfiguration;
import org.atteo.evo.inflector.English;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Lemmatizer {

    public static String CAMEL_CASE = "([a-z]+[A-Z]+\\w+)+";
    public static String SNAKE_CASE = "[a-z0-9]+(?:_[a-zA-Z0-9]+)*";
    public static String SPLIT_CAMEL = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])";
    public static String SPLIT_SNAKE = "_"; // words in snake case are separated by underscores

    public static String root(String word){
        // Disabling logger messages
        RedwoodConfiguration.current().clear().apply();

        // Building a new sentence: in our case it consists of only one word
        Sentence s = new Sentence(word);

        return s.lemmas().get(0);
    }


    /**
     * Checks whether a string is in camel case form. (camelCase)
     * @param s string to check
     * @return true if in camel case; false otherwise.
     */
    public static boolean is_camel(String s) {
        return s.matches(CAMEL_CASE);
    }

    /**
     * Checks whether a string is in snake case form. (snake_case; snake_CASE)
     * @param s string to check
     * @return true if in snake case; false otherwise.
     */
    public static boolean is_snake(String s) {
        return s.matches(SNAKE_CASE);
    }

    /**
     * Splits the given string into its parts. E.g., camelCase -> ["camel", "Case"]
     * @param s string in camel case form
     * @return string parts
     */
    public static List<String> split_camel(String s) {
        return Arrays.asList(s.split(SPLIT_CAMEL));
    }

    /**
     * Splits the given string into its parts. E.g., snake_case -> ["snake", "case"]
     * @param s string in snake case form
     * @return string parts
     */
    public static List<String> split_snake(String s) {
        return Arrays.asList(s.split(SPLIT_SNAKE));
    }

    /**
     * Returns the given word's plural form.
     * @param word  word to pluralize
     * @return  pluralized word.
     */
    public static String pluralize(String word) {
        return English.plural(word);
    }

}
