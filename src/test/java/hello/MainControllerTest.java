package hello;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * We have to import hello.Config so that the MainController uses the same ObjectMapper as it using during production
 * runs. Otherwise, tests that rely on that mapper (fail on unknown properties) will not pass.
 * <p></p>
 * In contrast, the ObjectMapper that we create in beforeAll() is only used for deserialization in these unit tests.
 * It has no effect on the MainController's behavior.
 */

@WebMvcTest(MainController.class)
@Import(hello.Config.class)
public class MainControllerTest {

    private static ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    public void invalidResource() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/blah/blah"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponseBody errorResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorResponseBody.class);

        ErrorResponseBody.ErrorResponseItem error = errorResponseBody.getErrors().get(0);
        assertNull(error.field());
        assertEquals("INVALID_RESOURCE", error.code());
        assertEquals(1, errorResponseBody.getErrors().size());
    }

    @Test
    public void get_success() throws Exception {
        mockMvc.perform(get("/api/users/1?includeAddress=true").accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    public void unsupportedType() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                        get("/api/users/1?includeAddress=true")
                                .accept(MediaType.APPLICATION_XML))
                .andDo(print())
                .andExpect(status().isNotAcceptable())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponseBody errorResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorResponseBody.class);

        ErrorResponseBody.ErrorResponseItem error = errorResponseBody.getErrors().get(0);
        assertNull(error.field());
        assertEquals("UNSUPPORTED_TYPE", error.code());
        assertEquals(1, errorResponseBody.getErrors().size());
    }

    @Test
    public void badMethod() throws Exception {
        MvcResult mvcResult = mockMvc.perform(put("/api/users"))
                .andDo(print())
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponseBody errorResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorResponseBody.class);

        ErrorResponseBody.ErrorResponseItem error = errorResponseBody.getErrors().get(0);
        assertNull(error.field());
        assertEquals("METHOD_NOT_ALLOWED", error.code());
        assertEquals(1, errorResponseBody.getErrors().size());
    }

    @ParameterizedTest
    @MethodSource
    public void get_parameterTypeMismatch(String userId, String includeAddress, ResultMatcher expectedStatus, String expectedField, String expectedCode) throws Exception {
        String url = "/api/users/" + userId + (includeAddress == null ? "" : "?includeAddress=" + includeAddress);

        MvcResult mvcResult = mockMvc.perform(get(url))
                .andDo(print())
                .andExpect(expectedStatus)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponseBody errorResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorResponseBody.class);

        ErrorResponseBody.ErrorResponseItem error = errorResponseBody.getErrors().get(0);
        assertEquals(expectedField, error.field());
        assertEquals(expectedCode, error.code());
        assertEquals(1, errorResponseBody.getErrors().size());
    }

    private static List<Arguments> get_parameterTypeMismatch() {
        return List.of(
                Arguments.of("not-an-integer", null, status().isNotFound(), null, "INVALID_RESOURCE"),
                Arguments.of(String.valueOf(Long.MAX_VALUE), null, status().isNotFound(), null, "INVALID_RESOURCE"),
                Arguments.of("1", "not-a-boolean", status().isBadRequest(), "includeAddress", "BAD_QUERY_PARAMETER")
        );
    }

    @ParameterizedTest
    @MethodSource
    public void post_badRequestBody(String content, ResultMatcher expectedStatus, String expectedField, String expectedCode) throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .characterEncoding(StandardCharsets.UTF_8)
                                .content(content))
                .andDo(print())
                .andExpect(expectedStatus)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        ErrorResponseBody errorResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), ErrorResponseBody.class);

        ErrorResponseBody.ErrorResponseItem error = errorResponseBody.getErrors().get(0);
        assertEquals(expectedField, error.field());
        assertEquals(expectedCode, error.code());
        assertEquals(1, errorResponseBody.getErrors().size());
    }

    private static List<Arguments> post_badRequestBody() {
        String invalidJson = "}{";
        String invalidIntegerValue = """
                {"id": "abc", "name": "name", "status": "OPEN", "startDate": "2025-01-01"}""";
        String integerValueTooLarge = """
                {"id": "92233720368547758070", "name": "name", "status": "OPEN", "startDate": "2025-01-01"}""";
        String missingRequiredProperty = """
                {"id": 1, "status": "OPEN", "startDate": "2025-01-01"}""";
        String invalidEnumerationValue = """
                {"id": 1, "name": "name", "status": "XXXX", "startDate": "2025-01-01"}""";
        String invalidDate = """
                {"id": 1, "name": "name", "status": "OPEN", "startDate": "X-01-01"}""";
        String extraProperty = """
                {"id": 1, "name": "name", "status": "OPEN", "startDate": "2025-01-01", "extra": "blah"}""";
        String missingIntegerValue = """
                {"name": "name", "status": "OPEN", "startDate": "2025-01-01"}""";
        return List.of(
                Arguments.of("", status().isBadRequest(), null, "INVALID_REQUEST_BODY"),
                Arguments.of(invalidJson, status().isBadRequest(), null, "INVALID_REQUEST_BODY"),
                Arguments.of(invalidIntegerValue, status().isBadRequest(), "id", "INVALID_VALUE"),
                Arguments.of(integerValueTooLarge, status().isBadRequest(), "id", "INVALID_VALUE"),
                Arguments.of(invalidEnumerationValue, status().isBadRequest(), "status", "INVALID_VALUE"),
                Arguments.of(invalidDate, status().isBadRequest(), "startDate", "INVALID_VALUE"),
                Arguments.of(extraProperty, status().isBadRequest(), "extra", "UNEXPECTED_PROPERTY"),
                Arguments.of(missingIntegerValue, status().isUnprocessableEntity(), "id", "NotNull"),
                Arguments.of(missingRequiredProperty, status().isUnprocessableEntity(), "name", "NotNull"));

    }

}
