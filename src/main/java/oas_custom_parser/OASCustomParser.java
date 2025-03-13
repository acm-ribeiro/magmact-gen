package oas_custom_parser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openapi4j.core.exception.EncodeException;
import org.openapi4j.core.exception.ResolutionException;
import org.openapi4j.core.validation.ValidationException;
import org.openapi4j.parser.OpenApi3Parser;
import org.openapi4j.parser.model.v3.*;

import com.fasterxml.jackson.databind.JsonNode;

import org.openapi4j.parser.model.v3.Link;
import oas_custom_parser.domain.*;
import oas_custom_parser.domain.Schema;
import oas_custom_parser.exceptions.NotJSONContentType;

public class OASCustomParser {

    // custom properties
    public static final String GEN = "x-gen";
    public static final String REFERS_TO = "x-refersTo";

    // types
    public static final String TYPE = "type";
    public static final String PATTERN = "pattern";
    public static final String ARRAY = "array";
    public static final String OBJECT = "object";
    public static final String STRING = "string";
    public static final String INTEGER = "integer";
    public static final String NUMBER = "number";
    public static final String BOOLEAN = "boolean";

    // oas keywords
    public static final String SCHEMA = "schema";
    public static final String ITEMS = "items";
    public static final String REQUIRED = "required";

    // oas paths
    public static final String COMPONENTS_PATH = "/components";
    public static final String SCHEMA_PATH = "#/components/schemas/";
    public static final String PARAMS_PATH = "#/components/parameters/";
    public static final String CUSTOM_SCHEMA_PATH = "#/schemas/";

    // content types
    public static final String JSON = "application/json";
    public static final String ENCODED_URL = "application/x-www-form-urlencoded";
    public static final String ALL_CONTENT_TYPES = "*/*";
    public static final String REF = "$ref";
    public static final String CONTENT = "content";

    // placeholders for min and max values, when not specified
    public static final int NO_MIN = -999;
    public static final int NO_MAX = 999;
    public static final String NO_VAL = "";

    // error messages
    public static final String NO_PROPERTIES = "Schema [%s] has no properties defined.\n";
    public static final String CONTENT_TYPES_ERR = "This specification has content types that not JSON.";

    public static Specification parse(File specFile) throws ValidationException, ResolutionException, EncodeException {
        // Parses and validates the OpenAPI file
        OpenApi3 api = new OpenApi3Parser().parse(specFile, true);

        Map<String, Schema> schemas = parseSchemas(api);
        Map<String, APIOperation> operations = parseOperations(api, schemas);
        List<String> servers = parseServers(api);
        List<String> tags = parseTags(api);

        return new Specification(servers, tags, operations, schemas,
                new ArrayList<>(api.getPaths().keySet()));
    }

    private static List<String> parseServers(OpenApi3 api) {
        List<Server> apiServers = api.getServers();
        List<String> servers = new ArrayList<>(apiServers.size());

        for (Server s : apiServers)
            servers.add(s.getUrl());

        return servers;
    }

    private static List<String> parseTags(OpenApi3 api) {
        List<Tag> apiTags = api.getTags();
        List<String> tags;
        if (apiTags != null) {
            tags = new ArrayList<>(apiTags.size());
            for (Tag t : apiTags)
                tags.add(t.getName());
        } else
            tags = new ArrayList<>();

        return tags;
    }

    private static Map<String, Schema> parseSchemas(OpenApi3 api) throws EncodeException {
        Map<String, Schema> schemas = new HashMap<>();
        Components components = api.getComponents();

        if (components != null) {
            Map<String, org.openapi4j.parser.model.v3.Schema> componentSchemas = components.getSchemas();

            for (Entry<String, org.openapi4j.parser.model.v3.Schema> componentEntry : componentSchemas.entrySet()) {
                String schemaName = componentEntry.getKey().toLowerCase();
                org.openapi4j.parser.model.v3.Schema s = componentEntry.getValue();
                schemas.put(schemaName, parseSchema(schemaName, s));
            }
        }

        return schemas;
    }

