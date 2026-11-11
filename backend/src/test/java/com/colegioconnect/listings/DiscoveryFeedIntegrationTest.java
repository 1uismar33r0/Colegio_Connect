package com.colegioconnect.listings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.colegioconnect.TestcontainersConfiguration;
import com.colegioconnect.accounts.entity.AccountType;
import com.colegioconnect.accounts.entity.User;
import com.colegioconnect.accounts.entity.UserStatus;
import com.colegioconnect.organizations.entity.ApprovalStatus;
import com.colegioconnect.organizations.entity.OrgType;
import com.colegioconnect.organizations.entity.Organization;
import com.colegioconnect.organizations.entity.Role;
import com.colegioconnect.security.NegativeAuthorizationTestHarness;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, DiscoveryFeedIntegrationTest.FixedTime.class})
class DiscoveryFeedIntegrationTest extends NegativeAuthorizationTestHarness {
    private static final Instant NOW = Instant.parse("2026-11-09T00:00:00Z");
    private static final Instant POSTED = Instant.parse("2026-11-08T00:00:00Z");
    private static final ObjectMapper JSON = new ObjectMapper();
    @Autowired private JdbcTemplate jdbc;
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock fixedDiscoveryClock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }
    @AfterEach void clearListings() {
        jdbc.update("DELETE FROM listings");
        jdbc.update("DELETE FROM capstone_teams");
    }
    @Test void emptyAndDenied() throws Exception {
        User reader = activeVerifiedUser("feed.empty@upr.edu", Role.STUDENT);
        var session = login(reader, status().isOk());
        mockMvc.perform(get("/api/listings/feed").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/listings/feed")).andExpect(status().isUnauthorized());
        reader.setStatus(UserStatus.SUSPENDED);
        userRepository.saveAndFlush(reader);
        mockMvc.perform(get("/api/listings/feed").session(session)).andExpect(status().isForbidden());
    }
    @Test void approvedEighteenCaseMatrixAndPuertoRicoBoundary() throws Exception {
        User reader = activeVerifiedUser("feed.matrix.reader@upr.edu", Role.STUDENT);
        Organization alpha = organization("Alpha office", OrgType.UPRM_OFFICE, ApprovalStatus.APPROVED);
        Organization beta = organization("Beta office", OrgType.UPRM_DEPARTMENT, ApprovalStatus.APPROVED);
        Organization company = organization("Gamma company", OrgType.EXTERNAL_COMPANY, ApprovalStatus.APPROVED);
        User a = activeVerifiedUser("feed.alpha@upr.edu"); assign(a, Role.INSTITUTIONAL, alpha);
        User b = activeVerifiedUser("feed.beta@upr.edu"); assign(b, Role.INSTITUTIONAL, beta);
        User c = external("feed.gamma@example.com"); assign(c, Role.COMPANY, company);
        User owner = activeVerifiedUser("feed.team@upr.edu", Role.STUDENT);
        UUID team = UUID.randomUUID();
        jdbc.update("INSERT INTO capstone_teams (id, name, owner_user_id) VALUES (?, 'Quantum team', ?)", team, owner.getId());
        UUID[] id = new UUID[8]; for (int i=1;i<=7;i++) id[i]=new UUID(0,i);
        add(id[1],a,alpha.getId(),null,"Quantum job","JOB","VERIFIED_UPRM_INSTITUTION","PUBLISHED","2026-11-11T03:59:59Z");
        add(id[2],a,alpha.getId(),null,"Internship","INTERNSHIP","VERIFIED_UPRM_INSTITUTION","PUBLISHED","2026-11-11T04:00:00Z");
        add(id[3],b,beta.getId(),null,"Marine co-op","COOP","VERIFIED_UPRM_INSTITUTION","PUBLISHED","2026-11-12T00:00:00Z");
        add(id[4],c,company.getId(),null,"Quantum research","RESEARCH_POSITION","APPROVED_EXTERNAL_COMPANY","PUBLISHED","2026-11-10T12:00:00Z");
        add(id[5],c,company.getId(),null,"Marine participation","RESEARCH_PARTICIPATION","APPROVED_EXTERNAL_COMPANY","PUBLISHED","2026-11-11T03:00:00Z");
        add(id[6],owner,null,team,"Team capstone","CAPSTONE","VERIFIED_STUDENT_PROJECT","PUBLISHED","2026-11-10T18:00:00Z");
        add(id[7],c,company.getId(),null,"Company job","JOB","APPROVED_EXTERNAL_COMPANY","PUBLISHED","2026-11-12T00:00:00Z");
        var session=login(reader,status().isOk());
        record Case(String query,int[] expected) {}
        List<Case> cases=List.of(
            new Case("category=JOB",new int[]{7,1}),
            new Case("category=INTERNSHIP",new int[]{2}),
            new Case("category=COOP",new int[]{3}),
            new Case("category=RESEARCH_POSITION",new int[]{4}),
            new Case("category=RESEARCH_PARTICIPATION",new int[]{5}),
            new Case("category=CAPSTONE",new int[]{6}),
            new Case("sourceLabel=VERIFIED_UPRM_INSTITUTION",new int[]{3,2,1}),
            new Case("sourceLabel=APPROVED_EXTERNAL_COMPANY",new int[]{7,5,4}),
            new Case("sourceLabel=VERIFIED_STUDENT_PROJECT",new int[]{6}),
            new Case("organizationId="+alpha.getId(),new int[]{2,1}),
            new Case("organizationId="+beta.getId(),new int[]{3}),
            new Case("deadlineOnOrBefore=2026-11-10",new int[]{6,5,4,1}),
            new Case("deadlineOnOrBefore=2026-11-11",new int[]{7,6,5,4,3,2,1}),
            new Case("keyword=quantum",new int[]{4,1}),
            new Case("keyword=marine",new int[]{5,3}),
            new Case("category=JOB&sourceLabel=APPROVED_EXTERNAL_COMPANY",new int[]{7}),
            new Case("organizationId="+alpha.getId()+"&deadlineOnOrBefore=2026-11-10",new int[]{1}),
            new Case("keyword=marine&category=RESEARCH_PARTICIPATION&sourceLabel=APPROVED_EXTERNAL_COMPANY",new int[]{5}));
        assertThat(cases).hasSize(18);
        for (Case test:cases) {
            String body=mockMvc.perform(get("/api/listings/feed?"+test.query()).session(session))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            List<String> actual=StreamSupport.stream(JSON.readTree(body).spliterator(),false)
                    .map(item->item.path("id").asText()).toList();
            List<String> expected=Arrays.stream(test.expected()).mapToObj(n->id[n].toString()).toList();
            assertThat(actual).as(test.query()).containsExactlyElementsOf(expected);
        }
        mockMvc.perform(get("/api/listings/feed?sourceLabel=VERIFIED_STUDENT_PROJECT").session(session))
                .andExpect(jsonPath("$[0].source").value("Quantum team"))
                .andExpect(jsonPath("$[0].sourceLabel").value("Verified student project"));
        for(String zone:List.of("UTC","Pacific/Honolulu")) {
            TimeZone previous=TimeZone.getDefault();
            try {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                String body=mockMvc.perform(get("/api/listings/feed?deadlineOnOrBefore=2026-11-10").session(session))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                List<String> result=StreamSupport.stream(JSON.readTree(body).spliterator(),false)
                        .map(item->item.path("id").asText()).toList();
                assertThat(result).contains(id[1].toString()).doesNotContain(id[2].toString());
            } finally { TimeZone.setDefault(previous); }
        }
        mockMvc.perform(get("/api/listings/feed?category=JOB&size=1&page=0").session(session))
                .andExpect(jsonPath("$[0].id").value(id[7].toString()));
        mockMvc.perform(get("/api/listings/feed?category=JOB&size=1&page=1").session(session))
                .andExpect(jsonPath("$[0].id").value(id[1].toString()));
    }
    @Test void aliasPrivacyEligibilityAndConstraints() throws Exception {
        User reader=activeVerifiedUser("feed.rules.reader@upr.edu",Role.STUDENT);
        Organization office=organization("Rules office",OrgType.UPRM_OFFICE,ApprovalStatus.APPROVED);
        User publisher=activeVerifiedUser("feed.rules.publisher@upr.edu"); assign(publisher,Role.INSTITUTIONAL,office);
        UUID visible=UUID.randomUUID();
        add(visible,publisher,office.getId(),null,"Visible job","JOB","VERIFIED_UPRM_INSTITUTION","PUBLISHED","2026-11-10T00:00:00Z");
        add(UUID.randomUUID(),publisher,office.getId(),null,"Hidden job","JOB","VERIFIED_UPRM_INSTITUTION","HIDDEN","2026-11-10T00:00:00Z");
        add(UUID.randomUUID(),publisher,office.getId(),null,"Rejected job","JOB","VERIFIED_UPRM_INSTITUTION","REJECTED","2026-11-10T00:00:00Z");
        add(UUID.randomUUID(),publisher,office.getId(),null,"Expired job","JOB","VERIFIED_UPRM_INSTITUTION","PUBLISHED","2026-11-09T00:00:00Z");
        Organization pending=organization("Pending company",OrgType.EXTERNAL_COMPANY,ApprovalStatus.PENDING);
        User external=external("feed.pending@example.com"); assign(external,Role.COMPANY,pending);
        add(UUID.randomUUID(),external,pending.getId(),null,"Pending job","JOB","APPROVED_EXTERNAL_COMPANY","PUBLISHED","2026-11-10T00:00:00Z");
        var session=login(reader,status().isOk());
        String body=mockMvc.perform(get("/api/listings/feed?type=JOB&category=Job").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(visible.toString()))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("passwordHash","publisherUserId","@upr.edu","@example.com");
        mockMvc.perform(get("/api/listings/feed?type=JOB").session(session))
                .andExpect(jsonPath("$.length()").value(1));
        for(String query:List.of("category=JOB&type=COOP","category=Event","type=Event",
                "field=Computing","deadlineOnOrBefore=2026-02-30","sourceLabel=unknown","size=101"))
            mockMvc.perform(get("/api/listings/feed?"+query).session(session))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        UUID team=UUID.randomUUID();
        jdbc.update("INSERT INTO capstone_teams (id,name,owner_user_id) VALUES (?, 'Rules team', ?)",team,reader.getId());
        assertThatThrownBy(()->add(UUID.randomUUID(),reader,office.getId(),team,"Bad owner","CAPSTONE",
                "VERIFIED_STUDENT_PROJECT","PUBLISHED","2026-11-10T00:00:00Z"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->add(UUID.randomUUID(),reader,null,team,"Bad type","JOB",
                "VERIFIED_STUDENT_PROJECT","PUBLISHED","2026-11-10T00:00:00Z"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->add(UUID.randomUUID(),publisher,UUID.randomUUID(),null,"Unknown org","JOB",
                "VERIFIED_UPRM_INSTITUTION","PUBLISHED","2026-11-10T00:00:00Z"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        publisher.setStatus(UserStatus.SUSPENDED); userRepository.saveAndFlush(publisher);
        mockMvc.perform(get("/api/listings/feed").session(session))
                .andExpect(jsonPath("$.length()").value(0));
    }
    private User external(String email) {
        User user=new User(email,passwordEncoder.encode(TEST_PASSWORD),AccountType.EXTERNAL_ORGANIZATION);
        user.setEmailVerified(true); return userRepository.save(user);
    }
    private void add(UUID id,User publisher,UUID org,UUID team,String title,String category,
            String label,String state,String closes) {
        jdbc.update("INSERT INTO listings (id,publisher_user_id,organization_id,capstone_team_id,title,summary,"
                + "opportunity_type,trust_label,status,posted_at,closes_at) VALUES (?,?,?,?,?,"
                + "'Synthetic summary',?,?,?,?,?)", id,publisher.getId(),org,team,title,category,label,state,
                "DRAFT".equals(state)?null:Timestamp.from(POSTED),Timestamp.from(Instant.parse(closes)));
    }
}
