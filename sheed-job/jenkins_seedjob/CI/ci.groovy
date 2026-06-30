def ciJobs = [
    'spring-boot-realworld': [
        url        : 'https://github.com/priyanshu499-ops/spring-boot-realworld-example-app.git',
        credentials: 'github-token',
        branch     : 'master',
        scriptPath : 'Jenkinsfile'
    ]
]

ciJobs.each { jobName, config ->
    pipelineJob("CI/${jobName}") {
        displayName("${jobName} — CI")
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
