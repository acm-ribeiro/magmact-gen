package serialization;

public class IntegerPropertyWrapper extends PropertyWrapper{

    private String format;
    private int min, max;

    public IntegerPropertyWrapper(String name, String type, boolean required, boolean gen, int min, int max, String format) {
        super(name, type, required, gen);
        this.min = min;
        this.max = max;
        this.format = format;
    }
}
