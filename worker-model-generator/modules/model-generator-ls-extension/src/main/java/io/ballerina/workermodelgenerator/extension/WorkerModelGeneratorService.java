package io.ballerina.workermodelgenerator.extension;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.projects.Project;
import io.ballerina.workermodelgenerator.core.ModelGenerator;
import io.ballerina.workermodelgenerator.core.WorkerModel;
import org.ballerinalang.annotation.JavaSPIService;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.jsonrpc.services.JsonSegment;
import org.eclipse.lsp4j.services.LanguageServer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;



@JavaSPIService("org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService")
@JsonSegment("workerDesignService")
public class WorkerModelGeneratorService implements ExtendedLanguageServerService {

    private WorkspaceManager workspaceManager;

    @Override
    public void init(LanguageServer langServer, WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Override
    public Class<?> getRemoteInterface() {
        return getClass();
    }

    @JsonRequest
    public CompletableFuture<WorkerDesignServiceResponse> getWorkerDesignModel(WorkerDesignServiceRequest request) {

        return CompletableFuture.supplyAsync(() -> {
            WorkerDesignServiceResponse response = new WorkerDesignServiceResponse();
            try {
                Path filePath = Path.of(request.getFilePath());
                Project project = getCurrentProject(filePath);
                if (this.workspaceManager.semanticModel(filePath).isEmpty()) {
                    throw new Exception();
                }
                SemanticModel semanticModel = this.workspaceManager.semanticModel(filePath).get();

                ModelGenerator modelGenerator = new ModelGenerator();
                WorkerModel generatedModel = modelGenerator.getWorkerModel(project, request.getLineRange());
                Gson gson = new GsonBuilder().serializeNulls().create();
                JsonElement modelJson = gson.toJsonTree(generatedModel);
                response.setWorkerDesignModel(modelJson);
                // System.out.println(response.getWorkerDesignModel());
            } catch (WorkspaceDocumentException | EventSyncException e) {
                //System.out.println(e.getCause());

            } catch (Exception e) {
                // System.out.println(e.getCause());
            }
            return response;
        });
    }

    private Project getCurrentProject(Path path) throws WorkspaceDocumentException, EventSyncException {
        Optional<Project> project = workspaceManager.project(path);
        if (project.isEmpty()) {
            return workspaceManager.loadProject(path);
        }
        return project.get();
    }
}

