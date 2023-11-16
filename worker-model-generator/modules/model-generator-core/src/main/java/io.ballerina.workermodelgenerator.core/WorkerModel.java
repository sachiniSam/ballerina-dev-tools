package io.ballerina.workermodelgenerator.core;

import java.util.ArrayList;
import java.util.List;

public class WorkerModel {
    private final List<WorkerNode> nodes;

    public WorkerModel() {
        this.nodes = new ArrayList<>();
    }

    public void addNode(WorkerNode node) {
        nodes.add(node);
    }

    public WorkerNode getNode(String name) {
        for (WorkerNode node : nodes) {
            if (node.getName().equals(name)) {
                return node;
            }
        }
        return null;
    }
}
