package opstree.cd.templates.helm

import opstree.common.*

def get_params_value(Boolean enableOverride, Map step_params, String paramName) {
    return enableOverride && params.containsKey(paramName) ? params[paramName] : step_params[paramName]
}

def call(Map step_params) {
    ansiColor('xterm') {
        def enableOverride = step_params.enable_jenkins_build_param_override?.toBoolean() ?: false
        echo "enableoverride = ${enableOverride}"

        def workspace = new workspace_management()
        def helm = new helm_operations()
        def vcs = new git_management()
        def notify = new notify()
        def credscan = new vulnerability_scanning()
        def static_code_analysis = new static_code_analysis()

        def repo_url
        if (step_params.repo_url_type == 'http') {
            repo_url = step_params.repo_https_url
        } else if (step_params.repo_url_type == 'ssh') {
            repo_url = step_params.repo_ssh_url
        }

        try {
            stage('Git Checkout') {
                vcs.git_checkout(
                    repo_url: "${repo_url}",
                    repo_branch: "${get_params_value(enableOverride, step_params, 'repo_branch')}",
                    clean_workspace: "${get_params_value(enableOverride, step_params, 'clean_workspace')}",
                    repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                    ssh_private_key_location: "${get_params_value(enableOverride, step_params, 'ssh_private_key_location')}",
                    jenkins_git_ssh_key_id: "${get_params_value(enableOverride, step_params, 'jenkins_git_ssh_key_id')}",
                    jenkins_git_creds_id: "${get_params_value(enableOverride, step_params, 'jenkins_git_creds_id')}",
                    source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}"
                )
            }

            def helmParams = [
                repo_url: "${repo_url}",
                source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                chart_name: "${get_params_value(enableOverride, step_params, 'chart_name')}",
                release_name: "${get_params_value(enableOverride, step_params, 'release_name')}",
                namespace: "${get_params_value(enableOverride, step_params, 'namespace')}",
                helm_values: "${get_params_value(enableOverride, step_params, 'values')}",
                helm_set_values: get_params_value(enableOverride, step_params, 'set_values'),
                chart_version: get_params_value(enableOverride, step_params, 'chart_version'),
                helm_repo_url: get_params_value(enableOverride, step_params, 'helm_repo_url'),
                helm_atomic: "${get_params_value(enableOverride, step_params, 'atomic')}",
                helm_wait_for: "${get_params_value(enableOverride, step_params, 'wait')}",
                helm_force: "${get_params_value(enableOverride, step_params, 'force')}",
                create_namespace: get_params_value(enableOverride, step_params, 'create_namespace'),
                helm_timeout: get_params_value(enableOverride, step_params, 'timeout')
            ]

            def kubeconfigCredentialId = get_params_value(enableOverride, step_params, 'kubeconfig_credential_id')
            withCredentials([file(credentialsId: kubeconfigCredentialId, variable: 'KUBECONFIG_FILE')]) {
                stage('Helm Precheck Validation') {
                    echo '🔍 Performing pre-deployment validation checks'
                    helmParams.kubeconfig = env.KUBECONFIG_FILE
                    def precheckResult = helm.precheckHelmInputs(helmParams)
                    if (!precheckResult.valid) {
                        error('Helm pre-deployment validation failed. Please check the logs for details.')
                    }
                    echo '✅ Pre-deployment validation passed. Ready to deploy.'
                }

                stage('Helm Deployment') {
                    echo "🚀 Performing Helm deployment for release ${helmParams.release_name} in namespace ${helmParams.namespace}"
                    helmParams.kubeconfig = env.KUBECONFIG_FILE
                    helm.helmDeploy(helmParams)
                }
            }
        } catch (Exception e) {
            currentBuild.result = 'FAILURE'
            if (step_params.notification_enabled != null && step_params.notification_enabled.toBoolean()) {
                notify.notification_factory(
                    build_status: 'FAILURE',
                    webhook_url_creds_id: "${get_params_value(enableOverride, step_params, 'webhook_url_creds_id')}",
                    notification_channel: "${get_params_value(enableOverride, step_params, 'notification_channel')}",
                    notification_enabled: "${get_params_value(enableOverride, step_params, 'notification_enabled')}",
                    slack_channel: "${get_params_value(enableOverride, step_params, 'slack_channel')}"
                )
            }
            throw e
        } finally {
            if (step_params.notification_enabled != null && step_params.notification_enabled.toBoolean()) {
                if (currentBuild.currentResult == 'SUCCESS') {
                    notify.notification_factory(
                        build_status: 'SUCCESS',
                        webhook_url_creds_id: "${get_params_value(enableOverride, step_params, 'webhook_url_creds_id')}",
                        notification_channel: "${get_params_value(enableOverride, step_params, 'notification_channel')}",
                        notification_enabled: "${get_params_value(enableOverride, step_params, 'notification_enabled')}",
                        slack_channel: "${get_params_value(enableOverride, step_params, 'slack_channel')}"
                    )
                }
            }
            if (step_params.clean_workspace != null && step_params.clean_workspace.toBoolean()) {
                workspace.workspace_management(
                    clean_workspace: "${get_params_value(enableOverride, step_params, 'clean_workspace')}",
                    ignore_clean_workspace_failure: "${get_params_value(enableOverride, step_params, 'ignore_clean_workspace_failure')}",
                    delete_dirs: "${get_params_value(enableOverride, step_params, 'delete_dirs')}",
                    clean_when_build_aborted: "${get_params_value(enableOverride, step_params, 'clean_when_build_aborted')}",
                    clean_when_build_failed: "${get_params_value(enableOverride, step_params, 'clean_when_build_failed')}",
                    clean_when_not_built: "${get_params_value(enableOverride, step_params, 'clean_when_not_built')}",
                    clean_when_build_succeed: "${get_params_value(enableOverride, step_params, 'clean_when_build_succeed')}",
                    clean_when_build_unstable: "${get_params_value(enableOverride, step_params, 'clean_when_build_unstable')}"
                )
            }
        }
    }
}
