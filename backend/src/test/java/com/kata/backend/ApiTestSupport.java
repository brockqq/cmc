package com.kata.backend;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers for API integration tests. Usernames must be unique across tests (shared context/DB). */
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    protected record Created(String id, String inviteCode) {
    }

    protected ResultActions call(String token, MockHttpServletRequestBuilder req, String json) throws Exception {
        req.header("Authorization", "Bearer " + token);
        if (json != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(req);
    }

    protected String adminToken() throws Exception {
        return login("admin", "admin12345");
    }

    protected Created createCommunity(String adminToken, String name) throws Exception {
        String body = call(adminToken, post("/api/admin/communities"), "{\"name\":\"%s\"}".formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Created(String.valueOf((Integer) JsonPath.read(body, "$.id")), JsonPath.read(body, "$.inviteCode"));
    }

    protected void makeManager(String adminToken, Created community, String username) throws Exception {
        call(adminToken, post("/api/admin/communities/{id}/managers", community.id()),
                "{\"username\":\"%s\"}".formatted(username))
                .andExpect(status().isOk());
    }

    /** Registers a user (optionally joining via invite code) and returns their id. */
    protected String register(String username, String inviteCode) throws Exception {
        String code = inviteCode == null ? "null" : "\"" + inviteCode + "\"";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"password123","inviteCode":%s}"""
                                .formatted(username, username, code)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return String.valueOf((Integer) JsonPath.read(body, "$.id"));
    }

    protected String login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}""".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected String login(String username) throws Exception {
        return login(username, "password123");
    }

    protected static String idOf(ResultActions result) throws Exception {
        return String.valueOf((Integer) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id"));
    }
}
