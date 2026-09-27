package com.kata.backend.user;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PasswordAndRoleTests {

    @Autowired
    MockMvc mvc;

    @Test
    void changePasswordRevokesOldTokensAndOldPassword() throws Exception {
        register("pwuser");
        String oldToken = login("pwuser", "password123");

        mvc.perform(put("/api/users/me/password").header("Authorization", bearer(oldToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"wrong-password","newPassword":"newPassword456"}"""))
                .andExpect(status().isBadRequest());

        mvc.perform(put("/api/users/me/password").header("Authorization", bearer(oldToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"password123","newPassword":"newPassword456"}"""))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/users/me").header("Authorization", bearer(oldToken)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"pwuser","password":"password123"}"""))
                .andExpect(status().isUnauthorized());

        String newToken = login("pwuser", "newPassword456");
        mvc.perform(get("/api/users/me").header("Authorization", bearer(newToken)))
                .andExpect(status().isOk());
    }

    @Test
    void newUsersAreRegularUsersAndCannotAccessAdminApi() throws Exception {
        register("plain");
        String token = login("plain", "password123");

        mvc.perform(get("/api/users/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.role").value("USER"));
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListUsersAndChangeRolesEffectiveImmediately() throws Exception {
        String userId = register("promoted");
        String userToken = login("promoted", "password123");
        String adminToken = login("admin", "admin12345");

        mvc.perform(get("/api/admin/users").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'promoted')].role").value("USER"));

        mvc.perform(put("/api/admin/users/{id}/role", userId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"role":"ADMIN"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // The user's existing token picks up the new role without logging in again
        mvc.perform(get("/api/admin/users").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk());
    }

    @Test
    void adminCannotChangeOwnRole() throws Exception {
        String adminToken = login("admin", "admin12345");
        String body = mvc.perform(get("/api/users/me").header("Authorization", bearer(adminToken)))
                .andReturn().getResponse().getContentAsString();
        Integer adminId = JsonPath.read(body, "$.id");

        mvc.perform(put("/api/admin/users/{id}/role", adminId).header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"role":"USER"}"""))
                .andExpect(status().isBadRequest());
    }

    private String register(String username) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"password123"}"""
                                .formatted(username, username)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((Integer) JsonPath.read(body, "$.id"));
    }

    private String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
