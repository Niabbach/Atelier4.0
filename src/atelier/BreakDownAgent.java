package atelier;

import com.google.gson.Gson;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.nio.channels.ScatteringByteChannel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BreakDownAgent extends Agent {

    // Attributs pour stocker les informations de l'agent
    InformationCenter ic; // Centre d'information
    String productName; // Nom du produit
    int stageId; // Identifiant de l'étape
    int replacement = Integer.MIN_VALUE; // Identifiant de la Robot de remplacement, initialisé à une valeur minimale par défaut

    @Override
    protected void setup() {
        // Récupération des arguments passés à l'agent lors de sa création
        BreakdownArgs args = (BreakdownArgs) (getArguments()[0]);
        productName = args.productName;
        stageId = args.stageId;
        
        // Ajout des comportements à l'agent
        addBehaviour(FindReplacement);
        addBehaviour(ReassignProduction);
    }

    // Comportement pour trouver une Robot de remplacement
    OneShotBehaviour FindReplacement = new OneShotBehaviour() {
        @Override
        public void action() {
            int bestTime = Integer.MAX_VALUE; // Initialisation d'une variable pour stocker le meilleur temps de production trouvé
            try {
                // Parcours des Robots disponibles dans le centre d'information
                for (Map.Entry<Integer, Robot> entry : ic.Robots.entrySet()) {
                    Integer key = entry.getKey();
                    Robot value = entry.getValue();

                    // Filtrage des actions de la Robot pour trouver celles correspondant au produit et à l'étape actuelle
                    List<RobotAction> result = value.actions.stream()
                            .filter(item -> item.productName.equals(productName) && item.stageId == stageId)
                            .collect(Collectors.toList());
                    
                    // Si aucune action correspondante n'est trouvée, passe à la Robot suivante
                    if (result == null)
                        continue;

                    // Mise à jour de la Robot de remplacement avec le temps de production le plus court trouvé
                    if (result.get(0).productionTime < bestTime) {
                        bestTime = result.get(0).productionTime;
                        replacement = key;
                    }
                }
            }
            // Gestion d'une éventuelle exception lors de la recherche de la Robot de remplacement
            catch(Exception e) {
                System.out.println("No replacement found to produce " + productName);
            }
        }
    };

    // Comportement pour réaffecter la production à la Robot de remplacement trouvée
    OneShotBehaviour ReassignProduction = new OneShotBehaviour() {
        @Override
        public void action() {
            // Si une Robot de remplacement a été trouvée, affiche un message indiquant sa sélection
            if(replacement!= Integer.MIN_VALUE)
            {
                System.out.println("Replacement found: Robot " + replacement);
            }
        }
    };

    @Override
    protected void takeDown() {
        super.takeDown();
    }
}
