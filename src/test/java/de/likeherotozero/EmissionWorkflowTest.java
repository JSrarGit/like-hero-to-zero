package de.likeherotozero;

import de.likeherotozero.data.CountryRepository;
import de.likeherotozero.data.EmissionRepository;
import de.likeherotozero.data.Origin;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:emission-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.data.csv=src/test/resources/emissions.csv",
        "app.research.password=fixture-only-password"
})
@AutoConfigureMockMvc
class EmissionWorkflowTest {
    @Autowired MockMvc mvc;
    @Autowired CountryRepository countries;
    @Autowired EmissionRepository emissions;

    @Test
    void showsTheLatestAvailableValueForEachCountry() throws Exception {
        mvc.perform(get("/").param("land", "DEU"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("572,319175")))
                .andExpect(content().string(containsString("<dd>2024</dd>")));
        mvc.perform(get("/").param("land", "BRA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("480,123456")))
                .andExpect(content().string(containsString("<dd>2023</dd>")));
        assertThat(emissions.findByCountry_CodeAndYear("DEU", 2025)).isEmpty();
    }

    @Test
    void leavesAggregatesAndRowsWithoutCountryCodesOutOfTheCountryList() throws Exception {
        assertThat(countries.count()).isEqualTo(3);
        mvc.perform(get("/").param("land", "WLD"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Kein Jahreswert")));
    }

    @Test
    void logsInTheConfiguredResearchAccount() throws Exception {
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(formLogin().user("forschung").password("fixture-only-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/forschung"));
    }

    @Test
    void requiresLoginBeforeWriting() throws Exception {
        mvc.perform(post("/forschung/emissionen").with(csrf())
                        .param("country", "DEU").param("year", "2025").param("amount", "602.5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        assertThat(emissions.findByCountry_CodeAndYear("DEU", 2025)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "READER")
    void requiresTheResearchRoleBeforeWriting() throws Exception {
        mvc.perform(post("/forschung/emissionen").with(csrf())
                        .param("country", "DEU").param("year", "2025").param("amount", "602.5"))
                .andExpect(status().isForbidden());
        assertThat(emissions.findByCountry_CodeAndYear("DEU", 2025)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "SCIENTIST")
    void requiresAnExplicitCountryForNewData() throws Exception {
        mvc.perform(get("/forschung"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<option value=\"\">Bitte auswählen</option>")));
        var initialCount = emissions.count();
        mvc.perform(post("/forschung/emissionen").with(csrf())
                        .param("country", "").param("year", "2025").param("amount", "8.234567"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("entry", "country"));
        assertThat(emissions.count()).isEqualTo(initialCount);
    }

    @Test
    @WithMockUser(roles = "SCIENTIST")
    void savesNewDataAndShowsItInThePublicView() throws Exception {
        var saved = mvc.perform(post("/forschung/emissionen").with(csrf())
                        .param("country", "AFG").param("year", "2025").param("amount", "8.234567"))
                .andExpect(redirectedUrl("/forschung?land=AFG&jahr=2025"))
                .andReturn();
        mvc.perform(get(saved.getResponse().getRedirectedUrl()).flashAttrs(saved.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Afghanistan · 2025")));
        mvc.perform(get("/").param("land", "AFG"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("8,234567")))
                .andExpect(content().string(containsString("Lokaler Beitrag")));
    }

    @Test
    @WithMockUser(roles = "SCIENTIST")
    void rejectsDuplicateYearsAndInvalidAmounts() throws Exception {
        mvc.perform(post("/forschung/emissionen").with(csrf())
                        .param("country", "DEU").param("year", "2024").param("amount", "1.0"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrorCode("entry", "year", "year.exists"));
        mvc.perform(post("/forschung/emissionen").with(csrf())
                        .param("country", "DEU").param("year", "2025").param("amount", "-1.0"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("entry", "amount"))
                .andExpect(content().string(containsString("value=\"DEU\" selected=\"selected\"")));
        assertThat(emissions.findByCountry_CodeAndYear("DEU", 2024).orElseThrow().getAmount())
                .isEqualByComparingTo("572.319175");
        assertThat(emissions.findByCountry_CodeAndYear("DEU", 2025)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "SCIENTIST")
    void correctsAnExistingValueAndRejectsAStaleForm() throws Exception {
        var original = emissions.findByCountry_CodeAndYear("DEU", 2019).orElseThrow();
        mvc.perform(get("/forschung/bearbeiten").param("land", "DEU").param("jahr", "2019"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Korrektur speichern")));
        var saved = mvc.perform(post("/forschung/emissionen/DEU/2019").with(csrf())
                        .param("amount", "699.125").param("version", original.getVersion().toString()))
                .andExpect(redirectedUrl("/forschung?land=DEU&jahr=2019"))
                .andReturn();
        mvc.perform(get(saved.getResponse().getRedirectedUrl()).flashAttrs(saved.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Deutschland · 2019")))
                .andExpect(content().string(containsString("href=\"/?land=DEU\">Jüngsten Wert anzeigen</a>")));
        mvc.perform(get("/").param("land", "DEU"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<dd>2024</dd>")));
        mvc.perform(post("/forschung/emissionen/DEU/2019").with(csrf())
                        .param("amount", "900.00").param("version", original.getVersion().toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Der Wert wurde inzwischen geändert")));
        var corrected = emissions.findByCountry_CodeAndYear("DEU", 2019).orElseThrow();
        assertThat(corrected.getOrigin()).isEqualTo(Origin.LOCAL_CORRECTION);
        assertThat(corrected.getAmount()).isEqualByComparingTo("699.125");
    }
}
