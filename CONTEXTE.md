# Contexte du Projet Backend Cluverse

## 📋 Vue d'ensemble

**Cluverse** est une plateforme backend Spring Boot 3.4.3 pour la gestion de clubs universitaires. C'est une application Java 17 utilisant Maven pour la gestion des dépendances.

**Application name**: Cluverse  
**Version**: 0.0.1-SNAPSHOT  
**GroupId**: com.hexaweb  
**ArtifactId**: Backend-Cluverse

---

## 🏗️ Structure des Packages

```
com.hexaweb.backendcluverse/
├── config/                  # Configuration Spring
│   ├── AdminInitializer.java        # Initialisation super admin
│   ├── CloudinaryConfig.java        # Configuration Cloudinary (stockage images)
│   ├── CorsConfig.java              # Configuration CORS
│   └── OpenApiConfig.java           # Configuration Swagger/OpenAPI
├── controllers/             # Contrôleurs REST (28 contrôleurs)
│   ├── AuthController.java
│   ├── UserController.java
│   ├── ClubController.java
│   ├── ElectionController.java
│   ├── CandidateController.java
│   ├── PositionController.java
│   ├── VoteController.java
│   ├── EventController.java
│   ├── EventParticipantController.java
│   ├── ReservationController.java
│   ├── LocationController.java
│   ├── ResourceController.java
│   ├── TransportController.java
│   ├── VehicleController.java
│   ├── RecruitmentController.java
│   ├── ApplicationController.java
│   ├── BudgetController.java
│   ├── TransactionController.java
│   ├── MembershipController.java
│   ├── SkillController.java
│   ├── UserSkillController.java
│   ├── SponsorController.java
│   ├── SponsorshipController.java
│   ├── NotificationController.java
│   ├── VerificationController.java
│   └── (autres controllers)
├── services/                # Couche métier (services)
├── repositories/            # Accès données (JPA)
├── entities/                # Modèles JPA
│   ├── User.java
│   ├── Club.java
│   ├── Membership.java
│   ├── Notification.java
│   ├── election/            # Entités élections
│   ├── event/               # Entités événements
│   ├── finance/             # Entités finances
│   ├── logistics/           # Entités logistique
│   ├── recruitement/        # Entités recrutement
│   ├── skills/              # Entités compétences
│   └── sponsoring/          # Entités sponsoring
├── dto/                     # Data Transfer Objects
├── enumerations/            # Énumérations (RoleType, ApplicationStatus, etc.)
├── exceptions/              # Exceptions personnalisées
└── utils/                   # Utilitaires
    └── JwtUtil.java         # Gestion JWT
```

---

## 🔌 Endpoints REST Exposés

### **1. AUTHENTIFICATION** (`/api/auth`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| POST | `/api/auth/signup` | `SignupRequest` | `AuthResponse` | Inscription nouvel utilisateur |
| POST | `/api/auth/login` | `LoginRequest` | `List<MembershipDto>` | Connexion utilisateur (retourne clubs) |
| POST | `/api/auth/login-club` | `LoginClubRequest` | `AuthResponse` | Connexion spécifique à un club |
| POST | `/api/auth/login-member` | `MemberLoginRequest` | `AuthResponse` | Connexion membre avec identifiant |
| POST | `/api/auth/refresh-token` | (header: Authorization) | `AuthResponse` | Renouvelle le token JWT |

### **2. UTILISATEURS** (`/api/users`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/users` | - | `List<User>` | Tous les utilisateurs |
| GET | `/api/users/{id}` | - | `User` | Utilisateur par ID |
| POST | `/api/users` | `User` | `User` | Créer utilisateur |
| PUT | `/api/users/{id}` | `User` | `User` | Modifier utilisateur |
| DELETE | `/api/users/{id}` | - | - | Supprimer utilisateur |
| GET | `/api/users/me` | (header: Authorization) | `User` | Profil utilisateur authentifié |
| PUT | `/api/users/me` | (header: Authorization), `UpdateProfileRequest` | `User` | Modifier profil personnel |
| POST | `/api/users/me/photo` | (header: Authorization), `file` (MultipartFile) | `String` (URL) | Télécharger photo de profil |

