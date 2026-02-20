package de.starima.pfw.base.processor.description.api;

import de.starima.pfw.base.processor.api.IProcessor;
import java.util.List;
import java.util.Map;

/**
 * Ein Prozessor, der fÃ¼r die AuflÃ¶sung und Bereitstellung von Deskriptor-Konfigurationen
 * fÃ¼r alle im System verfÃ¼gbaren @Processor und @ValueObject-Klassen zustÃ¤ndig ist.
 * Er fungiert als "Katalog-Generator".
 * @deprecated
 */
public interface IDescriptorResolver extends IProcessor {
    /**
     * Sammelt die beanParameterMaps aller potenziellen Kandidaten (@Processor, @ValueObject)
     * im System. Diese Maps reprÃ¤sentieren die Konfigurationen ihrer jeweiligen Deskriptoren.
     * Es findet hier noch keine Filterung nach Parameter-Anforderungen statt.
     *
     * @return Eine Liste von beanParameterMaps, wobei jede Map die Konfiguration eines Deskriptors darstellt.
     */
    List<Map<String, Map<String, Object>>> findAllCandidateDescriptorMaps();

    public boolean matchesCategories(String[] offeredCategories, String[] requiredCategories);
    public boolean matchesTags(String[] offeredTags, String[] requiredTags);
    public boolean matchesSubCategories(String[] offeredSubCategories, String[] requiredSubCategories);
}