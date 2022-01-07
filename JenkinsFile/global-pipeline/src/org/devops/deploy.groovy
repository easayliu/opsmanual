package org.devops


//项目发布
def Deploy(){
    container('helm'){
        
        //定义项目名
        devops.ProjectName()
        //拉取gitops代码
        devops.GitopsGitClone()
        // 初始化项目目录
        devops.InitDir()
        //判断是否需要初始化项目文件
        result = devops.FileExit()
        //修改values的参数
        if ("$group" == "frontend" || "$group" == "example" ){
            devops.FrontendChangeValues()
        } else {
            devops.BackendChangeValues()
        }
        //进行发布动作
        echo "$result"
        if (result == false && "$group" == "python" ){
            devops.GitopsGitPush()
            devops.ArgocdPythonDeploy()
        } else if(result == false) {
            devops.GitopsGitPush()
            devops.ArgocdInitDeploy()
        } else {
            devops.GitopsGitPush()
        }
    }        
}