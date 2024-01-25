package atelier;

import java.util.HashMap;
import java.util.List;

public class RobotAgentArguments {
    public Robot Robot;
    public HashMap<String, List<RobotReference>> subproductRobots;
    public HashMap<String, List<RobotReference>> sameProductRobots;
    public HashMap<String, Product> productsDefinitions;

    public RobotAgentArguments(
            Robot Robot, HashMap<String,
            List<RobotReference>> subproductRobots,
            HashMap<String, List<RobotReference>> sameProductRobots,
            HashMap<String, Product> productsDefinitions
                                 ) {
        this.Robot = Robot;
        this.subproductRobots = subproductRobots;
        this.sameProductRobots = sameProductRobots;
        this.productsDefinitions = productsDefinitions;
    }
}
