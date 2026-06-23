package opstree.common

def git_checkout(Map step_params) {
    def logger = new opstree.common.logger()
    def parser = new parser()

    logger.logger(msg:'Performing Git Checkout/Fetch!!', level:'INFO')

    def repo_url = "${step_params.repo_url}"
    def repo_branch = "${step_params.repo_branch}"
    def repo_url_type = "${step_params.repo_url_type}"
    def ssh_private_key_location = "${step_params.ssh_private_key_location}"
    def jenkins_git_ssh_key_id = "${step_params.jenkins_git_ssh_key_id}"
    def jenkins_git_creds_id = "${step_params.jenkins_git_creds_id}"
    def clean_workspace = "${step_params.clean_workspace}"
    sh "echo ${repo_url}"
    // sh "echo ${repo_dir}"

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    sh "echo ${repo_dir}"

    def scriptContent = libraryResource'fetch_source_code.sh'
    writeFile file: 'fetch_source_code.sh', text: scriptContent
    sh 'chmod +x fetch_source_code.sh'

    if (step_params.repo_url_type == 'ssh' && step_params.ssh_private_key_location != 'null') {
        try {
            if (step_params.clean_workspace == true) {
                sh "docker run --rm --entrypoint /git/fetch_source_code.sh -v ${ssh_private_key_location}:/root -e PRIVATE_KEY_LOCATION=/root/* -e GIT_SSH_URL=${repo_url} -e BRANCH_NAME=${repo_branch} -v $WORKSPACE:/git alpine/git sh"
            }
                else {
                sh "docker run --rm --entrypoint /git/fetch_source_code.sh -v ${ssh_private_key_location}:/root -e PRIVATE_KEY_LOCATION=/root/* -e GIT_SSH_URL=${repo_url} -e BRANCH_NAME=${repo_branch} -e GIT_FETCH=true -e REPO_DIR=${repo_dir} -v $WORKSPACE/:/git alpine/git sh"
                }
        }
            catch (Exception e) {
            logger.logger(['msg':"Git Checkout Failed Error Details: ${e}", 'level':'ERROR'])
            throw e
            }
    }

        else if (step_params.repo_url_type == 'ssh' && step_params.jenkins_git_ssh_key_id != 'null') {
        withCredentials([sshUserPrivateKey(credentialsId: jenkins_git_ssh_key_id, keyFileVariable: 'private_key', passphraseVariable: '', usernameVariable: 'USERNAME')]) {
                try {
                    if (step_params.clean_workspace == true) {
                        sh "docker run --rm --entrypoint /git/fetch_source_code.sh -v $WORKSPACE@tmp/secretFiles:/root -e PRIVATE_KEY=${private_key} -e GIT_SSH_URL=${repo_url} -e BRANCH_NAME=${repo_branch} -v $WORKSPACE:/git alpine/git sh"
                    }
                  else {
                        sh "docker run --rm --entrypoint /git/fetch_source_code.sh -v $WORKSPACE@tmp/secretFiles:/root -e PRIVATE_KEY=${private_key} -e GIT_SSH_URL=${repo_url} -e BRANCH_NAME=${repo_branch} -e GIT_FETCH=true -e REPO_DIR=${repo_dir} -v $WORKSPACE/:/git alpine/git sh"
                  }
                }
            catch (Exception e) {
                    logger.logger(['msg':"Git Checkout Failed Error Details: ${e}", 'level':'ERROR'])
                    throw e
            }
        }
        }

        else if (step_params.repo_url_type == 'http' && step_params.jenkins_git_creds_id != 'null') {
        withCredentials([usernamePassword(credentialsId: jenkins_git_creds_id, passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                try {
                    if (step_params.clean_workspace == 'true') {
                        sh "docker run --rm --entrypoint /git/fetch_source_code.sh -e GIT_USERNAME=${USERNAME} -e GIT_PASSWORD=${PASSWORD} -e BRANCH_NAME=${repo_branch} -e GIT_HTTPS_URL=${repo_url} -v $WORKSPACE:/git alpine/git sh"
                        sh "sudo chown -R jenkins:jenkins ${WORKSPACE}"
                    }
                  else {
                        sh "docker run --rm -v $WORKSPACE/'${repo_dir}':/git --entrypoint sh alpine/git -c 'git fetch --all && git reset --hard origin/${repo_branch}'"
                        sh "sudo chown -R jenkins:jenkins ${WORKSPACE}"
                  }
                }
            catch (Exception e) {
                    logger.logger(['msg':"Git Checkout Failed Error Details: ${e}", 'level':'ERROR'])
                    throw e
            }
        }
        }

        else {
        sh 'exit 1'
        logger.logger(['msg':"Incorrect values. Please provide correct values Error Details: ${e}", 'level':'ERROR'])
        }
}
