package top.jiangyin14.mineeye.handler.ActionHandler;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.GoalXZ;

/**
 * Goto the coords that provided in API
 * @author jiangyin14
 */
public class GotoHandler {

    public void gotoCoordinates(int x, int z) {
        // Configure Baritone settings
        BaritoneAPI.getSettings().allowSprint.value = true;
        BaritoneAPI.getSettings().primaryTimeoutMS.value = 2000L;

        // Set the goal and path
        BaritoneAPI.getProvider().getPrimaryBaritone()
                .getCustomGoalProcess()
                .setGoalAndPath(new GoalXZ(x, z));
    }

    // Example method that uses hardcoded coordinates
    public void gotoHardcodedCoordinates() {
        gotoCoordinates(10000, 20000);
    }
}