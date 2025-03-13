package serialization;

public class ArrayPropertyWrapper extends PropertyWrapper {

    private String itemsType, itemsFormat;

    public ArrayPropertyWrapper(String name, String type, boolean required, boolean gen,
                                String itemsType, String itemsFormat, String refersTo) {
        super(name, type, required, gen, refersTo);
        this.itemsType = itemsType;
        this.itemsFormat = itemsFormat;
    }
}
