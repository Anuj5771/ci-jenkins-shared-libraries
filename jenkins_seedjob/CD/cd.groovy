def cdJobs = [
    'spring-boot-realworld': [
        url        : 'https://github.com/priyanshu499-ops/ci-jenkins-shared-libraries.git',
        credentials: 'github-token',
        branch     : 'master',
        scriptPath : 'jenkins_wrapper/spring-boot-realworld/Jenkinsfile'
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
