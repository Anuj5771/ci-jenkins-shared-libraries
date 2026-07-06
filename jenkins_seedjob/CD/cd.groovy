def cdJobs = [
    'spring-boot-realworld': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/spring-boot-realworld/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ],
    'simple-nodejs-app': [
        url          : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials  : 'github-token',
        branch       : 'main',
        scriptPath   : 'jenkins_wrapper/CD/simple-nodejs-app/Jenkinsfile',
        owner        : 'priyanshu499-ops',
        logRotatorNum: 5, // keep only last 5 builds' history
        parameters   : [
            [name: 'BRANCH', defaultValue: 'main', description: 'Branch to build'],
            [name: 'image_tag', defaultValue: 'latest', description: 'Docker image tag to deploy']
        ]
    ]
]

cdJobs.each { jobName, config ->
    pipelineJob("CD/${jobName}") {
        displayName("${jobName}")
        description("CD pipeline for ${jobName} | Owner: ${config.owner}")
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