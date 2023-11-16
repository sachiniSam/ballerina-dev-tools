package io.ballerina.workermodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ballerina.projects.Project;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class GeneratorTest {
    private static final Path RES_DIR = Paths.get("src", "test", "resources").toAbsolutePath();
    private static final String BALLERINA = "ballerina";
    private static final String RESULTS = "results";
    Gson gson = new GsonBuilder().serializeNulls().create();

    @Test(description = "model generation for single module projects")
    public void testSingleModuleModelGeneration() throws Exception {
        Path projectPath = RES_DIR.resolve(BALLERINA).resolve(
                Path.of("worker_services"));
        Path expectedJsonPath = RES_DIR.resolve(RESULTS).resolve(Path.of("simple_service_sample.json"));

        Project project = TestUtils.loadBuildProject(projectPath, false);
        ModelGenerator modelGenerator = new ModelGenerator();
        LineRange lineRange = getLineRange(projectPath.toString(),
                LinePosition.from(4, 4), LinePosition.from(8, 5));
//        WorkerModel workerModel = modelGenerator.getWorkerModel(project,lineRange, semanticModel);
//        ArchitectureModelBuilder architectureModelBuilder = new ArchitectureModelBuilder();
//        ArchitectureModel generatedModel = architectureModelBuilder.constructComponentModel(project.currentPackage());
//        ArchitectureModel expectedModel = TestUtils.getComponentFromGivenJsonFile(expectedJsonPath);
//
//        generatedModel.getServices().forEach((id, service) -> {
//            String generatedService = TestUtils.replaceStdLibVersionStrings(gson.toJson(service)
//                    .replaceAll("\\s+", "")
//                    .replaceAll("\\\\\\\\", "/")
//                    .replaceAll("\"serviceId\": ?\"-?\\d*\"", "\"serviceId\": null"));
//            String expectedService = TestUtils.replaceStdLibVersionStrings(
//                    gson.toJson(expectedModel.getServices().get(id))
//                            .replaceAll("\\s+", "")
//                            .replaceAll("\\{srcPath}", RES_DIR.toString().replaceAll("\\\\", "/"))
//                            .replaceAll("\"serviceId\": ?\"-?\\d*\"", "\"serviceId\": null"));
//            Assert.assertEquals(generatedService, expectedService);
//        });
    }


    public LineRange getLineRange(String filePath, LinePosition startLine, LinePosition endLine) {
        LineRange lineRange = LineRange.from(filePath, startLine, endLine);
        return lineRange;
    }
}
