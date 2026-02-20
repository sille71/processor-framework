package de.starima.pfw.base.processor.description;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.context.api.ITransformationContext;
import de.starima.pfw.base.processor.context.domain.DefaultTransformationContext;
import de.dzbank.recon.ms.base.processor.description.api.*;
import de.starima.pfw.base.processor.description.api.IDescriptorProcessor;
import de.starima.pfw.base.processor.description.api.IMapValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueDescriptor;
import de.starima.pfw.base.processor.description.api.IValueFunction;
import de.starima.pfw.base.processor.set.api.ISetProcessor;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter @Setter
@Slf4j
@Processor(
        description = "Transformiert eine Map von rohen Key-Value-Paaren in eine Map von aufgelÃ¶sten Objekten.",
        categories = {"function/value/map"}