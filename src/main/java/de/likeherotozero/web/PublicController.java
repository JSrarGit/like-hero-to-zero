package de.likeherotozero.web;

import de.likeherotozero.data.Country;
import de.likeherotozero.data.EmissionRecord;
import de.likeherotozero.service.EmissionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Controller
public class PublicController {
    private final EmissionService service;

    public PublicController(EmissionService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String home(@RequestParam(name = "land", required = false) String countryCode,
                       Principal principal, Model model) {
        List<Country> countries = service.countries();
        String requestedCode = countryCode == null ? "DEU" : countryCode;
        Optional<Country> selected = countries.stream()
                .filter(country -> country.getCode().equals(requestedCode)).findFirst();
        if (countryCode == null && selected.isEmpty()) {
            selected = countries.stream().findFirst();
        }

        Optional<EmissionRecord> latest = selected.flatMap(country -> service.latest(country.getCode()));
        NumberFormat number = NumberFormat.getNumberInstance(Locale.GERMANY);
        number.setMinimumFractionDigits(2);
        number.setMaximumFractionDigits(6);

        model.addAttribute("countries", countries);
        model.addAttribute("selected", selected.orElse(null));
        model.addAttribute("unknownCountry", countryCode != null && selected.isEmpty());
        model.addAttribute("latest", latest.orElse(null));
        model.addAttribute("formattedAmount", latest.map(value -> number.format(value.getAmount())).orElse(""));
        model.addAttribute("signedIn", principal != null);
        return "home";
    }

    @GetMapping("/login")
    public String login(Principal principal) {
        return principal == null ? "login" : "redirect:/forschung";
    }
}
