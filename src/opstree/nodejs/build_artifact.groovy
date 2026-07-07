package opstree.nodejs

import opstree.common.*

def build_factory(Map step_params) {
    def logger = new logger()
    if (step_params.perform_code_build == 'true' || step_params.perform_code_build == true) {
        build_artifact(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Building Artifact. Please mention correct values.', 'level':'WARN')
  }
}

def build_artifact(Map step_params) {
    def logger = new logger()
    def parser = new parser()

    logger.logger('msg':'Performing Build Step for Node.js', 'level':'INFO')

    def repo_url = "${step_params.repo_url}"
    def source_code_path = "${step_params.source_code_path}"
    def node_version = step_params.node_version ?: "18"

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    def default_build_command = "npm install && npm run build"
    def build_command = step_params.build_command ?: default_build_command

    dir("${WORKSPACE}/${repo_dir}${source_code_path ?: ''}") {
            sh """ docker run --rm -v \${WORKSPACE}/${repo_dir}${source_code_path ?: ''}:/app/ -w /app node:${node_version} sh -c "${build_command}" """
            logger.logger('msg':'Build successful', 'level':'INFO')
    }
}
