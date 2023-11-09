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
}
