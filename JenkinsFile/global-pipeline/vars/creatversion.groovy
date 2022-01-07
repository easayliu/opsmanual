//判断是否存在TAG
def GitTag() {
    String tagStatus = sh (
        script: 'git describe --abbrev=0 --tags',
        returnStatus: true
    )

    if ("${tagStatus}" == "0"){
        String tag = sh (
            script: 'git describe --tags --always',
            returnStdout: true            
        ).trim()

        return tag;
    } else {
        String tag = "${deployEnv}" + "-${env.BUILD_ID}";
        return tag;
    }
}


