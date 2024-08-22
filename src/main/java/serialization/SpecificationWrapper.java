package serialization;

import oas_custom_parser.domain.APIOperation;
import oas_custom_parser.domain.Schema;
import oas_custom_parser.domain.Specification;

import java.util.*;
import java.util.Map.Entry;

public class SpecificationWrapper {

    private final List<String> servers; // server urls
    private final List<String> tags;
    private List<String> invariants;
    private final Map<String, Map<String, OperationWrapper>> paths;
    private final List<SchemaWrapper> schemas;

    public SpecificationWrapper(Specification spec) {
        servers = spec.getServers();
        tags = spec.getTags();
        invariants = spec.getInvariants();
        schemas = new ArrayList<>();

        for (Schema s : spec.getSchemas().values().stream().toList())
            schemas.add(new SchemaWrapper(s.getType(), s.getName(), s.getProperties()));

        paths = new HashMap<>();

        for (String pathName : spec.getPaths()) {
            Map<String, APIOperation> operationsByID = spec.getOperationsById();
            Map<String, OperationWrapper> operations = new HashMap<>();

            for (Entry<String, APIOperation> e : operationsByID.entrySet()) {
                APIOperation op = e.getValue();
                if (op.getUri().toString().equalsIgnoreCase(pathName)) {
                    operations.put(op.getVerb(),
                            new OperationWrapper(op.getRequires(), op.getEnsures(), op.getOperationId(),
                                    op.getBody(), op.getPathParams(), op.getQueryParams(),
                                    op.getResponses(), op.getTags())
                    );
                    paths.put(pathName, operations);
                }
            }
        }
    }


}
