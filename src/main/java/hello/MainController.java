package hello;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping(value= "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class MainController {

    public record User(int id, boolean includeAddress) {}

    @GetMapping("/{id}")
    public User getUser(@PathVariable int id, @RequestParam Optional<Boolean> includeAddress) {
        return new User(id, includeAddress.orElse(false));
    }

    @PostMapping
    public User createUser(@RequestBody @Valid CreateRequest request) {
        return new User(request.getId(), false);
    }
}
