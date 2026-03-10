package de.starima.pfw.base.processor.attribute;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeProviderProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttributeProviderSelectorProcessor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter
@Setter
@Slf4j
@Processor(description = "Dieser Processor selektiert einen geeigneten AttributeProvider an Hand einer Tabellendefinition. Dies ist hilfreich, wenn gleichnamige Attribute in unterschiedlichen Tabellen auch unterschiedliche Typen und Formate haben.")
public class TableAttributeProviderSelector extends AbstractProcessor implements IAttributeProviderSelectorProcessor<IAttribute> {
    @ProcessorParameter(description = "die Liste der mÃ¶glichen Provider, aus denen ein geeigneter Provider selektiert wird.")
    private List<IAttributeProviderProcessor<IAttribute>> attributeProviderProcessors;
    @Override
    public IAttributeProviderProcessor<IAttribute> getAttributeProviderForSubjectReference(String subjectReference) {
        if (attributeProviderProcessors != null) {
            return attributeProviderProcessors.stream().filter(ap -> ap.isResponsibleForSubject(subjectReference)).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public IAttributeProviderProcessor<IAttribute> getAttributeProviderForSubject(Object subject) {
        //im Falle einer Tabelle liefert toString den Tabellennamen!
        return subject != null ? getAttributeProviderForSubjectReference(subject.toString()) : null;
    }

    @Override
    public boolean isResponsibleForSubject(Object subject) {
        return getAttributeProviderForSubject(subject) != null;
    }
}