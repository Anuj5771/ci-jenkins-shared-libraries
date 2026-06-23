package opstree.common

import opstree.common.*

def jira_factory(Map step_params) {
    logger = new logger()
    if (step_params.enable_jira == 'true') {
        if (step_params.enable_buildlogurl_in_jiracomment == 'true') {
            jira_comments_management(step_params)
        }
    }
  else {
        logger.logger('msg':'No valid option selected for Jira management. Please mention correct values.', 'level':'WARN')
  }
}

def jira_comments_management(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Dependency Check Scanning', 'level':'INFO')

    jenkins_jira_url_env_name = "${step_params.jenkins_jira_url_env_name}"
    jira_url = env[jenkins_jira_url_env_name]
    jenkins_jira_creds_id = "${step_params.jenkins_jira_creds_id}"
    jira_ticket_id = "${step_params.jira_ticket_id}"
    fail_job_if_jira_operation_failed = "${step_params.fail_job_if_jira_operation_failed}"
    build_status = "${step_params.build_status}"
    console_log_url = "${step_params.console_log_url}"

    try {
        if (jira_ticket_id?.trim()) {
            withCredentials([usernamePassword(credentialsId: jenkins_jira_creds_id, usernameVariable: 'jira_email', passwordVariable: 'jira_api_token')]) {
                sh """
                   curl -X POST -H "Content-Type: application/json" \
                   -u $jira_email:$jira_api_token \
                   --data '{"body": "Jenkins Pipeline ${env.JOB_NAME} Status: ${build_status}. View console log: ${console_log_url}"}' \
                   ${jira_url}/rest/api/2/issue/${jira_ticket_id}/comment
                   """
            }
            logger.logger('msg':'Ticket updated with build status and console log link.', 'level':'INFO')
           } else {
            logger.logger('msg':'No Jira issue key provided!', 'level':'WARN')
        }
    }
            catch (Exception e) {
        if (fail_job_if_jira_operation_failed == true) {
            logger.logger('msg':"JIRA comment update failed!!. Error Details: ${e}", 'level':'ERROR')
        }
                else
                {
            logger.logger('msg':"JIRA comment update failed!!. Error Details: ${e}. Ignoring error as per user input", 'level':'WARN')
                }
            }
}

