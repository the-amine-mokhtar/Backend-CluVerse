-- Supprime tous les 500 transports du seeder
DELETE FROM transport;

-- Vérifie le nombre restant
SELECT COUNT(*) as remaining_transports FROM transport;

-- Si vous voulez voir tout ce qui reste
SELECT * FROM transport LIMIT 5;
