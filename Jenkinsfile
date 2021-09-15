#!groovy
@Library(['cop-pipeline-bootstrap']) _
loadPipelines('stable/v1.3.x')
loadSharedConfiguration('kratos-bmx-setup')

//Security Groups Test
def sgAppBronzeTest = "sg-0eb8c57f"
def sgInternalAppTrafficTest = "sg-72b1cc03"
def sgsampleInfrastructureTest = "sg-df2ac5ae"
def sgBridgeRouterTrafficTest = "sg-04b7ca75"

def repoUrl = "https://github.com/sample-internal/fulfillment.springbootsampleapp"
def branchName = "${env.BRANCH_NAME}"

//Security Groups Prod
def sgAppBronzeProd = "sg-51227320"
def sgInternalAppTrafficProd = "sg-423d6c33"
def sgsampleInfrastructureProd = "sg-8d3e6ffc"
def sgBridgeRouterTrafficProd = "sg-8b3e6ffa"

def parameterMap = [
        appName          : "springbootsampleapp",
        appDesc          : "Provides Tracking URL details",
        appClassification: "Bronze"
]

def config = [
        profile              : [
                team : "team/marketplace-platforms/kratos/common.groovy",
                app  : "team/marketplace-platforms/kratos/ec2-pipeline-defaults.groovy",
                build: "team/marketplace-platforms/kratos/spring-jdk11-build.groovy",
        ],

        scanAtSource: [
                repository: repoUrl,
                branch: branchName
        ],
        deploymentEnvironment: [
                test: [
                        deploy: [
                                securityGroups : [sgAppBronzeTest, sgsampleInfrastructureTest, sgInternalAppTrafficTest, sgBridgeRouterTrafficTest],
                        ],
                        eureka: [
                                host: 'https://external-eureka-us-east-1.test.commerce.samplecloud.com',
                                name: 'springbootsampleapp'
                        ]
                ],
                prod: [
                        deploy: [
                                securityGroups : [sgAppBronzeProd, sgsampleInfrastructureProd, sgInternalAppTrafficProd, sgBridgeRouterTrafficProd],
                                autoScalingGroupName:  'ship-springbootsampleapp-prod',
                                cloudformationStackName: 'ship-springbootsampleapp-prod',
                                instanceType   : 't3.medium',
                                desiredCapacity: 7,
                                maxSize        : 10,
                                minSize        : 5,
                        ],
                        eureka: [
                                host: 'https://external-eureka-us-east-1.prod.commerce.samplecloud.com',
                                name: 'springbootsampleapp'
                        ]
                ]
        ],
]

node {
    config = mergeConfiguration(config, parameterMap)
}

ec2BlueGreenDeployPipeline(config)
