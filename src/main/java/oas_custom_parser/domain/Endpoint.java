package oas_custom_parser.domain;

import org.openapi4j.parser.model.v3.Parameter;

import java.util.List;

public class Endpoint {

    private String uri;
    private List<Parameter> parameters;

    public Endpoint(String uri, List<Parameter> parameters) {
        this.uri = uri;
        this.parameters = parameters;
    }

    public String getUri() {
        return uri;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }
}
