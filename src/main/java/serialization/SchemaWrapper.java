package serialization;

import oas_custom_parser.domain.APIProperty;

import java.util.ArrayList;
import java.util.List;

public class SchemaWrapper extends RequestBodySchema {

    private static final String INTEGER = "integer";
    private static final String STRING = "string";
    private static final String ARRAY = "array";

    private final String name, type;
    private final List<PropertyWrapper> properties;

    public SchemaWrapper(String type, String name, List<APIProperty> properties) {
        this.type = type;
        this.name = name;
        this.properties = new ArrayList<>();

        for (APIProperty prop : properties) {
            if (prop.getType() == null)  // the schema is referenced
                this.properties.add(new ObjectPropertyWrapper(prop.getName(), prop.getType(),
                        prop.isRequired(), prop.isGen(), prop.getRef()));
            else {
                switch (prop.getType().toLowerCase()) {
                    case INTEGER -> this.properties.add(
                            new IntegerPropertyWrapper(prop.getName(), prop.getType(),
                                    prop.isRequired(), prop.isGen(),
                                    prop.getMinimum(), prop.getMaximum(), prop.getFormat())
                    );
                    case STRING -> this.properties.add(
                            new StringPropertyWrapper(prop.getName(), prop.getType(),
                                    prop.isRequired(), prop.isGen(),
                                    prop.getPattern())
                    );
                    case ARRAY -> this.properties.add(
                            new ArrayPropertyWrapper(prop.getName(), prop.getType(),
                                    prop.isRequired(), prop.isGen(),
                                    prop.getItemsType(), prop.getItemsFormat())
                    );
                }
            }
        }
    }

    public List<PropertyWrapper> getProperties() {
        return properties;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(name + ":\n");

        if (properties.isEmpty())
            s.append(name).append(": ").append(type);
        else
            for (PropertyWrapper p : properties)
                s.append("\t").append(p.toString());

        return s.toString();
    }

}
