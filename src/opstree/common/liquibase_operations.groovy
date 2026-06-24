package opstree.common

// ---------------------------------------------------------------------------
//  liquibase_operations.groovy
//  Shared-library module for all Liquibase CLI commands.
//
//  Assumptions:
//    • Liquibase binary is already installed on the Jenkins agent (or available
//      via Docker – see `use_docker` flag).
//    • DB credentials are stored as Jenkins Credentials of type
//      "Secret text" or "Username with password".
//    • liquibase.properties in the repo reads env vars:
//        url:      ${LIQUIBASE_URL}
//        username: ${LIQUIBASE_USERNAME}
//        password: ${LIQUIBASE_PASSWORD}
// ---------------------------------------------------------------------------

def liquibase_factory(Map step_params) {
    def logger = new opstree.common.logger()

    // ---- resolve parameters with defaults -----------------------------------
    def command              = get_value(step_params, 'command', 'validate')
    def source_code_path     = get_value(step_params, 'source_code_path', '.')
    def changelog_file       = get_value(step_params, 'changelog_file', 'changelog/db.changelog-master.xml')
    def liquibase_url_creds_id      = get_value(step_params, 'liquibase_url_creds_id', null)
    def liquibase_username_creds_id = get_value(step_params, 'liquibase_username_creds_id', null)
    def liquibase_password_creds_id = get_value(step_params, 'liquibase_password_creds_id', null)
    def extra_args           = get_value(step_params, 'extra_args', '')
    def fail_on_error        = get_value(step_params, 'fail_on_error', true)
    def use_docker           = get_value(step_params, 'use_docker', false)
    def liquibase_docker_image = get_value(step_params, 'liquibase_docker_image', 'liquibase/liquibase:4.29')

    logger.logger(msg: "Running Liquibase command: ${command}", level: 'INFO')

    try {
        // ---- inject credentials as env vars ---------------------------------
        withCredentials([
            string(credentialsId: "${liquibase_url_creds_id}",      variable: 'LIQUIBASE_URL'),
            string(credentialsId: "${liquibase_username_creds_id}", variable: 'LIQUIBASE_USERNAME'),
            string(credentialsId: "${liquibase_password_creds_id}", variable: 'LIQUIBASE_PASSWORD')
        ]) {
            dir("${WORKSPACE}/${source_code_path}") {
                if (use_docker.toBoolean()) {
                    // ---- run via Docker (agent doesn't need Liquibase installed) ---
                    sh """
                        docker run --rm \\
                          -e LIQUIBASE_URL="\${LIQUIBASE_URL}" \\
                          -e LIQUIBASE_USERNAME="\${LIQUIBASE_USERNAME}" \\
                          -e LIQUIBASE_PASSWORD="\${LIQUIBASE_PASSWORD}" \\
                          -v "\${PWD}":/liquibase/changelog \\
                          -w /liquibase/changelog \\
                          ${liquibase_docker_image} \\
                          --changelog-file=${changelog_file} \\
                          ${command} ${extra_args}
                    """
                } else {
                    // ---- run native Liquibase binary -------------------------
                    sh """
                        export LIQUIBASE_URL="\${LIQUIBASE_URL}"
                        export LIQUIBASE_USERNAME="\${LIQUIBASE_USERNAME}"
                        export LIQUIBASE_PASSWORD="\${LIQUIBASE_PASSWORD}"
                        liquibase ${command} ${extra_args}
                    """
                }
            }
        }
        logger.logger(msg: "Liquibase ${command} completed successfully", level: 'INFO')
    } catch (Exception e) {
        logger.logger(msg: "Liquibase ${command} failed: ${e}", level: 'ERROR')
        if (fail_on_error.toBoolean()) {
            throw e
        } else {
            logger.logger(msg: "fail_on_error=false – continuing despite error", level: 'WARN')
        }
    }
}

// ---------------------------------------------------------------------------
//  Convenience wrappers – call these directly from a Jenkinsfile or template
// ---------------------------------------------------------------------------

def validate(Map step_params) {
    step_params.command = 'validate'
    liquibase_factory(step_params)
}

def update(Map step_params) {
    step_params.command = 'update'
    liquibase_factory(step_params)
}

def status(Map step_params) {
    step_params.command    = 'status'
    def extra_args = get_value(step_params, 'extra_args', '--verbose')
    step_params.extra_args = extra_args
    liquibase_factory(step_params)
}

def drift_detection(Map step_params) {
    def logger = new opstree.common.logger()
    def source_code_path = get_value(step_params, 'source_code_path', '.')
    def snapshot_dir     = get_value(step_params, 'snapshot_dir', 'snapshots')
    def reports_dir      = get_value(step_params, 'reports_dir', 'drift-reports')

    // ---- step 1: show pending changesets ------------------------------------
    logger.logger(msg: 'Checking pending changesets (liquibase status)', level: 'INFO')
    step_params.command    = 'status'
    step_params.extra_args = '--verbose'
    liquibase_factory(step_params)

    // ---- step 2: diff against baseline snapshot (if it exists) -------------
    withCredentials([
        string(credentialsId: "${step_params.liquibase_url_creds_id}",      variable: 'LIQUIBASE_URL'),
        string(credentialsId: "${step_params.liquibase_username_creds_id}", variable: 'LIQUIBASE_USERNAME'),
        string(credentialsId: "${step_params.liquibase_password_creds_id}", variable: 'LIQUIBASE_PASSWORD')
    ]) {
        dir("${WORKSPACE}/${source_code_path}") {
            sh """
                mkdir -p ${reports_dir} ${snapshot_dir}

                if [ -f "${snapshot_dir}/baseline.snapshot.json" ]; then
                    echo "=== Comparing live DB against baseline snapshot ==="
                    export LIQUIBASE_URL="\${LIQUIBASE_URL}"
                    export LIQUIBASE_USERNAME="\${LIQUIBASE_USERNAME}"
                    export LIQUIBASE_PASSWORD="\${LIQUIBASE_PASSWORD}"
                    liquibase diff \\
                        --reference-url="offline:postgresql?snapshot=${snapshot_dir}/baseline.snapshot.json" \\
                        > ${reports_dir}/drift.txt 2>&1 || true
                    cat ${reports_dir}/drift.txt
                    echo "Drift report saved to ${reports_dir}/drift.txt"
                else
                    echo "=== No baseline snapshot found. Run deploy first to create one. ==="
                fi
            """
        }
    }
}

// ---------------------------------------------------------------------------
//  Helper
// ---------------------------------------------------------------------------
def get_value(Map map, String key, Object defaultVal) {
    return map.containsKey(key) ? map.get(key) : defaultVal
}
