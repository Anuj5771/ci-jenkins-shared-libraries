package opstree.ci.templates.nodejs_ci

import opstree.common.*
import opstree.nodejs.*

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
        credscan = new cred_scanning()
        dependencyscan = new dependency_scanning()
        static_code_analysis = new static_code_analysis()
        build_dockerfile = new build_dockerfile()
        publish = new publish_artifact()
        trivy = new docker_image_scanning()
        image_size_validator = new image_size_validator()
        notify = new notify()
        build = new build_artifact()
        unittest = new junit()

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
                                                fail_job_if_leak_detected: "${get_params_value(enableOverride, step_params, 'fail_job_if_leak_detected')}",
                                                gitleaks_scan_mode: "${get_params_value(enableOverride, step_params, 'gitleaks_scan_mode') ?: 'latest'}"
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

                if (get_params_value(enableOverride, step_params, 'perform_code_build') != null && get_params_value(enableOverride, step_params, 'perform_code_build').toBoolean()) {
                        stage('Build Artifact') {
                        build.build_factory(
                                perform_code_build: "${get_params_value(enableOverride, step_params, 'perform_code_build')}",
                                repo_url: "${repo_url}",
                                repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                                source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                                node_version: "${get_params_value(enableOverride, step_params, 'node_version')}"
                        )
                        }
                }

                if (get_params_value(enableOverride, step_params, 'static_code_analysis_check') != null && get_params_value(enableOverride, step_params, 'static_code_analysis_check').toBoolean()) {
                    static_code_analysis.static_code_analysis_factory(
                                                        repo_url: "${repo_url}",
                                                        repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                                                        codebase_to_scan_directory: "${get_params_value(enableOverride, step_params, 'codebase_to_scan_directory')}",
                                                        static_code_analysis_check: "${get_params_value(enableOverride, step_params, 'static_code_analysis_check')}",
                                                        // Inline sonar params (Option 1) — no sonar-project.properties needed in app repo
                                                        sonar_project_key: "${get_params_value(enableOverride, step_params, 'sonar_project_key')}",
                                                        sonar_project_name: "${get_params_value(enableOverride, step_params, 'sonar_project_name')}",
                                                        sonar_sources: "${get_params_value(enableOverride, step_params, 'sonar_sources')}",
                                                        sonar_host_url: "${get_params_value(enableOverride, step_params, 'sonar_host_url')}",
                                                        sonar_extra_args: "${get_params_value(enableOverride, step_params, 'sonar_extra_args')}",
                                                        // Fallback: file-based (backward compat)
                                                        path_to_sonar_properties: "${get_params_value(enableOverride, step_params, 'path_to_sonar_properties')}",
                                                        fail_job_if_analysis_returned_exception: "${get_params_value(enableOverride, step_params, 'fail_job_if_analysis_returned_exception')}",
                                                        jenkins_sonarqube_token_creds_id: "${get_params_value(enableOverride, step_params, 'jenkins_sonarqube_token_creds_id')}",
                                                        app_stack: "nodejs",
                                                        unit_testing_check: "${get_params_value(enableOverride, step_params, 'unit_testing_check')}",
                                                        source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}"
                                )
                }
                else {
                        echo 'Skipping Static Code Analysis stage as it is disabled.'
                }

                if (get_params_value(enableOverride, step_params, 'unit_testing_check') != null && get_params_value(enableOverride, step_params, 'unit_testing_check').toBoolean()) {
                        stage('Unit Test') {
                        unittest.unit_testing_factory(
                                            repo_url: "${repo_url}",
                                            repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                                            unit_testing_check: "${get_params_value(enableOverride, step_params, 'unit_testing_check')}",
                                            fail_job_if_unit_issue_detected: "${get_params_value(enableOverride, step_params, 'fail_job_if_unit_issue_detected')}",
                                            unit_test_reports_path: "${get_params_value(enableOverride, step_params, 'unit_test_reports_path')}",
                                            findbugs_test_report_path: "${get_params_value(enableOverride, step_params, 'findbugs_test_report_path')}",
                                            source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                                            node_version: "${get_params_value(enableOverride, step_params, 'node_version')}"
                                    )
                        }
                }
                else {
                    echo 'Skipping Unit test stage as it is disabled.'
                }

                if (get_params_value(enableOverride, step_params, 'perform_build_dockerfile') != null && get_params_value(enableOverride, step_params, 'perform_build_dockerfile').toBoolean()) {
                    stage('Build Docker Image') {
                        build_dockerfile.build_factory(
                                                    perform_build_dockerfile:"${get_params_value(enableOverride, step_params, 'perform_build_dockerfile')}",
                                                    repo_url: "${repo_url}",
                                                    image_name: "${get_params_value(enableOverride, step_params, 'image_name')}",
                                                    static_code_analysis_check: "${get_params_value(enableOverride, step_params, 'static_code_analysis_check')}",
                                                    app_stack: 'nodejs',
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
                                                artifact_publish_check: get_params_value(enableOverride, step_params, 'artifact_publish_check')?.toString()?.toBoolean(),
                                                artifact_destination_type: "${get_params_value(enableOverride, step_params, 'artifact_destination_type')}",
                                                jenkins_aws_credentials_id: "${get_params_value(enableOverride, step_params, 'jenkins_aws_credentials_id')}",
                                                docker_image_name: "${get_params_value(enableOverride, step_params, 'docker_image_name')}",
                                                ecr_repo_name: "${get_params_value(enableOverride, step_params, 'ecr_repo_name')}",
                                                ecr_region: "${get_params_value(enableOverride, step_params, 'ecr_region')}",
                                                account_id: "${get_params_value(enableOverride, step_params, 'account_id')}",
                                                // Harbor parameters
                                                harbor_url: "${get_params_value(enableOverride, step_params, 'harbor_url')}",
                                                harbor_project: "${get_params_value(enableOverride, step_params, 'harbor_project')}",
                                                harbor_credentials_id: "${get_params_value(enableOverride, step_params, 'harbor_credentials_id')}",
                                                // DockerHub parameters
                                                dockerhub_credentials_id: "${get_params_value(enableOverride, step_params, 'dockerhub_credentials_id')}",
                                                dockerhub_username: "${get_params_value(enableOverride, step_params, 'dockerhub_username')}",
                                                // S3 parameters
                                                artifact_source_path: "${get_params_value(enableOverride, step_params, 'artifact_source_path')}",
                                                artifact_s3_bucket_name: "${get_params_value(enableOverride, step_params, 'artifact_s3_bucket_name')}",
                                                artifact_s3_bucket_aws_region: "${get_params_value(enableOverride, step_params, 'artifact_s3_bucket_aws_region')}",
                                                // GCR parameters
                                                gcp_project_id: "${get_params_value(enableOverride, step_params, 'gcp_project_id')}",
                                                gcr_hostname: "${get_params_value(enableOverride, step_params, 'gcr_hostname')}",
                                                gcr_repository: "${get_params_value(enableOverride, step_params, 'gcr_repository')}"
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
                        string(name: get_params_value(enableOverride, step_params, 'image_tag_build_param') ?: 'image_tag', value: docker_image_tag)
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
                                ignore_clean_workspace_failure: "${get_params_value(enableOverride, step_params, 'ignore_clean_workspace_failure') ?: 'false'}",
                                delete_dirs: "${get_params_value(enableOverride, step_params, 'delete_dirs') ?: 'false'}",
                                clean_when_build_aborted: "${get_params_value(enableOverride, step_params, 'clean_when_build_aborted') ?: 'true'}",
                                clean_when_build_failed: "${get_params_value(enableOverride, step_params, 'clean_when_build_failed') ?: 'true'}",
                                clean_when_not_built: "${get_params_value(enableOverride, step_params, 'clean_when_not_built') ?: 'true'}",
                                clean_when_build_succeed: "${get_params_value(enableOverride, step_params, 'clean_when_build_succeed') ?: 'true'}",
                                clean_when_build_unstable: "${get_params_value(enableOverride, step_params, 'clean_when_build_unstable') ?: 'true'}"
                        )
            }
        }
    }
}
