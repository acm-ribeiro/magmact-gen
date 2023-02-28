package magmact_parser.exceptions;

import java.io.Serial;

public class NotJSONContentType extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    public NotJSONContentType(String operationId) {
        super("Unable to infer a complete APOSTL specification for [" + operationId + "] because it requires data that's" +
                "not [json] or [x-www-form-urlencoded].");
    }
}
