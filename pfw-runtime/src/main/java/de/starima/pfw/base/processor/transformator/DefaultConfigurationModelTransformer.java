package de.starima.pfw.base.processor.transformator;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.description.api.*;
import de.starima.pfw.base.processor.transformator.api.IConfigurationModelTransformer;
import de.starima.pfw.base.util.ConfigurationModel;
import de.starima.pfw.base.util.MapDetailLevel;
import de.starima.pfw.base.util.SerializationTarget;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//TODO: noch nicht fertig, noch zu Ã¼berarbeiten

@Slf4j
@Processor
public class DefaultConfigurationModelTransformer extends AbstractProcessor implements IConfigurationModelTransformer {

    // --- PARSING LOGIC ---

    @Override
    public ConfigurationModel detectModel(Map<String, Map<String, Object>> beanParameterMap) {
        // Heuristik: Wenn ein Prozessor einen 'contextProviderProcessor' hat, ist es das Vererbungsmodell.
        for (Map<String, Object> params : beanParameterMap.values()) {
            if (params.containsKey("contextProviderProcessor")) {
                return ConfigurationModel.INHERITANCE;
            }
        }
        return ConfigurationModel.COMPOSITION;
    }

    @Override
    public IProcessorDescriptor parse(String rootProcessorIdentifier, Map<String, Map<String, Object>> beanParameterMap, IProcessorContext context) {
        // Die Implementierung des Parsens ist komplex und wird in einem spÃ¤teren Schritt verfeinert.
        // Sie wÃ¼rde die `detectModel` Methode nutzen und dann rekursiv die Deskriptor-Hierarchie aufbauen.
        log.warn("Parsing logic is not yet implemented.");
        //TODO Implementation
        return null;
    }

    // --- SERIALIZATION LOGIC ---

    @Override
    public Map<String, Map<String, Object>> createBeanParameterMap(
            IProcessorDescriptor rootDescriptor,
            SerializationTarget target,
            ConfigurationModel model,
            MapDetailLevel detail) {

        if (rootDescriptor == null) {
            return Collections.emptyMap();
        }

        Map<String, Map<String, Object>> targetMap = new HashMap<>();

        switch (target) {
            case PROCESSOR_HIERARCHY:
                buildProcessorMapRecursively(rootDescriptor, targetMap, model, detail);
                break;
            case DESCRIPTOR_HIERARCHY:
                buildDescriptorMapRecursively(rootDescriptor, targetMap, detail);
                break;
        }

        return targetMap;
    }

    /**
     * Baut die Map fÃ¼r die Prozessor-Hierarchie auf (Varianten 1-4).
     */
    private void buildProcessorMapRecursively(
            IProcessorDescriptor descriptor,
            Map<String, Map<String, Object>> targetMap,
            ConfigurationModel model,
            MapDetailLevel detail) {

        if (descriptor == null || targetMap.containsKey(descriptor.getSourceIdentifier())) {
            return; // Verhindert Endlosschleifen
        }

        // 1. Parameter des Anwendungsprozessors extrahieren
        Map<String, Object> processorParams = new HashMap<>();
        targetMap.put(descriptor.getSourceIdentifier(), processorParams);
        if (descriptor.getParameterDescriptors() != null) {
            for (IParameterDescriptor paramDescriptor : descriptor.getParameterDescriptors()) {
                if (paramDescriptor.getValueDescriptor() != null) {
                    processorParams.put(paramDescriptor.getParameterName(), paramDescriptor.getValueDescriptor().getRawValue());
                }
            }
        }

        // 2. (Optional) Deskriptor-Hierarchie hinzufÃ¼gen, wenn Detailgrad FULL ist
        if (detail == MapDetailLevel.FULL_WITH_DESCRIPTORS) {
            buildDescriptorMapRecursively(descriptor, targetMap, MapDetailLevel.PROCESSORS_ONLY); // Nur die Deskriptoren, nicht deren Deskriptoren
            processorParams.put("processorDescriptor", descriptor.getFullBeanId());
        }

        // 3. Kinder  rekursiv verarbeiten
        if (descriptor.getParameterDescriptors() != null) {
            for (IParameterDescriptor paramDescriptor : descriptor.getParameterDescriptors()) {
                if (paramDescriptor.getValueDescriptor() instanceof IProcessorDescriptor) {
                    IProcessorDescriptor childDescriptor = (IProcessorDescriptor) paramDescriptor.getValueDescriptor();

                    // 4. Hierarchie-Modell anwenden
                    if (model == ConfigurationModel.INHERITANCE) { //es erfolgt jetzt eine extra Behandlung der contextProvider Parameter
                        buildProcessorMapRecursively(childDescriptor, targetMap, model, detail);
                        Map<String, Object> childParams = targetMap.get(childDescriptor.getSourceIdentifier());
                        if (childParams != null) {
                            String parentContextProviderId = descriptor.getSourceIdentifier() + "_contextProvider"; // Konvention
                            childParams.put("contextProviderProcessor", parentContextProviderId);
                        }
                    } else { // COMPOSITION Model
                        // Der Verweis ist bereits in `processorParams` enthalten.
                        // Wir mÃ¼ssen nur sicherstellen, dass das Kind auch verarbeitet wird.
                        buildProcessorMapRecursively(childDescriptor, targetMap, model, detail);
                    }
                }
            }
        }

        // 3. Kinder  rekursiv verarbeiten
        if (descriptor.getParameterDescriptors() != null) {
            for (IParameterDescriptor paramDescriptor : descriptor.getParameterDescriptors()) {
                if (paramDescriptor.getValueDescriptor() instanceof IProcessorDescriptor) {
                    IProcessorDescriptor childDescriptor = (IProcessorDescriptor) paramDescriptor.getValueDescriptor();

                    // 4. Hierarchie-Modell anwenden
                    if (model == ConfigurationModel.INHERITANCE) {
                        buildProcessorMapRecursively(childDescriptor, targetMap, model, detail);
                        Map<String, Object> childParams = targetMap.get(childDescriptor.getSourceIdentifier());
                        if (childParams != null) {
                            String parentContextProviderId = descriptor.getSourceIdentifier() + "_contextProvider"; // Konvention
                            childParams.put("contextProviderProcessor", parentContextProviderId);
                        }
                    } else { // COMPOSITION Model
                        // Der Verweis ist bereits in `processorParams` enthalten.
                        // Wir mÃ¼ssen nur sicherstellen, dass das Kind auch verarbeitet wird.
                        buildProcessorMapRecursively(childDescriptor, targetMap, model, detail);
                    }
                }
            }
        }
    }

