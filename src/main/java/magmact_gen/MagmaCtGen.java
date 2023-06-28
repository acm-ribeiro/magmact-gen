package magmact_gen;

import magmact_domain.*;
import magmact_parser.VisitorOrientedParser;
import oas_custom_parser.domain.*;
import utils.Lemmatizer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MagmaCtGen {

    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String PATCH = "PATCH";
    public static final String GET = "GET";
    public static final String DELETE = "DELETE";
    public static final String RESPONSE_CODE = "response_code";
    public static final String RESPONSE_BODY = "response_body";
    public static final String REQUEST_BODY = "request_body";
    public static final String EQUALS = "==";
    public static final String DIFFERENT = "!=";
    public static final String OK = "200";
    public static final String NOT_FOUND = "404";
    public static final String INCOMPLETE = "?";
    public static final String TRUE = "T";


    /**
     * Generates the magmact contracts for the specification
     */
    public static void generate(Specification spec) {
        // Operations by id
        Map<String, APIOperation> operations = spec.getOperationsById();
        List<String> getters, uri_parts;
        String verb, resource, id, request_body, last_part;
        URI uri;

        for (APIOperation op : operations.values()) {
            verb = op.getVerb();
            uri = op.getUri();
            uri_parts = op.getUri().getParts();
            getters = getters(spec, uri);
            resource = uri.getResourceName();
            id = getResourceId(spec, uri);

            // e.g. POST enrollment: the id will be null since there is no "enrollment" schema
            if (id == null)
                incomplete_contract(op);

            switch (verb.toUpperCase()) {
                case POST -> {
                    if (getters.isEmpty() && id != null) {
                        // response_code(GET /resource_name/{resource_id}) == 404
                        op.addPrecondition(response_code(resource, id, null, NOT_FOUND).toString());
                        // response_code(GET /resource_name/{resource_id}) == 200
                        op.addPostcondition(response_code(resource, id, null, OK).toString());
                    }

                    // Checking whether the contract is complete
                    // Using the singular of the resource name to compare with the schema
                    request_body = op.getBody().getName();
                    last_part = Lemmatizer.root(uri_parts.get(uri_parts.size() - 1));

                    // If the resource name is different from the schema, then there's a chance the contract is incomplete.
                    if(!request_body.equalsIgnoreCase(last_part))
                        incomplete_contract(op);
                }

                case GET -> {
                    // These are considered pure operations, therefore producing no effects on the API.
                    if ((op.isGetOne() && !op.multipleParameters()) || op.isGetAll()) {
                        op.addPrecondition(TRUE);
                        op.addPostcondition(TRUE);
                    }
                }

                case PUT, PATCH -> {
                    if (id != null) {
                        // response_code(GET /resource_name/{resource_id}) == 200
                        op.addPrecondition(response_code(resource, id, null,OK).toString());
                        op.addPostcondition(response_code(resource, id, null, OK).toString());

                        // request_body(this) != previous(response_body(GET /resource/{id}))
                        op.addPostcondition(previous(resource, id).toString());
                    }
                }

                case DELETE -> {
                    if (id != null) {
                        // response_code(GET /resource_name/{resource_id}) == 200
                        op.addPrecondition(response_code(resource, id, null,  OK).toString());

                        // response_code(GET /resource_name/{resource_id}) == 404
                        op.addPostcondition(response_code(resource, id, null, NOT_FOUND).toString());
                    }
                }

                default -> incomplete_contract(op);

            }

            // All the resources this operation depends on remain unchanged
            for(String getter : getters) {
                op.addPrecondition(response_code(null, null, getter, OK).toString());
                op.addPostcondition(response_code(null, null, getter, OK).toString());
            }
        }
    }

    /**
     * Adds the pre- and post-conditions stating the given operation's contract may be incomplete.
     * @param op operation
     */
    private static void incomplete_contract(APIOperation op) {
        List<String> req = op.getRequires();
        List<String> ens = op.getEnsures();

        // To avoid repetitions
        if(!req.contains(INCOMPLETE))
            op.addPrecondition(INCOMPLETE);

        if(!ens.contains(INCOMPLETE))
            op.addPostcondition(INCOMPLETE);
    }

    /**
     * Finds the resource id based on the operation uri and the schemas.
     * @param uri   operation uri
     * @return  resource id
     */
    private static String getResourceId(Specification spec, URI uri) {
        String resource, root, id = null;

        // Making the resource name a singular (e.g., resources -> resource)
        resource = uri.getResourceName();
        root = Lemmatizer.root(resource);

        // Finding the resource id assuming the first parameter of a schema is the id
        Schema s = spec.getSchemas().get(root);
        if (s != null)
            id = s.getProperties().get(0).getName();

        return id;
    }

    /**
     * Builds a MAGMACt formula of the type: response_code(GET /resource_name/{resource_id}) == code
     * When uri is present, resource and id should be null.
     * When resource and id are present, uri should be null.
     * @param resource  name of the resource
     * @param id        name of the property that is the resource id
     * @param uri       the uri is passed as is
     * @param code      HTTP status code
     * @return formula
     */
    public static Formula response_code(@Nullable String resource, @Nullable String id, @Nullable String uri, String code) {
        // Building the HTTP Request
        String url = uri != null? uri : "/" + resource + "/{" + id + "}";
        HTTPRequest request = new HTTPRequest("GET", url);

        // Building the operation
        OperationParameter parameter = new OperationParameter(request, null);
        OperationHeader header = new OperationHeader(RESPONSE_CODE);
        Operation operation = new Operation(header, parameter, null, null, null);

        // Building the comparison
        Call call = new Call(operation, null);
        LeftTerm left = new LeftTerm(call, null, false);
        LeftTerm r_left = new LeftTerm(null, new Param(new StringParam(code), null), false);
        RightTerm right = new RightTerm(r_left, null);

        Comparison comparison = new Comparison(left, right, new Comparator(EQUALS));

        // Building the boolean expression
        Clause clause = new Clause(null, comparison);
        BooleanExpression boolean_expression = new BooleanExpression(clause, null, null, null);

        Formula f = new Formula(null, boolean_expression);

        // To make sure the formula is according to the grammar rules
        VisitorOrientedParser parser = new VisitorOrientedParser();
        parser.parse(f.toString());

        return f;
    }

    /**
     * Builds a MAGMACt formula of the type: request_body(this) != previous(response_body(GET /resource/{id}))
     * @param resource  resource name
     * @param id        resource id
     * @return formula
     */
    public static Formula previous(String resource, String id) {
        // Building the uri
        String url = "/" + resource + "/{" + id + "}";

        // Building the left term
        OperationHeader l_op_header = new OperationHeader(REQUEST_BODY);
        OperationParameter l_parameter = new OperationParameter(null, "this");
        Operation l_operation = new Operation(l_op_header, l_parameter, null, null, null);
        Call l_call = new Call(l_operation, null);
        LeftTerm left = new LeftTerm(l_call, null, false);

        // Building operation previous: right term
        PreviousHeader header_prev = new PreviousHeader("previous");
        HTTPRequest request_prev = new HTTPRequest(GET, url);
        OperationParameter param_prev = new OperationParameter(request_prev, null);
        OperationHeader header_op_prev = new OperationHeader(RESPONSE_BODY);
        Operation operation_prev = new Operation(header_op_prev, param_prev, null, null, null);
        OperationPrevious previous = new OperationPrevious(header_prev, operation_prev);
        Call r_call = new Call(null, previous);

        // Building the comparison
        LeftTerm r_left = new LeftTerm(r_call, null, false);
        RightTerm right = new RightTerm(r_left, null);

        Comparison comparison = new Comparison(left, right, new Comparator(DIFFERENT));

        // Building the boolean expression
        Clause clause = new Clause(null, comparison);
        BooleanExpression expression = new BooleanExpression(clause, null, null, null);

        Formula f = new Formula(null, expression);

        // To make sure the formula is according to the grammar rules
        VisitorOrientedParser parser = new VisitorOrientedParser();
        parser.parse(f.toString());

        return f;
    }

    /**
     * Returns all getters from a URI, when applicable. e.g.,
     * uri = "/r1/{r1_id}/r2/{r2_id}"
     * returns ["/r1/{id1}", "/r1/{r1_id}/r2/{id2}"], if there are two get methods with these URIs.
     * @param uri uri
     * @return getter methods
     */
    public static List<String> getters(Specification spec, URI uri) {
        List<String> getter_uris = new ArrayList<>();

        List<String> params = uri.getParameters();
        Map<String, Map<String, APIOperation>> by_path = spec.getOperationsByPath();

        Map<String, APIOperation> path_ops;
        List<String> param_parts;
        String resource_name, plural, candidate_uri;

        // Find the getter methods for each parameter
        for(String p : params) {
            // Splitting the string by its parts
            param_parts = Lemmatizer.is_snake(p)? Lemmatizer.split_snake(p) : Lemmatizer.split_camel(p);

            // The first part is usually the parameter name, e.g., "playerNIF" -> player; tournamentId -> tournament
            resource_name = param_parts.get(0);

            // According to the REST standard, uris should be in the plural form
            plural = Lemmatizer.pluralize(resource_name);

            // the candidate uri will be the pluralized form and the parameter name
            //e.g., "players/{playerNIF}"
            candidate_uri = "/" + plural + "/{" + p + "}";

            // if there is a get method with this URI, we need to add it.
            if (by_path.containsKey(candidate_uri) && !uri.toString().equalsIgnoreCase(candidate_uri)) {
                path_ops = by_path.get(candidate_uri);
                if (path_ops.containsKey("GET"))
                    getter_uris.add(candidate_uri);
            }
        }

        return getter_uris;
    }

    /**
     * Prints the generated contracts for the given specification.
     * @param spec  oas specification.
     */
    public static void print_contracts(Specification spec) {
        Map<String, Map<String, APIOperation>> by_path = spec.getOperationsByPath();
        Map<String, APIOperation> path_ops;
        List<String> requires, ensures;
        APIOperation op;
        String path, verb;

        for (Entry<String, Map<String, APIOperation>> e1: by_path.entrySet()) {
            path = e1.getKey();
            path_ops = e1.getValue();

            // prints the path/uri
            System.out.println("\n" + path + ":");

            for (Entry<String, APIOperation> e2: path_ops.entrySet()) {
                verb = e2.getKey();
                op = e2.getValue();
                requires = op.getRequires();
                ensures = op.getEnsures();

                System.out.println("  " + verb + ":");
                System.out.println("    requires: ");
                for (String r : requires)
                    System.out.println("      - " + r);

                System.out.println("    ensures: ");
                for (String e : ensures)
                    System.out.println("      - " + e);
            }
            System.out.println();
        }
    }

}
