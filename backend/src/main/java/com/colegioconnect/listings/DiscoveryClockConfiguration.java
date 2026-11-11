package com.colegioconnect.listings;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class DiscoveryClockConfiguration {
    @Bean Clock discoveryClock() { return Clock.systemUTC(); }
}
