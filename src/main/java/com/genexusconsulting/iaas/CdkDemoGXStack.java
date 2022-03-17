package com.genexusconsulting.iaas;

import java.util.ArrayList;
import java.util.List;

import com.genexusconsulting.iaas.helpers.BaseStack;
import com.genexusconsulting.iaas.helpers.Contexto;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.iam.User;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;

public class CdkDemoGXStack extends BaseStack {
    public Vpc vpc;
    public User user;
    public Certificate certificate;

    public CdkDemoGXStack(final Construct scope, final String id, final StackProps props, Contexto contexto) {
        super(scope, id, props); 
        // Virtual Private Cloud Creation
        vpc = Vpc.Builder.create(this, project + "_vpc")
            .cidr("192.168.0.0/16")
            .maxAzs(2) // Amazon RDS needs at least 2 AZs
            .subnetConfiguration(subnetConfigurations()) 
            .build();

        // User for docker registry and ECR management
        // Passwordless, an authorization token must be issued manually 
        // to be used by CDK
        String ecrUser = project+"_ecr";
        user = User.Builder.create(this,ecrUser)
            .userName(ecrUser)
            .build();
        certificate = Certificate.Builder.create(this, project + "_certificate")
            .domainName(contexto.getFQDN())
            .validation(CertificateValidation.fromDns())
            .build(); 
    }

    private List<? extends SubnetConfiguration> subnetConfigurations() {
        List<SubnetConfiguration> confs = new ArrayList<>();
        SubnetConfiguration configuration = SubnetConfiguration.builder()
            .cidrMask(24)
            .name("ingress")
            .subnetType(SubnetType.PUBLIC)
            .build();
        confs.add(configuration);
        return confs;
    }
}