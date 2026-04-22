-- 📝 EXEMPLES DE REQUÊTES SQL - AJOUT MANUEL D'EVENTS & CAMPAIGNS

-- =================================================================
-- INSTRUCTIONS GÉNÉRALES
-- =================================================================

/*
1. Remplacez les valeurs entre < > par vos vraies données
2. Exécutez dans l'ordre : Campaign → Club → Location → Event
3. Les IDs sont auto-incrémentés, vous pouvez les omettre
4. Pour les dates : format 'YYYY-MM-DD HH:MM:SS'
5. Pour les booléens : true/false (minuscules)
6. Pour les enums : respectez la casse exacte
*/

-- =================================================================
-- 1. AJOUTER UNE CAMPAGNE (CAMPAIGN)
-- =================================================================

-- Exemple 1: Campagne Tech
INSERT INTO campaign (
    title,
    description,
    start_date,
    end_date,
    status,
    type,
    budget,
    target_audience,
    is_active
) VALUES (
    'Festival Tech 2026',
    'Festival annuel dédié aux technologies innovantes',
    '2026-03-01 00:00:00',
    '2026-05-31 23:59:59',
    'ACTIVE',
    'EVENT_SERIES',
    25000.00,
    'Étudiants en informatique et entrepreneurs',
    true
);

-- Exemple 2: Campagne Sportive
INSERT INTO campaign (
    title,
    description,
    start_date,
    end_date,
    status,
    type,
    budget,
    target_audience,
    is_active
) VALUES (
    'Championnat Sportif 2026',
    'Compétitions sportives inter-universitaires',
    '2026-02-01 00:00:00',
    '2026-06-30 23:59:59',
    'PLANNED',
    'COMPETITION',
    15000.00,
    'Étudiants sportifs et équipes universitaires',
    true
);

-- =================================================================
-- 2. AJOUTER UN CLUB (si nécessaire)
-- =================================================================

INSERT INTO club (
    name,
    description,
    email,
    phone,
    website,
    logo_url,
    is_active
) VALUES (
    'Club Informatique',
    'Club dédié aux technologies et à l''innovation',
    'club.info@univ.tn',
    '+216 12 345 678',
    'https://club-info.univ.tn',
    '/uploads/clubs/club-info-logo.png',
    true
);

-- =================================================================
-- 3. AJOUTER UNE LOCATION (si nécessaire)
-- =================================================================

INSERT INTO location (
    name,
    address,
    latitude,
    longitude,
    capacity,
    type,
    is_active
) VALUES (
    'Amphithéâtre Central',
    'Campus Universitaire, Bloc A - Niveau 1',
    36.8188,
    10.1658,
    250,
    'AUDITORIUM',
    true
);

-- =================================================================
-- 4. AJOUTER UN ÉVÉNEMENT (EVENT)
-- =================================================================

-- Exemple 1: Événement simple (sans campagne)
INSERT INTO event (
    title,
    description,
    start_date,
    end_date,
    capacity,
    status,
    category,
    participants_count,
    location_id,
    club_id,
    is_paid,
    price,
    reminder_sent
) VALUES (
    'Conférence Intelligence Artificielle',
    'Conférence sur les dernières avancées en IA et Machine Learning',
    '2026-04-15 14:00:00',
    '2026-04-15 18:00:00',
    150,
    'PLANNED',
    'TECHNOLOGY',
    0,
    1,  -- ID de la location créée ci-dessus
    1,  -- ID du club créé ci-dessus
    false,
    0.00,
    false
);

-- Exemple 2: Événement payant avec campagne
INSERT INTO event (
    title,
    description,
    start_date,
    end_date,
    capacity,
    status,
    category,
    participants_count,
    location_id,
    club_id,
    campaign_id,
    is_paid,
    price,
    reminder_sent
) VALUES (
    'Hackathon 24h',
    'Concours de programmation intensif sur 24 heures',
    '2026-05-20 09:00:00',
    '2026-05-21 09:00:00',
    80,
    'PLANNED',
    'TECHNOLOGY',
    0,
    1,
    1,
    1,  -- ID de la campagne créée ci-dessus
    true,
    25.00,
    false
);

