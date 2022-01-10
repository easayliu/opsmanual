// 发送消息

def getTime() {
    return new Date().format('yyyy-MM-dd  HH:mm:ss')
}

def SendMessage(status,webhook){
    wrap([$class: 'BuildUser']){
    def wechatHook = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=${webhook}"
    def nowTime = getTime()
    def reqBody = """{
    \"msgtype\": \"markdown\",
    \"markdown\": {
        \"content\": \"# [${env.JOB_NAME}](${env.BUILD_URL})\\n> **任务**:[${env.BUILD_DISPLAY_NAME}](${env.BUILD_URL})\\n> **分支**: ${env.gitlabBranch}\\n> **状态**:  ${status}\\n> **持续时间**: ${currentBuild.durationString}\\n> **当前时间**: ${nowTime}\\n> **执行人**: ${env.gitlabUserName}${env.gitlabUserUsername}\\n> [点击查看详情日志](${env.BUILD_URL}console)\"
    }
}"""

        httpRequest acceptType: 'APPLICATION_JSON_UTF8',
            consoleLogResponseBody: false,
            contentType: 'APPLICATION_JSON_UTF8',
            httpMode: 'POST',
            ignoreSslErrors: true,
            requestBody: "${reqBody}",
            url: "${wechatHook}",
            quiet: true
    }
}
//格式化输出
def PrintMes(value,color){
    colors = ['red'   :"\033[31m=========${value}=========\033[0m",
              'blue'  :"\033[38;5;4m=========${value}=========\033[0m",
              'green' :"\033[32m=========${value}=========\033[0m",
              'lightblue' :"\033[94m=========${value}=========\033[0m\n"
               ]
    ansiColor('xterm') {
        println(colors[color])
    }
}

//定义项目名称
def ProjectName(){
    if ("$extra" != ""){
        env.ProjectName = "$project-${extra}"
    } else {
        env.ProjectName = "$project"
    }    
}

// 判断是否存在项目values
def FileExit(){
    ProjectName()
    def projectfile = "example-helm/${group}/values/${ProjectName}-values.yaml" 
    result =  fileExists "${projectfile}"
    return result;
}

//获取文件名，判断文件名是否存在
def FileName(){
    ProjectName()
    result = FileExit()
    echo "$result"
    if (result == true){
        def filename = "example-helm/${group}/values/${ProjectName}-values.yaml";
        return filename;
    } else {
        def filename = "example-helm/${group}/values.yaml";
        return filename;
    }   
}

def InitFilename(filename){
    result = FileExit()    
    if (result == true){
        PrintMes("项目values已存在","green")
        sh "rm -f $filename"
    } else {
        PrintMes("项目values不存在，从values.yaml生成一份","blue")
    }    
}
// 初始化项目的helm文件,仅限后端使用
def InitDir(){
    def filename = "example-helm/${group}/values/${ProjectName}-values.yaml"
    def projectfile = "example-helm/${group}/values/${ProjectName}-values.yaml"
    def initfilename = "example-helm/${group}/values.yaml"    
    dirresult = fileExists "${initfilename}"
    switch("$dirresult") {
        case "false":
            sh "cp -r example-helm/demo example-helm/${group}"
            sh 'sed -i "s/demo/$group/g" `grep demo -rl example-helm/${group}`'
            sh 'chown -R 1000:1000 example-helm'
            PrintMes("${group}helm目录不存在，从demo目录创建 ","blue")
    }
}


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


//启用istio
def InitNs(){
    sh '''
    kubectl patch ns ${k8snamespace} -p '{"metadata":{"labels":{"istio-injection":"enabled"}}}' --kubeconfig=/root/.kube/${deployEnv}config
    '''
}

