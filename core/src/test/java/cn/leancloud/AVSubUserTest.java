package cn.leancloud;

import cn.leancloud.json.JSON;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class AVSubUserTest extends TestCase {
  private boolean operationSucceed = false;
  public static final String USERNAME = "jfeng20200618";
  public static final String PASSWORD = "FER$@$@#Ffwe";
  private static final String EMAIL = "jfeng20200618@test.com";

  public AVSubUserTest(String name) {
    super(name);
    AVUser.alwaysUseSubUserClass(SubUser.class);
    Configure.initializeRuntime();
  }

  public static Test suite() {
    return new TestSuite(AVSubUserTest.class);
  }

  @Override
  protected void setUp() throws Exception {
    operationSucceed = false;
  }

  @Override
  protected void tearDown() throws Exception {
    ;
  }

  public void testSingupWithEmail() throws Exception {
    SubUser user = new SubUser();
    user.setEmail(EMAIL);
    user.setUsername(USERNAME);
    user.setPassword(PASSWORD);
    final CountDownLatch latch = new CountDownLatch(1);
    user.signUpInBackground().subscribe(new Observer<AVUser>() {
      public void onSubscribe(Disposable disposable) {

      }

      public void onNext(AVUser avUser) {
        System.out.println(JSON.toJSONString(avUser));
        operationSucceed = true;
        latch.countDown();

      }

      public void onError(Throwable throwable) {
        operationSucceed = true;
        latch.countDown();
      }

      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

  public void testLogin() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    AVUser.logIn("jfeng", "FER$@$@#Ffwe").subscribe(new Observer<AVUser>() {
      public void onSubscribe(Disposable disposable) {
        System.out.println("onSubscribe " + disposable.toString());
      }

      public void onNext(AVUser avUser) {
        System.out.println("onNext. result=" + JSON.toJSONString(avUser));
        operationSucceed = avUser instanceof SubUser;

        AVUser currentUser = AVUser.getCurrentUser();
        System.out.println("currentUser. result=" + JSON.toJSONString(currentUser));
        System.out.println("sessionToken=" + currentUser.getSessionToken() + ", isAuthenticated=" + currentUser.isAuthenticated());

        operationSucceed = operationSucceed & (currentUser instanceof SubUser);
        latch.countDown();
      }

      public void onError(Throwable throwable) {
        latch.countDown();
      }

      public void onComplete() {
        System.out.println("onComplete");
      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

  public void testAnonymousLogin() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    AVUser.logInAnonymously().subscribe(new Observer<AVUser>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(AVUser avUser) {
        System.out.println("onNext. result=" + avUser.toString());
        operationSucceed = avUser instanceof SubUser;
        latch.countDown();
      }

      @Override
      public void onError(Throwable throwable) {
        latch.countDown();
      }

      @Override
      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

  public void testQueryUser() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    AVQuery<? extends AVUser> query = AVUser.getQuery();
    query.findInBackground().subscribe(new Observer<List<? extends AVUser>>() {
      @Override
      public void onSubscribe(Disposable disposable) {

      }

      @Override
      public void onNext(List<? extends AVUser> avUsers) {
        operationSucceed = true;
        latch.countDown();
      }

      @Override
      public void onError(Throwable throwable) {
        latch.countDown();
      }

      @Override
      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

  public void testCurrentUserWithNew() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    AVUser.logIn("jfeng", "FER$@$@#Ffwe").subscribe(new Observer<AVUser>() {
      public void onSubscribe(Disposable disposable) {
        System.out.println("onSubscribe " + disposable.toString());
      }

      public void onNext(AVUser avUser) {
        System.out.println("onNext. result=" + avUser.toString());
        AVUser.changeCurrentUser(avUser, true);
        AVUser u = AVUser.getCurrentUser();
        operationSucceed = avUser.equals(u);
        latch.countDown();
      }

      public void onError(Throwable throwable) {
        latch.countDown();
      }

      public void onComplete() {
        System.out.println("onComplete");
      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }

  public void testCurrentUserWithCached() throws Exception {
    AVUser.changeCurrentUser(null, true);
    AVUser current = AVUser.getCurrentUser();
    assertNull(current);
  }

  public void testCheckAuthenticatedFalse() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    SubUser u = new SubUser();
    u.setEmail("jfeng@test.com");
    u.setUsername("jfeng");
    u.setPassword("FER$@$@#Ffwe");
    u.setObjectId("ferewr2343");
    u.checkAuthenticatedInBackground().subscribe(new Observer<Boolean>() {
      public void onSubscribe(Disposable disposable) {

      }

      public void onNext(Boolean aBoolean) {
        operationSucceed = aBoolean;
        latch.countDown();
      }

      public void onError(Throwable throwable) {
        latch.countDown();
      }

      public void onComplete() {

      }
    });
    latch.await();
    assertTrue(!operationSucceed);
  }

  public void testCheckAuthenticatedTrue() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
    AVUser.logIn("jfeng", "FER$@$@#Ffwe").subscribe(new Observer<AVUser>() {
      public void onSubscribe(Disposable disposable) {
        System.out.println("onSubscribe " + disposable.toString());
      }

      public void onNext(AVUser avUser) {
        avUser.checkAuthenticatedInBackground().subscribe(new Observer<Boolean>() {
          public void onSubscribe(Disposable disposable) {

          }

          public void onNext(Boolean aBoolean) {
            operationSucceed = aBoolean;
            latch.countDown();
          }

          public void onError(Throwable throwable) {
            latch.countDown();
          }

          public void onComplete() {

          }
        });
      }

      public void onError(Throwable throwable) {
        latch.countDown();
      }

      public void onComplete() {
      }
    });
    latch.await();
    assertTrue(operationSucceed);
  }
}
