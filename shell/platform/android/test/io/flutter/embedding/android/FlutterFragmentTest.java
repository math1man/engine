package io.flutter.embedding.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.FragmentActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class FlutterFragmentTest {
  @Test
  public void itCreatesDefaultFragmentWithExpectedDefaults() {
    FlutterFragment fragment = FlutterFragment.createDefault();
    fragment.setDelegate(new FlutterActivityAndFragmentDelegate(fragment));

    assertEquals("main", fragment.getDartEntrypointFunctionName());
    assertEquals("/", fragment.getInitialRoute());
    assertArrayEquals(new String[] {}, fragment.getFlutterShellArgs().toArray());
    assertTrue(fragment.shouldAttachEngineToActivity());
    assertFalse(fragment.shouldHandleDeeplinking());
    assertNull(fragment.getCachedEngineId());
    assertTrue(fragment.shouldDestroyEngineWithHost());
    assertEquals(RenderMode.surface, fragment.getRenderMode());
    assertEquals(TransparencyMode.transparent, fragment.getTransparencyMode());
  }

  @Test
  public void itCreatesNewEngineFragmentWithRequestedSettings() {
    FlutterFragment fragment =
        FlutterFragment.withNewEngine()
            .dartEntrypoint("custom_entrypoint")
            .initialRoute("/custom/route")
            .shouldAttachEngineToActivity(false)
            .handleDeeplinking(true)
            .renderMode(RenderMode.texture)
            .transparencyMode(TransparencyMode.opaque)
            .build();
    fragment.setDelegate(new FlutterActivityAndFragmentDelegate(fragment));

    assertEquals("custom_entrypoint", fragment.getDartEntrypointFunctionName());
    assertEquals("/custom/route", fragment.getInitialRoute());
    assertArrayEquals(new String[] {}, fragment.getFlutterShellArgs().toArray());
    assertFalse(fragment.shouldAttachEngineToActivity());
    assertTrue(fragment.shouldHandleDeeplinking());
    assertNull(fragment.getCachedEngineId());
    assertTrue(fragment.shouldDestroyEngineWithHost());
    assertEquals(RenderMode.texture, fragment.getRenderMode());
    assertEquals(TransparencyMode.opaque, fragment.getTransparencyMode());
  }

  @Test
  public void itCreatesCachedEngineFragmentThatDoesNotDestroyTheEngine() {
    FlutterFragment fragment = FlutterFragment.withCachedEngine("my_cached_engine").build();

    assertTrue(fragment.shouldAttachEngineToActivity());
    assertEquals("my_cached_engine", fragment.getCachedEngineId());
    assertFalse(fragment.shouldDestroyEngineWithHost());
  }

  @Test
  public void itCreatesCachedEngineFragmentThatDestroysTheEngine() {
    FlutterFragment fragment =
        FlutterFragment.withCachedEngine("my_cached_engine")
            .destroyEngineWithFragment(true)
            .build();

    assertTrue(fragment.shouldAttachEngineToActivity());
    assertEquals("my_cached_engine", fragment.getCachedEngineId());
    assertTrue(fragment.shouldDestroyEngineWithHost());
  }

  @Test
  public void itCanBeDetachedFromTheEngineAndStopSendingFurtherEvents() {
    FlutterActivityAndFragmentDelegate mockDelegate =
        mock(FlutterActivityAndFragmentDelegate.class);
    FlutterFragment fragment =
        FlutterFragment.withCachedEngine("my_cached_engine")
            .destroyEngineWithFragment(true)
            .build();
    fragment.setDelegate(mockDelegate);
    fragment.onStart();
    fragment.onResume();

    verify(mockDelegate, times(1)).onStart();
    verify(mockDelegate, times(1)).onResume();

    fragment.onPause();
    fragment.detachFromFlutterEngine();
    verify(mockDelegate, times(1)).onPause();
    verify(mockDelegate, times(1)).onDestroyView();
    verify(mockDelegate, times(1)).onDetach();

    fragment.onStop();
    verify(mockDelegate, never()).onStop();

    fragment.onStart();
    fragment.onResume();
    // No more events through to the delegate.
    verify(mockDelegate, times(1)).onStart();
    verify(mockDelegate, times(1)).onResume();

    fragment.onDestroy();
    // 1 time same as before.
    verify(mockDelegate, times(1)).onDestroyView();
    verify(mockDelegate, times(1)).onDetach();
  }

  @Test
  public void itDelegatesOnBackPressedAutomaticallyWhenEnabled() {
    FlutterActivityAndFragmentDelegate mockDelegate =
        mock(FlutterActivityAndFragmentDelegate.class);
    FlutterFragment fragment =
        FlutterFragment.withCachedEngine("my_cached_engine")
            .destroyEngineWithFragment(true)
            .build();
    fragment.setDelegate(mockDelegate);
    ActivityController<FragmentActivity> activityController =
        Robolectric.setupActivity(FragmentActivity.class);
    activityController
        .get()
        .getSupportFragmentManager()
        .beginTransaction()
        .add(android.R.id.content, fragment)
        .commitNow();

    verify(mockDelegate, never()).onBackPressed();

    activityController.get().onBackPressed();

    verify(mockDelegate, times(1)).onBackPressed();
  }

  @Test
  public void itHandlesPopSystemNavigationAutomaticallyWhenEnabled() {
    FlutterActivityAndFragmentDelegate mockDelegate =
        mock(FlutterActivityAndFragmentDelegate.class);
    FlutterFragment fragment =
        FlutterFragment.withCachedEngine("my_cached_engine")
            .destroyEngineWithFragment(true)
            .build();
    fragment.setDelegate(mockDelegate);
    FragmentActivity activity =
        Robolectric.setupActivity(FragmentActivity.class);
    activity
        .getSupportFragmentManager()
        .beginTransaction()
        .add(android.R.id.content, fragment)
        .commitNow();
    OnBackPressedCallback callback = mock(OnBackPressedCallback.class);
    when(callback.isEnabled()).thenReturn(true);
    activity.getOnBackPressedDispatcher().addCallback(callback);

    assertTrue(fragment.popSystemNavigator());

    verify(mockDelegate, never()).onBackPressed();
    verify(callback, times(1)).handleOnBackPressed();
  }
}
