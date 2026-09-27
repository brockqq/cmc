package com.kata.backend.community;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CommunityTests {

    @Autowired
    MockMvc mvc;

    @Test
    void residentJoinsWithInviteCodeAndSeesOnlyOwnCommunity() throws Exception {
        String admin = login("admin", "admin12345");
        Created sunny = createCommunity(admin, "陽光社區");
        Created river = createCommunity(admin, "河岸社區");

        String userId = register("res1", null);
        String res = login("res1", "password123");

        // Invite codes are case-insensitive
        call(res, post("/api/communities/join"), "{\"inviteCode\":\"%s\"}".formatted(sunny.inviteCode.toLowerCase()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("RESIDENT"));
        call(res, post("/api/communities/join"), "{\"inviteCode\":\"%s\"}".formatted(sunny.inviteCode))
                .andExpect(status().isConflict());
        call(res, post("/api/communities/join"), "{\"inviteCode\":\"NOPE1234\"}")
                .andExpect(status().isBadRequest());

        call(res, get("/api/communities/mine"), null)
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("陽光社區"));

        // Residents can view their community but not its invite code or member list
        call(res, get("/api/communities/{id}", sunny.id), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canManage").value(false))
                .andExpect(jsonPath("$.inviteCode").doesNotExist());
        call(res, get("/api/communities/{id}/members", sunny.id), null).andExpect(status().isForbidden());
        call(res, put("/api/communities/{id}/members/{uid}/role", sunny.id, userId), "{\"role\":\"MANAGER\"}")
                .andExpect(status().isForbidden());

        // Communities they have not joined are off limits entirely
        call(res, get("/api/communities/{id}", river.id), null).andExpect(status().isNotFound());
        // So is the platform admin API
        call(res, get("/api/admin/communities"), null).andExpect(status().isForbidden());
    }

    @Test
    void managerManagesOwnCommunityOnly() throws Exception {
        String admin = login("admin", "admin12345");
        Created a = createCommunity(admin, "A 社區");
        Created b = createCommunity(admin, "B 社區");

        register("mgr", null);
        String residentId = register("resA", a.inviteCode);
        String otherManagerCandidate = register("resA2", a.inviteCode);
        call(admin, post("/api/admin/communities/{id}/managers", a.id), "{\"username\":\"mgr\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        String mgr = login("mgr", "password123");
        call(mgr, get("/api/communities/{id}", a.id), null)
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.inviteCode").value(a.inviteCode))
                .andExpect(jsonPath("$.memberCount").value(3));
        call(mgr, get("/api/communities/{id}/members", a.id), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));

        call(mgr, put("/api/communities/{id}/members/{uid}/role", a.id, otherManagerCandidate), "{\"role\":\"MANAGER\"}")
                .andExpect(status().isOk());
        call(mgr, delete("/api/communities/{id}/members/{uid}", a.id, residentId), null)
                .andExpect(status().isNoContent());

        // Managers cannot demote or remove each other (in either direction); only the platform admin can
        String mgrId = String.valueOf((Integer) JsonPath.read(call(mgr, get("/api/users/me"), null)
                .andReturn().getResponse().getContentAsString(), "$.id"));
        String newMgr = login("resA2", "password123");
        call(newMgr, delete("/api/communities/{id}/members/{uid}", a.id, mgrId), null)
                .andExpect(status().isForbidden());
        call(newMgr, put("/api/communities/{id}/members/{uid}/role", a.id, mgrId), "{\"role\":\"RESIDENT\"}")
                .andExpect(status().isForbidden());
        call(mgr, put("/api/communities/{id}/members/{uid}/role", a.id, otherManagerCandidate), "{\"role\":\"RESIDENT\"}")
                .andExpect(status().isForbidden());
        call(admin, put("/api/communities/{id}/members/{uid}/role", a.id, otherManagerCandidate), "{\"role\":\"RESIDENT\"}")
                .andExpect(status().isOk());

        // Regenerating the invite code invalidates the old one
        String body = call(mgr, post("/api/communities/{id}/invite-code", a.id), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inviteCode").value(not(a.inviteCode)))
                .andReturn().getResponse().getContentAsString();
        String newCode = JsonPath.read(body, "$.inviteCode");
        register("late", null);
        String late = login("late", "password123");
        call(late, post("/api/communities/join"), "{\"inviteCode\":\"%s\"}".formatted(a.inviteCode))
                .andExpect(status().isBadRequest());
        call(late, post("/api/communities/join"), "{\"inviteCode\":\"%s\"}".formatted(newCode))
                .andExpect(status().isCreated());

        // Another community looks like it does not exist
        call(mgr, get("/api/communities/{id}/members", b.id), null).andExpect(status().isNotFound());
        call(mgr, get("/api/communities/{id}/members", 999_999), null).andExpect(status().isNotFound());
    }

    @Test
    void managerCannotChangeOrRemoveSelf() throws Exception {
        String admin = login("admin", "admin12345");
        Created c = createCommunity(admin, "自我測試社區");
        String mgrId = register("selfmgr", null);
        call(admin, post("/api/admin/communities/{id}/managers", c.id), "{\"username\":\"selfmgr\"}");
        String mgr = login("selfmgr", "password123");

        call(mgr, put("/api/communities/{id}/members/{uid}/role", c.id, mgrId), "{\"role\":\"RESIDENT\"}")
                .andExpect(status().isBadRequest());
        call(mgr, delete("/api/communities/{id}/members/{uid}", c.id, mgrId), null)
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerWithInvalidInviteCodeCreatesNoAccount() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ghost","email":"ghost@example.com","password":"password123","inviteCode":"BADCODE9"}"""))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"ghost","password":"password123"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void platformAdminCanManageAnyCommunityWithoutJoining() throws Exception {
        String admin = login("admin", "admin12345");
        Created c = createCommunity(admin, "平台測試社區");
        call(admin, post("/api/admin/communities"), "{\"name\":\"平台測試社區\"}").andExpect(status().isConflict());

        call(admin, get("/api/communities/{id}", c.id), null)
                .andExpect(jsonPath("$.canManage").value(true))
                .andExpect(jsonPath("$.myRole").doesNotExist());
        call(admin, get("/api/communities/{id}/members", c.id), null).andExpect(status().isOk());
    }

    private record Created(String id, String inviteCode) {
    }

    private Created createCommunity(String adminToken, String name) throws Exception {
        String body = call(adminToken, post("/api/admin/communities"), "{\"name\":\"%s\"}".formatted(name))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Created(String.valueOf((Integer) JsonPath.read(body, "$.id")), JsonPath.read(body, "$.inviteCode"));
    }

    private ResultActions call(String token, MockHttpServletRequestBuilder req, String json) throws Exception {
        req.header("Authorization", "Bearer " + token);
        if (json != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mvc.perform(req);
    }

    private String register(String username, String inviteCode) throws Exception {
        String code = inviteCode == null ? "null" : "\"" + inviteCode + "\"";
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","email":"%s@example.com","password":"password123","inviteCode":%s}"""
                                .formatted(username, username, code)))
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
}
