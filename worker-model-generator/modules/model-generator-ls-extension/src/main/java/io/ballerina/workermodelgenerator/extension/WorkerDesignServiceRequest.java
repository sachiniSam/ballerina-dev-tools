package io.ballerina.workermodelgenerator.extension;

import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;

public class WorkerDesignServiceRequest {
    private final String filePath;
    private final LinePosition startLine;
    private final LinePosition endLine;

    public WorkerDesignServiceRequest(String filePath, LinePosition startLine, LinePosition endLine) {
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
    }

    public String getFilePath() {
        return filePath;
    }

    public LineRange getLineRange() {
        LineRange lineRange = LineRange.from(filePath, startLine, endLine);
        return lineRange;
    }
}