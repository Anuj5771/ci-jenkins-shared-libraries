package opstree.common

import opstree.common.*

def creds_scanning_factory(Map step_params) {
    def logger = new logger()
    if (step_params.gitleaks_check == 'true') {
        gitleaks(step_params)
    } else {
        logger.logger('msg':'No valid option selected for creds scanning. Please mention correct values.', 'level':'WARN')
    }
}

def gitleaks(Map step_params) {
    def logger  = new logger()
    def parser  = new parser()

    logger.logger('msg':'Performing Credentials Scanning', 'level':'INFO')

    def repo_url                        = "${step_params.repo_url}"
    def repo_url_type                   = "${step_params.repo_url_type}"
    def gitleaks_report                 = new reports_management()
    def gitleaks_check                  = "${step_params.gitleaks_check}"
    def gitleaks_report_format          = "${step_params.gitleaks_report_format}"
    def gitleaks_report_jenkins_publish = "${step_params.gitleaks_report_jenkins_publish}"
    def fail_job_if_leak_detected       = "${step_params.fail_job_if_leak_detected}"

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    dir("${WORKSPACE}/gitleaks") {
        sh "sudo chmod -R 777 ${WORKSPACE}/gitleaks"
        if (fail_job_if_leak_detected == 'false') {
            try {
                sh "docker run --rm -v ${WORKSPACE}/'${repo_dir}':/path -v ${WORKSPACE}/gitleaks:/home/gitleaks/reports zricethezav/gitleaks:v8.16.3 detect --source='/path' --report-format=${gitleaks_report_format} --report-path='/home/gitleaks/reports/gitleaks_report.${gitleaks_report_format}' --exit-code 0"
            } catch (Exception e) {
                logger.logger('msg':'Leaks Found!! Ignoring as per User inputs', 'level':'WARN')
            }
        } else {
            try {
                sh "docker run --rm -v ${WORKSPACE}/'${repo_dir}':/path -v ${WORKSPACE}/gitleaks:/home/gitleaks/reports zricethezav/gitleaks:v8.16.3 detect --source='/path' --report-format=${gitleaks_report_format} --report-path='/home/gitleaks/reports/gitleaks_report.${gitleaks_report_format}' --exit-code 1"
            } catch (Exception e) {
                logger.logger('msg':"Leaks Found!!! Git Creds Scanning Failed Error Details: ${e}", 'level':'ERROR')
                error()
            }
        }
    }

    if (gitleaks_report_jenkins_publish == 'true') {
        try {
            gitleaks_report.publish('report_dir':"${WORKSPACE}/gitleaks", 'report_file':"gitleaks_report.${gitleaks_report_format}", 'report_name':'Gitleaks Report')
        } catch (Exception e) {
            logger.logger('msg':"Gitleaks Report Publish Failed Error Details: ${e}", 'level':'ERROR')
            throw e
        }
    } else {
        logger.logger('msg':'Gitleaks Report Publish Jenkins Skipped.', 'level':'INFO')
    }
}
