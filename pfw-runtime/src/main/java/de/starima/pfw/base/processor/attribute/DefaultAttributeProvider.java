package de.starima.pfw.base.processor.attribute;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.attribute.api.IAttribute;
import de.starima.pfw.base.processor.attribute.api.IAttributeProviderProcessor;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter @Setter
@Processor(description = "Attribute Provider basierend auf dem Column Typ des Tableadmin. Dieser dient in der Regel als globale Quelle von Attributedefinitionen, auf die in anderen Kontexten selektiv zugegriffen wird. Siehe auch ReferenceAttributeProvider.")
public class DefaultAttributeProvider extends AbstractProcessor implements IAttributeProviderProcessor<IAttribute> {

    @ProcessorParameter(description = "Attribute Provider, der die Attributdefinitionen bereitstellt. Diese kÃ¶nnen dann an unterschiedlichen Stellen wiederverwendet werden.")
    private IAttributeProviderProcessor<IAttribute> parentAttributeProvider;
    @ProcessorParameter()
    private List<IAttribute> attributes;
    @ProcessorParameter
    private String tableName = "";

    private List<IAttribute> mergedAttributes;

    @Override
    public List<IAttribute> getAttributes() {
        if (mergedAttributes == null) {
            List<IAttribute> parentAttributes = new ArrayList<>();
            if (this.parentAttributeProvider != null) {
                parentAttributes = this.parentAttributeProvider.getAttributes();
            }

            //wir fÃ¼gen die beiden Arrays so zusammen, das Elemente aus this.attributes angehangen werden, falls sie in this.parentAttributeProvider.getAttributes() noch nicht vorhanden sind.
            //Existiert ein Element in beiden Listen (dabei dient name als Identifier - dest.name == source.name), dann werden die Properties von dest durch die von source Ã¼berschrieben.
            this.mergedAttributes = MapUtils.mergeLists(parentAttributes, this.attributes != null ? this.attributes : new ArrayList<IAttribute>(),
                    (attr1, attr2) -> attr1.getName().equals(attr2.getName()),
                    (attr1,attr2) -> {
                        try {
                            BeanUtils.copyProperties(attr1,attr2);
                            return attr1;
                        } catch (Exception e) {
                            log.error("{}.getAttributes() failed", this.getIdentifier());
                            throw new RuntimeException(e);
                        }
                    });
        }

        return this.mergedAttributes;
    }

    @Override
    public IAttribute getAttribute(String name) {
        if (name == null) return null;

       return this.getAttributes().stream().filter(column -> name.equalsIgnoreCase(column.getName())).findFirst().orElse(null);
    }

    @Override
    public List<IAttribute> getAttributes(List<String> names) {
        if (names != null) {
            ArrayList<IAttribute> result = new ArrayList<>();
            names.forEach(name -> result.add(getAttribute(name)));
            return result;
        }

        return new ArrayList<>();
    }

    @Override
    public List<String> getAttributeReferences() {
        List<IAttribute> collectedAttributes = getAttributes();
        if (collectedAttributes != null) {
            try {
                return collectedAttributes.stream().map(IAttribute::getName).collect(Collectors.toList());
            } catch (Exception e) {
                log.warn("{}: Can not collect column names", this.getIdentifier());
            }
        }
        return new ArrayList<>();
    }

    @Override
    public boolean isResponsibleForSubject(Object subject) {
        return subject != null ? subject.toString().startsWith(tableName) : false;
    }


}