### **3. CLUBS** (`/api/clubs`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/clubs` | - | `List<Club>` | Tous les clubs |
| GET | `/api/clubs/{id}` | - | `Club` | Club par ID |
| POST | `/api/clubs` | `Club` | `Club` | Créer club (envoie email de vérification) |
| PUT | `/api/clubs/{id}` | `Club` | `Club` | Modifier club |
| DELETE | `/api/clubs/{id}` | - | - | Supprimer club |
| GET | `/api/clubs/names` | - | `List<String>` | Noms de tous les clubs |
| GET | `/api/clubs/check-email` | `email` (query) | `Boolean` | Vérifier email disponible |
| GET | `/api/clubs/{clubId}/members` | - | `List<MemberProfileDto>` | Membres du club |
| POST | `/api/clubs/{clubId}/members/{userId}` | `role` (query, défaut: MEMBER) | `MembershipDto` | Ajouter membre au club |
| POST | `/api/clubs/{clubId}/members/send-invite` | `InviteMemberRequest` | - | Envoyer invitation membre |
| DELETE | `/api/clubs/{clubId}/members/{userId}` | - | - | Supprimer membre du club |
| POST | `/api/clubs/{id}/logo` | `file` (MultipartFile) | `String` (URL) | Télécharger logo club |
| GET | `/api/clubs/verify` | `code` (query) | `Map` | Vérifier et activer club via code |

### **4. ADHÉSIONS** (`/api/memberships`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/memberships` | - | `List<Membership>` | Toutes les adhésions |
| GET | `/api/memberships/{id}` | - | `Membership` | Adhésion par ID |
| POST | `/api/memberships` | `Membership` | `Membership` | Créer adhésion |
| PUT | `/api/memberships/{id}` | `Membership` | `Membership` | Modifier adhésion |
| DELETE | `/api/memberships/{id}` | - | - | Supprimer adhésion |

### **5. ÉLECTIONS** (`/api/elections`) ⚠️ *Authentification requise*

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/elections` | `clubId` (query optionnel), (header: Authorization) | `List<Election>` | Élections (filtrées par club si fourni) |
| GET | `/api/elections/{id}` | (header: Authorization) | `Election` | Election par ID |
| POST | `/api/elections` | (header: Authorization), `ElectionRequest` | `Election` | Créer élection |
| PUT | `/api/elections/{id}` | (header: Authorization), `ElectionRequest` | `Election` | Modifier élection |
| DELETE | `/api/elections/{id}` | (header: Authorization) | - | Supprimer élection |

### **6. CANDIDATS** (`/api/candidates`) ⚠️ *Authentification requise*

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/candidates` | `electionId` (query optionnel), (header: Authorization) | `List<Candidate>` | Candidats (filtrés par élection si fourni) |
| GET | `/api/candidates/{id}` | (header: Authorization) | `Candidate` | Candidat par ID |
| POST | `/api/candidates` | (header: Authorization), `CandidateRequest` | `Candidate` | Soumettre candidature |
| DELETE | `/api/candidates/{id}` | (header: Authorization) | - | Supprimer candidature |

