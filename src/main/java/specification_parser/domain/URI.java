package specification_parser.domain;

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
            if(!parts.get(i).contains("{")){
                last = parts.get(i);
                break;
            }

        return last;
    }

    @Override
    public String toString() {
        return uri;
    }
}
