package opstree.python

import opstree.common.*

def unit_testing_factory(Map step_params) {
    logger = new logger()
    if (step_params.unit_testing_check == 'true') {
        unit_test(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Unit Testing. Please mention correct values.', 'level':'WARN')
  }
}

def unit_test(Map step_params) {
    logger = new logger()
    parser = new parser()
    reports_manager = new reports_management()

    logger.logger('msg':'Performing Unit Tests', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    repo_url_type = "${step_params.repo_url_type}"

    unit_testing_check = "${step_params.unit_testing_check}"
    fail_job_if_unit_issue_detected = "${step_params.fail_job_if_unit_issue_detected}"
    unit_test_reports_path = "${step_params.unit_test_reports_path}"
    coverage_report_path = "${step_params.coverage_report_path}"
    python_version = "${step_params.python_version}"

    sh "mkdir -p ${WORKSPACE}/pytest-reports"

            // This below line is temp fix.. Need to check alternative

    sh "sudo chmod -R 777 ${WORKSPACE}/pytest-reports"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    if (fail_job_if_unit_issue_detected == 'false') {
        try {
            sh "docker run --rm -v $WORKSPACE/${repo_dir}:/home -v ${WORKSPACE}/pytest-reports:/reports -e PIP_CACHE_DIR=/home/.pip -e PYTHONPATH=/home/.pip -w /home python:${python_version}-alpine sh -c '.pip/bin/pytest --cov=/home --cov-report xml:/reports/\"${coverage_report_path}\" --junitxml=/reports/\"${unit_test_reports_path}\"'"
            reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "**/pytest-reports/${unit_test_reports_path}")
            reports_manager.publish_code_coverage(coverage_report_path: "**/pytest-reports/${coverage_report_path}")
        }
                    catch (Exception e) {
            logger.logger('msg':'Unit Test found Issues!! Ignoring as per User inputs', 'level':'WARN')
                    }
    }
            else {
        try {
            sh "docker run --rm -v $WORKSPACE/${repo_dir}:/home -v ${WORKSPACE}/pytest-reports:/reports -e PIP_CACHE_DIR=/home/.pip -e PYTHONPATH=/home/.pip -w /home python:${python_version}-alpine sh -c '.pip/bin/pytest --cov=/home --cov-report xml:/reports/\"${coverage_report_path}\" --junitxml=/reports/\"${unit_test_reports_path}\"'"
            reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "**/pytest-reports/${unit_test_reports_path}")
            reports_manager.publish_code_coverage(coverage_report_path: "**/pytest-reports/${coverage_report_path}")
        }
                    catch (Exception e) {
            logger.logger('msg':"Unit Test found Issues!!! Unit Testing Failed, Error Details: ${e} ", 'level':'ERROR')
            error()
                    }
            }
}

