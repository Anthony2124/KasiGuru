package com.kasiguru;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.kasiguru.data.local.KasiGuruDatabase;
import com.kasiguru.data.local.dao.AchievementDao;
import com.kasiguru.data.local.dao.GameScoreDao;
import com.kasiguru.data.local.dao.StoryDao;
import com.kasiguru.data.local.dao.UserProgressDao;
import com.kasiguru.data.local.dao.VocabularyDao;
import com.kasiguru.data.repository.GameRepository;
import com.kasiguru.data.repository.StoryRepository;
import com.kasiguru.data.repository.UserProgressRepository;
import com.kasiguru.data.repository.VocabularyRepository;
import com.kasiguru.di.DatabaseModule_ProvideAchievementDaoFactory;
import com.kasiguru.di.DatabaseModule_ProvideDatabaseFactory;
import com.kasiguru.di.DatabaseModule_ProvideGameScoreDaoFactory;
import com.kasiguru.di.DatabaseModule_ProvideStoryDaoFactory;
import com.kasiguru.di.DatabaseModule_ProvideUserProgressDaoFactory;
import com.kasiguru.di.DatabaseModule_ProvideVocabularyDaoFactory;
import com.kasiguru.ui.screens.achievements.AchievementsViewModel;
import com.kasiguru.ui.screens.achievements.AchievementsViewModel_HiltModules;
import com.kasiguru.ui.screens.auth.LoginViewModel;
import com.kasiguru.ui.screens.auth.LoginViewModel_HiltModules;
import com.kasiguru.ui.screens.auth.RegisterViewModel;
import com.kasiguru.ui.screens.auth.RegisterViewModel_HiltModules;
import com.kasiguru.ui.screens.auth.SplashViewModel;
import com.kasiguru.ui.screens.auth.SplashViewModel_HiltModules;
import com.kasiguru.ui.screens.flashcards.FlashcardViewModel;
import com.kasiguru.ui.screens.flashcards.FlashcardViewModel_HiltModules;
import com.kasiguru.ui.screens.games.AspectBuilderViewModel;
import com.kasiguru.ui.screens.games.AspectBuilderViewModel_HiltModules;
import com.kasiguru.ui.screens.games.AudioQuizViewModel;
import com.kasiguru.ui.screens.games.AudioQuizViewModel_HiltModules;
import com.kasiguru.ui.screens.games.FillBlankViewModel;
import com.kasiguru.ui.screens.games.FillBlankViewModel_HiltModules;
import com.kasiguru.ui.screens.games.GamesViewModel;
import com.kasiguru.ui.screens.games.GamesViewModel_HiltModules;
import com.kasiguru.ui.screens.games.SentenceOrderViewModel;
import com.kasiguru.ui.screens.games.SentenceOrderViewModel_HiltModules;
import com.kasiguru.ui.screens.games.WordMatchViewModel;
import com.kasiguru.ui.screens.games.WordMatchViewModel_HiltModules;
import com.kasiguru.ui.screens.home.HomeViewModel;
import com.kasiguru.ui.screens.home.HomeViewModel_HiltModules;
import com.kasiguru.ui.screens.profile.ProfileViewModel;
import com.kasiguru.ui.screens.profile.ProfileViewModel_HiltModules;
import com.kasiguru.ui.screens.stories.StoriesViewModel;
import com.kasiguru.ui.screens.stories.StoriesViewModel_HiltModules;
import com.kasiguru.ui.screens.stories.StoryReaderViewModel;
import com.kasiguru.ui.screens.stories.StoryReaderViewModel_HiltModules;
import com.kasiguru.ui.screens.vocabulary.VocabularyViewModel;
import com.kasiguru.ui.screens.vocabulary.VocabularyViewModel_HiltModules;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DelegateFactory;
import dagger.internal.DoubleCheck;
import dagger.internal.IdentifierNameString;
import dagger.internal.KeepFieldType;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DaggerKasiGuruApp_HiltComponents_SingletonC {
  private DaggerKasiGuruApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public KasiGuruApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements KasiGuruApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public KasiGuruApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements KasiGuruApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public KasiGuruApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements KasiGuruApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public KasiGuruApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements KasiGuruApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public KasiGuruApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements KasiGuruApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public KasiGuruApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements KasiGuruApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public KasiGuruApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements KasiGuruApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public KasiGuruApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends KasiGuruApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends KasiGuruApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends KasiGuruApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends KasiGuruApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(ImmutableMap.<String, Boolean>builderWithExpectedSize(16).put(LazyClassKeyProvider.com_kasiguru_ui_screens_achievements_AchievementsViewModel, AchievementsViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_AspectBuilderViewModel, AspectBuilderViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_AudioQuizViewModel, AudioQuizViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_FillBlankViewModel, FillBlankViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_flashcards_FlashcardViewModel, FlashcardViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_GamesViewModel, GamesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_home_HomeViewModel, HomeViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_auth_LoginViewModel, LoginViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_profile_ProfileViewModel, ProfileViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_auth_RegisterViewModel, RegisterViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_SentenceOrderViewModel, SentenceOrderViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_auth_SplashViewModel, SplashViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_stories_StoriesViewModel, StoriesViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_stories_StoryReaderViewModel, StoryReaderViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_vocabulary_VocabularyViewModel, VocabularyViewModel_HiltModules.KeyModule.provide()).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_WordMatchViewModel, WordMatchViewModel_HiltModules.KeyModule.provide()).build());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_kasiguru_ui_screens_auth_SplashViewModel = "com.kasiguru.ui.screens.auth.SplashViewModel";

      static String com_kasiguru_ui_screens_flashcards_FlashcardViewModel = "com.kasiguru.ui.screens.flashcards.FlashcardViewModel";

      static String com_kasiguru_ui_screens_games_AspectBuilderViewModel = "com.kasiguru.ui.screens.games.AspectBuilderViewModel";

      static String com_kasiguru_ui_screens_home_HomeViewModel = "com.kasiguru.ui.screens.home.HomeViewModel";

      static String com_kasiguru_ui_screens_auth_RegisterViewModel = "com.kasiguru.ui.screens.auth.RegisterViewModel";

      static String com_kasiguru_ui_screens_games_AudioQuizViewModel = "com.kasiguru.ui.screens.games.AudioQuizViewModel";

      static String com_kasiguru_ui_screens_profile_ProfileViewModel = "com.kasiguru.ui.screens.profile.ProfileViewModel";

      static String com_kasiguru_ui_screens_games_SentenceOrderViewModel = "com.kasiguru.ui.screens.games.SentenceOrderViewModel";

      static String com_kasiguru_ui_screens_auth_LoginViewModel = "com.kasiguru.ui.screens.auth.LoginViewModel";

      static String com_kasiguru_ui_screens_games_WordMatchViewModel = "com.kasiguru.ui.screens.games.WordMatchViewModel";

      static String com_kasiguru_ui_screens_games_FillBlankViewModel = "com.kasiguru.ui.screens.games.FillBlankViewModel";

      static String com_kasiguru_ui_screens_vocabulary_VocabularyViewModel = "com.kasiguru.ui.screens.vocabulary.VocabularyViewModel";

      static String com_kasiguru_ui_screens_achievements_AchievementsViewModel = "com.kasiguru.ui.screens.achievements.AchievementsViewModel";

      static String com_kasiguru_ui_screens_games_GamesViewModel = "com.kasiguru.ui.screens.games.GamesViewModel";

      static String com_kasiguru_ui_screens_stories_StoryReaderViewModel = "com.kasiguru.ui.screens.stories.StoryReaderViewModel";

      static String com_kasiguru_ui_screens_stories_StoriesViewModel = "com.kasiguru.ui.screens.stories.StoriesViewModel";

      @KeepFieldType
      SplashViewModel com_kasiguru_ui_screens_auth_SplashViewModel2;

      @KeepFieldType
      FlashcardViewModel com_kasiguru_ui_screens_flashcards_FlashcardViewModel2;

      @KeepFieldType
      AspectBuilderViewModel com_kasiguru_ui_screens_games_AspectBuilderViewModel2;

      @KeepFieldType
      HomeViewModel com_kasiguru_ui_screens_home_HomeViewModel2;

      @KeepFieldType
      RegisterViewModel com_kasiguru_ui_screens_auth_RegisterViewModel2;

      @KeepFieldType
      AudioQuizViewModel com_kasiguru_ui_screens_games_AudioQuizViewModel2;

      @KeepFieldType
      ProfileViewModel com_kasiguru_ui_screens_profile_ProfileViewModel2;

      @KeepFieldType
      SentenceOrderViewModel com_kasiguru_ui_screens_games_SentenceOrderViewModel2;

      @KeepFieldType
      LoginViewModel com_kasiguru_ui_screens_auth_LoginViewModel2;

      @KeepFieldType
      WordMatchViewModel com_kasiguru_ui_screens_games_WordMatchViewModel2;

      @KeepFieldType
      FillBlankViewModel com_kasiguru_ui_screens_games_FillBlankViewModel2;

      @KeepFieldType
      VocabularyViewModel com_kasiguru_ui_screens_vocabulary_VocabularyViewModel2;

      @KeepFieldType
      AchievementsViewModel com_kasiguru_ui_screens_achievements_AchievementsViewModel2;

      @KeepFieldType
      GamesViewModel com_kasiguru_ui_screens_games_GamesViewModel2;

      @KeepFieldType
      StoryReaderViewModel com_kasiguru_ui_screens_stories_StoryReaderViewModel2;

      @KeepFieldType
      StoriesViewModel com_kasiguru_ui_screens_stories_StoriesViewModel2;
    }
  }

  private static final class ViewModelCImpl extends KasiGuruApp_HiltComponents.ViewModelC {
    private final SavedStateHandle savedStateHandle;

    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<AchievementsViewModel> achievementsViewModelProvider;

    private Provider<AspectBuilderViewModel> aspectBuilderViewModelProvider;

    private Provider<AudioQuizViewModel> audioQuizViewModelProvider;

    private Provider<FillBlankViewModel> fillBlankViewModelProvider;

    private Provider<FlashcardViewModel> flashcardViewModelProvider;

    private Provider<GamesViewModel> gamesViewModelProvider;

    private Provider<HomeViewModel> homeViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<RegisterViewModel> registerViewModelProvider;

    private Provider<SentenceOrderViewModel> sentenceOrderViewModelProvider;

    private Provider<SplashViewModel> splashViewModelProvider;

    private Provider<StoriesViewModel> storiesViewModelProvider;

    private Provider<StoryReaderViewModel> storyReaderViewModelProvider;

    private Provider<VocabularyViewModel> vocabularyViewModelProvider;

    private Provider<WordMatchViewModel> wordMatchViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.savedStateHandle = savedStateHandleParam;
      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.achievementsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.aspectBuilderViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.audioQuizViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.fillBlankViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.flashcardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.gamesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.homeViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.registerViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.sentenceOrderViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
      this.splashViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 11);
      this.storiesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 12);
      this.storyReaderViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 13);
      this.vocabularyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 14);
      this.wordMatchViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 15);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(16).put(LazyClassKeyProvider.com_kasiguru_ui_screens_achievements_AchievementsViewModel, ((Provider) achievementsViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_AspectBuilderViewModel, ((Provider) aspectBuilderViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_AudioQuizViewModel, ((Provider) audioQuizViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_FillBlankViewModel, ((Provider) fillBlankViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_flashcards_FlashcardViewModel, ((Provider) flashcardViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_GamesViewModel, ((Provider) gamesViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_home_HomeViewModel, ((Provider) homeViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_auth_LoginViewModel, ((Provider) loginViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_profile_ProfileViewModel, ((Provider) profileViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_auth_RegisterViewModel, ((Provider) registerViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_SentenceOrderViewModel, ((Provider) sentenceOrderViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_auth_SplashViewModel, ((Provider) splashViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_stories_StoriesViewModel, ((Provider) storiesViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_stories_StoryReaderViewModel, ((Provider) storyReaderViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_vocabulary_VocabularyViewModel, ((Provider) vocabularyViewModelProvider)).put(LazyClassKeyProvider.com_kasiguru_ui_screens_games_WordMatchViewModel, ((Provider) wordMatchViewModelProvider)).build());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    @IdentifierNameString
    private static final class LazyClassKeyProvider {
      static String com_kasiguru_ui_screens_games_FillBlankViewModel = "com.kasiguru.ui.screens.games.FillBlankViewModel";

      static String com_kasiguru_ui_screens_stories_StoriesViewModel = "com.kasiguru.ui.screens.stories.StoriesViewModel";

      static String com_kasiguru_ui_screens_auth_SplashViewModel = "com.kasiguru.ui.screens.auth.SplashViewModel";

      static String com_kasiguru_ui_screens_games_AudioQuizViewModel = "com.kasiguru.ui.screens.games.AudioQuizViewModel";

      static String com_kasiguru_ui_screens_flashcards_FlashcardViewModel = "com.kasiguru.ui.screens.flashcards.FlashcardViewModel";

      static String com_kasiguru_ui_screens_home_HomeViewModel = "com.kasiguru.ui.screens.home.HomeViewModel";

      static String com_kasiguru_ui_screens_games_WordMatchViewModel = "com.kasiguru.ui.screens.games.WordMatchViewModel";

      static String com_kasiguru_ui_screens_vocabulary_VocabularyViewModel = "com.kasiguru.ui.screens.vocabulary.VocabularyViewModel";

      static String com_kasiguru_ui_screens_games_GamesViewModel = "com.kasiguru.ui.screens.games.GamesViewModel";

      static String com_kasiguru_ui_screens_achievements_AchievementsViewModel = "com.kasiguru.ui.screens.achievements.AchievementsViewModel";

      static String com_kasiguru_ui_screens_profile_ProfileViewModel = "com.kasiguru.ui.screens.profile.ProfileViewModel";

      static String com_kasiguru_ui_screens_auth_RegisterViewModel = "com.kasiguru.ui.screens.auth.RegisterViewModel";

      static String com_kasiguru_ui_screens_games_SentenceOrderViewModel = "com.kasiguru.ui.screens.games.SentenceOrderViewModel";

      static String com_kasiguru_ui_screens_games_AspectBuilderViewModel = "com.kasiguru.ui.screens.games.AspectBuilderViewModel";

      static String com_kasiguru_ui_screens_auth_LoginViewModel = "com.kasiguru.ui.screens.auth.LoginViewModel";

      static String com_kasiguru_ui_screens_stories_StoryReaderViewModel = "com.kasiguru.ui.screens.stories.StoryReaderViewModel";

      @KeepFieldType
      FillBlankViewModel com_kasiguru_ui_screens_games_FillBlankViewModel2;

      @KeepFieldType
      StoriesViewModel com_kasiguru_ui_screens_stories_StoriesViewModel2;

      @KeepFieldType
      SplashViewModel com_kasiguru_ui_screens_auth_SplashViewModel2;

      @KeepFieldType
      AudioQuizViewModel com_kasiguru_ui_screens_games_AudioQuizViewModel2;

      @KeepFieldType
      FlashcardViewModel com_kasiguru_ui_screens_flashcards_FlashcardViewModel2;

      @KeepFieldType
      HomeViewModel com_kasiguru_ui_screens_home_HomeViewModel2;

      @KeepFieldType
      WordMatchViewModel com_kasiguru_ui_screens_games_WordMatchViewModel2;

      @KeepFieldType
      VocabularyViewModel com_kasiguru_ui_screens_vocabulary_VocabularyViewModel2;

      @KeepFieldType
      GamesViewModel com_kasiguru_ui_screens_games_GamesViewModel2;

      @KeepFieldType
      AchievementsViewModel com_kasiguru_ui_screens_achievements_AchievementsViewModel2;

      @KeepFieldType
      ProfileViewModel com_kasiguru_ui_screens_profile_ProfileViewModel2;

      @KeepFieldType
      RegisterViewModel com_kasiguru_ui_screens_auth_RegisterViewModel2;

      @KeepFieldType
      SentenceOrderViewModel com_kasiguru_ui_screens_games_SentenceOrderViewModel2;

      @KeepFieldType
      AspectBuilderViewModel com_kasiguru_ui_screens_games_AspectBuilderViewModel2;

      @KeepFieldType
      LoginViewModel com_kasiguru_ui_screens_auth_LoginViewModel2;

      @KeepFieldType
      StoryReaderViewModel com_kasiguru_ui_screens_stories_StoryReaderViewModel2;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.kasiguru.ui.screens.achievements.AchievementsViewModel 
          return (T) new AchievementsViewModel(singletonCImpl.userProgressRepositoryProvider.get());

          case 1: // com.kasiguru.ui.screens.games.AspectBuilderViewModel 
          return (T) new AspectBuilderViewModel(singletonCImpl.vocabularyRepositoryProvider.get(), singletonCImpl.gameRepositoryProvider.get());

          case 2: // com.kasiguru.ui.screens.games.AudioQuizViewModel 
          return (T) new AudioQuizViewModel(singletonCImpl.vocabularyRepositoryProvider.get(), singletonCImpl.userProgressRepositoryProvider.get(), singletonCImpl.gameRepositoryProvider.get());

          case 3: // com.kasiguru.ui.screens.games.FillBlankViewModel 
          return (T) new FillBlankViewModel(singletonCImpl.vocabularyRepositoryProvider.get(), singletonCImpl.userProgressRepositoryProvider.get(), singletonCImpl.gameRepositoryProvider.get());

          case 4: // com.kasiguru.ui.screens.flashcards.FlashcardViewModel 
          return (T) new FlashcardViewModel(singletonCImpl.vocabularyRepositoryProvider.get(), singletonCImpl.userProgressRepositoryProvider.get());

          case 5: // com.kasiguru.ui.screens.games.GamesViewModel 
          return (T) new GamesViewModel(singletonCImpl.gameRepositoryProvider.get(), singletonCImpl.userProgressRepositoryProvider.get());

          case 6: // com.kasiguru.ui.screens.home.HomeViewModel 
          return (T) new HomeViewModel(singletonCImpl.userProgressRepositoryProvider.get(), singletonCImpl.vocabularyRepositoryProvider.get());

          case 7: // com.kasiguru.ui.screens.auth.LoginViewModel 
          return (T) new LoginViewModel(singletonCImpl.userProgressRepositoryProvider.get());

          case 8: // com.kasiguru.ui.screens.profile.ProfileViewModel 
          return (T) new ProfileViewModel(singletonCImpl.userProgressRepositoryProvider.get());

          case 9: // com.kasiguru.ui.screens.auth.RegisterViewModel 
          return (T) new RegisterViewModel(singletonCImpl.userProgressRepositoryProvider.get());

          case 10: // com.kasiguru.ui.screens.games.SentenceOrderViewModel 
          return (T) new SentenceOrderViewModel(singletonCImpl.gameRepositoryProvider.get());

          case 11: // com.kasiguru.ui.screens.auth.SplashViewModel 
          return (T) new SplashViewModel(singletonCImpl.userProgressRepositoryProvider.get());

          case 12: // com.kasiguru.ui.screens.stories.StoriesViewModel 
          return (T) new StoriesViewModel(singletonCImpl.storyRepositoryProvider.get(), singletonCImpl.userProgressRepositoryProvider.get());

          case 13: // com.kasiguru.ui.screens.stories.StoryReaderViewModel 
          return (T) new StoryReaderViewModel(viewModelCImpl.savedStateHandle, singletonCImpl.storyRepositoryProvider.get(), singletonCImpl.userProgressRepositoryProvider.get());

          case 14: // com.kasiguru.ui.screens.vocabulary.VocabularyViewModel 
          return (T) new VocabularyViewModel(singletonCImpl.vocabularyRepositoryProvider.get());

          case 15: // com.kasiguru.ui.screens.games.WordMatchViewModel 
          return (T) new WordMatchViewModel(singletonCImpl.vocabularyRepositoryProvider.get(), singletonCImpl.userProgressRepositoryProvider.get(), singletonCImpl.gameRepositoryProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends KasiGuruApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends KasiGuruApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends KasiGuruApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<KasiGuruDatabase> provideDatabaseProvider;

    private Provider<VocabularyDao> provideVocabularyDaoProvider;

    private Provider<StoryDao> provideStoryDaoProvider;

    private Provider<UserProgressDao> provideUserProgressDaoProvider;

    private Provider<AchievementDao> provideAchievementDaoProvider;

    private Provider<UserProgressRepository> userProgressRepositoryProvider;

    private Provider<VocabularyRepository> vocabularyRepositoryProvider;

    private Provider<GameScoreDao> provideGameScoreDaoProvider;

    private Provider<GameRepository> gameRepositoryProvider;

    private Provider<StoryRepository> storyRepositoryProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = new DelegateFactory<>();
      this.provideVocabularyDaoProvider = DoubleCheck.provider(new SwitchingProvider<VocabularyDao>(singletonCImpl, 3));
      this.provideStoryDaoProvider = DoubleCheck.provider(new SwitchingProvider<StoryDao>(singletonCImpl, 4));
      this.provideUserProgressDaoProvider = new DelegateFactory<>();
      this.provideAchievementDaoProvider = DoubleCheck.provider(new SwitchingProvider<AchievementDao>(singletonCImpl, 5));
      DelegateFactory.setDelegate(provideDatabaseProvider, DoubleCheck.provider(new SwitchingProvider<KasiGuruDatabase>(singletonCImpl, 2)));
      DelegateFactory.setDelegate(provideUserProgressDaoProvider, DoubleCheck.provider(new SwitchingProvider<UserProgressDao>(singletonCImpl, 1)));
      this.userProgressRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserProgressRepository>(singletonCImpl, 0));
      this.vocabularyRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<VocabularyRepository>(singletonCImpl, 6));
      this.provideGameScoreDaoProvider = DoubleCheck.provider(new SwitchingProvider<GameScoreDao>(singletonCImpl, 8));
      this.gameRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<GameRepository>(singletonCImpl, 7));
      this.storyRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<StoryRepository>(singletonCImpl, 9));
    }

    @Override
    public void injectKasiGuruApp(KasiGuruApp kasiGuruApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.kasiguru.data.repository.UserProgressRepository 
          return (T) new UserProgressRepository(singletonCImpl.provideUserProgressDaoProvider.get(), singletonCImpl.provideAchievementDaoProvider.get());

          case 1: // com.kasiguru.data.local.dao.UserProgressDao 
          return (T) DatabaseModule_ProvideUserProgressDaoFactory.provideUserProgressDao(singletonCImpl.provideDatabaseProvider.get());

          case 2: // com.kasiguru.data.local.KasiGuruDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideVocabularyDaoProvider, singletonCImpl.provideStoryDaoProvider, singletonCImpl.provideUserProgressDaoProvider, singletonCImpl.provideAchievementDaoProvider);

          case 3: // com.kasiguru.data.local.dao.VocabularyDao 
          return (T) DatabaseModule_ProvideVocabularyDaoFactory.provideVocabularyDao(singletonCImpl.provideDatabaseProvider.get());

          case 4: // com.kasiguru.data.local.dao.StoryDao 
          return (T) DatabaseModule_ProvideStoryDaoFactory.provideStoryDao(singletonCImpl.provideDatabaseProvider.get());

          case 5: // com.kasiguru.data.local.dao.AchievementDao 
          return (T) DatabaseModule_ProvideAchievementDaoFactory.provideAchievementDao(singletonCImpl.provideDatabaseProvider.get());

          case 6: // com.kasiguru.data.repository.VocabularyRepository 
          return (T) new VocabularyRepository(singletonCImpl.provideVocabularyDaoProvider.get());

          case 7: // com.kasiguru.data.repository.GameRepository 
          return (T) new GameRepository(singletonCImpl.provideGameScoreDaoProvider.get());

          case 8: // com.kasiguru.data.local.dao.GameScoreDao 
          return (T) DatabaseModule_ProvideGameScoreDaoFactory.provideGameScoreDao(singletonCImpl.provideDatabaseProvider.get());

          case 9: // com.kasiguru.data.repository.StoryRepository 
          return (T) new StoryRepository(singletonCImpl.provideStoryDaoProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
