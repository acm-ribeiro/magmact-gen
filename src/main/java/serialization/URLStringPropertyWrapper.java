package serialization;

public class URLStringPropertyWrapper extends URLPropertyWrapper {

    private String name, type, pattern;

    public URLStringPropertyWrapper(String name, String type, String pattern){
        this.name = name;
        this.type = type;
        this.pattern = pattern;
    }
}
