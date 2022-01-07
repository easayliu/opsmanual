package org.devops


//定义项目名称
// def ProjectName(){
//     if ("$extra" != "" ){
//         env.statusProjectName = "$project-${extra}"
//     } else if ("$branch" == "hotfix") {
//         env.statusProjectName = "$project-${extra}$branch"
//     } else {
//         env.statusProjectName = "$project"
//     }    
// }
def ProjectName(){
    if ("$extra" != "" ){
        statusProjectName = "$project-${extra}"
    } else if ("$branch" == "hotfix") {
        statusProjectName = "$project-${extra}$branch"
    } else {
        statusProjectName = "$project"
    }
    return statusProjectName
}

// def ArgocdProjectSyncStatus(){
//     ProjectName()
//     sh '''
//         eval ArgocdPassword='$'${deployEnv}password
//         eval ArgocdUrl='$'${deployEnv}url
//         argocd login --username ${argocdusername} --password $ArgocdPassword $ArgocdUrl    
//     '''
//     if ("$branch" == "hotfix"){
//         env.SyncStatus = sh (
//         script: '''
//         sleep 10
//         argocd app  get ${statusProjectName}-hotifx --show-operation --grpc-web|grep "Phase:" |awk -F ':' '{print $2}' |awk '{sub("^ *","");sub(" *$","");print}'
//         ''', 
//         returnStdout: true      
//         ).trim()        
//     } else {
//         env.SyncStatus = sh (
//         script: '''
//         sleep 10
//         argocd app  get ${statusProjectName} --show-operation --grpc-web|grep "Phase:" |awk -F ':' '{print $2}' |awk '{sub("^ *","");sub(" *$","");print}'
//         ''', 
//         returnStdout: true      
//         ).trim()
//     }

//     def sync = "${SyncStatus}"
//     print sync
//     if ( sync == "Succeeded" || sync == "Failed" ){
//         sh '''
//         argocd app sync ${statusProjectName}
//         argocd app wait ${statusProjectName} --health --timeout 300 --grpc-web
//         '''        
//     } else if ( sync == "Running" ) {
//         // argocd app terminate-op ${ProjectName}
//         // argocd app sync ${ProjectName}    
//         sh '''
//         argocd app wait ${ProjectName} --health --timeout 300 --grpc-web
//         '''            
//     } else {
//         sh '''
//         argocd app sync ${statusProjectName}
//         argocd app wait ${statusProjectName} --health --timeout 300 --grpc-web
//         '''          
//     }
// }
// 获取应用状态
def ArgocdApplicationStatus(projectname){
        syncStatus =  sh (
                script:  """
                argocd app list| awk '{split(\$0,a);if(a[1] == "${projectname}")print a[6]}'
                """, returnStdout: true
                ).trim()    
        return syncStatus
}
// 检查应用配置同步状态
def ArgocdApplicationOperation(projectname){
  syncStatus =  sh (
          script:"""
          argocd app  get $projectname --show-operation --grpc-web|grep "Phase:" |awk -F ':' '{print \$2}' |awk '{sub("^ *","");sub(" *\$","");print}'
          """, returnStdout: true
          ).trim()    
  return syncStatus  
  println syncStatus
}

// 检查应用状态
def ArgocdProjectSyncStatus(){
    def projectname = ProjectName()
    println projectname
    sh '''
        eval ArgocdPassword='$'${deployEnv}password
        eval ArgocdUrl='$'${deployEnv}url
        argocd login --username ${argocdusername} --password $ArgocdPassword $ArgocdUrl    
    '''
    sleep 10
    // 检查应用配置同步状态
    def currentApplicationOperatoion = ArgocdApplicationOperation(projectname)
    while ( currentApplicationOperatoion != "Succeeded" && currentApplicationOperatoion != "Failed" ){
      sleep 5
      currentApplicationOperatoion = ArgocdApplicationOperation(projectname)
      devops.PrintMes("应用配置同步状态: $currentApplicationOperatoion","green")
      
    }    
    // 检查应用在argocd的状态
    def currentApplicationStatus = ArgocdApplicationStatus(projectname)    
    if (currentApplicationStatus == "Healthy" ){
      devops.PrintMes("应用当前状态: $currentApplicationStatus,执行应用更新动作","green") 
      sh """
      argocd app sync ${projectname}
      sleep 10
      argocd app wait ${projectname} --health --timeout 300 --grpc-web
      """
    } else if (currentApplicationStatus == "Progressing"){
      devops.PrintMes("应用当前状态: $currentApplicationStatus,等待应用状态","green")    
      sh """
      argocd app wait ${projectname} --health --timeout 300 --grpc-web
      """
    } else {
      devops.PrintMes("应用当前状态: $currentApplicationStatus,无法正常获取应用状态","red") 
      error(message: "应用当前状态: ${currentApplicationStatus},无法正常获取应用状态")
    }
}
def StatusCheck(){
    container('helm'){
        ArgocdProjectSyncStatus();
    }
}