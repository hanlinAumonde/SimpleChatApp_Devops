## Biblios

**Spring** : git clone https://github.com/hanlinAumonde/SimpleWebApp-Springboot.git
> lancer le projet depuis ChatApplication.java

**Angular** : git clone https://github.com/hanlinAumonde/SimpleChatAppAngular.git

## Architecture

Nous avons un serveur Spring pour traiter les opérations de l'application et accèder à l'interface d'administration. Il offre une API Rest qui permet à Angular d'offir une interface utilisateur interactive aux clients et de les connecter aux chatrooms par websockets. 

Notre architecture s'inspire du modèle MVC : PostgreSQL (Modele) - Angular (Vue) - Spring (Controller) 

#### Organisation du projet : ####

- Package config : Lien entre le back (Spring) et le front (Angular)
- Package controller : Traitement des requetes http
- Package dao : Communication avec la base de données, tri avant envoi de la requete
- Package dto : Modele pour echanger les donnees entre backend et front end
- Package Model : Modele des objets User et Chatrooms, ResetValidate, chatMessage(history)
- Package security : Fonctinnalités de sécurité sur l'authentification 
- Package service : interfaces et leurs implémentations
- Package utils : facilite certaines méthodes
- Package websocket : Etablissement des Websockets
