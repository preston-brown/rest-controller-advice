package hello;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * This advice handles exceptions thrown by Spring's web layer during request processing.
 * It covers situations such as:
 * - no method found to handle the request
 * - unable to convert some part of the request into the parameters the method expects
 * - some part of the request failed validation after conversion
 */

@Slf4j
@RestControllerAdvice
@Order(1)
public class ControllerAdviceAlfa {

    /**
     * Thrown by Spring when a request has an unacceptable Accept header value.
     * In our case this means they requested something other than application/json.
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponseBody> handleException(HttpMediaTypeNotAcceptableException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("UNSUPPORTED_TYPE", "The resource cannot return the requested content type."));
    }

    /**
     * Thrown by Spring when a request provides invalid content type.
     * In our case, this means anything other than application/json.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseBody> handleException(HttpMediaTypeNotSupportedException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("INVALID_CONTENT_TYPE", "The resource does not accept the provided content type."));
    }

    /**
     * Thrown by Spring when it is unable to deserialize the request body.
     * In our case, it means the JSON in the request body did match up with the POJO we are trying to convert it to.
     * <p>
     * We look at exception.cause() for three specific issues and give them more specific error messages.
     * Everything else gets "The request body is invalid" because we don't have anything better to tell them.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseBody> handleException(HttpMessageNotReadableException exception) {
        log.debug("Handling exception in controller advice", exception);
        if (exception.getCause() instanceof JsonParseException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ErrorResponseBody.globalError("INVALID_REQUEST_BODY", "The request body is not valid JSON."));
        }
        if (exception.getCause() instanceof UnrecognizedPropertyException cause) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ErrorResponseBody.fieldError(cause.getPropertyName(), "UNEXPECTED_PROPERTY", "Unexpected property found in request body."));
        }
        if (exception.getCause() instanceof InvalidFormatException cause) {
            String fieldName = cause.getPath().stream()
                    .map(JsonMappingException.Reference::getFieldName)
                    .collect(Collectors.joining("."));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ErrorResponseBody.fieldError(fieldName, "INVALID_VALUE"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("INVALID_REQUEST_BODY", "The request body is invalid."));
    }

    /**
     * Thrown by Spring when a call is made to a valid resource with an HTTP verb that it doesn't support.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseBody> handleException(HttpRequestMethodNotSupportedException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("METHOD_NOT_ALLOWED", "This method is not allowed for the requested resource."));
    }

    /**
     * Thrown by Spring when some property annotated with @Valid does not pass its validation.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseBody> handleException(MethodArgumentNotValidException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.fromErrors(exception));
    }

    /**
     * Thrown by Spring when a Path Variable or Query Parameter cannot be converted to the expected type.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseBody> handleException(MethodArgumentTypeMismatchException exception) {
        log.debug("Handling exception in controller advice", exception);
        boolean isPathVariable = exception.getParameter().getParameterAnnotation(PathVariable.class) != null;
        if (isPathVariable) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ErrorResponseBody.globalError("INVALID_RESOURCE", "The requested resource does not exist."));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(ErrorResponseBody.fieldError(exception.getPropertyName(), "BAD_QUERY_PARAMETER", "The requested resource does not exist."));
        }
    }

    /**
     * Thrown by Spring when it doesn't find a controller to handle a request.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseBody> handleException(NoResourceFoundException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("INVALID_RESOURCE", "The requested resource does not exist."));
    }
}
