package vn.doancnpm;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import vn.doancnpm.controller.HealthController;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    private static final String BASE_URL = "/api/health";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();
    }

    @Test
    @DisplayName("GET /api/health trả về 200 OK cùng thông tin ứng dụng và trạng thái UP")
    void health_returnsApplicationAndUpStatus() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.application").value("DoAnCNPM"))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("POST /api/health không được hỗ trợ -> trả về 405 Method Not Allowed")
    void health_postMethodNotAllowed_returns405() throws Exception {
        mockMvc.perform(post(BASE_URL))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("PUT /api/health không được hỗ trợ -> trả về 405 Method Not Allowed")
    void health_putMethodNotAllowed_returns405() throws Exception {
        mockMvc.perform(put(BASE_URL))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("DELETE /api/health không được hỗ trợ -> trả về 405 Method Not Allowed")
    void health_deleteMethodNotAllowed_returns405() throws Exception {
        mockMvc.perform(delete(BASE_URL))
                .andExpect(status().isMethodNotAllowed());
    }
}

