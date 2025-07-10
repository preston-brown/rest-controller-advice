package hello;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateRequest {
    @NotNull
    private Integer id;
    @NotNull
    private String name;
    private Status status;
    private LocalDate startDate;
}
