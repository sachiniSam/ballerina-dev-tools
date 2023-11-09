package io.ballerina.workermodelgenerator.extension;

import com.google.gson.JsonElement;

public class WorkerDesignServiceResponse {
    private JsonElement workerDesignModel;

    public void setWorkerDesignModel(JsonElement workerDesignModel) {
        this.workerDesignModel = workerDesignModel;
    }

    public JsonElement getWorkerDesignModel() {
        return workerDesignModel;
    }
}
