package serialization;

public class PropertyWrapper {

    private String name, type;
    private boolean required, gen;

    public PropertyWrapper(String name, String type, boolean required, boolean gen) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.gen = gen;
    }

    @Override
    public String toString(){
        return String.format("{%s, %s, %b, %b}\n", name, type, required, gen);
    }
}
