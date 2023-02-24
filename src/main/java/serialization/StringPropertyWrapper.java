package serialization;

public class StringPropertyWrapper extends PropertyWrapper {

    private String pattern;

    public StringPropertyWrapper(String name, String type, Boolean required, Boolean gen, String pattern) {
        super(name, type, required, gen);
        this.pattern = pattern == null? "": pattern;
    }
}
