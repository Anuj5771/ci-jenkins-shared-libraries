package opstree.common

import opstree.common.*

def dependency_scanning_factory(Map step_params) {
    def logger = new logger()
    if (step_params.dependency_check == 'true') {
        dependency_scan(step_params)
    } else {
        logger.logger('msg':'No valid option selected for dependency scanning. Please mention correct values.', 'level':'WARN')
    }
}

def dependency_scan(Map step_params) {
    def logger                    = new logger()
    def parser                    = new parser()
    def dependency_check_reports  = new reports_management()

    logger.logger('msg':'Performing Dependency Check Scanning', 'level':'INFO')

    def repo_url                                  = "${step_params.repo_url}"
    def repo_url_type                             = "${step_params.repo_url_type}"
    def dependency_check                          = "${step_params.dependency_check}"
    def dependency_scan_tool                      = "${step_params.dependency_scan_tool}"
    def fail_job_if_dependency_returned_exception = "${step_params.fail_job_if_dependency_returned_exception}"
    def nvd_api_key_creds_id                      = "${step_params.nvd_api_key_creds_id ?: ''}"

    def owasp_project_name  = "${step_params.owasp_project_name}"
    def owasp_report_format = "${step_params.owasp_report_format}"
    def owasp_report_publish= "${step_params.owasp_report_publish}"
    def owasp_version       = 'latest'
    def source_code_path    = "${step_params.source_code_path ?: ''}"
    def app_stack           = "${step_params.app_stack ?: ''}"
    def pom_location        = "${step_params.pom_location ?: ''}"

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    // ── Resolve the directory to scan ───────────────────────────────────────
    // source_code_path is relative to WORKSPACE (e.g. "/spring-boot-realworld-example-app").
    // Do NOT append it to repo_dir — that creates a non-existent double directory.
    // Rule:
    //   • If source_code_path is set → mount WORKSPACE + source_code_path
    //   • Otherwise               → mount WORKSPACE/repo_dir (the git checkout root)
    def scanHostDir
    if (source_code_path?.trim()) {
        // Strip any leading slash to avoid double-slash, then rejoin cleanly
        scanHostDir = "${WORKSPACE}/${source_code_path.replaceAll('^/', '')}"
    } else {
        scanHostDir = "${WORKSPACE}/${repo_dir}"
    }

    logger.logger('msg':"OWASP scan source directory: ${scanHostDir}", 'level':'INFO')

    if (dependency_scan_tool == 'owasp') {
        sh "mkdir -p ${WORKSPACE}/owasp-reports"
        sh "mkdir -p ${JENKINS_HOME}/owasp-data/cache"
        sh "sudo chmod -R 777 ${WORKSPACE}/owasp-reports ${JENKINS_HOME}/owasp-data"

        // Remove stale lock files left by OOM-killed containers from previous runs
        sh "find ${JENKINS_HOME}/owasp-data -name '*.lock' -delete 2>/dev/null || true"

        // ── DB freshness check ───────────────────────────────────────────────
        // Use --noupdate when the DB was updated < 24h ago to avoid
        // 20+ min NVD downloads on every build.
        def noUpdateFlag = sh(script: """
            DB_FILE="${JENKINS_HOME}/owasp-data/odc.mv.db"
            if [ -f "\$DB_FILE" ]; then
                AGE_HOURS=\$(( ( \$(date +%s) - \$(stat -c %Y "\$DB_FILE" 2>/dev/null || echo 0) ) / 3600 ))
                if [ "\$AGE_HOURS" -lt "24" ] 2>/dev/null; then
                    echo "--noupdate"
                else
                    echo ""
                fi
            else
                echo ""
            fi
        """, returnStdout: true).trim()

        if (noUpdateFlag == '--noupdate') {
            logger.logger('msg':'OWASP DB is fresh (< 24h old) - skipping NVD update for faster scan', 'level':'INFO')
        } else {
            logger.logger('msg':'OWASP DB is stale or missing - downloading NVD updates', 'level':'INFO')
        }

        // ── Build base docker command ────────────────────────────────────────
        def owaspDockerBase = "docker run --rm --memory=4g --memory-swap=4g"
        def owaspDataVol    = "-v ${JENKINS_HOME}/owasp-data:/usr/share/dependency-check/data:z"
        def owaspReportsVol = "-v ${WORKSPACE}/owasp-reports:/reports:z"
        def owaspSrcVol     = "-v ${scanHostDir}:/src:z"

        // ── Helper closure: run the actual scan with optional NVD key ────────
        def runOwaspScan = { String extraFlags ->
            def experimentalFlag = (app_stack == 'python' || app_stack == 'angular') ? '--enableExperimental' : ''
            if (nvd_api_key_creds_id?.trim()) {
                withCredentials([string(credentialsId: "${nvd_api_key_creds_id}", variable: 'NVD_API_KEY')]) {
                    def owaspScanArgs = "--format ALL --project '${owasp_project_name}' --out /reports ${extraFlags} --nvdApiKey \${NVD_API_KEY} ${experimentalFlag}"
                    sh "${owaspDockerBase} ${owaspSrcVol} ${owaspDataVol} ${owaspReportsVol} owasp/dependency-check:${owasp_version} --scan /src ${owaspScanArgs}"
                }
            } else {
                def owaspScanArgs = "--format ALL --project '${owasp_project_name}' --out /reports ${extraFlags} ${experimentalFlag}"
                sh "${owaspDockerBase} ${owaspSrcVol} ${owaspDataVol} ${owaspReportsVol} owasp/dependency-check:${owasp_version} --scan /src ${owaspScanArgs}"
            }
        }

        try {
            // First attempt — use freshness flag (may be --noupdate or empty)
            runOwaspScan(noUpdateFlag)

        } catch (Exception e) {
            // ── Exit code 13: corrupt / incompatible DB ──────────────────────
            // Happens when the odc.mv.db schema is from an older dependency-check
            // version and --noupdate prevents the automatic migration.
            // Fix: purge the data directory and retry with a full NVD update.
            if (e.message?.contains('exit code 13')) {
                logger.logger('msg':'OWASP DB is corrupt or incompatible (exit 13) - purging data directory and retrying with full update', 'level':'WARN')
                sh "sudo rm -rf ${JENKINS_HOME}/owasp-data && mkdir -p ${JENKINS_HOME}/owasp-data/cache && sudo chmod -R 777 ${JENKINS_HOME}/owasp-data"
                try {
                    // Retry without --noupdate so the fresh DB is downloaded
                    runOwaspScan('')
                } catch (Exception retryEx) {
                    if (fail_job_if_dependency_returned_exception == 'true') {
                        logger.logger('msg':"Dependency Scanning Failed after purge+retry: ${retryEx}", 'level':'ERROR')
                        throw retryEx
                    } else {
                        logger.logger('msg':"Dependency Scanning Failed after purge+retry: [IGNORING] ${retryEx}", 'level':'WARN')
                    }
                }
            } else {
                if (fail_job_if_dependency_returned_exception == 'true') {
                    logger.logger('msg':"Dependency Scanning Failed: ${e}", 'level':'ERROR')
                    throw e
                } else {
                    logger.logger('msg':"Dependency Scanning Failed: [IGNORING] ${e}", 'level':'WARN')
                }
            }
        }

        if (owasp_report_publish == 'true') {
            logger.logger('msg':'Publishing OWASP Dependency Scan Report', 'level':'INFO')
            dependency_check_reports.publish(
                'dc_publisher' : 'true',
                'report_dir'   : "${WORKSPACE}/owasp-reports",
                'report_file'  : "dependency-check-report.${owasp_report_format}",
                'report_name'  : 'OWASP Dependency Check Report'
            )
        } else {
            logger.logger('msg':'OWASP Report Publishing Skipped', 'level':'INFO')
        }

    } else if (dependency_scan_tool == 'fossa') {
        echo 'Fossa will be added soon'
    } else {
        logger.logger('msg':"No valid option was selected for scanning tool.", 'level':'ERROR')
    }
}
