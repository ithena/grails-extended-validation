package be.ixor.grails.extendedvalidation;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import static org.springframework.asm.Opcodes.ACC_FINAL;
import static org.springframework.asm.Opcodes.ACC_PUBLIC;

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
public class ValidateableASTTransformation implements ASTTransformation {

    public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        if (nodes == null || nodes.length < 2) {
            // DO nothing
        } else {

            AnnotationNode annotation = (AnnotationNode) nodes[0];
            AnnotatedNode annotatedNode = (AnnotatedNode) nodes[1];

            if (annotatedNode instanceof ClassNode) {
                ClassNode cNode = (ClassNode) annotatedNode;
                addSerializableIdentityHashCodeField(cNode);
            }
        }
    }

    void addSerializableIdentityHashCodeField(ClassNode cNode) {
        FieldNode serializableIdentityHashCodeField = new FieldNode(
                "serializableIdentityHashCode",
                ACC_PUBLIC | ACC_FINAL,
                new ClassNode(String.class),
                new ClassNode(cNode.getClass()),
                createGenerateIdMethodCall());

        cNode.addField(serializableIdentityHashCodeField);
    }

    Expression createGenerateIdMethodCall() {
        return new MethodCallExpression(
                new ClassExpression(new ClassNode(System.class)),
                "identityHashCode",
                VariableExpression.THIS_EXPRESSION);
    }
}
