package hello;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * This is the advice of last resort. Anything that makes it here is treated as an internal server error.
 */

@Slf4j
@RestControllerAdvice
@Order(2)
public class ControllerAdviceBravo {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseBody> handleException(Exception exception) {
        log.error("Uncaught exception in rest controller", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ErrorResponseBody.globalError("INTERNAL_SERVER_ERROR"));
    }
}
