package atelier;

import java.util.ArrayList;
import java.util.HashMap;

// Classe InformationCenter gérant les données centrales de l'atelier
public class InformationCenter {

    // Collections pour stocker différentes informations
    public HashMap<Integer, Robot> Robots; // Stockage des Robots par identifiant
    public HashMap<String, Product> products; // Stockage des produits par nom
    public ArrayList<Simulation> simulations; // Stockage des simulations
    public int socketDelay; // Stockage du délai du socket
    public ArrayList<Breakdown> breakdowns; // Stockage des pannes
    public GUIFrame guiFrame; // Référence à l'interface graphique

    // Instance statique pour implémenter un Singleton
    private static InformationCenter instance;

    // Méthode pour obtenir l'instance unique de la classe InformationCenter
    public static InformationCenter getInstance() {
        if (instance == null)
            instance = new InformationCenter();
        return instance;
    }

    // Constructeur privé initialisant les collections
    private InformationCenter() {
        Robots = new HashMap<>();
        products = new HashMap<>();
        simulations = new ArrayList<>();
        breakdowns = new ArrayList<>();
    }

    // Méthodes pour ajouter des éléments aux collections

    public void addRobot(Robot m) {
        Robots.put(m.RobotId, m);
        System.out.println("Robot added");
    }

    public void addProduct(Product p) {
        products.put(p.name, p);
        System.out.println("Product added");
    }

    public void addSimulation(Simulation s) {
        simulations.add(s);
        System.out.println("Simulation added");
    }

    public void addSocketDelay(int d) {
        socketDelay = d;
        System.out.println("Socket delay added");
    }

    public void addBreakdown(Breakdown br) {
        breakdowns.add(br);
        Robots.get(br.RobotId).breakdown = br; // Association de la panne à une Robot spécifique
        System.out.println("Breakdown added");
    }

    public void addGUIFrame(GUIFrame gui) {
        guiFrame = gui;
    }
}
