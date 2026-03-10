package de.starima.pfw.base.processor.transformator.api;

import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;
import de.starima.pfw.base.util.ConfigurationModel;
import de.starima.pfw.base.util.MapDetailLevel;
import de.starima.pfw.base.util.SerializationTarget;

import java.util.Map;

public interface IConfigurationModelTransformer extends IProcessor {

    /**
     * Analysiert die Struktur einer beanParameterMap und erkennt das zugrundeliegende Modell.
     */
    ConfigurationModel detectModel(Map<String, Map<String, Object>> beanParameterMap);

    /**
     * Parst eine beanParameterMap und baut daraus die kanonische, hierarchische
     * IProcessorDescriptor-Struktur auf.
     */
    IProcessorDescriptor parse(String rootProcessorIdentifier, Map<String, Map<String, Object>> beanParameterMap, IProcessorContext context);

    /**
     * Serialisiert eine kanonische IProcessorDescriptor-Hierarchie in eine
     * flache beanParameterMap, basierend auf dem gewÃ¼nschten Ziel, Modell und Detaillierungsgrad.
     */
    Map<String, Map<String, Object>> createBeanParameterMap(
            IProcessorDescriptor rootDescriptor,
            SerializationTarget target,
            ConfigurationModel model,
            MapDetailLevel detail
    );
}