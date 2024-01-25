# Atelier Simulation

## Description

Ce projet met en œuvre une simulation d'atelier utilisant la plateforme JADE (Java Agent DEvelopment Framework). L'atelier comprend des robots, des produits, et des agents de simulation pour coordonner la production.

## Structure du Projet

Le projet est organisé comme suit:

- **atelier:** Contient les classes principales de l'atelier.

- **assets:** Stocke les ressources telles que les images utilisées par l'interface graphique.

## Dépendances

- **JADE (Java Agent DEvelopment Framework):** La plateforme de développement d'agents utilisée pour modéliser le comportement des robots et des agents de simulation. Téléchargez-le depuis [le site officiel de JADE](http://jade.tilab.com/).

- **Gson:** Une bibliothèque pour la sérialisation et la désérialisation d'objets Java au format JSON. Ajoutez-le à votre projet en utilisant Maven ou téléchargez-le depuis [le dépôt Gson sur GitHub](https://github.com/google/gson).

## Configuration

1. **Configuration de l'Environnement:**
   - Assurez-vous d'avoir JADE installé et configuré dans votre environnement de développement.

2. **Dépendances:**
   - Ajoutez les dépendances nécessaires, notamment JADE et Gson, à votre projet.

3. **Exécution:**
   - Exécutez le programme en lançant la classe principale `MainController` depuis votre IDE.

## Fonctionnalités

- **Simulation:** Les agents de simulation coordonnent la production en générant des demandes de produits et en planifiant les actions des robots.

- **Interface Graphique:** L'interface graphique (`RobotGUI`) affiche l'état actuel de la simulation, y compris les robots, les produits et les produits finis.

- **Communication entre Agents:** Les agents utilisent la messagerie JADE pour communiquer entre eux et coordonner les actions.

- **Génération de Rapports:** L'atelier génère des rapports détaillant la production, les performances des robots, et d'autres métriques importantes.

## Utilisation

1. **Lancement:**
   - Exécutez l'application depuis votre IDE ou utilisez les commandes de ligne de commande appropriées.

2. **Observation:**
   - Surveillez la sortie de la console pour des mises à jour sur l'état de la simulation.

3. **Interface Graphique:**
   - Ouvrez l'interface graphique pour une représentation visuelle de la simulation.

## Auteur

- Channel NIANGA
