package oas_custom_parser.domain;

import utils.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class Specification {

	public static final String QUERY = "query";

	private List<String> servers;
	private Map<String, APIOperation> operationsById;
	private transient Map<String, List<APIOperation>> operationsByTags;
	private List<String> invariants;
	private Map<String, Schema> schemas;
	private transient List<String> paths;

	
	public Specification(List<String> servers, Map<String, APIOperation> operations, Map<String, Schema> schemas, List<String> paths) {
		this.servers = servers;
		this.operationsById = operations;
		this.invariants = new ArrayList<>();
		this.schemas = schemas;
		this.paths = paths;

		initOperationsByTags();
		invariants.add("T");
	}


	public void addInvariant(String inv) {
		invariants.add(inv);
	}

	public List<String> getServers(){
		return servers;
	}
	
	public Map<String, APIOperation> getOperationsById() {
		return operationsById;
	}

	/**
	 * Returns operations by path, method and id. e.g, < "/resources", <"GET", APIOperation> >
	 * @return operations by path.
	 */
	public Map<String, Map<String, APIOperation>> getOperationsByPath() {
		Map<String, Map<String, APIOperation>> by_path = new HashMap<>();
		APIOperation op;
		String verb, uri;
		Map<String, APIOperation> path_ops;

		for (Entry<String, APIOperation> e : operationsById.entrySet()) {
			op = e.getValue();
			uri = op.getUri().toString();
			verb = op.getVerb();

			path_ops = by_path.containsKey(uri) ? by_path.get(uri) : new HashMap<>();
			path_ops.put(verb, op);

			by_path.put(uri, path_ops);
		}

		return by_path;
	}
	
	public List<String> getInvariants() {
		return invariants;
	}

	public Map<String, List<APIOperation>> getOperationsByTags() {
		return operationsByTags;
	}
	
	public Map<String, Schema> getSchemas() {
		return schemas;
	}
	
	public String getParameterRegex(String param) {
		for(Map.Entry<String, Schema> s : schemas.entrySet())
			for(APIProperty p: s.getValue().getProperties())
				if(p.getName().equals(param))
					return p.getPattern();
		
		return "";
	}
	
	public String getParameterType(String param) {
		for(Map.Entry<String, Schema> s : schemas.entrySet())
			for(APIProperty p: s.getValue().getProperties())
				if(p.getName().equals(param))
					return p.getType();		
		
		return "";
	}
	
	public int getParameterMinimum(String param) {
		for(Map.Entry<String, Schema> s : schemas.entrySet())
			for(APIProperty p: s.getValue().getProperties())
				if(p.getName().equals(param))
					return p.getMinimum();		
		
		return -99;
	}
	
	public List<APIOperation> getDeletes(){
		List<APIOperation> deletes = new ArrayList<>();
		APIOperation op;

		for(Map.Entry<String, APIOperation> entry: operationsById.entrySet()){
			op = entry.getValue();

			if(op.getVerb().equalsIgnoreCase("DELETE"))
				deletes.add(op);
		}

		return deletes;
	}

	public List<String> getPaths () {
		return paths;
	}

	@Override
	public String toString () {
		StringBuilder str = new StringBuilder();

		for(Map.Entry<String, APIOperation> entry : operationsById.entrySet())
			str.append("\n").append(entry.getValue().toString());

		return "\n**** Spec Operations ****\n" + str + "\n\nOperations: " + operationsById.size();
	}

	public APIOperation findGetAll(String tag) {
		List<APIOperation> tagOps = operationsByTags.get(tag);

		for(APIOperation op: tagOps)
			if(op.getUri().toString().equals("/" + tag) && op.getVerb().equalsIgnoreCase("GET"))
				return op;

		return null;
	}

	public List<String> findGetOnes(APIOperation op){
		return StringUtils.getOnes(op.getUri().toString());
	}

	private void initOperationsByTags() {
		operationsByTags = new HashMap<>();
		List<APIOperation> ops;
		List<String> tags;
		APIOperation op;

		for(Map.Entry<String, APIOperation> entry: operationsById.entrySet()) {
			op = entry.getValue();
			tags = op.getTags();

			for(String tag: tags) {
				ops = operationsByTags.containsKey(tag)? operationsByTags.get(tag) : new ArrayList<>();
				ops.add(op);
				operationsByTags.put(tag, ops);
			}
		}
	}

	public void printOperationsByTags(boolean printAPOSTL) {
		String uri, verb;
		for(Map.Entry<String, List<APIOperation>> e: operationsByTags.entrySet()) {
			System.out.println(e.getKey() + ":");

			for(APIOperation operation: e.getValue()) {
				uri =  operation.getUri().toString();
				verb = operation.getVerb();

				switch (operation.getVerb().toUpperCase()){
					case "POST":
						if(printAPOSTL) {
							System.out.println("   " + verb + " " + uri);
							operation.printAPOSTL();
						} else
							System.out.println("   " + verb + "   "  + uri);
						break;
					case "PUT", "GET":
						if(printAPOSTL) {
							System.out.println("   " + verb + " "  + uri);
							operation.printAPOSTL();
						} else
							System.out.println("   " + verb + "    "  + uri);
						break;
					case "PATCH":
						if(printAPOSTL) {
							System.out.println("   " + verb + " "  + uri);
							operation.printAPOSTL();
						} else
							System.out.println("   " + verb + "  "  + uri);
						break;
					default: // DELETE
						System.out.println("   " + verb + " "  + uri);
						if(printAPOSTL)
							operation.printAPOSTL();

				}
			}
			System.out.println();
		}
	}

	public void printOperationsByPaths() {
		Map<String, Map<String, APIOperation>> by_path = getOperationsByPath();
		Map<String, APIOperation> path_ops;
		APIOperation op;
		String uri, verb, op_id;

		for (Entry<String, Map<String, APIOperation>> e1 : by_path.entrySet()) {
			uri = e1.getKey();
			path_ops = e1.getValue();

			System.out.println(uri + ":");

			for (Entry<String, APIOperation> e2 : path_ops.entrySet()) {
				op = e2.getValue();
				op_id = op.getOperationId();
				verb = e2.getKey();

				System.out.println("\t" + verb + " " + op_id);
			}
		}
	}

	public void printSchemas() {
		List<APIProperty> props;
		Schema s;

		if(schemas.isEmpty())
			System.out.println("Schemas: {}");
		else {
			System.out.println("Schemas: ");

			for (Map.Entry<String, Schema> e : schemas.entrySet()) {
				s = e.getValue();
				props = s.getProperties();

				System.out.print("   " + s.getName() + " {");

				for (int i = 0; i < props.size(); i++)
					if (i == props.size() - 1)
						System.out.println(props.get(i).getName() + "}");
					else
						System.out.print(props.get(i).getName() + ", ");
			}
		}
	}
}
