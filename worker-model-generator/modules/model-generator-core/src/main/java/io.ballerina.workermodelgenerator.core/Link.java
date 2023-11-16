package io.ballerina.workermodelgenerator.core;

public class Link {
    private final String name;
    private String varName;
    private String type;

    public void setVarName(String varName) {
        this.varName = varName;
    }

    public void setType(String type) {
        this.type = type;
    }

//    public Link(String name, String varName, String type) {
//        this.name = name;
//        this.varName = varName;
//        this.type = type;
//    }

    public Link(String name) {
        this.name = name;
    }
}