    /**
     * Baut die Map fÃ¼r die Deskriptor-Hierarchie selbst auf (Varianten 5-6).
     */
    private void buildDescriptorMapRecursively(
            IDescriptorProcessor descriptor,
            Map<String, Map<String, Object>> targetMap,
            MapDetailLevel detail) {

        if (descriptor == null || targetMap.containsKey(descriptor.getFullBeanId())) {
            return; // Verhindert Endlosschleifen
        }

        // 1. Serialisiere den aktuellen Deskriptor selbst
        Map<String, Object> descriptorParams = descriptor.extractEffectiveProcessorParameters();
        targetMap.put(descriptor.getFullBeanId(), descriptorParams);

        // 2. (Optional) Wenn der Detailgrad "vollstÃ¤ndig" ist, fÃ¼ge den Deskriptor des Deskriptors hinzu
        if (detail == MapDetailLevel.FULL_WITH_DESCRIPTORS) {
            IProcessorDescriptor metaDescriptor = descriptor.generatePrototypeProcessorDescriptor();
            if (metaDescriptor != null) {
                // Rekursiver Aufruf, um die Meta-Deskriptor-Map zu bauen (aber nur "schlank")
                buildDescriptorMapRecursively(metaDescriptor, targetMap, MapDetailLevel.PROCESSORS_ONLY);
                descriptorParams.put("processorDescriptor", metaDescriptor.getFullBeanId());
            }
        }

        // 3. Steige rekursiv in die Kinder des Deskriptors hinab
        if (descriptor instanceof IProcessorDescriptor) {
            buildDescriptorMapRecursively(((IProcessorDescriptor) descriptor).getParameterGroupDescriptor(), targetMap, detail);
        } else if (descriptor instanceof IParameterGroupDescriptor) {
            List<IParameterDescriptor> children = ((IParameterGroupDescriptor) descriptor).getParameterDescriptors();
            if (children != null) {
                children.forEach(child -> buildDescriptorMapRecursively(child, targetMap, detail));
            }
        } else if (descriptor instanceof IParameterDescriptor) {
            buildDescriptorMapRecursively(((IParameterDescriptor) descriptor).getValueDescriptor(), targetMap, detail);
        } else if (descriptor instanceof IValueDescriptor) {
            // IValueDescriptor can contain a value function which is also a descriptor
            if (((IValueDescriptor) descriptor).getValueFunction() instanceof IDescriptorProcessor) {
                buildDescriptorMapRecursively((IDescriptorProcessor) ((IValueDescriptor) descriptor).getValueFunction(), targetMap, detail);
            }
        }
    }
}