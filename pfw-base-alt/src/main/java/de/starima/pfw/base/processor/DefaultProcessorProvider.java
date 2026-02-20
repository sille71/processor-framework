package de.starima.pfw.base.processor;

import de.dzbank.components.utils.log.LogOutputHelper;
import de.dzbank.recon.components.base.processors.api.IReconProcessorLifecycleListener;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.processor.context.api.IProcessorContext;
import de.starima.pfw.base.processor.api.IProcessorProvider;
import de.starima.pfw.base.processor.api.IProcessor;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter @Setter @SuperBuilder
@NoArgsConstructor
@Processor
public class DefaultProcessorProvider extends DefaultBeanProvider implements IProcessorProvider {
	@Builder.Default private List<IReconProcessorLifecycleListener> reconProcessorLifecycleListeners = new ArrayList<>();

	public void addReconProcessorLifecycleListener(IReconProcessorLifecycleListener listener) {
		reconProcessorLifecycleListeners.add(listener);
	}

	public void removeReconProcessorLifecycleListener(IReconProcessorLifecycleListener listener) {
		reconProcessorLifecycleListeners.remove(listener);
	}

	@Override
	public <T extends IProcessor> T getProcessorForType(Class<T> clazz, String processorType, IProcessorContext ctx, IProcessor parentProcessor) {
		log.trace("getProcessor({}, {})", processorType, ctx);
		try {
			@SuppressWarnings("unchecked")
			T processor = (T) getProcessorForType(processorType, ctx, parentProcessor);
			if (processor == null) {
				log.debug("Failed: getProcessorForType({}): No processor found for type {}", ctx, processorType);
			}
			return processor;
		} catch (ClassCastException e) {
			String msg = "Failed: getProcessorForType(" + ctx + "): Msg " + e.toString();
			log.error(msg);
			throw new IllegalArgumentException(msg);
		}
	}

	@Override
	public <T extends IProcessor> T getProcessorForBeanId(Class<T> clazz, String identifier, IProcessorContext ctx, IProcessor parentProcessor) {
		log.trace("Try to get processor {} of type {} for context {}",identifier, clazz.getName(), ctx);

		return getProcessorForBeanIdWithType(clazz, identifier, null, ctx, parentProcessor);
	}

	@Override
	public <T extends IProcessor> T getProcessorForBeanIdWithType(Class<T> clazz, String beanId, String type, IProcessorContext ctx, IProcessor parentProcessor) {
		log.trace("Try to get processor {} of class {} for type {}",beanId, clazz.getName(), type);

		try {
			@SuppressWarnings("unchecked")
			T processor = (T) this.getProcessorForBeanIdWithType(beanId, type, ctx, parentProcessor);

			if (processor != null)
				log.debug("Successfully created processor {} of class {} for type {}", beanId, clazz.getName(), type);
			return processor;
		} catch (ClassCastException ce) {
			log.warn("Bean with id {} is not of class {} for type {}", beanId, clazz.getName(), type);
		} catch (Exception e) {
			log.error("Can not create bean for id {} of class {} for type {}!", beanId, clazz.getName(), type, e);
		}

		return null;
	}

	public boolean isProcessorCreationPossible(String beanid, IProcessorContext ctx, IProcessor parentProcessor) {
		try {
			return ProcessorUtils.isProcessorCreationPossible(ctx,parentProcessor, (IProcessor) getBeanForId(ProcessorUtils.getPrototypeId(beanid)));
		} catch (Exception e) {
			log.debug("{}.isProcessorCreationPossible() failed: {} is probably not a prototyp id!",getIdentifier(), ProcessorUtils.getPrototypeId(beanid));
			return false;
		}
	}
	@Override
	public IProcessor getProcessorForBeanId(String beanid, IProcessorContext ctx, IProcessor parentProcessor) {
		log.debug("getProcessorForBeanId({}, {})", beanid, ctx);

		return getProcessorForBeanIdWithType(beanid, null, ctx, parentProcessor);
	}

