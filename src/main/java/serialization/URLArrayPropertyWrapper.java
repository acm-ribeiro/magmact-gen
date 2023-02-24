package serialization;

public class URLArrayPropertyWrapper extends URLPropertyWrapper {

    private String name, type, itemsType;

    public URLArrayPropertyWrapper (String name, String type, String itemsType) {
        this.name = name;
        this.type = type;
        this.itemsType = itemsType;
    }
}
