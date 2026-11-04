# API forfait — serveur mock (démo PFE)

Petit serveur qui imite une API forfait, pour démontrer la **récupération automatique** du forfait
dans DataKeeper sans dépendre du backend d'un opérateur. L'app ne vise jamais l'API privée d'un
opérateur réel : elle parle à **ce** serveur (ou à ton propre backend), que tu héberges toi-même.

## 1. Lancer le serveur

```bash
node tools/forfait-mock/server.js
# API forfait mock sur http://localhost:8080/forfait
```

Test rapide :

```bash
curl "http://localhost:8080/forfait?operator=60503"
# {"limitGb":25,"remainingGb":19,"validityDays":30,"priceDt":28.5}
```

## 2. Pointer l'app dessus

Par défaut, `FORFAIT_API_URL` est **vide** : le bouton « Récupérer automatiquement » renvoie un
forfait de démonstration intégré (aucun serveur requis — pratique pour une soutenance hors-ligne).

Pour viser le serveur mock, règle l'URL dans `app/build.gradle.kts` :

```kotlin
// Émulateur Android : 10.0.2.2 désigne le localhost de la machine hôte.
buildConfigField("String", "FORFAIT_API_URL", "\"http://10.0.2.2:8080/forfait\"")
```

> HTTP en clair : pour un build de dev visant `10.0.2.2`, autorise le trafic non chiffré
> (`android:usesCleartextTraffic="true"` sur une variante debug, ou un `network-security-config`).
> En vrai, héberge le serveur en HTTPS et mets l'URL `https://…`.

## Contrat de l'API

`GET /forfait?operator=<MCC/MNC>` → `200` avec le forfait, ou `404` si l'opérateur n'est pas couvert.

| Champ         | Type   | Oblig. | Exemple |
|---------------|--------|--------|---------|
| `limitGb`     | nombre | oui    | `25`    |
| `remainingGb` | nombre | non    | `19`    |
| `validityDays`| entier | non    | `30`    |
| `priceDt`     | nombre | non    | `28.5`  |

Codes opérateurs tunisiens (MCC 605) : `60501` Orange, `60502` Tunisie Telecom, `60503` Ooredoo.
