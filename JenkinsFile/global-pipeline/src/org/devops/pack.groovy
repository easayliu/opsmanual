package org.devops

//格式化输出
def PrintMes(value,color){
    colors = ['red'   : "\033[31m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m",
              'blue'  : "\033[38;5;4m ${value} \033[0m",
              'green' : "\033[32m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m",
              'lightblue' : "\033[94m ${value} \033[0m\n"
               ]
    ansiColor('xterm') {
        println(colors[color])
    }
}

//定义打包动作
def MavenPack(){
    PrintMes("执行Maven打包动作","blue")
    if("$project" == "wxapi"){
        container('helm'){
            sh """
            cp /root/.m2/cert/* ./src/main/resources/
            mvn clean
            mvn install -Dmaven.test.skip=true -U
            """
        }         
    } else if ("$group" == "jjt" || "$group" == "bfz"){
        container('helm'){
            sh """
            mvn clean
            mvn install -Dmaven.test.skip=true -U
            """
        }           
    } else {
        container('helm'){
            sh """
            mvn clean
            mvn install -Dmaven.test.skip=true -U 
            """
        }          
    }
}
def NpmPack(){
    PrintMes("执行npm打包动作","blue")
    container('helm') {
        sh '''
        node -v
        npm config set sass_binary_site=https://npm.taobao.org/mirrors/node-sass
        npm config set sentrycli_cdnurl=https://npm.taobao.org/mirrors/sentry-cli
        npm config set registry https://nexus.example.cn/repository/npm-public
        npm install
        npm run build:$deployEnv
        '''
    }
}
def GradlePack(){
    PrintMes("执行gradle打包动作","blue")
    container('helm') {
        sh '''
        yes | /home/gradle/cmdline-tools/bin/sdkmanager --sdk_root=/root/gradle --licenses
        gradle assemble${buildType}
        DIR=`date +%Y%m%d%H%M`
        MD5=`md5sum app/build/outputs/apk/${buildType}/app-${buildType}.apk | awk '{print $1}'`
        sed -i "s/DIR/$DIR/g" info.json
        sed -i "s/MD5/$MD5/g" info.json
        /home/gradle/ossutil64 mkdir oss://fhl-ai-video/apk/${DIR}
        /home/gradle/ossutil64 cp app/build/outputs/apk/${buildType}/app-${buildType}.apk oss://fhl-ai-video/apk/${DIR}/
        yes | /home/gradle/ossutil64 cp info.json oss://fhl-ai-video/apk/
        '''
    }
}

def PhpPack(){
    PrintMes("执行PHP打包动作","blue")
    container('helm'){
        sh '''
        composer config -g repo.packagist composer https://mirrors.aliyun.com/composer/
        rm -f composer.lock
        composer update
        composer install      
        tar -cf php.tar *
        '''
    }    
}

//打包环境
def Package(value){
    switch ( "${value}" ){
    case "java-ms":
        MavenPack();
        break;
    case "crawler":
        MavenPack();
        break;        
    case "example":
        PhpPack();
        break;        
    case "bfz":
        MavenPack();
        break;        
    case "support":
        MavenPack();
        break;        
    case "jjt":
        MavenPack();
        break;
    case "fhl":
        MavenPack();
        break;     
    case "live-saas":
        MavenPack();
        break;
    case "frontend":
        if ("$extra" == "1"){
            break;
        } else{
            NpmPack();
            break;
        }
        // NpmPack();
        // break;  
    case "python":
        PrintMes("python项目，打包集成在Dockerflie","blue");
        break;
    case "autotest":
        PrintMes("python项目，打包集成在Dockerflie","blue");
        break                               
    }
}