//argocd初始化项目
def ArgocdInitDeploy(){
    //判断exrtra参数
    ProjectName()
    if ("$branch" != "hotfix"){
        sh '''
        eval ArgocdPassword='$'${deployEnv}password
        eval ArgocdUrl='$'${deployEnv}url
        argocd login --username ${argocdusername} --password $ArgocdPassword $ArgocdUrl
        argocd proj create ${group} --allow-cluster-resource "*"/"*" --allow-namespaced-resource "*"/"*" --dest "*","*" --src "*" --upsert --grpc-web
        argocd app create ${ProjectName} \
            --upsert \
            --project ${group} \
            --repo http://gitlab.example.com:13140/operation/example-helm.git \
            --path ${group}\
            --revision ${deployEnv} \
            --dest-server  https://kubernetes.default.svc \
            --dest-namespace ${k8snamespace} \
            --sync-policy automated \
            --self-heal \
            --sync-option CreateNamespace=true \
            --sync-retry-backoff-max-duration 3m \
            --grpc-web \
            --values values/${ProjectName}-values.yaml
        '''
    } else {
        sh '''
        eval ArgocdPassword='$'${deployEnv}password
        eval ArgocdUrl='$'${deployEnv}url
        argocd login --username ${argocdusername} --password $ArgocdPassword $ArgocdUrl
        argocd proj create ${group} --allow-cluster-resource "*"/"*" --allow-namespaced-resource "*"/"*" --dest "*","*" --src "*" --upsert --grpc-web
        argocd app create ${ProjectName}-hotfix \
            --upsert \
            --project ${group} \
            --repo http://gitlab.example.com:13140/operation/example-helm.git \
            --path ${group}\
            --revision ${branch} \
            --dest-server  https://kubernetes.default.svc \
            --dest-namespace ${k8snamespace} \
            --sync-policy automated \
            --self-heal \
            --sync-option CreateNamespace=true \
            --sync-retry-backoff-max-duration 3m \
            --grpc-web \
            --values values/${ProjectName}-values.yaml
        '''        
    }

}

//python发布
def ArgocdPythonDeploy(){
    //判断exrtra参数
    ProjectName()
    sh '''
    eval ArgocdPassword='$'${deployEnv}password
    eval ArgocdUrl='$'${deployEnv}url
    argocd login --username ${argocdusername} --password $ArgocdPassword $ArgocdUrl
    argocd proj create ${group} --allow-cluster-resource "*"/"*" --allow-namespaced-resource "*"/"*" --dest "*","*" --src "*" --upsert --grpc-web
    argocd app create ${ProjectName} \
        --upsert \
        --project ${group} \
        --repo http://gitlab.example.com:13140/operation/example-helm.git \
        --path ${group}\
        --revision ${deployEnv} \
        --dest-server  https://kubernetes.default.svc \
        --dest-namespace ${k8snamespace} \
        --sync-policy automated \
        --sync-option CreateNamespace=true \
        --grpc-web \
        --values values/${ProjectName}-values.yaml
    '''
}

//获取gitops代码
def GitopsGitClone(){
    if ("$branch" != "hotfix"){
        sh '''
            git config --global user.email "${gitlab_username}@example.com"
            git config --global user.name "${gitlab_username}"                       
            git clone -b ${deployEnv} http://${gitlab_username}:${gitlab_password}@gitlab.example.com:13140/operation/example-helm.git   
            chown -R 1000:1000 example-helm
        '''          
    } else {
        sh '''
            git config --global user.email "${gitlab_username}@example.com"
            git config --global user.name "${gitlab_username}"                       
            git clone -b ${branch} http://${gitlab_username}:${gitlab_password}@gitlab.example.com:13140/operation/example-helm.git   
            chown -R 1000:1000 example-helm
        '''     
    }

}

//提交代码到远程分支，默认为master
def GitopsGitPush(){
    if ("$branch" != "hotfix"){
        sh '''
            cd example-helm
            git add .
            git commit -am "${ProjectName}'${deployEnv}' image update ，${tag}"
            [ "${extra}" = "mq" ] && sleep 10
            git pull origin ${deployEnv}
            git push origin ${deployEnv}
        '''            
    } else {
        sh '''
            cd example-helm
            git add .
            git commit -am "${ProjectName}'${branch}' image update ，${tag}"
            [ "${extra}" = "mq" ] && sleep 10
            git pull origin ${branch}
            git push origin ${branch}
        '''     
    }    

}

