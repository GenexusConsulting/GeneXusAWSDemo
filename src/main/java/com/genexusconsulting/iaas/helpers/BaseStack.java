package com.genexusconsulting.iaas.helpers;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;

public class BaseStack extends Stack {
    protected String project = "";
    protected String databaseEnvironmentPrefix = "";
    protected String datasourcePrefix = "DEFAULT";
    protected String datasourceGAMPrefix = "GAM";

    public BaseStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        if (props != null && props.getTags() != null ){
            project = props.getTags().getOrDefault("user:project", "");
        }
        else {
            throw new RuntimeException("Error: Se debe especificar un nombre de proyecto como TAG. No existe tag usr:project. Es utilizado con fines de organizar costos");
        }
        databaseEnvironmentPrefix = (String)this.getNode().tryGetContext("DATABASE_PREFIX_ENVIRONMENT");
    } 
}
