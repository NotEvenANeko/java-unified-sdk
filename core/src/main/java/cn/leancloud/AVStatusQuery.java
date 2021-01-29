package cn.leancloud;

import cn.leancloud.core.PaasClient;
import cn.leancloud.ops.Utils;
import cn.leancloud.utils.ErrorUtils;
import cn.leancloud.utils.StringUtil;
import cn.leancloud.json.JSONObject;
import io.reactivex.Observable;
import io.reactivex.functions.Function;

import java.util.List;
import java.util.Map;

public class AVStatusQuery extends AVQuery<AVStatus> {
  public enum SourceType {
    INBOX,
    OWNED
  }

  public enum PaginationDirection {
    NEW_TO_OLD(0),
    OLD_TO_NEW(1);
    PaginationDirection(int v) {
      value = v;
    }
    public int value() {
      return this.value;
    }
    int value;
  }

  private AVUser source = null;
  private AVUser owner = null;
  private String inboxType = null;

  private SourceType sourceType;
  private StatusIterator iterator;

  protected AVStatusQuery(SourceType type) {
    super(AVStatus.CLASS_NAME, AVStatus.class);
    sourceType = type;
    iterator = new StatusIterator(type);
  }

  /**
   * set since messageId.
   *
   * @param sinceId starter message id
   */
  public void setSinceId(long sinceId) {
    this.iterator.setSinceId(sinceId);
  }

  /**
   * get current since messageId.
   * @return since messageId
   */
  public long getSinceId() {
    return this.iterator.getSinceId();
  }

  /**
   * get current max messageId.
   * @return max messageId
   */
  public long getMaxId() {
    return this.iterator.getMaxId();
  }

  /**
   * set max messageId.
   * @param maxId max messageId
   */
  public void setMaxId(long maxId) {
    this.iterator.setMaxId(maxId);
  }

  /**
   * get pagination size.
   * @return pagination size.
   */
  public int getPageSize() {
    return iterator.getPageSize();
  }

  /**
   * set pagination size.
   * @param pageSize pagination size
   */
  public void setPageSize(int pageSize) {
    iterator.setPageSize(pageSize);
  }

  /**
   * set query direction.
   * @param direct pagination direction.
   */
  public void setDirection(PaginationDirection direct) {
    this.iterator.setDirection(direct);
  }

  void setSource(AVUser source) {
    this.source = source;
  }

  void setOwner(AVUser owner) {
    this.owner = owner;
    if (null != owner) {
      getInclude().add(AVStatus.ATTR_SOURCE);
    }
  }

  void setInboxType(String type) {
    this.inboxType = type;
  }

  /**
   * assemble query parameters.
   * @return parameter map.
   */
  @Override
  public Map<String, String> assembleParameters() {
    return assembleParameters(false);
  }

  private Map<String, String> assembleParameters(boolean withIterator) {
    if (SourceType.OWNED == this.sourceType) {
      // for status query, need to add inboxType filter into where clause.
      if (!StringUtil.isEmpty(inboxType)) {
        whereEqualTo(AVStatus.ATTR_INBOX_TYPE, inboxType);
      }
      if (null != this.source) {
        whereEqualTo(AVStatus.ATTR_SOURCE, this.source);
      }
      if (withIterator) {
        iterator.fillConditions(this);
      } else {
        if (PaginationDirection.NEW_TO_OLD == iterator.getDirection()) {
          addDescendingOrder(AVObject.KEY_CREATED_AT);
        } else {
          addAscendingOrder(AVObject.KEY_CREATED_AT);
        }
      }
    } else if (!withIterator) {
      if (PaginationDirection.NEW_TO_OLD == iterator.getDirection()) {
        addDescendingOrder(AVStatus.ATTR_MESSAGE_ID);
      } else {
        addAscendingOrder(AVStatus.ATTR_MESSAGE_ID);
      }
    }

    Map<String, String> result = super.assembleParameters();
    if (null != this.owner) {
      if (withIterator) {
        iterator.fillConditions(result);
      }
      if (!StringUtil.isEmpty(inboxType)) {
        // for inbox query, need to add inboxType filter on the top of parameter, it's different from status query.
        // maybe a bug?
        result.put(AVStatus.ATTR_INBOX_TYPE, inboxType);
      }
      String ownerString = JSONObject.Builder.create(Utils.mapFromAVObject(this.owner, false)).toJSONString();
      result.put(AVStatus.ATTR_OWNER, ownerString);
    } else if (SourceType.OWNED != this.sourceType && null != this.source) {
      String sourceString = JSONObject.Builder.create(Utils.mapFromAVObject(this.source, false)).toJSONString();
      result.put(AVStatus.ATTR_SOURCE, sourceString);
    }
    if (getPageSize() > 0) {
      result.put("limit", String.valueOf(getPageSize()));
    }

    return result;
  }

