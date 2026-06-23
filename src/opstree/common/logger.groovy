package opstree.common

def logger(Map<String, String> log_params) {
    if (log_params.level == 'ERROR') {
        echo "[\u001B[31m${log_params.level}\u001B[0m] :   [\u001B[31m${log_params.msg}\u001B[0m] "
        error()
    }

else if (log_params.level == 'WARN') {
        echo "[\u001B[33m${log_params.level}\u001B[0m] ${log_params.msg}"
}

else {
        echo "[\u001B[32m${log_params.level}\u001B[0m] ${log_params.msg}"
}
}
