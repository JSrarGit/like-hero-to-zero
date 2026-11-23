package de.likeherotozero.web;

import de.likeherotozero.data.EmissionRecord;
import de.likeherotozero.service.EmissionService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Year;
import java.util.Optional;

@Controller
@RequestMapping("/forschung")
public class ResearchController {
    private final EmissionService service;

    public ResearchController(EmissionService service) {
        this.service = service;
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("entry", new EmissionForm());
        return showWorkspace(model);
    }

    @PostMapping("/emissionen")
    public String add(@Valid @ModelAttribute("entry") EmissionForm form, BindingResult validation,
                      Model model, RedirectAttributes redirect) {
        if (!validation.hasFieldErrors("year") && form.getYear() > Year.now().getValue()) {
            validation.rejectValue("year", "year.future", "Das Bezugsjahr darf nicht in der Zukunft liegen.");
        }
        if (!validation.hasFieldErrors("country") && !service.containsCountry(form.getCountry())) {
            validation.rejectValue("country", "country.unknown", "Wähle ein Land oder Gebiet aus der Liste.");
        }
        if (validation.hasErrors()) {
            return showWorkspace(model);
        }

        try {
            if (!service.add(form.getCountry(), form.getYear(), form.getAmount())) {
                validation.rejectValue("year", "year.exists", "Für dieses Land gibt es bereits einen Wert für das Jahr.");
                return showWorkspace(model);
            }
        } catch (DataIntegrityViolationException conflict) {
            validation.reject("save.conflict", "Der Wert konnte nicht gespeichert werden. Prüfe das Land und das Jahr.");
            return showWorkspace(model);
        }
        return saved(redirect, form.getCountry(), form.getYear(), "Der neue Jahreswert wurde gespeichert.");
    }

    @GetMapping("/bearbeiten")
    public String edit(@RequestParam("land") String countryCode, @RequestParam("jahr") int year,
                       Model model, RedirectAttributes redirect) {
        Optional<EmissionRecord> result = service.find(countryCode, year);
        if (result.isEmpty()) {
            return missing(redirect);
        }
        EmissionRecord record = result.get();
        model.addAttribute("record", record);
        model.addAttribute("correction", new CorrectionForm(record.getAmount(), record.getVersion()));
        return "edit";
    }

    @PostMapping("/emissionen/{code}/{year}")
    public String update(@PathVariable("code") String countryCode, @PathVariable("year") int year,
                         @Valid @ModelAttribute("correction") CorrectionForm form, BindingResult validation,
                         Model model, RedirectAttributes redirect) {
        if (!validation.hasErrors()) {
            try {
                if (service.correct(countryCode, year, form.getAmount(), form.getVersion())) {
                    return saved(redirect, countryCode, year, "Die Korrektur wurde gespeichert.");
                }
                return missing(redirect);
            } catch (OptimisticLockingFailureException conflict) {
                validation.reject("revision.conflict", "Der Wert wurde inzwischen geändert. Öffne den aktuellen Stand.");
            }
        }
        Optional<EmissionRecord> result = service.find(countryCode, year);
        if (result.isEmpty()) {
            return missing(redirect);
        }
        model.addAttribute("record", result.get());
        return "edit";
    }

    private String showWorkspace(Model model) {
        var countries = service.countries();
        model.addAttribute("countries", countries);
        if (model.getAttribute("savedCountry") instanceof String countryCode) {
            countries.stream().filter(country -> country.getCode().equals(countryCode)).findFirst()
                    .ifPresent(country -> model.addAttribute("savedCountryName", country.getDisplayName()));
        }
        model.addAttribute("currentYear", Year.now().getValue());
        return "research";
    }

    private String saved(RedirectAttributes redirect, String countryCode, int year, String message) {
        redirect.addAttribute("land", countryCode);
        redirect.addAttribute("jahr", year);
        redirect.addFlashAttribute("message", message);
        redirect.addFlashAttribute("savedCountry", countryCode);
        redirect.addFlashAttribute("savedYear", year);
        return "redirect:/forschung";
    }

    private String missing(RedirectAttributes redirect) {
        redirect.addFlashAttribute("lookupError", "Für diese Kombination aus Land und Jahr gibt es keinen Wert.");
        return "redirect:/forschung";
    }
}
