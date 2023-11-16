package io.ballerina.workermodelgenerator.core;

public class LinkMetaData {
    private final String varName;
    private final String varType;

    private final String fromWorker;

    public LinkMetaData(String varName, String varType, String fromWorker) {
        this.varName = varName;
        this.varType = varType;
        this.fromWorker = fromWorker;
    }

    public String getVarName() {
        return varName;
    }

    public String getVarType() {
        return varType;
    }

    public String getFromWorker() {
        return fromWorker;
    }

}
