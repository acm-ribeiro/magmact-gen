package oas_custom_parser.domain;

public class APIProperty {

	private String name, type, pattern, format, itemsType, itemsFormat, itemsPattern, ref;
	private int minimum, maximum;
	private boolean isCollection, required, gen;

	public APIProperty(String name, String type, String pattern, String format, String itemsType,
					   String itemsFormat, String itemsPattern, String ref, int minimum,
					   int maximum, boolean isCollection, boolean required, boolean gen) {
		this.name = name;
		this.type = type;
		this.pattern = pattern;
		this.format = format;
		this.itemsType = itemsType;
		this.itemsFormat = itemsFormat;
		this.itemsPattern = itemsPattern;
		this.minimum = minimum;
		this.maximum = maximum;
		this.isCollection = isCollection;
		this.required = required;
		this.gen = gen;
		this.ref = ref;
	}

	public String getRef() {
		return ref;
	}

	public boolean isRequired() {
		return required;
	}
	
	public boolean gen() {
		return gen;
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getFormat() {
		return format;
	}
	
	public int getMinimum () {
		return minimum;
	}
	
	public int getMaximum () {
		return maximum;
	}
	
	public String getItemsType() {
		return itemsType;
	}

	public String getItemsFormat() {
		return itemsFormat;
	}

	public String getItemsPattern() {
		return itemsPattern;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}
	
	public boolean isCollection() {
		return isCollection;
	}
	
	public String toString () {		
		String space = "\n        ";
		if(isCollection)
			return space + name + " {type: " + type + ", pattern: " + pattern + ", itemType: " + itemsType + ", required: " + required + "}";
		else if(minimum == -999 && format.equals(""))
			return space + name + " {type: " + type + ", pattern: " + pattern + ", required: " + required + "}";
		else if(minimum != -999 && format.equals(""))
			return space + name + " {type: " + type + ", pattern: " + pattern +  ", required: " + required + ", minimum: " + minimum + "}";
		else if(minimum == -999 && !format.equals(""))
			return space + name + " {type: " + type + ", pattern: " + pattern + ", required: " + required + ", format: " + format + "}";
		else
			return space + name + " {type: " + type + ", pattern: " + pattern + ", required: " + required + ", format: " + format + ", minimum: " + minimum + "}";
	
	}
}
