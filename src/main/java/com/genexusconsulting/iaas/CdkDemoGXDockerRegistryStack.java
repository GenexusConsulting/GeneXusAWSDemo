package com.genexusconsulting.iaas;

import com.genexusconsulting.iaas.helpers.BaseStack;
import com.genexusconsulting.iaas.helpers.Contexto;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ecr.LifecycleRule;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.iam.User;


public class CdkDemoGXDockerRegistryStack extends BaseStack {

    public CdkDemoGXDockerRegistryStack(final Construct scope, final String id, final StackProps props, User user, Contexto contexto) {
        super(scope, id, props);

        Repository repository =  Repository.Builder.create(this,project+"_repository")
            .repositoryName(project)
            .imageScanOnPush(false)
            .removalPolicy(RemovalPolicy.DESTROY)
            .build();

        // grant permissions to user to push and pull images.
        repository.grantPullPush(user);

        // up to 5 images
        LifecycleRule lifecycleRule = LifecycleRule.builder()
            .maxImageCount(5)
            .build();
        repository.addLifecycleRule(lifecycleRule);
    }
}
