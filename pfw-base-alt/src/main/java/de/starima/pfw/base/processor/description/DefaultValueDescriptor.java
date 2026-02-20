package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Getter
@Setter
@Processor(description = "A generic descriptor for a value")
public class DefaultValueDescriptor extends DescriptorProcessor implements IValueDescriptor {

    @ProcessorParameter(description = "Der Rohwert, so wie er aus der Konfiguration kommt (z.B. ein String mit der Bean-ID).")
    private Object rawValue;

    @ProcessorParameter(description = "Das aufgelÃ¶ste Java-Objekt (z.B. die referenzierte Bean-Instanz).", ignoreExtractParameter = true)
    private Object resolvedValue;

    @ProcessorParameter
    private IValueDescriptor prototypeValueDescriptor;

    @ProcessorParameter(description = "Die ValueFunction, die fÃ¼r die Transformation und AuflÃ¶sung der Referenz zustÃ¤ndig ist.")
    private IValueFunction<ITransformationContext, Object, Object> valueFunction;

    @ProcessorParameter(description = "Dies ist der Hauptfilter. Wenn ein IValueDescriptor hier z.B. ['database/connector'] stehen hat, bedeutet das, dass nur Prozessoren oder ValueObjects, die ebenfalls in dieser Kategorie sind, als mÃ¶glicher Wert in der UI zur Auswahl angeboten werden.")
    private String[] requiredCategories;
    @ProcessorParameter(description = "Dies ist eine Verfeinerung. Es schrÃ¤nkt die Auswahl weiter ein, nachdem die Hauptkategorie bereits gefiltert hat.")
    private String[] requiredSubCategories;
    @ProcessorParameter(description = "Tags bieten einen flexiblen, zusÃ¤tzlichen Filtermechanismus, der quer zu den hierarchischen Kategorien funktioniert.")
    private String[] requiredTags;

    @Override
    public void processorOnInit() {
        if (getValueFunction() != null) {
            getValueFunction().setParentDescriptor(this);
        }
    }

    @Override
    public String getPath() {
        // Erbt den Pfad vom Parent.
        return getParentDescriptor() != null ? getParentDescriptor().getPath() : "";
    }

    @Override
    public Optional<IDescriptorProcessor> findDescriptor(String path) {
        // Ein Skalar ist ein Blattknoten. Er kann nicht weiter durchsucht werden.
        // Er kann nur sich selbst finden, wenn der Pfad leer ist (was bedeutet, dass der Parent ihn bereits gefunden hat).
        if (path == null || path.isEmpty()) {
            return Optional.of(this);
        }
        return Optional.empty();
    }

    @Override
    public List<IValueDescriptor> getPossibleValueDescriptors() {
        // IST DIES EINE INSTANZ (ein "GebÃ¤ude")?
        if (getPrototypeValueDescriptor() != null) {
            // Ja. Eine Instanz kann geÃ¤ndert werden. Welche Optionen gibt es?
            // Wir fragen den Bauplan nach den Renovierungsoptionen.
            // In den meisten FÃ¤llen wird der Prototyp sich selbst als mÃ¶gliche Option zurÃ¼ckgeben.
            return getPrototypeValueDescriptor().getPossibleValueDescriptors();
        }

        //TODO noch ausimplementieren
        return null;
        // WENN NICHT, IST DIES EIN PROTOTYP (ein "Bauplan").
        // Hat dieser Prototyp einen spezifischen Typ (eine feste ValueFunction)?
        /*
        if (getValueFunction() != null) {
            // Ja (z.B. der "Integer-Prototyp").
            // Die einzige MÃ¶glichkeit, die er anbietet, ist, eine Instanz von sich selbst zu erstellen.
            DefaultValueDescriptor instance = new DefaultValueDescriptor();
            instance.setRuntimeContext(getRuntimeContext());
            instance.setPrototypeDescriptor(this); // Wichtig: Die neue Instanz verweist auf uns als Prototyp.
            instance.setValueFunction(this.getValueFunction());
            return Collections.singletonList(instance);
        }

        // WENN NICHT, IST DIES EIN GENERISCHER PROTOTYP OHNE FESTE FUNKTION.
        // (z.B. ein allgemeiner DefaultValueDescriptor oder der AnyValueDescriptor)
        // Er muss alle mÃ¶glichen skalaren Typen anbieten.
        return getRuntimeContext().getProcessorProvider().getAllScalarValueDescriptorPrototypes();

         */
    }

    public void extractEffectiveParameterMap(ITransformationContext context, Map<String, Map<String, Object>> beanParameterMap, Set<Object> visited) {
        // Ein skalarer Wert ist ein Blatt im Objektbaum.
        // Er hat keine untergeordneten Parameter, die extrahiert werden mÃ¼ssten.
        // Die Rekursion endet hier. Diese Methode tut absichtlich nichts.
    }
}