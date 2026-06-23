package opstree.common

def deployToBeanstalk(Map step_params) {
    def logger = new maximor.common.logger()
    def parser = new parser()

    // Validate workspace exists
    def workspacePath = env.WORKSPACE ?: error('Workspace path not found')

    def params = [
        deployment_env: get_value(step_params, 'deployment_env', 'elasticbeanstalk'),
        credentialId: get_value(step_params, 'credentialId', null),
        awsRegion: get_value(step_params, 'awsRegion', 'us-east-1'),
        applicationName: get_value(step_params, 'applicationName', null),
        environmentName: get_value(step_params, 'environmentName', null),
        bucketName: get_value(step_params, 'bucketName', null),
        keyPrefix: get_value(step_params, 'keyPrefix', '/'),
        versionLabelFormat: get_value(step_params, 'versionLabelFormat', "${env.BUILD_TAG}"),
        versionDescriptionFormat: get_value(step_params, 'versionDescriptionFormat', "${env.BUILD_TAG}"),
        rootObject: get_value(step_params, 'rootObject', '.'),
        zeroDowntime: get_value(step_params, 'zeroDowntime', false),
        sleepTime: get_value(step_params, 'sleepTime', 90),
        checkHealth: get_value(step_params, 'checkHealth', false),
        maxAttempts: get_value(step_params, 'maxAttempts', 3),
        skipEnvironmentUpdates: get_value(step_params, 'skipEnvironmentUpdates', false),
        repo_url: get_value(step_params, 'repo_url', null),
        source_code_path: get_value(step_params, 'source_code_path', ''),
    ]

    def missingParams = []
    if (!params.credentialId) missingParams << 'credentialId'
    if (!params.applicationName) missingParams << 'applicationName'
    if (!params.environmentName) missingParams << 'environmentName'
    if (!params.bucketName) missingParams << 'bucketName'

    if (missingParams) {
        String errorMsg = "Missing required parameters: ${missingParams.join(', ')}"
        logger.logger('msg': errorMsg, 'level': 'ERROR')
        error(errorMsg)
    }

    logger.logger('msg': "Creating deployment package from: ${params.rootObject}", 'level': 'INFO')

    // Verify source directory exists
    def sourceDir = new File(params.rootObject)
    if (!sourceDir.exists()) {
        error("❌ Source directory missing: ${sourceDir.absolutePath}")
    }
    if (sourceDir.list().length == 0) {
        error("❌ Source directory is empty: ${sourceDir.absolutePath}")
    }

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${params.repo_url}")
    repo_dir = repo_dir + params.source_code_path

    logger.logger(
        'msg': """Starting AWS Elastic Beanstalk deployment:
                 | Application: ${params.applicationName}
                 | Environment: ${params.environmentName}
                 | Dir: ${repo_dir}
                 """.stripMargin(),
        'level': 'INFO'
    )

    dir("${WORKSPACE}/${repo_dir}") {
        step([
        $class: 'AWSEBDeploymentBuilder',
        credentialId: params.credentialId,
        awsRegion: params.awsRegion,
        applicationName: params.applicationName,
        environmentName: params.environmentName,
        bucketName: params.bucketName,
        keyPrefix: params.keyPrefix,
        versionLabelFormat: params.versionLabelFormat,
        versionDescriptionFormat: params.versionDescriptionFormat,
        rootObject: params.rootObject,
        zeroDowntime: params.zeroDowntime,
        sleepTime: params.sleepTime,
        checkHealth: params.checkHealth,
        maxAttempts: params.maxAttempts,
        skipEnvironmentUpdates: params.skipEnvironmentUpdates,
        includes: '**'
    ])
    }

    logger.logger(
        'msg': """Successfully deployed to AWS Elastic Beanstalk:
                 | Environment: ${params.environmentName}
                 | Version: ${params.versionLabelFormat}""".stripMargin(),
        'level': 'INFO'
    )
}

def get_value(Map map, String key, Object defaultVal) {
    return map.containsKey(key) ? map.get(key) : defaultVal
}
