package de.starima.pfw.base.processor.parameter;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.migration.domain.configuration.ReconConfiguration;
import de.starima.pfw.base.processor.migration.api.IConfigurationMigrationProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Processor
public class StringConfigurationParameterProvider extends AbstractParameterProviderProcessor{

    public static Map<String, Map<String, Object>> createBeanParameterMap(ReconConfiguration reconConfiguration, ReconConfiguration clusterConfiguration, ReconConfiguration globalConfiguration) {
        HashMap<String, Map<String, Object>> beanParameterMap = new HashMap<>();
        beanParameterMap.put("stringReconConfigurationParameterProvider", createConfigStringParameter(reconConfiguration, "recon"));
        beanParameterMap.put("stringClusterConfigurationParameterProvider", createConfigStringParameter(clusterConfiguration, "cluster"));
        beanParameterMap.put("stringGlobalConfigurationParameterProvider", createConfigStringParameter(globalConfiguration, "global"));

        return beanParameterMap;
    }

    private static Map<String, Object> createConfigStringParameter(ReconConfiguration cfg, String type) {
        Map<String, Object> parameter = new HashMap<>(1);
        parameter.put("configString", ProcessorUtils.getConfigurationAsString(cfg));
        parameter.put("reconContextType", type);
        return parameter;
    }
    @ProcessorParameter(value = "configurationMigrationProcessorChain")
    private IConfigurationMigrationProcessor configurationMigrationProcessor;
    @ProcessorParameter(range = {"recon", "cluster", "global"}, value = "recon")
    private String reconContextType;
    @ProcessorParameter(description = "Dieser Parameter wird Ã¼blicherweise dynamisch als beanParameterMap in der Methode createProcessor(...) gesetzt.")
    private String configString;

    @Override
    public Map<String, Map<String, Object>> getBeanParameterMap() {
        return configurationMigrationProcessor != null ? configurationMigrationProcessor.transformValue(ReconContextType.valueOf(reconContextType.trim().toUpperCase()), configString) : new HashMap<>();
    }
}