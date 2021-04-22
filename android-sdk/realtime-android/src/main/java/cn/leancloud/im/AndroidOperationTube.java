package cn.leancloud.im;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import cn.leancloud.AVException;
import cn.leancloud.AVInstallation;
import cn.leancloud.AVLogger;
import cn.leancloud.LeanCloud;
import cn.leancloud.callback.AVCallback;
import cn.leancloud.codec.MDFive;
import cn.leancloud.im.v2.AVIMClient;
import cn.leancloud.im.v2.AVIMClient.AVIMClientStatus;
import cn.leancloud.im.v2.AVIMException;
import cn.leancloud.im.v2.AVIMMessage;
import cn.leancloud.im.v2.AVIMMessageOption;
import cn.leancloud.im.v2.Conversation;
import cn.leancloud.im.v2.Conversation.AVIMOperation;
import cn.leancloud.im.v2.callback.AVIMClientCallback;
import cn.leancloud.im.v2.callback.AVIMClientStatusCallback;
import cn.leancloud.im.v2.callback.AVIMCommonJsonCallback;
import cn.leancloud.im.v2.callback.AVIMConversationCallback;
import cn.leancloud.im.v2.callback.AVIMConversationIterableResult;
import cn.leancloud.im.v2.callback.AVIMConversationIterableResultCallback;
import cn.leancloud.im.v2.callback.AVIMMessagesQueryCallback;
import cn.leancloud.im.v2.callback.AVIMOnlineClientsCallback;
import cn.leancloud.json.JSON;
import cn.leancloud.livequery.AVLiveQuery;
import cn.leancloud.livequery.AVLiveQuerySubscribeCallback;
import cn.leancloud.push.PushService;
import cn.leancloud.session.AVConnectionManager;
import cn.leancloud.session.AVSession;
import cn.leancloud.session.AVSessionManager;
import cn.leancloud.utils.LogUtil;
import cn.leancloud.utils.StringUtil;

/**
 * Created by fengjunwen on 2018/7/3.
 */

public class AndroidOperationTube implements OperationTube {
  private static AVLogger LOGGER = LogUtil.getLogger(AndroidOperationTube.class);

