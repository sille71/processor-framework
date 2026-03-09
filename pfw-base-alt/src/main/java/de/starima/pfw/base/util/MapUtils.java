package de.starima.pfw.base.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.starima.pfw.base.util.LogOutputHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

@Slf4j
public class MapUtils {    

    /* private constructor to not allow creation of object */
    private MapUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static <K,V> Map<K, V> updateMap(final Map<K, V> mapToUpdate, final List<Entry<K, V>> entries) {
        if (mapToUpdate == null || entries == null)
            return new HashMap<>();

        if (mapToUpdate.isEmpty() || entries.isEmpty())
            return mapToUpdate;

        Map<K, V> mapCopy = new HashMap<>(mapToUpdate);
        entries.stream().forEach(e -> mapCopy.put(e.getKey(), e.getValue()));
        return mapCopy;
    }

    public static <T> Map<String, T> mergeMaps(Map<String, T> destMap,
                                                Map<String, T> srcMap) {
        if (srcMap != null) {
            // create destMap if not available
            if (destMap == null)
                destMap = new HashMap<>();

            // merge
            for (Entry<String,T> entry: srcMap.entrySet()) {
                if (entry.getValue() != null) destMap.put(entry.getKey(), entry.getValue());
            }
            return destMap;
        }
        return destMap;
    }

    public static <T> Map<String, Map<String,T>> mergeBeanIdParameterMap(Map<String, Map<String,T>> destMap, Map<String, Map<String,T>> srcMap) {
        if (srcMap != null) {
            // create destMap if not available
            if (destMap == null)
                destMap = new HashMap<>();

            for (Entry<String, Map<String,T>> entry : srcMap.entrySet()) {
                if (entry.getValue() != null)
                destMap.put(entry.getKey(), mergeMaps(destMap.get(entry.getKey()), entry.getValue()));
            }
        }
        return destMap;
    }

    public static <T> Map<String, List<T>> mergeMapsExtended(Map<String, List<T>> destMap,
                                                            Map<String, List<T>> srcMap) {
        if (srcMap != null) {
            // create destMap if not available
            if (destMap == null)
                destMap = new HashMap<>();

            List<T> cluster;
            // merge
            for (Entry<String, List<T>> entry : srcMap.entrySet()) {
                cluster = destMap.get(entry.getKey());
                if (cluster == null) {
                    cluster = new ArrayList<>();
                    destMap.put(entry.getKey(), cluster);
                }
                cluster.addAll(entry.getValue());
            }
            return destMap;
        }
        return destMap;
    }

    public static <T> Map<String, List<T>> mergeEntryExtended(Map<String, List<T>> destMap,
                                                                String key, List<T> srcList) {
        if (key != null && srcList != null && !srcList.isEmpty()) {
            // create destMap if not available
            if (destMap == null)
                destMap = new HashMap<>();

            List<T> cluster = destMap.get(key);
            if (cluster == null) {
                cluster = new ArrayList<>();
                destMap.put(key, cluster);
            }
            cluster.addAll(srcList);

            return destMap;
        }
        return destMap;
    }

    public static Map<String,Object> convertStringMapToObjectMap(Map<String,String> srcMap) {
        Map<String, Object> destMap = new HashMap<>();

        for (Map.Entry<String, String> entry : srcMap.entrySet()) {
            destMap.put(entry.getKey(), entry.getValue());
        }
        return destMap;
    }

    public static Map<String,String> convertObjectMapToStringMap(Map<String,Object> srcMap) {
        Map<String, String> destMap = new HashMap<>();
        if (srcMap == null) return destMap;

        for (Map.Entry<String, Object> entry : srcMap.entrySet()) {
            destMap.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
        }
        return destMap;
    }

    public static Map<String, Map<String,Object>> convertMap(Map<String, Map<String,String>> srcMap) {
        Map<String, Map<String,Object>> destMap = new HashMap<>();

        for (Map.Entry<String, Map<String,String>> entry : srcMap.entrySet()) {
            destMap.put(entry.getKey(), convertStringMapToObjectMap(entry.getValue()));
        }

        return destMap;
    }

    /**
     * Verschmilzt 2 Listen von Elementen, dabei werden gleiche Elemente nur einmal verwendet. Die Gleichheit wird Ã¼ber die compareFunction geprÃ¼ft.
     * Gleiche Element werden auf Objektebene mit der mergeFunction verschmolzen.
     * @param dest
     * @param source
     * @param compareFunction
     * @param mergeFunction
     * @return
     * @param <T>
     */
    public static <T> List<T> mergeLists(List<T> dest, List<T> source, BiPredicate<T,T> compareFunction, BiFunction<T,T,T> mergeFunction) {
        if (dest == null || dest.isEmpty()) return source;
        if (source == null || source.isEmpty()) return dest;
        HashMap<String,T> mergedMap = new HashMap<>();

        source.forEach(value -> {
            T destAttr = dest.stream().filter(attr -> compareFunction.test(attr, value)).findFirst().orElse(null);
            if (destAttr != null) {
                mergedMap.computeIfAbsent(destAttr.toString(), k -> mergeFunction.apply(destAttr, value));
            } else {
                mergedMap.computeIfAbsent(value.toString(), k -> value);
            }
        });

        //es mÃ¼ssen noch die aus dest, die nicht in source sind hinzugefÃ¼gt werden
        dest.forEach(destAttr -> {
            T value = source.stream().filter(attr -> compareFunction.test(attr, destAttr)).findFirst().orElse(null);
            if (value == null) {
                mergedMap.computeIfAbsent(destAttr.toString(), k -> destAttr);
            }
        });

        return new ArrayList<>(mergedMap.values());
    }

    public static String getJsonRepresentation(Map<String ,Map<String ,Object>> beanParameterMap) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(beanParameterMap);
        } catch (Exception e) {
            log.warn("getJsonRepresentation failed for map {}", LogOutputHelper.toLogString(beanParameterMap), e);
        }
        return null;
    }

    public static Map<String,Map<String, Object>> getBeanParameterMapFromJson(byte[] json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String,Map<String, Object>> map = mapper.readValue(json, mapper.getTypeFactory().constructMapType(HashMap.class, String.class, HashMap.class));
            return map;
        } catch (JsonParseException e) {
            log.error("Can not parse json {}!", json, e);
        } catch (JsonMappingException e) {
            log.error("Can not map json {}! ", json, e);
        } catch (Exception e) {
            log.warn("getBeanParameterMapFromJson failed for json {}", json, e);
        }
        return null;
    }

    public static boolean isMapOfStringToMapOfStringToObject(Object obj) {
        if (obj instanceof Map<?, ?>) {
            Map<?, ?> outerMap = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> outerEntry : outerMap.entrySet()) {
                if (!(outerEntry.getKey() instanceof String)) {
                    return false;
                }
                if (!(outerEntry.getValue() instanceof Map<?, ?>)) {
                    return false;
                }
                Map<?, ?> innerMap = (Map<?, ?>) outerEntry.getValue();
                for (Map.Entry<?, ?> innerEntry : innerMap.entrySet()) {
                    if (!(innerEntry.getKey() instanceof String)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }
}