package run.exceptions;

public class NoSpecificationPathException extends Exception {

    public NoSpecificationPathException() {
        super("Please, provide the Open API Specification JSON file path.");
    }

}
