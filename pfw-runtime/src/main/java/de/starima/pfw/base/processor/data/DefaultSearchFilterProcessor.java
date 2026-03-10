package de.starima.pfw.base.processor.data;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeProviderProcessor;
import de.starima.pfw.base.processor.attribute.domain.DefaultAttribute;
import de.starima.pfw.base.processor.data.api.ISearchFilterProcessor;
import de.starima.pfw.base.processor.data.domain.Pager;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
@Processor
public class DefaultSearchFilterProcessor extends AbstractProcessor implements ISearchFilterProcessor<IAttribute> {
    @ProcessorParameter(description = "Namen der default sort Attribute. Die Attribute selbst werden vom sortAttributeProvider zur VerfÃ¼gung gestellt", parameterFunctionProcessorPrototypeIdentifier = "jacksonGenericListParameterFunctionProcessor")
    private List<String> defaultSortAttributeReferences;
    @ProcessorParameter(description = "Alternative Definition der default sort Attribute. Falls nicht gesetzt wrid der Parameter aus defaultSortAttributeReferences erzeugt und an das FE gesendet.", parameterFunctionProcessorPrototypeIdentifier = "jacksonGenericListParameterFunctionProcessor")
    private List<IAttribute> defaultSortAttributes;

    @ProcessorParameter(description = "angefragte search Attribute mit den Suchwerten, wird gewÃ¶hnlich vom Frontend geliefert", parameterFunctionProcessorPrototypeIdentifier = "jacksonGenericListParameterFunctionProcessor")
    private List<IAttribute> requestedSearchAttributes;

    @ProcessorParameter(description = "Namen der anzuzeigenden Attribute. Die Attribute selbst werden vom displayAttributeProvider zur VerfÃ¼gung gestellt", parameterFunctionProcessorPrototypeIdentifier = "jacksonGenericListParameterFunctionProcessor")
    private List<String> requestedDisplayAttributeReferences;

    @ProcessorParameter(description = "Map der Form {\"firstRow\" :  0, \"pageSize\" : 100}", parameterFunctionProcessorPrototypeIdentifier = "jacksonParameterFunctionProcessor")
    private Pager pager;

    @ProcessorParameter(description = "Sortier Attribute der aktuellen Suchanfrage", parameterFunctionProcessorPrototypeIdentifier = "jacksonGenericListParameterFunctionProcessor")
    private List<IAttribute> requestedSortAttributes;

    @ProcessorParameter(description = "Dieser Attributprovider stellt die Sortier Attribute bereit.")
    private IAttributeProviderProcessor<IAttribute> sortAttributeProvider;

    @ProcessorParameter(description = "Dieser Attributprovider stellt die Such Attribute bereit.")
    private IAttributeProviderProcessor<IAttribute> searchAttributeProvider;

    @ProcessorParameter(description = "Dieser Attributprovider stellt die Display Attribute bereit.")
    private IAttributeProviderProcessor<IAttribute> displayAttributeProvider;


    @Override
    public List<IAttribute> getRequestedSearchAttributes() {
        return requestedSearchAttributes;
    }

    @Override
    public List<IAttribute> getRequestedSortAttributes() {
        return requestedSortAttributes;
    }

    @Override
    public List<IAttribute> getDefaultSortAttributes() {
        if (this.defaultSortAttributes != null && !this.defaultSortAttributes.isEmpty()) return this.defaultSortAttributes;
        if(defaultSortAttributeReferences == null) return null;
        log.info("create defaultSortAttributes from references {}", StringUtils.arrayToCommaDelimitedString(this.defaultSortAttributeReferences.toArray()));
        this.defaultSortAttributes = new ArrayList<>();
        int i = 0;
        for (String reference : defaultSortAttributeReferences) {
            String[] split = reference.split(":");
            String direction = split.length > 1 ? split[1] : null;
            IAttribute sortAttribute = getSortAttribute(split[0], direction);
            sortAttribute.setSortOrder(i);
            ++i;
            defaultSortAttributes.add(sortAttribute);
        }
        return defaultSortAttributes;
    }

    protected IAttribute getSortAttribute(String reference,String direction) {
        IAttribute sortAttribute = sortAttributeProvider != null ? sortAttributeProvider.getAttribute(reference) : new DefaultAttribute();
        sortAttribute.setName(reference);
        sortAttribute.setSortDirection(direction);
        return sortAttribute;
    }

    @Override
    public List<IAttribute> getRequestedDisplayAttributes() {
        return this.getDisplayAttributeProvider().getAttributes(this.requestedDisplayAttributeReferences);
    }

    @Override
    public Pager getRequestedPager() {
        return this.pager;
    }

    @Override
    public void processorOnInit() {
        getDefaultSortAttributes();
        //log.info("{}: initialized with parameters {}",this.getIdentifier(), LogOutputHelper.getModelAsStringBuffer(this.extractEffectiveProcessorParameterMap(), null));
    }
}