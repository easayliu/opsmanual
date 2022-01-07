package org.devops

// 获取时间
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
        \"content\": \"# [${env.JOB_NAME}](${env.BUILD_URL})\\n> **任务**:[${env.BUILD_DISPLAY_NAME}](${env.BUILD_URL})\\n> **分支**: ${env.gitlabBranch}\\n> **状态**:  ${status}\\n> **持续时间**: ${currentBuild.durationString}\\n> **当前时间**: ${nowTime}\\n> **执行人**: ${env.gitlabUserName}\\n> [点击查看详情日志](${env.BUILD_URL}console)\"
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
// 
def SendMessageTextCardReqBody(status){
  def nowTime = getTime()
  reqBody = """
  {
      "msgtype":"template_card",
      "template_card":{
          "card_type":"text_notice",
          "source":{
              "desc":"Jenkins",
              "desc_color":0
          },
          "main_title":{
              "title":"${env.JOB_NAME}"
          },
          "horizontal_content_list":[
              {
                  "keyname":"任务",
                  "value":"${env.BUILD_DISPLAY_NAME}"
              },
              {
                  "keyname":"分支",
                  "value":"${env.gitlabBranch}"
              },                       
              {
                  "keyname":"构建状态",
                  "value":"$status"
              },            
              {
                  "keyname":"持续时间",
                  "value":"${currentBuild.durationString}"
              },
              {
                  "keyname":"当前时间",
                  "value":"${nowTime}"
              },
              {
                  "keyname":"执行人",
                  "value":"${env.gitlabUserName}"
              }            
          ],
          "jump_list":[
              {
                  "type":1,
                  "url":"${env.BUILD_URL}",
                  "title":"${env.JOB_NAME}详情日志"
              }
          ],
          "card_action":{
              "type":1,
              "url":"${env.BUILD_URL}"
          }
      }
  }  
  """
  return reqBody
}

def SendMessageTextCard(status,webhook){
    wrap([$class: 'BuildUser']){
    def wechatHook = "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=${webhook}"
    // def nowTime = getTime()
    reqBody = SendMessageTextCardReqBody("$status")
    // println reqBody

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
