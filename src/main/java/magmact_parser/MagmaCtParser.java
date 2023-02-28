package magmact_parser;

import java.io.File;
import java.sql.SQLOutput;
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
import magmact_parser.domain.*;
import magmact_parser.domain.Schema;
import magmact_parser.exceptions.NotJSONContentType;

import javax.sound.midi.Soundbank;

public class MagmaCtParser {

	public static final String REGEX = "x-regex";
	public static final String PATTERN = "pattern";
	public static final String GEN = "x-gen";
	public static final String ARRAY = "array";
	public static final String OBJECT = "object";
	public static final String STRING = "string";
	public static final String NUMBER = "number";
	public static final String BOOLEAN = "boolean";
	public static final String JSON = "application/json";
	public static final String ENCODED_URL = "application/x-www-form-urlencoded";
	public static final String ALL_CONTENT_TYPES = "*/*";
	public static final String REF = "$ref";
	public static final int NO_MIN = -999;
	public static final int NO_MAX = 999;

	public static Specification parse(File specFile) throws ValidationException, ResolutionException, EncodeException {
		// Parses and validates the OpenAPI file
		OpenApi3 api = new OpenApi3Parser().parse(specFile, true);

		Map<String, Endpoint> endpoints = parseEndpoints(api);
		Map<String, Schema> schemas = parseSchemas(api);
		Map<String, APIOperation> operations = parseOperations(api, schemas);
		List<String> servers = parseServers(api);

		return new Specification(servers, operations, schemas, new ArrayList<>(api.getPaths().keySet()), endpoints);
	}

	/**
	 * Parses the APIs endpoints and returns them organised by operation id.
	 * @param api API to be parsed
	 * @return endpoints by operation id
	 */
	private static Map<String, Endpoint> parseEndpoints(OpenApi3 api) {
		Map<String, Endpoint> endpoints = new HashMap<>();
		Map<String, Path> paths = api.getPaths();

		for (Entry<String, Path> path_entry: paths.entrySet()) {
			Map<String, Operation> operations = path_entry.getValue().getOperations();

			for (Entry<String, Operation> op_entry: operations.entrySet()) {
				String opId = op_entry.getValue().getOperationId();

				String uri = path_entry.getKey();
				List<Parameter> params = path_entry.getValue().getParameters();

				endpoints.put(opId, new Endpoint(uri, params));
			}
		}

		return endpoints;
	}

	private static List<String> parseServers(OpenApi3 api) {
		List<Server> apiServers = api.getServers();
		List<String> servers = new ArrayList<>();

		for(Server s: apiServers)
			servers.add(s.getUrl());

		return servers;
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

				if(op.getRequestBody() != null && op.getRequestBody().getContentMediaTypes() != null)
					contentTypes = new ArrayList<>(op.getRequestBody().getContentMediaTypes().keySet());
				else
					contentTypes = new ArrayList<>();

				httpMethod = opEntry.getKey();
				url = pathEntry.getKey();
				operationId = op.getOperationId();
				parameters = parseURLParameters(op, schemas);

				try {
					body = parseRequestBodySchema(op, schemas);
					responses = parseOperationResponses(op, schemas);

					operations.put(operationId, new APIOperation(url, httpMethod, operationId, body, parameters,
							responses, contentTypes, op.getTags()));
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
			immediateReferenced = operation.getRequestBody().toNode().get("$ref") != null;
			hasJSONContentType = operation.getRequestBody().toNode().get("content").get(JSON) != null;
			hasEncodedURLContentType = operation.getRequestBody().toNode().get("content").get(ENCODED_URL) != null;

			if(immediateReferenced)
				isReferenced = true;
			else
				if (hasJSONContentType)
					isReferenced = operation.getRequestBody().toNode().get("content").get(JSON).get("schema").get(REF) != null;
				else if (hasEncodedURLContentType)
					isReferenced = operation.getRequestBody().toNode().get("content").get(ENCODED_URL).get("schema").get(REF) != null;
				else
					throw new NotJSONContentType(operation.getOperationId());

			System.out.println(operation.getOperationId());
			if (isReferenced && immediateReferenced)
				type = operation.getRequestBody().toNode().get("$ref").asText();
			else if (isReferenced && hasJSONContentType)
				type = operation.getRequestBody().toNode().get("content").get(JSON).get("schema").get(REF).asText();
			else if (isReferenced && hasEncodedURLContentType)
				type = operation.getRequestBody().toNode().get("content").get(ENCODED_URL).get("schema").get(REF).asText();
			else
				type = operation.getRequestBody().toNode().get("content").get(JSON).get("schema").get("type").asText();

			body = getReferencedSchema(type, schemas);
		}

		return body;
	}
	
