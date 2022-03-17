package com.genexusconsulting.iaas;

import java.util.HashMap;
import java.util.Map;

import com.genexusconsulting.iaas.helpers.Contexto;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.StackProps;

public class CdkDemoGXApp {
    public static void main(final String[] args) {
        String project="demogx";
        Contexto contexto = new Contexto();
        contexto.setDB_USER_ID(project);
        contexto.setDATABASE_NAME(project);
        contexto.setFQDN("demogx.example.com");
        App app = new App();

        Map<String, String> tags = new HashMap<>();
        tags.putIfAbsent("user:project", project);
        StackProps stackProps = StackProps.builder()
                                .tags(tags)
                                .build();
        CdkDemoGXStack stack = new CdkDemoGXStack(app, "CdkDemoGXStack", stackProps, contexto );
        CdkDemoGXDatabaseStack databaseStack = new CdkDemoGXDatabaseStack(app, "CdkDemoGXDatabaseStack", stackProps,contexto,stack.vpc);
        new CdkDemoGXDockerRegistryStack(app, "CdkDemoGXDockerRegistryStack", stackProps,stack.user,contexto );
        new CdkDemoGXECRStack(app, "CdkDemoGXECRStack", stackProps,contexto,stack.vpc,stack.user,databaseStack.database,databaseStack.secret, stack.certificate);
        app.synth();
    }
}
