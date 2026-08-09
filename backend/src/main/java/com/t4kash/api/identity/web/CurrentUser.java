package com.t4kash.api.identity.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Obtiene al usuario autenticado desde el encabezado Authorization. Cuando se
 * indica {@link #role()}, rechaza con 403 las cuentas que no poseen ese rol y
 * evita repetir la misma validacion en cada controlador.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CurrentUser {
    String role() default "";
}
