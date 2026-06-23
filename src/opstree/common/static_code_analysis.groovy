package opstree.common

import opstree.common.*

def static_code_analysis_factory(Map step_params) {
    logger = new logger()
    if (step_params.static_code_analysis_check == 'true') {
        sonar(step_params)
    }
    else {
        logger.logger('msg':'No valid option selected for static code analysis. Please mention correct values.', 'level':'WARN')
    }
}

def sonar(Map step_params) {
    stage('Static Code Analysis') {
        logger = new logger()
        parser = new parser()

        logger.logger('msg':'Performing Static Code Analysis ', 'level':'INFO')

        repo_url = "${step_params.repo_url}"
        repo_url_type = "${step_params.repo_url_type}"

        codebase_to_scan_directory = "${step_params.codebase_to_scan_directory}"
        static_code_analysis_check = "${step_params.static_code_analysis_check}"
        path_to_sonar_properties = "${step_params.path_to_sonar_properties}"
        fail_job_if_analysis_returned_exception = "${step_params.fail_job_if_analysis_returned_exception}"
        jenkins_sonarqube_token_creds_id = "${step_params.jenkins_sonarqube_token_creds_id}"
        app_stack = "${step_params.app_stack}"
        unit_testing_check = "${step_params.unit_testing_check}"
        withmaven_globaltool_jdk = "${step_params.withmaven_globaltool_jdk}"
        withmaven_globaltool_maven = "${step_params.withmaven_globaltool_maven}"
        source_code_path = "${step_params.source_code_path}"

        repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
        repo_dir = repo_dir + source_code_path

        if (fail_job_if_analysis_returned_exception == 'false') {
                try {
                        withCredentials([string(credentialsId: jenkins_sonarqube_token_creds_id , variable: 'SONAR_TOKEN')]) {
                            withSonarQubeEnv('SonarQube') {
                                sh "docker run --rm --user root -v $WORKSPACE/'${repo_dir}':/usr/src -v $WORKSPACE/${repo_dir}/.scannerwork:/tmp/.scannerwork  -e SONAR_TOKEN=${SONAR_TOKEN} -e CODEBASE_DIR=/usr/src/'${codebase_to_scan_directory}' -w /usr/src sonarsource/sonar-scanner-cli '-Dproject.settings=${path_to_sonar_properties}'"
                                logger.logger('msg':'Static Code Analysis Scanning Complete', 'level':'INFO')

                                sleep(60)
                                timeout(time: 1, unit: 'MINUTES') {
                                dir("${WORKSPACE}/${repo_dir}") {
                                    def qg = waitForQualityGate()
                                    echo 'Finished waiting'
                                    if (qg.status != 'OK') {
                                        logger.logger('msg': "Pipeline aborted due to quality gate failure: ${qg.status}. But Ignoring as per User input", 'level':'WARN')
                                    }
                                }
                                }
                            }
                        }
                }
                catch (Exception e) {
                    logger.logger('msg':'Static Code Analysis Scanning Failed!! Ignoring as per User inputs', 'level':'WARN')
                }
        }
        else {
                try {
                        withCredentials([string(credentialsId: jenkins_sonarqube_token_creds_id , variable: 'SONAR_TOKEN')]) {
                            withSonarQubeEnv('SonarQube') {
                                sh "docker run --rm --user root -v $WORKSPACE/'${repo_dir}':/usr/src -v $WORKSPACE/${repo_dir}/.scannerwork:/tmp/.scannerwork -e SONAR_TOKEN=${SONAR_TOKEN} -e CODEBASE_DIR=/usr/src/'${codebase_to_scan_directory}' -w /usr/src  sonarsource/sonar-scanner-cli '-Dproject.settings=${path_to_sonar_properties}'"

                                logger.logger('msg':'Static Code Analysis Scanning Complete', 'level':'INFO')
                                sleep(60)
                                timeout(time: 1, unit: 'MINUTES') {
                                    dir("${WORKSPACE}/${repo_dir}") {
                                        def qg = waitForQualityGate()
                                        echo 'Finished waiting'
                                        if (qg.status != 'OK') {
                                            logger.logger('msg': "Pipeline aborted due to quality gate failure: ${qg.status}.", 'level':'ERROR')
                                        }
                                    }
                                }
                            }
                        }
                }
                catch (Exception e) {
                    logger.logger('msg':"Static Code Analysis Scanning Failed. Error Details: ${e}", 'level':'ERROR')
                    error()
                }
        }
    }
}
