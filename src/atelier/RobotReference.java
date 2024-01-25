package atelier;

public class RobotReference {
    public String name;
    public Robot Robot;

    public RobotReference(String name, Robot Robot) {
        this.name = name;
        this.Robot = Robot;
    }
    public RobotReference(Robot Robot) {
        this.name = "Robot" + Robot.RobotId;
        this.Robot = Robot;
    }
}
