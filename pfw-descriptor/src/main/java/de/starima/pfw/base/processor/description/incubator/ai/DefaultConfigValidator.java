package de.starima.pfw.base.processor.description.incubator.ai;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.api.IAIConfigValidator;
import de.starima.pfw.base.processor.description.incubator.ai.api.IProcessorCatalog;
import de.starima.pfw.base.processor.description.incubator.ai.domain.BlueprintDraft;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ProcessorBlueprint;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ValidationReport;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ValidationReport.ValidationIssue;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Standard-Implementierung des {@link IAIConfigValidator}.
 *
 * <p>Prüft einen {@link BlueprintDraft} gegen den Prozessor-Katalog:
 * <ul>
 *   <li>Fehlende Pflichtparameter → ERROR</li>
 *   <li>Unbekannte Prozessor-IDs → WARNING</li>
 *   <li>Leere beanParameterMap → WARNING</li>
 *   <li>Fehlende rootProcessorId → ERROR</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Validiert KI-generierte Konfigurationsentwürfe gegen den Prozessor-Katalog. " +
                "Findet fehlende Pflichtparameter, unbekannte Prozessor-IDs und andere Inkonsistenzen.",
        categories = {"ai", "validation"},
        tags = {"validate", "constraint", "check", "blueprint"}
)
public class DefaultConfigValidator extends AbstractProcessor implements IAIConfigValidator {

    @ProcessorParameter(description = "Ob unbekannte Prozessor-IDs als ERROR (true) oder WARNING (false) gemeldet werden.",
            value = "false")
    private boolean strictMode = false;

    @Override
    public ValidationReport validate(BlueprintDraft draft, IProcessorCatalog catalog) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (draft == null) {
            issues.add(ValidationIssue.builder()
                    .severity(ValidationIssue.Severity.ERROR)
                    .parameterPath("(root)")
                    .message("BlueprintDraft ist null.")
                    .build());
            return ValidationReport.builder().valid(false).issues(issues).build();
        }

        // 1. rootProcessorId vorhanden?
        if (draft.getRootProcessorId() == null || draft.getRootProcessorId().isBlank()) {
            issues.add(ValidationIssue.builder()
                    .severity(ValidationIssue.Severity.ERROR)
                    .parameterPath("rootProcessorId")
                    .message("Keine rootProcessorId definiert.")
                    .suggestion("Setze rootProcessorId auf die ID des Hauptprozessors.")
                    .build());
        }

        // 2. beanParameterMap vorhanden?
        Map<String, Map<String, Object>> bpm = draft.getBeanParameterMap();
        if (bpm == null || bpm.isEmpty()) {
            issues.add(ValidationIssue.builder()
                    .severity(ValidationIssue.Severity.WARNING)
                    .parameterPath("beanParameterMap")
                    .message("Die beanParameterMap ist leer — kein Prozessor wurde konfiguriert.")
                    .build());
        } else {
            // 3. Pro Prozessor: Pflichtparameter prüfen
            for (Map.Entry<String, Map<String, Object>> beanEntry : bpm.entrySet()) {
                String beanId = beanEntry.getKey();
                Map<String, Object> params = beanEntry.getValue();

                // Aus beanId die prototypeId extrahieren (Format: "prototypeId[:instance][@scope]")
                String prototypeId = extractPrototypeId(beanId);

                if (catalog != null) {
                    ProcessorBlueprint blueprint = catalog.getBlueprint(prototypeId);
                    if (blueprint == null) {
                        ValidationIssue.Severity severity = strictMode
                                ? ValidationIssue.Severity.ERROR
                                : ValidationIssue.Severity.WARNING;
                        issues.add(ValidationIssue.builder()
                                .severity(severity)
                                .parameterPath(beanId)
                                .message("Prozessor '" + prototypeId + "' nicht im Katalog gefunden.")
                                .suggestion("Prüfe ob der Prozessor korrekt registriert ist.")
                                .build());
                    } else {
                        // Pflichtparameter prüfen
                        validateRequiredParameters(beanId, params, blueprint, issues);
                    }
                }
            }
        }

        boolean valid = issues.stream()
                .noneMatch(i -> i.getSeverity() == ValidationIssue.Severity.ERROR);

        log.debug("DefaultConfigValidator: {} Issues ({} Errors), valid={}",
                issues.size(),
                issues.stream().filter(i -> i.getSeverity() == ValidationIssue.Severity.ERROR).count(),
                valid);

        return ValidationReport.builder()
                .valid(valid)
                .issues(issues)
                .build();
    }

    private void validateRequiredParameters(String beanId, Map<String, Object> params,
                                            ProcessorBlueprint blueprint,
                                            List<ValidationIssue> issues) {
        if (blueprint.getParameters() == null) return;

        for (var paramBp : blueprint.getParameters()) {
            if (paramBp.isRequired()) {
                Object value = params != null ? params.get(paramBp.getName()) : null;
                boolean hasDefault = paramBp.getDefaultValue() != null
                        && !paramBp.getDefaultValue().isBlank();

                if (value == null && !hasDefault) {
                    issues.add(ValidationIssue.builder()
                            .severity(ValidationIssue.Severity.ERROR)
                            .parameterPath(beanId + "." + paramBp.getName())
                            .message("Pflichtparameter '" + paramBp.getName() + "' fehlt.")
                            .suggestion(paramBp.getDescription() != null
                                    ? "Benötigt: " + paramBp.getDescription()
                                    : "Pflichtparameter muss gesetzt werden.")
                            .build());
                }
            }
        }
    }

    /**
     * Extrahiert die prototypeId aus einer vollständigen beanId.
     *
     * <p>Format: {@code prototypeId[:identifier][@scope]}
     * Beispiel: {@code "csvReaderProcessor:reader1@parentcontext"} → {@code "csvReaderProcessor"}
     */
    private String extractPrototypeId(String beanId) {
        if (beanId == null) return "";
        int colonIdx = beanId.indexOf(':');
        int atIdx = beanId.indexOf('@');

        if (colonIdx > 0) return beanId.substring(0, colonIdx);
        if (atIdx > 0) return beanId.substring(0, atIdx);
        return beanId;
    }
}