 
// 封装HTTP请求
def HttpReq(requestType,requestUrl,requestBody){
    // 定义sonar api接口
    def sonarServer = "https://sonarqube.example.cn/api"
    result = httpRequest authentication: '56f0d64c-4cb2-4a91-8e9f-545de6bdcba9',
            httpMode: requestType,
            contentType: "APPLICATION_JSON",
            consoleLogResponseBody: true,
            ignoreSslErrors: true,
            requestBody: requestBody,
            url: "${sonarServer}/${requestUrl}"
    return result
    
}
 
// 获取soanr项目的状态
def GetSonarStatus(projectName){
    def apiUrl = "project_branches/list?project=${projectName}"
    // 发请求
    response = HttpReq("GET",apiUrl,"")
    // 对返回的文本做JSON解析
    response = readJSON text: """${response.content}"""
    // 获取状态值
    result = response["branches"][0]["status"]["qualityGateStatus"]
    return result
}
 
// 获取sonar项目，判断项目是否存在
def SearchProject(projectName){
    def apiUrl = "projects/search?projects=${projectName}"
    // 发请求
    response = HttpReq("GET",apiUrl,"")
    println "搜索的结果：${response}"
    // 对返回的文本做JSON解析
    response = readJSON text: """${response.content}"""
    // 获取total字段，该字段如果是0则表示项目不存在,否则表示项目存在
    result = response["paging"]["total"]
    // 对result进行判断
    if (result.toString() == "0"){
        return "false"
    }else{
        return "true"
    }
}
 
// 创建sonar项目
def CreateProject(projectName){
    def apiUrl = "projects/create?name=${projectName}&project=${projectName}"
    // 发请求
    response = HttpReq("POST",apiUrl,"")
    println(response)
}
 
// 配置项目质量规则
def ConfigQualityProfiles(projectName,lang,qpname){
    def apiUrl = "qualityprofiles/add_project?language=${lang}&project=${projectName}&qualityProfile=${qpname}"
    // 发请求
    response = HttpReq("POST",apiUrl,"")
    println(response)
}
 
// 获取质量阈ID
def GetQualityGateId(gateName){
    def apiUrl = "qualitygates/show?name=${gateName}"
    // 发请求
    response = HttpReq("GET",apiUrl,"")
    // 对返回的文本做JSON解析
    response = readJSON text: """${response.content}"""
    // 获取total字段，该字段如果是0则表示项目不存在,否则表示项目存在
    result = response["id"]
    return result
}
 
// 更新质量阈规则
def ConfigQualityGate(projectKey,gateName){
    // 获取质量阈id
    gateId = GetQualityGateId(gateName)
    apiUrl = "qualitygates/select?projectKey=${projectKey}&gateId=${gateId}"
    // 发请求
    response = HttpReq("POST",apiUrl,"")
    println(response)
}
 
//获取Sonar质量阈状态
def GetProjectStatus(projectName){
    apiUrl = "project_branches/list?project=${projectName}"
    response = HttpReq("GET",apiUrl,'')
    
    response = readJSON text: """${response.content}"""
    result = response["branches"][0]["status"]["qualityGateStatus"]
    
    //println(response)
    
   return result
}

// 获取项目source路径

def ProjcetPath(){
    if ("$project" == "cas" ){
        env.ProjectPath = "cas-rest/src"
        env.BinariesPath = "cas-rest/target"
    } else if("$project" == "content-center"){
        env.ProjectPath = "content-rest/src"
        env.BinariesPath = "content-rest/target"
    } else {
        env.ProjectPath = "src"
        env.BinariesPath = "target"
    }    
}

// 获取时间
def getTime() {
    return new Date().format('yyyy-MM-dd  HH:mm:ss')
}


// 发送检查报告通知
def SendMessage(status,webhook){
    wrap([$class: 'BuildUser']){
    def wechatHook = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=${webhook}"
    def nowTime = getTime()
    def sonarurl = " https://sonarqube.example.cn/dashboard?id=${project}"
    def reqBody = """{
    \"msgtype\": \"markdown\",
    \"markdown\": {
        \"content\": \"# [${env.JOB_NAME}](${env.BUILD_URL})\\n> **任务**:[${env.BUILD_DISPLAY_NAME}](${env.BUILD_URL})\\n> **分支**: ${env.gitlabBranch}\\n> **代码检查结果**:  ${status}\\n> **持续时间**: ${currentBuild.durationString}\\n> **当前时间**: ${nowTime}\\n> **执行人**: ${env.gitlabUserName}\\n> [点击查看代码检查面板](${sonarurl})\"
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