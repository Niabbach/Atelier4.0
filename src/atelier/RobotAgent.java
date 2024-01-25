package atelier;

import com.google.gson.Gson;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import javax.swing.*;
import java.sql.Time;
import java.util.*;
import java.util.Timer;

public class RobotAgent extends Agent {
    Robot Robot;
    HashMap<String, List<RobotReference>> subproductRobots;
    HashMap<String, List<RobotReference>> sameProductRobots;
    HashMap<String, Product> productsDefinitions;
    Gson parser = new Gson();
    int lastGenerated = 0;
    Boolean recievedAllComponents = false;
    Map<String, Map<String, List<PlanElement>>> PlanMap = new HashMap<String, Map<String, List<PlanElement>>>();
    List<List<ProduceElement>> ProduceList = new LinkedList<List<ProduceElement>>();
    ProduceElement currProdElement;
    List<TimeElement> TimeAxis = new LinkedList<TimeElement>();

    @Override
    protected void setup() {
        RobotAgentArguments args = (RobotAgentArguments) (getArguments()[0]);
        Robot = args.Robot;
        subproductRobots = args.subproductRobots;
        sameProductRobots = args.sameProductRobots;
        productsDefinitions = args.productsDefinitions;

        Behaviour initRobot = new OneShotBehaviour() {
            @Override
            public void action() {
                System.out.println(getAID().getLocalName() + " started");
                System.out.println("My actions:");
                System.out.println("My AID: " + getAID().toString());
                Robot.actions.forEach((action) -> {
                    System.out.println("Product: " + action.productName + ", action: " + action.actionName);
                });
                subproductRobots.forEach((product, Robots) -> {
                    Robots.forEach(m -> {
                        System.out.println("I'm " + getAID().getLocalName() + " and I produce " + product + " Robot that produce subproducts: " + m.name);
                    });
                });
                sameProductRobots.forEach((product, Robots) -> {
                    Robots.forEach(m -> {
                        System.out.println("I'm " + getAID().getLocalName() + " and I produce " + product + " Robot that also produces this: " + m.name);
                    });
                });
            }
        };
        InitProductionList();
        addBehaviour(Produce);
        addBehaviour(initRobot);
        addBehaviour(GetPlan());
        addBehaviour(GetAcceptOffer());
        addBehaviour(HandleBreakDown);
        addBehaviour(HandleBreakdownReplacement);
        addBehaviour(GetReplacementAsk);
    }
    private void InitProductionList(){
        for(int i = 0; i < 10; i++){
            ProduceList.add(new LinkedList<ProduceElement>());
        }
    }

    @Override
    protected void takeDown() {
        super.takeDown();
    }

    SimpleBehaviour GetPlan() {
        return new SimpleBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchProtocol("plan");
                ACLMessage rcv = receive(mt);
                if (rcv != null) {
                    PlanMessage message = parser.fromJson(rcv.getContent(), PlanMessage.class);
                    HandlePlan(message);
                } else
                    block();
            }

