-- Add granular permissions for Marketing Templates and Campaign Viewing
INSERT INTO permissions (code, name, description, module) VALUES
('marketing:campaign:view', 'Ver Campañas', 'Permite consultar el historial de campañas', 'MARKETING'),
('marketing:template:view', 'Ver Plantillas', 'Permite consultar plantillas de marketing', 'MARKETING'),
('marketing:template:create', 'Crear Plantillas', 'Permite crear nuevas plantillas', 'MARKETING'),
('marketing:template:edit', 'Editar Plantillas', 'Permite modificar plantillas existentes', 'MARKETING'),
('marketing:template:delete', 'Eliminar Plantillas', 'Permite eliminar plantillas', 'MARKETING')
ON CONFLICT (code) DO NOTHING;

-- Assign new permissions to ADMIN role
INSERT INTO role_permissions (role_name, permission_code) VALUES
('ADMIN', 'marketing:campaign:view'),
('ADMIN', 'marketing:template:view'),
('ADMIN', 'marketing:template:create'),
('ADMIN', 'marketing:template:edit'),
('ADMIN', 'marketing:template:delete')
ON CONFLICT DO NOTHING;
