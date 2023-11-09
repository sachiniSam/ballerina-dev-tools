package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.projects.*;
import io.ballerina.projects.Module;
import io.ballerina.projects.Package;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.Range;

import java.nio.file.Path;

public class ModelGenerator {

    public WorkerModel getWorkerModel(Project project, LineRange position) throws
            Exception {
        Package packageName = project.currentPackage();
        DocumentId docId;
        Document doc;
        if (project.kind().equals(ProjectKind.BUILD_PROJECT)) {
            Path filePath = Path.of(position.fileName());
            docId = project.documentId(filePath);
            ModuleId moduleId = docId.moduleId();
            doc = project.currentPackage().module(moduleId).document(docId);
        } else {
            Module currentModule = packageName.getDefaultModule();
            docId = currentModule.documentIds().iterator().next();
            doc = currentModule.document(docId);
        }

        SyntaxTree syntaxTree = doc.syntaxTree();
        Range range = CommonUtil.toRange(position);
        NonTerminalNode node = CommonUtil.findSTNode(range, syntaxTree);
        if (node.kind() != SyntaxKind.FUNCTION_DEFINITION && node.kind() != SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
            throw new Exception();
        }

        WorkerAnalyzer workerAnalyzer = new WorkerAnalyzer(project, node);
        WorkerModel workerModel =  workerAnalyzer.serializeDependencyGraph();

        return workerModel;
    }
}
