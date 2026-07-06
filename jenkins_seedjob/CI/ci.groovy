def ciJobs = [
    'spring-boot-realworld': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CI/spring-boot-realworld/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
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
        logRotatorNum: 5, // keep only last 5 builds' history
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