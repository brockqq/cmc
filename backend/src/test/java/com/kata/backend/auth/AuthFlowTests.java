package com.kata.backend.auth;

import com.jayway.jsonpath.JsonPath;
import com.kata.backend.ApiTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTests extends ApiTestSupport {

    @Autowired
    AuthService authService;

    @Autowired
    TransactionTemplate transactions;

    @Test
    void registerLoginAndFetchCurrentUser() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"alice@example.com","password":"password123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.password").doesNotExist());

        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","password":"password123"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void rejectsWrongPasswordAndMissingToken() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","email":"bob@example.com","password":"password123"}"""))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"bob","password":"wrong-password"}"""))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateUsernameAndInvalidInput() throws Exception {
        String carol = """
                {"username":"carol","email":"carol@example.com","password":"password123"}""";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(carol))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(carol))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"x","email":"not-an-email","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void usernamesAndEmailsAreCaseInsensitive() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"Dave","email":"Dave@Example.com","password":"password123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("Dave"))
                .andExpect(jsonPath("$.email").value("dave@example.com"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"dAVE","email":"other@example.com","password":"password123"}"""))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"dave2","email":"DAVE@example.COM","password":"password123"}"""))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"ADMIN","email":"fake-admin@example.com","password":"password123"}"""))
                .andExpect(status().isConflict());

        // Logging in with another spelling yields a token for the stored account
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"dave","password":"password123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("Dave"));
    }

    @Test
    void twoRegistrationsRacingForTheSameUsernameGiveOne409NotA500() throws Exception {
        CountDownLatch registered = new CountDownLatch(1);
        CountDownLatch commit = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            // First registration has inserted "Racer" but not committed yet, so the second one's check passes
            Future<?> first = pool.submit(() -> transactions.executeWithoutResult(tx -> {
                authService.register(new RegisterRequest("Racer", "racer1@example.com", "password123", null));
                registered.countDown();
                try {
                    commit.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
            assertThat(registered.await(5, TimeUnit.SECONDS)).isTrue();
            Future<Integer> second = pool.submit(() -> mvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"username":"racer","email":"racer2@example.com","password":"password123"}"""))
                    .andReturn().getResponse().getStatus());
            Thread.sleep(300);
            commit.countDown();
            first.get(5, TimeUnit.SECONDS);
            // The database's case-insensitive unique key catches it, answered as a conflict
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo(409);
        } finally {
            commit.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void repeatedWrongPasswordsAreRateLimited() throws Exception {
        register("frank", null);
        String wrong = """
                {"username":"frank","password":"wrong-password"}""";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .andExpect(status().isUnauthorized());
        }
        String right = """
                {"username":"frank","password":"password123"}""";
        // Even the right password is refused from this IP until the window passes
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(right))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("分鐘後再試")));
        // ...but the account itself is not locked for everyone
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(right)
                        .with(req -> {
                            req.setRemoteAddr("203.0.113.7");
                            return req;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    void passwordsAreLimitedToBcrypts72Bytes() throws Exception {
        String chinese30 = "密".repeat(30); // 30 characters, 90 bytes
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"erin","email":"erin@example.com","password":"%s"}""".formatted(chinese30)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        String chinese24 = "密".repeat(24); // exactly 72 bytes
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"erin","email":"erin@example.com","password":"%s"}""".formatted(chinese24)))
                .andExpect(status().isCreated());

        // An over-long password at login is just a wrong password, not a server error
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"username":"erin","password":"%s"}""".formatted(chinese30)))
                .andExpect(status().isUnauthorized());
    }
}
