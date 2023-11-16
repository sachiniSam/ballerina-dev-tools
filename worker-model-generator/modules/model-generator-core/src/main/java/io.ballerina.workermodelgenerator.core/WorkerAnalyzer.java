package io.ballerina.workermodelgenerator.core;

import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.TypeBuilder;
import io.ballerina.compiler.api.symbols.TupleTypeSymbol;
import io.ballerina.compiler.api.symbols.TypeDescKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.*;
import io.ballerina.projects.DependencyGraph;
import io.ballerina.projects.Project;

import java.util.*;

public class WorkerAnalyzer {

    private Project project;
    private NonTerminalNode functionDefNode;
    private SemanticModel semanticModel;
    // TODO: Remove this when we remove the graph builder logic
    private Map<String, LinkMetaData> linkMetaData;

    public WorkerAnalyzer(Project project, NonTerminalNode functionDefNode, SemanticModel semanticModel) {
        this.project = project;
        this.functionDefNode = functionDefNode;
        this.semanticModel = semanticModel;
        this.linkMetaData = new HashMap<>();
    }

    public static final String DATAFLOW_GRAPH_DOT_FILENAME = "dataflow_graph.dot";
    private static final String FUNC_START_NODE = "FunctionStart";
    private static final String FUNC_END_NODE = "FunctionEnd";
    private static final String FUNC_NODE = "function";


    public WorkerModel serializeDependencyGraph() throws Exception {
        DependencyGraph.DependencyGraphBuilder<String> graphBuilder = DependencyGraph.DependencyGraphBuilder.getBuilder(FUNC_START_NODE);
//        NamedWorkerDeclarator namedWorkerDeclarator = (NamedWorkerDeclarator) workerNode;

//        NonTerminalNode inputNode;
//        if (functionDefNode.kind() == SyntaxKind.FUNCTION_DEFINITION) {
//            inputNode = (FunctionDefinitionNode) functionDefNode;
//        } else if (functionDefNode.kind() == SyntaxKind.RESOURCE_ACCESSOR_DEFINITION) {
//            inputNode = (ResourceA) functionDefNode;
//        } else {
//            throw new Exception();
//        }
        FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) functionDefNode;
        NamedWorkerFinder namedWorkerFinder = new NamedWorkerFinder();
        functionDefinitionNode.accept(namedWorkerFinder);
        List<NamedWorkerDeclarationNode> namedWorkers = namedWorkerFinder.getNamedWorkers();
        for (NamedWorkerDeclarationNode namedWorkerDeclaration : namedWorkers) {
            String curWorkerName = namedWorkerDeclaration.workerName().text();
            graphBuilder.add(curWorkerName);
            BlockStatementNode blockStatementNode = namedWorkerDeclaration.workerBody();
            for (StatementNode statement : blockStatementNode.statements()) {
                if (statement instanceof ExpressionStatementNode) {
                    // This function processes ASYNC_SEND_ACTION and SYNC_SEND_ACTION nodes
                    processExpressionStmtNode(statement, curWorkerName, graphBuilder);
                } else if (statement instanceof VariableDeclarationNode) {
                    // This function processes RECEIVE_ACTION nodes
                    processVarDeclarationNode(statement, curWorkerName, graphBuilder);
                }
            }
        }

        // Create the dependency graph
        DependencyGraph<String> dependencyGraph = graphBuilder.build();
        DependencyGraph.DependencyGraphBuilder<String> newGraphBuilder = DependencyGraph.DependencyGraphBuilder.getBuilder(FUNC_START_NODE);
        newGraphBuilder.mergeGraph(dependencyGraph);

        // Find out nodes that doesn't have any dependents and add function as a dependent.
        for (String node : dependencyGraph.getNodes()) {
            if (node.equals(FUNC_START_NODE)) {
                continue;
            }
            if (dependencyGraph.getDirectDependents(node).isEmpty()) {
                newGraphBuilder.addDependency(FUNC_START_NODE, node);
            }
        }

