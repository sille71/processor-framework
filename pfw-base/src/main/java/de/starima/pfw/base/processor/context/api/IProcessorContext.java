package de.starima.pfw.base.processor.context.api;

/**
 * Basiskontext (der eigentliche RuntimeContext) in dem die gesamte Anwendung lÃ¤uft. Wird vom ContextProviderProcessor erzeugt.
 * TODO: evtl. in RuntimeContext umbenennen.
 */

import de.starima.pfw.base.processor.api.IProcessorProvider;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.processor.parameter.api.IParameterProviderProcessor;

import java.util.Locale;
import java.util.Map;
public interface IProcessorContext {
	public String getName();

	public void setName(String name);

	public IProcessorContext getParentContext();

	public void setParentContext(IProcessorContext parentContext);

	public Map<String, Map<String, Object>> getBeanParameterMap();

	public Map<String, Map<String, Object>> getContextMergedBeanParameterMap();

	public void setBeanParameterMap(Map<String, Map<String, Object>> beanParameterMap);

	public Map<String, Map<String, Object>> refreshBeanParameterMap(Map<String, Map<String, Object>> beanParameterMap);

	/**
	 * Die BeanTypeMap ist dann gefordert, wenn keine beanIds bekannt sind, sondern nur der Typ des gewÃ¼nschten Processors.
	 * So ist beispielsweise beim Deployment nicht klar, welche Konfiguration die gegebene Datei enthÃ¤lt
	 * (global Konfig, Cluster Konfig oder Recon Konfig). Es kann aber nach einem bean vom typ deployment in der Konfig gesucht werden.
	 * Aus der BeanTypMap erhÃ¤lt man dann den dort hinterlegten Deploymentprozessor. (globalCfgDeploymentProcessor, c
	 * lusterCfgDeploymentProcessor, clusterCfgDeploymentProcessor)
	 * @return
	 */
	public Map<String, String> getBeanIdTypeMap();

	public void setBeanIdTypeMap(Map<String, String> beanIdTypeMap);

	/**
	 * Liefert die vereinigte Bean Typ Map des aktuellen und der Eltern Kontexte. Dabei kÃ¶nnen Typdefinitionen
	 * im Elternkontext Ã¼berschrieben werden.
	 * @return
	 */
	public Map<String, String> getContextMergedBeanIdTypeMap();

	/**
	 * Liefert die bean Id zu einem gegebenen Typ aus der Map in diesem Kontext.
	 * @param type
	 * @return
	 */
	public String getBeanIdForType(String type);

	public String getContextMergedBeanIdForType(String type);

	/**
	 * Liefert die Parameter zu einer gegebenen beanId aus der Map in diesem Kontext.
	 * @param beanId
	 * @return
	 */
	public Map<String, Object> getBeanParameters(String beanId);

	public void addBeanParameters
            (String beanId, Map<String, Object> parameters);

	public void addBeanIdType(String type, String beanId);

	public void removeBeanParameters(String beanId);

	/**
	 * Liefert die Parameter zu einer gegebenen beanId. Dabei werden die Parameter zu dieser beanId der Eltern Kontexte verschmolzen.
	 * Wurden Parameter im Eltern Kontext definiert, dann kÃ¶nnen diese im Kind Kontext Ã¼berschrieben werden.
	 * So kÃ¶nnen auch Parameter erreicht werden, die nicht im Kind Kontext definiert wurden.
	 * @param beanId
	 * @return
	 */
	public Map<String, Object> getContextMergedBeanParameters(String beanId);

	public IRuntimeContextProviderProcessor getContextProviderProcessor();

	public void setContextProviderProcessor(IRuntimeContextProviderProcessor contextProviderProcessor);

	public IParameterProviderProcessor getParameterProviderProcessor();

	public IProcessorProvider getProcessorProvider();

	public Map<String, IProcessor> getBeanIdProcessorMap();

	public void setBeanIdProcessorMap(Map<String, IProcessor> beanIdProcessorMap);

	public void addProcessor(IProcessor processor);

	public void addProcessorToParentContext(IProcessor processor);

	public void removeProcessor(IProcessor processor);

	public IProcessor getProcessor(String identifier);

	public IProcessor findProcessor(String contextIdentifier, String processorIdentifier);

	public IProcessor getProcessorFromHierarchy(String beanId);

	public void addReconContext(IProcessorContext ctx);

	public void removeReconContext(IProcessorContext ctx);

	public IProcessorContext getChildContext(String identifier);

	public IProcessorContext getRootContext();

	public IProcessorContext findContext(String identifier);

	public IProcessor getInitializedProcessor();

	public void setInitializedProcessor(IProcessor processor);

	public boolean isInitializerContext();

	public boolean isFinalContext();

	public IProcessorContext getFinalContext();

	public IProcessorContext cleanUpInitializerContext(IProcessorContext finalContext);

	//<T> T getContextArtifact(Class<T> clazz);

	//TODO: werden die folgenden Methoden im Kontext benÃ¶tigt? Dies ist zu prÃ¼fen und evtl. zurÃ¼ckzubauen.
	public Locale getLocale();

	//public Recon getRecon();

	//public Recon getOldRecon();

	//public void setRecon(Recon recon);

	//public ReconResult getReconResult();

	//public ReconResult getOldReconResult();

	//public void setReconResult(ReconResult reconResult);

	//public String getResultSuffix();

	//public ReconConfiguration getReconConfiguration();
}