  protected Observable<List<AVStatus>> findInBackground(int explicitLimit) {
    return this.findInBackground(null, explicitLimit);
  }
  protected Observable<List<AVStatus>> findInBackground(AVUser asAuthenticatedUser, int explicitLimit) {
    return internalFindInBackground(asAuthenticatedUser, explicitLimit, false);
  }

  private Observable<List<AVStatus>> internalFindInBackground(AVUser asAuthenticatedUser,
                                                              int explicitLimit, boolean enableIterator) {
    if (null == this.owner && null == this.source) {
      return Observable.error(ErrorUtils.illegalArgument("source or owner is null, please initialize correctly."));
    }
    if (null != this.owner && !this.owner.isAuthenticated()) {
      return Observable.error(ErrorUtils.sessionMissingException());
    }
    Map<String, String> query;
    if (enableIterator) {
      query = assembleParameters(true);
    } else {
      query = assembleParameters();
    }
    if (explicitLimit > 0) {
      query.put("limit", String.valueOf(explicitLimit));
    }

    if (null != this.owner) {
      return PaasClient.getStorageClient().queryInbox(asAuthenticatedUser, query).map(new Function<List<AVStatus>, List<AVStatus>>() {
        @Override
        public List<AVStatus> apply(List<AVStatus> avStatuses) throws Exception {
          if (null == avStatuses || avStatuses.size() < 1) {
            return avStatuses;
          }
          for (AVStatus status: avStatuses) {
            iterator.encounter(status);
          }
          return avStatuses;
        }
      });
    } else {
      return PaasClient.getStorageClient().queryStatus(asAuthenticatedUser, query).map(new Function<List<AVStatus>, List<AVStatus>>() {
        @Override
        public List<AVStatus> apply(List<AVStatus> avStatuses) throws Exception {
          if (null == avStatuses || avStatuses.size() < 1) {
            return avStatuses;
          }
          for (AVStatus status: avStatuses) {
            iterator.encounter(status);
          }
          return avStatuses;
        }
      });
    }
  }

  /**
   * get next pagination result.
   * @return observable instance.
   */
  public Observable<List<AVStatus>> nextInBackground() {
    return internalFindInBackground(null, 0, true);
  }

  /**
   * get status count.
   * @return observable instance.
   */
  @Override
  public Observable<Integer> countInBackground() {
    if (null == this.owner && null == this.source) {
      return Observable.error(ErrorUtils.illegalArgument("source or owner is null, please initialize correctly."));
    }
    if (null != this.owner) {
      return Observable.error(ErrorUtils.invalidStateException("countInBackground doesn't work for inbox query," +
              " please use unreadCountInBackground."));
    }

    Map<String, String> query = assembleParameters();
    query.put("count", "1");
    query.put("limit", "0");
    return PaasClient.getStorageClient().queryCount(null, AVStatus.CLASS_NAME, query);
  }

  /**
   * get (read, unread) count.
   *
   * @return observable instance.
   */
  public Observable<JSONObject> unreadCountInBackground() {
    if (null == this.owner || !this.owner.isAuthenticated()) {
      return Observable.error(ErrorUtils.sessionMissingException());
    }
    Map<String, String> query = assembleParameters();
    query.put("count", "1");
    query.put("limit", "0");
    return PaasClient.getStorageClient().getInboxCount(null, query);
  }
}
