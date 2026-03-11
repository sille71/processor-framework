package de.starima.pfw.base.processor.description.incubator.ai;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.description.incubator.ai.api.IProcessorCatalog;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ParameterBlueprint;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ProcessorBlueprint;
import de.starima.pfw.base.processor.description.incubator.ai.domain.ProcessorCatalogEntry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Standard-Implementierung des {@link IProcessorCatalog}.
 *
 * <p>Hält eine Liste von {@link ProcessorCatalogEntry}-Einträgen und
 * bietet Suchoperationen via Text-Matching und Tag/Kategorie-Filter.
 *
 * <h3>Suche</h3>
 * {@link #search(String)} splittet die Query in Wörter und sucht
 * nach Einträgen, die mindestens eines dieser Wörter in prototypeId,
 * shortDescription, categories oder tags enthalten. Ergebnisse sind
 * nach Trefferanzahl sortiert.
 *
 * <h3>Population</h3>
 * Der Katalog wird typischerweise von einem
 * {@code ProcessorCatalogBuilder} befüllt, der alle @Processor-annotierten
 * Klassen im Classpath scannt (Phase 5 Session 2).
 */
@Slf4j
@Getter
@Setter
@Processor(
        description = "Standard-Implementierung des IProcessorCatalog. " +
                "Durchsuchbarer Index aller Prozessor-Blueprints — " +
                "Kategorien, Tags, Beschreibungen, Parameter-Signaturen. " +
                "Dient als Wissensbasis für KI-Assistenten.",
        categories = {"ai", "catalog"},
        tags = {"search", "index", "blueprint", "knowledge"}
)
public class DefaultProcessorCatalog extends AbstractProcessor implements IProcessorCatalog {

    @ProcessorParameter(description = "Alle Prozessor-Einträge im Katalog. " +
            "Wird beim Startup aus @Processor-annotierten Klassen befüllt.",
            ignoreInitialization = true)
    private List<ProcessorCatalogEntry> entries = new ArrayList<>();

    // =========================================================================
    // search — Freitext-Suche
    // =========================================================================

    @Override
    public List<ProcessorCatalogEntry> search(String naturalLanguageQuery) {
        if (naturalLanguageQuery == null || naturalLanguageQuery.isBlank()) {
            return List.copyOf(entries);
        }

        String[] queryWords = naturalLanguageQuery.toLowerCase().split("\\s+");

        // Score-basierte Suche
        Map<ProcessorCatalogEntry, Integer> scores = new LinkedHashMap<>();
        for (ProcessorCatalogEntry entry : entries) {
            int score = computeScore(entry, queryWords);
            if (score > 0) {
                scores.put(entry, score);
            }
        }

        // Sortieren nach Score (absteigend)
        return scores.entrySet().stream()
                .sorted(Map.Entry.<ProcessorCatalogEntry, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private int computeScore(ProcessorCatalogEntry entry, String[] queryWords) {
        int score = 0;
        for (String word : queryWords) {
            if (matchesEntry(entry, word)) {
                score++;
                // Mehr Punkte wenn prototypeId oder capabilities matchen
                if (entry.getPrototypeId() != null
                        && entry.getPrototypeId().toLowerCase().contains(word)) {
                    score++;
                }
                if (entry.getCapabilities() != null) {
                    for (String cap : entry.getCapabilities()) {
                        if (cap != null && cap.toLowerCase().contains(word)) {
                            score++;
                        }
                    }
                }
            }
        }
        return score;
    }

    private boolean matchesEntry(ProcessorCatalogEntry entry, String word) {
        if (entry.getPrototypeId() != null
                && entry.getPrototypeId().toLowerCase().contains(word)) return true;
        if (entry.getShortDescription() != null
                && entry.getShortDescription().toLowerCase().contains(word)) return true;
        if (entry.getLongDescription() != null
                && entry.getLongDescription().toLowerCase().contains(word)) return true;
        if (entry.getDisplayName() != null
                && entry.getDisplayName().toLowerCase().contains(word)) return true;

        if (entry.getCategories() != null) {
            for (String cat : entry.getCategories()) {
                if (cat != null && cat.toLowerCase().contains(word)) return true;
            }
        }
        if (entry.getTags() != null) {
            for (String tag : entry.getTags()) {
                if (tag != null && tag.toLowerCase().contains(word)) return true;
            }
        }
        if (entry.getTypicalUseCases() != null) {
            for (String uc : entry.getTypicalUseCases()) {
                if (uc != null && uc.toLowerCase().contains(word)) return true;
            }
        }
        return false;
    }

    // =========================================================================
    // findByCategory
    // =========================================================================

    @Override
    public List<ProcessorCatalogEntry> findByCategory(String category) {
        if (category == null) return List.of();
        String cat = category.toLowerCase();

        return entries.stream()
                .filter(e -> e.getCategories() != null
                        && Arrays.stream(e.getCategories())
                        .anyMatch(c -> c != null && c.toLowerCase().equals(cat)))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // findByTags — AND-Verknüpfung
    // =========================================================================

    @Override
    public List<ProcessorCatalogEntry> findByTags(String... tags) {
        if (tags == null || tags.length == 0) return List.copyOf(entries);

        return entries.stream()
                .filter(e -> e.getTags() != null
                        && allTagsMatch(e.getTags(), tags))
                .collect(Collectors.toList());
    }

    private boolean allTagsMatch(String[] entryTags, String[] searchTags) {
        Set<String> entryTagSet = Arrays.stream(entryTags)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String searchTag : searchTags) {
            if (searchTag != null && !entryTagSet.contains(searchTag.toLowerCase())) {
                return false;
            }
        }
        return true;
    }

    // =========================================================================
    // findCandidatesForParameter
    // =========================================================================

    @Override
    public List<ProcessorCatalogEntry> findCandidatesForParameter(
            String processorId, String parameterName) {
        // Holt den Blueprint des Prozessors und filtert nach requiredCategories
        ProcessorBlueprint blueprint = getBlueprint(processorId);
        if (blueprint == null || blueprint.getParameters() == null) {
            log.debug("findCandidatesForParameter: kein Blueprint für '{}'", processorId);
            return List.of();
        }

        Optional<ParameterBlueprint> paramBlueprintOpt = blueprint.getParameters().stream()
                .filter(p -> parameterName.equals(p.getName()))
                .findFirst();

        if (paramBlueprintOpt.isEmpty()) {
            log.debug("findCandidatesForParameter: Parameter '{}' nicht gefunden in '{}'",
                    parameterName, processorId);
            return List.of();
        }

        ParameterBlueprint paramBlueprint = paramBlueprintOpt.get();

        // Wenn requiredCategories definiert: nach Kategorien filtern
        if (paramBlueprint.getRequiredCategories() != null
                && paramBlueprint.getRequiredCategories().length > 0) {
            return entries.stream()
                    .filter(e -> matchesAnyCategory(e, paramBlueprint.getRequiredCategories()))
                    .collect(Collectors.toList());
        }

        // Wenn explizite Kandidaten definiert: nach Kandidaten-IDs filtern
        if (paramBlueprint.getCandidates() != null && !paramBlueprint.getCandidates().isEmpty()) {
            Set<String> candidateIds = new HashSet<>(paramBlueprint.getCandidates());
            return entries.stream()
                    .filter(e -> candidateIds.contains(e.getPrototypeId()))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private boolean matchesAnyCategory(ProcessorCatalogEntry entry, String[] requiredCategories) {
        if (entry.getCategories() == null) return false;
        Set<String> entryCategories = Arrays.stream(entry.getCategories())
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String required : requiredCategories) {
            if (required != null && entryCategories.contains(required.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // getBlueprint
    // =========================================================================

    @Override
    public ProcessorBlueprint getBlueprint(String prototypeId) {
        if (prototypeId == null) return null;

        Optional<ProcessorCatalogEntry> entryOpt = entries.stream()
                .filter(e -> prototypeId.equals(e.getPrototypeId()))
                .findFirst();

        if (entryOpt.isEmpty()) {
            log.debug("getBlueprint: kein Eintrag für '{}'", prototypeId);
            return null;
        }

        ProcessorCatalogEntry entry = entryOpt.get();

        // ParameterBlueprints aus ParameterSummaries ableiten
        List<ParameterBlueprint> paramBlueprints = entry.getParameters() == null
                ? List.of()
                : entry.getParameters().stream()
                .map(ps -> ParameterBlueprint.builder()
                        .name(ps.getName())
                        .typeSignature(ps.getTypeSignature())
                        .description(ps.getDescription())
                        .required(ps.isRequired())
                        .defaultValue(ps.getDefaultValue())
                        .build())
                .collect(Collectors.toList());

        return ProcessorBlueprint.builder()
                .prototypeId(entry.getPrototypeId())
                .description(entry.getShortDescription())
                .parameters(paramBlueprints)
                .build();
    }

    // =========================================================================
    // getCapabilitySummary
    // =========================================================================

    @Override
    public String getCapabilitySummary() {
        if (entries.isEmpty()) {
            return "Kein Prozessor-Katalog geladen.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Verfügbare Prozessoren (").append(entries.size()).append(" Einträge):\n\n");

        // Nach Kategorien gruppieren
        Map<String, List<ProcessorCatalogEntry>> byCategory = new LinkedHashMap<>();
        for (ProcessorCatalogEntry entry : entries) {
            String primaryCategory = (entry.getCategories() != null
                    && entry.getCategories().length > 0)
                    ? entry.getCategories()[0]
                    : "Sonstige";
            byCategory.computeIfAbsent(primaryCategory, k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<ProcessorCatalogEntry>> catEntry : byCategory.entrySet()) {
            sb.append("### ").append(catEntry.getKey()).append("\n");
            for (ProcessorCatalogEntry e : catEntry.getValue()) {
                sb.append("- **").append(e.getPrototypeId()).append("**: ")
                        .append(e.getShortDescription() != null
                                ? e.getShortDescription() : "(keine Beschreibung)")
                        .append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // =========================================================================
    // Catalog-Management
    // =========================================================================

    /**
     * Fügt einen einzelnen Eintrag zum Katalog hinzu.
     *
     * @param entry der neue Eintrag
     */
    public void addEntry(ProcessorCatalogEntry entry) {
        if (entry != null) {
            entries.add(entry);
            log.debug("DefaultProcessorCatalog: Eintrag hinzugefügt '{}'", entry.getPrototypeId());
        }
    }

    /**
     * Entfernt alle Einträge und befüllt den Katalog neu.
     *
     * @param newEntries die neuen Einträge
     */
    public void rebuildFrom(List<ProcessorCatalogEntry> newEntries) {
        this.entries = new ArrayList<>(newEntries != null ? newEntries : List.of());
        log.info("DefaultProcessorCatalog: {} Einträge geladen", this.entries.size());
    }
}