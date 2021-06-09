package cn.leancloud.callback;

import cn.leancloud.LCException;

abstract class GenericRetryCallback extends GenericObjectCallback {
  GenericObjectCallback callback;

  public GenericRetryCallback(GenericObjectCallback callback) {
    this.callback = callback;
  }

  @Override
  public void onSuccess(String content, LCException e) {
    if (callback != null) {
      callback.onSuccess(content, e);
    }
  }

  @Override
  public void onFailure(Throwable error, String content) {
    if (callback != null) {
      callback.onFailure(error, content);
    }
  }

  @Override
  public boolean isRequestStatisticNeed() {
    return callback.isRequestStatisticNeed();
  }
}
