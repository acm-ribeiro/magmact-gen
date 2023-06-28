package oas_custom_parser.domain;

import java.util.ArrayList;
import java.util.List;

public class URI {
    private String uri;
    private List<String> parts;

    public URI(String uri) {
        this.uri = uri;
        parts = List.of(uri.replaceFirst("/", "").split("/"));
    }

    public List<String> getParts() {
        return parts;
    }

    public String getResourceName(){
        String last = "";

        for (int i = parts.size() - 1; i >= 0; i--)
            if(!parts.get(i).contains("{")) {
                last = parts.get(i);
                break;
            }

        return last;
    }

    /**
     * Returns the uri parameter names. An uri parameter is always between curly brackets.
     * @return uri parameters
     */
    public List<String> getParameters() {
        List<String> params = new ArrayList<>();

        for (String part : parts)
            if (part.contains("{"))
                params.add(part.replace("{", "").replace("}", ""));

        return params;
    }

    @Override
    public String toString() {
        return uri;
    }
}
