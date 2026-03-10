package de.starima.pfw.base.processor.description.config;

import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.domain.DefaultTaskContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigCreationContext;
import de.starima.pfw.base.processor.description.config.api.IDescriptorConfigProvider;
import de.starima.pfw.base.processor.description.config.api.ITypeRefProvider;
import de.starima.pfw.base.processor.description.config.api.IValueConfig;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

@Getter
@Setter
@Accessors(chain = true) // Behalten wir fÃ¼r die flÃ¼ssige API bei
@ToString(callSuper = true) // WICHTIG: Bezieht die toString()-Methode der Elternklasse mit ein
@EqualsAndHashCode(callSuper = true) // WICHTIG: Bezieht die Felder der Elternklasse in equals() und hashCode() mit ein
public class DefaultDescriptorConfigCreationContext extends DefaultTaskContext implements IDescriptorConfigCreationContext {
    private Type typeToResolve;
    private Object objectToResolve;
    private Field fieldToResolve;
    private ProcessorParameter processorParameter;
    private IDescriptorConfigProvider rootProvider;
    private ITypeRefProvider typeRefRootProvider;
    private IValueConfig valueConfigToResolve;


    public DefaultDescriptorConfigCreationContext(IDescriptorConfigCreationContext parent) {
        super(parent);
        this.rootProvider = parent.getRootProvider();
        this.typeRefRootProvider = parent.getTypeRefRootProvider();
    }

}