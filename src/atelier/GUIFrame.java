package atelier;

// Importation des classes nécessaires
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// Définition de la classe GUIFrame qui étend JFrame pour l'IHM
public class GUIFrame extends JFrame {

    // Liste des références des Robots
    List<RobotReference> Robots;

    // Panneaux pour chaque Robot et leurs étiquettes
    HashMap<RobotReference, JPanel> RobotPanels = new HashMap<>();
    HashMap<JPanel, List<JLabel>> labels = new HashMap<>();

    // Panneau pour les produits finis et leurs quantités commandées/produites
    JPanel finishedProducts;
    HashMap<String, Integer> ordered = new HashMap<>();
    HashMap<String, Integer> produced = new HashMap<>();
    HashMap<String, JLabel> orderedLabel = new HashMap<>();
    HashMap<String, JLabel> producedLabel = new HashMap<>();

    // Instance de InformationCenter pour obtenir des informations
    InformationCenter ic = InformationCenter.getInstance();

    // Constructeur prenant une liste de références de Robots
    public GUIFrame(List<RobotReference> Robots) {
        super("atelier 4.0");
        this.Robots = Robots;

        // Configuration de la fenêtre principale
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocation(50, 50);
        JPanel mainPanel = new JPanel();
        mainPanel.setSize(800, 400);

        // Création des panneaux pour chaque Robot et ajout des étiquettes
        for (int i = 0; i < Robots.size(); i++) {
            JPanel Robot = new RobotGUI(); // Un panneau spécifique à chaque Robot
            JLabel label = new JLabel("  " + Robots.get(i).name + "  ", SwingConstants.CENTER); // Étiquette pour le nom de la Robot
            Robot.add(label); // Ajout de l'étiquette au panneau de la Robot
            RobotPanels.put(Robots.get(i), Robot); // Stockage du panneau de la Robot
            labels.put(Robot, new ArrayList<>()); // Initialisation d'une liste d'étiquettes pour ce panneau
            mainPanel.add(Robot); // Ajout du panneau principal
        }

        // Configuration du panneau pour les produits finis
        finishedProducts = new JPanel();
        finishedProducts.setSize(800, 200);
        finishedProducts.add(new JLabel("Finished products: ", SwingConstants.CENTER));
        finishedProducts.setLocation(0, 400);

        // Ajout des panneaux à la fenêtre principale
        add(finishedProducts);
        add(mainPanel);

        setVisible(true);
    }

    // Méthode pour ajouter un produit à une Robot spécifique
    public void addProduct(String Robot, String product) {
        RobotReference RobotRef = Robots.stream().filter(m -> m.name.equals(Robot)).findFirst().get(); // Obtention de la référence de la Robot
        JPanel panel = RobotPanels.get(RobotRef); // Obtention du panneau correspondant à la Robot

        JLabel label = new JLabel(product, SwingConstants.CENTER); // Création de l'étiquette pour le produit
        panel.add(label); // Ajout de l'étiquette au panneau de la Robot
        panel.revalidate(); // Mise à jour de l'interface pour refléter les modifications
        panel.repaint();

        labels.get(panel).add(label); // Stockage de l'étiquette pour cette Robot
    }

    // Méthode pour retirer un produit d'une Robot spécifique
    public void removeProduct(String Robot, String product) {
        RobotReference RobotRef = Robots.stream().filter(m -> m.name.equals(Robot)).findFirst().get(); // Obtention de la référence de la Robot
        JPanel panel = RobotPanels.get(RobotRef); // Obtention du panneau correspondant à la Robot

        JLabel label = labels.get(panel).stream().filter(l -> l.getText().equals(product)).findFirst().get(); // Recherche de l'étiquette du produit à retirer
        panel.remove(label); // Retrait de l'étiquette du panneau
        labels.get(panel).remove(label); // Retrait de l'étiquette de la liste
        panel.revalidate(); // Mise à jour de l'interface pour refléter les modifications
        panel.repaint();
    }
}