        dependencyGraph = newGraphBuilder.build();
        DotGraphSerializer dotGraphSerializer = new DotGraphSerializer();
        WorkerModel workerModel = new WorkerModel();
        for (String node : dependencyGraph.getNodes()) {
            WorkerNode workerNode = new WorkerNode(node);
            for (String directDependency : dependencyGraph.getDirectDependencies(node)) {
                // dotGraphSerializer.addEdge(node, directDependency);
                Link link = new Link(directDependency);
                if (linkMetaData.containsKey(directDependency)) {
                    LinkMetaData linkMetaDataObj = linkMetaData.get(directDependency);
                    if (linkMetaDataObj.getFromWorker().equals(node)) {
                        link.setVarName(linkMetaDataObj.getVarName());
                        link.setType(linkMetaDataObj.getVarType());
                    }
//                    link.setVarName(linkMetaDataObj.getVarName());
//                    link.setType(linkMetaDataObj.getVarType());
                }
                workerNode.addLink(link);
            }

            workerModel.addNode(workerNode);
        }





//        String fileNamePrefix = getFileNamePrefix(functionDefinitionNode);
//        String serializedDotGraph = dotGraphSerializer.toString();
//        Path packageRootPath = ProjectPaths.packageRoot(project.currentPackage().project().sourceRoot());
//        Path graphFilePath = packageRootPath.resolve(fileNamePrefix + "-" + DATAFLOW_GRAPH_DOT_FILENAME);
//        if (!Files.exists(graphFilePath)) {
//            Files.createFile(graphFilePath);
//        }
//        Files.writeString(graphFilePath, serializedDotGraph, StandardOpenOption.TRUNCATE_EXISTING);

