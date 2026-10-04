package com.mojing.novel.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RegistrationControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AppUserRepository users;

    @Test
    void registrationCreatesAnActiveAccountAndItsOwnEmptyBookshelf() throws Exception {
        String username = randomUsername();
        JsonNode registered = register(username, "新作者", "strongPass123");
        String token = registered.path("accessToken").asText();
        long id = registered.path("user").path("id").asLong();

        assertThat(token).isNotBlank();
        assertThat(registered.path("user").path("username").asText()).isEqualTo(username);
        assertThat(registered.path("user").path("nickname").asText()).isEqualTo("新作者");
        assertThat(users.findById(id)).isPresent()
                .get().extracting(AppUserEntity::getPasswordHash).asString().isNotEqualTo("strongPass123");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value(username));
        mockMvc.perform(get("/api/novels").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void usernameIsCaseInsensitiveAndCannotBeRegisteredTwice() throws Exception {
        String username = randomUsername();
        register(username.toUpperCase(), "第一位作者", "strongPass123");
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload(username, "第二位作者", "strongPass456")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("用户名已被使用"));
    }

    @Test
    void registeredAccountsCannotSeeEachOthersNovels() throws Exception {
        String firstToken = register(randomUsername(), "甲", "strongPass123").path("accessToken").asText();
        String secondToken = register(randomUsername(), "乙", "strongPass456").path("accessToken").asText();
        String novelJson = mockMvc.perform(post("/api/novels").header("Authorization", "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"私有小说\",\"description\":\"只有甲能看\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long novelId = objectMapper.readTree(novelJson).path("id").asLong();

        mockMvc.perform(get("/api/novels").header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/novels/{id}", novelId).header("Authorization", "Bearer " + secondToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRegistrationIsRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload("1bad", "作者", "strongPass123")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload(randomUsername(), "作者", "1234567")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload(randomUsername(), "   ", "strongPass123")))
                .andExpect(status().isBadRequest());
    }

    private JsonNode register(String username, String nickname, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(payload(username, nickname, password)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String payload(String username, String nickname, String password) throws Exception {
        return objectMapper.writeValueAsString(new AuthDtos.RegisterRequest(username, nickname, password));
    }

    private String randomUsername() {
        return "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
