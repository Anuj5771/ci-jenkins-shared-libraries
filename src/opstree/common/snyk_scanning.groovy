package opstree.common

import opstree.common.*

def snyk_security_scanning_factory(Map step_params) {
    logger = new logger()
    if (step_params.snyk_security_check == 'true') {
        snyk_security_scan(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for snyk_security scanning. Please mention correct values.', 'level':'WARN')
  }
}

def snyk_security_scan(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Snyk Security Check Scanning', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    repo_url_type = "${step_params.repo_url_type}"

    snyk_security_check = "${step_params.snyk_security_check}"
    fail_job_if_snyk_security_returned_exception = "${step_params.fail_job_if_snyk_security_returned_exception}"
    snyk_report_format = "${step_params.snyk_report_format}"
    snyk_api_creds_id = "${step_params.snyk_api_creds_id}"
    language_codebase_for_snyk_check = "${step_params.language_codebase_for_snyk_check}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    sh "mkdir -p ${WORKSPACE}/snyk-reports"

        // This below line is temp fix.. Need to check alternative

    sh "sudo chmod -R 777 ${WORKSPACE}/snyk-reports "

    if (fail_job_if_snyk_security_returned_exception == true) {
        try {
            withCredentials([string(credentialsId: snyk_api_creds_id , variable: 'SNYK_TOKEN')]) {
                    sh "docker run --rm --env SNYK_TOKEN=${SNYK_TOKEN} -v $WORKSPACE/'${repo_dir}':/app -v $WORKSPACE/snyk-reports:/home/snyk-reports snyk/snyk:'${language_codebase_for_snyk_check}' snyk test --allow-missing --'${snyk_report_format}'-file-output=/home/snyk-reports/snyk_report.'${snyk_report_format}'"
            }
        }
                catch (Exception e) {
            logger.logger('msg':"Snyk Security Scanning Failed Error Details: ${e}", 'level':'ERROR')
                }
    }

            else {
        try {
            withCredentials([string(credentialsId: snyk_api_creds_id , variable: 'SNYK_TOKEN')]) {
                    sh "docker run --rm --env SNYK_TOKEN=${SNYK_TOKEN} -v $WORKSPACE/'${repo_dir}':/app -v $WORKSPACE/snyk-reports:/home/snyk-reports snyk/snyk:'${language_codebase_for_snyk_check}' snyk test --allow-missing --'${snyk_report_format}'-file-output=/home/snyk-reports/snyk_report.'${snyk_report_format}'"
            }
        }
                catch (Exception e) {
            logger.logger('msg':"Snyk Security Scanning Failed: [IGNORING] ${e}", 'level':'WARN')
                }
            }
}
