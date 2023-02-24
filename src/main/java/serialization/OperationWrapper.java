package serialization;

import specification_parser.domain.APIResponse;
import specification_parser.domain.Schema;
import specification_parser.domain.URLParameter;

import java.util.ArrayList;
import java.util.List;

public class OperationWrapper {

    private String operationID;
    private List<String> requires, ensures;
    private List<URLParameterWrapper> pathParams, queryParams;
    private RequestBodySchema requestBody;
    private List<APIResponse> responses;

    public OperationWrapper(List<String> requires, List<String> ensures, String operationID, Schema requestBody, List<URLParameter> path, List<URLParameter> query, List<APIResponse> responses) {
        this.ensures = ensures;
        this.requires = requires;
        this.operationID = operationID;
        pathParams = new ArrayList<>();
        queryParams = new ArrayList<>();
        this.responses = responses;

        initRequestBody(requestBody);

        for (URLParameter p : path)
            pathParams.add(new URLParameterWrapper(p.getIn(), p.getName(), p.isRequired(), p.getSchema()));

        for (URLParameter p : query)
            queryParams.add(new URLParameterWrapper(p.getIn(), p.getName(), p.isRequired(), p.getSchema()));
    }

    private void initRequestBody(Schema requestBody) {
        if(requestBody != null)
            this.requestBody = requestBody.isRef() ?
                    new SchemaWrapper(requestBody.getType(), requestBody.getName(), requestBody.getProperties()) :
                    new ReferencedSchema(requestBody.getName());
        else
            this.requestBody = null;
    }
}
