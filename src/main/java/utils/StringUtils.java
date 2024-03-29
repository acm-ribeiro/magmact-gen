package utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import com.google.gson.JsonElement;


public class StringUtils {

	// Converts an entry set to a list of entries
	public static List<Entry<String, JsonElement>> fromEntrySetToList(Set<Entry<String, JsonElement>> properties) {
		return new ArrayList<>(properties);
	}

	public static List<String> getOnes(String url) {
		List<String> ones = getOnesPartial(url);
		List<String> result = new ArrayList<>();
		StringBuilder soFar = new StringBuilder();

		for(int i = 0; i < ones.size(); i++) {
			if (i == 0)
				result.add(ones.get(i));
			else
				result.add(soFar + ones.get(i));
			soFar.append(ones.get(i));
		}

		return result;
	}

	private static List<String> getOnesPartial(String url){
		List<String> result = new ArrayList<>();
		List<String> parts = List.of(url.split("/"));
		String one = "dummy", newUrl;

		// root url
		if(url.equals("/"))
			return result;

		// if the last element of the url is not a parameter, remove it
		if(!parts.get(parts.size() - 1).contains("{"))
			parts = parts.subList(0, parts.size() - 1);

		newUrl = joinParts(parts);

		while(!one.equals("")) {
			one = getOne(List.of(newUrl.split("/")));
			result.add(one);
			
			newUrl = newUrl.replace(one, "");
		}

		result.remove("");

		return result;
	}

	private static String getOne(List<String> parts){
		StringBuilder str = new StringBuilder();

		for (String part : parts) {
			str.append(part).append("/");

			if (part.contains("{"))
				break;
		}

		return str.substring(0, str.length()-1);
	}

	private static String joinParts(List<String> parts) {
		StringBuilder builder = new StringBuilder();

		for(String p : parts)
			builder.append(p).append("/");

		return builder.substring(0, builder.length()-1);
	}
}
