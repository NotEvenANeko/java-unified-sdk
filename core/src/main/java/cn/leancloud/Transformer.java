package cn.leancloud;

import cn.leancloud.annotation.AVClassName;
import cn.leancloud.utils.LogUtil;
import cn.leancloud.utils.StringUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Transformer {
  private static AVLogger logger = LogUtil.getLogger(Transformer.class);

  private static Pattern classnamePattern = Pattern.compile("^[a-zA-Z_][a-zA-Z_0-9]*$");
  private static final Map<String, Class<? extends AVObject>> subClassesMAP =
          new HashMap<String, Class<? extends AVObject>>();
  private static final Map<Class<? extends AVObject>, String> subClassesReverseMAP =
          new HashMap<Class<? extends AVObject>, String>();

  static Class<? extends AVObject> getSubClass(String className) {
    return subClassesMAP.get(className);
  }

  public static String getSubClassName(Class clazz) {
    if (AVUser.class.isAssignableFrom(clazz)) {
      return AVUser.CLASS_NAME;
    } else if (AVRole.class.isAssignableFrom(clazz)) {
      return AVRole.CLASS_NAME;
    } else if (AVStatus.class.isAssignableFrom(clazz)) {
      return AVStatus.CLASS_NAME;
    } else if (AVFile.class.isAssignableFrom(clazz)) {
      return AVFile.CLASS_NAME;
    } else if (AVFriendship.class.isAssignableFrom(clazz)) {
      return AVFriendship.CLASS_NAME;
    } else if (AVFriendshipRequest.class.isAssignableFrom(clazz)) {
      return AVFriendshipRequest.CLASS_NAME;
    } else {
      return subClassesReverseMAP.get(clazz);
    }
  }

  public static <T extends AVObject> void registerClass(Class<T> clazz) {
    AVClassName avClassName = clazz.getAnnotation(AVClassName.class);
    if (avClassName == null) {
      throw new IllegalArgumentException("The class is not annotated by @AVClassName");
    }
    String className = avClassName.value();
    checkClassName(className);
    subClassesMAP.put(className, clazz);
    subClassesReverseMAP.put(clazz, className);
  }

  public static <T extends AVObject> T transform(AVObject rawObj, String className) {
    if (null == rawObj) {
      return null;
    }
    AVObject result = objectFromClassName(className);
    result.resetByRawData(rawObj);
    return (T) result;
  }

  public static <T extends AVObject> T transform(AVObject rawObj, Class<T> clazz) {
    if (null == rawObj) {
      return null;
    }
    AVObject result = null;
    if (subClassesReverseMAP.containsKey(clazz)) {
      try {
        result = clazz.newInstance();
      } catch (Exception ex) {
        logger.w("newInstance failed. cause: " + ex.getMessage());
        result = new AVObject(clazz.getSimpleName());
      }
    } else if (AVUser.class.isAssignableFrom(clazz)) {
      result = new AVUser();
    } else if (AVRole.class.isAssignableFrom(clazz)) {
      result = new AVRole();
    } else if (AVStatus.class.isAssignableFrom(clazz)) {
      result = new AVStatus();
    } else if (AVFile.class.isAssignableFrom(clazz)) {
      result = new AVFile();
    } else if (AVFriendshipRequest.class.isAssignableFrom(clazz)) {
      result = new AVFriendshipRequest();
    } else if (AVFriendship.class.isAssignableFrom(clazz)) {
      result = new AVFriendship();
    } else {
      result = new AVObject(clazz.getSimpleName());
    }
    result.resetByRawData(rawObj);
    return (T)result;
  }

  public static void checkClassName(String className) {
    if (StringUtil.isEmpty(className))
      throw new IllegalArgumentException("Blank class name");
    if (!classnamePattern.matcher(className).matches())
      throw new IllegalArgumentException("Invalid class name");
  }

  public static AVObject objectFromClassName(String className) {
    AVObject result = null;
    if (AVUser.CLASS_NAME.equals(className)) {
      result = new AVUser();
    } else if (AVStatus.CLASS_NAME.equals(className)) {
      result = new AVStatus();
    } else if (AVRole.CLASS_NAME.equals(className)) {
      result = new AVRole();
    } else if (AVFile.CLASS_NAME.equals(className)) {
      result = new AVFile();
    } else if (AVInstallation.CLASS_NAME.equals(className)) {
      result = new AVInstallation();
    } else if (AVFriendshipRequest.CLASS_NAME.equals(className)) {
      result = new AVFriendshipRequest();
    } else if (AVFriendship.CLASS_NAME.equals(className)) {
      result = new AVFriendship();
    } else if (subClassesMAP.containsKey(className)) {
      try {
        result = subClassesMAP.get(className).newInstance();
      } catch (Exception ex) {
        logger.w("failed to create subClass: " + className, ex);
        result = new AVObject(className);
      }
    } else {
      result = new AVObject(className);
    }
    return result;
  }

  private Transformer() {
  }
}
