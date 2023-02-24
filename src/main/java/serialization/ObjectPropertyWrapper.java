package serialization;

public class ObjectPropertyWrapper  extends PropertyWrapper {

    private String ref;

    public ObjectPropertyWrapper(String name, String type, boolean required, boolean gen, String ref) {
        super(name, type, required, gen);
        this.ref = ref;
    }
}
