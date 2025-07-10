package hello;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorResponseBody {
    private final List<ErrorResponseItem> errors = new ArrayList<>();

    public static ErrorResponseBody globalError(String code) {
        ErrorResponseBody result = new ErrorResponseBody();
        result.getErrors().add(new ErrorResponseItem(null, code, null));
        return result;
    }

    public static ErrorResponseBody globalError(String code, String message) {
        ErrorResponseBody result = new ErrorResponseBody();
        result.getErrors().add(new ErrorResponseItem(null, code, message));
        return result;
    }

    public static ErrorResponseBody fieldError(String field, String code) {
        ErrorResponseBody result = new ErrorResponseBody();
        result.getErrors().add(new ErrorResponseItem(field, code, null));
        return result;
    }

    public static ErrorResponseBody fieldError(String field, String code, String message) {
        ErrorResponseBody result = new ErrorResponseBody();
        result.getErrors().add(new ErrorResponseItem(field, code, message));
        return result;
    }

    public static ErrorResponseBody fromErrors(Errors errors) {
        ErrorResponseBody result = new ErrorResponseBody();
        for (ObjectError error : errors.getGlobalErrors()) {
                result.errors.add(new ErrorResponseItem(null, error.getCode(), error.getDefaultMessage()));
            }
            for (FieldError error : errors.getFieldErrors()) {
                result.errors.add(new ErrorResponseItem(error.getField(), error.getCode(), error.getDefaultMessage()));
            }
        return result;
    }

    public record ErrorResponseItem(String field, String code, String message) {}
}
