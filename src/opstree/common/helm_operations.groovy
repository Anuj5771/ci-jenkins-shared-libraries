package opstree.common

def workdir = env.WORKSPACE ?: '/workspace'

def executeHelmCommand(String command, Map config, Boolean returnStatus = false) {
    def logger = new opstree.common.logger()
    String dockerRun = """
        docker run --rm \
        -v ${config.kubeconfig}:/root/.kube/config \
        -v ${env.WORKSPACE}:/workspace \
        -v ~/.helm:/root/.helm \
        -v ~/.config/helm:/root/.config/helm \
        -v ~/.cache/helm:/root/.cache/helm \
        -w /workspace \
        alpine/k8s:1.31.8 \
        /bin/sh -c "${command} 2>&1"
    """.stripIndent().trim()
    try {
        def result = sh(
            script: dockerRun,
            label: "Executing: ${command}",
            returnStatus: returnStatus,
            returnStdout: true
        )
        if (!returnStatus) {
            logger.logger(msg: "Helm output: ${result}", level: 'INFO')
        }
        return result
    } catch (Exception e) {
        logger.logger(msg: "Helm operation failed for command: ${command}. Error: ${e.toString()}", level: 'ERROR')
        error('Helm command failed. See logs for details.')
    }
}

private def validateKubeConfig(Map config) {
    def logger = new opstree.common.logger()
    String kubeconfigPath = config.kubeconfig
    logger.logger(msg: "Validating Kubernetes cluster access using kubeconfig: ${kubeconfigPath}", level: 'INFO')
    def exitCode = sh(
        script: """
            docker run --rm \
              -v ${kubeconfigPath}:/root/.kube/config:ro -e KUBECONFIG=/root/.kube/config \
              alpine/k8s:1.31.8 sh -c 'kubectl cluster-info 2>&1'
        """,
        returnStdout: true,
        returnStatus: true
    )
    if (exitCode != 0) {
        logger.logger(msg: "Kubeconfig validation failed. Output: ${result.stdout}", level: 'ERROR')
        error("Kubeconfig validation failed. Output: ${result.stdout}")
    } else {
        logger.logger(msg: 'Kubeconfig file is valid', level: 'INFO')
        return [valid: true, summary: 'Kubeconfig file is valid']
    }
}

private def validateValuesFilePath(String filePath, String repoDir, def logger, def summary, def valid) {
    def normalizedPath = filePath.replaceFirst(/^(\.\/|\/)/, '')
    def workspace = env.WORKSPACE
    def directPath = "${workspace}/${repoDir}/${normalizedPath}"
    logger.logger(msg: "Checking values file: ${directPath}", level: 'INFO')
    if (fileExists(directPath)) {
        summary << "Values file found: ${directPath}"
    } else {
        logger.logger(msg: "Values file does not exist: ${directPath}", level: 'WARN')
        summary << "No values file found at: ${directPath}"
    }
    return valid
}

