# 📝 GUIDE PRATIQUE - AJOUT MANUEL D'EVENTS & CAMPAIGNS

## 🎯 Objectif
Ce guide explique comment ajouter manuellement des événements et campagnes via des requêtes SQL directes.

## 📋 Prérequis

### 1. Connexion à la base de données
```bash
mysql -u root -p cluverse_db
# ou via phpMyAdmin
```

### 2. Ordre d'insertion obligatoire
```
1. Campaign (optionnel)
2. Club (si nouveau)
3. Location (si nouveau)
4. Event
```

## 🚀 Exemples pratiques

### Exemple 1: Événement simple (sans campagne)

```sql
-- 1. Vérifier qu'un club existe
SELECT id, name FROM club WHERE name LIKE '%Info%';

-- 2. Vérifier qu'une location existe
SELECT id, name, capacity FROM location WHERE type = 'AUDITORIUM';

-- 3. Ajouter l'événement
INSERT INTO event (
    title,
    description,
    start_date,
    end_date,
    capacity,
    status,
    category,
    location_id,
    club_id,
    is_paid,
    price
) VALUES (
    'Atelier Développement Web',
    'Apprendre les bases du développement web moderne',
    '2026-05-10 14:00:00',
    '2026-05-10 17:00:00',
    30,
    'PLANNED',
    'TECHNOLOGY',
    1,  -- ID de l'amphithéâtre
    1,  -- ID du club informatique
    false,
    0.00
);
```

### Exemple 2: Événement avec campagne

```sql
-- 1. Créer d'abord la campagne
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
    'Semaine de l'Innovation',
    'Événements dédiés à l'innovation technologique',
    '2026-04-01 00:00:00',
    '2026-04-07 23:59:59',
    'ACTIVE',
    'EVENT_SERIES',
    10000.00,
    'Tous les étudiants intéressés par la tech',
    true
);

-- 2. Récupérer l'ID de la campagne créée
SELECT LAST_INSERT_ID() as campaign_id;

-- 3. Ajouter l'événement lié à la campagne
INSERT INTO event (
    title,
    description,
    start_date,
    end_date,
    capacity,
    status,
    category,
    location_id,
    club_id,
    campaign_id,
    is_paid,
    price
) VALUES (
    'Hackathon Innovation',
    'Concours de programmation sur le thème de l'innovation',
    '2026-04-05 09:00:00',
    '2026-04-05 18:00:00',
    50,
    'PLANNED',
    'TECHNOLOGY',
    1, 1,  -- location et club
    LAST_INSERT_ID(),  -- ID de la campagne
    true, 10.00
);
```

### Exemple 3: Événement sportif payant

```sql
INSERT INTO event (
    title,
    description,
    start_date,
    end_date,
    capacity,
    status,
    category,
    location_id,
    club_id,
    is_paid,
    price
) VALUES (
    'Marathon Universitaire',
    'Course de 10km ouverte à tous les étudiants',
    '2026-06-15 08:00:00',
    '2026-06-15 12:00:00',
    300,
    'PLANNED',
    'SPORTS',
    3,  -- Terrain de sport
    3,  -- Club sportif
    true,
    5.00
);
```

## 🔧 Commandes de vérification

### Lister tous les événements
```sql
SELECT
    e.id,
    e.title,
    e.start_date,
    e.capacity,
    e.participants_count,
    c.title as campaign,
    l.name as location,
    cl.name as club,
    CASE WHEN e.is_paid THEN CONCAT(e.price, '€') ELSE 'Gratuit' END as price
FROM event e
LEFT JOIN campaign c ON e.campaign_id = c.id
LEFT JOIN location l ON e.location_id = l.id
LEFT JOIN club cl ON e.club_id = cl.id
ORDER BY e.start_date;
```

### Événements disponibles pour inscription
```sql
SELECT
    id,
    title,
    capacity,
    participants_count,
    (capacity - participants_count) as available_seats,
    start_date
FROM event
WHERE status = 'PLANNED'
  AND start_date > NOW()
  AND participants_count < capacity
ORDER BY start_date;
```

### Événements complets (pour tester la file d'attente)
```sql
SELECT
    id,
    title,
    capacity,
    participants_count
FROM event
WHERE participants_count >= capacity
ORDER BY title;
```

## 📊 Valeurs des énumérations

### EventStatus
- `PLANNED` - Planifié
- `ACTIVE` - Actif
- `FINISHED` - Terminé
- `CANCELLED` - Annulé
- `COMPLETED` - Complété (calculé automatiquement)
- `ONGOING` - En cours (calculé automatiquement)

### EventCategory
- `TECHNOLOGY` - Technologie
- `MUSIC` - Musique
- `ART` - Art
- `THEATER` - Théâtre
- `DANCE` - Danse
- `SPORTS` - Sports
- `CAREER` - Carrière
- `ENVIRONMENT` - Environnement
- `SOCIAL` - Social
- `ACADEMIC` - Académique
- `WORKSHOP` - Atelier
- `CONFERENCE` - Conférence

### CampaignStatus
- `ACTIVE` - Active
- `PLANNED` - Planifiée
- `COMPLETED` - Terminée
- `CANCELLED` - Annulée

### CampaignType
- `EVENT_SERIES` - Série d'événements
- `SEASONAL` - Saisonnier
- `COMPETITION` - Compétition
- `EDUCATIONAL` - Éducatif
- `AWARENESS` - Sensibilisation

## 🆘 Dépannage

### Erreur de clé étrangère
```sql
-- Vérifier que les références existent
SELECT id, name FROM club;
SELECT id, name FROM location;
SELECT id, title FROM campaign;
```

### Erreur de format de date
```sql
-- Format correct: 'YYYY-MM-DD HH:MM:SS'
-- Exemple: '2026-05-15 14:30:00'
```

### Erreur de capacité
```sql
-- La capacité doit être > 0
-- participants_count commence à 0
```

## 🎯 Tests recommandés

1. **Ajoutez un événement gratuit** → Test inscription directe
2. **Ajoutez un événement payant** → Test intégration paiement
3. **Ajoutez un événement petite capacité** → Test file d'attente
4. **Ajoutez une campagne** → Test regroupement d'événements

Ces exemples vous permettent d'ajouter rapidement des événements pour tester toutes les fonctionnalités ! 🚀