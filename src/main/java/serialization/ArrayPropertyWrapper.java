package serialization;

public class ArrayPropertyWrapper extends PropertyWrapper {

    private String itemsType, itemsFormat;

    public ArrayPropertyWrapper(String name, String type, boolean required, boolean gen,
                                String itemsType, String itemsFormat) {
        super(name, type, required, gen);
        this.itemsType = itemsType;
        this.itemsFormat = itemsFormat;
    }
}
