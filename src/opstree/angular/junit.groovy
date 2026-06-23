package opstree.java

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
    build_tool = "${step_params.build_tool}"
    unit_test_reports_path = "${step_params.unit_test_reports_path}"
    findbugs_test_report_path = "${step_params.findbugs_test_report_path}"
    withmaven_globaltool_jdk = "${step_params.withmaven_globaltool_jdk}"
    withmaven_globaltool_maven = "${step_params.withmaven_globaltool_maven}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    // dir("${WORKSPACE}/${repo_dir}") {
    //         if (fail_job_if_unit_issue_detected == 'false') {
    //             if (build_tool == 'maven') {
    //                 try {
    //                     withMaven(globalMavenSettingsConfig: '', jdk: "${withmaven_globaltool_jdk}", maven: "${withmaven_globaltool_maven}", mavenSettingsConfig: '') {
    //                         sh ''' echo ${WORKSPACE} '''
    //                         sh """ docker run --rm \
    //                              -v ${WORKSPACE}/${repo_dir}/src:/app/src \
    //                              -v ${WORKSPACE}/${repo_dir}/pom.xml:/app/pom.xml \
    //                              -v ${WORKSPACE}/${repo_dir}/target:/app/target \
    //                              -w /app \
    //                              maven:3.8.6-jdk-11 \
    //                              mvn test """
    //                         reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "${unit_test_reports_path}", findbugs_test_report_path: "${findbugs_test_report_path}")
    //                     }
    //                 }
    //                 catch (Exception e) {
    //                         logger.logger('msg':'Unit Test found Issues!! Ignoring as per User inputs', 'level':'WARN')
    //                 }
    //             }
    //             else {
    //                     logger.logger('msg':"Choose appropriate build tool !!! Unit Testing Failed Error Details: ${e}", 'level':'ERROR')
    //                     error()
    //             }
    //         }
    //         else {
    //             if (build_tool == 'maven') {
    //                 try {
    //                 withMaven(globalMavenSettingsConfig: '', jdk: "${withmaven_globaltool_jdk}", maven: "${withmaven_globaltool_maven}", mavenSettingsConfig: '') {
    //                         sh """ docker run --rm \
    //                              -v ${WORKSPACE}/${repo_dir}/src:/app/src \
    //                              -v ${WORKSPACE}/${repo_dir}/pom.xml:/app/pom.xml \
    //                              -v ${WORKSPACE}/${repo_dir}/target:/app/target \
    //                              -w /app \
    //                              maven:3.8.6-jdk-11 \
    //                              mvn test """
    //                     reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "${unit_test_reports_path}", findbugs_test_report_path: "${findbugs_test_report_path}")
    //                 }
    //                 }
    //                 catch (Exception e) {
    //                     logger.logger('msg':"Unit Test found Issues!!! Unit Testing Failed Error Details: ${e}", 'level':'ERROR')
    //                     error()
    //                 }
    //             }
    //             else {
    //                 logger.logger('msg':"Choose appropriate build tool !!! Unit Testing Failed  Error Details: ${e}", 'level':'ERROR')
    //                 error()
    //             }
    //         }
    // }

    dir("${WORKSPACE}/${repo_dir}") {
            if (fail_job_if_unit_issue_detected == 'false') {
                if (build_tool == 'maven') {
                    try {
                        withMaven(globalMavenSettingsConfig: '', jdk: "${withmaven_globaltool_jdk}", maven: "${withmaven_globaltool_maven}", mavenSettingsConfig: '') {
                            sh ''' echo ${WORKSPACE} '''
                            sh """ docker run --rm \
                                 -v ${WORKSPACE}/${repo_dir}:/app \
                                 -w /app \
                                 maven:3.8.6-jdk-11 \
                                 mvn test """
                            reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "${unit_test_reports_path}", findbugs_test_report_path: "${findbugs_test_report_path}")
                        }
                    }
                    catch (Exception e) {
                            logger.logger('msg':'Unit Test found Issues!! Ignoring as per User inputs', 'level':'WARN')
                    }
                }
                else {
                        logger.logger('msg':"Choose appropriate build tool !!! Unit Testing Failed Error Details: ${e}", 'level':'ERROR')
                        error()
                }
            }
            else {
                if (build_tool == 'maven') {
                    try {
                    withMaven(globalMavenSettingsConfig: '', jdk: "${withmaven_globaltool_jdk}", maven: "${withmaven_globaltool_maven}", mavenSettingsConfig: '') {
                            sh """ docker run --rm \
                                 -v ${WORKSPACE}/${repo_dir}:/app \
                                 -w /app \
                                 maven:3.8.6-jdk-11 \
                                 mvn test """
                        reports_manager.publish_static_code_analysis_issues(unit_test_reports_path: "${unit_test_reports_path}", findbugs_test_report_path: "${findbugs_test_report_path}")
                    }
                    }
                    catch (Exception e) {
                        logger.logger('msg':"Unit Test found Issues!!! Unit Testing Failed Error Details: ${e}", 'level':'ERROR')
                        error()
                    }
                }
                else {
                    logger.logger('msg':"Choose appropriate build tool !!! Unit Testing Failed  Error Details: ${e}", 'level':'ERROR')
                    error()
                }
            }
    }
}
