package serialization;

public class PropertyWrapper {

    private String name;
    private final String type;
    private boolean required, gen;

    public PropertyWrapper(String name, String type, boolean required, boolean gen) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.gen = gen;
    }

    public boolean isReferenced() {
        return name.contains("#");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public void setGen(boolean gen) {
        this.gen = gen;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isGen() {
        return gen;
    }

    @Override
    public String toString() {
        return String.format("%s: {type: %s, required: %b, gen: %b}\n", name, type, required, gen);
    }
}
