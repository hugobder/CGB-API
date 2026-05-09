# Rapport de Réalisation — API CGB (Credit General Bank)

**Projet :** Api de gestion des transferts bancaires  
**Artefact Maven :** `credit-general-banque:credit-general-banque-api:0.0.1-SNAPSHOT`  
**Date de rédaction :** 17 avril 2026  
**Technologies :** Spring Boot 3.4.2 · Java 20 · Maven · H2 (fichier persistant)

---

## Table des matières

1. [Introduction](#1-introduction)
2. [M1.2 — Conformité IBAN](#2-m12--conformité-iban)
3. [M1.3 — Correctifs fonctionnels](#3-m13--correctifs-fonctionnels)
4. [M1.6 — Comptes courants et bénéficiaires](#4-m16--comptes-courants-et-bénéficiaires)
5. [M1.4 — Traitement des lots](#5-m14--traitement-des-lots)
6. [M1.5 — Notifications et rapports](#6-m15--notifications-et-rapports)
7. [M1.7 — Rejeu des transactions](#7-m17--rejeu-des-transactions)
8. [M1.8 — Sécurisation JWT](#8-m18--sécurisation-jwt)
9. [Tableau récapitulatif des endpoints](#9-tableau-récapitulatif-des-endpoints)
10. [Vue d'ensemble des entités](#10-vue-densemble-des-entités)
11. [Tests](#11-tests)
12. [Conclusion](#12-conclusion)

---

## 1. Introduction

### Contexte du projet

Le projet CGB (Credit General Bank) est une API REST développée pour la gestion des virements bancaires. Elle est réalisée dans le cadre d'un projet pédagogique structuré en missions progressives, chacune apportant une couche fonctionnelle supplémentaire à l'application.

L'API est construite sur Spring Boot 3.4.2 avec Java 20, persistant ses données dans une base H2 stockée sur fichier (`./db/db_cgb`). Elle expose des endpoints REST consommables via des clients HTTP (navigateur, Postman, applications front-end).

### Client : GSB (Galaxy Swiss Bourdin)

Le client de référence utilisé tout au long du développement est **Galaxy Swiss Bourdin (GSB)**, une entité fictive domiciliée au 15 Rue de la Paix, 75002 Paris, identifiée par le code LEI `529900T8BM49AURSDO55`. GSB dispose de cinq comptes courants sources et de quinze comptes bénéficiaires, initialisés automatiquement au démarrage de l'application par le composant `DatabaseInitializer`.

Deux utilisateurs de test représentent les profils fonctionnels attendus :
- **padelphi** (Phil Adelphi) — rôle `COMPTABLE`, autorisé à créer, modifier et supprimer des ressources
- **patchaude** (Pat Atchaude) — rôle `UTILISATEUR`, limité aux consultations

---

## 2. M1.2 — Conformité IBAN

### Objectif

Implémenter un mécanisme de validation robuste des numéros IBAN utilisés comme identifiants de comptes, en distinguant les erreurs de format des erreurs de CRC (clé de contrôle).

### Ce qui a été réalisé

#### Validateur singleton : `CGBIbanValidator`

La classe `cgb.utils.CGBIbanValidator` implémente le **patron de conception Singleton**. Elle expose une unique instance via la méthode statique `getInstanceValidator()` et encapsule une instance de `IBANValidator` issue de la bibliothèque `commons-validator:1.9.0`.

La validation s'effectue en deux étapes successives :

1. **Validation structurelle** (`isIbanStructureValide`) : vérification par expression régulière que l'IBAN commence par deux lettres majuscules, deux chiffres de contrôle, puis entre 11 et 30 caractères alphanumériques — `^[A-Z]{2}\d{2}[A-Za-z0-9]{11,30}$`
2. **Validation du CRC** (`isIbanValide`) : délégation à `commons-validator` pour vérifier la clé de contrôle par l'algorithme modulo 97

La méthode publique `validate(String iban)` orchestre ces deux vérifications et lève l'exception appropriée en cas d'échec. Des méthodes utilitaires (`getCountryCode`, `getCheckDigits`, `getBBAN`) permettent d'extraire les différentes parties de l'IBAN.

#### Hiérarchie d'exceptions IBAN

Une hiérarchie de classes d'exception propre a été définie :

```
ExceptionInvalideIBAN (abstraite, extends Exception)
├── InvalidIbanFormatException     — format structurel incorrect
└── InvalidUnCheckableIbanException — CRC invalide malgré un format correct
```

Cette séparation permet aux appelants de distinguer précisément la nature de l'erreur de validation.

#### Générateur d'IBAN fictifs : `IbanGenerator`

La classe utilitaire `cgb.utils.IbanGenerator` génère des IBAN français (`FR`) valides (structure + CRC) à des fins de test et d'initialisation. Elle implémente l'algorithme standard de calcul des chiffres de contrôle : réarrangement de la chaîne, conversion alphabétique (A=10, …, Z=35), calcul de `98 - (nombre mod 97)` avec `BigInteger` pour éviter les dépassements entiers.

#### Initialisation de la base : `DatabaseInitializer`

Le composant `cgb.transfer.exemple.DatabaseInitializer` (annoté `@Component`) utilise `@PostConstruct` pour peupler la base de données au premier démarrage (condition `if (accountRepository.count() == 0)`). Il crée 20 comptes dont les numéros sont des IBAN français valides générés par `IbanGenerator`, avec des soldes initiaux allant de 300 € à 5 000 €.

### Décisions d'architecture

- Le choix du **Singleton** pour `CGBIbanValidator` évite d'instancier répétitivement l'objet `IBANValidator` de commons-validator, qui est coûteux à initialiser.
- La délégation à commons-validator garantit une conformité avec les règles IBAN internationales sans réimplémenter l'algorithme complet.
- La double validation (format puis CRC) produit des messages d'erreur plus précis qu'une validation monolithique.

---

## 3. M1.3 — Correctifs fonctionnels

### Objectif

Corriger le comportement de l'API de virement en ajoutant des validations métier manquantes et en restituant des codes HTTP sémantiquement corrects.

### Ce qui a été réalisé

#### Hiérarchie d'exceptions métier

Une classe abstraite `TransferException` (qui étend `Exception`) fédère toutes les exceptions liées aux virements :

```
TransferException (abstraite, extends Exception)
├── NegativeAmountException         — montant nul ou négatif
├── AntidatedTransferException      — date de virement dans le passé
├── InsufficientFundsException      — solde insuffisant sur le compte source
├── AccountNotFoundException        — compte source ou destinataire introuvable
├── UnauthorizedAccountException    — compte non autorisé pour le client (M1.6)
└── DeleteTransferException         — échec de suppression (enum OBJECT_NOT_FOUND / REMOVAL_FAILURE)
```

#### Validations dans `TransferService`

La méthode `createTransfer` de `cgb.transfer.service.TransferService` applique les règles métier dans l'ordre suivant :

1. **Montant positif strict** : lève `NegativeAmountException` si `amount` est `null`, nul ou négatif
2. **Date non antidatée** : si `transferDate` est `null`, la date du jour est assignée automatiquement ; si elle est dans le passé, lève `AntidatedTransferException`
3. **Existence des comptes** : interroge `AccountRepository` par IBAN ; lève `AccountNotFoundException` si le compte source ou le compte destinataire est absent
4. **Autorisation client** (optionnelle, M1.6) : si un `customerId` est fourni, vérifie que le compte source appartient à `myaccounts` du client et que le compte destinataire est dans sa liste `recipientAccounts`
5. **Solde suffisant** : compare le solde du compte source avec le montant ; lève `InsufficientFundsException` si insuffisant

La méthode est annotée `@Transactional` : en cas d'exception, aucune modification de solde n'est persistée (principe du tout-ou-rien).

#### Codes HTTP dans `TransferRestController`

Le contrôleur `cgb.transfer.controller.TransferRestController` traduit les exceptions en codes HTTP appropriés :

| Exception | Code HTTP |
|---|---|
| `UnauthorizedAccountException` | 403 Forbidden |
| `AccountNotFoundException` | 404 Not Found |
| `TransferException` (autres) | 400 Bad Request |
| Succès | 200 OK |

Une classe interne `TransferResponse` (champs `status`, `message`) fournit un corps de réponse uniforme pour les cas d'erreur.

---

## 4. M1.6 — Comptes courants et bénéficiaires

### Objectif

Introduire une notion de client bancaire possédant ses propres comptes courants et une liste de bénéficiaires autorisés, et conditionner les virements à ces autorisations.

### Ce qui a été réalisé

#### Nouvelles entités

Trois nouvelles entités JPA ont été introduites :

- **`Role`** : entité simple (`id`, `name`) représentant un rôle applicatif (`COMPTABLE`, `UTILISATEUR`)
- **`UserCGB`** : utilisateur de l'application (`id`, `username`, `password`, `email`), lié à un `Role` par `@ManyToOne` et à un `Customer` par `@ManyToOne` (champ `belongTo`)
- **`Customer`** : client bancaire (`id`, `name`, `address`, `lei`) possédant deux relations `@ManyToMany` vers `Account` :
  - `myaccounts` — comptes courants dont le client est propriétaire (table de jointure `customer_accounts`)
  - `recipientAccounts` — comptes bénéficiaires autorisés (table de jointure `customer_recipient_accounts`)

#### Logique d'autorisation

La méthode `TransferService.createTransfer` accepte un paramètre optionnel `Long customerId`. Lorsqu'il est fourni et que le client existe en base, elle effectue deux vérifications :

- Le compte source doit figurer dans `customer.getMyaccounts()`
- Le compte destinataire doit figurer dans `customer.getRecipientAccounts()`

Tout manquement lève une `UnauthorizedAccountException`, renvoyée en `403 Forbidden` par le contrôleur.

#### Initialisation GSB

`DatabaseInitializer` instancie le client GSB avec les 5 premiers comptes comme comptes courants (`myaccounts`) et les 15 comptes restants comme bénéficiaires (`recipientAccounts`).

### Décisions d'architecture

- La relation `@ManyToMany` modélise naturellement le fait qu'un même compte peut appartenir à plusieurs clients (multi-tenancy partiel).
- Le paramètre `customerId` étant optionnel, l'API reste rétrocompatible : les appels sans contexte client ne subissent pas de vérification d'autorisation.

---

## 5. M1.4 — Traitement des lots

### Objectif

Permettre la soumission d'un ensemble de virements regroupés dans un lot, traité de façon asynchrone, avec suivi d'état détaillé par virement.

### Ce qui a été réalisé

#### Entités du lot

- **`Lot`** : entête du lot (`id`, `refLot`, `sourceAccount`, `descriptionLot`, `dateLot`, `state`), lié à une liste de `TransferLot` par `@OneToMany(cascade = ALL, fetch = EAGER)`
- **`TransferLot`** : ligne de virement au sein d'un lot (`id`, `destAccount`, `amount`, `description`, `completionDate`, `state`), lié au lot parent par `@ManyToOne @JsonIgnore` pour éviter la sérialisation circulaire

#### DTOs

- **`LotRequest`** : corps de la requête de soumission (`refLot`, `sourceAccount`, `descriptionLot`, `List<VirementRequest>`)
- **`VirementRequest`** : descripteur d'un virement individuel (`destAccount`, `amount`, `description`)
- **`LotResponse`** : réponse immédiate de soumission (`numLot`, `dateLancement`, `message`, `etat`)

#### Machine à états

| État | Signification |
|---|---|
| `waiting` | Virement en attente de traitement (état initial de chaque `TransferLot`) |
| `received` | Lot reçu et enregistré (état initial du `Lot`) |
| `success` | Virement exécuté avec succès |
| `failure` | Virement échoué (compte destinataire introuvable) |
| `delayed` | Virement différé (solde insuffisant au moment du traitement) |
| `canceled` | Virement annulé (utilisé dans les filtres de rapport) |
| `closed` | Lot entièrement traité (état final du `Lot`) |

#### Traitement asynchrone dans `LotService`

La classe `cgb.transfer.service.LotService` orchestre le cycle de vie complet d'un lot :

1. **`submitLot(LotRequest)`** : crée le `Lot` en état `received`, persiste chaque `TransferLot` en état `waiting`, puis appelle immédiatement `processLot` de façon asynchrone.
2. **`processLot(Long lotId)`** (annotée `@Async`) : exécutée dans un thread séparé grâce à l'annotation `@EnableAsync` sur la classe principale `ServerTransferApp`. Elle charge le lot, récupère les virements en état `waiting`, puis appelle `processVirement` pour chacun.
3. **`processVirement(TransferLot, Account)`** (annotée `@Transactional`) : tente le débit/crédit et met à jour l'état du virement (`success`, `failure`, ou `delayed`). Le lot est finalement passé en `closed`.

#### Contrôleur `LotRestController`

Mappé sur `/api/lots`, il expose la soumission et la consultation des lots, ainsi que les endpoints de rapport et de rejeu (voir sections suivantes).

### Décisions d'architecture

- `@Async` sur `processLot` garantit que la réponse HTTP est retournée immédiatement au client sans attendre la fin du traitement, ce qui est indispensable pour des lots potentiellement volumineux.
- Le fetch `EAGER` sur `Lot.virements` simplifie les accès aux virements sans requête supplémentaire, acceptable pour des lots de taille raisonnable.
- `@Transactional` sur `processVirement` isole chaque virement : un échec sur un virement ne compromet pas les suivants.

---

## 6. M1.5 — Notifications et rapports

### Objectif

Fournir des mécanismes de rapport détaillé sur l'état d'un lot et d'envoi de notifications par email lors de la complétion d'un traitement.

### Ce qui a été réalisé

#### `ReportService`

La classe `cgb.transfer.service.ReportService` expose quatre méthodes de consultation :

- **`generateLotReport(Long lotId)`** : retourne une `Map<String, Object>` contenant l'identifiant, la référence, la date, l'état global du lot, le nombre total de virements, et les décomptes par état (`success`, `failure`, `delayed`, `canceled`, `waiting`), ainsi que la liste complète des virements.
- **`getFailedTransfersByLot(Long lotId)`** : retourne les `TransferLot` d'un lot dans les états `failure`, `delayed` ou `canceled`.
- **`getFailedTransfersByDateRange(LocalDate from, LocalDate to)`** : recherche par plage de dates de complétion des virements en échec.
- **`getFailedTransfersByDestAccount(String destAccount)`** : filtre les virements en échec par compte destinataire.

Les états considérés comme "en échec" sont définis dans une constante : `List.of("failure", "delayed", "canceled")`.

#### `NotificationService`

La classe `cgb.transfer.service.NotificationService` utilise `JavaMailSender` (Spring Boot Starter Mail) pour envoyer des emails de récapitulatif. La méthode `sendLotCompletionEmail(Long lotId, String recipientEmail)` construit un `SimpleMailMessage` indiquant la référence du lot, sa date, le nombre de virements réussis et le nombre d'échecs. L'expéditeur est fixé à `noreply@cgb.com`.

La configuration SMTP est externalisée dans `application.properties` (`spring.mail.*`), ce qui permet de la modifier sans recompilation.

#### Endpoints de rapport

Intégrés dans `LotRestController` :

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/lots/{id}/report` | Rapport complet d'un lot |
| GET | `/api/lots/{id}/failures` | Virements en échec d'un lot |
| GET | `/api/lots/failures?from=&to=` | Virements en échec par plage de dates |
| GET | `/api/lots/failures/account/{destAccount}` | Virements en échec par compte destinataire |

---

## 7. M1.7 — Rejeu des transactions

### Objectif

Permettre de rejouer automatiquement les virements qui ont échoué ou été différés, sans ressaisie manuelle des données.

### Ce qui a été réalisé

Le rejeu est implémenté dans `LotService` via deux méthodes et exposé par deux endpoints dans `LotRestController`.

#### `replayFromDelayed(Long lotId)`

Retrouve tous les `TransferLot` d'un lot donné dont l'état est `delayed`. Si la liste n'est pas vide, construit un nouveau `LotRequest` de rejeu en préfixant la référence du lot original de `-REJEU` et en préfixant chaque description de `REJEU - `. Retourne `null` si aucun virement différé n'est trouvé.

#### `replayFromIds(List<Long> virementIds)`

Charge les `TransferLot` correspondant aux identifiants fournis, puis filtre ceux dont l'état n'est **pas** `success`. Si tous sont en succès, retourne `null`. Sinon, construit un `LotRequest` de rejeu à partir du lot parent du premier virement éligible.

#### Construction du lot de rejeu

La méthode privée `buildReplayLot` facttorise la construction d'un `LotRequest` de rejeu : même compte source que le lot original, référence suffixée, description préfixée. Chaque `VirementRequest` reproduit le compte destinataire, le montant et la description (préfixée) du virement d'origine.

Le `LotRequest` retourné peut être immédiatement soumis à `submitLot` pour déclencher un nouveau cycle de traitement asynchrone.

#### Endpoints

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/lots/{id}/replay` | Construit un lot de rejeu pour les virements différés du lot |
| POST | `/api/lots/replay` | Construit un lot de rejeu pour une liste d'identifiants de virements |

### Décisions d'architecture

- Le rejeu produit un **nouveau** `LotRequest` plutôt que de modifier les entités existantes, ce qui préserve l'historique complet et l'auditabilité.
- Le filtrage des virements `success` évite de doubler des transactions déjà exécutées avec succès.

---

## 8. M1.8 — Sécurisation JWT

### Objectif

Protéger tous les endpoints de l'API par une authentification basée sur des tokens JWT (JSON Web Token), avec contrôle d'accès différencié selon les rôles.

### Ce qui a été réalisé

#### `JwtService`

La classe `cgb.transfer.security.service.JwtService` (annotée `@Service`) gère le cycle de vie des tokens JWT via la bibliothèque `io.jsonwebtoken:jjwt` (version 0.12.6) :

- **`generateToken(UserCGB user)`** : crée un JWT signé avec HMAC-SHA256 incluant comme claims le `subject` (nom d'utilisateur), le `role` (nom du rôle), le `customerId` (identifiant du client rattaché) et une date d'expiration configurable via la propriété `jwt.expiration` (défaut : 86 400 000 ms = 24 heures). La clé secrète est lue depuis `jwt.secret` dans `application.properties`.
- **`validateToken(String token)`** : parse et vérifie la signature du token ; retourne `false` en cas de toute exception (token malformé, expiré, signature invalide).
- **`extractUsername`**, **`extractRole`**, **`extractCustomerId`** : méthodes d'extraction de claims individuels.

#### `JwtAuthenticationFilter`

La classe `cgb.transfer.security.JwtAuthenticationFilter` étend `OncePerRequestFilter`. À chaque requête, elle :

1. Extrait l'en-tête `Authorization`
2. Vérifie qu'il commence par `Bearer `
3. Valide le token via `JwtService`
4. Crée un `UsernamePasswordAuthenticationToken` avec l'autorité `ROLE_<role>` extraite du token
5. Injecte cette authentification dans le `SecurityContextHolder`

Ce filtre est enregistré **avant** `UsernamePasswordAuthenticationFilter` dans la chaîne Spring Security.

#### `CustomUserDetailsService`

Implémente `UserDetailsService` pour le chargement des utilisateurs depuis `UserCGBRepository`. Construit un `UserDetails` Spring Security avec l'autorité `ROLE_<role.name>`.

#### `SecurityConfig`

La configuration Spring Security (annotée `@Configuration`) est entièrement sans état (`SessionCreationPolicy.STATELESS`) et définit les règles d'accès suivantes :

| Règle | Accès |
|---|---|
| `POST /api/auth/login` | Public (sans authentification) |
| `GET /console/**` | Public (console H2) |
| `GET /api/**` | Rôles `COMPTABLE` ou `UTILISATEUR` |
| `POST /api/**` | Rôle `COMPTABLE` uniquement |
| `PUT /api/**` | Rôle `COMPTABLE` uniquement |
| `DELETE /api/**` | Rôle `COMPTABLE` uniquement |
| Toute autre requête | Authentifiée |

Le CSRF est désactivé (API REST stateless), et les options de framing H2 sont configurées avec `sameOrigin`.

Le `PasswordEncoder` utilisé est **BCrypt** (`BCryptPasswordEncoder`), configuré comme bean Spring.

#### `AuthController`

Mappé sur `/api/auth`, il expose :

- **`POST /api/auth/login`** : authentifie l'utilisateur par `username`/`password`, vérifie le hash BCrypt, retourne un JWT avec le nom d'utilisateur et le rôle
- **`POST /api/auth/register`** : crée un nouvel utilisateur (hashage BCrypt du mot de passe, vérification d'unicité du `username`)
- **`PUT /api/auth/users/{id}`** : mise à jour partielle d'un utilisateur (chaque champ fourni remplace l'existant)
- **`DELETE /api/auth/users/{id}`** : suppression d'un utilisateur

Les messages d'erreur retournés (`Identifiants invalides`, `Utilisateur deja existant`) sont volontairement génériques pour ne pas révéler d'informations à un attaquant.

### Décisions d'architecture

- L'extraction du rôle directement depuis le token JWT (et non une requête base à chaque appel) réduit la charge sur la base de données.
- La propagation du `customerId` dans le token permet à des services avals de connaître le contexte client sans requête supplémentaire.
- L'utilisation de `OncePerRequestFilter` garantit que le filtre n'est exécuté qu'une seule fois par requête, même dans les chaînes de dispatch complexes.

---

## 9. Tableau récapitulatif des endpoints

### Authentification (`/api/auth`)

| Méthode | URL | Description | Authentification requise |
|---|---|---|---|
| POST | `/api/auth/login` | Authentification, retourne un JWT | Non |
| POST | `/api/auth/register` | Création d'un utilisateur | COMPTABLE |
| PUT | `/api/auth/users/{id}` | Mise à jour d'un utilisateur | COMPTABLE |
| DELETE | `/api/auth/users/{id}` | Suppression d'un utilisateur | COMPTABLE |

### Virements unitaires (`/api/transfers`)

| Méthode | URL | Description | Authentification requise |
|---|---|---|---|
| POST | `/api/transfers` | Créer un virement unitaire | COMPTABLE |
| DELETE | `/api/transfers` | Supprimer un virement par ID (corps JSON) | COMPTABLE |

### Lots de virements (`/api/lots`)

| Méthode | URL | Description | Authentification requise |
|---|---|---|---|
| POST | `/api/lots` | Soumettre un lot de virements | COMPTABLE |
| GET | `/api/lots/{id}` | Consulter un lot par ID | COMPTABLE ou UTILISATEUR |
| GET | `/api/lots/{id}/report` | Rapport détaillé d'un lot | COMPTABLE ou UTILISATEUR |
| GET | `/api/lots/{id}/failures` | Virements en échec d'un lot | COMPTABLE ou UTILISATEUR |
| GET | `/api/lots/failures?from=&to=` | Virements en échec par plage de dates | COMPTABLE ou UTILISATEUR |
| GET | `/api/lots/failures/account/{destAccount}` | Virements en échec par compte destinataire | COMPTABLE ou UTILISATEUR |
| GET | `/api/lots/{id}/replay` | Construire un lot de rejeu (virements différés) | COMPTABLE ou UTILISATEUR |
| POST | `/api/lots/replay` | Construire un lot de rejeu par IDs de virements | COMPTABLE |

### Accès technique

| Méthode | URL | Description | Authentification requise |
|---|---|---|---|
| GET | `/console/**` | Console H2 (développement) | Non |
| GET | `/test/{id}` | Endpoint de test basique | Non |

---

## 10. Vue d'ensemble des entités

### Diagramme de relations simplifié

```
Role (1) <---[ManyToOne]--- UserCGB ---[ManyToOne]---> Customer
                                                         |
                                          [ManyToMany] myaccounts
                                          [ManyToMany] recipientAccounts
                                                         |
                                                      Account

Lot (1) ---[OneToMany, CASCADE ALL]--> TransferLot
```

### Entité `Account`

| Champ | Type | Description |
|---|---|---|
| `accountNumber` | `String` (@Id) | Numéro IBAN du compte |
| `solde` | `Double` | Solde courant |

### Entité `Transfer`

| Champ | Type | Description |
|---|---|---|
| `id` | `Long` (@Id, auto-généré) | Identifiant technique |
| `sourceAccountNumber` | `String` | IBAN du compte débiteur |
| `destinationAccountNumber` | `String` | IBAN du compte créditeur |
| `amount` | `Double` | Montant du virement |
| `transferDate` | `LocalDate` | Date d'exécution |
| `description` | `String` | Libellé du virement |

### Entité `Role`

| Champ | Type | Description |
|---|---|---|
| `id` | `Long` (@Id, auto-généré) | Identifiant technique |
| `name` | `String` | Nom du rôle (`COMPTABLE`, `UTILISATEUR`) |

### Entité `UserCGB`

| Champ | Type | Description |
|---|---|---|
| `id` | `Long` (@Id, auto-généré) | Identifiant technique |
| `username` | `String` | Identifiant de connexion |
| `password` | `String` | Mot de passe hashé BCrypt |
| `email` | `String` | Adresse email |
| `role` | `Role` (@ManyToOne) | Rôle applicatif |
| `belongTo` | `Customer` (@ManyToOne) | Client bancaire rattaché |

### Entité `Customer`

| Champ | Type | Description |
|---|---|---|
| `id` | `Long` (@Id, auto-généré) | Identifiant technique |
| `name` | `String` | Raison sociale |
| `address` | `String` | Adresse postale |
| `lei` | `String` (max 20) | Identifiant légal (Legal Entity Identifier) |
| `myaccounts` | `List<Account>` (@ManyToMany) | Comptes courants du client |
| `recipientAccounts` | `List<Account>` (@ManyToMany) | Bénéficiaires autorisés |

### Entité `Lot`

| Champ | Type | Description |
|---|---|---|
| `id` | `Long` (@Id, auto-généré) | Identifiant technique |
| `refLot` | `String` | Référence métier du lot |
| `sourceAccount` | `String` | IBAN du compte débiteur commun |
| `descriptionLot` | `String` | Libellé du lot |
| `dateLot` | `LocalDate` | Date de soumission |
| `state` | `String` | État du lot |
| `virements` | `List<TransferLot>` (@OneToMany) | Virements du lot |

### Entité `TransferLot`

| Champ | Type | Description |
|---|---|---|
| `id` | `Long` (@Id, auto-généré) | Identifiant technique |
| `destAccount` | `String` | IBAN du compte bénéficiaire |
| `amount` | `Double` | Montant du virement |
| `description` | `String` | Libellé |
| `completionDate` | `LocalDate` | Date d'exécution ou d'échec |
| `state` | `String` | État du virement |
| `lot` | `Lot` (@ManyToOne, @JsonIgnore) | Lot parent |

---

## 11. Tests

### Vue d'ensemble

La suite de tests comprend **55 tests** répartis en 10 classes, couvrant les couches service, contrôleur et utilitaires. Les frameworks utilisés sont JUnit 5, Mockito (avec l'extension `MockitoExtension`) et MockMvc (Spring Boot Test).

| Classe de test | Type | Nb tests | Ce qui est testé |
|---|---|---|---|
| `CGBIbanValidatorTest` | Unitaire (JUnit pur) | 13 | Singleton, validation structure, CRC, méthodes d'extraction, null-safety |
| `TransferServiceTest` | Unitaire (Mockito) | 8 | Virement réussi, montant négatif/nul, antidatage, compte introuvable, solde insuffisant, date nulle |
| `CustomerAccountValidationTest` | Unitaire (Mockito) | 4 | Autorisation compte source/destinataire, sans vérification client |
| `LotServiceTest` | Unitaire (Mockito) | 4 | Soumission de lot, succès, différé (solde insuffisant), échec (compte inexistant) |
| `ReplayTest` | Unitaire (Mockito) | 4 | Rejeu depuis différés, rejeu sans différé, rejeu par IDs, filtrage des succès |
| `ReportServiceTest` | Unitaire (Mockito) | 5 | Rapport de lot, lot introuvable, virements en échec, plage de dates, envoi email |
| `TransferControllerTest` | Intégration (MockMvc + SpringBootTest) | 5 | Virement réussi, solde insuffisant, montant négatif, antidatage, compte inexistant |
| `LotControllerTest` | Intégration (MockMvc + SpringBootTest) | 2 | Soumission de lot, lot introuvable |
| `AuthControllerTest` | Intégration (MockMvc + SpringBootTest) | 5 | Login succès/échec, accès sans token, accès avec token valide, rôle UTILISATEUR refusé en POST |
| `JwtServiceTest` | Intégration (SpringBootTest) | 5 | Génération + validation token, extraction username/rôle/customerId, token invalide |

### Stratégie de test

**Tests unitaires (Mockito)** : les dépendances des services sont mockées (`@Mock`, `@InjectMocks`). Cette approche isole la logique métier des accès base de données et permet de tester les cas limites sans contexte applicatif complet. Les mocks sont configurés avec `when(...).thenReturn(...)` ou `thenAnswer(...)` pour simuler les réponses des repositories.

**Tests d'intégration (SpringBootTest + MockMvc)** : le contexte Spring complet est chargé avec la base H2 peuplée par `DatabaseInitializer`. Les requêtes HTTP sont simulées via MockMvc. Pour les endpoints sécurisés, un token JWT est obtenu via une requête de login (`padelphi` / `password123`) dans la méthode `@BeforeEach`, ce qui valide simultanément le flux d'authentification.

### Points de couverture notables

- Le singleton `CGBIbanValidator` est testé pour garantir que deux appels à `getInstanceValidator()` retournent la même instance (`assertSame`).
- Les tests de `TransferService` vérifient que les débits/crédits sont correctement appliqués sur les objets `Account` mockés.
- `AuthControllerTest.testReadOnlyRoleCannotPost` vérifie explicitement que l'utilisateur `patchaude` (rôle `UTILISATEUR`) reçoit un `403 Forbidden` lors d'une tentative de création de virement.
- `ReportServiceTest.testSendLotCompletionEmail` vérifie que `JavaMailSender.send()` est appelé exactement une fois avec un `SimpleMailMessage`.

---

## 12. Conclusion

### Bilan des missions

L'API CGB implémente l'ensemble des huit missions demandées :

| Mission | Fonctionnalité | Statut |
|---|---|---|
| M1.2 | Validation IBAN (singleton, commons-validator, générateur) | Implémenté |
| M1.3 | Validations métier et codes HTTP corrects | Implémenté |
| M1.6 | Gestion des clients, comptes courants et bénéficiaires | Implémenté |
| M1.4 | Traitement asynchrone des lots de virements | Implémenté |
| M1.5 | Rapports de lots et notifications email | Implémenté |
| M1.7 | Rejeu des transactions différées ou en échec | Implémenté |
| M1.8 | Sécurisation JWT avec contrôle d'accès par rôle | Implémenté |

### Points forts

- **Architecture en couches** claire (contrôleur → service → repository → entité), respectant les principes de responsabilité unique.
- **Hiérarchies d'exceptions** bien structurées permettant un traitement différencié des erreurs métier.
- **Traitement asynchrone** des lots permettant de répondre immédiatement au client sans blocage.
- **Sécurité sans état** adaptée à une API REST, avec propagation du contexte client dans le token JWT.
- **Couverture de tests** complète (55 tests) mêlant tests unitaires isolés et tests d'intégration de bout en bout.

### Points d'amélioration possibles

- La validation IBAN (`CGBIbanValidator`) n'est pas encore intégrée dans les flux de virement et de soumission de lot : les numéros de compte passés dans les requêtes ne sont pas validés structurellement avant accès en base.
- Le filtre `JwtAuthenticationFilter` crée l'authentification directement depuis les claims du token sans recharger l'utilisateur depuis la base, ce qui ne détecte pas une révocation de compte en cours de session.
- La gestion de la concurrence dans `processVirement` (lecture et mise à jour du solde source sans verrou optimiste) pourrait poser des problèmes si plusieurs lots partagent le même compte source et sont traités simultanément.
- L'endpoint de soumission d'un lot (`POST /api/lots`) ne valide pas l'IBAN du `sourceAccount` fourni avant de lancer le traitement asynchrone.
- Les tests d'intégration partagent la même instance de base H2 entre les classes de test, ce qui peut créer des dépendances d'ordre d'exécution sur les soldes de comptes.
