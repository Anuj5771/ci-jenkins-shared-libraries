def cdJobs = [
    'spring-boot-realworld': [
        url        : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials: 'github-token',
        branch     : 'main',
        scriptPath : 'jenkins_wrapper/CD/spring-boot-realworld/Jenkinsfile'
    ]
    'simple-nodejs-app': [
        url        : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials: 'github-token',
        branch     : 'main',
        scriptPath : 'jenkins_wrapper/CD/spring-boot-realworld/Jenkinsfile'
    ]
]

cdJobs.each { jobName, config ->
    pipelineJob("CD/${jobName}") {
        displayName("${jobName}")
        description("Continuous Integration pipeline for ${jobName}")
        logRotator {
            numToKeep(10)
        }
        parameters {
        stringParam('BRANCH', config.branch, 'Branch to build')
        stringParam('image_tag', 'latest', 'Docker image tag to deploy')
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