	public IProcessor getProcessorForBeanIdWithType(String beanid, String type, IProcessorContext ctx, IProcessor parentProcessor) {
		log.debug("getProcessorForBeanIdWithType({}, {})", beanid, type);

		try {
			// Note: the beanId may consist of the prototype bean name and the
			// real bean identifier separated by colon
			String identifier = ProcessorUtils.getIdentifier(beanid);
			String prototypeId = ProcessorUtils.getPrototypeId(beanid);
			ProcessorScope scope = ProcessorUtils.getProcessorScope(beanid);
			IProcessor processor = ProcessorUtils.getProcessorFromScope(beanid, ctx);

			if (processor != null) {
				log.info("Found processor {} in scope {} and context {}", processor.getFullBeanId(), processor.getScope().name(), ctx.getName());
				return processor;
			}

			try {
				processor = (IProcessor) getBeanForId(prototypeId);
			} catch (Exception e) {
				log.debug("{} is probably not a prototyp id!", prototypeId);
			}

			// may be the beanid is just the identifier without the prototypeId or an processor type
			// in this cases we ask the processor type map
			if (processor == null && prototypeId.equals(identifier)) {
				log.debug("{} may be the identifier without the prototypeId or an processor type! In this cases we ask the processor type map",
						prototypeId);
				processor = getProcessorForType(prototypeId, ctx, parentProcessor);
			} else if (processor != null) {
				// set the internal identifier used to get the bean
				// parameters
				processor.setIdentifier(identifier);
				processor.setScope(scope);


				if (ProcessorUtils.isProcessorCreationPossible(ctx, parentProcessor, processor)) {
					//processor.setParentProcessor(parentProcessor);

					//der Prozessor wird bei dieser Reihenfolge des Aufrufes in ctx registriert.
					//und nicht im context der mÃ¶glicherweise nach der init Methode entsteht!
					//ProcessorUtils.registerProcessorInScope(processor, ctx);
					processor.init(ctx);
					ProcessorUtils.registerProcessorWithType(processor, type, processor.getRuntimeContext());

					log.debug("Success: getProcessorForBeanId({}, {}): Created  processor [id, class] = [{} : {}]",
							beanid, ctx, processor.getIdentifier(), processor.getClass().getCanonicalName());
				} else {
					log.info("can not create processor {} in parent {}",processor.getFullBeanId(), parentProcessor);
					processor = null;
				}
			} else {
				log.debug("Failed: getProcessorForBeanId({}, {}) No processor found!", beanid, ctx);
			}

			// this.reconProcessorCreated(processor);

			return processor;
		} catch (Exception e) {
			log.warn("Failed: getProcessorForBeanIdWithType({}, {}): Msg {}", beanid, ctx, e);
		}

		return null;
	}

	@Override
    public IProcessor getProcessorForType(String processorType, IProcessorContext ctx, IProcessor parentProcessor) {
		log.trace("getProcessorForType({}, {})", processorType, ctx);

		try {
			String beanId = ctx.getContextMergedBeanIdForType(processorType);

			if (beanId != null) {
				IProcessor processor = getProcessorForBeanId(beanId, ctx, parentProcessor);
				if (processor != null) {
					//processor.init(ctx);
					// set the real identifier if there is one
					log.debug("Success: getProcessorForType({}, {}): Created  processor [id, class] = [{} : {}]",
							processorType, ctx, processor.getIdentifier(), processor.getClass().getCanonicalName());
				} else {
					log.trace("Failed: getProcessorForType({}, {}) No processor found!", processorType, ctx);
				}
				return processor;
			} else {
				log.trace("Failed: getProcessorForType({}, {}) No beanId defined!", processorType, ctx);
			}

		} catch (Exception e) {
			log.error("Failed: getProcessorForType({}, {}): Msg {}", processorType, ctx, e.toString());
		}

		return null;
	}

	// // informiert die Listener
	// private void reconProcessorCreated(IReconProcessor processor) {
	// 	if (reconProcessorLifecycleListeners != null) {
	// 		for (IReconProcessorLifecycleListener listener: reconProcessorLifecycleListeners) {
	// 			listener.processorCreated(processor);
	// 		}
	// 	}
	// }
}