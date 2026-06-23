package opstree.common

import opstree.common.*

def notification_factory(Map step_params) {
    def logger = new opstree.common.logger()
    if (step_params.notification_enabled == 'true') {
        notification(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Notification. Please mention correct values.', 'level':'WARN')
  }
}

def notification(Map step_params) {
    def logger = new opstree.common.logger()
    def parser = new parser()

    logger.logger('msg':'Performing Notification', 'level':'INFO')
    def build_status = "${step_params.build_status}"
    def notification_channel = "${step_params.notification_channel}"

    if (step_params.notification_channel == 'teams') {
        def message = ''
        def color = ''
        def remarks = "Started by user ${env.BUILD_USER_ID}." // Customize as needed
        webhook_url_creds_id = "${step_params.webhook_url_creds_id}"

        if (build_status == 'SUCCESS') {
            message = "${env.JOB_NAME}: BUILD SUCCESS."
            color = '#008000'
        } else if (build_status == 'FAILURE') {
            message = "${env.JOB_NAME}: BUILD FAILED!!!"
            color = '#FF0000'
        } else if (build_status == 'UNSTABLE') {
            message = "${env.JOB_NAME}: BUILD UNSTABLE!!"
            color = '#FFFF00'
        } else {
            message = "${env.JOB_NAME}: BUILD RESULT UNKNOWN!!"
            color = '#FFA500'
        }

        withCredentials([string(credentialsId: webhook_url_creds_id, variable: 'WEBHOOK_URL')]) {
            office365ConnectorSend(
                webhookUrl: "${WEBHOOK_URL}",
                status: build_status,
                color: color,
                message: """<b>${message}</b><br><br>
                        <strong>Build No: #${env.BUILD_NUMBER}</strong><br><br>
                        <strong>Remarks</strong>: ${remarks}<br><br>"""
            )
        }
    }

    if (step_params.notification_channel == 'slack') {
        // def build_status = "${step_params.build_status}"
        def slack_channel = "${step_params.slack_channel}"
        def triggeredBy = currentBuild.getBuildCauses().find { cause -> cause instanceof hudson.model.Cause.UserIdCause }?.userId
        ?: (env.BUILD_USER_ID ?: 'SCM Trigger/Unknown')
        def jobStartTime = new Date(currentBuild.startTimeInMillis).format('yyyy-MM-dd HH:mm:ss', TimeZone.getTimeZone('Asia/Kolkata'))
        def message
        // Construct the message based on the build status
        if (build_status == 'FAILURE') {
            message = 'Job Build Failed!!!!'
        } else if (build_status == 'SUCCESS') {
            message = 'Job Build Successfully Completed.'
        } else if (build_status == 'ABORTED') {
            message = 'Job Build was Aborted'
        } else {
            message = "Job Build status is ${build_status}"
        }
        // Determine the color based on build status
        def color
        switch (build_status) {
            case 'SUCCESS':
                color = 'good'
                break
            case 'FAILURE':
                color = 'danger'
                break
            case 'ABORTED':
                color = 'warning'
                break
            default:
                color = 'warning'
        }
        // Send the Slack notification using a variable for the channel
        slackSend(
            channel: slack_channel,
            color: color,
            message: """
            ${message}
            Find Status of Pipeline: ${build_status}
            Triggered By: ${triggeredBy}
            Job Name: ${env.JOB_NAME}
            Build Number: ${env.BUILD_NUMBER}
            Build URL: ${env.BUILD_URL}
            """
        )
    }

    if (step_params.notification_channel == 'gmail') {
        def gmail_notification_recipients_email_ids = "${step_params.gmail_notification_recipients_email_ids}"
        def gmail_notification_from_email_id = "${step_params.gmail_notification_from_email_id}"

        def message = ''
        def color = ''
        def remarks = "Started by user ${env.BUILD_USER_ID}." // Customize as needed

        if (build_status == 'SUCCESS') {
            message = "${env.JOB_NAME}: BUILD SUCCESS."
            color = '#008000'
        } else if (build_status == 'FAILURE') {
            message = "${env.JOB_NAME}: BUILD FAILED!!!"
            color = '#FF0000'
        } else if (build_status == 'UNSTABLE') {
            message = "${env.JOB_NAME}: BUILD UNSTABLE!!"
            color = '#FFFF00'
        } else {
            message = "${env.JOB_NAME}: BUILD RESULT UNKNOWN!!"
            color = '#FFA500'
        }

        emailext(
                to: gmail_notification_recipients_email_ids,
                subject: "JenkinsJob Name: ${env.JOB_NAME} Build Number: ${env.BUILD_NUMBER} is ${build_status}",
                from: gmail_notification_from_email_id,
                body: """
                        <strong>Build No: #${env.BUILD_NUMBER}</strong><br><br>
                        <strong>Remarks</strong>: ${remarks}<br><br>
                        Find Detailed Status of Pipeline: ${build_status}<br><br>
                        Triggered By: ${env.BUILD_USER_ID}<br><br>
                        Job Name: ${env.JOB_NAME}<br><br>
                        Build Number: ${env.BUILD_NUMBER}<br><br>
                        Build URL: ${env.BUILD_URL}""",
                mimeType: 'text/html',
                attachmentsPattern: 'gitleaks-report.json'
            )
    }

    // if (step_params.notification_channel == 'outlook') {
    //     def message = ''
    //     def color = ''
    //     def remarks = "Started by user ${env.BUILD_USER_ID}." // Customize as needed
    //     webhook_url_creds_id = "${step_params.webhook_url_creds_id}"

    //     if (build_status == 'SUCCESS') {
    //         message = "${env.JOB_NAME}: BUILD SUCCESS."
    //         color = '#008000'
    //     } else if (build_status == 'FAILURE') {
    //         message = "${env.JOB_NAME}: BUILD FAILED!!!"
    //         color = '#FF0000'
    //     } else if (build_status == 'UNSTABLE') {
    //         message = "${env.JOB_NAME}: BUILD UNSTABLE!!"
    //         color = '#FFFF00'
    //     } else {
    //         message = "${env.JOB_NAME}: BUILD RESULT UNKNOWN!!"
    //         color = '#FFA500'
    //     }

//     withCredentials([string(credentialsId: webhook_url_creds_id, variable: 'WEBHOOK_URL')]) {
//         office365ConnectorSend(
//             webhookUrl: "${WEBHOOK_URL}",
//             status: build_status,
//             color: color,
//             message: """<b>${message}</b><br><br>
//                     <strong>Build No: #${env.BUILD_NUMBER}</strong><br><br>
//                     <strong>Remarks</strong>: ${remarks}<br><br>"""
//         )
//     }
// }
}
