package de.starima.pfw.base.processor.attribute;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.artifact.api.IArtifactProvider;
import de.starima.pfw.base.processor.attribute.api.IAttributeProviderProcessor;
import de.starima.pfw.base.processor.attribute.domain.RcnAttribute;
import de.starima.pfw.base.util.MapUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Processor
public class AttributeProviderChain extends AbstractProcessor implements IAttributeProviderProcessor<RcnAttribute> {
    @ProcessorParameter
    private List<IAttributeProviderProcessor<RcnAttribute>> attributeProviderProcessors;
    @ProcessorParameter(description = "Das Subject, z.B. Tabelle, kann im Kontext zur Laufzeit hinterlegt werden. Dazu kann ein ArtifactProvider benutz werden.")
    private String subjectProviderIdentifier;

    private Object getSubject() {
        if (subjectProviderIdentifier == null) return null;
        IArtifactProvider ap = getRuntimeContext().getProcessorProvider().getProcessorForBeanId(IArtifactProvider.class, subjectProviderIdentifier, getRuntimeContext(), this);
        return ap != null ? ap.getArtifact(Object.class) : null;
    }

    @Override
    public RcnAttribute getAttribute(String attributeReference) {
        if(attributeProviderProcessors == null) return null;
        IAttributeProviderProcessor<RcnAttribute> ap = null;
        if (getSubject() != null) {
            ap = getResponsibleAttributeProvider(getSubject());

        } else {
            ap = attributeProviderProcessors.stream().filter(p -> p.getAttribute(attributeReference) != null).findFirst().orElse(null);
        }
        return ap != null ? ap.getAttribute(attributeReference) : null;
    }

    @Override
    public List<RcnAttribute> getAttributes(List<String> attributeReferences) {
        if (getSubject() != null) {
            IAttributeProviderProcessor<RcnAttribute> ap = getResponsibleAttributeProvider(getSubject());
            return ap != null ? ap.getAttributes(attributeReferences) : new ArrayList<>();
        }
        //TODO auf collector umstellen
        List<RcnAttribute> attributeList = new ArrayList<>();
        for (IAttributeProviderProcessor<RcnAttribute> ap : attributeProviderProcessors) {
            attributeList.addAll(ap.getAttributes(attributeReferences));
        }
        return attributeList;
    }

    @Override
    public List<RcnAttribute> getAttributes() {
        if (getSubject() != null) {
            IAttributeProviderProcessor<RcnAttribute> ap = getResponsibleAttributeProvider(getSubject());
            return ap != null ? ap.getAttributes() : new ArrayList<>();
        }
        //TODO auf collector umstellen
        List<RcnAttribute> attributeList = new ArrayList<>();
        for (IAttributeProviderProcessor<RcnAttribute> ap : attributeProviderProcessors) {
            attributeList = MapUtils.mergeLists(attributeList, ap.getAttributes(),
                    (attr1, attr2) -> attr1 == attr2,
                    (attr1, attr2) -> {return attr1;});
        }
        return attributeList;
    }

    @Override
    public List<String> getAttributeReferences() {
        if (getSubject() != null) {
            IAttributeProviderProcessor<RcnAttribute> ap = getResponsibleAttributeProvider(getSubject());
            return ap != null ? ap.getAttributeReferences() : new ArrayList<>();
        }
        List<String> attributeReferenceList = new ArrayList<>();
        for (IAttributeProviderProcessor<RcnAttribute> ap : attributeProviderProcessors) {
            attributeReferenceList.addAll(ap.getAttributeReferences());
        }
        return attributeReferenceList;
    }

    @Override
    public boolean isResponsibleForSubject(Object subject) {
        return getResponsibleAttributeProvider(subject) != null;
    }

    private IAttributeProviderProcessor getResponsibleAttributeProvider(Object subject) {
        if(attributeProviderProcessors == null) return null;
        return attributeProviderProcessors.stream().filter(p -> p.isResponsibleForSubject(subject)).findFirst().orElse(null);
    }
}