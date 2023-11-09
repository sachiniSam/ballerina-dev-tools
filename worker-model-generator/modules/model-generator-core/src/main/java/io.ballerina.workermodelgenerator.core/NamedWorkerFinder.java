package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.syntax.tree.NamedWorkerDeclarationNode;
import io.ballerina.compiler.syntax.tree.NodeVisitor;

import java.util.ArrayList;
import java.util.List;

public class NamedWorkerFinder extends NodeVisitor {

    List<NamedWorkerDeclarationNode> namedWorkers = new ArrayList<>();

    public List<NamedWorkerDeclarationNode> getNamedWorkers() {

        return namedWorkers;
    }

    @Override
    public void visit(NamedWorkerDeclarationNode namedWorkerDeclarationNode) {
        namedWorkers.add(namedWorkerDeclarationNode);

//        if (functionDefinitionNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
//            StringBuilder name = new StringBuilder();
//            for (Node node : functionDefinitionNode.relativeResourcePath()) {
//                name.append(node.toString());
//            }
//            resources.add(new Resource(name.toString(), functionDefinitionNode.lineRange()));
//        }
    }
}
