import opstree.common.*
import opstree.java.*

// Utility function to get parameter with override support
def get_params_value(Boolean enableOverride, Map step_params, String paramName) {
    return enableOverride && params.containsKey(paramName) ? params[paramName] : step_params[paramName]
}

def call(Map step_params) {
    ansiColor('xterm') {
        def enableOverride = step_params.enable_jenkins_build_param_override?.toBoolean() ?: false
        echo "enableoveride = ${enableOverride}"

        workspace = new workspace_management()
        vcs = new git_management()
        notify = new notify()
        packer = new build_packer_ami()
        jira = new jira_management()

        if (get_params_value(enableOverride, step_params, 'repo_url_type') == 'http') {
                repo_url = "${get_params_value(enableOverride, step_params, 'repo_https_url')}"
        }
        else if (get_params_value(enableOverride, step_params, 'repo_url_type') == 'ssh') {
                repo_url = "${get_params_value(enableOverride, step_params, 'repo_ssh_url')}"
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

            stage('Packer AMI Build') {
                        packer.packer_ami_build(
                                repo_url: "${repo_url}",
                                source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                                packer_ami_template_file: "${get_params_value(enableOverride, step_params, 'packer_ami_template_file')}",
                                packer_var_file: "${get_params_value(enableOverride, step_params, 'packer_var_file')}",
                                packer_ami_region: "${get_params_value(enableOverride, step_params, 'packer_ami_region')}",
                                jenkins_aws_credentials_id: "${get_params_value(enableOverride, step_params, 'jenkins_aws_credentials_id')}"

                        )
            }
        } catch (Exception e) {
            // Handle any exception or failure scenario
            currentBuild.result = 'FAILURE'
            if (get_params_value(enableOverride, step_params, 'notification_enabled') != null && get_params_value(enableOverride, step_params, 'notification_enabled').toBoolean()) {
                notify.notification_factory(
                        build_status: 'Failure',
                        webhook_url_creds_id: "${get_params_value(enableOverride, step_params, 'webhook_url_creds_id')}",
                        notification_channel: "${get_params_value(enableOverride, step_params, 'notification_channel')}",
                        notification_enabled: "${get_params_value(enableOverride, step_params, 'notification_enabled')}"

                    )
            }
            throw e
        } finally {
                // This block will always execute
                if (get_params_value(enableOverride, step_params, 'enable_jira') != null && get_params_value(enableOverride, step_params, 'enable_jira').toBoolean()) {
                jira.jira_factory(
                        enable_jira: "${get_params_value(enableOverride, step_params, 'enable_jira')}",
                        jenkins_jira_url_env_name: "${get_params_value(enableOverride, step_params, 'jenkins_jira_url_env_name')}",
                        jenkins_jira_creds_id: "${get_params_value(enableOverride, step_params, 'jenkins_jira_creds_id')}",
                        jira_ticket_id: "${params.jira_ticket_id}",
                        fail_job_if_jira_operation_failed: "${get_params_value(enableOverride, step_params, 'fail_job_if_jira_operation_failed')}",
                        build_status: currentBuild.currentResult,
                        console_log_url: "${env.BUILD_URL}console",
                        enable_buildlogurl_in_jiracomment: "${get_params_value(enableOverride, step_params, 'enable_buildlogurl_in_jiracomment')}",
                        )
                }

                if (currentBuild.currentResult == 'SUCCESS' && get_params_value(enableOverride, step_params, 'enable_trigger_cd_pipeline') != null && get_params_value(enableOverride, step_params, 'enable_trigger_cd_pipeline').toBoolean() ) {
                        parser = new parser()
                        repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

                        def docker_image_tag = sh(
                        script: """git config --global --add safe.directory ${WORKSPACE}/${repo_dir} && \
                                   cd ${WORKSPACE}/${repo_dir} && git rev-parse --short HEAD""",
                        returnStdout: true
                    ).trim()

                        build job: get_params_value(enableOverride, step_params, 'trigger_cd_pipeline_path'),
                    parameters: [
                        string(name: get_params_value(enableOverride, step_params, 'image_tag_build_param'), value: docker_image_tag),
                        booleanParam(name: get_params_value(enableOverride, step_params, 'enable_jira_build_param'), value:  "${get_params_value(enableOverride, step_params, 'enable_jira')}"),
                        string(name: get_params_value(enableOverride, step_params, 'jira_ticket_id_build_param'), value: "${params.jira_ticket_id}")
                    ], wait: false
                }

                if (get_params_value(enableOverride, step_params, 'notification_enabled') != null && get_params_value(enableOverride, step_params, 'notification_enabled').toBoolean()) {
                        if (currentBuild.currentResult == 'SUCCESS') {
                    notify.notification_factory(
                                        build_status: 'Success',
                                        webhook_url_creds_id: "${get_params_value(enableOverride, step_params, 'webhook_url_creds_id')}",
                                        notification_channel: "${get_params_value(enableOverride, step_params, 'notification_channel')}",
                                        notification_enabled: "${get_params_value(enableOverride, step_params, 'notification_enabled')}"

                                        )
                        }
                }

            if (get_params_value(enableOverride, step_params, 'clean_workspace') != null && get_params_value(enableOverride, step_params, 'clean_workspace').toBoolean()) {
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
