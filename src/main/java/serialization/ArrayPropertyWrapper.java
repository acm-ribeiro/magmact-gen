package serialization;

public class ArrayPropertyWrapper extends PropertyWrapper {

    private String itemsType;

    public ArrayPropertyWrapper(String name, String type, boolean required, boolean gen, String itemsType) {
        super(name, type, required, gen);
        this.itemsType = itemsType;
    }
}