//修改后端values的参数
def BackendChangeValues(){
    def filename = FileName()    
    def projectfile = "example-helm/${group}/values/${ProjectName}-values.yaml"
    def tag = GitTag()    
    //获取初始化yaml
    def data = readYaml file: filename
    if (data.image.tag == tag){
        PrintMes("未发现新的提交，请提交代码后重新发布","red")
        error ''
    }

    //修改参数
    data.extra = "$env.extra"
    data.image.tag = "$tag"
    data.project.name = "$ProjectName"
    data.project.deploy = "$env.deployEnv"
    data.project.port = "$env.port"
    data.nacos.namespace = "$env.namespace"
    data.nacos.addr = "$env.addr"
    data.nameOverride = "$env.project"
    data.imagePullSecrets = [[name: "$ProjectName-docker"]]
    //参数写入 
    InitFilename("$filename")
    writeYaml file: projectfile, data: data
    PrintMes("${ProjectName}-values.yaml已创建","green")
}


//修改前端的values参数
def FrontendChangeValues(){
    // 获取文件名
    def filename = FileName()
    def projectfile = "example-helm/${group}/values/${ProjectName}-values.yaml"
    def tag = GitTag() 
    //获取初始化yaml

    def data = readYaml file: filename 
    //判断是否有tag变更
    if (data.image.tag == tag){
        PrintMes("未发现新的提交，请提交代码后重新发布","red")
        error ''
    }
    
    //读取uri的JSON
    uri = readJSON(text: "$uri",returnPojo: true) 
    
    //修改参数
    data.image.tag = "$tag"
    data.project.name = "$ProjectName"
    data.project.deploy = "$deployEnv"
    data.project.port = "80"
    data.nacos.enabled = false
    if ("$istioGateway" == "true"){
        data.istioGateway.enabled = true     
    } else {
        data.istioGateway.enabled = false
    }
    data.istioGateway.host = "$host"
    data.istioGateway.uri = uri
    data.nameOverride = "$project"
    data.imagePullSecrets = [[name: "$ProjectName-docker"]]
    //参数写入
    InitFilename("$filename")
    writeYaml file: projectfile, data: data
    PrintMes("${ProjectName}-values.yaml已创建","green")    
}

//获取gitlab的项目ID
def GitlabProjectID(){
    def url = "http://gitlab.example.com/api/v4/projects/${group}%2f${project}"
    def response = httpRequest (customHeaders: [[name: 'PRIVATE-TOKEN', value: 'r7dQycuYwx5Ld7yX_oUH' ]],
            url: "$url",
            consoleLogResponseBody: true)
    def gitlabid = response.content
    def data = readJSON(text: "$gitlabid")
    println data.id
    return data.id;
}
//确认push触发&tag触发
def RequestBody(){
    def jenkinsurl = "http://jenkins.cloud.example.net/project/$env.JOB_NAME"  
    if ("$deployEnv" == "pro"){
      def reqBody = "url=$jenkinsurl&token=$tokenexample&push_events=false&tag_push_events=true"
      return reqBody;
    } else {
      def reqBody = "url=$jenkinsurl&token=$tokenexample&push_events_branch_filter=$branch"
      return reqBody;
    }
}
//初始化gitlabwebhook地址
def AddGitlabWebhook(){
    def projectId = GitlabProjectID()
    def url = "http://gitlab.example.com/api/v4/projects/${projectId}/hooks"
    def reqBody = RequestBody()

    httpRequest (
          httpMode: 'POST',
          customHeaders: [[name: 'PRIVATE-TOKEN', value: '${PRIVATE-TOKEN}' ]],
          requestBody: "$reqBody", 
          consoleLogResponseBody: true, 
          wrapAsMultipart: true,
          url: "$url")
    echo "$env.JOB_NAME"
}
