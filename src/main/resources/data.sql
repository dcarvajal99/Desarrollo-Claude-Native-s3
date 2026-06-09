-- ============================================================================
--  Transportistas precargados (usuarios para el login JWT).
--  Las claves estan encriptadas con BCrypt. Credenciales para la demo:
--    usuario: TransportistaX        clave: 1234
--    usuario: TransportistaHermes   clave: 1234
--    usuario: admin                 clave: admin1234   (rol ADMIN)
-- ============================================================================
INSERT INTO transportistas (username, password, role)
VALUES ('TransportistaX', '$2a$10$JNYSF0S5RCIswZjqvArceuT01G.QLHKwHoav98sonG6IDRhLaoGfS', 'TRANSPORTISTA');
INSERT INTO transportistas (username, password, role)
VALUES ('TransportistaHermes', '$2a$10$rkhTCzJGxFl71NEVlAGRTOLVg0quR018.XCE4SXFmBHufDCShSfGO', 'TRANSPORTISTA');
INSERT INTO transportistas (username, password, role)
VALUES ('admin', '$2a$10$jJzuC2SbZkgn6Au6BqHRG.bYvjAr0zbF5MNRTjt82xEg9JXkVFCpK', 'ADMIN');

-- Datos de ejemplo para la demo. Estas guias NO tienen archivo en EFS/S3 hasta
-- que se generan/suben via los endpoints; sirven para probar la consulta (Endpoint 6).
INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1001', 'TransportistaX', 'Comercial Andes Ltda', 'Av. Apoquindo 4500, Santiago', 'Caja con 10 notebooks', '2025-06-01', NULL, NULL, FALSE, CURRENT_TIMESTAMP);

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1002', 'TransportistaX', 'Ferreteria El Tornillo', 'Camino Lonquen 1200, Maipu', 'Pallet de herramientas', '2025-06-01', NULL, NULL, FALSE, CURRENT_TIMESTAMP);

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1003', 'TransportistaHermes', 'Botilleria La Esquina', 'Gran Avenida 8800, La Cisterna', 'Bebidas y abarrotes', '2025-06-02', NULL, NULL, FALSE, CURRENT_TIMESTAMP);
