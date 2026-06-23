package opstree.common

import opstree.common.*
import opstree.common.k8s_deployment

def workdir = env.WORKSPACE ?: '/workspace'

def k8s_deployment_factory(Map config, Boolean returnStatus = false) {
    logger = new logger()
    parser = new parser()

    try {
        def repo_dir = parser.fetch_git_repo_name('repo_url': "${config.repo_url}")
        def base_path = "/workspace${config.source_code_path}"

        // Determine what to apply
        def isRecursive = (config.apply_recursive?.toString() == 'true')
        def apply_target = isRecursive
            ? base_path
            : "${base_path}/${config.manifest_file_path}"

        if (isRecursive && config.manifest_file_path) {
            logger.logger(msg: "'manifest_file_path' is ignored because 'apply_recursive' is true.", level: 'WARN')
        }

        logger.logger(msg: "Running: kubectl apply -f ${apply_target}", level: 'INFO')

        sh """
            docker run --rm \
            -v '${config.kubeconfig}:/root/.kube/config' \
            -v '${env.WORKSPACE}/${repo_dir}:/workspace' \
            -w /workspace \
            alpine/k8s:1.31.8 \
            /bin/sh -c "kubectl apply -f ${apply_target}"
        """
    } catch (Exception e) {
        logger.logger(msg: "K8s operation failed for kubectl command. Error: ${e.toString()}", level: 'ERROR')
        error('K8s command failed. See logs for details.')
    }
}

