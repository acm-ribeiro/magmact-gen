package serialization;

public class URLIntegerPropertyWrapper extends URLPropertyWrapper {

    private int min, max;
    private String name, type, format;

    public URLIntegerPropertyWrapper(String name, String type, int min, int max, String format) {
        this.name = name;
        this.type = type;
        this.min = min;
        this.max = max;
        this.format = format;
    }
}
