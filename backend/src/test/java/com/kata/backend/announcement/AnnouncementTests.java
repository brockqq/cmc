package com.kata.backend.announcement;

import com.kata.backend.ApiTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnnouncementTests extends ApiTestSupport {

    String admin;
    Created a;
    Created b;

    /** Each test gets its own two communities with a manager and a resident each. */
    @BeforeEach
    void setUp() throws Exception {
        String s = String.valueOf(System.nanoTime()).substring(8);
        admin = adminToken();
        a = createCommunity(admin, "公告A" + s);
        b = createCommunity(admin, "公告B" + s);
        register("amgr" + s, null);
        makeManager(admin, a, "amgr" + s);
        register("ares" + s, a.inviteCode());
        register("bres" + s, b.inviteCode());
        suffix = s;
    }

    String suffix;

    @Test
    void residentsSeePlatformWideAndOwnCommunityOnly() throws Exception {
        String mgrA = login("amgr" + suffix);
        String resA = login("ares" + suffix);
        String resB = login("bres" + suffix);

        String platformTitle = "全平台公告" + suffix;
        call(admin, post("/api/announcements"), """
                {"communityId":null,"title":"%s","content":"大家好","pinned":false}""".formatted(platformTitle))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.communityName").doesNotExist());
        call(mgrA, post("/api/announcements"), """
                {"communityId":%s,"title":"A停水通知%s","content":"週三停水","pinned":true}""".formatted(a.id(), suffix))
                .andExpect(status().isCreated());
        call(admin, post("/api/announcements"), """
                {"communityId":%s,"title":"B公告%s","content":"B only","pinned":false}""".formatted(b.id(), suffix))
                .andExpect(status().isCreated());

        List<String> seenByA = titles(call(resA, get("/api/announcements"), null).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        assertThat(seenByA).contains(platformTitle, "A停水通知" + suffix).doesNotContain("B公告" + suffix);
        // Pinned announcements come first
        assertThat(seenByA.getFirst()).isEqualTo("A停水通知" + suffix);

        List<String> seenByB = titles(call(resB, get("/api/announcements"), null)
                .andReturn().getResponse().getContentAsString());
        assertThat(seenByB).contains(platformTitle, "B公告" + suffix).doesNotContain("A停水通知" + suffix);

        // A community the user has not joined looks like it does not exist
        call(resA, get("/api/announcements").param("communityId", b.id()), null).andExpect(status().isNotFound());
        call(resA, get("/api/announcements").param("communityId", a.id()), null)
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void onlyManagersPostToTheirCommunityAndOnlyAdminsPostPlatformWide() throws Exception {
        String mgrA = login("amgr" + suffix);
        String resA = login("ares" + suffix);

        call(resA, post("/api/announcements"), """
                {"communityId":%s,"title":"t","content":"c"}""".formatted(a.id()))
                .andExpect(status().isForbidden());
        call(mgrA, post("/api/announcements"), """
                {"communityId":%s,"title":"t","content":"c"}""".formatted(b.id()))
                .andExpect(status().isNotFound());
        call(mgrA, post("/api/announcements"), """
                {"communityId":null,"title":"t","content":"c"}""")
                .andExpect(status().isForbidden());
        call(mgrA, post("/api/announcements"), """
                {"communityId":%s,"title":"","content":""}""".formatted(a.id()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());
    }

    @Test
    void editAndDeleteRequireManagePermission() throws Exception {
        String mgrA = login("amgr" + suffix);
        String resA = login("ares" + suffix);
        String resB = login("bres" + suffix);

        String id = idOf(call(mgrA, post("/api/announcements"), """
                {"communityId":%s,"title":"原標題","content":"內容"}""".formatted(a.id())));

        call(resA, get("/api/announcements").param("communityId", a.id()), null)
                .andExpect(jsonPath("$.items[0].canEdit").value(false));
        call(resA, put("/api/announcements/{id}", id), """
                {"title":"改","content":"改"}""").andExpect(status().isForbidden());
        // Other communities' announcements are invisible, not just read-only
        call(resB, delete("/api/announcements/{id}", id), null).andExpect(status().isNotFound());

        call(mgrA, put("/api/announcements/{id}", id), """
                {"title":"新標題","content":"新內容","pinned":true}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("新標題"))
                .andExpect(jsonPath("$.pinned").value(true));
        // Platform admin can manage any community's announcements
        call(admin, delete("/api/announcements/{id}", id), null).andExpect(status().isNoContent());
    }

    @Test
    void paginatesWithPinnedFirstAcrossPages() throws Exception {
        String mgrA = login("amgr" + suffix);
        for (int i = 1; i <= 23; i++) {
            call(mgrA, post("/api/announcements"), """
                    {"communityId":%s,"title":"第%d則","content":"c","pinned":%s}"""
                    .formatted(a.id(), i, i == 1))
                    .andExpect(status().isCreated());
        }

        call(mgrA, get("/api/announcements").param("communityId", a.id()).param("size", "10"), null)
                .andExpect(jsonPath("$.totalItems").value(23))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.items.length()").value(10))
                // The oldest one is pinned, so it still leads page 1; the rest are newest first
                .andExpect(jsonPath("$.items[0].title").value("第1則"))
                .andExpect(jsonPath("$.items[1].title").value("第23則"));

        call(mgrA, get("/api/announcements").param("communityId", a.id()).param("size", "10").param("page", "2"), null)
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[2].title").value("第2則"));

        // Past the last page: empty, not an error
        call(mgrA, get("/api/announcements").param("communityId", a.id()).param("page", "9"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));

        call(mgrA, get("/api/announcements").param("size", "0"), null).andExpect(status().isBadRequest());
        call(mgrA, get("/api/announcements").param("size", "101"), null).andExpect(status().isBadRequest());
        call(mgrA, get("/api/announcements").param("page", "-1"), null).andExpect(status().isBadRequest());
    }

    @Test
    void platformFilterReturnsOnlyPlatformWide() throws Exception {
        String mgrA = login("amgr" + suffix);
        call(admin, post("/api/announcements"), """
                {"communityId":null,"title":"平台限定%s","content":"c"}""".formatted(suffix));
        call(mgrA, post("/api/announcements"), """
                {"communityId":%s,"title":"社區限定%s","content":"c"}""".formatted(a.id(), suffix));

        List<String> platform = titles(call(login("ares" + suffix), get("/api/announcements")
                .param("platform", "true").param("size", "100"), null)
                .andReturn().getResponse().getContentAsString());
        assertThat(platform).contains("平台限定" + suffix).doesNotContain("社區限定" + suffix);

        call(mgrA, get("/api/announcements").param("platform", "true").param("communityId", a.id()), null)
                .andExpect(status().isBadRequest());
    }

    private static List<String> titles(String json) {
        return com.jayway.jsonpath.JsonPath.read(json, "$.items[*].title");
    }
}
