package de.starima.pfw.base.processor.locale.api;

import de.starima.pfw.base.processor.api.IProcessor;

import java.util.Locale;

public interface ILocalProviderProcessor extends IProcessor {
    public Locale getLocaleForSubjectReference(String itemReference);

    /**
     * Liest das Locale aus dem aktuellen Processor Kontext.
     * @return
     */
    public Locale getContextLocal();

    /**
     * Liefert das Locale, welches einem bestimmten subject zugeordnet wurde (beispielsweise aus einem RcnAttribute)
     * @param subject
     * @return
     */
    public Locale getLocaleForSubject(Object subject);

    /**
     * Das Processor eigene Locale falls konfiguriert. Oder es wurde ein subject im Kontext hinterlegt.
     * @return
     */
    public Locale getLocale();
}