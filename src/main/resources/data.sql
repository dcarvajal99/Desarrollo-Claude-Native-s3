-- ============================================================================
--  Datos de ejemplo para la demo (Semana 5 - autenticacion con Azure AD).
--
--  Ya NO hay tabla de usuarios/transportistas para login: la identidad la entrega
--  Azure AD en el token JWT. El campo "transportista" de la guia guarda el dueño,
--  y para descargar (Endpoint 3) ese valor debe coincidir con el claim "sub" del
--  token de Azure (lo que devuelve jwt.getSubject() en el controller).
--
--  El "sub" del token Client Credentials de esta app es el service principal:
--    63afb75d-7d95-440b-bad0-e2568da64751
--  Por eso las guias 1001 y 1002 usan ESE valor como dueño -> se podran descargar.
--  La guia 1003 usa otro dueño -> al intentar descargarla con tu token dara 403/404
--  (sirve para demostrar la validacion de permisos).
-- ============================================================================

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1001', '63afb75d-7d95-440b-bad0-e2568da64751', 'Comercial Andes Ltda', 'Av. Apoquindo 4500, Santiago', 'Caja con 10 notebooks', '2025-06-01', NULL, NULL, FALSE, CURRENT_TIMESTAMP);

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1002', '63afb75d-7d95-440b-bad0-e2568da64751', 'Ferreteria El Tornillo', 'Camino Lonquen 1200, Maipu', 'Pallet de herramientas', '2025-06-01', NULL, NULL, FALSE, CURRENT_TIMESTAMP);

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1003', 'otro-transportista-ejemplo', 'Botilleria La Esquina', 'Gran Avenida 8800, La Cisterna', 'Bebidas y abarrotes', '2025-06-02', NULL, NULL, FALSE, CURRENT_TIMESTAMP);