	private static List<URLParameter> parseURLParameters(Operation operation, Map<String, Schema> schemas) throws EncodeException {
		List<URLParameter> parameters = new ArrayList<>();

		if (operation.hasParameters()) {
			List<Parameter> params = operation.getParameters();

			for (Parameter p : params) {
				org.openapi4j.parser.model.v3.Schema s = p.getSchema();
				List<APIProperty> URLSchema = new ArrayList<>();

				// TODO we do not deal with referenced URL parameter schemas
				if (s != null && s.getRef() == null) {
					String name, type, regex, itemsType = null, format = "";
					boolean required, gen, isCollection = false;
					int minimum = -999, maximum = 999;

					name = p.getName();
					type = s.getType();

					if(type != null) {
						minimum = type.equals("integer") && s.getMinimum() != null ? (int) s.getMinimum() : NO_MIN;
						maximum = type.equals("integer") && s.getMaximum() != null ? (int) s.getMaximum() : NO_MAX;
						format = type.equals("integer") ? s.getFormat() : "";
						isCollection = type.equalsIgnoreCase(ARRAY);
					}

					// TODO what if it's not a referenced type?
					if(isCollection) {
						boolean referencedItems = s.toNode().get("items").get("type") == null;
						 if(referencedItems)
							 itemsType = getItemTypes(s.getItemsSchema());
						 else
							 itemsType = s.toNode().get("items").asText();
					}

					JsonNode schemaNode = s.toNode();
					regex = schemaNode.get(REGEX) != null ? schemaNode.get(REGEX).toString().replace("\"", "") : null;
					gen = schemaNode.get(GEN) != null && schemaNode.get(GEN).asBoolean();
					required = schemaNode.get("required") != null && schemaNode.get("required").asBoolean();

					URLSchema.add(new APIProperty(name, type, regex, format, itemsType, null, minimum, maximum, isCollection, required, gen));
				}

				URLParameter urlParam = new URLParameter(p.getDescription(), p.getIn(), p.getName(), p.isRequired(), URLSchema);
				parameters.add(urlParam);
			}
		}
		return parameters;
	}

