package opstree.python

import opstree.common.*

def dependency_install_factory(Map step_params) {
    logger = new logger()
    if (step_params.dependency_install_check == 'true') {
        dependency_install(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Installing Dependencies. Please mention correct values.', 'level':'WARN')
  }
}

def dependency_install(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Dependency Installing', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    repo_url_type = "${step_params.repo_url_type}"

    dependency_install_check = "${step_params.dependency_install_check}"
    fail_job_if_dependency_issue_detected = "${step_params.fail_job_if_dependency_issue_detected}"
    requirements_file_path = "${step_params.requirements_file_path}"
    python_version = "${step_params.python_version}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    sh "sudo mkdir -p ${WORKSPACE}/${repo_dir}/.pip"

            // This below line is temp fix.. Need to check alternative

    sh "sudo chmod 755 -R ${WORKSPACE}/${repo_dir}/.pip"

    if (fail_job_if_dependency_issue_detected == 'false') {
        try {
            sh "docker run --rm -v $WORKSPACE/${repo_dir}:/home --user root:root -w /home -e PIP_CACHE_DIR=/home/.pip -e PYTHONPATH=/home/.pip python:${python_version}-alpine pip3 install -r ${requirements_file_path} -t .pip --cache-dir .pip"
        }
                    catch (Exception e) {
            logger.logger('msg':'Dependency Installing found Issues!! Ignoring as per User inputs', 'level':'WARN')
                    }
    }
            else {
        try {
            sh "docker run --rm -v $WORKSPACE/${repo_dir}:/home --user root:root -w /home -e PIP_CACHE_DIR=/home/.pip -e PYTHONPATH=/home/.pip python:${python_version}-alpine pip3 install -r ${requirements_file_path} -t .pip --cache-dir .pip"
        }
                    catch (Exception e) {
            logger.logger('msg':"Dependency Installing found Issues!!! Dependency Installing Failed, Error Details: ${e}", 'level':'ERROR')
            error()
                    }
            }
}
