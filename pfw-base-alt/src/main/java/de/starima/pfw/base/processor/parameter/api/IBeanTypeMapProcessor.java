package de.starima.pfw.base.processor.parameter.api;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.api.IProcessor;

import java.util.Map;

/**
 * Dieser Prozessor kommt dann zum Einsatz, wenn die gesuchte beanId nicht bekannt ist, sondern nur der Prozessor Typ.
 * (siehe DeploymentInstanz, hier ist zunÃ¤chst nicht erkennbar, ob es sich bei dem vorliegenden File um ein Recon, Cluster oder GlobalConfig Deployment handelt.
 * Im File ist aber ein IBeanTypeMapProcessor definiert, dessen Name bekannt ist. AuÃŸerdem ist der Typ "deployment"
 * bekannt. Wir kennen also jemanden, der den Deployment Prozessor genauer kennt.)
 * Dieser Prozessor ersetzt kÃ¼nftig die beanTypeMap aus der ReconKonfiguration. Damit kÃ¶nnen wir die gesamte alte Recon Konfiguration
 * als Parameter Map abbilden!
 */
@Processor
public interface IBeanTypeMapProcessor extends IProcessor {
    public String getBeanIdForType(String type);
    public Map<String, String> getBeanIdTypeMap();
    public Map<String, String> getContextMergedBeanIdTypeMap();
    public Map<String, String> getBeanIdTypeMapFromParameterMap(Map<String, Map<String, Object>> parameterMap);
    public Map<String, String> getContextMergedBeanIdTypeMapFromParameterMap(Map<String, Map<String, Object>> parameterMap);
}