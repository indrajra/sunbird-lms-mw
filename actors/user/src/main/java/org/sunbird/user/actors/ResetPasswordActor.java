package org.sunbird.user.actors;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sunbird.actor.core.BaseActor;
import org.sunbird.actor.router.ActorConfig;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.TelemetryEnvKey;
import org.sunbird.common.request.ExecutionContext;
import org.sunbird.common.request.Request;
import org.sunbird.common.responsecode.ResponseCode;
import org.sunbird.learner.util.UserUtility;
import org.sunbird.learner.util.Util;
import org.sunbird.models.user.User;
import org.sunbird.services.sso.SSOManager;
import org.sunbird.services.sso.SSOServiceFactory;
import org.sunbird.telemetry.util.TelemetryUtil;
import org.sunbird.user.dao.UserDao;
import org.sunbird.user.dao.impl.UserDaoImpl;

/** This actor process the request for reset password. */
@ActorConfig(
  tasks = {"resetPassword"},
  asyncTasks = {}
)
public class ResetPasswordActor extends BaseActor {

  private SSOManager ssoManager = SSOServiceFactory.getInstance();
  ObjectMapper mapper = new ObjectMapper();

  @Override
  public void onReceive(Request request) throws Throwable {
    // Generate Telemetry (initializing context)
    Util.initializeContext(request, TelemetryEnvKey.USER);
    ExecutionContext.setRequestId(request.getRequestId());
    String userId = (String) request.get(JsonKey.USER_ID);
    String type = (String) request.get(JsonKey.TYPE);
    resetPassword(userId, type);
    generateTelemetry(request.getRequest());
  }

  private void resetPassword(String userId, String type) {
    ProjectLogger.log("ResetPasswordActor:resetPassword: method called.", LoggerEnum.INFO.name());
    UserDao userDao = new UserDaoImpl();
    User user = userDao.getUserById(userId);
    if (null != user) {
      user = removeUnUsedIdentity(user, type);
      Map<String, Object> userMap = mapper.convertValue(user, Map.class);
      UserUtility.decryptUserData(userMap);
      userMap.put(JsonKey.USERNAME, userMap.get(JsonKey.USERNAME));
      userMap.put(JsonKey.REDIRECT_URI, Util.getSunbirdLoginUrl());
      Util.getUserRequiredActionLink(userMap, false);
      if ((String) userMap.get(JsonKey.SET_PASSWORD_LINK) != null) {
        ProjectLogger.log(
            "ResetPasswordActor:resetPassword: link generated for reset password.",
            LoggerEnum.INFO.name());
        Response response = new Response();
        response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
        response.put(JsonKey.LINK, (String) userMap.get(JsonKey.SET_PASSWORD_LINK));
        sender().tell(response, self());
      } else {
        ProjectLogger.log(
            "ResetPasswordActor:resetPassword: not able to generate reset password link.",
            LoggerEnum.INFO.name());
        ProjectCommonException.throwServerErrorException(ResponseCode.internalError);
      }
    } else {
      ProjectCommonException.throwClientErrorException(ResponseCode.userNotFound);
    }
  }

  private void generateTelemetry(Map<String, Object> request) {
    Map<String, Object> targetObject = null;
    List<Map<String, Object>> correlatedObject = new ArrayList<>();
    targetObject =
        TelemetryUtil.generateTargetObject(
            (String) request.get(JsonKey.USER_ID), TelemetryEnvKey.USER, JsonKey.UPDATE, null);
    TelemetryUtil.generateCorrelatedObject(
        (String) request.get(JsonKey.USER_ID), TelemetryEnvKey.USER, null, correlatedObject);
    TelemetryUtil.telemetryProcessingCall(request, targetObject, correlatedObject);
  }

  private User removeUnUsedIdentity(User user, String type) {
    if (!(JsonKey.EMAIL.equalsIgnoreCase(type))) {
      user.setEmail(null);
      user.setMaskedEmail(null);
    }
    if (!(JsonKey.PHONE.equalsIgnoreCase(type))) {
      user.setPhone(null);
      user.setMaskedPhone(null);
    }
    if (JsonKey.PREV_USED_PHONE.equalsIgnoreCase(type)) {
      user.setPhone(user.getPrevUsedPhone());
    }
    if (JsonKey.PREV_USED_EMAIL.equalsIgnoreCase(type)) {
      user.setEmail(user.getPrevUsedEmail());
    }
    return user;
  }
}
