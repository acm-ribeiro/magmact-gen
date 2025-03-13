package serialization;

public class StringPropertyWrapper extends PropertyWrapper {

    private String pattern;

    public StringPropertyWrapper(String name, String type, Boolean required, Boolean gen,
                                 String pattern, String refersTo) {
        super(name, type, required, gen, refersTo);
        this.pattern = pattern == null? "": pattern;
    }
}
