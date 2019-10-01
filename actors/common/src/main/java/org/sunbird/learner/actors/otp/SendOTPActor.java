package org.sunbird.learner.actors.otp;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.request.Request;
import org.sunbird.learner.util.OTPUtil;

@ActorConfig(
  tasks = {},
  asyncTasks = {"sendOTP"}
)
public class SendOTPActor extends BaseActor {
  public static final String RESET_PASSWORD = "resetPassword";

  @Override
  public void onReceive(Request request) throws Throwable {

    if (ActorOperations.SEND_OTP.getValue().equals(request.getOperation())) {
      sendOTP(request);
    } else {
      onReceiveUnsupportedOperation("SendOTPActor");
    }
  }

  private void sendOTP(Request request) {
    String type = (String) request.getRequest().get(JsonKey.TYPE);
    String key = (String) request.getRequest().get(JsonKey.KEY);
    String otp = (String) request.getRequest().get(JsonKey.OTP);
    if (JsonKey.EMAIL.equalsIgnoreCase(type) || JsonKey.PREV_USED_EMAIL.equalsIgnoreCase(type)) {
      String userId = (String) request.get(JsonKey.USER_ID);
      ProjectLogger.log(
          "SendOTPActor:sendOTP userId found for send otp " + userId, LoggerEnum.INFO.name());
      sendOTPViaEmail(key, otp, userId);
    } else if (JsonKey.PHONE.equalsIgnoreCase(type)
        || JsonKey.PREV_USED_PHONE.equalsIgnoreCase(type)) {
      sendOTPViaSMS(key, otp);
    }
  }

  private void sendOTPViaEmail(String key, String otp, String otpType) {
    Map<String, Object> emailTemplateMap = new HashMap<>();
    emailTemplateMap.put(JsonKey.EMAIL, key);
    emailTemplateMap.put(JsonKey.OTP, otp);
    emailTemplateMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, OTPUtil.getOTPExpirationInMinutes());
    Request emailRequest = null;
    if (StringUtils.isBlank(otpType)) {
      emailRequest = OTPUtil.sendOTPViaEmail(emailTemplateMap);
    } else {
      emailRequest = OTPUtil.sendOTPViaEmail(emailTemplateMap, RESET_PASSWORD);
    }
    tellToAnother(emailRequest);
  }

  private void sendOTPViaSMS(String key, String otp) {
    Map<String, Object> otpMap = new HashMap<>();
    otpMap.put(JsonKey.PHONE, key);
    otpMap.put(JsonKey.OTP, otp);
    otpMap.put(JsonKey.OTP_EXPIRATION_IN_MINUTES, OTPUtil.getOTPExpirationInMinutes());
    OTPUtil.sendOTPViaSMS(otpMap);
  }
}
