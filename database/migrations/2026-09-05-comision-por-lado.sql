-- Reparto del take rate del 15 % entre las dos partes.
--
-- Hasta ahora `pagos` solo guardaba la comision total de la plataforma. Con el
-- modelo 10 % cliente + 5 % estudiante hace falta saber cuanto puso cada lado,
-- porque el wallet del estudiante tiene que mostrar lo que se le retuvo a el y
-- no la suma de los dos.
--
-- Las dos columnas son aditivas y arrancan en 0, asi que los pagos existentes
-- quedan validos: la restriccion nueva se cumple sola mientras comision_plataforma
-- tambien sea 0. Para los pagos anteriores al cambio, donde toda la comision la
-- pagaba el cliente, se rellena comision_cliente con el valor que ya estaba.

ALTER TABLE pagos
    ADD COLUMN IF NOT EXISTS comision_cliente numeric(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS comision_estudiante numeric(12,2) NOT NULL DEFAULT 0;

UPDATE pagos
SET comision_cliente = comision_plataforma
WHERE comision_cliente = 0
  AND comision_estudiante = 0
  AND comision_plataforma > 0;

ALTER TABLE pagos
    DROP CONSTRAINT IF EXISTS ck_pagos_comision_por_lado;

ALTER TABLE pagos
    ADD CONSTRAINT ck_pagos_comision_por_lado CHECK (
        comision_cliente >= 0
        AND comision_estudiante >= 0
        AND comision_plataforma = comision_cliente + comision_estudiante
    );
