package com.github.xuzw.object_builder_generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.AssignExpr.Operator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * @author 徐泽威 xuzewei_2012@126.com
 * @time 2017年4月9日 下午5:17:24
 */
public class ObjectBuilderGenerator {
    private Charset encoding;
    private String sourceJavaFilePath;

    public Charset getEncoding() {
        return encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = encoding;
    }

    public String getSourceJavaFilePath() {
        return sourceJavaFilePath;
    }

    public void setSourceJavaFilePath(String sourceJavaFilePath) {
        this.sourceJavaFilePath = sourceJavaFilePath;
    }

    private String _getFieldSetter(String fieldName) {
        return "set" + StringUtils.capitalize(fieldName);
    }

    private String _getTargetJavaFilePath() {
        return sourceJavaFilePath.replaceFirst(".java$", "") + "Builder.java";
    }

    public void generate() throws IOException {
        CompilationUnit source = JavaParser.parse(new File(sourceJavaFilePath), encoding);
        CompilationUnit target = new CompilationUnit();
        target.setPackageDeclaration(source.getPackageDeclaration().get());
        for (ImportDeclaration importDeclaration : source.getImports()) {
            target.addImport(importDeclaration);
        }
        String sourceClassName = source.getTypes().get(0).getName().toString();
        String targetClassName = sourceClassName + "Builder";
        ClassOrInterfaceDeclaration classDeclaration = target.addClass(targetClassName);
        classDeclaration.addPrivateField(sourceClassName, "obj");
        ConstructorDeclaration constructorDeclaration = classDeclaration.addConstructor(Modifier.PUBLIC);
        constructorDeclaration.setBody(new BlockStmt().addStatement(new AssignExpr(new NameExpr("obj"), new ObjectCreationExpr().setType(new ClassOrInterfaceType(sourceClassName)), Operator.ASSIGN)));
        source.accept(new VoidVisitorAdapter<Object>() {
            @Override
            public void visit(FieldDeclaration n, Object arg) {
                String fieldName = n.getVariables().get(0).toString();
                MethodDeclaration methodDeclaration = classDeclaration.addMethod(fieldName, Modifier.PUBLIC);
                methodDeclaration.setType(targetClassName);
                methodDeclaration.addParameter(new Parameter(n.getElementType(), fieldName));
                BlockStmt blockStmt = new BlockStmt();
                NodeList<Expression> list = new NodeList<>();
                list.add(new NameExpr(fieldName));
                blockStmt.addStatement(new MethodCallExpr(new NameExpr("obj"), new SimpleName(_getFieldSetter(fieldName)), list));
                blockStmt.addStatement(new ReturnStmt(new ThisExpr()));
                methodDeclaration.setBody(blockStmt);
            }
        }, null);
        MethodDeclaration buildMethodDeclaration = classDeclaration.addMethod("build", Modifier.PUBLIC);
        buildMethodDeclaration.setType(sourceClassName);
        buildMethodDeclaration.setBody(new BlockStmt().addStatement(new ReturnStmt(new NameExpr("obj"))));
        OutputStream output = new FileOutputStream(_getTargetJavaFilePath());
        IOUtils.write(target.toString(), output, encoding);
        IOUtils.closeQuietly(output);
    }
}
