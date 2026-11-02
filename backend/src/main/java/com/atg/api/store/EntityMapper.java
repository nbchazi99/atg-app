package com.atg.api.store;

import com.google.cloud.firestore.DocumentSnapshot;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Reflection-based mapping between entity POJOs and Firestore documents. */
final class EntityMapper {

    private EntityMapper() {
    }

    static Map<String, Object> toDoc(Object entity) {
        Map<String, Object> doc = new HashMap<>();
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (field.getName().equals("id")) continue;
            Object value = read(field, entity);
            if (value == null) continue;
            doc.put(field.getName(), toStorable(value));
        }
        return doc;
    }

    static <T> T fromDoc(Class<T> type, DocumentSnapshot snap) {
        try {
            T entity = type.getDeclaredConstructor().newInstance();
            for (Field field : type.getDeclaredFields()) {
                if (field.getName().equals("id")) {
                    if (snap.getId() != null && !snap.getId().isBlank()) {
                        write(field, entity, Long.parseLong(snap.getId()));
                    }
                    continue;
                }
                Object value = snap.get(field.getName());
                if (value == null) continue;
                write(field, entity, fromStorable(field.getType(), value));
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map document " + snap.getReference().getPath(), e);
        }
    }

    private static Object toStorable(Object value) {
        if (value instanceof Enum<?> e) return e.name();
        if (value instanceof BigDecimal d) return d.toPlainString();
        if (value instanceof LocalDateTime t) return t.toString();
        return value;
    }

    private static Object fromStorable(Class<?> type, Object value) {
        if (value == null) return null;
        if (type.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Object e = Enum.valueOf((Class) type, (String) value);
            return e;
        }
        if (type == BigDecimal.class) {
            return value instanceof String s ? new BigDecimal(s) : BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.parse((String) value);
        }
        if (type == long.class || type == Long.class) {
            return value instanceof Number n ? n.longValue() : Long.parseLong(value.toString());
        }
        if (type == int.class || type == Integer.class) {
            return value instanceof Number n ? n.intValue() : Integer.parseInt(value.toString());
        }
        if (type == boolean.class || type == Boolean.class) {
            return value instanceof Boolean b ? b : Boolean.parseBoolean(value.toString());
        }
        return value;
    }

    private static Object read(Field field, Object entity) {
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to read field " + field.getName(), e);
        }
    }

    private static void write(Field field, Object entity, Object value) {
        try {
            field.setAccessible(true);
            field.set(entity, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to write field " + field.getName(), e);
        }
    }
}
