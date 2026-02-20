package de.starima.pfw.base.processor.attribute;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.attribute.api.IAttributeProviderProcessor;
import de.starima.pfw.base.processor.attribute.domain.RcnAttribute;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter @Setter
@Processor(description = "Stellt die Attribute fÃ¼r einen konkreten Anwendungsfall bereit (z.B. ReconChangelog - display, search). Dabei werden lediglich die Attribute referenziert. Die eigentliche Definition wird aus einem Ã¼bergeordneten Prozessor gezogen. Optional kÃ¶nnen auch Attributdefinitionen im Parameter attributes Ã¼berschrieben werden.")
public class ReferenceAttributeProvider extends DefaultAttributeProvider {

    @ProcessorParameter(description = "Attribute Provider, der die Attributdefinitionen bereitstellt. Diese kÃ¶nnen dann an unterschiedlichen Stellen wiederverwendet werden.")
    private IAttributeProviderProcessor<RcnAttribute> parentAttributeReferenceProvider;
    @ProcessorParameter(description = "Explizit benannte Attribute, die dieser Provider zur VerfÃ¼gung stellt.", parameterFunctionProcessorPrototypeIdentifier = "jacksonGenericListParameterFunctionProcessor")
    private List<String> attributeReferences;

    private List<String> mergedAttributeReferences;

    @Override
    public List<RcnAttribute> getAttributes() {
        List<String> collectedAttributeReferences = getAttributeReferences();

        if (collectedAttributeReferences == null || collectedAttributeReferences.isEmpty()) {
            log.warn("{}: Can not get attributes, no attribute references defined!", this.getIdentifier());
            return new ArrayList<>();
        }

        return this.getAttributes(collectedAttributeReferences);
    }

    @Override
    public List<String> getAttributeReferences() {
        if (this.mergedAttributeReferences == null) {
            List<String> parentAttributeReferences = new ArrayList<>();
            if ( this.parentAttributeReferenceProvider != null) {
                parentAttributeReferences = this.parentAttributeReferenceProvider.getAttributeReferences();
            }

            //wir fÃ¼gen die beiden Arrays so zusammen, das Elemente aus this.attributes angehangen werden, falls sie in this.parentAttributeProvider.getAttributes() noch nicht vorhanden sind.
            //Existiert ein Element in beiden Listen (dabei dient name als Identifier - dest.name == source.name), dann werden die Properties von dest durch die von source Ã¼berschrieben.
            this.mergedAttributeReferences = MapUtils.mergeLists(parentAttributeReferences, this.attributeReferences != null ? this.attributeReferences : new ArrayList<>(),
                    (attr1, attr2) -> attr1 == attr2,
                    (attr1, attr2) -> {return attr1;});
        }

        return this.mergedAttributeReferences;
    }

    //TODO: hier wird die Reihenfolge der zugrunde liegenden Attribute benutzt und nicht die vom Array names!
    @Override
    public List<RcnAttribute> getAttributes(List<String> names) {
        if (names == null) return new ArrayList<>();
        return super.getAttributes().stream().filter(attr -> names.contains(attr.getName())).collect(Collectors.toList());
    }
}