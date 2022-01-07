package org.devops






def CreateVersion(){
    // 定义一个版本号作为当次构建的版本，输出结果 dev-1extra
    String tag = "${deployEnv}" + "-${env.BUILD_ID}";
    return tag;
}

//定义项目名称
def ProjectName(){
    if ("$extra" != ""){
        env.ProjectName = "$project-${extra}"
    } else {
        env.ProjectName = "$project"
    }    
}

def DockerBuild(value){
    container('helm'){
        ProjectName()
        sh 'docker login -u aft_ops2@example -p $docker registry-vpc.cn-hangzhou.aliyuncs.com' 
        def image = "registry-vpc.cn-hangzhou.aliyuncs.com/example/${ProjectName}"
        def baseimage = "registry-vpc.cn-hangzhou.aliyuncs.com/example/opslab:java-pp-v1.0.3"
        def oldimage = "registry-vpc.cn-hangzhou.aliyuncs.com/example/base:ppjava8"
        def tag = creatversion.GitTag()
        // 检查Dockfile文件是否存在
        result = fileExists "Dockerfile"
        if("$result" == "false"){
            println "不存在Dockerfile，请创建Dockerfile"
            break;
        } else {
                sh """
                sed -i "s@$oldimage@$baseimage@g" Dockerfile
                docker build . -t ${image}:${tag} --no-cache
                docker push ${image}:${tag}
                docker tag ${image}:${tag} ${image}:${deployEnv}-latest
                docker push ${image}:${deployEnv}-latest
                """
        }
    }
}

def DockerBuildSky(value){
    container('helm'){
        ProjectName()
        sh 'docker login -u aft_ops2@example -p $docker registry-vpc.cn-hangzhou.aliyuncs.com' 
        def image = "registry-vpc.cn-hangzhou.aliyuncs.com/example/${ProjectName}"
        if("$group" == "java-ms"){
            skywalkingimage = "registry-vpc.cn-hangzhou.aliyuncs.com/example/basecontainer:V0.1.3"
        } else {
            // skywalkingimage = "registry-vpc.cn-hangzhou.aliyuncs.com/example/basecontainer:V0.1.3"
            skywalkingimage = "registry-vpc.cn-hangzhou.aliyuncs.com/example/base:java-sk-v0.0.7"
        }
        def oldimage = "registry-vpc.cn-hangzhou.aliyuncs.com/example/base:ppjava8"
        def tag = creatversion.GitTag()
        // 检查Dockfile文件是否存在
        result = fileExists "Dockerfile"
        if("$result" == "false"){
            println "不存在Dockerfile，请创建Dockerfile"
            break;
        } else {
            sh """
            sed -i "s@$oldimage@$skywalkingimage@g" Dockerfile
            docker build . -t ${image}:${tag} --no-cache
            docker push ${image}:${tag}
            docker tag  ${image}:${tag} ${image}:${deployEnv}-latest
            docker push ${image}:${deployEnv}-latest
            """  
        }
    }
}