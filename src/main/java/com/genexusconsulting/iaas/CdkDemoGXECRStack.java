package com.genexusconsulting.iaas;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.genexusconsulting.iaas.helpers.BaseStack;
import com.genexusconsulting.iaas.helpers.Contexto;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.Secret;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedServiceRecordType;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.ApplicationProtocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.Policy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.iam.User;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.secretsmanager.ISecret;

public class CdkDemoGXECRStack extends BaseStack {

    public CdkDemoGXECRStack(final Construct scope, final String id, final StackProps props, Contexto contexto, Vpc vpc, User user, 
                                DatabaseInstance dbi, ISecret secret, Certificate certificate) {
        super(scope, id, props);

        // Crear un cluster ECR        
        Cluster cluster = Cluster.Builder.create(this, project+"_cluster")
            .vpc(vpc)
            .clusterName(project+"_cluster")
            .build();  

        // Password de la base de datos "GX_"
		Map<String,Secret> secretos = new HashMap<String,Secret>();
		secretos.put("GX_" +databaseEnvironmentPrefix + "_"+ datasourcePrefix +"_USER_PASSWORD",Secret.fromSecretsManager(secret));
        secretos.put("GX_" +databaseEnvironmentPrefix + "_"+ datasourceGAMPrefix +"_USER_PASSWORD",Secret.fromSecretsManager(secret));
		
        //Connection String
        // Esto es un ejemplo de config de DB de GX.
        // export GX_********_DEFAULT_USER_ID=*****
        // export GX_*****_DEFAULT_DB_URL=jdbc:postgresql://XX.XX.XX.XX:PORT/database
		Map<String,String> ambiente = new HashMap<String, String>();
		ambiente.put("GX_" +databaseEnvironmentPrefix + "_"+ datasourcePrefix +"_USER_ID", contexto.getDB_USER_ID() );
		ambiente.put("GX_" +databaseEnvironmentPrefix + "_"+ datasourcePrefix +"_DB_URL", contexto.getDB_URL() );

        ambiente.put("GX_" +databaseEnvironmentPrefix + "_"+ datasourceGAMPrefix +"_USER_ID", contexto.getDB_USER_ID() );
		ambiente.put("GX_" +databaseEnvironmentPrefix + "_"+ datasourceGAMPrefix +"_DB_URL", contexto.getDB_URL());

        // repositorio con las imagenes
        IRepository repository =  Repository.fromRepositoryName(this,project+"_repository" , project);

        // esto es para hacer que el log no sea de por vida
        LogDriver log = LogDriver.awsLogs(
            AwsLogDriverProps.builder()
            .logGroup(
                LogGroup.Builder.create(this, project)
                .retention(RetentionDays.SIX_MONTHS)
                .build())
            .streamPrefix(project)
            .build());

        // Hay que hacer esto para que no de el problema de permisos
        // https://aws.amazon.com/premiumsupport/knowledge-center/ecs-tasks-pull-images-ecr-repository/
        //  Stopped reason ResourceInitializationError: unable to pull secrets or registry auth: pull command failed: : signal: killed
        Role ecsTaskExecutionRole = Role.Builder.create(this,"FargateContainerRole")
            .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
            .build();

        ApplicationLoadBalancedFargateService applicationLoadBalancedFargateService = ApplicationLoadBalancedFargateService.Builder.create(this, project +"_service")
            .cluster(cluster)
            .desiredCount(1)
            .taskImageOptions(
                ApplicationLoadBalancedTaskImageOptions.builder()
                    .image(ContainerImage.fromEcrRepository(repository))
                    .containerName(project + "_web_container")
                    .secrets(secretos)
                    .environment(ambiente)
                    .taskRole(ecsTaskExecutionRole)
                    .executionRole(ecsTaskExecutionRole)
                    .containerPort(8080)
                    .build()
            )
            .cpu(256)
            .memoryLimitMiB(512)
            .publicLoadBalancer(true)
            .serviceName(project + "_service")
            .assignPublicIp(true)
            .listenerPort(80)
            .protocol(ApplicationProtocol.HTTPS)
            .certificate(certificate)
            .redirectHttp(true)
            .recordType(ApplicationLoadBalancedServiceRecordType.ALIAS)
            .build();
		
        applicationLoadBalancedFargateService.getTargetGroup().configureHealthCheck(HealthCheck.builder()
            .path("/demogx/com.gxdemo.wwenti")
            .interval(Duration.seconds(60))
            .healthyThresholdCount(3)
            .timeout(Duration.seconds(15))
            .build()
            );

        // policy para poder actualizar el servicio con la nueva version con el comando 
        // aws ecs update-service --cluster DemoGX_cluster --service DemoGX_service --force-new-deployment  
        Policy policy = Policy.Builder.create(this, project+"_ecr_policy")
            .statements( 
                Arrays.asList(
                    PolicyStatement.Builder.create()
                    .actions(Arrays.asList("ecs:UpdateService"))
                    .effect(Effect.ALLOW)
                    .resources(Arrays.asList(applicationLoadBalancedFargateService.getService().getServiceArn()))
                    .build()
                )
            )
        .build();
        user.attachInlinePolicy(policy);
    }
}