    private static Map<String, APIOperation> parseOperations(OpenApi3 api, Map<String, Schema> schemas) throws EncodeException {
        String url, httpMethod, operationId;
        List<URLParameter> parameters;
        Schema body;
        List<APIResponse> responses;
        List<String> contentTypes; // requestBody content types

        Map<String, APIOperation> operations = new HashMap<>();
        Map<String, Path> paths = api.getPaths();

        for (Entry<String, Path> pathEntry : paths.entrySet()) {
            Map<String, Operation> ops = pathEntry.getValue().getOperations();

            for (Entry<String, Operation> opEntry : ops.entrySet()) {
                Operation op = opEntry.getValue();

                if (op.getRequestBody() != null && op.getRequestBody().getContentMediaTypes() != null)
                    contentTypes = new ArrayList<>(op.getRequestBody().getContentMediaTypes().keySet());
                else
                    contentTypes = new ArrayList<>();

                httpMethod = opEntry.getKey();
                url = pathEntry.getKey();
                operationId = op.getOperationId();
                parameters = parseURLParameters(op, api);

                try {
                    body = parseRequestBodySchema(op, schemas);
                    responses = parseOperationResponses(op, schemas);
                    operations.put(operationId, new APIOperation(url, httpMethod, operationId, body, parameters, responses, contentTypes, op.getTags()));
                } catch (NotJSONContentType e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        return operations;
    }

    /*
     * TODO what if it's not a referenced type?
     * TODO what if the request has an array as request body?
     */
    private static Schema parseRequestBodySchema(Operation operation, Map<String, Schema> schemas) throws EncodeException, NotJSONContentType {
        Schema body = null;
        String type;
        boolean isReferenced, immediateReferenced, hasJSONContentType, hasEncodedURLContentType;

        if (operation.getRequestBody() != null) {
            immediateReferenced = operation.getRequestBody().toNode().get(REF) != null;
            hasJSONContentType = operation.getRequestBody().toNode().get(CONTENT).get(JSON) != null;
            hasEncodedURLContentType = operation.getRequestBody().toNode().get(CONTENT).get(ENCODED_URL) != null;

            if (immediateReferenced)
                isReferenced = true;
            else if (hasJSONContentType)
                isReferenced = operation.getRequestBody().toNode().get(CONTENT).get(JSON).get(SCHEMA).get(REF) != null;
            else if (hasEncodedURLContentType)
                isReferenced = operation.getRequestBody().toNode().get(CONTENT).get(ENCODED_URL).get(SCHEMA).get(REF) != null;
            else
                throw new NotJSONContentType(operation.getOperationId());

            if (isReferenced && immediateReferenced)
                type = operation.getRequestBody().toNode().get(REF).asText();
            else if (isReferenced && hasJSONContentType)
                type = operation.getRequestBody().toNode().get(CONTENT).get(JSON).get(SCHEMA).get(REF).asText();
            else if (isReferenced)
                type = operation.getRequestBody().toNode().get(CONTENT).get(ENCODED_URL).get(
                        SCHEMA).get(REF).asText();
            else
                type = operation.getRequestBody().toNode().get(CONTENT).get(JSON).get(SCHEMA).get(TYPE).asText();

            body = getReferencedSchema(type, schemas);
        }

        return body;
    }

    private static List<URLParameter> parseURLParameters(Operation operation, OpenApi3 api) throws EncodeException {
        Map<String, Parameter> parameters = api.getComponents().getParameters();
        List<URLParameter> urlParameters = new ArrayList<>();

        String name = null, in = null, type, parameterSchema;
        boolean required, gen;
        org.openapi4j.parser.model.v3.Schema s;

        if (operation.hasParameters()) {
            List<Parameter> urlParams = operation.getParameters();
            for (Parameter p : urlParams) {
                List<APIProperty> URLSchema = new ArrayList<>();

                // Dealing with referenced schemas
                if (p.isRef()) {
                    p = parameters.get(p.getRef().replace(PARAMS_PATH, NO_VAL));
                    parameterSchema = p.getSchema().getRef().replace(SCHEMA_PATH, NO_VAL);
                    in = p.getIn();
                    s = api.getComponents().getSchema(parameterSchema);
                    required = p.isRequired();
                } else {
                    s = p.getSchema();
                    required = s.toNode().get(REQUIRED) != null && s.toNode().get(REQUIRED).asBoolean();
                }

                JsonNode schemaNode = s.toNode();

                // Dealing with inline schemas
                if (!s.isRef()) {
                    String itemsType = null, format = NO_VAL;
                    boolean isCollection = false;
                    int minimum = -999, maximum = 999;

                    name = p.getName();
                    type = s.getType();
                    gen = schemaNode.get(GEN) != null && schemaNode.get(GEN).asBoolean();
                    in = p.getIn();

                    if (type != null) {
                        minimum = type.equals(INTEGER) && s.getMinimum() != null ? (int) s.getMinimum() : NO_MIN;
                        maximum = type.equals(INTEGER) && s.getMaximum() != null ? (int) s.getMaximum() : NO_MAX;
                        format = type.equals(INTEGER) ? s.getFormat() : NO_VAL;
                        isCollection = type.equalsIgnoreCase(ARRAY);
                    }

                    if (isCollection) {
                        boolean referencedItems = schemaNode.get(ITEMS).get(TYPE) == null;
                        itemsType = referencedItems ?
                                getItemTypes(s.getItemsSchema()) :
                                s.toNode().get(ITEMS).asText();
                    }

                    URLSchema.add(new APIProperty(name, type, NO_VAL, format, itemsType, NO_VAL,
                            null, null, minimum, maximum, isCollection, required, gen,
                            ""));
                }

                URLParameter urlParam = new URLParameter(in, name, p.isRequired(), URLSchema);
                urlParameters.add(urlParam);
            }
        }

        return urlParameters;
    }

    private static List<APIResponse> parseOperationResponses(Operation op, Map<String, Schema> schemas) throws EncodeException {
        List<APIResponse> responses = new ArrayList<>();
        Map<String, Response> specResponses = op.getResponses();
        Response specResponse;
        List<APILink> links = null;
        List<String> contentTypes;

        for (Entry<String, Response> responseEntry : specResponses.entrySet()) {
            String responseCode, description, schemaType = NO_VAL, ref = NO_VAL, contentSchemaFormat = NO_VAL;
            ResponseSchema contentSchema = null;

            responseCode = responseEntry.getKey();
            specResponse = responseEntry.getValue();
            description = specResponse.getDescription();
            contentTypes = specResponse.getContentMediaTypes() != null ?
                    new ArrayList<>(specResponse.getContentMediaTypes().keySet())
                    : new ArrayList<>();

            if (!contentTypes.isEmpty()) {
                org.openapi4j.parser.model.v3.Schema responseSchema = null;

                if (specResponse.getContentMediaType(JSON) != null)
                    responseSchema = specResponse.getContentMediaType(JSON).getSchema();
                else if (specResponse.getContentMediaType(ALL_CONTENT_TYPES) != null)
                    responseSchema = specResponse.getContentMediaType(ALL_CONTENT_TYPES).getSchema();
                else {
                    System.err.println(CONTENT_TYPES_ERR);
                    System.exit(1);
                }

                // If both type and $ref are null, then we consider that the schema is not defined (or empty)
                if (responseSchema.getRef() != null || responseSchema.getType() != null) {
                    if (!responseSchema.isRef()) { // array or integer
                        schemaType = responseSchema.getType();

                        if (schemaType.equals(ARRAY)) {
                            ref = responseSchema.toNode().get(ITEMS).get(REF) != null ?
                                    responseSchema.toNode().get(ITEMS).get(REF).asText() :
                                    responseSchema.toNode().get(ITEMS).get(TYPE).asText();
                        } else {
                            contentSchemaFormat = responseSchema.getFormat();
                        }
                    } else {
                        if (specResponse.getContentMediaType(JSON) != null)
                            ref = specResponse.getContentMediaType(JSON).getSchema().getRef();
                        else if (specResponse.getContentMediaType(ENCODED_URL) != null)
                            ref = specResponse.getContentMediaType(ENCODED_URL).getSchema().getRef();
                        else
                            ref = specResponse.getContentMediaType(ALL_CONTENT_TYPES).getSchema().getRef();
                    }

                    // ref can be an object reference or the reference for the array items (mutually exclusive)
                    if (ref.equals(NO_VAL))
                        contentSchema = new ResponseSchema(schemaType, contentSchemaFormat, null, NO_VAL);
                    else
                        contentSchema = new ResponseSchema(schemaType, contentSchemaFormat, getReferencedSchema(ref, schemas), ref);
                }
            }

            if (specResponse.getLinks() != null)
                links = parseLinks(specResponse);

            responses.add(new APIResponse(responseCode, description, contentSchema, schemaType, links, contentTypes));
        }
        return responses;
    }

    private static List<APILink> parseLinks(Response res) {
        List<APILink> links = new ArrayList<>();
        String name, operationId;
        Map<String, String> parameters;

        Map<String, Link> specLinks = res.getLinks();
        Link specLink;

        for (Entry<String, Link> entryLink : specLinks.entrySet()) {
            specLink = entryLink.getValue();
            name = entryLink.getKey();
            operationId = specLink.getOperationId();
            parameters = specLink.getParameters();

            links.add(new APILink(name, operationId, parameters));
        }

        return links;
    }

    private static Schema parseSchema(String schemaName, org.openapi4j.parser.model.v3.Schema s) throws EncodeException {
        Schema schema = null;
        String schemaType = s.getType();

        switch (schemaType) {
            case INTEGER, NUMBER -> schema = parseNumberSchema(schemaName, s);
            case STRING  -> schema = parseStringSchema(schemaName, s);
            case BOOLEAN -> schema = parseBooleanSchema(schemaName, s);
            case ARRAY   -> schema = parseArraySchema(schemaName, s);
            case OBJECT  -> schema = parseObjectSchema(schemaName, s);
        }

        return schema;
    }

    // Auxiliary Methods

    private static Schema parseObjectSchema(String schemaName, org.openapi4j.parser.model.v3.Schema s) throws EncodeException {
        Map<String, org.openapi4j.parser.model.v3.Schema> props = s.getProperties();
        List<APIProperty> properties = new ArrayList<>();

        if (props == null)
            System.err.printf(NO_PROPERTIES, schemaName);
        else {
            List<String> requiredFields = s.hasRequiredFields() ? s.getRequiredFields() : null;
            String name, type, refName, refersTo = NO_VAL;
            String pattern = NO_VAL, itemsType = NO_VAL, itemsFormat = NO_VAL;
            String ref = NO_VAL, itemsPattern = NO_VAL, format = NO_VAL;
            int minimum = NO_MIN, maximum = NO_MAX;
            boolean required, gen = false, isCollection = false;

            for (Entry<String, org.openapi4j.parser.model.v3.Schema> propertiesEntry : props.entrySet()) {
                name = propertiesEntry.getKey();
                type = propertiesEntry.getValue().getType();

                if (type != null) {
                    required = requiredFields != null && requiredFields.contains(name);
                    minimum = type.equals(INTEGER) && propertiesEntry.getValue().getMinimum() != null ?
                            (int) propertiesEntry.getValue().getMinimum()
                            : NO_MIN;
                    maximum = type.equals(INTEGER) && propertiesEntry.getValue().getMaximum() != null ?
                            (int) propertiesEntry.getValue().getMaximum()
                            : NO_MAX;
                    format = type.equals(INTEGER) ? propertiesEntry.getValue().getFormat()
                            : NO_VAL;

                    isCollection = type.equalsIgnoreCase(ARRAY);

                    JsonNode schemaNode = propertiesEntry.getValue().toNode();

                    itemsType = isCollection ? getItemTypes(propertiesEntry.getValue()) : NO_VAL;
                    itemsFormat = isCollection ? getItemsFormat(propertiesEntry.getValue()) : NO_VAL;
                    itemsPattern = isCollection ? getItemsPattern(propertiesEntry.getValue()) : NO_VAL;

                    pattern = schemaNode.get(PATTERN) != null ?
                            schemaNode.get(PATTERN).toString().replace("\"", NO_VAL)
                            : null;
                    gen = schemaNode.get(GEN) != null && schemaNode.get(GEN).asBoolean();
                    refersTo = schemaNode.get(REFERS_TO) != null?
                            schemaNode.get(REFERS_TO).asText() : NO_VAL;
                } else {
                    // The property has a referenced schema
                    ref = propertiesEntry.getValue().getRef().replace(COMPONENTS_PATH, NO_VAL);
                    refName = ref.replace(CUSTOM_SCHEMA_PATH, NO_VAL);
                    required = requiredFields != null && requiredFields.contains(refName);
                }

                properties.add(new APIProperty(name, type, pattern, format, itemsType, itemsFormat,
                        itemsPattern, ref, minimum, maximum, isCollection, required, gen,
                        refersTo));
            }
        }

        return new Schema(s.getType(), schemaName, properties);
    }

    private static Schema parseNumberSchema(String name, org.openapi4j.parser.model.v3.Schema s) throws EncodeException {
        List<APIProperty> properties = new ArrayList<>();

        int minimum = s.getMinimum() != null ? (int) s.getMinimum() : NO_MIN;
        int maximum = s.getMaximum() != null ? (int) s.getMaximum() : NO_MAX;
        String format = s.getFormat() != null ? s.getFormat() : NO_VAL;
        JsonNode node = s.toNode();
        boolean gen = node.get(GEN) != null && node.get(GEN).asBoolean();
        String refersTo = node.get(REFERS_TO) != null ? node.get(REFERS_TO).asText() : NO_VAL;

        APIProperty property = new APIProperty(name, s.getType(), NO_VAL, format, NO_VAL, NO_VAL,
                NO_VAL, NO_VAL, minimum, maximum, false, true, gen, refersTo);
        properties.add(property);

        return new Schema(s.getType(), name, properties);
    }

    private static Schema parseStringSchema(String name, org.openapi4j.parser.model.v3.Schema s) throws EncodeException {
        List<APIProperty> properties = new ArrayList<>();
        String pattern = s.getPattern() != null ? s.getPattern() : NO_VAL;
        JsonNode node = s.toNode();
        String refersTo = node.get(REFERS_TO) != null ? node.get(REFERS_TO).asText() : NO_VAL;

        APIProperty property = new APIProperty(name, s.getType(), pattern, NO_VAL,
                NO_VAL, NO_VAL, null, NO_VAL, NO_MIN, NO_MAX,
                false, true, false, refersTo);
        properties.add(property);

        return new Schema(s.getType(), name, properties);
    }

    private static Schema parseBooleanSchema(String name, org.openapi4j.parser.model.v3.Schema s) throws EncodeException {
        List<APIProperty> properties = new ArrayList<>();
        JsonNode node = s.toNode();
        String refersTo = node.get(REFERS_TO) != null ? node.get(REFERS_TO).asText() : NO_VAL;

        APIProperty property = new APIProperty(name, s.getType(), NO_VAL, NO_VAL, NO_VAL, NO_VAL,
                NO_VAL, NO_VAL, NO_MIN, NO_MAX, false, true, false, refersTo);
        properties.add(property);

        return new Schema(s.getType(), name, properties);
    }

    private static Schema parseArraySchema(String name, org.openapi4j.parser.model.v3.Schema s) throws EncodeException {
        List<APIProperty> properties = new ArrayList<>();
        String itemsType = s.getItemsSchema().getRef();
        JsonNode node = s.toNode();
        String refersTo = node.get(REFERS_TO) != null ? node.get(REFERS_TO).asText() : NO_VAL;
        APIProperty property = new APIProperty(name, s.getType(), NO_VAL, NO_VAL, itemsType,
                NO_VAL, NO_VAL, NO_VAL, NO_MIN, NO_MAX, true, true, false, refersTo);
        properties.add(property);

        return new Schema(s.getType(), name, properties);
    }

    private static String getItemTypes(org.openapi4j.parser.model.v3.Schema schema) throws EncodeException {
        boolean isReferenced = schema.toNode().get(ITEMS).get(REF) != null;

        if (isReferenced)
            return schema.toNode().get(ITEMS).get(REF).toString()
                    .replace(COMPONENTS_PATH, NO_VAL)
                    .replace("\"", NO_VAL);
        else
            return schema.getItemsSchema().getType();
    }

    private static String getItemsFormat(org.openapi4j.parser.model.v3.Schema schema) throws EncodeException {
        return getItemTypes(schema).equals(INTEGER) ? schema.getItemsSchema().getFormat() : NO_VAL;
    }

    private static String getItemsPattern(org.openapi4j.parser.model.v3.Schema schema) throws EncodeException {
        return getItemTypes(schema).equals(STRING) ? schema.getItemsSchema().getPattern() : NO_VAL;
    }

    private static Schema getReferencedSchema(String refType, Map<String, Schema> schemas) {
        Schema schema = null;

        for (Entry<String, Schema> s : schemas.entrySet()) {
            String[] parts = refType.split("/");
            String type = parts[parts.length - 1];

            if (s.getKey().equalsIgnoreCase(type))
                schema = s.getValue();
        }

        return schema;
    }
}
