def ciJobs = [
    'spring-boot-realworld': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/spring-boot-realworld/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'simple-nodejs-app': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/simple-nodejs-app/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'auth-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/auth-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'instacard-mock-apis': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/instacard-mock-apis/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'instacard-user-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/instacard-user-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'virtualcard-service': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/virtualcard-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'sdk-instacard-frontend': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/sdk-instacard-frontend/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
       ]
    ],
    'design-ui-framework': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/design-ui-framework/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'apigateway': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/apigateway/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'montra-bom': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/montra-bom/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ],
    'salary-service': [
        url          : 'https://github.com/Anuj5771/ci-jenkins-shared-libraries.git',
        credentials  : '',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/salary-service/Jenkinsfile',
        owner        : 'CI-CD Team',
        logRotatorNum:  5,
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'ENVIRONMENT', defaultValue: 'dev', description: 'Environment']
        ]
    ]
]

ciJobs.each { jobName, config ->
    pipelineJob("CI/${jobName}") {
        displayName("${jobName}")
        description("CI pipeline for ${jobName} | Owner: ${config.owner}")
        logRotator {
            numToKeep(config.logRotatorNum)
        }
        parameters {
            config.parameters.each { param ->
                stringParam(param.name, param.defaultValue, param.description)
            }
        }
        definition {
            cpsScm {
                scm {
                    git {
                        remote {
                            url(config.url)
                            credentials(config.credentials)
                        }
                        branch(config.branch)
                    }
                }
                scriptPath(config.scriptPath)
                lightweight(true)
            }
        }
    }
}
