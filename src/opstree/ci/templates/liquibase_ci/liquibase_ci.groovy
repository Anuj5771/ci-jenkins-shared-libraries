package opstree.ci.templates.liquibase_ci

import opstree.common.*

// ---------------------------------------------------------------------------
//  liquibase_ci.groovy  –  Jenkins Shared Library template
//
//  Handles THREE pipeline types based on  step_params.pipeline_type:
//    • "pr-validation"  → runs liquibase validate
//    • "deploy"         → runs liquibase update
//    • "drift-detection"→ runs liquibase status + diff
//
//  Usage in Jenkinsfile:
//    @Library('ci-jenkins-shared-libraries') _
//    liquibase_ci(
//        pipeline_type          : 'deploy',           // pr-validation | deploy | drift-detection
//        repo_https_url         : 'https://github.com/priyanshu499-ops/montra-liquibase.git',
//        repo_branch            : 'main',
//        jenkins_git_creds_id   : 'github-creds',
//        liquibase_url_creds_id      : 'LIQUIBASE_URL',
//        liquibase_username_creds_id : 'LIQUIBASE_USERNAME',
//        liquibase_password_creds_id : 'LIQUIBASE_PASSWORD',
//        source_code_path       : '.',
//        changelog_file         : 'changelog/db.changelog-master.xml',
//        use_docker             : false,               // true = run via Docker image
//        notification_enabled   : false,
//        clean_workspace        : true
//    )
// ---------------------------------------------------------------------------

def get_value(Map map, String key, Object defaultVal) {
    return map.containsKey(key) ? map.get(key) : defaultVal
}

