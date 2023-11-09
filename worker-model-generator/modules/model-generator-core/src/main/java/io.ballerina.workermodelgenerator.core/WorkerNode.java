package io.ballerina.workermodelgenerator.core;

import java.util.ArrayList;
import java.util.List;

public class WorkerNode {

    private final String name;
    private final List<Link> links;

    public WorkerNode(String name) {
        this.name = name;
        this.links = new ArrayList<>();
    }

    public void addLink(Link link) {
        links.add(link);
    }
}