  public boolean openClient(AVConnectionManager connectionManager, final String clientId, String tag, String userSessionToken,
                            boolean reConnect, final AVIMClientCallback callback) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CLIENT_TAG, tag);
    params.put(Conversation.PARAM_CLIENT_USERSESSIONTOKEN, userSessionToken);
    params.put(Conversation.PARAM_CLIENT_RECONNECTION, reConnect);

    LOGGER.d("openClient. clientId:" + clientId + ", tag:" + tag + ", callback:" + callback);
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          LOGGER.d("openClient get response. error:" + error);
          callback.internalDone(AVIMClient.getInstance(clientId), AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, JSON.toJSONString(params), receiver,
        AVIMOperation.CLIENT_OPEN);
  }

  public boolean queryClientStatus(AVConnectionManager connectionManager, String clientId, final AVIMClientStatusCallback callback) {
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          AVIMClientStatus status = null;
          if (null != intentResult
              && intentResult.containsKey(Conversation.callbackClientStatus)) {
            status = AVIMClientStatus.getClientStatus((int) intentResult.get(Conversation.callbackClientStatus));
          }
          callback.internalDone(status, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, null, receiver, AVIMOperation.CLIENT_STATUS);
  }

  public boolean closeClient(AVConnectionManager connectionManager, final String self, final AVIMClientCallback callback) {
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          AVIMClient client = AVIMClient.getInstance(self);
          callback.internalDone(client, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(self, null, receiver, AVIMOperation.CLIENT_DISCONNECT);
  }

  public boolean renewSessionToken(AVConnectionManager connectionManager, String clientId, final AVIMClientCallback callback) {
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          callback.internalDone(null, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, null, receiver, AVIMOperation.CLIENT_REFRESH_TOKEN);
  }

  public boolean queryOnlineClients(AVConnectionManager connectionManager, String self, List<String> clients, final AVIMOnlineClientsCallback callback) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_ONLINE_CLIENTS, clients);

    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          if (error != null) {
            callback.internalDone(null, AVIMException.wrapperAVException(error));
          } else {
            List<String> onlineClients = null;
            if (null != intentResult && intentResult.containsKey(Conversation.callbackOnlineClients)) {
              onlineClients = (List<String>) intentResult.get(Conversation.callbackOnlineClients);
            }
            callback.internalDone(onlineClients, null);
          }
        }
      };
    }

    return this.sendClientCMDToPushService(self, JSON.toJSONString(params), receiver, AVIMOperation.CLIENT_ONLINE_QUERY);
  }

  public boolean createConversation(AVConnectionManager connectionManager, final String self, final List<String> members,
                                    final Map<String, Object> attributes, final boolean isTransient, final boolean isUnique,
                                    final boolean isTemp, int tempTTL, final AVIMCommonJsonCallback callback) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put(Conversation.PARAM_CONVERSATION_MEMBER, members);
    params.put(Conversation.PARAM_CONVERSATION_ISUNIQUE, isUnique);
    params.put(Conversation.PARAM_CONVERSATION_ISTRANSIENT, isTransient);
    params.put(Conversation.PARAM_CONVERSATION_ISTEMPORARY, isTemp);
    if (isTemp) {
      params.put(Conversation.PARAM_CONVERSATION_TEMPORARY_TTL, tempTTL);
    }
    if (null != attributes && attributes.size() > 0) {
      params.put(Conversation.PARAM_CONVERSATION_ATTRIBUTE, attributes);
    }
    BroadcastReceiver receiver = null;
    if (null != callback) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          callback.internalDone(intentResult, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(self, JSON.toJSONString(params), receiver,
        AVIMOperation.CONVERSATION_CREATION);
  }

  public boolean updateConversation(AVConnectionManager connectionManager, final String clientId, String conversationId, int convType,
                                    final Map<String, Object> param, final AVIMCommonJsonCallback callback) {
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {

        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          callback.internalDone(intentResult, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, conversationId, convType, JSON.toJSONString(param),
        null, null, AVIMOperation.CONVERSATION_UPDATE, receiver);
  }

  public boolean participateConversation(AVConnectionManager connectionManager, final String clientId, String conversationId, int convType, final Map<String, Object> param,
                                         Conversation.AVIMOperation operation, final AVIMConversationCallback callback) {
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {

        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          callback.internalDone(intentResult, AVIMException.wrapperAVException(error));
        }
      };
    }
    String paramString = null != param ? JSON.toJSONString(param) : null;
    return this.sendClientCMDToPushService(clientId, conversationId, convType, paramString,
        null, null, operation, receiver);
  }

  public boolean queryConversations(AVConnectionManager connectionManager, final String clientId, final String queryString, final AVIMCommonJsonCallback callback) {
    BroadcastReceiver receiver = null;
    if (callback != null) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {

        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          callback.internalDone(intentResult, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, queryString, receiver, AVIMOperation.CONVERSATION_QUERY);
  }

  public boolean queryConversationsInternally(AVConnectionManager connectionManager, final String clientId, final String queryString,
                                              final AVIMCommonJsonCallback callback) {
    // internal query conversation.
    LOGGER.d("queryConversationsInternally...");
    int requestId = WindTalker.getNextIMRequestId();
    RequestCache.getInstance().addRequestCallback(clientId, null, requestId, callback);
    AVSession session = AVSessionManager.getInstance().getOrCreateSession(clientId, AVInstallation.getCurrentInstallation().getInstallationId(), connectionManager);
    session.queryConversations(JSON.parseObject(queryString, Map.class), requestId, MDFive.computeMD5(queryString));
    return true;
  }

  public boolean sendMessage(AVConnectionManager connectionManager, String clientId, String conversationId, int convType, final AVIMMessage message,
                             final AVIMMessageOption messageOption, final AVIMCommonJsonCallback callback) {
    BroadcastReceiver receiver = null;
    if (null != callback) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          callback.internalDone(intentResult, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, conversationId, convType, null,
        message, messageOption, AVIMOperation.CONVERSATION_SEND_MESSAGE, receiver);
  }

  public boolean updateMessage(AVConnectionManager connectionManager, String clientId, int convType, AVIMMessage oldMessage, AVIMMessage newMessage,
                               final AVIMCommonJsonCallback callback) {
    BroadcastReceiver receiver = null;
    if (null != callback) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          callback.internalDone(intentResult, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService2(clientId, oldMessage.getConversationId(), convType, oldMessage,
        newMessage, AVIMOperation.CONVERSATION_UPDATE_MESSAGE, receiver);
  }

  public boolean recallMessage(AVConnectionManager connectionManager, String clientId, int convType, AVIMMessage message,
                               final AVIMCommonJsonCallback callback) {
    BroadcastReceiver receiver = null;
    if (null != callback) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          callback.internalDone(intentResult, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, message.getConversationId(), convType, null,
        message, null, AVIMOperation.CONVERSATION_RECALL_MESSAGE, receiver);
  }

  public boolean fetchReceiptTimestamps(AVConnectionManager connectionManager, String clientId,
                                        String conversationId, int convType, Conversation.AVIMOperation operation,
                                        final AVIMCommonJsonCallback callback) {
    return false;
  }

  public boolean queryMessages(AVConnectionManager connectionManager, String clientId, String conversationId, int convType, String params,
                               final Conversation.AVIMOperation operation, final AVIMMessagesQueryCallback callback) {
    BroadcastReceiver receiver = null;
    if (null != callback) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          List<AVIMMessage> msg = (null == intentResult) ?
              null : (List<AVIMMessage>) intentResult.get(Conversation.callbackHistoryMessages);
          callback.internalDone(msg, AVIMException.wrapperAVException(error));
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, conversationId, convType, params, null, null,
        AVIMOperation.CONVERSATION_MESSAGE_QUERY, receiver);
  }

  public boolean processMembers(AVConnectionManager connectionManager, String clientId,
                                String conversationId, int convType, String params,
                                Conversation.AVIMOperation op, final AVCallback callback) {
    BroadcastReceiver receiver = null;
    if (null != callback) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          if (AVIMOperation.CONVERSATION_MEMBER_COUNT_QUERY == op) {
            int result = 0;
            if (null != intentResult) {
              Object memberCount = intentResult.get(Conversation.callbackMemberCount);
              if (memberCount instanceof Integer) {
                result = (Integer) memberCount;
              }
            }
            callback.internalDone(result, AVIMException.wrapperAVException(error));
          } else if (AVIMOperation.CONVERSATION_BLOCKED_MEMBER_QUERY == op
              || AVIMOperation.CONVERSATION_MUTED_MEMBER_QUERY == op) {
            List<String> result = new ArrayList<>();
            String next = null;
            if (null != intentResult) {
              Object memberList = intentResult.get(Conversation.callbackData);
              next = (String) intentResult.get(Conversation.callbackIterableNext);
              if (memberList instanceof Collection) {
                result.addAll((Collection<? extends String>) memberList);
              } else if (memberList instanceof String[]) {
                result.addAll(Arrays.asList((String[])memberList));
              }
            }
            if (callback instanceof AVIMConversationIterableResultCallback) {
              AVIMConversationIterableResult iterableResult = new AVIMConversationIterableResult();
              iterableResult.setMembers(result);
              iterableResult.setNext(next);
              callback.internalDone(iterableResult, AVIMException.wrapperAVException(error));
            } else {
              callback.internalDone(result, AVIMException.wrapperAVException(error));
            }
          } else {
            callback.internalDone(intentResult, AVIMException.wrapperAVException(error));
          }
        }
      };
    }
    return this.sendClientCMDToPushService(clientId, conversationId, convType, params, null, null,
        op, receiver);
  }

  public boolean markConversationRead(AVConnectionManager connectionManager, String clientId, String conversationId, int convType,
                                      Map<String, Object> lastMessageParam) {
    String dataString = null == lastMessageParam ? null : JSON.toJSONString(lastMessageParam);
    return this.sendClientCMDToPushService(clientId, conversationId, convType, dataString,
        null, null, AVIMOperation.CONVERSATION_READ, null);
  }

  public boolean loginLiveQuery(AVConnectionManager connectionManager, String subscriptionId, final AVLiveQuerySubscribeCallback callback) {
    BroadcastReceiver receiver = null;
    if (null != callback) {
      receiver = new LCIMBaseBroadcastReceiver(callback) {
        @Override
        public void execute(Map<String, Object> intentResult, Throwable error) {
          if (null != callback) {
            callback.internalDone(null == error ? null : new AVException(error));
          }
        }
      };
    }
    if (LeanCloud.getContext() == null) {
      LOGGER.e("failed to startService. cause: root Context is null.");
      if (null != callback) {
        callback.internalDone(new AVException(AVException.OTHER_CAUSE,
            "root Context is null, please initialize at first."));
      }
      return false;
    }
    int requestId = WindTalker.getNextIMRequestId();
    LocalBroadcastManager.getInstance(LeanCloud.getContext()).registerReceiver(receiver,
        new IntentFilter(AVLiveQuery.LIVEQUERY_PRIFIX + requestId));
    try {
      Intent i = new Intent(LeanCloud.getContext(), PushService.class);
      i.setAction(AVLiveQuery.ACTION_LIVE_QUERY_LOGIN);
      i.putExtra(AVLiveQuery.SUBSCRIBE_ID, subscriptionId);
      i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
      LeanCloud.getContext().startService(IntentUtil.setupIntentFlags(i));
    } catch (Exception ex) {
      LOGGER.e("failed to start PushServer. cause: " + ex.getMessage());
      return false;
    }
    return true;
  }

  protected boolean sendClientCMDToPushService(String clientId, String dataAsString, BroadcastReceiver receiver,
                                               AVIMOperation operation) {

    if (LeanCloud.getContext() == null) {
      LOGGER.e("failed to startService. cause: root Context is null.");
      if (null != receiver && receiver instanceof LCIMBaseBroadcastReceiver) {
        ((LCIMBaseBroadcastReceiver)receiver).execute(new HashMap<>(),
            new AVException(AVException.OTHER_CAUSE, "root Context is null, please initialize at first."));
      }
      return false;
    }
    int requestId = WindTalker.getNextIMRequestId();

    if (receiver != null) {
      LocalBroadcastManager.getInstance(LeanCloud.getContext()).registerReceiver(receiver,
          new IntentFilter(operation.getOperation() + requestId));
    }
    Intent i = new Intent(LeanCloud.getContext(), PushService.class);
    i.setAction(Conversation.AV_CONVERSATION_INTENT_ACTION);
    if (!StringUtil.isEmpty(dataAsString)) {
      i.putExtra(Conversation.INTENT_KEY_DATA, dataAsString);
    }

    i.putExtra(Conversation.INTENT_KEY_CLIENT, clientId);
    i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
    i.putExtra(Conversation.INTENT_KEY_OPERATION, operation.getCode());
    try {
      LeanCloud.getContext().startService(IntentUtil.setupIntentFlags(i));
    } catch (Exception ex) {
      LOGGER.e("failed to startService. cause: " + ex.getMessage());
      return false;
    }
    return true;
  }

  protected boolean sendClientCMDToPushService(String clientId, String conversationId, int convType,
                                               String dataAsString, final AVIMMessage message,
                                               final AVIMMessageOption option, final AVIMOperation operation,
                                               BroadcastReceiver receiver) {
    if (LeanCloud.getContext() == null) {
      LOGGER.e("failed to startService. cause: root Context is null.");
      if (null != receiver && receiver instanceof LCIMBaseBroadcastReceiver) {
        ((LCIMBaseBroadcastReceiver)receiver).execute(new HashMap<>(),
            new AVException(AVException.OTHER_CAUSE, "root Context is null, please initialize at first."));
      }
      return false;
    }

    int requestId = WindTalker.getNextIMRequestId();
    if (null != receiver) {
      LocalBroadcastManager.getInstance(LeanCloud.getContext()).registerReceiver(receiver,
          new IntentFilter(operation.getOperation() + requestId));
    }
    Intent i = new Intent(LeanCloud.getContext(), PushService.class);
    i.setAction(Conversation.AV_CONVERSATION_INTENT_ACTION);
    if (!StringUtil.isEmpty(dataAsString)) {
      i.putExtra(Conversation.INTENT_KEY_DATA, dataAsString);
    }
    if (null != message) {
      i.putExtra(Conversation.INTENT_KEY_DATA, message.toJSONString());
      if (null != option) {
        i.putExtra(Conversation.INTENT_KEY_MESSAGE_OPTION, option.toJSONString());
      }
    }
    i.putExtra(Conversation.INTENT_KEY_CLIENT, clientId);
    i.putExtra(Conversation.INTENT_KEY_CONVERSATION, conversationId);
    i.putExtra(Conversation.INTENT_KEY_CONV_TYPE, convType);
    i.putExtra(Conversation.INTENT_KEY_OPERATION, operation.getCode());
    i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
    try {
      LeanCloud.getContext().startService(IntentUtil.setupIntentFlags(i));
    } catch (Exception ex) {
      LOGGER.e("failed to startService. cause: " + ex.getMessage());
      return false;
    }
    return true;
  }

  protected boolean sendClientCMDToPushService2(String clientId, String conversationId, int convType,
                                                final AVIMMessage message, final AVIMMessage message2,
                                                final AVIMOperation operation,
                                                BroadcastReceiver receiver) {
    if (LeanCloud.getContext() == null) {
      LOGGER.e("failed to startService. cause: root Context is null.");
      if (null != receiver && receiver instanceof LCIMBaseBroadcastReceiver) {
        ((LCIMBaseBroadcastReceiver)receiver).execute(new HashMap<>(),
            new AVException(AVException.OTHER_CAUSE, "root Context is null, please initialize at first."));
      }
      return false;
    }
    int requestId = WindTalker.getNextIMRequestId();
    if (null != receiver) {
      LocalBroadcastManager.getInstance(LeanCloud.getContext()).registerReceiver(receiver,
          new IntentFilter(operation.getOperation() + requestId));
    }
    Intent i = new Intent(LeanCloud.getContext(), PushService.class);
    i.setAction(Conversation.AV_CONVERSATION_INTENT_ACTION);

    if (null != message) {
      i.putExtra(Conversation.INTENT_KEY_DATA, message.toJSONString());
    }
    if (null != message2) {
      i.putExtra(Conversation.INTENT_KEY_MESSAGE_EX, message2.toJSONString());
    }
    i.putExtra(Conversation.INTENT_KEY_CLIENT, clientId);
    i.putExtra(Conversation.INTENT_KEY_CONVERSATION, conversationId);
    i.putExtra(Conversation.INTENT_KEY_CONV_TYPE, convType);
    i.putExtra(Conversation.INTENT_KEY_OPERATION, operation.getCode());
    i.putExtra(Conversation.INTENT_KEY_REQUESTID, requestId);
    try {
      LeanCloud.getContext().startService(IntentUtil.setupIntentFlags(i));
    } catch (Exception ex) {
      LOGGER.e("failed to startService. cause: " + ex.getMessage());
      return false;
    }
    return true;
  }

  // response notifier
  public void onOperationCompleted(String clientId, String conversationId, int requestId,
                                   Conversation.AVIMOperation operation, Throwable throwable) {
    if (AVIMOperation.CONVERSATION_QUERY == operation) {
      AVCallback callback = RequestCache.getInstance().getRequestCallback(clientId, null, requestId);
      if (null != callback) {
        // internal query conversation.
        callback.internalDone(null, AVIMException.wrapperAVException(throwable));
        RequestCache.getInstance().cleanRequestCallback(clientId, null, requestId);
        return;
      }
    }
    IntentUtil.sendIMLocalBroadcast(clientId, conversationId, requestId, throwable, operation);
  }

  public void onOperationCompletedEx(String clientId, String conversationId, int requestId,
                                     Conversation.AVIMOperation operation, HashMap<String, Object> resultData) {
    if (AVIMOperation.CONVERSATION_QUERY == operation) {
      AVCallback callback = RequestCache.getInstance().getRequestCallback(clientId, null, requestId);
      if (null != callback) {
        // internal query conversation.
        callback.internalDone(resultData, null);
        RequestCache.getInstance().cleanRequestCallback(clientId, null, requestId);
        return;
      }
    }
    IntentUtil.sendMap2LocalBroadcase(clientId, conversationId, requestId, resultData, null, operation);
    return;
  }

  public void onLiveQueryCompleted(int requestId, Throwable throwable) {
    IntentUtil.sendLiveQueryLocalBroadcast(requestId, throwable);
  }

  public void onPushMessage(String message, String messageId) {
    return;
  }
}