def call(Map step_params) {
    ansiColor('xterm') {
        def workspace  = new workspace_management()
        def vcs        = new git_management()
        def liquibase  = new liquibase_operations()
        def notify     = new notify()
        def logger     = new logger()

        def pipeline_type = get_value(step_params, 'pipeline_type', 'pr-validation')
        def repo_url      = get_value(step_params, 'repo_https_url', '')
        def repo_branch   = get_value(step_params, 'repo_branch', 'main')

        logger.logger(msg: "=== Liquibase Pipeline :: ${pipeline_type.toUpperCase()} ===", level: 'INFO')

        try {
            // ----------------------------------------------------------------
            //  STAGE 1 – Checkout (skipped for drift-detection cron builds
            //  where workspace already has code; but it's safe to always run)
            // ----------------------------------------------------------------
            stage('Git Checkout') {
                vcs.git_checkout(
                    repo_url            : repo_url,
                    repo_branch         : repo_branch,
                    repo_url_type       : 'http',
                    jenkins_git_creds_id: "${get_value(step_params, 'jenkins_git_creds_id', 'github-creds')}",
                    jenkins_git_ssh_key_id: 'null',
                    ssh_private_key_location: 'null',
                    clean_workspace     : 'true'
                )
            }

            // ----------------------------------------------------------------
            //  Build the common params map for liquibase_operations
            // ----------------------------------------------------------------
            def liq_params = [
                source_code_path            : get_value(step_params, 'source_code_path', '.'),
                changelog_file              : get_value(step_params, 'changelog_file', 'changelog/db.changelog-master.xml'),
                liquibase_url_creds_id      : get_value(step_params, 'liquibase_url_creds_id', 'LIQUIBASE_URL'),
                liquibase_username_creds_id : get_value(step_params, 'liquibase_username_creds_id', 'LIQUIBASE_USERNAME'),
                liquibase_password_creds_id : get_value(step_params, 'liquibase_password_creds_id', 'LIQUIBASE_PASSWORD'),
                use_docker                  : get_value(step_params, 'use_docker', false),
                liquibase_docker_image      : get_value(step_params, 'liquibase_docker_image', 'liquibase/liquibase:4.29'),
                fail_on_error               : true
            ]

            // ----------------------------------------------------------------
            //  STAGE 2 – Pipeline-specific stages
            // ----------------------------------------------------------------
            if (pipeline_type == 'pr-validation') {
                stage('Liquibase Validate') {
                    logger.logger(msg: 'Validating changelog syntax and structure', level: 'INFO')
                    liquibase.validate(liq_params)
                    logger.logger(msg: 'Changelog is VALID – PR can be merged', level: 'INFO')
                }

            } else if (pipeline_type == 'deploy') {
                stage('Liquibase Validate') {
                    logger.logger(msg: 'Pre-deploy validation', level: 'INFO')
                    liquibase.validate(liq_params)
                }

                stage('Liquibase Update') {
                    logger.logger(msg: 'Applying pending migrations to database', level: 'INFO')
                    liquibase.update(liq_params)
                    logger.logger(msg: 'Database migrations applied successfully', level: 'INFO')
                }

                // Optional: create/refresh baseline snapshot after deploy
                if (get_value(step_params, 'create_snapshot_after_deploy', false).toBoolean()) {
                    stage('Create Baseline Snapshot') {
                        withCredentials([
                            string(credentialsId: "${liq_params.liquibase_url_creds_id}",      variable: 'LIQUIBASE_URL'),
                            string(credentialsId: "${liq_params.liquibase_username_creds_id}", variable: 'LIQUIBASE_USERNAME'),
                            string(credentialsId: "${liq_params.liquibase_password_creds_id}", variable: 'LIQUIBASE_PASSWORD')
                        ]) {
                            dir("${WORKSPACE}/${liq_params.source_code_path}") {
                                sh """
                                    mkdir -p snapshots
                                    export LIQUIBASE_URL="\${LIQUIBASE_URL}"
                                    export LIQUIBASE_USERNAME="\${LIQUIBASE_USERNAME}"
                                    export LIQUIBASE_PASSWORD="\${LIQUIBASE_PASSWORD}"
                                    liquibase snapshot \\
                                        --snapshot-format=json \\
                                        --output-file=snapshots/baseline.snapshot.json
                                    echo "Baseline snapshot updated after deploy"
                                """
                            }
                        }
                    }
                }

            } else if (pipeline_type == 'drift-detection') {
                stage('Drift Detection') {
                    logger.logger(msg: 'Running drift detection', level: 'INFO')
                    liquibase.drift_detection(liq_params + [
                        snapshot_dir: get_value(step_params, 'snapshot_dir', 'snapshots'),
                        reports_dir : get_value(step_params, 'reports_dir', 'drift-reports')
                    ])
                }

                // Publish drift report as a build artifact if it was generated
                stage('Publish Drift Report') {
                    dir("${WORKSPACE}/${liq_params.source_code_path}") {
                        sh 'ls -la drift-reports/ || true'
                        archiveArtifacts artifacts: 'drift-reports/**', allowEmptyArchive: true
                    }
                }

            } else {
                error("Unknown pipeline_type: '${pipeline_type}'. Valid values: pr-validation | deploy | drift-detection")
            }

        } catch (Exception e) {
            currentBuild.result = 'FAILURE'
            logger.logger(msg: "Pipeline failed: ${e}", level: 'ERROR')

            if (get_value(step_params, 'notification_enabled', false).toBoolean()) {
                notify.notification_factory(
                    build_status          : 'Failure',
                    webhook_url_creds_id  : "${get_value(step_params, 'webhook_url_creds_id', '')}",
                    notification_channel  : "${get_value(step_params, 'notification_channel', '')}",
                    notification_enabled  : 'true'
                )
            }
            throw e

        } finally {
            if (get_value(step_params, 'notification_enabled', false).toBoolean()
                    && currentBuild.currentResult == 'SUCCESS') {
                notify.notification_factory(
                    build_status          : 'Success',
                    webhook_url_creds_id  : "${get_value(step_params, 'webhook_url_creds_id', '')}",
                    notification_channel  : "${get_value(step_params, 'notification_channel', '')}",
                    notification_enabled  : 'true'
                )
            }

            if (get_value(step_params, 'clean_workspace', true).toBoolean()) {
                workspace.workspace_management(
                    clean_workspace              : 'true',
                    ignore_clean_workspace_failure: 'false',
                    delete_dirs                  : 'true',
                    clean_when_build_aborted     : 'true',
                    clean_when_build_failed      : 'true',
                    clean_when_not_built         : 'true',
                    clean_when_build_succeed     : 'true',
                    clean_when_build_unstable    : 'true'
                )
            }
        }
    }
}