            @Override
            public boolean done() {
                return false;
            }
        };
    }
    SimpleBehaviour GetAcceptOffer() {
        return new SimpleBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchProtocol("acceptOffer");
                ACLMessage rcv = receive(mt);
                if (rcv != null) {
                    AcceptOfferMessage message = parser.fromJson(rcv.getContent(), AcceptOfferMessage.class);
                    HandleAcceptOffer(message);
                } else
                    block();
            }

            @Override
            public boolean done() {
                return false;
            }
        };
    }

    private void ReturnOffer(String id, Long productionEnd, String receiver) {
        OfferMessage content = Robot.active? new OfferMessage(productionEnd) :  new OfferMessage(-1);
        ACLMessage msg = new ACLMessage();
        msg.setContent(parser.toJson(content));
        AID AIDreceiver = new AID(receiver, AID.ISLOCALNAME);
        msg.addReceiver(AIDreceiver);
        msg.setProtocol(id);
        send(msg);
    }

    private RobotAction FindAction(PlanMessage request) {
        for (int i = 0; i < Robot.actions.size(); i++) {
            if (Robot.actions.get(i).actionName.equals(request.GetActionName())
                    && Robot.actions.get(i).stageId == request.GetStageId()
                    && Robot.actions.get(i).productName.equals(request.GetProductName())) {
                return Robot.actions.get(i);
            }
        }
        return null;
    }
    private RobotAction FindAction(String actionName, int stageId, String productName) {
        for (int i = 0; i < Robot.actions.size(); i++) {
            if (Robot.actions.get(i).actionName.equals(actionName)
                    && Robot.actions.get(i).stageId == stageId
                    && Robot.actions.get(i).productName.equals(productName)) {
                return Robot.actions.get(i);
            }
        }
        return null;
    }

    private ProductAction GetProductAction(RobotAction RobotAction) {
        InformationCenter ic = InformationCenter.getInstance();
        Product requestedProduct = ic.products.get(RobotAction.productName);
        return requestedProduct.stages.get(RobotAction.stageId)
                .stream().filter(a -> a.actionName.equals(RobotAction.actionName))
                .findAny().get();
    }

    private boolean RobotActionMatch(RobotAction mAction, String pName, int pStage, String aName) {
        if (mAction.actionName.equals(aName) && mAction.stageId == pStage && mAction.productName.equals(pName))
            return true;
        return false;
    }

    private String GenerateId() {
        ++lastGenerated;
        String id = Integer.toString(lastGenerated);
        for (int i = id.length(); i < 9; i++) {
            id = "0" + id;
        }
        return Integer.toString(Robot.RobotId) + id;
    }
    private long GetFinalProductionEnd(int priority, int prodTime, Collection<Long> times){
        long lastSubProdTime = Collections.max(times);
        return GetFinalProductionEnd(priority, prodTime, lastSubProdTime);
    }
    private long GetFinalProductionEnd(int priority, int prodTime, long possibleStartTime){
        for(int i = 0; i < TimeAxis.size(); i++) {
            if(TimeAxis.get(i)._priority >= priority) {
                if (TimeAxis.get(i)._startTime - possibleStartTime > prodTime) {
                    return possibleStartTime + prodTime;
                }
                else {
                    possibleStartTime = TimeAxis.get(i)._endTime;
                }
            }
        }
        return possibleStartTime + prodTime;
    }
    private void HandleAcceptOffer(AcceptOfferMessage message){
        Map<String, List<PlanElement>> planMapNode = PlanMap.get(message.getId());
        List<PlanElement> tmpSubProductList = new LinkedList<PlanElement>();
        String[] keys = planMapNode.keySet().toArray(new String[planMapNode.keySet().size()]);
        for(int i = 0; i < keys.length; i++){
            for(int j = 0; j < planMapNode.get(keys[i]).size(); j++){
                if(planMapNode.get(keys[i]).get(j)._bestOffer){
                    tmpSubProductList.add(planMapNode.get(keys[i]).get(j));
                    break;
                }
            }
        }
        List<PlanElement> subProductList = new LinkedList<PlanElement>();
        if(!tmpSubProductList.get(0)._lastProcess){
            subProductList = tmpSubProductList;
        }
        ProduceElement prodElement = new ProduceElement(subProductList, tmpSubProductList.get(0)._upperMessageContent, tmpSubProductList.get(0)._lastPlanSubproductTime);
        AddToProduceList(prodElement);
        for(int i = 0; i < subProductList.size(); i++){
            AcceptOffer(message.getId(), subProductList.get(i));
        }
    }

    private void AcceptOffer(String productId, PlanElement planElem){
        Robot.state = RobotState.busy;
        addBehaviour(GetProduct(productId, planElem._messageContent.getId()));
        ACLMessage msg = new ACLMessage();
        msg.addReceiver(planElem._receiver);
        msg.setProtocol("acceptOffer");
        AcceptOfferMessage messageContent = new AcceptOfferMessage(planElem._messageContent.getId());
        msg.setContent(parser.toJson(messageContent));
        System.out.println("Agent " + planElem._requestingAgent.getLocalName() + " accept offer from " +
                planElem._receiver.getLocalName());
        send(msg);
    }
    SimpleBehaviour GetProduct(String productId, String id){
        return new SimpleBehaviour() {
            boolean finished = false;
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchProtocol("product" + id);
                ACLMessage rcv = receive(mt);
                if (rcv != null) {
                    ProduceElement tmpProduceElem = FindProduceElement(productId);
                    tmpProduceElem.Acquire(id);
                    finished = true;
                } else
                    block();
            }
            @Override
            public boolean done() {
                return finished;
            }
        };
    }
    private ProduceElement FindProduceElement(String Id){
        for(int i = 0; i<10; i++){
            for(int j = 0; j<ProduceList.get(i).size(); j++){
                if(ProduceList.get(i).get(j)._planMessage.getId().equals(Id))
                    return ProduceList.get(i).get(j);
            }
        }
        return null;
    }
    private void HandlePlan(PlanMessage plan) {
        RobotAction requestedAction = FindAction(plan);
        if (requestedAction == null) {
            ReturnOffer(plan.getId(), Long.MAX_VALUE, plan.getRequestingAgent());
        }
        PlanMap.put(plan.getId(), new HashMap<String, List<PlanElement>>());
        Map<Integer, List<String>> bookedActions = plan.getBookedActions();
        ProductAction action = GetProductAction(requestedAction);
        if (bookedActions.get(plan.GetStageId()) != null)
            bookedActions.get(plan.GetStageId()).add(action.actionName);
        else
            bookedActions.put(plan.GetStageId(), new ArrayList<String>() {
                {
                    add(action.actionName);
                }
            });
        List<String> subproducts = new ArrayList<String>(action.subproducts);
        InformationCenter ic = InformationCenter.getInstance();
        HashMap<String, List<PlanElement>> tmpPlanMap = new HashMap<String, List<PlanElement>>();

        
        subproducts.forEach(p -> {
            final Product product = ic.products.get(p);
            List<PlanElement> planList = new LinkedList<PlanElement>();
            subproductRobots.get(p).forEach(subRobot -> {
                int stageId = Collections.max(product.stages.keySet());
                product.stages.get(stageId).forEach(productAction -> {
                    subRobot.Robot.actions.forEach(RobotAction -> {
                        if (RobotActionMatch(RobotAction, product.name, stageId, productAction.actionName)) {
                            PlanMessage msg = new PlanMessage(product.name, productAction.actionName, stageId, plan.getPriority(),
                                    new HashMap<Integer, List<String>>(), getLocalName(), GenerateId(), Robot.socketId);
                            ACLMessage aclMessage = new ACLMessage();
                            aclMessage.setProtocol("plan");
                            AID receiver = new AID("Robot" + Integer.toString(subRobot.Robot.RobotId), AID.ISLOCALNAME);
                            aclMessage.addReceiver(receiver);
                            planList.add(new PlanElement(msg, aclMessage, receiver, new AID(plan.getRequestingAgent(), AID.ISLOCALNAME), plan));
                        }
                    });
                });
            });
            tmpPlanMap.put(p, planList);
        });
        
        Product requestedProduct = ic.products.get(plan.GetProductName());
        int tmpPrevStageId = plan.GetStageId();
        if (bookedActions.get(plan.GetStageId()).size() == requestedProduct.stages.get(plan.GetStageId()).size())
            --tmpPrevStageId;
        if (tmpPrevStageId == 0) {
            long productionTime = GetFinalProductionEnd(plan.getPriority(), FindAction(plan.GetActionName(), plan.GetStageId(),
                    plan.GetProductName()).productionTime, System.currentTimeMillis());
            List<PlanElement> lastActionPlanList = new LinkedList<PlanElement>();
            PlanElement lastActionPlanElement = new PlanElement(null, null, null, new AID(plan.getRequestingAgent(), AID.ISLOCALNAME), plan);
            lastActionPlanElement._lastProcess = true;
            lastActionPlanElement._bestOffer = true;
            lastActionPlanElement._lastPlanSubproductTime = System.currentTimeMillis();
            lastActionPlanList.add(lastActionPlanElement);
            tmpPlanMap.put("lastAction", lastActionPlanList);
            PlanMap.put(plan.getId(), tmpPlanMap);
            ReturnOffer(plan.getId(), productionTime, plan.getRequestingAgent());
            return ;
        }
        
        List<PlanElement> prevActionPlanList = new LinkedList<PlanElement>();
        final int prevStageId = tmpPrevStageId;
        requestedProduct.stages.get(prevStageId).stream().filter(a ->
                (prevStageId != plan.GetStageId() || bookedActions.get(prevStageId).contains(a.actionName) == false)
        ).forEach(productAction -> {
            PlanMessage msg = new PlanMessage(requestedProduct.name, productAction.actionName, prevStageId, plan.getPriority(),
                    bookedActions, getLocalName(), "dummyID", Robot.socketId);
            sameProductRobots.get(plan.GetProductName()).forEach(subRobot -> {
                subRobot.Robot.actions.forEach(RobotAction -> {
                    if (RobotActionMatch(RobotAction, requestedProduct.name, prevStageId, productAction.actionName)) {
                        ACLMessage aclMessage = new ACLMessage();
                        aclMessage.setProtocol("plan");
                        AID receiver = new AID("Robot" + Integer.toString(subRobot.Robot.RobotId), AID.ISLOCALNAME);
                        aclMessage.addReceiver(receiver);
                        msg.setId(GenerateId());
                        prevActionPlanList.add(new PlanElement(new PlanMessage(msg), aclMessage, receiver, new AID(plan.getRequestingAgent(), AID.ISLOCALNAME), plan));
                    }
                });
            });
        });
        tmpPlanMap.put("prevActions", prevActionPlanList);
        PlanMap.put(plan.getId(), tmpPlanMap);
        addBehaviour(PlanBehaviour(plan.getId()));
    }

    SimpleBehaviour PlanBehaviour(String productId) {
        return new SimpleBehaviour() {
            boolean finished = false;

            @Override
            public void action() {
                String[] keys = PlanMap.get(productId).keySet().toArray(new String[PlanMap.get(productId).keySet().size()]);
                for (int j = 0; j < keys.length; j++) {
                    List<PlanElement> refPlanList = PlanMap.get(productId).get(keys[j]);
                    for (int i = 0; i < refPlanList.size(); i++) {
                        addBehaviour(WaitForOffer(productId, refPlanList.get(i)._messageContent.getId()));
                        refPlanList.get(i)._aclMessage.setContent(parser.toJson(refPlanList.get(i)._messageContent));
                        send(refPlanList.get(i)._aclMessage);
                    }
                }
                finished = true;
            }

            @Override
            public boolean done() {
                return finished;
            }
        };
    }

    SimpleBehaviour WaitForOffer(String productId, String id) {
        return new SimpleBehaviour() {
            boolean finished = false;

            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchProtocol(id);
                ACLMessage rcv = receive(mt);
                if (rcv != null) {
                    OfferMessage offer = parser.fromJson(rcv.getContent(), OfferMessage.class);
                    boolean allOffersArrived = true;
                    String[] keys = PlanMap.get(productId).keySet().toArray(new String[PlanMap.get(productId).keySet().size()]);
                    for (int j = 0; j < keys.length; j++) {
                        List<PlanElement> refPlanList = PlanMap.get(productId).get(keys[j]);
                        for (int i = 0; i < refPlanList.size(); i++) {
                            if (refPlanList.get(i)._messageContent.getId().equals(id)) {
                                refPlanList.get(i)._productionEnd = offer.getProductionEnd();
                            } else if (refPlanList.get(i)._productionEnd == 0) {
                                allOffersArrived = false;
                            }
                        }
                    }
                    if (allOffersArrived) {
                        addBehaviour(ChooseOffers(productId));
                    }
                    finished = true;
                } else
                    block();
            }

            @Override
            public boolean done() {
                return finished;
            }
        };
    }
    SimpleBehaviour ChooseOffers(String productId){
        return new SimpleBehaviour() {
            boolean finished = false;
            @Override
            public void action() {
                String[] keys = PlanMap.get(productId).keySet().toArray(new String[PlanMap.get(productId).keySet().size()]);
                Map<String, Long> offersTimes = new HashMap<String, Long>();
                Map<String, Integer> offersIdx = new HashMap<String, Integer>();
                for (int j = 0; j < keys.length; j++) {
                    offersTimes.put(keys[j], Long.MAX_VALUE);
                    offersIdx.put(keys[j], 0);
                    List<PlanElement> refPlanList = PlanMap.get(productId).get(keys[j]);
                    for (int i = 0; i < refPlanList.size(); i++) {
                        String requesting = refPlanList.get(i)._requestingAgent.getLocalName();
                        int delay = GetSocketDelay(requesting);
                        if (refPlanList.get(i)._productionEnd + delay < offersTimes.get(keys[j])){
                            offersIdx.replace(keys[j], i);
                            offersTimes.replace(keys[j], refPlanList.get(i)._productionEnd + delay);
                        }
                    }
                }
                long lastSubproductTime = Collections.max(offersTimes.values());
                for(int i = 0; i < keys.length; i++){
                    if (PlanMap.get(productId).get(keys[i]).size() > 0){
                        PlanMap.get(productId).get(keys[i]).get(offersIdx.get(keys[i]))._bestOffer = true;
                        PlanMap.get(productId).get(keys[i]).get(offersIdx.get(keys[i]))._lastPlanSubproductTime = lastSubproductTime;
                    }
                }
                    PlanMessage productPlan = PlanMap.get(productId).get(keys[0]).get(0)._upperMessageContent;
                    long finalProductionEnd = GetFinalProductionEnd(productPlan.getPriority(),
                            FindAction(productPlan.GetActionName(), productPlan.GetStageId(), productPlan.GetProductName()).productionTime, offersTimes.values());
                    ReturnOffer(productId, finalProductionEnd, PlanMap.get(productId).get(keys[0]).get(0)._requestingAgent.getLocalName());
                    finished = true;
            }
            @Override
            public boolean done() {
                return finished;
            }
        };
    }
    Behaviour Produce = new TickerBehaviour(this, 500) {
        @Override
        public void onTick() {
            recievedAllComponents = true;
            if (Robot.active) {
                if (currProdElement != null) {
                    String req = currProdElement._planMessage.getRequestingAgent();
                    int delay = GetSocketDelay(req);
                    addBehaviour(SendProduct(currProdElement, delay));
                    currProdElement = null;
                }
                for (int i = 9; i >= 0; i--) {
                    for (int j = 0; j < ProduceList.get(i).size(); j++) {
                        if (ProduceList.get(i).get(j)._readyToProduce) {
                            currProdElement = ProduceList.get(i).get(j);
                            ProduceList.get(i).remove(j);
                            break;
                        }
                    }
                    if (currProdElement != null)
                        break;
                }
                if (currProdElement != null) {
                    RobotAction action = FindAction(currProdElement._planMessage);
                    InformationCenter.getInstance().guiFrame.addProduct(getLocalName(), action.productName);
                    System.out.println(getLocalName() + " started produce " + action.productName
                            + ". Product will be ready in " + action.productionTime + " ms");
                    reset(action.productionTime);
                }
            }
        }
    };
    WakerBehaviour SendProduct(ProduceElement product, int delay) {
        return new WakerBehaviour(this, delay) {
            @Override
            public void onWake() {
                if (Robot.active) {
                    ACLMessage msg = new ACLMessage();
                    msg.addReceiver(new AID(product._planMessage.getRequestingAgent(), AID.ISLOCALNAME));
                    //if (product._planMessage.getRequestingAgent().equals("SimulationAgent"))
                    InformationCenter.getInstance().guiFrame.removeProduct(getLocalName(), product._planMessage.GetProductName());
                    msg.setProtocol("product" + product._planMessage.getId());
                    send(msg);
                }
            }
        };
    }

    private int GetSocketDelay(String requestingAgent) {
        int delay = 0;
        int socketId = Robot.socketId;
        if (!requestingAgent.equals("SimulationAgent")) {
            int len = requestingAgent.length();
            requestingAgent = requestingAgent.substring(7, len);
            int socket = InformationCenter.getInstance().Robots.get(Integer.parseInt(requestingAgent)).socketId;
            if (socket != socketId)
                delay += InformationCenter.getInstance().socketDelay;
        }
        return delay;
    }

    private void AddToProduceList(ProduceElement prodElement){
        int priority = prodElement._planMessage.getPriority();
        ProduceList.get(priority).add(prodElement);
        TimeAxis = new LinkedList<TimeElement>();
        for(int i = 9; i >= 0; i--){
            for(int j = 0; j < ProduceList.get(i).size(); j++){
                long possibleStart = ProduceList.get(i).get(j)._lastPlanSubproductTime;
                int prodTime = FindAction(prodElement._planMessage).productionTime;
                boolean notAdded = true;
                for(int k = 0; k < TimeAxis.size(); k++){
                    if(TimeAxis.get(k)._startTime - possibleStart > prodTime){
                        TimeAxis.add(k, new TimeElement(possibleStart, possibleStart + prodTime, ProduceList.get(i).get(j)._planMessage.getPriority()));
                        ProduceList.get(i).get(j)._productionTimeStart = possibleStart;
                        ProduceList.get(i).get(j)._productionTimeEnd = possibleStart + prodTime;
                        notAdded = false;
                        break;
                    }
                    else{
                        possibleStart = TimeAxis.get(k)._endTime;
                    }
                }
                if(notAdded){
                    TimeAxis.add(new TimeElement(possibleStart, possibleStart + prodTime, ProduceList.get(i).get(j)._planMessage.getPriority()));
                    ProduceList.get(i).get(j)._productionTimeStart = possibleStart;
                    ProduceList.get(i).get(j)._productionTimeEnd = possibleStart + prodTime;
                }
            }
        }
    }

    OneShotBehaviour HandleBreakDown = new OneShotBehaviour() {
        @Override
        public void action() {
            if (Robot.breakdown != null) {
                Timer workTimer = new Timer();
                workTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Robot.active = false;
                        System.out.println(Robot.RobotId + " Robot IS BROKE");
                        InformationCenter.getInstance().guiFrame.breakRobot(getLocalName());
                    }
                }, Robot.breakdown.breakTime);

                Timer breakdownTimer = new Timer();
                breakdownTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Robot.active = true;
                        System.out.println(Robot.RobotId + " Robot IS WORKING AGAIN");
                        InformationCenter.getInstance().guiFrame.resumeRobot(getLocalName());
                        Robot.breakdown = null;
                    }
                }, Robot.breakdown.breakTime + Robot.breakdown.durationOfBreak);
            }
        }
    };

    TickerBehaviour HandleBreakdownReplacement = new TickerBehaviour(this, 500) {
        @Override
        protected void onTick() {
            if (!Robot.active && recievedAllComponents) {
                //System.out.println("Reasigment task");
                recievedAllComponents = true;
                //if (!Robot.active) {
                    for (int i = 9; i >= 0; i--) { 
                        for (int j = 0; j < ProduceList.get(i).size(); j++) {
                            if (ProduceList.get(i).get(j)._readyToProduce) {
                                currProdElement = ProduceList.get(i).get(j);
                                ProduceList.get(i).remove(j);
                                break;
                            }
                        }
                        if (currProdElement != null)
                            break;
                    }
                    if (currProdElement != null) {
                        try {
                            ContainerController cc = getContainerController();
                            Object[] args = new Object[1];
                            args[0] = new BreakdownArgs(currProdElement._planMessage.GetProductName(), currProdElement._planMessage.GetStageId());
                            AgentController ac = cc.createNewAgent("BreakdownAgent_" + Robot.RobotId + currProdElement._planMessage.getId(), "atelier.BreakDownAgent", args);
                            ac.start();
                            currProdElement = null;
                        } catch (StaleProxyException e) {
                            e.printStackTrace();
                        }
                    }
                }
            //}
        }
    };

    SimpleBehaviour GetReplacementAsk = new SimpleBehaviour(){
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchProtocol("replacementAsk");
            ACLMessage rcv = receive(mt);
            if (rcv != null) {
                System.out.println("REPLACEMENT");
//                    replacementAskMessage message = parser.fromJson(rcv.getContent(), replacementAskMessage);
//                    ProduceList = message.prod;
//                    for (int i = 9; i >= 0; i--) { // priority of actions
//                        for (int j = 0; j < ProduceList.get(i).size(); j++) {
//                            if (ProduceList.get(i).get(j)._readyToProduce) {
//                                currProdElement = ProduceList.get(i).get(j);
//                                ProduceList.get(i).remove(j);
//                                    System.out.println(getLocalName() + " started produce " + action.productName
//                                        + ". Product will be ready in " + action.productionTime + " ms");
//                                break;
//                            }
//                        }
//                    }
            } else
                block();
        }

        @Override
        public boolean done() {
            return false;
        }
    };
}