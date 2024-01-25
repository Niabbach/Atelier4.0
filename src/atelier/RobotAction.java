package atelier;

public class RobotAction {
    public String productName;
    public int stageId;
    public String actionName;
    public int productionTime;
    public RobotAction (String productName, int stageId, String actionName, int productionTime){
        this.productName = productName;
        this.stageId = stageId;
        this.actionName = actionName;
        this.productionTime = productionTime;
    }
}
