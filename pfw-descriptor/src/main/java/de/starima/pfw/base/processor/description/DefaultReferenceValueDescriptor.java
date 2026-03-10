package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.description.api.IParameterDescriptor;
import de.starima.pfw.base.processor.description.api.IReferenceValueDescriptor;
import de.starima.pfw.base.processor.description.api.IStructureValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * {@code DefaultReferenceValueDescriptor} ist eine Implementierung von {@link IReferenceValueDescriptor}.
 * Sie beschreibt keinen direkten Wert, sondern einen Verweis auf einen Wert, der an anderer Stelle
 * definiert ist (z.B. in einem Ã¼bergeordneten Kontext oder einer zentralen Bibliothek).
 *
 * <p>Dieser Deskriptor-Typ wird nicht durch Reflection eines Feld-Typs bestimmt, sondern ist eine
 * "Meta-Option", die von einem {@link IParameterDescriptor}
 * angeboten wird, um Vererbung oder Wiederverwendung zu ermÃ¶glichen.</p>
 *
 * <p><b>Prozessor-Beschreibung:</b></p>
 * <p><b>Name:</b> DefaultReferenceValueDescriptor</p>
 * <p><b>Beschreibung:</b> Beschreibt einen Verweis auf einen Wert, der an anderer Stelle definiert ist (z.B. in einem Ã¼bergeordneten Kontext).</p>
 * <p><b>Kategorien:</b> descriptor/value/reference</p>
 * <p><b>Parameter:</b></p>
 * <ul>
 *     <li>{@code resolvedValue}: Der aktuelle Java-Objekt Wert.</li>
 *     <li>{@code rawValue}: Der aktuelle Parameter Wert (wie bei StructureDescriptoren auch die fullBeanId).</li>
 *     <li>{@code valueFunction}: Die {@link IValueFunction}, die fÃ¼r die Transformation und AuflÃ¶sung der Referenz zustÃ¤ndig ist.</li>
 *     <li>{@code referencePath}: Der logische Pfad zum referenzierten Wert (z.B. "parentContext.defaultProcessor").</li>
 *     <li>{@code version}: Die Version der referenzierten Konfiguration.</li>
 *     <li>{@code overrides}: Ein {@link IStructureValueDescriptor}, der nur die lokalen Ãœberschreibungen der referenzierten Konfiguration beschreibt.</li>
 * </ul>
 */
@Slf4j
@Getter
@Setter
@Processor(
    description = "Beschreibt einen Verweis auf einen Wert, der an anderer Stelle definiert ist (z.B. in einem Ã¼bergeordneten Kontext).",
    categories = {"descriptor/value/reference"}
)
public class DefaultReferenceValueDescriptor<T extends IStructureValueDescriptor<T>> extends DefaultValueDescriptor implements IReferenceValueDescriptor<T> {
    @ProcessorParameter(description = "Der logische Pfad zum referenzierten Wert (z.B. 'parentContext.defaultProcessor').")
    private String referencePath;

    @ProcessorParameter(description = "Die Version der referenzierten Konfiguration.")
    private String version;

    @ProcessorParameter(description = "Ein Struktur-Deskriptor, der NUR die lokalen Ãœberschreibungen der referenzierten Konfiguration beschreibt.")
    private T overrides;

    @Override
    public void processorOnInit() {
        super.processorOnInit();
        if (overrides != null) {
            overrides.setParentDescriptor(this);
        }
    }

    // Die Methoden getReferencePath(), getVersion(), getOverrides() werden von Lombok generiert.
}