        return workerModel;
    }

    private void processExpressionStmtNode(StatementNode statement,
                                           String curWorkerName,
                                           DependencyGraph.DependencyGraphBuilder<String> graphBuilder) {

        String toWorker;
        String varName = "";
        String typeName = "";
        ExpressionNode expression = ((ExpressionStatementNode) statement).expression();
        if (expression.kind() == SyntaxKind.ASYNC_SEND_ACTION) {
            AsyncSendActionNode sendActionNode = (AsyncSendActionNode) expression;
            // Get the details about varName and Type
            ExpressionNode sendExpression = sendActionNode.expression();
            if(sendActionNode.expression().kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                SimpleNameReferenceNode simpleNameReferenceNode = (SimpleNameReferenceNode) sendActionNode.expression();
                varName = simpleNameReferenceNode.name().text().trim();
            } else if (sendExpression.kind() == SyntaxKind.NUMERIC_LITERAL) {
                BasicLiteralNode literalNode = (BasicLiteralNode) sendExpression;
                varName = literalNode.literalToken().text().trim();
            } else {
                varName = sendExpression.toString().trim();
            }
            
            Optional<TypeSymbol> expressionType = semanticModel.typeOf(sendExpression);
            if (expressionType.isPresent()) {
                TypeSymbol typeSymbol = expressionType.get();
                if (typeSymbol.typeKind() == TypeDescKind.TUPLE) {
                    TupleTypeSymbol tupleTypeSymbol = (TupleTypeSymbol) typeSymbol;
                    List<TypeSymbol> memberTypes = tupleTypeSymbol.memberTypeDescriptors();
                    StringBuilder typeNameBuilder = new StringBuilder("[");
                    for (TypeSymbol memberType : memberTypes) {
                        if (memberType.getName().isPresent()) {
                            typeNameBuilder.append(memberType.getName().get()).append(",");
                        }
                    }
                    if (typeNameBuilder.length() > 1) {
                        typeNameBuilder.setLength(typeNameBuilder.length() - 1); // remove last comma
                    }
                    typeNameBuilder.append("]");
                    typeName = typeNameBuilder.toString();
                } else if (typeSymbol.getName().isPresent()) {
                    typeName = typeSymbol.getName().get();
                }

            }
            toWorker = sendActionNode.peerWorker().name().text();
        } else if (expression.kind() == SyntaxKind.SYNC_SEND_ACTION) {
            SyncSendActionNode sendActionNode = (SyncSendActionNode) expression;

            // Get the details about type and name
            ExpressionNode sendExpression = sendActionNode.expression();
            if(sendActionNode.expression().kind() == SyntaxKind.SIMPLE_NAME_REFERENCE) {
                SimpleNameReferenceNode simpleNameReferenceNode = (SimpleNameReferenceNode) sendActionNode.expression();
                varName = simpleNameReferenceNode.name().text().trim();
            } else if (sendExpression.kind() == SyntaxKind.NUMERIC_LITERAL) {
                BasicLiteralNode literalNode = (BasicLiteralNode) sendExpression;
                varName = literalNode.literalToken().text().trim();
            } else {
                varName = sendExpression.toString().trim();
            }

            Optional<TypeSymbol> expressionType = semanticModel.typeOf(sendExpression);
            if (expressionType.isPresent()) {
                TypeSymbol typeSymbol = expressionType.get();
                if (typeSymbol.typeKind() == TypeDescKind.TUPLE) {
                    TupleTypeSymbol tupleTypeSymbol = (TupleTypeSymbol) typeSymbol;
                    List<TypeSymbol> memberTypes = tupleTypeSymbol.memberTypeDescriptors();
                    StringBuilder typeNameBuilder = new StringBuilder("[");
                    for (TypeSymbol memberType : memberTypes) {
                        if (memberType.getName().isPresent()) {
                            typeNameBuilder.append(memberType.getName().get()).append(",");
                        }
                    }
                    if (typeNameBuilder.length() > 1) {
                        typeNameBuilder.setLength(typeNameBuilder.length() - 1); // remove last comma
                    }
                    typeNameBuilder.append("]");
                    typeName = typeNameBuilder.toString();
                } else if (typeSymbol.getName().isPresent()) {
                    typeName = typeSymbol.getName().get();
                }

            }



            toWorker = sendActionNode.peerWorker().name().text();
        } else {
            return;
        }

        addSendDependency(curWorkerName, toWorker, graphBuilder, varName, typeName);
    }

    private void processVarDeclarationNode(StatementNode statement,
                                           String curWorkerName,
                                           DependencyGraph.DependencyGraphBuilder<String> graphBuilder) {
        VariableDeclarationNode varDclNode = (VariableDeclarationNode) statement;
        if (varDclNode.initializer().isEmpty()) {
            return;
        }

        ExpressionNode initializer = varDclNode.initializer().get();
        if (initializer.kind() == SyntaxKind.CHECK_ACTION &&
                ((CheckExpressionNode) initializer).expression().kind() == SyntaxKind.RECEIVE_ACTION) {
            CheckExpressionNode checkExpressionNode = (CheckExpressionNode) initializer;
            processReceiveActionNode((ReceiveActionNode) checkExpressionNode.expression(),
                    curWorkerName, graphBuilder);
        } else if (initializer.kind() == SyntaxKind.RECEIVE_ACTION) {
            processReceiveActionNode((ReceiveActionNode) initializer, curWorkerName, graphBuilder);
        } else if (initializer.kind() == SyntaxKind.WAIT_ACTION) {
            processWaitActionNode((WaitActionNode) initializer, curWorkerName, graphBuilder);
        } else if (initializer.kind() == SyntaxKind.CHECK_ACTION && ((CheckExpressionNode) initializer).expression().kind() == SyntaxKind.WAIT_ACTION) {
            CheckExpressionNode checkExpressionNode = (CheckExpressionNode) initializer;
            processWaitActionNode((WaitActionNode) checkExpressionNode.expression(), curWorkerName, graphBuilder);
        }
    }


    private String getFileNamePrefix(FunctionDefinitionNode funcDefNode) {
        // FunctionDefinitionNode funcDefNode = (FunctionDefinitionNode) namedWorkerDeclarator.parent().parent();

        StringJoiner joiner = new StringJoiner("-");

        if (funcDefNode.parent() instanceof ServiceDeclarationNode) {
            ServiceDeclarationNode svcDeclNode = (ServiceDeclarationNode) funcDefNode.parent();
            String svcName = convertNodeListToFileNamePart(svcDeclNode.absoluteResourcePath());
            if (!svcName.isEmpty()) {
                joiner.add(svcName);
            }
        }
        String funcName = funcDefNode.functionName().toSourceCode();
        joiner.add(convertToFileNamePart(funcName).trim());
        joiner.add(convertNodeListToFileNamePart(funcDefNode.relativeResourcePath()));
        return joiner.toString();
    }

    private String convertNodeListToFileNamePart(NodeList<Node> nodeList) {
        StringJoiner joiner = new StringJoiner("-");
        for (Node node : nodeList) {
            String path = node.toSourceCode().trim();
            if (path.equals("/")) {
                continue;
            }
            path = convertToFileNamePart(path);
            joiner.add(path);
        }

        return joiner.toString();
    }

    private String convertToFileNamePart(String part) {
        part = part.replace("[", "");
        part = part.replace("]", "");
        part = part.replace(" ", "");
        return part;
    }


    private void processWaitActionNode(WaitActionNode waitActionNode, String curWorkerName,
                                       DependencyGraph.DependencyGraphBuilder<String> graphBuilder) {
        Node exprNode = waitActionNode.waitFutureExpr();
        if (exprNode instanceof WaitFieldsListNode) {
            WaitFieldsListNode waitFieldsListNode = (WaitFieldsListNode) exprNode;
            for (Node waitField : waitFieldsListNode.waitFields()) {
                if (waitField instanceof WaitFieldNode) {
                    WaitFieldNode waitFieldNode = (WaitFieldNode) waitField;
                    String fromWorker = waitFieldNode.waitFutureExpr().toSourceCode().trim();
                    addReceiveDependency(fromWorker, curWorkerName, graphBuilder);
                } else if (waitField instanceof SimpleNameReferenceNode) {
                    SimpleNameReferenceNode simpleNameReferenceNode = (SimpleNameReferenceNode) waitField;
                    addReceiveDependency(simpleNameReferenceNode.toSourceCode().trim(), curWorkerName, graphBuilder);
                }
            }
        } else if (exprNode instanceof BinaryExpressionNode) {
            BinaryExpressionNode binaryExpressionNode = (BinaryExpressionNode) exprNode;
            processWaitActionBinaryExprNode(binaryExpressionNode, curWorkerName, graphBuilder);
        }
    }

    private void processWaitActionBinaryExprNode(BinaryExpressionNode binaryExpressionNode, String curWorkerName,
                                                 DependencyGraph.DependencyGraphBuilder<String> graphBuilder) {
        if (binaryExpressionNode.lhsExpr() instanceof BinaryExpressionNode ) {
            BinaryExpressionNode lhsBinaryExpressionNode = (BinaryExpressionNode) binaryExpressionNode.lhsExpr();
            processWaitActionBinaryExprNode(lhsBinaryExpressionNode, curWorkerName, graphBuilder);
        }

        if (binaryExpressionNode.rhsExpr() instanceof BinaryExpressionNode ) {
            BinaryExpressionNode rhsBinaryExpressionNode = (BinaryExpressionNode) binaryExpressionNode.rhsExpr();
            processWaitActionBinaryExprNode(rhsBinaryExpressionNode, curWorkerName, graphBuilder);
        }

        if (binaryExpressionNode.lhsExpr() instanceof SimpleNameReferenceNode ) {
            SimpleNameReferenceNode simpleNameReferenceNode = (SimpleNameReferenceNode) binaryExpressionNode.lhsExpr();
            addReceiveDependency(simpleNameReferenceNode.toSourceCode().trim(), curWorkerName, graphBuilder);
        }

        if (binaryExpressionNode.rhsExpr() instanceof SimpleNameReferenceNode ) {
            SimpleNameReferenceNode simpleNameReferenceNode = (SimpleNameReferenceNode) binaryExpressionNode.rhsExpr();
            addReceiveDependency(simpleNameReferenceNode.toSourceCode().trim(), curWorkerName, graphBuilder);
        }
    }

    private void processReceiveActionNode(ReceiveActionNode receiveActionNode,
                                          String curWorkerName,
                                          DependencyGraph.DependencyGraphBuilder<String> graphBuilder) {
        Node receiveWorker = receiveActionNode.receiveWorkers();
        if (receiveWorker instanceof SimpleNameReferenceNode) {
            String fromWorker = ((SimpleNameReferenceNode) receiveWorker).name().text();
            addReceiveDependency(fromWorker, curWorkerName, graphBuilder);
        }
    }

    private void addSendDependency(String fromWorker,
                                   String toWorker,
                                   DependencyGraph.DependencyGraphBuilder<String> graphBuilder, String varName, String typeName) {
        String newToWorker = toWorker;
        if (FUNC_NODE.equals(toWorker)) {
            newToWorker = FUNC_END_NODE;
        }
        graphBuilder.addDependency(fromWorker, newToWorker);
        linkMetaData.put(newToWorker, new LinkMetaData(varName, typeName, fromWorker));

    }

    private void addReceiveDependency(String fromWorker,
                                      String toWorker,
                                      DependencyGraph.DependencyGraphBuilder<String> graphBuilder) {
        String newFromWorker = fromWorker;
        if (FUNC_NODE.equals(fromWorker)) {
            newFromWorker = FUNC_START_NODE;
        }
        graphBuilder.addDependency(newFromWorker, toWorker);
    }
}
