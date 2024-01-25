package atelier;

import com.google.gson.reflect.TypeToken;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;

import com.google.gson.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class ManagerAgent extends Agent {
    private List<RobotReference> RobotAgents = new ArrayList<RobotReference>();
    private InformationCenter ic;

    // Méthode pour analyser et traiter un objet Robot
    protected void parseRobotObject(JsonObject Robot, InformationCenter ic) {
        int RobotId = Robot.get("RobotId").getAsInt();
        int socketId = Robot.get("socketId").getAsInt();
        List<RobotAction> actions = new ArrayList<RobotAction>();
        JsonArray actionsJson = Robot.get("actions").getAsJsonArray();
        actionsJson.forEach(p-> actions.add(new RobotAction(
                p.getAsJsonObject().get("productName").getAsString(),
                p.getAsJsonObject().get("stageId").getAsInt(),
                p.getAsJsonObject().get("actionName").getAsString(),
                p.getAsJsonObject().get("productionTime").getAsInt()
        )));
        Robot m = new Robot(RobotId, socketId, actions);
        ic.addRobot(m);
    }
    // Méthode pour analyser et traiter un objet breakdown
    protected void parseBreakDownObject(JsonObject breakdown, InformationCenter ic) {
        int RobotId = breakdown.get("RobotId").getAsInt();
        int breakTime = breakdown.get("breakTime").getAsInt();
        int duration = breakdown.get("duration").getAsInt();

        ic.addBreakdown(new Breakdown(RobotId, breakTime, duration));
    }

    // Méthode pour analyser et traiter un objet product
    protected void parseProductObject(JsonObject product, InformationCenter ic) {
        String productName = product.get("name").getAsString();
        HashMap<Integer, List<ProductAction>> stages = new HashMap<Integer, List<ProductAction>>();
        JsonArray stagesJsonArray = product.get("stages").getAsJsonArray();
        for(int i = 0; i < stagesJsonArray.size(); i++){
            int stageId = stagesJsonArray.get(i).getAsJsonObject().get("stageId").getAsInt();
            List<ProductAction> actions = new ArrayList<ProductAction>();
            JsonArray actionJsonArray = stagesJsonArray.get(i).getAsJsonObject().get("actions").getAsJsonArray();
            for(int j = 0; j < actionJsonArray.size(); j++){
                List<String> subproducts = new ArrayList<String>();
                JsonArray subproductsJsonArray = actionJsonArray.get(j).getAsJsonObject().get("subproducts").getAsJsonArray();
                subproductsJsonArray.forEach(s -> subproducts.add(s.getAsString()));
                actions.add(new ProductAction(actionJsonArray.get(j).getAsJsonObject().get("actionName").getAsString(), subproducts));
            }
            stages.put(stageId, actions);
        }
        Product p = new Product(productName, stages);
        ic.addProduct(p);
    }

    // Méthode pour analyser et traiter un objet simulation
    protected void parseSimulationObject(JsonObject simulation, InformationCenter ic) {
        JsonArray simulations = simulation.get("demandedProducts").getAsJsonArray();
        ArrayList<DemandedProduct> users = new ArrayList<DemandedProduct>();
        simulations.forEach(p-> users.add( new DemandedProduct(p.getAsJsonObject().get("name").getAsString(),p.getAsJsonObject().get("amount").getAsInt(), p.getAsJsonObject().get("priority").getAsInt())));
        Simulation s = new Simulation(simulation.get("duration").getAsInt(), users);
        ic.addSimulation(s);
    }

    // Méthode pour ajouter un nouvel agent Robot
    protected void addNewRobotAgent(Robot Robot, Collection<Robot> Robots, HashMap<String, Product> products) {
        ContainerController cc = getContainerController();
        AgentController ac = null;
        try {
            String agentName = "Robot" + Robot.RobotId;
            RobotAgents.add(new RobotReference(agentName, Robot));
            HashMap<String, List<RobotReference>> productsSubRobots = new HashMap<String, List<RobotReference>>();
            HashMap<String, List<RobotReference>> sameProdRobots = new HashMap<String, List<RobotReference>>();
            HashMap<String, Product> productsDefinitions = new HashMap<>();

            Robot.actions.forEach((action)-> {
                
                List<ProductAction> productAction = products.get(action.productName).stages.get(action.stageId)
                      .stream().filter(a -> a.actionName.equals(action.actionName)).collect(Collectors.toList());
                List<String> subproducts = new ArrayList<>();
                if (productAction.size() > 0)
                    subproducts = productAction.get(0).subproducts;
                
                subproducts.forEach(product -> {
                    int maxStage = Collections.max(products.get(product).stages.keySet());
                    productsSubRobots.put(product, Robots.stream().filter(m -> m.actions.stream()
                            .anyMatch(a -> a.productName.equals(product) && a.stageId == maxStage))
                            .map(m -> new RobotReference(m)).collect(Collectors.toList()));
                });
                
                List<RobotReference> sameProductRobots = Robots.stream().filter(m -> m.actions.stream()
                        .anyMatch(a -> a.productName.equals(action.productName) && !a.actionName.equals(action.actionName)
                                && (a.stageId == action.stageId || a.stageId == action.stageId - 1)))
                        .map(m -> new RobotReference(m)).collect(Collectors.toList());
                if(sameProdRobots.containsKey(action.productName)){
                    for(int i = 0; i < sameProductRobots.size(); i++){
                        sameProdRobots.get(action.productName).add(sameProductRobots.get(i));
                    }
                }
                else{
                    sameProdRobots.put(action.productName, sameProductRobots);
                }
                
                if(!productsDefinitions.containsKey(action.productName)){
                    productsDefinitions.put(action.productName, products.get(action.productName));
                }
            });
            Object[] args = new Object[1];
            args[0] = new RobotAgentArguments(Robot, productsSubRobots, sameProdRobots, productsDefinitions);
            ac = cc.createNewAgent(agentName, "atelier.RobotAgent", args);
            ac.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    // Méthode pour vérifier si une liste contient au moins un élément d'une autre liste
    private boolean containsAny(HashMap<String, Integer> map, List<String> list) {
        for (String candidate : list) {
            if (map.containsKey(candidate))
                return true;
        }
        return false;
    }

    // Méthode pour lancer la simulation
    private void RunSimulation(){
        ContainerController cc = getContainerController();
        try {
            Object[] args = new Object[1];
            args[0] = InformationCenter.getInstance();
            AgentController ac = cc.createNewAgent("SimulationAgent", "atelier.SimulationAgent", args);
            ac.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    // Méthode de configuration initiale de l'agent ManagerAgent
    protected void setup() {
        Behaviour readConfiguration = new OneShotBehaviour() {
            @Override
            public void action() {
                Gson gson = new Gson();
                Reader reader = null;
                JsonParser parser = new JsonParser();
                try {
                        System.out.print("Enter config path: ");
                        Scanner scanner = new Scanner(System. in);
                        String configPath = scanner.nextLine();
                        configPath = configPath.toLowerCase();
                        if (!configPath.contains(".json"))
                            configPath = configPath.concat(".json");
                        reader = Files.newBufferedReader(Paths.get(configPath));
                        JsonElement jsonTree = parser.parse(reader);
                        JsonObject jsonObject = jsonTree.getAsJsonObject();

                        JsonArray Robots = jsonObject.get("Robots").getAsJsonArray();
                        Robots.forEach(m-> parseRobotObject(m.getAsJsonObject(), ic));

                        JsonArray products =  jsonObject.get("products").getAsJsonArray();
                        products.forEach(p-> parseProductObject(p.getAsJsonObject(), ic));

                        JsonArray simulations = jsonObject.get("simulation").getAsJsonArray();
                        simulations.forEach(s -> parseSimulationObject(s.getAsJsonObject(), ic));

                        try {
                            int delay = jsonObject.get("socketDelay").getAsInt();
                            ic.addSocketDelay(delay);

                            JsonArray breakdowns = jsonObject.get("breakdown").getAsJsonArray();
                            breakdowns.forEach(b -> parseBreakDownObject(b.getAsJsonObject(), ic));
                        }
                        catch(Exception e)
                        {

                        }


                        reader.close();
                    }
                catch (Exception e) {
                        e.printStackTrace();
                    }
            }
        };
        Behaviour generateRobots = new WakerBehaviour(this, 2000) {
            @Override
            protected void onWake() {
                System.out.println("Initializing Robots...");
                ic.Robots.forEach((id, Robot)-> {
                    addNewRobotAgent(Robot, ic.Robots.values(), ic.products);
                });
            }
        };

        Behaviour initGUI = new WakerBehaviour(this, 3000) {
            @Override
            protected void onWake() {
                System.out.println("Start gui...");
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ic.addGUIFrame(new GUIFrame(RobotAgents));
                    }
                });
            }
        };

        Behaviour startSimulation = new WakerBehaviour(this, 4000) {
            @Override
            protected void onWake() {
                System.out.println("Starting simulation...");
                RunSimulation();
            }
        };
        ic = InformationCenter.getInstance();
        addBehaviour(readConfiguration);
        addBehaviour(generateRobots);
        addBehaviour(initGUI);
        addBehaviour(startSimulation);

    }

    // Méthode appelée lors de la désactivation de l'agent ManagerAgent
    @Override
    protected void takeDown() {
        super.takeDown();
    }
}