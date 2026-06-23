package opstree.common

import opstree.common.build_dockerfile

import opstree.common.*

def build_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_build_dockerfile == 'true') {
        build_dockerfile(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Building Dockerfile. Please mention correct values.', 'level':'WARN')
  }
}

def build_dockerfile(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Docker Build Step', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    dockerfile_context = "${step_params.dockerfile_context}"
    dockerfile_location = "${step_params.dockerfile_location}"

    if (dockerfile_context != null) {
        dockerfile_context = "${WORKSPACE}" + '/' + "${repo_dir}" + dockerfile_context
    }
    else {
        dockerfile_context = "${WORKSPACE}" + '/' + "${repo_dir}"
    }

    if (dockerfile_location != '') {
        dockerfile_location = "${WORKSPACE}" + '/' + "${repo_dir}" + dockerfile_location
    }
    else {
        dockerfile_location = 'Dockerfile'
    }

    image_name = "${step_params.image_name}"
    static_code_analysis_check = "${step_params.static_code_analysis_check}"
    app_stack = "${step_params.app_stack}"
    source_code_path = "${step_params.source_code_path}"
    repo_dir = repo_dir + source_code_path
    codeartifact_dependency = "${step_params.codeartifact_dependency}"
    codeartifact_domain = "${step_params.codeartifact_domain}"
    codeartifact_owner = "${step_params.codeartifact_owner}"

    dir("${WORKSPACE}/${repo_dir}") {
            if (codeartifact_dependency == 'true') {
            withAWS() {
                def codeArtifactToken = sh(
                        script: """
                        aws codeartifact get-authorization-token --domain ${codeartifact_domain} --domain-owner ${codeartifact_owner} --query authorizationToken --output text
                        """,
                        returnStdout: true
                    ).trim()

                    // Export the token as an environment variable
                    def CODEARTIFACT_AUTH_TOKEN = codeArtifactToken
                    sh """
                        git config --global --add safe.directory ${WORKSPACE}/${repo_dir} && \
                        COMMIT_HASH=\$(git rev-parse --short HEAD) && \
                        docker build --build-arg CODEARTIFACT_AUTH_TOKEN=${CODEARTIFACT_AUTH_TOKEN} -f ${dockerfile_location} -t ${image_name}:\${COMMIT_HASH} ${dockerfile_context} && \
                        docker tag ${image_name}:\${COMMIT_HASH} ${image_name}:latest
                    """
            }
            }

            else {
                    sh """
                        git config --global --add safe.directory ${WORKSPACE}/${repo_dir} && \
                        COMMIT_HASH=\$(git rev-parse --short HEAD) && \
                        docker build -f ${dockerfile_location} -t ${image_name}:\${COMMIT_HASH} ${dockerfile_context} && \
                        docker tag ${image_name}:\${COMMIT_HASH} ${image_name}:latest
                    """
            }

            logger.logger('msg':'Docker Build successful', 'level':'INFO')

            repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    }
}
