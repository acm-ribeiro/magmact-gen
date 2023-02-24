package specification_parser.domain;

public class ResponseSchema {

    public static final String ARRAY = "array";

    private String schemaType, format, ref, items;
    private transient String reference;
    private transient Schema referencedSchema;

    public ResponseSchema(String schemaType, String format, Schema referencedSchema, String ref) {
        this.schemaType = schemaType.isEmpty()? null : schemaType;
        this.format = format;
        this.referencedSchema = referencedSchema;
        this.reference = ref;
        this.ref = ref == null? ref : ref.replace("/components", "").toLowerCase();

        if(this.schemaType != null && this.schemaType.equalsIgnoreCase(ARRAY)) {
            this.items = ref;
            this.ref = null;
        }
    }

    public String getRef() {
        return ref;
    }

    public int getNumProperties() {
        return referencedSchema != null? referencedSchema.getProperties().size() : 0;
    }

    public Schema getReferencedSchema() {
        return referencedSchema;
    }

    public String getSchemaType () {
        return schemaType;
    }

}
