package com.genexusconsulting.iaas;

import java.util.Arrays;

import com.genexusconsulting.iaas.helpers.BaseStack;
import com.genexusconsulting.iaas.helpers.Contexto;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.PostgresEngineVersion;
import software.amazon.awscdk.services.rds.PostgresInstanceEngineProps;
import software.amazon.awscdk.services.secretsmanager.Secret;
import software.amazon.awscdk.services.secretsmanager.SecretStringGenerator;

public class CdkDemoGXDatabaseStack extends BaseStack {
    public DatabaseInstance database;
    public Secret secret;

    public CdkDemoGXDatabaseStack(final Construct scope, final String id, final StackProps props, Contexto contexto, Vpc vpc) {
        super(scope, id, props);

        // Para conectarse desde internet a la base de datos hay que generar una regla 
        SecurityGroup securityGroup = SecurityGroup.Builder.create(this, project+ "_security_group_postgres")
            .securityGroupName(project+ "_security_group_postgres")
            .vpc(vpc)
            .allowAllOutbound(false)
            .build();
        int databasePort = 5432;
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(databasePort),
            project + "_security_group_postgres allow incoming connections to the database");

        // Secreto: Usuario de Base de datos
        // tiene en cuenta el formato que acepta genexus. El secreto en formato AWS Database no es práctico
        // De utilizarlo, hay que modificar las variables de entorno en la imagen Docker.
        generateSecret();

        database =  DatabaseInstance.Builder.create(this, project+"_database_postgres")
            .instanceIdentifier(project+"-database-postgres")
            .databaseName(project)
            .credentials(Credentials.fromPassword(contexto.getDB_USER_ID(), secret.getSecretValue()) )
            .engine(DatabaseInstanceEngine.postgres(getpostgresInstanceEngineProps()))
            .vpc(vpc)
            .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
            .removalPolicy(RemovalPolicy.DESTROY)
            .deletionProtection(false)
            .securityGroups(Arrays.asList(securityGroup))
            .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PUBLIC).build())
            .autoMinorVersionUpgrade(true)
            .backupRetention(software.amazon.awscdk.core.Duration.days(0))
            .build();

        contexto.setDB_URL(getDBUrl(contexto));

    }

    private Secret generateSecret() {
        return secret = Secret.Builder.create(this, project + "_secret")
            .secretName(project + "_secret")
            .description("Database Password for "+ project)
            .generateSecretString( getDatabaseSecretStringGenerator4GeneXus())
            .build();
    }

    /**
     * 
     * @return un password de base de datos como lo pide AWS y compatible 
     * con la forma que GX espera las variables de entorno
     */
    private SecretStringGenerator getDatabaseSecretStringGenerator4GeneXus() {
		return SecretStringGenerator.builder().excludePunctuation(true).includeSpace(false).build();
	}

    /**
     * 
     * @param contexto
     * @return Connection String para la base de datos PostgreSQL
     */
	private String getDBUrl(Contexto contexto) {
        return "jdbc:postgresql://"+ database.getDbInstanceEndpointAddress()+":"+ database.getDbInstanceEndpointPort()
        +"/"+ contexto.getDB_DATABASE_NAME();
    }

	private PostgresInstanceEngineProps getpostgresInstanceEngineProps() {
		return PostgresInstanceEngineProps.builder()
            .version(PostgresEngineVersion.VER_12_5)
            .build();
	}
}
