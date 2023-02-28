package serialization;

import oas_custom_parser.domain.APIProperty;

import java.util.List;

public class URLParameterWrapper {

    private String name, in;
    private boolean required;
    private URLPropertyWrapper schema;

    public URLParameterWrapper(String in, String name, boolean required, List<APIProperty> schema) {
        this.in = in;
        this.name = name;
        this.required = required;

        for (APIProperty prop : schema) {
            switch (prop.getType().toLowerCase()) {
                case "integer":
                    this.schema = new URLIntegerPropertyWrapper(prop.getName(), prop.getType(), prop.getMinimum(), prop.getMaximum(), prop.getFormat());
                    break;
                case "string":
                    String pattern = prop.getPattern() != null? prop.getPattern() : "";
                    this.schema = new URLStringPropertyWrapper(prop.getName(), prop.getType(), pattern);
                    break;
                case "array":
                    System.out.println(prop.getItemType());
                    this.schema = new URLArrayPropertyWrapper(prop.getName(), prop.getType(), prop.getItemType());
                    break;
            }
        }
    }
}
