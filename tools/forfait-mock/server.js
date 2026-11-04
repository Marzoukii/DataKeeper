// Serveur mock de l'API forfait pour DataKeeper — zéro dépendance (module http natif de Node).
//
// Démarrer :   node tools/forfait-mock/server.js        (écoute sur le port 8080)
// Tester  :   curl "http://localhost:8080/forfait?operator=60503"
//
// Réponse JSON attendue par l'app (voir ForfaitApiRepositoryImpl) :
//   { "limitGb": 25, "remainingGb": 19, "validityDays": 30, "priceDt": 28.5 }
// Les champs facultatifs peuvent être null. Opérateur inconnu => HTTP 404.

const http = require('http');
const { URL } = require('url');

const PORT = process.env.PORT || 8080;

// Forfaits de démonstration par code MCC/MNC d'opérateur tunisien (MCC 605).
const FORFAITS = {
  '60501': { limitGb: 20, remainingGb: 12.5, validityDays: 30, priceDt: 25 },   // Orange
  '60502': { limitGb: 25, remainingGb: 19, validityDays: 30, priceDt: 28.5 },   // Tunisie Telecom
  '60503': { limitGb: 25, remainingGb: 19, validityDays: 30, priceDt: 28.5 },   // Ooredoo
};

const server = http.createServer((req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`);
  if (url.pathname !== '/forfait') {
    res.writeHead(404).end();
    return;
  }
  const operator = url.searchParams.get('operator');
  const forfait = FORFAITS[operator];
  if (!forfait) {
    // Opérateur non couvert : l'app retombe proprement sur la saisie manuelle.
    res.writeHead(404, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ error: 'operator_not_supported', operator }));
    return;
  }
  res.writeHead(200, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify(forfait));
});

server.listen(PORT, () => {
  console.log(`API forfait mock sur http://localhost:${PORT}/forfait`);
  console.log(`Émulateur Android : http://10.0.2.2:${PORT}/forfait`);
});
