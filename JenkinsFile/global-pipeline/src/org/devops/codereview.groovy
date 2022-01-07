package ore.devops
 
// 代码质量检查
def CodeScanner(){
    container('sonarqube'){
        script{
            //定义项目名
            devops.ProjectName()
            
            // 获取项目source路径
            sonarapi.ProjcetPath()
     
            result = sonarapi.SearchProject("$ProjectName")
            println(result)
            if (result == "false"){
                println("${ProjectName}---项目不存在,准备创建项目---> ${ProjectName}！")
                sonarapi.CreateProject("${ProjectName}")
            } else {
                println("${ProjectName}---项目已存在！")
            }
            
            if ("$project" == "dy-register"){
                sh "echo skip"                
            } else if("$group" == "python"){
                withSonarQubeEnv(credentialsId: 'sonar-token') {
                sh """
                /opt/sonar-scanner/bin/sonar-scanner \
                -Dsonar.projectKey=${ProjectName} \
                """                
                }
            } else {
                withSonarQubeEnv(credentialsId: 'sonar-token') {
                sh """
                export SONAR_SCANNER_OPTS="-Xmx1024m"
                /opt/sonar-scanner/bin/sonar-scanner \
                -Dsonar.projectKey=${ProjectName} \
                -Dsonar.sources=${ProjectPath} \
                -Dsonar.java.binaries=${BinariesPath}/classes \
                -Dsonar.sourceEncoding=UTF-8 \
                -Dsonar.java.test.binaries=target/test-classes \
                -Dsonar.exclusions=**/pd/**
                """
                }
            }

            sleep 10
            devops.PrintMes("获取扫描结果","green")
            result = sonarapi.GetProjectStatus("${ProjectName}")

            println(result)
            if (result.toString() == "ERROR"){
                sonarapi.SendMessage("Failed","$key")
                sonarapi.SendMessage("Failed","$webhook")
                // error " 代码质量阈错误！请及时修复！"
            } else {
                println(result)
            }
        }
    }
}