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

        def workspace             = new workspace_management()
        def vcs                   = new git_management()
        def credscan              = new cred_scanning()
        def dependencyscan        = new dependency_scanning()
        def static_code_analysis  = new static_code_analysis()
        def unittest              = new junit()
        def build_dockerfile      = new build_dockerfile()
        def coverage              = new codecoverage()
        def publish               = new publish_artifact()
        def trivy                 = new docker_image_scanning()
        def dockle                = new dockle_scanning()
        def image_size_validator  = new image_size_validator()
        def notify                = new notify()
       // def build                 = new build_artifact()
 
        def buildArtifact         = new build_artifact() 

        def repo_url
        if (get_params_value(enableOverride, step_params, 'repo_url_type') == 'http') {
                repo_url = "${get_params_value(enableOverride, step_params, 'repo_https_url')}"
        } else if (get_params_value(enableOverride, step_params, 'repo_url_type') == 'ssh') {
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
                                        pom_location: "${get_params_value(enableOverride, step_params, 'pom_location')}",
                                        nvd_api_key_creds_id: "${get_params_value(enableOverride, step_params, 'nvd_api_key_creds_id')}"
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
                        buildArtifact.build_factory(
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

                if (get_params_value(enableOverride, step_params, 'unit_testing_check') != null && get_params_value(enableOverride, step_params, 'unit_testing_check').toBoolean()) {
                        stage('Unit Test & Code Coverage') {
                    unittest.unit_testing_factory(
                                        repo_url: "${repo_url}",
                                        repo_url_type: "${get_params_value(enableOverride, step_params, 'repo_url_type')}",
                                        unit_testing_check: "${get_params_value(enableOverride, step_params, 'unit_testing_check')}",
                                        build_tool: "${get_params_value(enableOverride, step_params, 'build_tool')}",
                                        fail_job_if_unit_issue_detected: "${get_params_value(enableOverride, step_params, 'fail_job_if_unit_issue_detected')}",
                                        unit_test_reports_path: "${get_params_value(enableOverride, step_params, 'unit_test_reports_path')}",
                                        findbugs_test_report_path: "${get_params_value(enableOverride, step_params, 'findbugs_test_report_path')}",
                                        withmaven_globaltool_jdk: "${get_params_value(enableOverride, step_params, 'withmaven_globaltool_jdk')}",
                                        withmaven_globaltool_maven: "${get_params_value(enableOverride, step_params, 'withmaven_globaltool_maven')}",
                                        source_code_path: "${get_params_value(enableOverride, step_params, 'source_code_path')}",
                                        mvn_settings_path: "${get_params_value(enableOverride, step_params, 'mvn_settings_path')}",
                                        pom_location : "${get_params_value(enableOverride, step_params, 'pom_location')}",
                                        java_version : "${get_params_value(enableOverride, step_params, 'java_version')}"

                                )
                           }
                }

                else {
                echo 'Skipping Unit test and Code Coverage stage as it is disabled.'
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

                        if (get_params_value(enableOverride, step_params, 'dockle_scan_check') != null && get_params_value(enableOverride, step_params, 'dockle_scan_check').toBoolean()) {
                    tasks['DockleHardeningScan'] = {
                                dockle.dockle(
                                        image_scanning_check: "${get_params_value(enableOverride, step_params, 'dockle_scan_check')}",
                                        image_name: "${get_params_value(enableOverride, step_params, 'image_name')}",
                                        image_tag: "${get_params_value(enableOverride, step_params, 'image_tag')}",
                                        image_scanning_report_publish: "${get_params_value(enableOverride, step_params, 'dockle_report_publish')}"
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
                        // DockerHub parameters
                        dockerhub_credentials_id: "${step_params.dockerhub_credentials_id}",
                        dockerhub_username: "${step_params.dockerhub_username}",
                        // S3 parameters
                        artifact_source_path: "${step_params.artifact_source_path}",
                        artifact_s3_bucket_name: "${step_params.artifact_s3_bucket_name}",
                        artifact_s3_bucket_aws_region: "${step_params.artifact_s3_bucket_aws_region}",
                        // GCR parameters
                        gcp_project_id: "${step_params.gcp_project_id}",
                        gcr_hostname: "${step_params.gcr_hostname}",
                        gcr_repository: "${step_params.gcr_repository}"
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
                        try {
                            def parser  = new parser()
                            def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

                            def docker_image_tag = sh(
                                script: """git config --global --add safe.directory ${WORKSPACE}/${repo_dir} && \
                                           cd ${WORKSPACE}/${repo_dir} && git rev-parse --short HEAD""",
                                returnStdout: true
                            ).trim()

                            def cd_params = [
                                string(name: get_params_value(enableOverride, step_params, 'image_tag_build_param'), value: docker_image_tag)
                            ]

                            def enableJira = get_params_value(enableOverride, step_params, 'enable_jira')?.toBoolean() ?: false
                            if (enableJira) {
                                cd_params += [
                                    booleanParam(name: get_params_value(enableOverride, step_params, 'enable_jira_build_param'), value: true),
                                    string(name: get_params_value(enableOverride, step_params, 'jira_ticket_id_build_param'), value: "${params.jira_ticket_id}")
                                ]
                            }

                            build job: get_params_value(enableOverride, step_params, 'trigger_cd_pipeline_path'),
                                parameters: cd_params,
                                wait: false
                        } catch (Exception triggerEx) {
                            echo "WARNING: CD pipeline trigger failed: ${triggerEx.getMessage()}. Continuing with workspace cleanup."
                        }
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
