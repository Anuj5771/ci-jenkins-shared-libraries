folder('CI') {
    displayName('CI')
    description('Continuous Integration pipelines')
}

folder('CD') {
    displayName('CD')
    description('Continuous Delivery pipelines')
}
evaluate(readFileFromWorkspace('jenkins_seedjob/CI/ci.groovy'))
evaluate(readFileFromWorkspace('jenkins_seedjob/CD/cd.groovy'))
