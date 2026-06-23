package opstree.angular

import opstree.common.*

def build_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_code_build == 'true') {
        build_artifact(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Building Artifact. Please mention correct values.', 'level':'WARN')
  }
}

def build_artifact(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Build Step', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    source_code_path = "${step_params.source_code_path}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    dir("${WORKSPACE}/${repo_dir}") {
            sh """ docker run  --rm -v ~/.node_modules:/app/node_modules -v ${WORKSPACE}/${repo_dir}:/app/ -w /app node:14.21.3 sh -c " npm install && node --max_old_space_size=8192 ./node_modules/@angular/cli/bin/ng build --prod " """
            logger.logger('msg':'Build successful', 'level':'INFO')
    }
}
