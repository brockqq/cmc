package com.kata.backend.config;

import com.kata.backend.BackendApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Outside the dev profile the app must not start with guessable secrets, and the H2 console is off. */
class ProductionConfigTests {

    static final String SECRET = "cHJvZC10ZXN0LXNlY3JldC1rZXktdGhhdC1pcy0zMi1ieXRlcyE=";

    /** Starts the app with the prod profile; each run gets its own in-memory database. */
    static ConfigurableApplicationContext start(String... properties) {
        List<String> args = new ArrayList<>(List.of(
                "--server.port=0",
                "--spring.datasource.url=jdbc:h2:mem:prod-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1"));
        for (String p : properties) {
            args.add("--" + p);
        }
        return new SpringApplicationBuilder(BackendApplication.class).profiles("prod")
                .run(args.toArray(String[]::new));
    }

    /** The startup error may be wrapped (bean creation) or not (application runner). */
    static void assertStartupFails(Runnable startup, String message) {
        assertThatThrownBy(startup::run).satisfies(e -> assertThat(NestedExceptionUtils.getMostSpecificCause(e))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(message));
    }

    @Test
    void refusesToStartWithoutAJwtSecret() {
        assertStartupFails(() -> start("app.admin.password=a-strong-password"), "APP_JWT_SECRET");
    }

    @Test
    void refusesToStartWithAShortJwtSecret() {
        assertStartupFails(() -> start("app.jwt.secret=c2hvcnQ=", "app.admin.password=a-strong-password"), "太短");
    }

    @Test
    void refusesToCreateTheAdminWithoutAPassword() {
        assertStartupFails(() -> start("app.jwt.secret=" + SECRET), "APP_ADMIN_PASSWORD");
    }

    @Test
    void startsWithSecretsAndKeepsTheH2ConsoleClosed() throws Exception {
        try (ConfigurableApplicationContext ctx = start("app.jwt.secret=" + SECRET,
                "app.admin.password=a-strong-password")) {
            assertThat(ctx.getEnvironment().getProperty("spring.h2.console.enabled")).isNull();
            MockMvc mvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) ctx).apply(springSecurity()).build();
            mvc.perform(get("/h2-console")).andExpect(status().isUnauthorized());
        }
    }
}
