package serialization;

import specification_parser.domain.APIProperty;

import java.util.ArrayList;
import java.util.List;

public class SchemaWrapper extends RequestBodySchema {
    private String name, type;
    private List<PropertyWrapper> properties;

    public SchemaWrapper(String type, String name, List<APIProperty> properties) {
        this.type = type;
        this.name = name;
        this.properties = new ArrayList<>();

        for (APIProperty prop : properties) {

            if (prop.getType() == null) { // the schema is referenced
                this.properties.add(new ObjectPropertyWrapper(prop.getName(), "object", false, false, prop.getRef()));
            } else {
                switch (prop.getType().toLowerCase()) {
                    case "integer" -> this.properties.add(
                            new IntegerPropertyWrapper(prop.getName(), prop.getType(), prop.isRequired(), prop.gen(),
                                    prop.getMinimum(), prop.getMaximum(), prop.getFormat())
                    );

                    case "string" -> this.properties.add(
                            new StringPropertyWrapper(prop.getName(), prop.getType(), prop.isRequired(), prop.gen(),
                                    prop.getPattern())
                    );

                    case "array" -> this.properties.add(
                            new ArrayPropertyWrapper(prop.getName(), prop.getType(), prop.isRequired(), prop.gen(),
                                    prop.getItemType())
                    );
                }
            }
        }
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
