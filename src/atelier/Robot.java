package atelier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

enum RobotState
{
    free,
    busy
}

public class Robot {
    public int RobotId;
    public int socketId;
    public List<RobotAction> actions;
    public Breakdown breakdown;
    public Boolean active = true;
    public RobotState state;
    public Robot(int RobotId, int socketId, List<RobotAction> actions)
    {
        this.RobotId = RobotId;
        this.socketId = socketId;
        this.actions = actions;
        state = RobotState.free;
    }
}

