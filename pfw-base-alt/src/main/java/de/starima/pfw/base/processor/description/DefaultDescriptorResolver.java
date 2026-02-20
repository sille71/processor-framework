package de.starima.pfw.base.processor.description;

import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ValueObject;
import de.starima.pfw.base.processor.AbstractProcessor;
import de.starima.pfw.base.processor.context.domain.DefaultDescriptorConstructorContext;
import de.starima.pfw.base.processor.description.api.IDescriptorResolver;
import de.starima.pfw.base.util.ProcessorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Processor(description = "Sammelt und liefert die Konfigurationen aller beschreibbaren Kandidaten (@Processor, @ValueObject).")
public class DefaultDescriptorResolver extends AbstractProcessor implements IDescriptorResolver, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<Map<String, Map<String, Object>>> findAllCandidateDescriptorMaps() {
        if (applicationContext == null) {
            log.error("ApplicationContext is not set in DefaultDescriptorResolver. Cannot find candidate descriptor maps.");
            return new ArrayList<>();
        }

        Map<String, Object> processorBeans = applicationContext.getBeansWithAnnotation(Processor.class);
        Map<String, Object> valueObjectBeans = applicationContext.getBeansWithAnnotation(ValueObject.class);

        return Stream.concat(
                processorBeans.values().stream().map(this::generateDescriptorMap),
                valueObjectBeans.values().stream().map(this::generateDescriptorMap)
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private Map<String, Map<String, Object>> generateDescriptorMap(Object bean) {
        DefaultDescriptorConstructorContext context = new DefaultDescriptorConstructorContext();
        context.setSourceType(bean.getClass());
        Map<String, Map<String, Object>> bluePrint = new HashMap<>();
        ProcessorUtils.generateDescriptorBlueprint(context, bluePrint);
        return bluePrint;
    }

    @Override
    public boolean matchesCategories(String[] offeredCategories, String[] requiredCategories) {
        if (requiredCategories == null || requiredCategories.length == 0) {
            return true;
        }
        if (offeredCategories == null || offeredCategories.length == 0) {
            return false;
        }

        for (String reqCategory : requiredCategories) {
            for (String offCategory : offeredCategories) {
                if (offCategory.startsWith(reqCategory)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean matchesTags(String[] offeredTags, String[] requiredTags) {
        if (requiredTags == null || requiredTags.length == 0) {
            return true;
        }
        if (offeredTags == null || offeredTags.length == 0) {
            return false;
        }
        List<String> offeredTagsList = Arrays.asList(offeredTags);
        return offeredTagsList.containsAll(Arrays.asList(requiredTags));
    }

    @Override
    public boolean matchesSubCategories(String[] offeredSubCategories, String[] requiredSubCategories) {
        if (requiredSubCategories == null || requiredSubCategories.length == 0) {
            return true;
        }
        if (offeredSubCategories == null || offeredSubCategories.length == 0) {
            return false;
        }
        for (String reqSubCategory : requiredSubCategories) {
            for (String offSubCategory : offeredSubCategories) {
                if (offSubCategory.startsWith(reqSubCategory)) {
                    return true;
                }
            }
        }
        return false;
    }
}