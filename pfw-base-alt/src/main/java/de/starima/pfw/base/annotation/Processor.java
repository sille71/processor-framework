package de.starima.pfw.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public @interface Processor {
	/**
	 * Hier steht eine Kurzbeschreibung des Prozessors. Falls es mehr zu sagen gibt, so kann das in descriptionFileName erzÃ¤hlt werden.
	 * Diese Beschreibung prÃ¤sentiert ReconLight. Falls der Nutzer im ReconLigth mehr erfahren will, so kann er descriptionFileName abfragen.
	 * @return
	 */
	public String description() default "";

	/**
	 * Falls die Beschreibung lÃ¤nger ist, kann sie in eine Datei/Asset ausgelagert bzw. erweitert werden.
	 * Diese kann mit dem konfigurierten AssetProvider extrahiert werden.
	 * Wurde kein Name angegeben, so wird nach <Klassenname.md> gesucht
	 */
	public String descriptionAssetName() default "";

	/**
	 * Der mimeType der Beschreibungsdatei /asset.
	 * @return
	 */
	public String descriptionAssetMimetype() default "";

	/**
	 * Der Processor Identifier des IAssetProviders.
	 * @return
	 */
	public String assetProviderIdentifier() default "documentAssetProviderProcessor";

	/**
	 * Liste von parameterName|value Paaren. Damit kann der value() in der ProcessorParameter Annotation Ã¼berschrieben werden.
	 * Das macht in Subklassen Sinn, die den default Wert der Super Klasse Ã¼berschreiben sollen.
	 * Zu beachten ist, das hier der Parametername angegeben wird, wie er in der ProcessorParameter Annotation des Feldes definiert ist (in der Regel wird dies der Propertyname sein)
	 * @return
	 */
	public String[] defaultValues() default {};

	/**
	 * Verweist auf eine json Datei unter resources/defaults/processors, welche eine default beanParameterMap fÃ¼r den entsprechenden Prozessor bereitstellt.
	 * Damit kÃ¶nnen auch komplexere default Werte abgebildet werden.
	 * @return
	 */
	public String defaultBeanParameterMapFileName() default "";

	/**
	 * Definiert eine oder mehrere hierarchische Kategorien, denen der Prozessor zugeordnet ist.
	 * Dient der primÃ¤ren technischen Klassifizierung (z.B. "Transformator/DeploymentArtifact").
	 * Ein Prozessor kann mehrere Rollen erfÃ¼llen und daher in mehreren Kategorien sein.
	 */
	public String[] categories() default {};

	/**
	 * Definiert eine oder mehrere hierarchische Sub-Kategorien zur Querschnittsklassifizierung.
	 * Diese sind unabhÃ¤ngig von den Hauptkategorien und beschreiben Aspekte wie "performanceOptimized",
	 * "experimental", "deprecated" oder "bankspezifisch".
	 */
	public String[] subCategories() default {};

	/**
	 * Definiert eine Liste von flexiblen, nicht-hierarchischen Tags fÃ¼r ad-hoc Filterung.
	 * z.B. {"database-write", "ui-component", "security-relevant"}
	 */
	public String[] tags() default {};

	public String explicitProcessorDescriptorFileName() default "";

	/**
	 * der zustÃ¤ndige Descriptor kann explizit angegeben werden.
	 * @return
	 */
	public String descriptorPrototypeIdentifier() default "defaultProcessorDescriptor";

	/**
	 * @deprecated
	 * @return
	 */
	public String processorDescriptorIdentifier() default "";
	/**
	 * @deprecated
	 * @return
	 */
	public String processorDescriptorPrototypeIdentifier() default "";

	/**
	 * @deprecated
	 * @return
	 */
	public String parameterGroupDescriptorIdentifier() default "";
	/**
	 * @deprecated
	 * @return
	 */
	public String parameterGroupDescriptorPrototypeIdentifier() default "";
}