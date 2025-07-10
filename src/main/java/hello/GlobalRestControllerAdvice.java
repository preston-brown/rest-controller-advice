package hello;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestControllerAdvice
public class GlobalRestControllerAdvice {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseBody> handleException(Exception exception) {
        log.error("Uncaught exception in rest controller", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("INTERNAL_SERVER_ERROR"));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponseBody> handleException(HttpMediaTypeNotAcceptableException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("UNSUPPORTED_TYPE", "The resource cannot return the requested content type."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseBody> handleException(HttpMediaTypeNotSupportedException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("INVALID_CONTENT_TYPE", "The resource does not accept the provided content type."));
    }

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

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseBody> handleException(HttpRequestMethodNotSupportedException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("METHOD_NOT_ALLOWED", "This method is not allowed for the requested resource."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseBody> handleException(MethodArgumentNotValidException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.fromErrors(exception));
    }

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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseBody> handleException(NoResourceFoundException exception) {
        log.debug("Handling exception in controller advice", exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("INVALID_RESOURCE", "The requested resource does not exist."));
    }
}
