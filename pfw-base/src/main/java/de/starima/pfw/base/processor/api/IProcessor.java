package de.starima.pfw.base.processor.api;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.domain.ProcessorScope;
import de.starima.pfw.base.processor.context.api.IRuntimeContextProviderProcessor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.context.api.LoadStrategy;
import de.starima.pfw.base.processor.description.api.IProcessorDescriptor;

import java.util.List;
import java.util.Map;

@Processor
public interface IProcessor {
    void init(IProcessorContext ctx) throws Exception;
	public void refreshParameters(Map<String,Map<String, Object>> beanParameterMap);
	String getIdentifier();
	void setIdentifier(String identifier);

	ProcessorScope getScope();
	void setScope(ProcessorScope scope);
	String getProtoTypeIdentifier();
	String getFullBeanId();
	public IProcessorDescriptor getProcessorDescriptor();
	public IRuntimeContextProviderProcessor getContextProviderProcessor();
	public IProcessorContext getRuntimeContext();
	public void setRuntimeContext(IProcessorContext ctx);

	public void processorOnInit();

	public void processorOnRefresh();

	/**
	 * Lifecycle-Hook für Shutdown und Refresh.
	 * Wird aufgerufen bevor der Prozessor deregistriert oder ersetzt wird.
	 */
	public void processorOnDestroy();

	/**
	 * Initialisiert eine mÃ¶gliche Default beanParameterMap. Diese kann entweder direkt im Java Code verankert werden, oder sie wir in einer json Datei unter
	 * resources/defaults/processors abgelegt. Der Name der json Datei kann in der Prozessor Annotation unter defaultBeanParameterMapFileName() definiert werden.
	 * Wird kein Name angegeben, so wird der kleingeschriebene Klassenname verwendet.
	 * @return
	 */
	public Map<String,Map<String,Object>>  initDefaultBeanParameterMap();

	//TODO: machen solche Methoden kÃ¼nftig Sinn?
	//public void initProcessorParameters(Map<String, Object> parameters);
	//public void initProcessorParameters(ReconContext ctx, Map<String, Object> parameters);

	public Map<String, Object> extractEffectiveProcessorParameters();

	public Map<String,Map<String, Object>> extractEffectiveProcessorParameterMap();

	public IProcessorDescriptor generatePrototypeProcessorDescriptor();
	public IProcessorDescriptor generateProcessorDescriptorInstance(LoadStrategy loadStrategy);

	public  <T extends IProcessor> T  createProcessor(Class<T> clazz, String processorIdentifier, String processorType, IProcessor parentProcessor, List<Map<String,Map<String, Object>>> beanParameterMaps);
	public  <T extends IProcessor> T  createProcessor(Class<T> clazz, String processorIdentifier, String processorType, List<Map<String,Map<String, Object>>> beanParameterMaps);

	//public IProcessor getParentProcessor();
	//public void setParentProcessor(IProcessor parentProcessor);
	//public IInstanceProcessor getInstanceProcessor();

	/**
	 * Liefert einen Prozessor aus der lokalen Registry dieses Prozessors.
	 * @param beanid
	 * @return
	 */
	//public IProcessor getProcessorForBeanId(String beanid);

	/**
	 * Ist der Prozessor ein Parameter, so hat er eine ParameterFunction, die ihn erzeugt hat, wenn die Initialisierung durch einen Prozessordescriptor erfolgt ist.
	 * Der ProcessorDescriptor und ContextProvider werden nicht durch eine ParameterFunction erzeugt.
	 * Damit lÃ¤sst sich die ProzessorParameterHierarchie aufbauen (wichtig fÃ¼r den ReconLight).
	 * @return
	 */
	//public IParameterFunctionProcessor getCreatorParameterFunctionProcessor();
	//public void setCreatorParameterFunctionProcessor(IParameterFunctionProcessor parameterFunctionProcessor);
}