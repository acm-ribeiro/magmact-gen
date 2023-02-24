package run.exceptions;

import java.io.Serial;

public class CreateDirectoryException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    public CreateDirectoryException(String directory) {
        super("Unable to create the following directory: " + directory + ".");
    }
}