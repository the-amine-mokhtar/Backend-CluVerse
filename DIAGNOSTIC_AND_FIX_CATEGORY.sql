-- ============================================
-- DIAGNOSTIC: Identifiez les catégories invalides
-- ============================================

-- 1. Voir TOUTES les valeurs de category dans la BD (y compris NULL)
SELECT id, title, category FROM event ORDER BY id;

-- 2. Voir les catégories DISTINCTES 
SELECT DISTINCT category FROM event;

-- 3. Voir COMBIEN d'événements ont une catégorie INVALIDE
SELECT COUNT(*) as events_with_invalid_category FROM event 
WHERE category NOT IN (
    'CONFERENCE', 'WORKSHOP', 'MEETING', 'TRAINING', 
    'HACKATHON', 'SOCIAL', 'SEMINAR', 'NETWORKING', 
    'COMPETITION', 'OTHER'
) 
AND category IS NOT NULL 
AND category != '';

-- 4. Afficher les événements avec catégories INVALIDES
SELECT id, title, category FROM event 
WHERE category NOT IN (
    'CONFERENCE', 'WORKSHOP', 'MEETING', 'TRAINING', 
    'HACKATHON', 'SOCIAL', 'SEMINAR', 'NETWORKING', 
    'COMPETITION', 'OTHER'
) 
AND category IS NOT NULL 
AND category != '';

-- ============================================
-- SOLUTION: Corriger les valeurs invalides
-- ============================================

-- Option 1: Remplacer par NULL (recommandé)
UPDATE event 
SET category = NULL 
WHERE category NOT IN (
    'CONFERENCE', 'WORKSHOP', 'MEETING', 'TRAINING', 
    'HACKATHON', 'SOCIAL', 'SEMINAR', 'NETWORKING', 
    'COMPETITION', 'OTHER'
) 
AND category IS NOT NULL 
AND category != '';

-- Option 2: Remplacer par 'OTHER' (si vous ne voulez pas de NULL)
-- UPDATE event 
-- SET category = 'OTHER' 
-- WHERE category NOT IN (
--     'CONFERENCE', 'WORKSHOP', 'MEETING', 'TRAINING', 
--     'HACKATHON', 'SOCIAL', 'SEMINAR', 'NETWORKING', 
--     'COMPETITION', 'OTHER'
-- ) 
-- AND category IS NOT NULL 
-- AND category != '';

-- Vérifier qu'il n'y a plus de problèmes
SELECT COUNT(*) as total_events FROM event;
SELECT COUNT(*) as events_with_valid_category FROM event 
WHERE category IN (
    'CONFERENCE', 'WORKSHOP', 'MEETING', 'TRAINING', 
    'HACKATHON', 'SOCIAL', 'SEMINAR', 'NETWORKING', 
    'COMPETITION', 'OTHER'
) 
OR category IS NULL;