### **7. POSTES** (`/api/positions`) ⚠️ *Authentification requise*

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/positions` | `clubId` (query optionnel), (header: Authorization) | `List<Position>` | Postes (filtrés par club si fourni) |
| GET | `/api/positions/{id}` | (header: Authorization) | `Position` | Poste par ID |
| POST | `/api/positions` | (header: Authorization), `PositionRequest` | `Position` | Créer poste |
| DELETE | `/api/positions/{id}` | (header: Authorization) | - | Supprimer poste |

### **8. VOTES** (`/api/votes`) ⚠️ *Authentification requise*

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/votes` | `electionId` (query optionnel), (header: Authorization) | `List<Vote>` | Votes (filtrés par élection si fourni) |
| GET | `/api/votes/{id}` | (header: Authorization) | `Vote` | Vote par ID |
| POST | `/api/votes` | (header: Authorization), `VoteRequest` | `Vote` | Voter (vote de l'utilisateur authentifié) |

### **9. ÉVÉNEMENTS** (`/api/events`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/events` | - | `List<Event>` | Tous les événements |
| GET | `/api/events/{id}` | - | `Event` | Événement par ID |
| POST | `/api/events` | `Event` | `Event` | Créer événement |
| PUT | `/api/events/{id}` | `Event` | `Event` | Modifier événement |
| DELETE | `/api/events/{id}` | - | - | Supprimer événement |

### **10. PARTICIPANTS AUX ÉVÉNEMENTS** (`/api/event-participants`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/event-participants` | - | `List<EventParticipant>` | Tous les participants |
| GET | `/api/event-participants/{id}` | - | `EventParticipant` | Participant par ID |
| POST | `/api/event-participants` | `EventParticipant` | `EventParticipant` | Ajouter participant |
| PUT | `/api/event-participants/{id}` | `EventParticipant` | `EventParticipant` | Modifier participant |
| DELETE | `/api/event-participants/{id}` | - | - | Supprimer participant |

### **11. RÉSERVATIONS** (`/api/reservations`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/reservations` | - | `List<Reservation>` | Toutes les réservations |
| GET | `/api/reservations/{id}` | - | `Reservation` | Réservation par ID |
| POST | `/api/reservations` | `ReservationRequest` | `Reservation` | Créer réservation |
| PUT | `/api/reservations/{id}` | `ReservationRequest` | `Reservation` | Modifier réservation |
| DELETE | `/api/reservations/{id}` | - | - | Supprimer réservation |

### **12. LIEUX** (`/api/locations`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/locations` | - | `List<Location>` | Tous les lieux |
| GET | `/api/locations/{id}` | - | `Location` | Lieu par ID |
| POST | `/api/locations` | `Location` | `Location` | Créer lieu |
| PUT | `/api/locations/{id}` | `Location` | `Location` | Modifier lieu |
| DELETE | `/api/locations/{id}` | - | - | Supprimer lieu |

### **13. RESSOURCES** (`/api/resources`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/resources` | - | `List<Resource>` | Toutes les ressources |
| GET | `/api/resources/{id}` | - | `Resource` | Ressource par ID |
| POST | `/api/resources` | `ResourceRequest` | `Resource` | Créer ressource |
| PUT | `/api/resources/{id}` | `ResourceRequest` | `Resource` | Modifier ressource |
| DELETE | `/api/resources/{id}` | - | - | Supprimer ressource |

### **14. TRANSPORTS** (`/api/transports`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/transports` | - | `List<Transport>` | Tous les transports |
| GET | `/api/transports/{id}` | - | `Transport` | Transport par ID |
| POST | `/api/transports` | `TransportRequest` | `Transport` | Créer transport |
| PUT | `/api/transports/{id}` | `TransportRequest` | `Transport` | Modifier transport |
| DELETE | `/api/transports/{id}` | - | - | Supprimer transport |

### **15. VÉHICULES** (`/api/vehicles`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/vehicles` | - | `List<Vehicle>` | Tous les véhicules |
| GET | `/api/vehicles/{id}` | - | `Vehicle` | Véhicule par ID |
| POST | `/api/vehicles` | `Vehicle` | `Vehicle` | Créer véhicule |
| PUT | `/api/vehicles/{id}` | `Vehicle` | `Vehicle` | Modifier véhicule |
| DELETE | `/api/vehicles/{id}` | - | - | Supprimer véhicule |

### **16. RECRUTEMENT** (`/api/recruitment`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| POST | `/api/recruitment/campaigns` | `clubId` (query), `RecruitmentCampaign` | `RecruitmentCampaign` | Créer campagne recrutement |
| PUT | `/api/recruitment/campaigns/{id}` | `RecruitmentCampaign` | `RecruitmentCampaign` | Modifier campagne |
| GET | `/api/recruitment/campaigns/club/{clubId}` | - | `List<RecruitmentCampaign>` | Campagnes d'un club |
| GET | `/api/recruitment/campaigns/{id}` | - | `RecruitmentCampaign` | Campagne par ID |
| DELETE | `/api/recruitment/campaigns/{id}` | - | - | Supprimer campagne |
| POST | `/api/recruitment/campaigns/{id}/questions` | `CampaignQuestion` | `CampaignQuestion` | Ajouter question à campagne |
| PUT | `/api/recruitment/questions/{id}` | `CampaignQuestion` | `CampaignQuestion` | Modifier question |
| DELETE | `/api/recruitment/questions/{id}` | - | - | Supprimer question |
| POST | `/api/recruitment/campaigns/{id}/apply` | `ApplicationSubmissionDto` | `Application` | Candidater à campagne |
| GET | `/api/recruitment/campaigns/{id}/applications` | - | `List<Application>` | Applications pour campagne |

### **17. APPLICATIONS** (`/api/applications`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/applications` | - | `List<Application>` | Toutes les applications |
| GET | `/api/applications/{id}` | - | `Application` | Application par ID |
| POST | `/api/applications` | `Application` | `Application` | Créer application |
| PUT | `/api/applications/{id}` | `Application` | `Application` | Modifier application |
| DELETE | `/api/applications/{id}` | - | - | Supprimer application |

### **18. FINANCES** (`/api/budgets`, `/api/transactions`)

**Budgets** (`/api/budgets`):

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/budgets` | - | `List<Budget>` | Tous les budgets |
| GET | `/api/budgets/{id}` | - | `Budget` | Budget par ID |
| POST | `/api/budgets` | `Budget` | `Budget` | Créer budget |
| PUT | `/api/budgets/{id}` | `Budget` | `Budget` | Modifier budget |
| DELETE | `/api/budgets/{id}` | - | - | Supprimer budget |

**Transactions** (`/api/transactions`):

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/transactions` | - | `List<Transaction>` | Toutes les transactions |
| GET | `/api/transactions/{id}` | - | `Transaction` | Transaction par ID |
| POST | `/api/transactions` | `Transaction` | `Transaction` | Créer transaction |
| PUT | `/api/transactions/{id}` | `Transaction` | `Transaction` | Modifier transaction |
| DELETE | `/api/transactions/{id}` | - | - | Supprimer transaction |

### **19. COMPÉTENCES** (`/api/skills`, `/api/user-skills`)

**Skills** (`/api/skills`):

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/skills` | - | `List<Skill>` | Toutes les compétences |
| GET | `/api/skills/{id}` | - | `Skill` | Compétence par ID |
| POST | `/api/skills` | `Skill` | `Skill` | Créer compétence |
| PUT | `/api/skills/{id}` | `Skill` | `Skill` | Modifier compétence |
| DELETE | `/api/skills/{id}` | - | - | Supprimer compétence |

**User Skills** (`/api/user-skills`):

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/user-skills` | - | `List<UserSkill>` | Toutes les compétences utilisateur |
| GET | `/api/user-skills/{userId}/{skillId}` | - | `UserSkill` | Compétence utilisateur |
| POST | `/api/user-skills` | `UserSkill` | `UserSkill` | Ajouter compétence à utilisateur |
| PUT | `/api/user-skills/{userId}/{skillId}` | `UserSkill` | `UserSkill` | Modifier compétence utilisateur |
| DELETE | `/api/user-skills/{userId}/{skillId}` | - | - | Supprimer compétence utilisateur |

### **20. SPONSORING** (`/api/sponsors`, `/api/sponsorships`)

**Sponsors** (`/api/sponsors`):

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/sponsors` | - | `List<Sponsor>` | Tous les sponsors |
| GET | `/api/sponsors/{id}` | - | `Sponsor` | Sponsor par ID |
| POST | `/api/sponsors` | `Sponsor` | `Sponsor` | Créer sponsor |
| PUT | `/api/sponsors/{id}` | `Sponsor` | `Sponsor` | Modifier sponsor |
| DELETE | `/api/sponsors/{id}` | - | - | Supprimer sponsor |

**Sponsorships** (`/api/sponsorships`):

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/sponsorships` | - | `List<Sponsorship>` | Tous les sponsorages |
| GET | `/api/sponsorships/{id}` | - | `Sponsorship` | Sponsorage par ID |
| POST | `/api/sponsorships` | `Sponsorship` | `Sponsorship` | Créer sponsorage |
| PUT | `/api/sponsorships/{id}` | `Sponsorship` | `Sponsorship` | Modifier sponsorage |
| DELETE | `/api/sponsorships/{id}` | - | - | Supprimer sponsorage |

### **21. LOGISTIQUE** (`/api/inventory-transactions`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/inventory-transactions` | - | `List<InventoryTransaction>` | Toutes les transactions stock |
| GET | `/api/inventory-transactions/{id}` | - | `InventoryTransaction` | Transaction stock par ID |
| POST | `/api/inventory-transactions` | `InventoryTransactionRequest` | `InventoryTransaction` | Créer transaction stock |
| PUT | `/api/inventory-transactions/{id}` | `InventoryTransactionRequest` | `InventoryTransaction` | Modifier transaction stock |
| DELETE | `/api/inventory-transactions/{id}` | - | - | Supprimer transaction stock |

### **22. NOTIFICATIONS** (`/api/notifications`)

| Méthode | Path | Entrée | Sortie | Description |
|---------|------|--------|--------|-------------|
| GET | `/api/notifications` | `clubId` (query) | `List<Notification>` | Notifications du club (triées par date) |
| PUT | `/api/notifications/{id}/read` | - | `String` | Marquer notification comme lue |
| PUT | `/api/notifications/read-all` | `clubId` (query) | `String` | Marquer toutes notifications comme lues |
| GET | `/api/notifications/unread-count` | `clubId` (query) | `Long` | Nombre de notifications non lues |

---

## 🔐 Système d'Authentification

### Technologie: **JWT (JSON Web Token)**

**Libraire utilisée**: `com.auth0:java-jwt:4.4.0`

**Chiffrement des mots de passe**: BCrypt (`org.mindrot:jbcrypt:0.4`)

### Clé secrète:
```
hexaweb_7ell_el_beb_2026
```

### Durée d'expiration du token:
```
864_000_000 ms = 10 jours
```

### Claims du JWT:
- `subject`: ID utilisateur
- `email`: Email utilisateur
- `clubid`: ID du club (long)
- `role`: Rôle (RoleType: PRESIDENT, VICE_PRESIDENT, TREASURER, MEMBER, etc.)
- `isSuperAdmin`: Booléen
- `firstName`: Prénom
- `lastName`: Nom

### Format du token:
```
Bearer <JWT_TOKEN>
```

### Endpoints d'authentification:
- **Inscription**: `POST /api/auth/signup` → `SignupRequest` → `AuthResponse` (contient token)
- **Connexion**: `POST /api/auth/login` → `LoginRequest` → `List<MembershipDto>` (clubs de l'utilisateur)
- **Connexion club**: `POST /api/auth/login-club` → `LoginClubRequest` → `AuthResponse` (token pour ce club)
- **Connexion membre**: `POST /api/auth/login-member` → `MemberLoginRequest` → `AuthResponse` (token)
- **Renouvellement**: `POST /api/auth/refresh-token` → Nouvel `AuthResponse` (nouveau token)

### Super Admin par défaut:
```
Email: admin@cluverse.tn
Mot de passe: 123
Créé automatiquement au démarrage (AdminInitializer)
```

---

## 🌐 Configuration CORS

**Fichier**: `CorsConfig.java`

### Configuration actuelle:
```java
registry.addMapping("/**")                    // Tous les chemins
        .allowedOrigins("http://localhost:4200")  // Origine frontend
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
```

### Détails:
- **Origines autorisées**: `http://localhost:4200` (frontend Angular/React local)
- **Méthodes HTTP autorisées**: GET, POST, PUT, DELETE, OPTIONS
- **Tous les chemins** (`/**`) sont autorisés

⚠️ **Important**: Pour la production, modifier `http://localhost:4200` par le domaine réel du frontend.

---

## 📡 Communication Externe & Configuration

### Base de Données

**Type**: MySQL  
**Port**: 3306  
**Base de données**: `cluverse`  
**Chaîne de connexion**:
```
jdbc:mysql://localhost:3306/cluverse?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
```

**Identifiants**:
```
username=root
password= (vide par défaut)
```

**DDL**: `update` (Hibernate crée/met à jour automatiquement les tables)

### Email (SMTP)

**Provider**: Gmail  
**Host**: smtp.gmail.com  
**Port**: 587  
**Email**: louayzorai24@gmail.com  
**App Password**: huhf ehet gapi ryfh  

**Propriétés SMTP**:
```
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

### Stockage d'Images (Cloudinary)

**Service**: Cloudinary (cloud storage)  
**Cloud name**: dx3uxylyk  
**API Key**: 829464512539775  
**API Secret**: VbFgpPwup4RmHNj0l-CNZGmgOxE  

**Limites de fichiers**:
```
Max file size: 10MB
Max request size: 10MB
```

### Variables d'Environnement / Configuration Externe

**URL de base du frontend** (pour liens d'activation):
```
app.base-url=http://localhost:4200
```

---

## 🖥️ Configuration du Serveur

**Port par défaut**: `8081`  
**Profil Spring**: (défaut, pas de profil spécifique configuré)  
**Environnement JAVA**: Java 17  

### Démarrage:
```bash
./mvnw spring-boot:run
# ou
java -jar target/Backend-Cluverse-0.0.1-SNAPSHOT.jar
```

**Swagger/OpenAPI**: Disponible via SpringDoc OpenAPI  
**URL Swagger**: `http://localhost:8081/swagger-ui.html` (si activé)

---

## 📚 Dépendances Principales

```xml
<!-- Spring Boot Starters -->
spring-boot-starter-data-jpa      # ORM/Persistance
spring-boot-starter-web           # REST/Web
spring-boot-starter-mail          # Envoi emails
spring-boot-starter-websocket     # WebSockets (temps réel)
spring-boot-starter-webflux       # Client HTTP (Groq API)

<!-- Base de données -->
mysql-connector-j                 # Driver MySQL

<!-- Sécurité & JWT -->
java-jwt (4.4.0)                 # JWT Auth0
jbcrypt                          # Hash mots de passe

<!-- Stockage cloud -->
cloudinary-http44 (1.36.0)       # Upload images

<!-- Documentation API -->
springdoc-openapi-starter-webmvc-ui (2.8.6)  # Swagger UI

<!-- Utilitaires -->
lombok (1.18.32)                 # Annotations boilerplate

<!-- Tests -->
spring-boot-starter-test         # JUnit, Mockito, etc.
```

---

## 🎯 Types de Rôles (Énumération)

```java
RoleType:
- PRESIDENT        // Président du club
- VICE_PRESIDENT   // Vice-président
- TREASURER        // Trésorier
- MEMBER           // Membre simple
- RECRUITER        // Recruteur
- (autres rôles spécialisés possibles)
```

---

## 📊 Modèles Principaux (Entités)

### Entités Core:
- **User**: Utilisateurs du système
- **Club**: Clubs universitaires
- **Membership**: Adhésion utilisateur-club (relation many-to-many)
- **Notification**: Notifications du système

### Modules Spécialisés:

**Elections** (`election/`):
- `Election`: Élections du club
- `Candidate`: Candidats aux élections
- `Position`: Postes à pourvoir
- `Vote`: Votes des membres

**Événements** (`event/`):
- `Event`: Événements/activités
- `EventParticipant`: Participation aux événements
- `Reservation`: Réservations (lieux, ressources)
- `Location`: Lieux des événements

**Finances** (`finance/`):
- `Budget`: Budgets par club/projet
- `Transaction`: Transactions financières

**Logistique** (`logistics/`):
- `Resource`: Ressources (équipements)
- `Transport`: Services de transport
- `Vehicle`: Véhicules
- `InventoryTransaction`: Transactions stock

**Recrutement** (`recruitement/`):
- `RecruitmentCampaign`: Campagnes de recrutement
- `CampaignQuestion`: Questions du formulaire
- `Application`: Applications des candidats
- `ApplicationAnswer`: Réponses aux questions

**Compétences** (`skills/`):
- `Skill`: Catalogue de compétences
- `UserSkill`: Compétences des utilisateurs (relation many-to-many)

**Sponsoring** (`sponsoring/`):
- `Sponsor`: Sponsors externes
- `Sponsorship`: Relations club-sponsor

---

## 🔄 Flux Principaux

### 1. Inscription et Activation Club
1. `POST /api/clubs` → Création club
2. Email d'activation envoyé automatiquement
3. Club reçoit code d'activation
4. `GET /api/clubs/verify?code=...` → Activation
5. Président créé automatiquement
6. Membership créée (PRESIDENT)

### 2. Authentification Utilisateur
1. `POST /api/auth/login` → Login global (retourne clubs)
2. `POST /api/auth/login-club` → Login spécifique à un club
3. Token JWT obtenu → Utilisé en header Authorization

### 3. Recrutement
1. Club crée campagne: `POST /api/recruitment/campaigns`
2. Ajoute questions: `POST /api/recruitment/campaigns/{id}/questions`
3. Candidats postulent: `POST /api/recruitment/campaigns/{id}/apply`
4. Obtiennent réponses: `GET /api/recruitment/campaigns/{id}/applications`

### 4. Élections
1. Club crée élection: `POST /api/elections`
2. Ajoute postes: `POST /api/positions`
3. Membres candidatent: `POST /api/candidates`
4. Membres votent: `POST /api/votes`

---

## 🛠️ Outils & Documentation

- **IDE**: IntelliJ IDEA (JetBrains)
- **Build**: Maven 3
- **Documentation API**: Swagger/OpenAPI 3.0 (SpringDoc)
- **Version contrôle**: Git
- **Base de données**: MySQL 8+

---

## 📝 Notes Importantes

1. **Super Admin**: Créé automatiquement au démarrage (email: `admin@cluverse.tn`)
2. **CORS**: Limité à localhost:4200 (frontend local)
3. **JWT Secret**: Changez en production!
4. **Emails**: Gmail SMTP utilisé pour tous les emails
5. **Images**: Téléchargées via Cloudinary (nécessite internet)
6. **WebSocket**: Support websocket pour temps réel (à vérifier si utilisé)
7. **API Externe**: WebFlux suggère communication avec API externe (possiblement Groq IA)

---

## 🔗 Références

- Spring Boot 3.4.3: https://spring.io/projects/spring-boot
- JWT: https://auth0.com/blog/java-jwt-authentication/
- Cloudinary: https://cloudinary.com/
- MySQL: https://dev.mysql.com/doc/
- Swagger/OpenAPI: https://swagger.io/

---

**Date de création du contexte**: Avril 2026  
**Dernière mise à jour**: Avril 15, 2026

