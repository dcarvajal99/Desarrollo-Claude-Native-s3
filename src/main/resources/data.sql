-- ============================================================================
--  Datos de ejemplo para la demo (Semana 6 - roles + nombre de transportista).
--
--  La identidad la entrega Azure AD B2C en el token JWT:
--    - rol      : custom claim "extension_consultaRole"  (descarga | gestion)
--    - dueño    : custom claim "extension_nombreTransportista" (nombre del transportista)
--
--  El campo "transportista" de la guia guarda el dueño. Para descargar (Endpoint 3,
--  rol 'descarga'), ese valor debe coincidir con el "extension_nombreTransportista"
--  del token (lo que devuelve jwt.getClaimAsString(...) en el controller).
--
--  Pon como dueño de tus guias de prueba el MISMO nombre que asignes al atributo
--  nombreTransportista de tu usuario en Azure B2C. Ejemplo abajo: "TransportistaDiego".
--  La guia 1003 usa otro dueño para demostrar el bloqueo de permiso.
-- ============================================================================

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1001', 'TransportistaDiego', 'Comercial Andes Ltda', 'Av. Apoquindo 4500, Santiago', 'Caja con 10 notebooks', '2025-06-01', NULL, NULL, FALSE, CURRENT_TIMESTAMP);

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1002', 'TransportistaDiego', 'Ferreteria El Tornillo', 'Camino Lonquen 1200, Maipu', 'Pallet de herramientas', '2025-06-01', NULL, NULL, FALSE, CURRENT_TIMESTAMP);

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1003', 'OtroTransportista', 'Botilleria La Esquina', 'Gran Avenida 8800, La Cisterna', 'Bebidas y abarrotes', '2025-06-02', NULL, NULL, FALSE, CURRENT_TIMESTAMP);