-- Exemple 3: Événement sportif
INSERT INTO event (
    title,
    description,
    start_date,
    end_date,
    capacity,
    status,
    category,
    participants_count,
    location_id,
    club_id,
    campaign_id,
    is_paid,
    price,
    reminder_sent
) VALUES (
    'Tournoi de Football',
    'Matchs de football entre facultés',
    '2026-03-25 14:00:00',
    '2026-03-25 18:00:00',
    200,
    'PLANNED',
    'SPORTS',
    0,
    3,  -- Terrain de sport
    3,  -- Club sportif
    2,  -- Campagne sportive
    false,
    0.00,
    false
);

-- =================================================================
-- 5. AJOUTER UNE IMAGE À UN ÉVÉNEMENT (après upload)
-- =================================================================

-- Après avoir uploadé l'image via l'API /api/events/upload
UPDATE event
SET image_url = '/uploads/events/hackathon-2026.jpg'
WHERE id = 2;  -- ID de l'événement

-- =================================================================
-- 6. EXEMPLES AVANCÉS
-- =================================================================

-- Événement avec toutes les options
INSERT INTO event (
    title,
    description,
    start_date,
    end_date,
    capacity,
    status,
    category,
    participants_count,
    location_id,
    club_id,
    campaign_id,
    is_paid,
    price,
    reminder_sent,
    image_url
) VALUES (
    'Festival de Musique Universitaire',
    'Festival musical avec concerts d''artistes étudiants et professionnels',
    '2026-06-10 18:00:00',
    '2026-06-10 23:00:00',
    500,
    'ACTIVE',  -- Événement déjà actif
    'MUSIC',
    0,
    3,  -- Terrain extérieur
    2,  -- Club culturel
    2,  -- Campagne culturelle
    true,
    15.00,
    false,
    '/uploads/events/music-festival-2026.jpg'
);

-- =================================================================
-- 7. REQUÊTES UTILITAIRES
-- =================================================================

-- Vérifier les données insérées
SELECT 'Campaigns:' as type, COUNT(*) as count FROM campaign
UNION ALL
SELECT 'Events:', COUNT(*) FROM event
UNION ALL
SELECT 'Clubs:', COUNT(*) FROM club
UNION ALL
SELECT 'Locations:', COUNT(*) FROM location;

-- Lister tous les événements avec leur campagne
SELECT
    e.id,
    e.title,
    e.status,
    e.category,
    e.capacity,
    e.participants_count,
    c.title as campaign_title,
    l.name as location_name,
    cl.name as club_name
FROM event e
LEFT JOIN campaign c ON e.campaign_id = c.id
LEFT JOIN location l ON e.location_id = l.id
LEFT JOIN club cl ON e.club_id = cl.id
ORDER BY e.id;

-- Événements avec file d'attente potentielle (capacité < 100)
SELECT
    id,
    title,
    capacity,
    participants_count,
    (capacity - participants_count) as available_seats
FROM event
WHERE capacity < 100
ORDER BY capacity;

-- =================================================================
-- 8. NETTOYAGE (si nécessaire)
-- =================================================================

-- Supprimer un événement
-- DELETE FROM event WHERE id = <ID>;

-- Supprimer une campagne (attention: supprime aussi ses événements)
-- DELETE FROM campaign WHERE id = <ID>;

-- =================================================================
-- VALEURS POSSIBLES POUR LES ENUMS
-- =================================================================

/*
EVENT STATUS: PLANNED, ACTIVE, FINISHED, CANCELLED, COMPLETED, ONGOING
EVENT CATEGORY: TECHNOLOGY, MUSIC, ART, THEATER, DANCE, SPORTS, CAREER,
                ENVIRONMENT, SOCIAL, ACADEMIC, WORKSHOP, CONFERENCE
CAMPAIGN STATUS: ACTIVE, PLANNED, COMPLETED, CANCELLED
CAMPAIGN TYPE: EVENT_SERIES, SEASONAL, COMPETITION, EDUCATIONAL, AWARENESS
LOCATION TYPE: AUDITORIUM, CONFERENCE_ROOM, OUTDOOR, CAFETERIA, LIBRARY
*/