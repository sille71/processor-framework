package de.starima.pfw.base.processor.parameter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.starima.pfw.base.annotation.Processor;
import de.starima.pfw.base.annotation.ProcessorParameter;
import de.starima.pfw.base.processor.parameter.api.IParameterChangeListener;
import de.starima.pfw.base.util.MapUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter @Setter
@Processor
public class FileSystemParameterProviderProcessor extends AbstractParameterProviderProcessor {

    @ProcessorParameter(value = "parameters", description = "Verzeichnis unter APPL_CFG in dem nach Parameterkonfigurationen gesucht wird.")
    protected String cfgDirName = "parameters";

    /**
     * Diese Datei enthÃ¤lt die Konfiguration fÃ¼r mehrere Parameter Maps.
     * Die Konfiguration liegt in key, value Form vor, wobei der key ein identifier und value eine Parameter Map im Format wie unter parameterValueMapFileName ist.
     */
    @ProcessorParameter(description = "Diese Datei enthÃ¤lt die Konfiguration fÃ¼r mehrere Parameter Maps.\n" +
            "Wird kein File Name definiert, so ergibt sich der Name aus dem prefix beanParameters_ plus aktuellen Processor identifier plus dem suffix .json.\n" +
            "Die Konfiguration liegt in key, value Form vor, wobei der key ein identifier und value eine Parameter Map im Format wie unter parameterValueMapFileName ist." +
            "Der identifier kann hier einen Prozessoridentifier beschreiben oder einen anderen Parameterraum wie beispielsweise ein ftl Template.")
    private String beanParameterValueMapFileName = null;

    private List<IParameterChangeListener> parameterChangeListeners;

    public String getBeanParameterValueMapFileName() {
        if (beanParameterValueMapFileName == null) {
            String prefix = "beanParameters_";
            beanParameterValueMapFileName = prefix.concat(getIdentifier()).concat(".json");
        }
        return beanParameterValueMapFileName;
    }

    protected File loadParameterMapFile() {
        try {
            File mapFile = new File(this.applicationCfgDir, getCfgDirName());

            mapFile = new File(mapFile, getBeanParameterValueMapFileName());

            if (!mapFile.exists()) {
                log.debug("create file {}", mapFile.getAbsolutePath());
                try {
                    mapFile.createNewFile();
                } catch (IOException e) {
                    log.error("Can not create parameter file {} Msg: {}", mapFile.getAbsolutePath(), e.toString());
                    return null;
                }
            }

            return mapFile;
        } catch (Exception e) {
            log.error("Can not get parameter file {}/{} Msg: {}", this.applicationCfgDir, getCfgDirName(), e.toString());
        }

        return null;
    }

    @Override
    public void addParameterChangeListener(IParameterChangeListener listener) {
        if (parameterChangeListeners == null) parameterChangeListeners = new ArrayList<>();
        parameterChangeListeners.add(listener);
    }

    @Override
    public void removeParameterChangeListener(IParameterChangeListener listener) {
        if (parameterChangeListeners == null) parameterChangeListeners = new ArrayList<>();
        parameterChangeListeners.remove(listener);
    }

