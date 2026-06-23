package opstree.common

import opstree.common.*

def size_validator_factory(Map step_params) {
    logger = new logger()
    if (step_params.image_size_validator_check == 'true') {
        image_size_validator(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for creds scanning. Please mention correct values.', 'level':'WARN')
  }
}

def image_size_validator(Map step_params) {
    logger = new logger()
    parser = new parser()
    image_scanning_check_reports = new reports_management()

    logger.logger('msg':'Performing Image Scanning', 'level':'INFO')

    fail_job_if_validation_fail =  "${step_params.fail_job_if_validation_fail}"

    dir("${WORKSPACE}") {
        try {
            def imageExists = sh(script: "docker images -q ${step_params.image_name}:${step_params.image_tag}", returnStdout: true).trim()

            if (imageExists) {
                logger.logger('msg':'Image found, proceeding with size validation', 'level':'INFO')

                def size = sh(script: "docker image inspect ${step_params.image_name}:${step_params.image_tag} --format='{{.Size}}'", returnStdout: true).trim()
                def imageSize = size.toInteger() / 1000000

                logger.logger('msg':"Image size is ${imageSize}MB", 'level':'INFO')
                logger.logger('msg':"Image size allowed is ${step_params.max_allowed_image_size}MB", 'level':'INFO')

                if (imageSize > step_params.max_allowed_image_size.toInteger()) {
                    logger.logger('msg':'Build failed. Image size is larger than the allowed limit.', 'level':'ERROR')

                    if (step_params.fail_job_if_validation_fail == 'true') {
                        logger.logger('msg':'Size of image is more than expected.', 'level':'ERROR')
                        error 'Size of image is more than expected.'
                    }
                    else {
                        logger.logger('msg':'Size of image is more than expected, but ignoring this issue as per user input.', 'level':'INFO')
                    }
                }
                else {
                    logger.logger('msg':'Image size is within the expected limit.', 'level':'INFO')
                }
            }
            else {
                logger.logger('msg':"Image ${step_params.image_name} not found", 'level':'ERROR')
                error "Image ${step_params.image_name} not found"
            }
        } catch (Exception e) {
            if (fail_job_if_validation_fail == 'true') {
                logger.logger('msg':"Size validation failed: ${e.message}", 'level':'ERROR')
                error 'Size validation failed. Please check the logs for more details.'
            }

            else  {
                logger.logger('msg':'Size validation failed: But ignoring based on user inputs', 'level':'INFO')
            }
        }
    }
}
