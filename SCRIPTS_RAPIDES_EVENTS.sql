-- 🔧 SCRIPTS RAPIDES - AJOUT D'EVENTS COURANTS

-- =================================================================
-- ÉVÉNEMENTS TECHNIQUES
-- =================================================================

-- Atelier Développement Web
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Atelier Développement Web', 'Apprendre HTML, CSS, JavaScript', '2026-05-15 14:00:00', '2026-05-15 17:00:00', 25, 'PLANNED', 'TECHNOLOGY', 2, 1, false, 0.00);

-- Conférence IA
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Conférence Intelligence Artificielle', 'Dernières avancées en IA', '2026-06-01 10:00:00', '2026-06-01 12:00:00', 100, 'PLANNED', 'TECHNOLOGY', 1, 1, false, 0.00);

-- Hackathon
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Hackathon 24h', 'Concours de programmation', '2026-07-10 09:00:00', '2026-07-11 09:00:00', 50, 'PLANNED', 'TECHNOLOGY', 1, 1, true, 20.00);

-- =================================================================
-- ÉVÉNEMENTS CULTURELS
-- =================================================================

-- Concert Étudiant
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Concert de Musique Universitaire', 'Artistes étudiants', '2026-04-20 20:00:00', '2026-04-20 22:00:00', 200, 'PLANNED', 'MUSIC', 3, 2, true, 10.00);

-- Exposition Art
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Exposition d''Art Contemporain', 'Œuvres étudiantes', '2026-05-05 10:00:00', '2026-05-07 18:00:00', 150, 'PLANNED', 'ART', 5, 2, false, 0.00);

-- Théâtre
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Pièce de Théâtre Étudiante', 'Production universitaire', '2026-06-15 19:00:00', '2026-06-15 21:00:00', 80, 'PLANNED', 'THEATER', 1, 2, true, 8.00);

-- =================================================================
-- ÉVÉNEMENTS SPORTIFS
-- =================================================================

-- Football
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Tournoi de Football', 'Matchs inter-facultés', '2026-03-25 14:00:00', '2026-03-25 17:00:00', 150, 'PLANNED', 'SPORTS', 3, 3, false, 0.00);

-- Basketball
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Match de Basketball', 'Équipes universitaires', '2026-04-10 16:00:00', '2026-04-10 18:00:00', 100, 'PLANNED', 'SPORTS', 3, 3, false, 0.00);

-- Marathon
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Marathon Universitaire', 'Course de 10km', '2026-05-20 08:00:00', '2026-05-20 12:00:00', 300, 'PLANNED', 'SPORTS', 3, 3, true, 5.00);

-- =================================================================
-- ÉVÉNEMENTS CARRIÈRE
-- =================================================================

-- Atelier CV
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Atelier CV et Lettres', 'Rédaction professionnelle', '2026-02-15 14:00:00', '2026-02-15 16:00:00', 30, 'PLANNED', 'CAREER', 2, 5, false, 0.00);

-- Forum Entreprises
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Forum Emploi Tech', 'Rencontres entreprises', '2026-03-05 09:00:00', '2026-03-05 17:00:00', 200, 'PLANNED', 'CAREER', 1, 5, false, 0.00);

-- Networking
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Soirée Networking', 'Échanges professionnels', '2026-04-18 18:00:00', '2026-04-18 21:00:00', 60, 'PLANNED', 'CAREER', 4, 5, true, 15.00);

-- =================================================================
-- ÉVÉNEMENTS ENVIRONNEMENT
-- =================================================================

-- Nettoyage Campus
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Opération Nettoyage', 'Nettoyage collectif campus', '2026-04-22 08:00:00', '2026-04-22 12:00:00', 100, 'PLANNED', 'ENVIRONMENT', 3, 4, false, 0.00);

-- Conférence Climat
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Conférence Climat', 'Enjeux environnementaux', '2026-05-12 14:00:00', '2026-05-12 16:00:00', 120, 'PLANNED', 'ENVIRONMENT', 1, 4, false, 0.00);

-- Atelier Zéro Déchet
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Atelier Zéro Déchet', 'Pratiques écologiques', '2026-06-08 10:00:00', '2026-06-08 15:00:00', 25, 'PLANNED', 'ENVIRONMENT', 2, 4, false, 0.00);

-- =================================================================
-- ÉVÉNEMENTS SOCIAUX
-- =================================================================

-- Soirée d'intégration
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Soirée d''Intégration', 'Bienvenue nouveaux étudiants', '2026-09-25 19:00:00', '2026-09-25 23:00:00', 250, 'PLANNED', 'SOCIAL', 4, 2, true, 10.00);

-- Gala de fin d'année
INSERT INTO event (title, description, start_date, end_date, capacity, status, category, location_id, club_id, is_paid, price)
VALUES ('Gala de Fin d''Année', 'Cérémonie de clôture', '2026-12-18 18:00:00', '2026-12-18 22:00:00', 300, 'PLANNED', 'ACADEMIC', 1, 1, true, 25.00);

-- =================================================================
-- VÉRIFICATION RAPIDE
-- =================================================================

-- Compter les événements par catégorie
SELECT category, COUNT(*) as count
FROM event
WHERE start_date > NOW()
GROUP BY category
ORDER BY count DESC;

-- Événements cette semaine
SELECT title, start_date, capacity
FROM event
WHERE start_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 7 DAY)
ORDER BY start_date;

-- Événements payants
SELECT title, price, capacity
FROM event
WHERE is_paid = true
ORDER BY price DESC;