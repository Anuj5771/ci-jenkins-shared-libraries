package opstree.common

import opstree.common.argocd_deployment

import opstree.common.*

def deployment_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_argocd_deployment == 'true') {
        eks_deployment(step_params)
    }
    else {
        logger.logger('msg':"${step_params.perform_argocd_deployment} No valid option selected for Deplying application via Argocd. Please mention correct values.", 'level':'WARN')
    }
}

def eks_deployment(Map step_params) {
    logger = new logger()

    def app_name               = "${step_params.app_name}"
    def argocd_credential_id   = "${step_params.argocd_credential_id}"
    def argocd_server_env_name = "${step_params.argocd_server_env_name}"
    def argocd_server_url      = env[argocd_server_env_name]
    def prune_post_deployment  = "${step_params.prune_post_deployment}"
    def perform_health_check   = step_params.perform_health_check?.toString()?.toBoolean() ?: false
    def dry_run                = step_params.dry_run?.toString()?.toBoolean() ?: false

    withCredentials([string(credentialsId: argocd_credential_id, variable: 'PASSWORD')]) {

        logger.logger('msg':'Login into ArgoCD', 'level':'INFO')
        sh """
        #!/bin/bash
        export XDG_CONFIG_HOME=\$(pwd)/.config
        argocd login ${argocd_server_url} --username admin --password \$PASSWORD --insecure
        """

        if (params.image_tag) {
            logger.logger('msg':'Setting image tag and running ArgoCD operation...', 'level':'INFO')
            sh """
            #!/bin/bash
            export XDG_CONFIG_HOME=\$(pwd)/.config

            if [ "${dry_run}" = "true" ]; then
                echo "[DRY-RUN] Disabling auto-sync to prevent immediate deployment..."
                argocd app set ${app_name} --sync-policy none

                echo "[DRY-RUN] Setting new image tag: ${params.image_tag}"
                argocd app set ${app_name} --helm-set image.tag=${params.image_tag}
                sleep 10

                echo "=== DRY-RUN: Showing changes that will be applied ==="
                argocd app get ${app_name} --refresh > /dev/null || true
                PAGER="" argocd app diff ${app_name} --exit-code=false
                echo "=== DRY-RUN Diff Complete ==="
            else
                echo "[DEPLOY] Updating image tag: ${params.image_tag}"
                argocd app set ${app_name} --sync-policy none
                argocd app set ${app_name} --helm-set image.tag=${params.image_tag}
                echo "[INFO] App definition updated. Please sync manually from ArgoCD UI."
            fi
            """
        }

        else {
            logger.logger('msg':'Running ArgoCD operation (no image tag)...', 'level':'INFO')
            sh """
            #!/bin/bash
            export XDG_CONFIG_HOME=\$(pwd)/.config

            if [ "${dry_run}" = "true" ]; then
                echo "=== DRY-RUN: Showing changes that will be applied ==="
                argocd app get ${app_name} --refresh > /dev/null || true
                PAGER="" argocd app diff ${app_name} --exit-code=false
                echo "=== DRY-RUN Diff Complete ==="
            else
                echo "[INFO] App definition already up to date. Please sync manually from ArgoCD UI."
            fi
            """
        }

        if (!dry_run && perform_health_check) {
            logger.logger('msg':'Checking Application Health check. Please wait for sometime...', 'level':'INFO')
            sh """
                #!/bin/bash
                export XDG_CONFIG_HOME=\$(pwd)/.config
                MAX_RETRIES=5
                RETRY_INTERVAL=30
                RETRIES=0

                while [ \$RETRIES -lt \$MAX_RETRIES ]; do
                    APP_STATUS=\$(argocd app get ${app_name} --output json | jq -r '.status.health.status')
                    echo "Application health status: \$APP_STATUS"

                    if [ "\$APP_STATUS" = "Healthy" ]; then
                        echo "Application is Healthy."
                        exit 0
                    else
                        echo "Application is not Healthy yet. Retrying in \${RETRY_INTERVAL} seconds..."
                        sleep \${RETRY_INTERVAL}
                        RETRIES=\$((RETRIES + 1))
                    fi
                done

                echo "Application did not become healthy within the expected time."
                exit 1
                """
            logger.logger('msg':'Application Deployed successfully', 'level':'INFO')
        } else if (dry_run) {
            logger.logger('msg':'Dry run completed. Skipping health check.', 'level':'INFO')
        } else {
            logger.logger('msg':'Health check skipped (perform_health_check=false).', 'level':'INFO')
        }
    }
}
