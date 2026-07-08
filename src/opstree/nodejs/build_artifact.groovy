package opstree.nodejs

import opstree.common.*

def build_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_code_build == 'true' || step_params.perform_code_build == true) {
        build_artifact(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for Building Artifact. Please mention correct values.', 'level':'WARN')
  }
}

def build_artifact(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Build Step for Node.js', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    source_code_path = "${step_params.source_code_path}"
    node_version = step_params.node_version ?: "18"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

    dir("${WORKSPACE}/${repo_dir}${source_code_path ?: ''}") {
            sh """ docker run --rm -v \${WORKSPACE}/${repo_dir}${source_code_path ?: ''}:/app/ -w /app node:${node_version} sh -c "npm install && npm run build --if-present" """
            logger.logger('msg':'Build successful', 'level':'INFO')
    }
}


// package opstree.nodejs

// import opstree.common.*

// def build_factory(Map step_params) {
//     def logger = new logger()
//     if (step_params.perform_code_build == 'true' || step_params.perform_code_build == true) {
//         build_artifact(step_params)
//     }
//     else {
//         logger.logger('msg':'No valid option selected for Building Artifact. Please mention correct values.', 'level':'WARN')
//     }
// }

// def build_artifact(Map step_params) {
//     def logger = new logger()
//     def parser = new parser()

//     logger.logger('msg':'Performing Build Step for Node.js', 'level':'INFO')

//     def repo_url = "${step_params.repo_url}"
//     def source_code_path = "${step_params.source_code_path}"
//     def node_version = step_params.node_version ?: "18"
//     def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

//     def default_build_command = "npm install && npm run build"
//     def build_command = step_params.build_command ?: default_build_command

//     // ─── DEBUG: show what paths and values we are working with ───
//     logger.logger('msg': "repo_url         = ${repo_url}",         'level': 'INFO')
//     logger.logger('msg': "repo_dir         = ${repo_dir}",         'level': 'INFO')
//     logger.logger('msg': "source_code_path = ${source_code_path}", 'level': 'INFO')
//     logger.logger('msg': "node_version     = ${node_version}",     'level': 'INFO')
//     logger.logger('msg': "WORKSPACE        = ${WORKSPACE}",        'level': 'INFO')
//     logger.logger('msg': "build_command    = ${build_command}",    'level': 'INFO')

//     def full_path = "${WORKSPACE}/${repo_dir}${source_code_path ?: ''}"
//     logger.logger('msg': "Full source path being mounted = ${full_path}", 'level': 'INFO')

//     dir(full_path) {

//         // ─── DEBUG: confirm directory, .npmrc, package.json ───
//         sh """
//             echo "=== [DEBUG] Current directory ==="
//             pwd

//             echo "=== [DEBUG] Listing directory contents ==="
//             ls -la

//             echo "=== [DEBUG] Checking .npmrc in project root ==="
//             if [ -f .npmrc ]; then
//                 echo ".npmrc FOUND."
//                 echo "--- Non-auth lines (registry config) ---"
//                 grep -v "_authToken\\|_auth\\|password" .npmrc || echo "(none)"
//                 echo "--- Auth lines (token masked) ---"
//                 grep "_authToken\\|_auth\\|password" .npmrc | sed 's/=.*/=<TOKEN_PRESENT>/' || echo "(no auth lines found)"
//             else
//                 echo ".npmrc NOT FOUND in project root"
//             fi

//             echo "=== [DEBUG] All registries referenced in .npmrc ==="
//             if [ -f .npmrc ]; then
//                 echo "--- Scoped registries (@scope:registry=...) ---"
//                 grep -i "registry" .npmrc | grep -v "_authToken\\|_auth\\|password" || echo "(none found)"

//                 echo "--- Full registry URLs being used ---"
//                 grep "registry=" .npmrc | awk -F'=' '{print \$2}' || echo "(none)"
//             else
//                 echo "No .npmrc found - npm will use default public registry"
//             fi

//             echo "=== [DEBUG] package.json registry hints ==="
//             if [ -f package.json ]; then
//                 echo "--- publishConfig ---"
//                 cat package.json | grep -A5 '"publishConfig"' || echo "(no publishConfig)"
//                 echo "--- registry key ---"
//                 cat package.json | grep '"registry"' || echo "(no registry key)"
//                 echo "--- scoped dependencies (private registry hints) ---"
//                 grep '"@' package.json | grep -v '"scripts"\\|"//' | head -20 || echo "(no scoped packages)"
//             fi

//             echo "=== [DEBUG] package-lock.json resolved registries ==="
//             if [ -f package-lock.json ]; then
//                 echo "--- Unique registry URLs from resolved fields ---"
//                 grep '"resolved"' package-lock.json | awk -F'"' '{print \$4}' | \\
//                 sed 's|/[^/]*\$||' | sort -u | head -20 || echo "(no resolved entries)"
//             else
//                 echo "package-lock.json NOT FOUND"
//             fi

//             echo "=== [DEBUG] NPM_TOKEN presence on Jenkins agent ==="
//             if [ -z "\${NPM_TOKEN:-}" ]; then
//                 echo "NPM_TOKEN is NOT SET on the Jenkins agent"
//             else
//                 echo "NPM_TOKEN is SET on Jenkins agent (length: \$(echo -n \$NPM_TOKEN | wc -c) chars)"
//             fi

//             echo "=== [DEBUG] Other npm-related env vars on Jenkins agent ==="
//             env | grep -i "npm\\|node\\|registry\\|token\\|auth" | sed 's/=.*/=<PRESENT>/' || echo "(none found)"
//         """

//         // ─── Actual Docker build with full debug inside container ───
//         sh """
//             docker run --rm \\
//                 -v ${full_path}:/app/ \\
//                 -w /app \\
//                 -e NPM_TOKEN=\${NPM_TOKEN:-} \\
//                 node:${node_version} sh -c "
//                     echo '=== [CONTAINER] whoami ==='
//                     whoami

//                     echo '=== [CONTAINER] Node and npm versions ==='
//                     node --version
//                     npm --version

//                     echo '=== [CONTAINER] npm config registry ==='
//                     npm config get registry

//                     echo '=== [CONTAINER] Full npm config ==='
//                     npm config list

//                     echo '=== [CONTAINER] .npmrc in /app ==='
//                     if [ -f /app/.npmrc ]; then
//                         echo '.npmrc FOUND inside container'
//                         echo '--- Non-auth lines ---'
//                         grep -v '_authToken\\\\|_auth\\\\|password' /app/.npmrc || echo '(none)'
//                         echo '--- Auth lines (masked) ---'
//                         grep '_authToken\\\\|_auth\\\\|password' /app/.npmrc | sed 's/=.*/=<TOKEN_PRESENT>/' || echo '(no auth lines)'
//                     else
//                         echo 'No .npmrc in /app'
//                     fi

//                     echo '=== [CONTAINER] NPM_TOKEN inside container ==='
//                     if [ -z \\\"\\\${NPM_TOKEN:-}\\\" ]; then
//                         echo 'NPM_TOKEN is NOT SET inside container - likely cause of 401'
//                     else
//                         echo 'NPM_TOKEN is SET inside container'
//                     fi

//                     echo '=== [CONTAINER] npm whoami (auth check) ==='
//                     npm whoami || echo 'npm whoami FAILED - not authenticated to registry'

//                     echo '=== [CONTAINER] Running build command ==='
//                     ${build_command}
//                 "
//         """

//         logger.logger('msg':'Build successful', 'level':'INFO')
//     }
// }