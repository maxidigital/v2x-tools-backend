package main.repo;

/** The repo has no definition for the requested alias / message (HTTP 404). */
public class DefinitionNotFoundException extends RuntimeException {
    public DefinitionNotFoundException(String message) {
        super(message);
    }
}
