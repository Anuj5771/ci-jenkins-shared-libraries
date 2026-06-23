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
        build_dockerfile = new build_dockerfile()
        publish = new publish_artifact()
        notify = new notify()
        build = new build_artifact()
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
                


                if (get_params_value(enableOverride, step_params, 'perform_code_build') != null && get_params_value(enableOverride, step_params, 'perform_code_build').toBoolean()) {
                        stage('Build Artifact') {
                        build.build_factory(
                                perform_code_build: "${get_params_value(enableOverride, step_params, 'perform_code_build')}",
                                repo_url: "${repo_url}",
                                repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                                build_tool: "${get_params_value(enableOverride, step_params, 'build_tool')}",
                                withmaven_globaltool_jdk: "${get_params_value(enableOverride, step_params, 'withmaven_globaltool_jdk')}",
                                withmaven_globaltool_maven: "${get_params_value(enableOverride, step_params, 'withmaven_globaltool_maven')}",
                                source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                                mvn_settings_path: "${get_params_value(enableOverride, step_params, 'mvn_settings_path')}",
                                codeartifact_dependency: "${get_params_value(enableOverride, step_params, 'codeartifact_dependency')}",
                                codeartifact_domain:  "${get_params_value(enableOverride, step_params, 'codeartifact_domain')}",
                                codeartifact_owner: "${get_params_value(enableOverride, step_params, 'codeartifact_owner')}",
                                pom_location : "${get_params_value(enableOverride, step_params, 'pom_location')}",
                                java_version: "${get_params_value(enableOverride, step_params, 'java_version')}",
                                gradle_command: "${get_params_value(enableOverride, step_params, 'gradle_command')}",
                                gradle_build_file_location: "${get_params_value(enableOverride, step_params, 'gradle_build_file_location')}"
                        )
                        }
                }

                


                if (get_params_value(enableOverride, step_params, 'perform_build_dockerfile') != null && get_params_value(enableOverride, step_params, 'perform_build_dockerfile').toBoolean()) {
                stage('Build Docker Image') {
                    build_dockerfile.build_factory(
                                                perform_build_dockerfile:"${get_params_value(enableOverride, step_params, 'perform_build_dockerfile')}",
                                                repo_url: "${repo_url}",
                                                image_name: "${get_params_value(enableOverride, step_params, 'image_name')}",
                                                static_code_analysis_check: "${get_params_value(enableOverride, step_params, 'static_code_analysis_check')}",
                                                app_stack: 'java',
                                                source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                                                dockerfile_context: "${get_params_value(enableOverride, step_params, 'dockerfile_context')}",
                                                dockerfile_location: "${get_params_value(enableOverride, step_params, 'dockerfile_location')}",
                                                codeartifact_dependency: "${get_params_value(enableOverride, step_params, 'codeartifact_dependency')}",
                                                codeartifact_domain: "${get_params_value(enableOverride, step_params, 'codeartifact_domain')}",
                                                codeartifact_owner: "${get_params_value(enableOverride, step_params, 'codeartifact_owner')}"
                                        )
                }
                }
                else {
                        echo 'Skipping Build Docker Image stage as it is disabled.'
                }


                if (step_params.artifact_publish_check?.toBoolean()) {
                stage('Publish Artifact') {
                    publish.publish_factory(
                        source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                        artifact_publish_check: true,
                        artifact_destination_type: "${step_params.artifact_destination_type}",
                        // Common parameters
                        docker_image_name: "${step_params.image_name}",
                        repo_url: "${repo_url}",
                        // AWS/ECR parameters
                        jenkins_aws_credentials_id: "${step_params.jenkins_aws_credentials_id}",
                        ecr_repo_name: "${step_params.ecr_repo_name}",
                        ecr_region: "${step_params.ecr_region}",
                        account_id: "${step_params.account_id}",
                        // Harbor parameters
                        harbor_url: "${step_params.harbor_url}",
                        harbor_project: "${step_params.harbor_project}",
                        harbor_credentials_id: "${step_params.harbor_credentials_id}",
                        // S3 parameters
                        artifact_source_path: "${step_params.artifact_source_path}",
                        artifact_s3_bucket_name: "${step_params.artifact_s3_bucket_name}",
                        artifact_s3_bucket_aws_region: "${step_params.artifact_s3_bucket_aws_region}"
                    )
                }
                }
                else {
                        echo 'Skipping Publish Artifact stage as it is disabled.'
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
