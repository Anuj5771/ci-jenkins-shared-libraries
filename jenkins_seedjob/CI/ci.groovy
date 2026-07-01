def ciJobs = [
    'spring-boot-realworld': [
        url        : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials: 'github-token',
        branch     : 'main',
        scriptPath : 'jenkins_wrapper/spring-boot-realworld/Jenkinsfile'
    ]
]

ciJobs.each { jobName, config ->
    pipelineJob("CI/${jobName}") {
        displayName("${jobName}")
        description("Continuous Integration pipeline for ${jobName}")
        logRotator {
            numToKeep(10)
        }
        parameters {
            stringParam('BRANCH', 'master', 'Branch to build')
            stringParam('ENVIRONMENT', 'dev', 'Environment')
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
