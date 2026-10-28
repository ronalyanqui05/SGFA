UPDATE situaciones_academicas
SET created_at = replace(created_at, ' ', 'T') || 'Z',
    updated_at = replace(updated_at, ' ', 'T') || 'Z'
WHERE created_at NOT LIKE '%T%';
