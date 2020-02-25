node {
   stage('Preparation') {
      checkoutRepo.checkOutFrom "${branch}", "powerbuy-headless"
      prepareSpace()
      
      if ("${runFailed}"=="true") {
      sh "/usr/bin/python3.7 ./etc/testrail_runner/testrail_runId_parser.py -r ${planid} " + \
         "--api-key H4u/Vob36vpKGkUrZrE5-QcwRfVOZ7xb5FcT5eZGl -o . --username jenkins@testrail.net -f test " + \
         "--failed-only"
      }
      else {
      sh "/usr/bin/python3.7 ./etc/testrail_runner/testrail_runId_parser.py -r ${planid} " + \
         "--api-key H4u/Vob36vpKGkUrZrE5-QcwRfVOZ7xb5FcT5eZGl -o . --username jenkins@testrail.net -f test"
      }
    }
   stage('Testing') {
      testrail =""
         if("${planid}"!="0"){
           testrail = """-V ./web*.yaml \
                         --prerunmodifier "./etc/testrail_runner/testrail_rbfw_prerunmodifier.py:./web_*.yaml" \
                         --listener ./etc/testrail_runner/testrail_rbfw_listener.py:https:cenergy.testrail.net:jenkins@testrail.net:H4u/Vob36vpKGkUrZrE5-QcwRfVOZ7xb5FcT5eZGl \
                      """
           echo testrail
      }
      
      def robotCommand = "--exclude ${exclude_tags} \
      --variable ENV:staging \
      --variable DEVICE_PLATFORM:${device_platform} \
      ${testrail}"
      
        withCredentials([sshUserPrivateKey(credentialsId: 'qa-pwb-pk', keyFileVariable: 'ssh_key', passphraseVariable: '', usernameVariable: 'ssh_username'),
        usernamePassword(credentialsId: 'qa-pwb-user', passwordVariable: 'db_password', usernameVariable: 'db_username')]) {
            def dockerEnv = "-e PCLOUDY_USERNAME=${PCLOUDY_USERNAME} \
            -e PCLOUDY_APIKEY=${PCLOUDY_APIKEY} \
            -e QA_KEY='${ssh_key}' \
            -v '${ssh_key}':'${ssh_key}'"
            testing.executeDockerWebByTags "${tags}", "${testFile}", "${robotCommand}", "stg-oms-admin", "${dockerEnv}"
        }
   }
   stage('Results') {
      robotReport.robotPublishResult "${tags}", "100.0", "0.0", "true"
   }
   stage('Slack Notification'){
      slackNotification.slackPublishResult "${slack_channel}"
   }
}
