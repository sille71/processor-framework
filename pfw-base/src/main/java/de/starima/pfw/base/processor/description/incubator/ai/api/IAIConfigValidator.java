package de.starima.pfw.base.processor.description.incubator.ai.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.domain.BlueprintDraft;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ValidationReport;

/**
 * Validiert einen KI-generierten Konfigurationsentwurf gegen die Descriptor-Constraints.
 *
 * <p>Findet fehlende Pflichtparameter, ungültige Werte und inkompatible
 * Typ-Kombinationen. Kann als Qualitätsstufe im AI Planning Layer eingesetzt
 * werden, bevor ein Entwurf dem Nutzer präsentiert wird.
 *
 * <p>Kategorien: {@code ai, validation}<br>
 * Tags: {@code validate, constraint, check, blueprint}
 */
public interface IAIConfigValidator extends IProcessor {

    /**
     * Validiert einen Entwurf vollständig gegen den Prozessor-Katalog.
     *
     * @param draft   Der zu validierende Entwurf
     * @param catalog Der Prozessor-Katalog mit Constraint-Informationen
     * @return Ein Validierungsreport mit Fehlern, Warnungen und Hinweisen
     */
    ValidationReport validate(BlueprintDraft draft, IProcessorCatalog catalog);
}