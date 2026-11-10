package com.colegioconnect.roles;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapService bootstrap;
    private final String email;

    public AdminBootstrapRunner(AdminBootstrapService bootstrap,
                                @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:}") String email) {
        this.bootstrap = bootstrap;
        this.email = email;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrap.bootstrap(email);
    }
}
