package org.devops

//格式化输出
def PrintMes(value,color){
    colors = ['red'   : "\033[31m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m",
              'blue'  : "\033[34m ${value} \033[0m",
              'green' : "\033[32m >>>>>>>>>>>${value}<<<<<<<<<<< \033[0m",
              'lightblue' : "\033[94m ${value} \033[0m\n"
               ]
    ansiColor('xterm') {
        println(colors[color])
    }
}


// 获取镜像版本
def CreateVersion(value) {
    // 定义一个版本号作为当次构建的版本，输出结果 20191210175842_69
    return new Date().format('yyyyMMddHHmmss') + "_${env.BUILD_ID}" + "_${deployEnv}" + "${value}"
}


// 获取时间
def getTime() {
    // 定义一个版本号作为当次构建的版本，输出结果 20191210175842
    return new Date().format('yyyyMMddHHmmss')
}

// 企业微信机器人
def SendMessage(product,profile,srv_name,status) {
    wrap([$class: 'BuildUser']){
    def WechatHook = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=68d7c696-006c-4538-98b6-f0780aff557d"
    def reqBody = """{
                        "msgtype": "markdown",
                        "markdown": {
                            "content": "### 构建信息:\n>- 产品线 **${product}**\n>- 发布环境:  **${profile}**\n>- 应用名称:  **${srv_name}**\n>- 构建发起人:  **${env.BUILD_USER}**\n>- 构建结果: **${status}**\n>- 持续时间: **${currentBuild.durationString}**\n- 构建日志:  [点击查看详情](${env.BUILD_URL})"
                        }
                    }"""

        httpRequest acceptType: 'APPLICATION_JSON_UTF8',
            consoleLogResponseBody: false,
            contentType: 'APPLICATION_JSON_UTF8',
            httpMode: 'POST',
            ignoreSslErrors: true,
            requestBody: "${reqBody}",
            url: "${WechatHook}",
            quiet: true
    }
}