package cn.leancloud;

import cn.leancloud.core.LeanCloud;

public class Configure {
  public static final String TEST_APP_ID = "dYRQ8YfHRiILshUnfFJu2eQM-gzGzoHsz";
  public static final String TEST_APP_KEY = "ye24iIK6ys8IvaISMC4Bs5WK";
  public static final LeanCloud.REGION REGION = LeanCloud.REGION.NorthChina;

  public static void initialize() {
    LeanCloud.setRegion(LeanCloud.REGION.NorthChina);
    LeanCloud.setLogLevel(LCLogger.Level.VERBOSE);
    LeanCloud.initialize(Configure.TEST_APP_ID, Configure.TEST_APP_KEY);
  }

  public static void initializeWithApp(String appId, String appKey) {
    LeanCloud.setRegion(LeanCloud.REGION.NorthChina);
    LeanCloud.setLogLevel(LCLogger.Level.VERBOSE);
    LeanCloud.initialize(appId, appKey);
  }
}
