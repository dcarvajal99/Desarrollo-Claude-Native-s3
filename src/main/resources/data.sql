-- Datos de ejemplo para la demo. Estas guias NO tienen archivo en EFS/S3 hasta
-- que se generan/suben via los endpoints; sirven para probar la consulta (Endpoint 6).
INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1001', 'TransportistaX', 'Comercial Andes Ltda', 'Av. Apoquindo 4500, Santiago', 'Caja con 10 notebooks', '2025-06-01', NULL, NULL, FALSE, CURRENT_TIMESTAMP);

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1002', 'TransportistaX', 'Ferreteria El Tornillo', 'Camino Lonquen 1200, Maipu', 'Pallet de herramientas', '2025-06-01', NULL, NULL, FALSE, CURRENT_TIMESTAMP);

INSERT INTO guias (numero_guia, transportista, destinatario, direccion, descripcion, fecha_despacho, efs_path, s3_key, subida_s3, creada_en)
VALUES ('1003', 'TransportistaHermes', 'Botilleria La Esquina', 'Gran Avenida 8800, La Cisterna', 'Bebidas y abarrotes', '2025-06-02', NULL, NULL, FALSE, CURRENT_TIMESTAMP);
