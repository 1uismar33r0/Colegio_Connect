package com.colegioconnect;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import com.colegioconnect.notifications.EmailSender;
import com.colegioconnect.notifications.SmtpEmailSender;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ColegioConnectApplicationTests {

    @Autowired private ApplicationContext context;

    @Test
    void contextLoads() {
    }

    @Test
    void defaultMailProviderIsSmtp() {
        assertThat(context.getBeansOfType(EmailSender.class).values())
                .singleElement()
                .isInstanceOf(SmtpEmailSender.class);
    }
}