def precheckHelmInputs(Map config) {
    def logger = new opstree.common.logger()
    def parser = new parser()
    def summary = []
    def valid = true
    def requiredParams = ['release_name', 'chart_name', 'namespace', 'kubeconfig']
    def missingParams = []
    requiredParams.each { param ->
        if (!config.containsKey(param) || config[param] == null || config[param].toString().trim().isEmpty()) {
            missingParams << param
            valid = false
        }
    }
    if (missingParams.size() > 0) {
        logger.logger(msg: "Missing required parameters: ${missingParams.join(', ')}", level: 'ERROR')
        summary << "Missing required parameters: ${missingParams.join(', ')}"
    } else {
        summary << 'All required parameters are present'
    }
    if (config.containsKey('helm_repo_url') && config.helm_repo_url) {
        summary << "Using chart from repository: ${config.helm_repo_url}"
    } else if (config.containsKey('chart_name') && config.chart_name) {
        def repo_dir = parser.fetch_git_repo_name(['repo_url': "${config.repo_url ?: ''}"])
        def normalizedChartPath = config.chart_name.replaceFirst(/^(\.\/|\/)/, '')
        def chartRef = "${repo_dir}/${normalizedChartPath}"
        logger.logger(msg: "Checking local chart path: ${chartRef}", level: 'INFO')
        if (!fileExists(chartRef)) {
            logger.logger(msg: "Local chart path does not exist: ${chartRef}", level: 'ERROR')
            summary << "Local chart path does not exist: ${chartRef}"
            valid = false
        } else {
            summary << "Using local chart path: ${chartRef}"
        }
    }
    if (config?.values && !(config.values instanceof String && config.values.trim() == 'null')) {
        def repo_dir = parser.fetch_git_repo_name(['repo_url': "${config.repo_url ?: ''}"])
        def valuesList = (config.values instanceof List) ? config.values : [config.values]
        valuesList.each { filePath ->
            if (filePath && filePath != 'null') {
                validateValuesFilePath(filePath, repo_dir, logger, summary, valid)
            }
        }
    } else {
        logger.logger(msg: 'No values file(s) specified. Proceeding without values.', level: 'INFO')
        summary << 'No values file(s) specified'
    }
    if (config.containsKey('kubeconfig') && config.kubeconfig) {
        try {
            def kubeconfigResult = validateKubeConfig(config)
            if (kubeconfigResult.valid) {
                summary << 'Kubeconfig is valid and cluster is accessible'
            } else {
                valid = false
                summary << 'Kubeconfig validation failed'
            }
        } catch (Exception e) {
            valid = false
            logger.logger(msg: "Kubeconfig validation failed: ${e.message}", level: 'ERROR')
            summary << "Kubeconfig validation failed: ${e.message}"
        }
    }
    logger.logger(msg: 'Helm Pre-deployment Validation Summary:', level: 'INFO')
    summary.each { logger.logger(msg: "- ${it}", level: 'INFO') }
    return [valid: valid, summary: summary]
}

