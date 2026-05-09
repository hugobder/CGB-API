# Guide des Endpoints - API CGB (Credit General GSB)

API REST Spring Boot pour la gestion des virements bancaires.  
**Base URL :** `http://localhost:8080`  
**Format :** JSON (UTF-8)  
**Authentification :** JWT Bearer Token (sauf `/api/auth/login`)

---

## Sommaire

1. [Authentification](#1-authentification)
   - [POST /api/auth/login](#11-post-apiauthlogin)
   - [POST /api/auth/register](#12-post-apiauthregister)
   - [PUT /api/auth/users/{id}](#13-put-apiauthusersid)
   - [DELETE /api/auth/users/{id}](#14-delete-apiauthusersid)
2. [Virements](#2-virements)
   - [POST /api/transfers](#21-post-apitransfers)
   - [DELETE /api/transfers](#22-delete-apitransfers)
3. [Lots](#3-lots)
   - [POST /api/lots](#31-post-apilots)
   - [GET /api/lots/{id}](#32-get-apilotsid)
   - [GET /api/lots/{id}/report](#33-get-apilotsidreport)
   - [GET /api/lots/{id}/failures](#34-get-apilotsidfailures)
   - [GET /api/lots/failures](#35-get-apilotsfailures)
   - [GET /api/lots/failures/account/{destAccount}](#36-get-apilotsfailuresaccountdestaccount)
   - [GET /api/lots/{id}/replay](#37-get-apilotsidreplay)
   - [POST /api/lots/replay](#38-post-apilotsreplay)

---

## Regles d'autorisation generales

| Methode HTTP | Role minimum requis       |
|--------------|---------------------------|
| POST         | COMPTABLE                 |
| PUT          | COMPTABLE                 |
| DELETE       | COMPTABLE                 |
| GET          | COMPTABLE ou UTILISATEUR  |
| /api/auth/login | Public (aucun token) |

---

## 1. Authentification

### 1.1 POST /api/auth/login

**Description :** Authentifie un utilisateur et retourne un token JWT a inclure dans les appels suivants.

**Authentification requise :** Aucune (endpoint public)

**Headers :**
```
Content-Type: application/json
```

**Corps de la requete :**
```json
{
  "username": "jdupont",
  "password": "motDePasse123"
}
```

| Champ      | Type   | Obligatoire | Description             |
|------------|--------|-------------|-------------------------|
| `username` | string | Oui         | Nom d'utilisateur       |
| `password` | string | Oui         | Mot de passe en clair   |

**Reponse en cas de succes — 200 OK :**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "username": "jdupont",
  "role": "COMPTABLE"
}
```

| Champ      | Type   | Description                              |
|------------|--------|------------------------------------------|
| `token`    | string | Token JWT a utiliser dans Authorization  |
| `username` | string | Nom d'utilisateur authentifie            |
| `role`     | string | Role de l'utilisateur (`COMPTABLE` ou `UTILISATEUR`) |

**Reponse en cas d'erreur — 401 Unauthorized :**
```json
{
  "error": "Identifiants invalides"
}
```

**Codes HTTP retournes :**

| Code | Description                                 |
|------|---------------------------------------------|
| 200  | Connexion reussie, token JWT retourne        |
| 401  | Identifiants incorrects ou utilisateur inconnu |

---

### 1.2 POST /api/auth/register

**Description :** Cree un nouvel utilisateur dans le systeme. La route est reservee aux comptables.

**Authentification requise :** Role `COMPTABLE`

**Headers :**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Corps de la requete :**
```json
{
  "username": "mmartin",
  "password": "motDePasse456",
  "email": "m.martin@cgb.fr",
  "role": {
    "id": 2,
    "name": "UTILISATEUR"
  },
  "belongTo": {
    "id": 5
  }
}
```

| Champ      | Type   | Obligatoire | Description                                        |
|------------|--------|-------------|----------------------------------------------------|
| `username` | string | Oui         | Nom d'utilisateur unique                           |
| `password` | string | Oui         | Mot de passe en clair (sera hache en BCrypt)       |
| `email`    | string | Non         | Adresse email                                      |
| `role`     | objet  | Non         | Objet Role avec `id` et/ou `name`                  |
| `belongTo` | objet  | Non         | Objet Customer auquel l'utilisateur est rattache   |

**Reponse en cas de succes — 201 Created :**
```json
{
  "message": "Utilisateur cree"
}
```

**Reponse en cas d'erreur — 409 Conflict :**
```json
{
  "error": "Utilisateur deja existant"
}
```

**Codes HTTP retournes :**

| Code | Description                                   |
|------|-----------------------------------------------|
| 201  | Utilisateur cree avec succes                  |
| 401  | Token absent ou invalide                      |
| 403  | Role insuffisant (UTILISATEUR tente de creer) |
| 409  | Nom d'utilisateur deja utilise                |

---

### 1.3 PUT /api/auth/users/{id}

**Description :** Modifie les informations d'un utilisateur existant (mise a jour partielle : seuls les champs fournis sont modifies).

**Authentification requise :** Role `COMPTABLE`

**Headers :**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Parametre de chemin :**

| Parametre | Type | Description                     |
|-----------|------|---------------------------------|
| `id`      | Long | Identifiant numerique de l'utilisateur |

**Corps de la requete (tous les champs sont optionnels) :**
```json
{
  "username": "nouveau_nom",
  "email": "nouveau@cgb.fr",
  "password": "nouveauMotDePasse",
  "role": {
    "id": 1,
    "name": "COMPTABLE"
  },
  "belongTo": {
    "id": 3
  }
}
```

**Reponse en cas de succes — 200 OK :**
```json
{
  "message": "Utilisateur mis a jour"
}
```

**Reponse en cas d'erreur — 404 Not Found :**
```json
{
  "error": "Utilisateur introuvable"
}
```

**Codes HTTP retournes :**

| Code | Description                        |
|------|------------------------------------|
| 200  | Utilisateur mis a jour             |
| 401  | Token absent ou invalide           |
| 403  | Role insuffisant                   |
| 404  | Aucun utilisateur avec cet identifiant |

---

### 1.4 DELETE /api/auth/users/{id}

**Description :** Supprime definitivement un utilisateur du systeme.

**Authentification requise :** Role `COMPTABLE`

**Headers :**
```
Authorization: Bearer <token>
```

**Parametre de chemin :**

| Parametre | Type | Description                            |
|-----------|------|----------------------------------------|
| `id`      | Long | Identifiant numerique de l'utilisateur |

**Corps de la requete :** Aucun

**Reponse en cas de succes — 200 OK :**
```json
{
  "message": "Utilisateur supprime"
}
```

**Reponse en cas d'erreur — 404 Not Found :**
```json
{
  "error": "Utilisateur introuvable"
}
```

**Codes HTTP retournes :**

| Code | Description                            |
|------|----------------------------------------|
| 200  | Utilisateur supprime                   |
| 401  | Token absent ou invalide               |
| 403  | Role insuffisant                       |
| 404  | Aucun utilisateur avec cet identifiant |

---

## 2. Virements

### 2.1 POST /api/transfers

**Description :** Cree et execute un virement unitaire entre deux comptes. Le compte source doit etre rattache au client de l'utilisateur connecte.

**Authentification requise :** Role `COMPTABLE`

**Headers :**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Corps de la requete :**
```json
{
  "sourceAccountNumber": "FR7630006000011234567890189",
  "destinationAccountNumber": "FR7614508059203002130704982",
  "amount": 1500.00,
  "transferDate": "2026-04-17",
  "description": "Paiement facture 2026-042"
}
```

| Champ                      | Type   | Obligatoire | Description                                              |
|----------------------------|--------|-------------|----------------------------------------------------------|
| `sourceAccountNumber`      | string | Oui         | IBAN du compte debiteur                                  |
| `destinationAccountNumber` | string | Oui         | IBAN du compte crediteur                                 |
| `amount`                   | double | Oui         | Montant du virement (doit etre positif)                  |
| `transferDate`             | string | Oui         | Date d'execution au format `YYYY-MM-DD` (ne peut pas etre anterieure a aujourd'hui) |
| `description`              | string | Non         | Libelle du virement                                      |

**Reponse en cas de succes — 200 OK :**
```json
{
  "id": 42,
  "sourceAccountNumber": "FR7630006000011234567890189",
  "destinationAccountNumber": "FR7614508059203002130704982",
  "amount": 1500.00,
  "transferDate": "2026-04-17",
  "description": "Paiement facture 2026-042"
}
```

**Reponse en cas d'erreur :**
```json
{
  "status": "FAILURE",
  "message": "Description de l'erreur"
}
```

**Codes HTTP retournes :**

| Code | Cause                                                             |
|------|-------------------------------------------------------------------|
| 200  | Virement cree avec succes                                         |
| 400  | Montant negatif, date antedatee, format IBAN invalide, solde insuffisant |
| 401  | Token absent ou invalide                                          |
| 403  | Le compte source n'appartient pas au client de l'utilisateur     |
| 404  | Compte source ou destinataire introuvable                         |

---

### 2.2 DELETE /api/transfers

**Description :** Supprime un virement existant identifie par son ID. L'ID est passe directement comme corps de la requete (valeur brute Long).

**Authentification requise :** Role `COMPTABLE`

**Headers :**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Corps de la requete :**
```json
42
```

> Note : le corps est un entier Long brut, pas un objet JSON.

**Reponse en cas de succes — 200 OK :**
```json
{
  "status": "SUCCESS",
  "message": "Transfer{id=42, ...}"
}
```

**Reponse en cas d'erreur — 400 Bad Request :**
```json
{
  "status": "FAILURE",
  "message": "Description de l'erreur"
}
```

**Codes HTTP retournes :**

| Code | Description                              |
|------|------------------------------------------|
| 200  | Virement supprime avec succes            |
| 400  | ID introuvable ou suppression impossible |
| 401  | Token absent ou invalide                 |
| 403  | Role insuffisant                         |

---

## 3. Lots

### 3.1 POST /api/lots

**Description :** Soumet un lot de virements. Tous les virements du lot partagent le meme compte source. Chaque virement du lot est traite independamment.

**Authentification requise :** Role `COMPTABLE`

**Headers :**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Corps de la requete :**
```json
{
  "refLot": "LOT-2026-001",
  "sourceAccount": "FR7630006000011234567890189",
  "descriptionLot": "Salaires avril 2026",
  "virements": [
    {
      "destAccount": "FR7614508059203002130704982",
      "amount": 2500.00,
      "description": "Salaire M. Martin"
    },
    {
      "destAccount": "FR7610107001011234567890129",
      "amount": 1800.00,
      "description": "Salaire Mme Durand"
    }
  ]
}
```

| Champ            | Type   | Obligatoire | Description                                 |
|------------------|--------|-------------|---------------------------------------------|
| `refLot`         | string | Non         | Reference interne du lot                    |
| `sourceAccount`  | string | Oui         | IBAN du compte debiteur commun a tous les virements |
| `descriptionLot` | string | Non         | Description globale du lot                  |
| `virements`      | liste  | Oui         | Liste des virements individuels             |
| `virements[].destAccount`  | string | Oui | IBAN du compte crediteur               |
| `virements[].amount`       | double | Oui | Montant du virement                    |
| `virements[].description`  | string | Non | Libelle du virement individuel         |

**Reponse en cas de succes — 200 OK :**
```json
{
  "numLot": 7,
  "dateLancement": "2026-04-17",
  "message": "Lot soumis avec succes",
  "etat": "PROCESSED"
}
```

| Champ           | Type   | Description                                               |
|-----------------|--------|-----------------------------------------------------------|
| `numLot`        | Long   | Identifiant genere pour le lot                            |
| `dateLancement` | string | Date de soumission au format `YYYY-MM-DD`                 |
| `message`       | string | Message de synthese                                       |
| `etat`          | string | Etat global du lot (`PROCESSED`, `PARTIAL`, `FAILED`, etc.) |

**Codes HTTP retournes :**

| Code | Description                    |
|------|--------------------------------|
| 200  | Lot soumis et traite           |
| 401  | Token absent ou invalide       |
| 403  | Role insuffisant               |

---

### 3.2 GET /api/lots/{id}

**Description :** Retourne le detail complet d'un lot, incluant la liste de tous ses virements avec leurs etats respectifs.

**Authentification requise :** Role `COMPTABLE` ou `UTILISATEUR`

**Headers :**
```
Authorization: Bearer <token>
```

**Parametre de chemin :**

| Parametre | Type | Description                    |
|-----------|------|--------------------------------|
| `id`      | Long | Identifiant numerique du lot   |

**Reponse en cas de succes — 200 OK :**
```json
{
  "id": 7,
  "refLot": "LOT-2026-001",
  "sourceAccount": "FR7630006000011234567890189",
  "descriptionLot": "Salaires avril 2026",
  "dateLot": "2026-04-17",
  "state": "PROCESSED",
  "virements": [
    {
      "id": 101,
      "destAccount": "FR7614508059203002130704982",
      "amount": 2500.00,
      "description": "Salaire M. Martin",
      "completionDate": "2026-04-17",
      "state": "success"
    },
    {
      "id": 102,
      "destAccount": "FR7610107001011234567890129",
      "amount": 1800.00,
      "description": "Salaire Mme Durand",
      "completionDate": "2026-04-17",
      "state": "failure"
    }
  ]
}
```

**Etats possibles d'un virement (`state`) :**

| Valeur     | Description                                     |
|------------|-------------------------------------------------|
| `success`  | Virement execute avec succes                    |
| `failure`  | Virement echoue (solde insuffisant, IBAN invalide, etc.) |
| `delayed`  | Virement differe (date d'execution future)      |
| `canceled` | Virement annule                                 |
| `waiting`  | Virement en attente de traitement               |

**Reponse en cas d'erreur — 404 Not Found :**
```
"Lot introuvable: 7"
```

**Codes HTTP retournes :**

| Code | Description                        |
|------|------------------------------------|
| 200  | Lot retourne avec succes           |
| 401  | Token absent ou invalide           |
| 403  | Role insuffisant                   |
| 404  | Aucun lot avec cet identifiant     |

---

### 3.3 GET /api/lots/{id}/report

**Description :** Retourne un rapport statistique d'un lot : nombre de virements par etat (`success`, `failure`, `delayed`, `canceled`, `waiting`) ainsi que la liste detaillee de tous les virements.

**Authentification requise :** Role `COMPTABLE` ou `UTILISATEUR`

**Headers :**
```
Authorization: Bearer <token>
```

**Parametre de chemin :**

| Parametre | Type | Description                  |
|-----------|------|------------------------------|
| `id`      | Long | Identifiant numerique du lot |

**Reponse en cas de succes — 200 OK :**
```json
{
  "lotId": 7,
  "refLot": "LOT-2026-001",
  "dateLot": "2026-04-17",
  "state": "PROCESSED",
  "totalVirements": 2,
  "success": 1,
  "failure": 1,
  "delayed": 0,
  "canceled": 0,
  "waiting": 0,
  "virements": [
    {
      "id": 101,
      "destAccount": "FR7614508059203002130704982",
      "amount": 2500.00,
      "description": "Salaire M. Martin",
      "completionDate": "2026-04-17",
      "state": "success"
    },
    {
      "id": 102,
      "destAccount": "FR7610107001011234567890129",
      "amount": 1800.00,
      "description": "Salaire Mme Durand",
      "completionDate": "2026-04-17",
      "state": "failure"
    }
  ]
}
```

**Reponse en cas d'erreur — 404 Not Found :**
```
"Lot introuvable: 7"
```

**Codes HTTP retournes :**

| Code | Description                     |
|------|---------------------------------|
| 200  | Rapport genere avec succes      |
| 401  | Token absent ou invalide        |
| 403  | Role insuffisant                |
| 404  | Aucun lot avec cet identifiant  |

---

### 3.4 GET /api/lots/{id}/failures

**Description :** Retourne la liste des virements en echec (etats `failure`, `delayed`, `canceled`) pour un lot specifique.

**Authentification requise :** Role `COMPTABLE` ou `UTILISATEUR`

**Headers :**
```
Authorization: Bearer <token>
```

**Parametre de chemin :**

| Parametre | Type | Description                  |
|-----------|------|------------------------------|
| `id`      | Long | Identifiant numerique du lot |

**Reponse en cas de succes — 200 OK :**
```json
[
  {
    "id": 102,
    "destAccount": "FR7610107001011234567890129",
    "amount": 1800.00,
    "description": "Salaire Mme Durand",
    "completionDate": "2026-04-17",
    "state": "failure"
  }
]
```

> Si aucun echec n'existe pour ce lot, la reponse est une liste vide `[]`.

**Codes HTTP retournes :**

| Code | Description                            |
|------|----------------------------------------|
| 200  | Liste retournee (peut etre vide)       |
| 401  | Token absent ou invalide               |
| 403  | Role insuffisant                       |

---

### 3.5 GET /api/lots/failures

**Description :** Retourne tous les virements en echec (etats `failure`, `delayed`, `canceled`) dont la date de completion se situe dans un intervalle de dates donne.

**Authentification requise :** Role `COMPTABLE` ou `UTILISATEUR`

**Headers :**
```
Authorization: Bearer <token>
```

**Parametres de requete :**

| Parametre | Type   | Obligatoire | Format       | Description                             |
|-----------|--------|-------------|--------------|---------------------------------------- |
| `from`    | string | Oui         | `YYYY-MM-DD` | Date de debut de l'intervalle (incluse) |
| `to`      | string | Oui         | `YYYY-MM-DD` | Date de fin de l'intervalle (incluse)   |

**Exemple d'appel :**
```
GET /api/lots/failures?from=2026-04-01&to=2026-04-30
```

**Reponse en cas de succes — 200 OK :**
```json
[
  {
    "id": 55,
    "destAccount": "FR7610107001011234567890129",
    "amount": 950.00,
    "description": "Remboursement trop-percu",
    "completionDate": "2026-04-12",
    "state": "delayed"
  },
  {
    "id": 102,
    "destAccount": "FR7614508059203002130704982",
    "amount": 1800.00,
    "description": "Salaire Mme Durand",
    "completionDate": "2026-04-17",
    "state": "failure"
  }
]
```

**Codes HTTP retournes :**

| Code | Description                            |
|------|----------------------------------------|
| 200  | Liste retournee (peut etre vide)       |
| 400  | Format de date invalide                |
| 401  | Token absent ou invalide               |
| 403  | Role insuffisant                       |

---

### 3.6 GET /api/lots/failures/account/{destAccount}

**Description :** Retourne tous les virements en echec (etats `failure`, `delayed`, `canceled`) a destination d'un compte specifique.

**Authentification requise :** Role `COMPTABLE` ou `UTILISATEUR`

**Headers :**
```
Authorization: Bearer <token>
```

**Parametre de chemin :**

| Parametre     | Type   | Description                                        |
|---------------|--------|----------------------------------------------------|
| `destAccount` | string | IBAN du compte destinataire (encode en URL si besoin) |

**Exemple d'appel :**
```
GET /api/lots/failures/account/FR7614508059203002130704982
```

**Reponse en cas de succes — 200 OK :**
```json
[
  {
    "id": 102,
    "destAccount": "FR7614508059203002130704982",
    "amount": 1800.00,
    "description": "Salaire Mme Durand",
    "completionDate": "2026-04-17",
    "state": "failure"
  }
]
```

**Codes HTTP retournes :**

| Code | Description                            |
|------|----------------------------------------|
| 200  | Liste retournee (peut etre vide)       |
| 401  | Token absent ou invalide               |
| 403  | Role insuffisant                       |

---

### 3.7 GET /api/lots/{id}/replay

**Description :** Genere un objet `LotRequest` pret a soumettre, construit a partir des virements en etat `delayed` du lot specifie. Cet endpoint ne soumet pas de nouveau lot : il retourne uniquement la charge utile a renvoyer via `POST /api/lots`.

**Authentification requise :** Role `COMPTABLE` ou `UTILISATEUR`

**Headers :**
```
Authorization: Bearer <token>
```

**Parametre de chemin :**

| Parametre | Type | Description                   |
|-----------|------|-------------------------------|
| `id`      | Long | Identifiant numerique du lot  |

**Reponse en cas de succes — 200 OK :**
```json
{
  "refLot": "REPLAY-LOT-7",
  "sourceAccount": "FR7630006000011234567890189",
  "descriptionLot": "Rejeu des virements retardes du lot 7",
  "virements": [
    {
      "destAccount": "FR7610107001011234567890129",
      "amount": 1800.00,
      "description": "Salaire Mme Durand"
    }
  ]
}
```

**Reponse en cas d'erreur — 404 Not Found :**
```
"Aucun virement a rejouer pour le lot: 7"
```

> Cette reponse est retournee si le lot n'existe pas ou si aucun de ses virements n'est en etat `delayed`.

**Codes HTTP retournes :**

| Code | Description                                               |
|------|-----------------------------------------------------------|
| 200  | Charge utile de rejeu generee                             |
| 401  | Token absent ou invalide                                  |
| 403  | Role insuffisant                                          |
| 404  | Lot introuvable ou aucun virement en etat `delayed`       |

---

### 3.8 POST /api/lots/replay

**Description :** Genere un objet `LotRequest` pret a soumettre, construit a partir d'une liste d'IDs de virements fournis explicitement. Cet endpoint ne soumet pas de nouveau lot : il retourne la charge utile a renvoyer via `POST /api/lots`.

**Authentification requise :** Role `COMPTABLE`

**Headers :**
```
Content-Type: application/json
Authorization: Bearer <token>
```

**Corps de la requete :**
```json
[101, 102, 115]
```

> Le corps est une liste JSON d'entiers Long correspondant aux IDs des virements (`TransferLot`) a rejouer.

**Reponse en cas de succes — 200 OK :**
```json
{
  "refLot": "REPLAY-IDS",
  "sourceAccount": "FR7630006000011234567890189",
  "descriptionLot": "Rejeu depuis liste d'IDs",
  "virements": [
    {
      "destAccount": "FR7614508059203002130704982",
      "amount": 2500.00,
      "description": "Salaire M. Martin"
    },
    {
      "destAccount": "FR7610107001011234567890129",
      "amount": 1800.00,
      "description": "Salaire Mme Durand"
    }
  ]
}
```

**Reponse en cas d'erreur — 404 Not Found :**
```
"Aucun virement a rejouer"
```

**Codes HTTP retournes :**

| Code | Description                                              |
|------|----------------------------------------------------------|
| 200  | Charge utile de rejeu generee                            |
| 401  | Token absent ou invalide                                 |
| 403  | Role insuffisant                                         |
| 404  | Aucun virement correspondant aux IDs fournis             |

---

## Annexes

### A. Utilisation du token JWT

Apres un appel reussi a `POST /api/auth/login`, inclure le token dans toutes les requetes suivantes :

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### B. Recapitulatif des endpoints

| Methode | Endpoint                                | Role requis               | Description                              |
|---------|-----------------------------------------|---------------------------|------------------------------------------|
| POST    | /api/auth/login                         | Public                    | Connexion, retourne JWT                  |
| POST    | /api/auth/register                      | COMPTABLE                 | Creation d'utilisateur                   |
| PUT     | /api/auth/users/{id}                    | COMPTABLE                 | Modification d'utilisateur               |
| DELETE  | /api/auth/users/{id}                    | COMPTABLE                 | Suppression d'utilisateur                |
| POST    | /api/transfers                          | COMPTABLE                 | Creer un virement unitaire               |
| DELETE  | /api/transfers                          | COMPTABLE                 | Supprimer un virement                    |
| POST    | /api/lots                               | COMPTABLE                 | Soumettre un lot de virements            |
| GET     | /api/lots/{id}                          | COMPTABLE / UTILISATEUR   | Consulter un lot                         |
| GET     | /api/lots/{id}/report                   | COMPTABLE / UTILISATEUR   | Rapport statistique d'un lot             |
| GET     | /api/lots/{id}/failures                 | COMPTABLE / UTILISATEUR   | Virements en echec d'un lot              |
| GET     | /api/lots/failures?from=...&to=...      | COMPTABLE / UTILISATEUR   | Echecs par intervalle de dates           |
| GET     | /api/lots/failures/account/{destAccount}| COMPTABLE / UTILISATEUR   | Echecs par compte destinataire           |
| GET     | /api/lots/{id}/replay                   | COMPTABLE / UTILISATEUR   | Generer rejeu depuis virements retardes  |
| POST    | /api/lots/replay                        | COMPTABLE                 | Generer rejeu depuis liste d'IDs         |

### C. Etats des virements dans un lot

| Etat       | Description                                           |
|------------|-------------------------------------------------------|
| `success`  | Virement execute avec succes                          |
| `failure`  | Virement echoue (ex : solde insuffisant, IBAN invalide) |
| `delayed`  | Virement differe (date d'execution dans le futur)     |
| `canceled` | Virement annule manuellement                          |
| `waiting`  | Virement en attente de traitement                     |

Les etats `failure`, `delayed` et `canceled` sont consideres comme des **echecs** par les endpoints de reporting.
