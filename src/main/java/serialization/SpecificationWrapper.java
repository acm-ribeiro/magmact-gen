package serialization;

import magmact_parser.domain.APIOperation;
import magmact_parser.domain.Schema;
import magmact_parser.domain.Specification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SpecificationWrapper {

    private List<String> servers; // server urls
    private List<String> invariants;
    private Map<String, Map<String, OperationWrapper>> paths;
    private List<SchemaWrapper> schemas;

    public SpecificationWrapper(Specification spec){
        servers = spec.getServers();
        invariants = spec.getInvariants();
        schemas = new ArrayList<>();

        for(Schema s : spec.getSchemas().values().stream().toList())
            schemas.add(new SchemaWrapper(s.getType(), s.getName(), s.getProperties()));

        paths = new HashMap<>();

        for(String pathName: spec.getPaths()) {
            Map<String, APIOperation> operationsByID = spec.getOperations();
            Map<String, OperationWrapper> operations = new HashMap<>();

            for(Entry<String, APIOperation> e: operationsByID.entrySet()) {
                APIOperation op = e.getValue();

                if(op.getUri().toString().equalsIgnoreCase(pathName)) {
                    operations.put(op.getVerb(),
                            new OperationWrapper(op.getRequires(), op.getEnsures(), op.getOperationId(),
                                    op.getBody(), op.getPathParams(), op.getQueryParams(), op.getResponses())
                    );

                    paths.put(pathName, operations);
                }
            }
        }
    }
}