def helmDeploy(Map step_params) {
    def logger = new opstree.common.logger()
    def parser = new parser()
    def params = [
        release_name: get_value(step_params, 'release_name', null),
        chart_name: get_value(step_params, 'chart_name', null),
        namespace: get_value(step_params, 'namespace', 'default'),
        helm_repo_url: get_value(step_params, 'helm_repo_url', null),
        helm_values: get_value(step_params, 'values', get_value(step_params, 'values_file', '')),
        helm_set_values: get_value(step_params, 'set_values', ''),
        chart_version: get_value(step_params, 'chart_version', ''),
        kubeconfig: get_value(step_params, 'kubeconfig', ''),
        helm_atomic: get_value(step_params, 'atomic', true),
        helm_wait_for: get_value(step_params, 'wait', true),
        helm_timeout: get_value(step_params, 'timeout', '5m'),
        helm_force: get_value(step_params, 'force', false),
        repo_url: get_value(step_params, 'repo_url', null),
        source_code_path: get_value(step_params, 'source_code_path', ''),
        create_namespace: get_value(step_params, 'create_namespace', true)
    ]

    def repo_dir = parser.fetch_git_repo_name(['repo_url': "${params.repo_url ?: ''}"])

    try {
        String chartRef
        if (params.helm_repo_url != null && !params.helm_repo_url.toString().trim().isEmpty()) {
            String repoName = params.chart_name.contains('/') ? params.chart_name.split('/')[0] : 'chart-repo'
            executeHelmCommand("helm repo add ${repoName} ${params.helm_repo_url} --force-update", [kubeconfig: params.kubeconfig])
            executeHelmCommand("helm repo update ${repoName}", [kubeconfig: params.kubeconfig])
            chartRef = params.chart_name
            logger.logger(msg: "Using chart from repository: ${chartRef}", level: 'INFO')
        } else {
            def normalizedChartPath = params.chart_name.replaceFirst(/^(\.\/|\/)/, '')
            chartRef = "${repo_dir}/${normalizedChartPath}"
            logger.logger(msg: "Using local chart path: ${chartRef}", level: 'INFO')

            // Add helm dependency build for local charts
            logger.logger(msg: "Building dependencies for local chart: ${chartRef}", level: 'INFO')
            def depBuildStatus = executeHelmCommand("helm dependency build ${chartRef}", [kubeconfig: params.kubeconfig], true)

            if (depBuildStatus == 0) {
                def depOutput = executeHelmCommand("helm dependency list ${chartRef}", [kubeconfig: params.kubeconfig])

                if (depOutput.contains('No dependencies')) {
                    logger.logger(msg: "No dependencies found for chart: ${chartRef}", level: 'INFO')
                } else {
                    logger.logger(msg: "Dependencies successfully built for chart: ${chartRef}", level: 'INFO')
                    logger.logger(msg: "Dependency details:\n${depOutput}", level: 'INFO')
                }
            } else {
                def errorOutput = executeHelmCommand("helm dependency build ${chartRef}", [kubeconfig: params.kubeconfig])
                logger.logger(msg: "Failed to build dependencies for chart: ${chartRef}", level: 'ERROR')
                logger.logger(msg: "Error details: ${errorOutput}", level: 'ERROR')
                error("Failed to build dependencies for chart: ${chartRef}. See logs for details.")
            }
        }
        List<String> helmCmdParts = ['helm', 'upgrade', '--install', params.release_name, chartRef, '-n', params.namespace]
        if (params.create_namespace) helmCmdParts << '--create-namespace'
        if (params.helm_atomic) helmCmdParts << '--atomic'
        if (params.helm_wait_for) helmCmdParts << '--wait'
        if (params.helm_timeout) helmCmdParts << '--timeout' << params.helm_timeout
        if (params.helm_force) helmCmdParts << '--force'
        if (params.chart_version) helmCmdParts << '--version' << params.chart_version
        if (params.helm_values != null && params.helm_values) {
            if (params.helm_values instanceof List) {
                params.helm_values.each { filePath ->
                    String fullPath = filePath.replaceFirst(/^(\.\/|\/)/, '')
                    if (fileExists("${repo_dir}/${fullPath}")) {
                        helmCmdParts << '-f' << "${repo_dir}/${fullPath}"
                    } else {
                        logger.logger(msg: "Skipping non-existent values file: ${fullPath}", level: 'WARN')
                    }
                }
            } else {
                String fullPath = params.helm_values.replaceFirst(/^(\.\/|\/)/, '')
                if (fileExists("${repo_dir}/${fullPath}")) {
                    helmCmdParts << '-f' << "${repo_dir}/${fullPath}"
                } else {
                    logger.logger(msg: "Skipping non-existent values file: ${fullPath}", level: 'WARN')
                }
            }
        }
        if (params.helm_set_values != null && params.helm_set_values) {
            if (params.helm_set_values instanceof Map) {
                params.helm_set_values.each { k, v ->
                    if (v != null) {
                        String valueStr = (v instanceof Number) ? v.toString() : v.toString().replace(',', '\\,').replace('"', '\\"')
                        helmCmdParts << '--set' << "${k}=${valueStr}"
                    }
                }
            } else if (params.helm_set_values instanceof List) {
                params.helm_set_values.each { item ->
                    helmCmdParts << '--set' << item
                }
            } else {
                helmCmdParts << '--set' << params.helm_set_values
            }
        }
        String helmCmd = helmCmdParts.join(' ')
        logger.logger(msg: "Constructed Helm command: ${helmCmd}", level: 'INFO')
        def helmOutput = executeHelmCommand(helmCmd, [kubeconfig: params.kubeconfig])
        logger.logger(msg: "Helm output: ${helmOutput}", level: 'INFO')
        def statusCmd = "helm status ${params.release_name} -n ${params.namespace}"
        def statusOutput = executeHelmCommand(statusCmd, [kubeconfig: params.kubeconfig])
        logger.logger(msg: "Deployment status: ${statusOutput}", level: 'INFO')
    } catch (Exception e) {
        logger.logger(msg: "Helm deployment failed: ${e.message}", level: 'ERROR')
        throw e
    }
}

private def get_value(Map params, String key, def default_value) {
    return params.containsKey(key) ? params[key] : default_value
}
