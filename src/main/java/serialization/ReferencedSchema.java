package serialization;


public class ReferencedSchema extends RequestBodySchema {

    private static final String PATH = "#/schemas/";

    private String ref;

    public ReferencedSchema(String name) {
        ref = PATH + name;
    }
}
