package opstree.ci.templates.python_ci

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
        credscan = new vulnerability_scanning()
        dependencyscan = new dependency_scanning()
        static_code_analysis = new static_code_analysis()
        securityscan = new snyk_scanning()
        build_dockerfile = new build_dockerfile()
        publish = new publish_artifact()
        trivy = new docker_image_scanning()
        image_size_validator = new image_size_validator()
        notify = new notify()
        beanstalk = new beanstalk()
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

            stage('Pre-Build Checks') {
                def tasks = [:]

                if (get_params_value(enableOverride, step_params, 'gitleaks_check') != null && get_params_value(enableOverride, step_params, 'gitleaks_check').toBoolean()) {
                    tasks['GitleaksCredsScanning'] = {
                        credscan.creds_scanning_factory(
                                 gitleaks_check: "${get_params_value(enableOverride, step_params, 'gitleaks_check')}",
                                repo_url: "${repo_url}",
                                repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                                gitleaks_report_format: "${get_params_value(enableOverride, step_params, 'gitleaks_report_format')}",
                                gitleaks_report_jenkins_publish: "${get_params_value(enableOverride, step_params, 'gitleaks_report_jenkins_publish')}",
                                fail_job_if_leak_detected: "${get_params_value(enableOverride, step_params, 'fail_job_if_leak_detected')}"
                        )
                    }
                }

                if (get_params_value(enableOverride, step_params, 'dependency_check') != null && get_params_value(enableOverride, step_params, 'dependency_check').toBoolean()) {
                    tasks['OWASPCodeDepedencyScanning'] = {
                                dependencyscan.dependency_scanning_factory(
                                        repo_url: "${repo_url}",
                                        repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                                        owasp_project_name: "${get_params_value(enableOverride, step_params, 'owasp_project_name')}",
                                        owasp_report_publish: "${get_params_value(enableOverride, step_params, 'owasp_report_publish')}",
                                        owasp_report_format: "${get_params_value(enableOverride, step_params, 'owasp_report_format')}",
                                        dependency_check: "${get_params_value(enableOverride, step_params, 'dependency_check')}",
                                        dependency_scan_tool: "${get_params_value(enableOverride, step_params, 'dependency_scan_tool')}",
                                        fail_job_if_dependency_returned_exception: "${get_params_value(enableOverride, step_params, 'fail_job_if_dependency_returned_exception')}",
                                        source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                                        app_stack: "${get_params_value(enableOverride, step_params, 'app_stack')}",
                                        pom_location: "${get_params_value(enableOverride, step_params, 'pom_location')}"
                                )
                    }
                }

                if (tasks) {
                        parallel tasks
                } else {
                        echo 'No checks were enabled, skipping Pre-Build Checks stage.'
                }
            }

            if (get_params_value(enableOverride, step_params, 'perform_build_dockerfile') != null && get_params_value(enableOverride, step_params, 'perform_build_dockerfile').toBoolean()) {
                stage('Build Docker Image') {
                    build_dockerfile.build_factory(
                                                perform_build_dockerfile:"${get_params_value(enableOverride, step_params, 'perform_build_dockerfile')}",
                                                repo_url: "${repo_url}",
                                                image_name: "${get_params_value(enableOverride, step_params, 'image_name')}",
                                                static_code_analysis_check: "${get_params_value(enableOverride, step_params, 'static_code_analysis_check')}",
                                                app_stack: 'python',
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

            if (get_params_value(enableOverride, step_params, 'static_code_analysis_check') != null && get_params_value(enableOverride, step_params, 'static_code_analysis_check').toBoolean()) {
                        stage('Static Code Analysis') {
                        static_code_analysis.static_code_analysis_factory(
                                                repo_url: "${repo_url}",
                                                repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                                                codebase_to_scan_directory: "${get_params_value(enableOverride, step_params, 'codebase_to_scan_directory')}",
                                                static_code_analysis_check: "${get_params_value(enableOverride, step_params, 'static_code_analysis_check')}",
                                                path_to_sonar_properties: "${get_params_value(enableOverride, step_params, 'path_to_sonar_properties')}",
                                                fail_job_if_analysis_returned_exception: "${get_params_value(enableOverride, step_params, 'fail_job_if_analysis_returned_exception')}",
                                                jenkins_sonarqube_token_creds_id: "${get_params_value(enableOverride, step_params, 'jenkins_sonarqube_token_creds_id')}",
                                                app_stack: "${get_params_value(enableOverride, step_params, 'app_stack')}",
                                                unit_testing_check: "${get_params_value(enableOverride, step_params, 'unit_testing_check')}",
                                                withmaven_globaltool_jdk: "${get_params_value(enableOverride, step_params, 'withmaven_globaltool_jdk')}",
                                                withmaven_globaltool_maven: "${get_params_value(enableOverride, step_params, 'withmaven_globaltool_maven')}",
                                                source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}"
                                )
                        }
            }
        else {
                echo 'Skipping Static Code Analysis stage as it is disabled.'
        }
            if (get_params_value(enableOverride, step_params, 'deployment_env') == 'elasticbeanstalk') {
                stage('AWS Elastic Beanstalk Deployment') {
                        def deployParams = [
                                repo_url: "${repo_url}",
                                source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                                credentialId: "${get_params_value(enableOverride, step_params, 'eb_aws_creds_id')}",
                                awsRegion: "${get_params_value(enableOverride, step_params, 'eb_awsRegion')}",
                                applicationName: "${get_params_value(enableOverride, step_params, 'eb_applicationName')}",
                                environmentName: "${get_params_value(enableOverride, step_params, 'eb_environmentName')}",
                                bucketName: "${get_params_value(enableOverride, step_params, 'eb_bucketName')}",
                                keyPrefix: "${get_params_value(enableOverride, step_params, 'eb_keyPrefix')}",
                                // versionLabelFormat: "${get_params_value(enableOverride, step_params, 'eb_versionLabelFormat')}",
                                // versionDescriptionFormat: "${get_params_value(enableOverride, step_params, 'eb_versionDescriptionFormat')}",
                                versionLabelFormat: "${env.BUILD_TAG}",
                                versionDescriptionFormat: "${env.BUILD_TAG}",
                                rootObject: "${get_params_value(enableOverride, step_params, 'eb_rootObject')}",
                                zeroDowntime: "${get_params_value(enableOverride, step_params, 'eb_zeroDowntime')}",
                                sleepTime: "${get_params_value(enableOverride, step_params, 'eb_sleepTime')}",
                                checkHealth: "${get_params_value(enableOverride, step_params, 'eb_checkHealth')}",
                                maxAttempts: "${get_params_value(enableOverride, step_params, 'eb_maxAttempts')}",
                                skipEnvironmentUpdates: "${get_params_value(enableOverride, step_params, 'eb_skipEnvironmentUpdates')}"
                        ]

                        if (get_params_value(enableOverride, step_params, 'beanstalk_params')) {
                                deployParams.putAll(get_params_value(enableOverride, step_params, 'beanstalk_params'))
                        }

                    beanstalk.deployToBeanstalk(deployParams)
                }
            }
        else {
                echo 'Skipping Deployment stage as it is disabled.'
        }

            stage('Post-Build Checks') {
                def tasks = [:]

                // Conditionally add ImageScanning task
                if (get_params_value(enableOverride, step_params, 'image_scanning_check') != null && get_params_value(enableOverride, step_params, 'image_scanning_check').toBoolean()) {
                    tasks['ImageScanning'] = {
                        trivy.image_scanning_factory(
                                image_scanning_check: "${get_params_value(enableOverride, step_params, 'image_scanning_check')}",
                                image_name: "${get_params_value(enableOverride, step_params, 'image_name')}",
                                image_tag: "${get_params_value(enableOverride, step_params, 'image_tag')}",
                                scan_severity: "${get_params_value(enableOverride, step_params, 'scan_severity')}",
                                image_scanning_report_publish: "${get_params_value(enableOverride, step_params, 'image_scanning_report_publish')}"
                        )
                    }
                }

                // Conditionally add ImageSizeValidator task
                if (get_params_value(enableOverride, step_params, 'image_size_validator_check') != null && get_params_value(enableOverride, step_params, 'image_size_validator_check').toBoolean()) {
                    tasks['ImageSizeValidator'] = {
                        image_size_validator.size_validator_factory(
                                image_size_validator_check: "${get_params_value(enableOverride, step_params, 'image_size_validator_check')}",
                                image_name: "${get_params_value(enableOverride, step_params, 'image_name')}",
                                image_tag: "${get_params_value(enableOverride, step_params, 'image_tag')}",
                                max_allowed_image_size: "${get_params_value(enableOverride, step_params, 'max_allowed_image_size')}",
                                fail_job_if_validation_fail: "${get_params_value(enableOverride, step_params, 'fail_job_if_validation_fail')}"
                        )
                    }
                }

                // Only run the parallel block if there are tasks
                if (tasks) {
                        parallel tasks
                } else {
                        echo 'No image scanning or validation tasks are enabled, skipping this stage.'
                }
            }

            if (get_params_value(enableOverride, step_params, 'artifact_publish_check') != null && get_params_value(enableOverride, step_params, 'artifact_publish_check').toBoolean()) {
                        stage('Publish Artifact') {
                        publish.publish_factory(
                                repo_url: "${repo_url}",
                                artifact_publish_check: "${get_params_value(enableOverride, step_params, 'artifact_publish_check')}",
                                artifact_destination_type: "${get_params_value(enableOverride, step_params, 'artifact_destination_type')}",
                                jenkins_aws_credentials_id: "${get_params_value(enableOverride, step_params, 'jenkins_aws_credentials_id')}",
                                docker_image_name: "${get_params_value(enableOverride, step_params, 'docker_image_name')}",
                                ecr_repo_name: "${get_params_value(enableOverride, step_params, 'ecr_repo_name')}",
                                ecr_region: "${get_params_value(enableOverride, step_params, 'ecr_region')}",
                                account_id: "${get_params_value(enableOverride, step_params, 'account_id')}"
                        )
                        }
            }
         else {
                echo 'Skipping Publish Artifact stage as it is disabled.'
         }
        }
    catch (Exception e) {
            // Handle any exception or failure scenario
            currentBuild.result = 'FAILURE'
            if (get_params_value(enableOverride, step_params, 'notification_enabled') != null && get_params_value(enableOverride, step_params, 'notification_enabled').toBoolean()) {
                notify.notification_factory(
                        build_status: 'Failure',
                        webhook_url_creds_id: "${get_params_value(enableOverride, step_params, 'webhook_url_creds_id')}",
                        notification_channel: "${get_params_value(enableOverride, step_params, 'notification_channel')}",
                        notification_enabled: "${get_params_value(enableOverride, step_params, 'notification_enabled')}",
                        slack_channel: "${get_params_value(enableOverride, step_params, 'slack_channel')}"
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
                                        notification_enabled: "${get_params_value(enableOverride, step_params, 'notification_enabled')}",
                                        slack_channel: "${get_params_value(enableOverride, step_params, 'slack_channel')}"

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
