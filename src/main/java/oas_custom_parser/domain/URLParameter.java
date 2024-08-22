package oas_custom_parser.domain;

import java.util.List;

public class URLParameter {

    private final String in, name;
    private final boolean required;
    private final List<APIProperty> schema;

    public URLParameter(String in, String name, boolean required, List<APIProperty> schema) {
        this.in = in;
        this.name = name;
        this.required = required;
        this.schema = schema;
    }

    public String getIn() {
        return in;
    }

    public String getName() {
        return name;
    }

    public List<APIProperty> getSchema() {
        return schema;
    }

    public boolean isRequired() {
        return required;
    }

}
