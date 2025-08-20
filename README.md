# Aperçu du projet

Ceci est une application de messagerie instantanée basée sur une architecture **microservices** avec séparation front-end / back-end :

- **Back-end** : Spring Boot 3.3.0 + Java 17, fournissant des API REST et le support WebSocket  
- **Front-end** : Angular 19, fournissant l’interface utilisateur  
- **Stockage de données** : PostgreSQL (utilisateurs et salons de discussion) + MongoDB (historique des messages)  
- **Middleware** : Redis (cache et gestion de session distribuée) + RabbitMQ (file de messages)  
- **Déploiement** : Docker Compose + GitHub Actions CI/CD  

---

## Commandes de développement

### Back-end (Spring Boot)
```bash
# Démarrer en mode développement
cd ChatApp_BackEnd
./mvnw spring-boot:run

# Construire le projet
./mvnw clean package

# Lancer les tests
./mvnw test

# Construire l’image Docker
docker build -t chatapp-backend .
```

### Front-end (Angular)
```bash
# Installer les dépendances
cd ChatApp_FrontEnd
npm install

# Serveur de développement
npm run start
# ou
ng serve

# Construire en mode production
npm run build
# ou
ng build --configuration production

# Tests unitaires
npm run test
# ou
ng test

# Tests E2E (Cypress)
npm run cypress:open  # mode interactif
npm run cypress:run   # mode en ligne de commande
```

### Déploiement conteneurisé
```bash
# Déploiement complet en local
docker-compose up -d

# Vérifier l’état des services
docker-compose ps

# Consulter les logs
docker-compose logs -f [service-name]

# Reconstruire un service spécifique
docker-compose up -d --build [service-name]
```

---

## Points clés de l’architecture

### Flux métier principal
- **Authentification** : connexion par identifiant/mot de passe ou par code e-mail, gestion via JWT  
- **Gestion des salons** : création, adhésion, modification des salons, invitations d’utilisateurs  
- **Communication en temps réel** : connexion WebSocket, diffusion des messages en déploiement distribué  
- **Persistance des messages** : PostgreSQL pour les utilisateurs et salons, MongoDB pour l’historique des messages  

### Support distribué
- Redis pub/sub pour la gestion distribuée des sessions WebSocket  
- RabbitMQ pour le traitement d’événements asynchrones (adhésion, changements de membres, etc.)  
- Spring Security Remember-Me pour la gestion persistante des sessions  

### Configurations clés
- **Port back-end** : 53050  
- **Port front-end** : 4200 (développement), 80/443 (production)  
- **Bases de données** : PostgreSQL (chatAppDB), MongoDB (ChatApp)  
- **Cache** : Redis pour les codes de vérification et la liste noire (expiration 5 min)  

### Structure du projet
```
ChatApp_BackEnd/src/main/java/com/devStudy/chat/
├── config/          # Configurations Spring (Security, WebSocket, Redis, RabbitMQ)
├── controller/      # Contrôleurs REST API
├── dao/             # Interfaces JPA Repository
├── dto/             # Objets de transfert de données et mappers
├── model/           # Entités JPA
├── security/        # Logique d’authentification et d’autorisation
├── service/         # Services métier
├── websocket/       # Gestion WebSocket et support distribué
└── utils/           # Utilitaires et exceptions

ChatApp_FrontEnd/src/app/
├── ChatAppComponents/     # Composants principaux des pages
├── CommonComponents/      # Composants UI communs
├── LoginComponents/       # Composants liés à la connexion
├── Models/                # Modèles TypeScript
├── Services/              # Services Angular
└── RouteGuards/           # Gardes de routes
```

---

## Variables d’environnement

Variables essentielles (définies dans `docker-compose.yml`) :  
- `DB_HOST`, `DB_USERNAME`, `DB_PASSWORD` : connexion PostgreSQL  
- `MONGO_HOST` : connexion MongoDB  
- `REDIS_HOST` : connexion Redis  
- `RABBITMQ_HOST`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` : connexion RabbitMQ  
- `FRONT_URL` : adresse du front-end (pour configuration CORS)  

---

## Déploiement CI/CD

Déploiement automatisé sur **EC2** avec GitHub Actions :  
1. Construire et publier l’image Docker sur Docker Hub  
2. Connexion SSH à l’instance EC2  
3. Exécution du script `deploy.sh` pour mettre à jour les services  
4. Vérification de l’état du déploiement via un **health-check**  

---
