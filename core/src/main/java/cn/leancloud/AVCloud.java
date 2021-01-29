package cn.leancloud;

import cn.leancloud.core.PaasClient;
import cn.leancloud.ops.Utils;
import io.reactivex.Observable;

import java.util.Map;

public class AVCloud {
  private static boolean isProduction = true;
  /**
   * 设置调用云代码函数的测试环境或者生产环境，默认为true，也就是生产环境。
   * @param productionMode flag to production mode.
   */
  public static void setProductionMode(boolean productionMode) {
    isProduction = productionMode;
  }

  /**
   * Whether current mode is production or not.
   * @return flag to production mode.
   */
  public static boolean isProductionMode() {return isProduction;}

  /**
   * Call Cloud Function in Background.
   * @param name function name.
   * @param params invoke parameters.
   * @param <T> template type.
   * @return observable instance.
   */
  public static <T> Observable<T> callFunctionInBackground(String name, Map<String, Object> params) {
    return PaasClient.getStorageClient().callFunction(null, name, Utils.getParsedMap(params));
  }

  public static <T> Observable<T> callFunctionWithCacheInBackground(String name, Map<String, Object> params,
                                                                    AVQuery.CachePolicy cachePolicy, long maxCacheAge) {
    return PaasClient.getStorageClient().callFunctionWithCachePolicy(name, Utils.getParsedMap(params), cachePolicy, maxCacheAge);
  }

  /**
   * Call Cloud RPC Function in Background.
   * @param name function name.
   * @param params invoke parameters.
   * @param <T> template type.
   * @return observable instance.
   */
  public static <T> Observable<T> callRPCInBackground(String name, Object params) {
    return PaasClient.getStorageClient().callRPC(null, name, Utils.getParsedObject(params));
  }

  public static <T> Observable<T> callRPCWithCacheInBackground(String name, Map<String, Object> params,
                                                               AVQuery.CachePolicy cachePolicy, long maxCacheAge) {
    return PaasClient.getStorageClient().callRPCWithCachePolicy(name, Utils.getParsedMap(params), cachePolicy, maxCacheAge);
  }

  private AVCloud() {
  }
}
