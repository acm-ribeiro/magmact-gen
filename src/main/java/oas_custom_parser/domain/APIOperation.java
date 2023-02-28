package oas_custom_parser.domain;

import java.util.ArrayList;
import java.util.List;

public class APIOperation {

	private String verb, operationId;
	private URI uri;
	private List<String> requires, ensures;
	private List<URLParameter> pathParams, queryParams;
	private Schema body;
	private List<String> contentTypes;
	private List<APIResponse> responses;
	private List<String> tags;

	public APIOperation(String url, String verb, String operationId, Schema body, List<URLParameter> params,
						List<APIResponse> responses, List<String> contentTypes, List<String> tags) {
		this.uri = new URI(url);
		this.verb = verb.toUpperCase();
		this.operationId = operationId;
		this.requires = new ArrayList<>();
		this.ensures = new ArrayList<>();
		this.body = body;
		this.responses = responses;
		this.contentTypes = contentTypes;
		this.tags = tags;

		pathParams = new ArrayList<>();
		queryParams = new ArrayList<>();

		if (params != null)
			for (URLParameter p : params)
				switch (p.getIn().toLowerCase()) {
					case "path" -> pathParams.add(p);
					case "query" -> queryParams.add(p);
				}
	}

	public URI getUri() {
		return uri;
	}
	
	public String getVerb () {
		return verb;
	}

	public String getOperationId () {
		return operationId;
	}

	public List<String> getRequires() {
		return requires;
	}

	public void addPrecondition(String pre){
		requires.add(pre);
	}

	public void addPostcondition(String post){
		ensures.add(post);
	}


	public List<String> getEnsures() {
		return ensures;
	}
	
	public Schema getBody () {
		return body;
	}

	public List<String> getBodyContentTypes() {
		return contentTypes;
	}
	
	public List<URLParameter> getPathParams () {
		return pathParams;
	}

	public List<URLParameter> getQueryParams () {
		return queryParams;
	}

	public List<APIResponse> getResponses () {
		return responses;
	}

	public List<String> getTags() {
		return tags;
	}

	public boolean isGetAll() {
		return verb.equalsIgnoreCase("GET") && !uri.toString().contains("{");
	}

	public boolean multipleParameters() {
		List<String> parts = uri.getParts();
		int numParams = 0;

		for(String p : parts)
			if(p.contains("{"))
				numParams++;

		return numParams > 1;
	}

	public boolean isGetOne() {
		List<String> parts = uri.getParts();
		boolean isParam = parts.get(parts.size() - 1).contains("{");

		return verb.equalsIgnoreCase("GET") && isParam;
	}


	@Override
	public String toString () {
		StringBuilder builder = new StringBuilder(
				"\nOPERATION ID: " + operationId + "\n"
				+ "URI: " + verb + " " + uri.toString() + "\nTAGS: {"
		);

		for(String tag: tags)
			builder.append(tag)
			   .append(", ");

		return builder.substring(0, builder.length() - 2) + "}\n";
	}

	public void printAPOSTL() {
		System.out.println("      Requires");

		for(String req: requires)
			System.out.println("         " + req);

		System.out.println("      Ensures");

		for(String ens: ensures)
			System.out.println("         " + ens);

		System.out.println();
	}
}
