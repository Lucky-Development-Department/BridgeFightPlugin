package me.molfordan.bridgefightplugin.manager;

import me.molfordan.bridgefightplugin.task.EggBridgeTask;

import java.util.HashSet;
import java.util.Set;

public class EggBridgeManager {

    private final Set<EggBridgeTask> activeTasks = new HashSet<>();

    public void registerTask(EggBridgeTask task) {
        activeTasks.add(task);
    }

    public void unregisterTask(EggBridgeTask task) {
        activeTasks.remove(task);
    }

    public void cancelAllTasks() {
        // Create a copy to avoid concurrent modification
        Set<EggBridgeTask> tasksToCancel = new HashSet<>(activeTasks);
        
        for (EggBridgeTask task : tasksToCancel) {
            if (task != null) {
                task.cancel();
            }
        }
        
        activeTasks.clear();
    }

    public int getActiveTaskCount() {
        return activeTasks.size();
    }
}
