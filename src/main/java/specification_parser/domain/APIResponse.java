package specification_parser.domain;

import java.util.List;

public class APIResponse {
    public static final int DEFAULT = 999;

    private int responseCode;
    private transient String description, contentSchemaType;
    private ResponseSchema contentSchema;
    private List<APILink> links;
    private List<String> contentTypes;

    public APIResponse(String responseCode, String description, ResponseSchema contentSchema, String contentSchemaType, List<APILink> links, List<String> contentTypes) {
        try {
            this.responseCode = Integer.parseInt(responseCode);
        } catch (NumberFormatException e) {
            // it's a default response
            this.responseCode = DEFAULT;
        }
        this.description = description;
        this.contentSchema = contentSchema;
        this.contentSchemaType = contentSchemaType;
        this.links = links;
        this.contentTypes = contentTypes;
    }

    public List<String> getContentTypes () {
        return contentTypes;
    }

    public int getResponseCode () {
        return responseCode;
    }

    public ResponseSchema getContentSchema() {
        return contentSchema;
    }

    public List<APILink> getLinks() {
        return links;
    }

    public boolean hasLinks () {
        return links != null;
    }

}
