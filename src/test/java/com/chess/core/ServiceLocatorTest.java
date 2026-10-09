package com.chess.core;

import org.junit.jupiter.api.Test;

import com.chess.ports.ServiceLocator;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceLocatorTest {
    @Test
    public void testRegisterAndResolve() {
        ServiceLocator locator = new ServiceLocator();
        String service = "Servicio de prueba";
        locator.register(String.class, service);
        
        // Se espera recuperar exactamente el mismo objeto registrado
        assertEquals(service, locator.resolve(String.class));
    }

    @Test
    public void testResolveUnregisteredThrowsException() {
        ServiceLocator locator = new ServiceLocator();
        // Se espera una excepción al intentar resolver algo no registrado
        assertThrows(Exception.class, () -> {
            locator.resolve(Integer.class);
        });
    }
}