    @Override
    public Map<String, Map<String, Object>> getBeanParameterMap() {
        File parameterMap = loadParameterMapFile();

        if (parameterMap == null) return null;

        ObjectMapper mapper = new ObjectMapper();

        try {
            @SuppressWarnings("unchecked")
            Map<String,Map<String, Object>> map = mapper.readValue(parameterMap, mapper.getTypeFactory().constructMapType(HashMap.class, String.class, HashMap.class));
            if (map == null) {
                log.debug("No processor parameter map {} with identifiers defined!", parameterMap.getAbsolutePath());
                map = new HashMap<>();
            }
            return map;
        } catch (JsonParseException e) {
            log.warn("Can not parse {}! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        } catch (JsonMappingException e) {
            log.warn("Can not map {}! Msg: {}: ", parameterMap.getAbsolutePath(), e.toString());
        } catch (IOException e) {
            log.warn("No {} found! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        } catch (Exception e) {
            log.warn("No {} found! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        }
        return null;
    }

    @Override
    public void setStringParameters(Map<String, String> parameters, String identifier) {
        if (parameters == null) parameters = new HashMap<>();

        File parameterMap = loadParameterMapFile();

        ObjectMapper mapper = new ObjectMapper();

        try {
            Map<String,Map<String, Object>> map = mapper.readValue(parameterMap, Map.class);
            if (map == null) {
                log.debug("No processor parameter map {} defined!", parameterMap.getAbsolutePath());
                map = new HashMap<>();
            }

            Map<String, Object> oldMap = map.get(identifier);
            if (oldMap == null) oldMap = new HashMap<>();

            //wir mergen die ursprÃ¼nglichen Parameter mit den neuen
            parameters = MapUtils.mergeMaps(MapUtils.convertObjectMapToStringMap(oldMap), parameters);

            map.put(identifier, MapUtils.convertStringMapToObjectMap(parameters));
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(parameterMap, map);

            notifyListener(MapUtils.convertStringMapToObjectMap(parameters), identifier);
        } catch (JsonParseException e) {
            log.warn("Can not parse parameters to {}! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        } catch (JsonMappingException e) {
            log.warn("Can not map parameters to {}! Msg: {}: ", parameterMap.getAbsolutePath(), e.toString());
        } catch (IOException e) {
            log.warn("No {} found! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        } catch (Exception e) {
            log.warn("No {} found! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        }
    }

    @Override
    public void setStringParameter(String parameterName, String value, String identifier) {
        File parameterMap = loadParameterMapFile();

        ObjectMapper mapper = new ObjectMapper();

        try {
            Map<String,Map<String, Object>> map = mapper.readValue(parameterMap, Map.class);
            if (map == null) {
                log.debug("No processor parameter map {} defined!", parameterMap.getAbsolutePath());
                map = new HashMap<>();
            }

            if (map.get(identifier) != null) map.get(identifier).put(parameterName, value);

            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(parameterMap, map);

            notifyListener(parameterName, value, identifier);
        } catch (JsonParseException e) {
            log.warn("Can not write parameters to {}! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        } catch (JsonMappingException e) {
            log.warn("Can not write parameters to {}! Msg: {}: ", parameterMap.getAbsolutePath(), e.toString());
        } catch (IOException e) {
            log.warn("No {} found! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        } catch (Exception e) {
            log.warn("No {} found! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        }
    }

    @Override
    public void setParameters(Map<String, Object> parameters, String identifier) {
        super.setParameters(parameters, identifier);
    }

    @Override
    public void setParameter(String parameterName, Object value, String identifier) {
        File parameterMap = loadParameterMapFile();

        ObjectMapper mapper = new ObjectMapper();

        try {
            Map<String,Map<String, Object>> map = mapper.readValue(parameterMap, Map.class);
            if (map == null) {
                log.debug("No processor parameter map {} defined!", parameterMap.getAbsolutePath());
                map = new HashMap<>();
            }

            if (map.get(identifier) != null) map.get(identifier).put(parameterName, value);

            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(parameterMap, map);

            notifyListener(parameterName, value, identifier);
        } catch (JsonParseException e) {
            log.warn("Can not write parameters to {}! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        } catch (JsonMappingException e) {
            log.warn("Can not write parameters to {}! Msg: {}: ", parameterMap.getAbsolutePath(), e.toString());
        } catch (IOException e) {
            log.warn("No {} found! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        } catch (Exception e) {
            log.warn("No {} found! Msg: {}", parameterMap.getAbsolutePath(), e.toString());
        }
    }

    private void notifyListener(String parameterName, Object value, String identifier) {
        if (parameterChangeListeners == null) return;
        for (IParameterChangeListener listener : parameterChangeListeners) {
            listener.parameterChanged(parameterName, value, identifier);
        }
    }

    private void notifyListener(Map<String, Object> parameters, String identifier) {
        if (parameterChangeListeners == null) return;
        for (IParameterChangeListener listener : parameterChangeListeners) {
            listener.parametersChanged(parameters, identifier);
        }
    }

    private void notifyListener() {
        if (parameterChangeListeners == null) return;
        for (IParameterChangeListener listener : parameterChangeListeners) {
            listener.parametersChanged();
        }
    }
}