	private static List<APIResponse> parseOperationResponses(Operation op, Map<String, Schema> schemas) throws EncodeException {
		List<APIResponse> responses = new ArrayList<>();
		Map<String, Response> specResponses = op.getResponses();
		Response specResponse;
		List<APILink> links = null;
		List<String> contentTypes;

		for (Entry<String, Response> responseEntry : specResponses.entrySet()) {
			String responseCode, description, schemaType = "", ref = "", contentSchemaFormat = "";
			ResponseSchema contentSchema = null;

			responseCode = responseEntry.getKey();
			specResponse = responseEntry.getValue();
			description = specResponse.getDescription();
			contentTypes = specResponse.getContentMediaTypes()!= null? new ArrayList<>(specResponse.getContentMediaTypes().keySet()): new ArrayList<>();

			if(!contentTypes.isEmpty()) {
				org.openapi4j.parser.model.v3.Schema responseSchema = null;

				if(specResponse.getContentMediaType(JSON) != null)
					responseSchema = specResponse.getContentMediaType(JSON).getSchema();
				else if(specResponse.getContentMediaType(ALL_CONTENT_TYPES) != null)
					responseSchema = specResponse.getContentMediaType(ALL_CONTENT_TYPES).getSchema();
				else {
					System.err.println("This specification has content types that are not JSON.");
					System.exit(1);
				}

				// If both type and $ref are null, then we consider that the schema is not defined (or empty)
				if(responseSchema.getRef() != null || responseSchema.getType() != null){
					if (!responseSchema.isRef()) { // array or integer
						schemaType = responseSchema.getType();

						if (schemaType.equals(ARRAY)) {
							ref = responseSchema.toNode().get("items").get(REF) != null?
									responseSchema.toNode().get("items").get(REF).asText() :
									responseSchema.toNode().get("items").get("type").asText();
						} else {
							contentSchemaFormat = responseSchema.getFormat();
						}
					} else {
						if(specResponse.getContentMediaType(JSON) != null)
							ref = specResponse.getContentMediaType(JSON).getSchema().getRef();
						else if (specResponse.getContentMediaType(ENCODED_URL) != null)
							ref = specResponse.getContentMediaType(ENCODED_URL).getSchema().getRef();
						else
							ref = specResponse.getContentMediaType(ALL_CONTENT_TYPES).getSchema().getRef();
					}
					// ref can be an object reference or the reference for the array items (mutually exclusive)
					if(ref.equals(""))
						contentSchema = new ResponseSchema(schemaType, contentSchemaFormat, null, null);
					else
						contentSchema = new ResponseSchema(schemaType, contentSchemaFormat, getReferencedSchema(ref, schemas), ref);
				}
			}

			if(specResponse.getLinks() != null)
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

		for(Entry<String, Link> entryLink: specLinks.entrySet()) {
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
		Map<String, org.openapi4j.parser.model.v3.Schema> props = s.getProperties();
		List<APIProperty> properties = new ArrayList<>();
		List<String> requiredFields;
		String name, type, regex, itemsType, format, ref;
		int minimum, maximum;
		boolean required, gen, isCollection;

		String schemaType = s.getType();

		switch (schemaType){
			case NUMBER -> {
				minimum = s.getMinimum() != null? (int) s.getMinimum() : NO_MIN;
				maximum = s.getMaximum() != null? (int) s.getMaximum() : NO_MAX;
				format = s.getFormat() != null? s.getFormat() : "";

				APIProperty property = new APIProperty(schemaName, schemaType, "", format, "", null,
						minimum, maximum, false, true, false);

				properties.add(property);

				 schema = new Schema(s.getType(), schemaName, properties);
			}
			case STRING -> {
				regex = s.getPattern() != null? s.getPattern() : "";

				APIProperty property = new APIProperty(schemaName, schemaType, regex, "", "", null,
						NO_MIN, NO_MAX, false, true, false);

				properties.add(property);

				schema = new Schema(s.getType(), schemaName, properties);
			}
			case BOOLEAN -> {
				APIProperty property = new APIProperty(schemaName, schemaType, "", "", "", null,
						NO_MIN, NO_MAX, false, true, false);

				properties.add(property);

				schema = new Schema(s.getType(), schemaName, properties);
			}
			case ARRAY -> {
				itemsType = s.getItemsSchema().getRef();

				APIProperty property = new APIProperty(schemaName, schemaType, "", "", itemsType, null,
						NO_MIN, NO_MAX, true, true, false);
				properties.add(property);

				schema = new Schema(s.getType(), schemaName, properties);
			}
			case OBJECT -> {
				if(props == null) {
					properties = new ArrayList<>();
					System.out.println("Schema [" + schemaName + "] has no properties defined.\n");
				} else {
					for (Entry<String, org.openapi4j.parser.model.v3.Schema> propertiesEntry : props.entrySet()) {
						requiredFields = s.hasRequiredFields() ? s.getRequiredFields() : null;

						name = propertiesEntry.getKey();
						type = propertiesEntry.getValue().getType();

						if(type != null) {
							minimum = type.equals("integer") && propertiesEntry.getValue().getMinimum() != null ?
									(int) propertiesEntry.getValue().getMinimum() : NO_MIN;
							maximum = type.equals("integer") && propertiesEntry.getValue().getMaximum() != null ?
									(int) propertiesEntry.getValue().getMaximum() : NO_MAX;
							format = type.equals("integer") ? propertiesEntry.getValue().getFormat() : "";
							isCollection = type.equalsIgnoreCase(ARRAY);

							required = requiredFields != null && requiredFields.contains(name);

							// TODO what if it's not a referenced type?
							JsonNode schemaNode = propertiesEntry.getValue().toNode();

							itemsType = isCollection ? getItemTypes(propertiesEntry.getValue()) : null;
							regex = schemaNode.get(PATTERN) != null ?
									schemaNode.get(PATTERN).toString().replace("\"", "") : null;
							gen = schemaNode.get(GEN) != null && schemaNode.get(GEN).asBoolean();

							properties.add(new APIProperty(name, type, regex, format, itemsType, null,
									minimum, maximum, isCollection, required, gen));
						} else {
							// The property has a referenced schema
							ref = propertiesEntry.getValue().getRef().replace("/components", "");
							properties.add(new APIProperty(name, null, "", "", "", ref,
									NO_MIN, NO_MAX, false, false, false));
						}
					}
				}

				schema = new Schema(s.getType(), schemaName, properties);
			}
		}

		return schema;
	}

	// Auxiliary Methods

	private static String getItemTypes(org.openapi4j.parser.model.v3.Schema schema) throws EncodeException {
		boolean isReferenced = schema.toNode().get("items").get(REF) != null;

		if(isReferenced) {
			String[] split = schema.toNode().get("items").get(REF).toString()
					.replace("\"", "")
					.split("/");
			return split[split.length - 1];
		} else
			return "TODO";
	}

	private static Schema getReferencedSchema(String refType, Map<String, Schema> schemas) {
		Schema schema = null;

		for(Entry<String, Schema> s : schemas.entrySet()) {
			String[] parts = refType.split("/");
			String type = parts[parts.length - 1];

			if(s.getKey().equalsIgnoreCase(type))
				schema = s.getValue();
		}

		return schema;
	}
}
