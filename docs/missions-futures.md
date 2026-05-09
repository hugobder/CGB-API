# Missions Futures — CGB API (Credit General Bank)

> Document prospectif sur les évolutions techniques envisageables pour l'API Spring Boot de la CGB.  
> Version : Avril 2026 — Etat actuel : Spring Boot 3.4.2, Java 20, H2, Maven.

---

## Sommaire

1. [Migration PostgreSQL](#1-migration-postgresql)
2. [Rate Limiting](#2-rate-limiting)
3. [Versioning API](#3-versioning-api)
4. [Swagger / OpenAPI](#4-swagger--openapi)
5. [Planification de virements](#5-planification-de-virements)
6. [Multi-devises](#6-multi-devises)
7. [Tableau de bord audit](#7-tableau-de-bord-audit)
8. [Interface admin Thymeleaf](#8-interface-admin-thymeleaf)
9. [Pipeline CI/CD](#9-pipeline-cicd)
10. [Optimisation performance](#10-optimisation-performance)

---

## 1. Migration PostgreSQL

### Description

L'API utilise actuellement H2, une base de données embarquée en mémoire, ce qui convient parfaitement au développement et aux tests. Pour un environnement de production bancaire, ce moteur est insuffisant : les données ne persistent pas entre les redémarrages, la concurrence est limitée, et les fonctionnalités avancées (partitionnement, JSONB, réplication) sont absentes. Cette mission consiste à remplacer H2 par PostgreSQL comme moteur de base de données de production.

### Justification métier

Une banque manipule des données critiques et réglementées : soldes, virements, journaux d'audit. La perte de données lors d'un redémarrage applicatif est inacceptable en production. PostgreSQL offre une fiabilité ACID éprouvée, un support natif des transactions imbriquées, et répond aux exigences de conformité (RGPD, DSP2) grâce à ses mécanismes de journalisation et de sauvegarde.

### Approche technique

- Conserver le profil H2 pour les environnements `dev` et `test` via `application-dev.properties`.
- Créer un profil `prod` avec la configuration JDBC PostgreSQL.
- Remplacer les scripts SQL H2 spécifiques (types `IDENTITY`, `VARCHAR IGNORECASE`) par des équivalents PostgreSQL.
- Utiliser **Flyway** pour gérer les migrations de schéma de manière versionnée et reproductible.
- Adapter les entités JPA si des types propriétaires H2 ont été utilisés (ex. `UUID` natif, `JSONB`).
- Tester la bascule sur un conteneur Docker PostgreSQL 16 en intégration continue.

### Technologies suggérées

- PostgreSQL 16+
- Flyway 10+
- Docker / Docker Compose (environnement local)
- Spring Boot `spring.datasource.*` profils

### Complexité estimée

**Moyenne** — La migration du schéma est méthodique mais sans obstacle majeur si l'ORM JPA est correctement découplé du dialecte H2. La mise en place de Flyway introduit une discipline supplémentaire sur les évolutions du schéma.

---

## 2. Rate Limiting

### Description

Le rate limiting (limitation de débit) consiste à restreindre le nombre de requêtes qu'un client peut émettre vers l'API sur une période donnée. Sans cette protection, l'API est vulnérable aux attaques par force brute sur l'authentification, aux abus de la part de clients mal configurés, et aux dénis de service applicatifs.

### Justification métier

Dans un contexte bancaire, les endpoints sensibles (authentification JWT, création de virements, consultation de soldes) doivent être protégés contre les tentatives de scraping massif ou d'automatisation abusive. Le rate limiting est une exigence de sécurité reconnue par l'OWASP API Security Top 10 (API4:2023 — Unrestricted Resource Consumption).

### Approche technique

- Intégrer la bibliothèque **Bucket4j** qui implémente l'algorithme Token Bucket, nativement compatible avec Spring Boot.
- Définir des règles par endpoint et par identité (adresse IP, identifiant JWT `sub`).
- Exposer un filtre Spring Security ou un `HandlerInterceptor` qui consomme un token avant d'autoriser la requête.
- Retourner une réponse `429 Too Many Requests` avec l'en-tête `Retry-After` pour informer le client.
- Stocker les compteurs dans **Redis** pour garantir la cohérence en cas de déploiement multi-instance.
- Configurer des seuils différenciés par rôle : un administrateur aura des quotas plus élevés qu'un client standard.

### Technologies suggérées

- Bucket4j 8+
- Redis (via Spring Data Redis) pour le stockage distribué des compteurs
- Spring Boot `HandlerInterceptor` ou filtre Spring Security

### Complexité estimée

**Faible à Moyenne** — L'intégration de Bucket4j est rapide pour un déploiement mono-instance. L'ajout de Redis pour la distribution des compteurs augmente la complexité opérationnelle mais reste bien documenté.

---

## 3. Versioning API

### Description

Le versioning d'API consiste à maintenir plusieurs versions coexistantes des contrats d'interface (ex. `/api/v1/virements`, `/api/v2/virements`), permettant aux clients existants de continuer à fonctionner pendant qu'une nouvelle version est déployée avec des changements incompatibles.

### Justification métier

Une API bancaire est consommée par des clients tiers (applications mobiles, partenaires, front-ends internes) qui ne peuvent pas être mis à jour simultanément. Modifier un endpoint existant sans versioning provoque des ruptures de contrat. Le versioning garantit la rétrocompatibilité et permet d'introduire des améliorations (nouveaux champs, changement de sémantique) sans casser les intégrations existantes.

### Approche technique

- Adopter le versioning par **préfixe d'URL** (`/api/v1/`, `/api/v2/`) qui est la stratégie la plus lisible et la mieux supportée par les outils de documentation.
- Utiliser le mécanisme de `@RequestMapping` de Spring MVC avec des préfixes explicites par version.
- Créer un package séparé par version (`controller.v1`, `controller.v2`) pour isoler les contrats.
- Définir des DTOs versionnés distincts, les entités JPA restant communes.
- Établir une politique de dépréciation : une version reste supportée N mois après la mise en production de la suivante, avec des en-têtes `Deprecation` et `Sunset` dans les réponses.
- Documenter la politique de versioning dans Swagger (voir mission 4).

### Technologies suggérées

- Spring MVC `@RequestMapping` avec préfixe de version
- Jackson pour la sérialisation différenciée des DTOs
- En-têtes HTTP `Deprecation` / `Sunset` (RFC 8594)

### Complexité estimée

**Moyenne** — La mise en place initiale est simple, mais la gestion à long terme de plusieurs versions parallèles implique une discipline d'architecture et un effort de maintenance non négligeable sur les tests de régression.

---

## 4. Swagger / OpenAPI

### Description

Swagger / OpenAPI permet de générer automatiquement une documentation interactive des endpoints de l'API à partir des annotations et du code source. Les développeurs consommateurs peuvent explorer les routes, tester des requêtes directement depuis le navigateur, et générer des clients dans leur langage cible.

### Justification métier

La CGB a vocation à exposer son API à des partenaires et à des équipes front-end internes. Sans documentation formelle, chaque intégration requiert un effort manuel de communication. Une documentation vivante, synchronisée avec le code, réduit les erreurs d'intégration et accélère l'onboarding des nouveaux développeurs.

### Approche technique

- Ajouter la dépendance **springdoc-openapi** (compatible Spring Boot 3.x, contrairement à springfox qui n'est plus maintenu).
- Annoter les contrôleurs avec `@Operation`, `@ApiResponse`, `@Parameter` pour enrichir la documentation générée.
- Configurer la sécurité JWT dans le schéma OpenAPI (`SecurityScheme` de type `http bearer`) afin que les tests depuis l'UI Swagger puissent inclure le token d'authentification.
- Regrouper les endpoints par tag métier (`Virements`, `Clients`, `Lots`, `Rapports`).
- Exposer l'interface Swagger UI uniquement sur les profils `dev` et `staging`, pas en production (restriction via `@ConditionalOnProperty`).
- Générer un fichier `openapi.json` statique lors du build Maven pour archivage et partage.

### Technologies suggérées

- springdoc-openapi-starter-webmvc-ui 2.x
- Swagger UI (inclus dans springdoc)
- Maven plugin `springdoc-openapi-maven-plugin` pour la génération statique

### Complexité estimée

**Faible** — L'intégration de springdoc est rapide (ajout de dépendance + configuration minimale). L'enrichissement progressif avec des annotations est incrémental et ne bloque pas le développement.

---

## 5. Planification de virements

### Description

Cette mission vise à permettre à un utilisateur de programmer un virement à une date future, ou de configurer des virements récurrents (hebdomadaires, mensuels). Un scheduler exécute automatiquement ces virements à l'échéance, en appliquant les mêmes règles de validation et de sécurité que les virements immédiats.

### Justification métier

Les virements différés et récurrents sont une fonctionnalité bancaire standard (paiements de loyers, remboursements de crédits, épargne automatique). Leur absence limite fortement l'utilité de l'API pour des cas d'usage réels. Du point de vue métier, les virements planifiés représentent également un engagement juridique dès leur création, nécessitant une traçabilité rigoureuse.

### Approche technique

- Ajouter une entité `VirementPlanifie` avec les champs : montant, IBAN source/destinataire, date d'exécution cible, périodicité (optionnelle), statut (`EN_ATTENTE`, `EXECUTE`, `ECHOUE`, `ANNULE`).
- Utiliser **Spring Scheduler** (`@Scheduled`) pour un polling régulier des virements dont la date d'échéance est dépassée.
- Pour les cas à haute volumétrie ou nécessitant une précision à la seconde, envisager **Quartz Scheduler** avec persistance en base de données.
- Intégrer le mécanisme de replay existant pour rejouer les virements planifiés échoués.
- Gérer les cas limites : solde insuffisant au moment de l'exécution, compte désactivé, jour férié.
- Exposer des endpoints REST pour créer, modifier, annuler et consulter les virements planifiés.

### Technologies suggérées

- Spring `@Scheduled` (simple) ou Quartz 2.x avec `JobStore` JPA (avancé)
- Spring Boot `@EnableScheduling`
- Entité JPA `VirementPlanifie` avec index sur la colonne `dateExecution`

### Complexité estimée

**Moyenne** — Le cas simple (Spring Scheduler + polling) est rapide à mettre en oeuvre. La gestion robuste des erreurs, des cas limites et la persistance Quartz augmentent significativement la complexité.

---

## 6. Multi-devises

### Description

Le support multi-devises permet à l'API de gérer des comptes et des virements dans plusieurs devises (EUR, USD, GBP, CHF, etc.), avec conversion automatique lors de virements inter-devises. Cela implique la gestion de taux de change, leur mise à jour régulière, et l'arrondi conforme aux normes ISO 4217.

### Justification métier

Dans un contexte de banque internationale ou de clientèle expatriée, la limitation à une seule devise est un frein majeur. La réglementation impose une transparence totale sur les taux de change appliqués et les frais de conversion, notamment dans le cadre de la DSP2. Cette fonctionnalité ouvre également la voie à des produits financiers plus sophistiqués.

### Approche technique

- Ajouter un champ `devise` (code ISO 4217, ex. `"EUR"`) sur les entités `Compte` et `Virement`.
- Créer un service `TauxDeChangeService` qui récupère les taux depuis une API externe (ex. **Frankfurter API**, open-source) et les met en cache localement.
- Utiliser `BigDecimal` pour tous les montants (jamais `double`) et appliquer les règles d'arrondi `RoundingMode.HALF_UP` conformément aux normes bancaires.
- Stocker les taux de change historiques en base pour permettre l'audit des conversions passées.
- Exposer un endpoint `/api/v1/devises/taux` pour consultation des taux en vigueur.
- Gérer les erreurs de récupération des taux (fallback sur le dernier taux connu, alertes).

### Technologies suggérées

- Frankfurter API (taux BCE, gratuit) ou Open Exchange Rates
- Spring `@Scheduled` pour la mise à jour périodique des taux
- Spring Cache (`@Cacheable`) pour éviter des appels HTTP répétitifs
- `java.util.Currency` et `BigDecimal` pour la précision numérique

### Complexité estimée

**Elevée** — La gestion correcte des devises touche au modèle de données, à la logique métier, aux règles réglementaires et à la dépendance externe. Chaque erreur de conversion ou d'arrondi peut avoir des conséquences financières directes.

---

## 7. Tableau de bord audit

### Description

Un tableau de bord d'audit centralise l'historique de toutes les opérations effectuées sur l'API : qui a fait quoi, quand, depuis quelle adresse IP, avec quel résultat. Il s'agit d'un journal d'événements immuable et consultable, destiné aux administrateurs et aux équipes de conformité.

### Justification métier

Les établissements financiers sont soumis à des obligations légales strictes en matière de traçabilité (directive DORA, RGPD, réglementation bancaire). En cas d'incident ou de fraude, l'audit trail est la première source d'investigation. Sa présence est souvent une condition préalable aux audits de certification (ISO 27001, PCI-DSS).

### Approche technique

- Créer une entité `EvenementAudit` : timestamp, utilisateur, action (ex. `VIREMENT_CREE`), ressource concernée, adresse IP, résultat (`SUCCES` / `ECHEC`), détails JSON.
- Implémenter un `ApplicationEventListener` Spring ou un aspect AOP (`@Around`) pour capturer automatiquement les événements sur les services critiques sans polluer le code métier.
- Rendre les entrées d'audit immuables en base (pas d'UPDATE ni de DELETE — contrainte applicative et base de données).
- Exposer des endpoints de consultation avec filtres (par utilisateur, par période, par type d'action) et pagination.
- Protéger ces endpoints par le rôle `ADMIN` via Spring Security.
- Envisager l'export CSV / PDF des journaux pour les besoins de conformité.

### Technologies suggérées

- Spring AOP (`@Aspect`, `@Around`) ou `ApplicationEventPublisher`
- Spring Data JPA avec `@Immutable` sur l'entité audit
- Pagination Spring Data (`Pageable`)
- iText ou Apache POI pour l'export PDF/Excel (optionnel)

### Complexité estimée

**Moyenne** — La mécanique de capture via AOP est élégante et non intrusive. La difficulté principale réside dans la définition exhaustive des événements à tracer et dans la garantie d'immutabilité des données.

---

## 8. Interface admin Thymeleaf

### Description

Cette mission consiste à développer une interface web d'administration accessible depuis un navigateur, construite avec le moteur de templates **Thymeleaf** intégré à Spring Boot. Elle permettrait de gérer les comptes clients, visualiser les lots de traitement, consulter les journaux d'audit et déclencher des replays, sans passer par l'API REST.

### Justification métier

Les équipes opérationnelles (support bancaire, administrateurs système) ne disposent pas toujours d'outils pour interagir avec des APIs REST. Une interface web dédiée réduit le risque d'erreur humaine, accélère les interventions de support et permet de déléguer certaines tâches administratives sans former les agents à l'utilisation de Postman ou d'outils similaires.

### Approche technique

- Ajouter la dépendance `spring-boot-starter-thymeleaf` et créer un module `web` distinct du module `api`.
- Protéger toutes les routes web par un formulaire de connexion Spring Security (session HTTP, distinct de l'authentification JWT de l'API).
- Créer des vues Thymeleaf pour : liste des clients, détail d'un client, liste des lots avec statuts, journal d'audit, gestion des rôles.
- Utiliser **Bootstrap 5** pour un rendu professionnel sans développement CSS custom.
- Implémenter la pagination côté serveur via `Pageable` Spring Data pour les listes volumineuses.
- Dissocier clairement les `@Controller` web (retournant des vues) des `@RestController` API (retournant du JSON).

### Technologies suggérées

- Thymeleaf 3.x (inclus dans Spring Boot starter)
- Bootstrap 5 (via CDN ou WebJars)
- Spring Security (formulaire de login dédié)
- Spring MVC `@Controller` + `Model`

### Complexité estimée

**Moyenne** — Thymeleaf s'intègre naturellement à Spring Boot. La complexité principale est organisationnelle : maintenir la cohérence entre la logique API et la logique web, et gérer deux mécanismes d'authentification coexistants (JWT pour l'API, session pour le web).

---

## 9. Pipeline CI/CD

### Description

Un pipeline CI/CD (Intégration Continue / Déploiement Continu) automatise la vérification de chaque modification de code (compilation, tests, analyse qualité) et, optionnellement, le déploiement sur un environnement cible. Cette mission cible la mise en place d'un pipeline via **GitHub Actions**.

### Justification métier

Sans automatisation, la qualité du code repose sur la discipline individuelle des développeurs. Dans un contexte bancaire, une régression en production peut avoir des conséquences financières et réputationnelles graves. Le CI/CD garantit qu'aucune modification n'atteint la production sans avoir été validée par une suite de tests, et réduit le délai entre le développement d'une fonctionnalité et sa mise à disposition.

### Approche technique

- Créer un fichier `.github/workflows/ci.yml` déclenché sur chaque `push` et `pull_request` vers `master`.
- Étapes du pipeline CI :
  1. Checkout du code
  2. Configuration du JDK 20 (`actions/setup-java`)
  3. Compilation et exécution des tests (`mvn verify`)
  4. Analyse de qualité avec **SonarCloud** (détection de code mort, vulnérabilités, dette technique)
  5. Génération du rapport de couverture JaCoCo
- Étapes optionnelles CD :
  1. Construction de l'image Docker (`Dockerfile` multi-stage)
  2. Push vers un registre (GitHub Container Registry, Docker Hub)
  3. Déploiement sur un VPS ou une plateforme cloud (Render, Railway, Fly.io)
- Utiliser les secrets GitHub pour les variables sensibles (credentials base de données, clé JWT de test).

### Technologies suggérées

- GitHub Actions (`.github/workflows/`)
- Maven Wrapper (`mvnw`) pour la reproductibilité des builds
- JaCoCo pour la couverture de tests
- SonarCloud (tier gratuit pour projets open-source)
- Docker + Dockerfile multi-stage

### Complexité estimée

**Faible à Moyenne** — La configuration de base (CI avec tests Maven) est rapide. L'ajout de SonarCloud, Docker et du déploiement automatisé augmente progressivement la complexité mais chaque étape est indépendante et peut être ajoutée itérativement.

---

## 10. Optimisation performance

### Description

Au fur et à mesure que le volume de données et le nombre d'utilisateurs augmentent, certaines opérations de l'API peuvent devenir des goulets d'étranglement. Cette mission regroupe plusieurs optimisations : pagination des résultats, mise en cache des données fréquemment lues, et traitement parallèle des lots volumineux.

### Justification métier

Une API bancaire en production peut traiter des milliers de virements par jour et gérer des centaines de milliers de comptes clients. Sans pagination, une requête `GET /clients` charge l'intégralité de la table en mémoire, ce qui peut provoquer des `OutOfMemoryError`. Le cache réduit la charge sur la base de données pour les données peu volatiles (taux de change, référentiels). Le parallélisme accélère le traitement des lots asynchrones existants.

### Approche technique

**Pagination :**
- Remplacer les `List<T>` par `Page<T>` dans les repositories Spring Data (`PagingAndSortingRepository`).
- Accepter les paramètres `page`, `size`, `sort` dans les endpoints REST via `Pageable`.
- Retourner des réponses paginées avec métadonnées (`totalElements`, `totalPages`, liens HATEOAS optionnels).

**Cache applicatif :**
- Activer Spring Cache (`@EnableCaching`) avec un provider **Caffeine** (cache en mémoire, très performant).
- Annoter les méthodes de service avec `@Cacheable`, `@CacheEvict`, `@CachePut` selon la volatilité des données.
- Envisager **Redis** comme cache distribué si l'application est déployée en plusieurs instances.

**Traitement parallèle des lots :**
- Le traitement asynchrone des lots existe déjà. L'optimisation consiste à utiliser un `ThreadPoolTaskExecutor` configuré finement (taille du pool adaptée au nombre de coeurs disponibles).
- Décomposer les grands lots en sous-lots traités en parallèle via `CompletableFuture` ou l'API Stream parallèle.
- Ajouter des métriques de performance sur les lots (temps de traitement, taux d'erreur) via **Spring Boot Actuator** et **Micrometer**.

### Technologies suggérées

- Spring Data `Pageable` / `Page<T>`
- Caffeine Cache (via `spring-boot-starter-cache` + `caffeine`)
- Redis (optionnel, pour le cache distribué)
- Spring Boot Actuator + Micrometer + Prometheus/Grafana (observabilité)
- `ThreadPoolTaskExecutor` pour le parallélisme des lots

### Complexité estimée

**Moyenne à Elevée** — La pagination et le cache Caffeine sont des améliorations rapides. L'optimisation fine du parallélisme des lots et la mise en place d'une stack d'observabilité complète (Prometheus, Grafana) représentent un investissement technique significatif.

---

## Synthèse et priorisation suggérée

| # | Mission | Complexité | Impact métier | Priorité suggérée |
|---|---------|-----------|---------------|-------------------|
| 4 | Swagger / OpenAPI | Faible | Elevé | Immédiate |
| 9 | Pipeline CI/CD | Faible–Moyenne | Elevé | Immédiate |
| 1 | Migration PostgreSQL | Moyenne | Critique | Court terme |
| 3 | Versioning API | Moyenne | Elevé | Court terme |
| 2 | Rate Limiting | Faible–Moyenne | Elevé | Court terme |
| 5 | Planification de virements | Moyenne | Elevé | Moyen terme |
| 7 | Tableau de bord audit | Moyenne | Elevé | Moyen terme |
| 10 | Optimisation performance | Moyenne–Elevée | Moyen | Moyen terme |
| 8 | Interface admin Thymeleaf | Moyenne | Moyen | Long terme |
| 6 | Multi-devises | Elevée | Dépend du marché | Long terme |

> Les missions de priorité **Immédiate** ont le meilleur rapport effort/valeur : elles améliorent la qualité du projet sans impacter le modèle de données ni les contrats d'interface existants.

---

*Document interne CGB — Credit General Bank. Usage technique, non contractuel.*
