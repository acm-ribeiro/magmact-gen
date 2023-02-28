package magmact_gen;

import org.openapi4j.core.exception.EncodeException;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import magmact_parser.domain.APIOperation;
import magmact_parser.domain.Schema;
import magmact_parser.domain.Specification;
import utils.Lemmatizer;

import java.util.List;
import java.util.Map;

public class APOSTLGen {

    public static final String RESPONSE_CODE = "response_code(GET %s)";
    public static final String EQUALS = " == ";
    public static final String OK = "200";
    public static final String NOT_FOUND = "404";
    public static final String RES_EQUALS_REQ = "response_body(this) == request_body(this)";
    public static final String FAILED_INFERENCE = "?";
    public static final String TRUE = "T";


    public static void generate(Specification spec) throws ResolutionException, ValidationException, EncodeException {
        Map<String, APIOperation> operations = spec.getOperations();
        List<String> ones;
        Schema s;
        String uri, resource, root, oneUrl, resourceId;

        for(APIOperation op: operations.values()) {
            ones = spec.findGetOnes(op);
            uri = op.getUri().toString();

            switch(op.getVerb().toUpperCase()){
                case "GET" -> {
                    if ((op.isGetOne() && !op.multipleParameters()) || op.isGetAll())
                        op.addPrecondition(TRUE);
                    else
                        for(String one: ones)
                            if(!one.equalsIgnoreCase(uri))
                                op.addPrecondition(String.format(RESPONSE_CODE, one) + EQUALS + OK);
                    op.addPostcondition(TRUE);
                }
                case "POST" -> {
                    resource = op.getUri().getResourceName();
                    root = Lemmatizer.root(resource);

                    // Finding the id of the resource
                    s = spec.getSchemas().get(root);

                    if (s != null) {
                        // Assuming the first parameter of a schema is its id
                        resourceId = s.getProperties().get(0).getName();
                        oneUrl = uri + "/{" + resourceId + "}";

                        // For POST /resources
                        if (ones.isEmpty()) {
                            op.addPrecondition(String.format(RESPONSE_CODE, oneUrl) + EQUALS + NOT_FOUND);
                            op.addPostcondition(String.format(RESPONSE_CODE, oneUrl) + EQUALS + OK);
                        }
                    } else {
                        op.addPrecondition(FAILED_INFERENCE);
                        op.addPostcondition(FAILED_INFERENCE);
                        System.out.printf("Unable to infer a complete APOSTL specification for [%s] because schema " +
                                "[%s] is not defined.\n", op.getOperationId(), root);
                    }

                    for(String one: ones) {
                        op.addPrecondition(String.format(RESPONSE_CODE, one) + EQUALS + OK);
                        op.addPostcondition(String.format(RESPONSE_CODE, one) + EQUALS + OK);
                    }
                }
                case "PUT" -> {
                    for(String one: ones)
                            op.addPrecondition(String.format(RESPONSE_CODE, one) + EQUALS + OK);

                    op.addPostcondition(String.format(RESPONSE_CODE, uri) + EQUALS + OK);
                    op.addPostcondition(RES_EQUALS_REQ);
                }
                case "PATCH" -> {
                    for(String one: ones) {
                        op.addPrecondition(String.format(RESPONSE_CODE, one) + EQUALS + OK);
                        op.addPostcondition(String.format(RESPONSE_CODE, one) + EQUALS + OK);
                    }
                }
                case "DELETE" -> {
                    for(String one: ones) {
                        op.addPrecondition(String.format(RESPONSE_CODE, one) + EQUALS + OK);

                        if(!one.equalsIgnoreCase(uri))
                            op.addPostcondition(String.format(RESPONSE_CODE, one) + EQUALS + OK);
                        else
                            op.addPostcondition(String.format(RESPONSE_CODE, uri) + EQUALS + NOT_FOUND);
                    }
                    op.addPostcondition(RES_EQUALS_REQ);
                }
                default -> { }
            }
        }
    }

}
