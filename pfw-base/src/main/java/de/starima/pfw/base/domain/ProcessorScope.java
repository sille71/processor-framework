package de.starima.pfw.base.domain;
/**
 * Der Scope eines Prozessors gibt an, wo dieser registriert wir.
 *
 */
public enum ProcessorScope {
    //der Prozessor wird stets neu erzeugt (bisheriges Verhalten, default) - er wird nicht registriert.  BeanId:  <prototypeId>:<identifier> oder <prototypeId>:<identifier>@prototype
    prototype,
    //der Prozessor wird im Parent Prozessor registriert. Er kann von anderen Prozessoren innerhalb des Parents referenziert werden. Die GÃ¼ltigkeit beschrÃ¤nkt sich also nur auf das Innenleben eines Prozessors. BeanId: <prototypeId>:<identifier>@parent
    //parent,
    //der Prozessor wird im aktuellen Kontext registriert und gilt auch nur innerhalb dieses Kontextes, es erfolgt keine Vererbung. BeanId:  <prototypeId>:<identifier>@context
    context,
    //der Prozessor wird im aktuellen Kontext registriert und kann von allen untergeordneten Kontexten abgerufen werden
    parentcontext,
    //der Prozessor wird im Root Kontext (also im Kontext der Instance) registriert und ist damit global. BeanId:  @<prototypeId>:<identifier>@instance
    instance,
    //der Prozessor muss bereits im Kontext vorhanden sein — KEINE Erzeugung. Er wird von einem früheren RunLevel bereitgestellt.
    // BeanId: <prototypeId>:<identifier>@provided
    provided,
    unknown
}