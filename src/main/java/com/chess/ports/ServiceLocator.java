package com.chess.ports;

import java.util.HashMap;
import java.util.Map;

public class ServiceLocator {

    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void register(Class<T> type, T implementation) {
        services.put(type, implementation);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("No se encontró ningún servicio registrado para el tipo: " + type.getName());
        }
        return (T) service;
    }
}