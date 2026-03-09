package de.starima.pfw.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Diese Annotation entspricht einer Bean Parameter Map, welche den ParameterDescriptorProzessor beschreibt.
 * D.h. Der Parameterdsescriptor kÃ¶nnte auch genauso gut in einer separaten Datei abgespeichert werden.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ProcessorParameter {
	public String name() default "";
	public String value() default "";
	//the possible parameter value range
	public String[] range() default {};
	//the delimiter to split the value in case of an string array
	public String delimiter() default ",";
	public String description() default "";
	//beschreibt die Typen der Art integer, string oder unions davon string|integer,...
	//Kann eine StÃ¼tze sein, um die richtige ValueFunction und ihren Werte- bzw. Definitionsbereich zu ermitteln.
	public String type() default "";
	/**
	 * Falls die Beschreibung lÃ¤nger ist, kann sie in eine Datei/Asset ausgelagert bzw. erweitert werden.
	 * Diese kann mit dem konfigurierten AssetProvider extrahiert werden.
	 * Wurde kein Name angegeben, so wird nach <prototypeIdentifier>_<Parametername>.md gesucht
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
	public String assetProviderIdentifier() default "";
	//wenn true, dann wird eine Meldung bei der Initialisierung ausgegeben, wenn der Parameterwert null ist.
	public boolean required() default false;
	//beim extrahieren der beanParameterMap wird der Parameter ignoriert
	public boolean ignoreExtractParameter() default false;
	/**
	 * Falls true, so wird der Parameter nicht in der initParameters Methode initialisiert. Z.B. contextProvider und processorDescriptor werden gesondert initialisiert!
	 */
	public boolean ignoreInitialization() default false;
	//wenn true, wird der refresh des Parameters ignoriert.
	public boolean ignoreRefresh() default false;
	public boolean isInput() default true;
	public boolean isOutput() default true;

	/**
	 * Definiert die erwarteten hierarchischen Kategorien fÃ¼r einen Prozessor oder ein Wertobjekt,
	 * das diesem Parameter zugewiesen wird. Ein zugewiesenes Objekt muss in mindestens einer
	 * der hier definierten Kategorien sein. Das Matching ist hierarchisch
	 * (z.B. "Transformator" matcht "Transformator/DeploymentArtifact").
	 */
	public String[] requiredCategories() default {};

	/**
	 * Definiert die erwarteten hierarchischen Sub-Kategorien.
	 */
	public String[] requiredSubCategories() default {};

	/**
	 * Definiert eine Liste von Tags, die ein Prozessor oder Wertobjekt haben muss,
	 * um fÃ¼r diesen Parameter gÃ¼ltig zu sein. Ein zugewiesenes Objekt muss alle
	 * hier definierten Tags besitzen (UND-VerknÃ¼pfung).
	 */
	public String[] requiredTags() default {};

	/**
	 * Wenn auf 'true' gesetzt, wird dieses Feld als Teil des
	 * zusammengesetzten SchlÃ¼ssels zur Identifizierung einer @ValueObject-Instanz verwendet.
	 * Dies wird nur berÃ¼cksichtigt, wenn das ValueObject nicht das IArtifact-Interface implementiert.
	 */
	boolean key() default false;

	public String[] tags() default {}; // Beibehaltung fÃ¼r allgemeine Zwecke, aber requiredTags ist spezifischer

	/**
	 * Definiert eine Liste von alternativen oder veralteten Namen fÃ¼r diesen Parameter.
	 * <p>
	 * Dies ist der SchlÃ¼ssel zur AbwÃ¤rtskompatibilitÃ¤t. Wenn eine Konfiguration
	 * einen der hier definierten Alias-Namen verwendet, wird er vom Framework
	 * korrekt diesem Feld zugeordnet. Bei der Extraktion oder Serialisierung
	 * wird jedoch immer der aktuelle Feldname verwendet, wodurch Konfigurationen
	 * quasi "automatisch" migriert werden.
	 *
	 * @return Ein Array von alternativen Parameternamen.
	 */
	String[] aliases() default {};

	/**
	 * Falls die Beschreibung lÃ¤nger ist, kann sie in eine Datei ausgelagert werden.
	 * Diese wird unter resources im gleichen Pfad wie der zugehÃ¶rige Prozessor abgelegt.
	 * Wurde kein Name angegeben, so wird nach <Parametername.md> gesucht
	 */
	public String descriptionFileName() default "";

	public String parameterDescriptorIdentifier() default "";
	public String parameterDescriptorPrototypeIdentifier() default "";

	@Deprecated
	public String parameterFunctionProcessorIdentifier() default "";
	/**
	 * Der prototype identifier eines IParameterFunctionProcessor, um den gegebenen Parameterwert zu transformieren.
	 * Das kann hilfreich sein, wenn komplexere Objekte gebaut werden mÃ¼ssen oder z.B. bestimmte Domainobjekte aus json zu erzeugen sind.
	 * In manchen FÃ¤llen kann aber auch ein konkreter ParameterProvider fÃ¼r den betroffenen Prozessor implementiert werden. Damit ist aber immer
	 * ein neuer Kontext verbunden.
	 *
	 */
	@Deprecated
	public String parameterFunctionProcessorPrototypeIdentifier() default "";

    String valueFunctionIdentifier() default "";
    String valueFunctionPrototypeIdentifier() default "";

	public String parameterDomainProcessorPrototypeIdentifier() default "";

	public String parameterDomainProcessorIdentifier() default "";

	// =========================================================================
	// NEU: Rollen-Markierungen für Framework-relevante Parameter
	// =========================================================================

	/**
	 * Markiert diesen Parameter als den ContextProvider des Prozessors.
	 *
	 * <p>Der {@code IContextProviderResolver} sucht in der Prozessorklasse nach
	 * dem Feld, das mit {@code contextProvider = true} annotiert ist, statt
	 * nach einem fest codierten Parameternamen zu suchen.
	 *
	 * <p>Dadurch kann jeder Prozessor seinen ContextProvider-Parameter frei
	 * benennen — das Framework findet ihn über die Annotation.
	 *
	 * <p>Beispiel:
	 * <pre>
	 * &#64;ProcessorParameter(contextProvider = true, ignoreInitialization = true)
	 * private IRuntimeContextProviderProcessor contextProviderProcessor;
	 * </pre>
	 *
	 * <p>In der beanParameterMap wird weiterhin der Feldname (oder {@code name()})
	 * als Schlüssel verwendet. Der Resolver nutzt diese Markierung nur zur
	 * Feldsuche, nicht zur Umbenennung.
	 */
	boolean contextProvider() default false;

	/**
	 * Markiert diesen Parameter als den ProcessorDescriptor.
	 *
	 * <p>Analog zu {@code contextProvider()} — der Descriptor-Incubator sucht
	 * das Feld mit dieser Markierung, um den Descriptor zu injizieren.
	 *
	 * <p>Beispiel:
	 * <pre>
	 * &#64;ProcessorParameter(processorDescriptor = true, ignoreInitialization = true)
	 * private IProcessorDescriptor processorDescriptor;
	 * </pre>
	 */
	boolean processorDescriptor() default false;
}