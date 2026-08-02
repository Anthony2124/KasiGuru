package com.kasiguru.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.kasiguru.data.local.entity.UserProgressEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class UserProgressDao_Impl implements UserProgressDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<UserProgressEntity> __insertionAdapterOfUserProgressEntity;

  private final SharedSQLiteStatement __preparedStmtOfAddXp;

  private final SharedSQLiteStatement __preparedStmtOfIncrementWordsLearned;

  private final SharedSQLiteStatement __preparedStmtOfIncrementStoriesCompleted;

  private final SharedSQLiteStatement __preparedStmtOfIncrementGamesPlayed;

  private final SharedSQLiteStatement __preparedStmtOfUpdateGameStats;

  private final SharedSQLiteStatement __preparedStmtOfUpdateStreak;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLevel;

  private final SharedSQLiteStatement __preparedStmtOfUpdateUserName;

  public UserProgressDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfUserProgressEntity = new EntityInsertionAdapter<UserProgressEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `user_progress` (`id`,`userName`,`totalXp`,`level`,`currentStreak`,`longestStreak`,`lastActiveDate`,`wordsLearned`,`storiesCompleted`,`gamesPlayed`,`totalCorrectAnswers`,`totalQuestionsAnswered`,`lessonsCompleted`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UserProgressEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUserName());
        statement.bindLong(3, entity.getTotalXp());
        statement.bindLong(4, entity.getLevel());
        statement.bindLong(5, entity.getCurrentStreak());
        statement.bindLong(6, entity.getLongestStreak());
        statement.bindString(7, entity.getLastActiveDate());
        statement.bindLong(8, entity.getWordsLearned());
        statement.bindLong(9, entity.getStoriesCompleted());
        statement.bindLong(10, entity.getGamesPlayed());
        statement.bindLong(11, entity.getTotalCorrectAnswers());
        statement.bindLong(12, entity.getTotalQuestionsAnswered());
        statement.bindLong(13, entity.getLessonsCompleted());
      }
    };
    this.__preparedStmtOfAddXp = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET totalXp = totalXp + ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementWordsLearned = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET wordsLearned = wordsLearned + 1 WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementStoriesCompleted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET storiesCompleted = storiesCompleted + 1 WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementGamesPlayed = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET gamesPlayed = gamesPlayed + 1 WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateGameStats = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET totalCorrectAnswers = totalCorrectAnswers + ?, totalQuestionsAnswered = totalQuestionsAnswered + ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateStreak = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET currentStreak = ?, longestStreak = CASE WHEN ? > longestStreak THEN ? ELSE longestStreak END, lastActiveDate = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLevel = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET level = ? WHERE id = 1";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateUserName = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE user_progress SET userName = ? WHERE id = 1";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final UserProgressEntity progress,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUserProgressEntity.insert(progress);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object addXp(final int xp, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfAddXp.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, xp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfAddXp.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementWordsLearned(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementWordsLearned.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementWordsLearned.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementStoriesCompleted(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementStoriesCompleted.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementStoriesCompleted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementGamesPlayed(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementGamesPlayed.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfIncrementGamesPlayed.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateGameStats(final int correct, final int total,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateGameStats.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, correct);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, total);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateGameStats.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateStreak(final int streak, final String date,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateStreak.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, streak);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, streak);
        _argIndex = 3;
        _stmt.bindLong(_argIndex, streak);
        _argIndex = 4;
        _stmt.bindString(_argIndex, date);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateStreak.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLevel(final int level, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLevel.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, level);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateLevel.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateUserName(final String name, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateUserName.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, name);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateUserName.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<UserProgressEntity> getUserProgress() {
    final String _sql = "SELECT * FROM user_progress WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"user_progress"}, new Callable<UserProgressEntity>() {
      @Override
      @Nullable
      public UserProgressEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfTotalXp = CursorUtil.getColumnIndexOrThrow(_cursor, "totalXp");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLongestStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "longestStreak");
          final int _cursorIndexOfLastActiveDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActiveDate");
          final int _cursorIndexOfWordsLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "wordsLearned");
          final int _cursorIndexOfStoriesCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "storiesCompleted");
          final int _cursorIndexOfGamesPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "gamesPlayed");
          final int _cursorIndexOfTotalCorrectAnswers = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCorrectAnswers");
          final int _cursorIndexOfTotalQuestionsAnswered = CursorUtil.getColumnIndexOrThrow(_cursor, "totalQuestionsAnswered");
          final int _cursorIndexOfLessonsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "lessonsCompleted");
          final UserProgressEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final int _tmpTotalXp;
            _tmpTotalXp = _cursor.getInt(_cursorIndexOfTotalXp);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final int _tmpLongestStreak;
            _tmpLongestStreak = _cursor.getInt(_cursorIndexOfLongestStreak);
            final String _tmpLastActiveDate;
            _tmpLastActiveDate = _cursor.getString(_cursorIndexOfLastActiveDate);
            final int _tmpWordsLearned;
            _tmpWordsLearned = _cursor.getInt(_cursorIndexOfWordsLearned);
            final int _tmpStoriesCompleted;
            _tmpStoriesCompleted = _cursor.getInt(_cursorIndexOfStoriesCompleted);
            final int _tmpGamesPlayed;
            _tmpGamesPlayed = _cursor.getInt(_cursorIndexOfGamesPlayed);
            final int _tmpTotalCorrectAnswers;
            _tmpTotalCorrectAnswers = _cursor.getInt(_cursorIndexOfTotalCorrectAnswers);
            final int _tmpTotalQuestionsAnswered;
            _tmpTotalQuestionsAnswered = _cursor.getInt(_cursorIndexOfTotalQuestionsAnswered);
            final int _tmpLessonsCompleted;
            _tmpLessonsCompleted = _cursor.getInt(_cursorIndexOfLessonsCompleted);
            _result = new UserProgressEntity(_tmpId,_tmpUserName,_tmpTotalXp,_tmpLevel,_tmpCurrentStreak,_tmpLongestStreak,_tmpLastActiveDate,_tmpWordsLearned,_tmpStoriesCompleted,_tmpGamesPlayed,_tmpTotalCorrectAnswers,_tmpTotalQuestionsAnswered,_tmpLessonsCompleted);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getUserProgressOnce(final Continuation<? super UserProgressEntity> $completion) {
    final String _sql = "SELECT * FROM user_progress WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UserProgressEntity>() {
      @Override
      @Nullable
      public UserProgressEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfTotalXp = CursorUtil.getColumnIndexOrThrow(_cursor, "totalXp");
          final int _cursorIndexOfLevel = CursorUtil.getColumnIndexOrThrow(_cursor, "level");
          final int _cursorIndexOfCurrentStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "currentStreak");
          final int _cursorIndexOfLongestStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "longestStreak");
          final int _cursorIndexOfLastActiveDate = CursorUtil.getColumnIndexOrThrow(_cursor, "lastActiveDate");
          final int _cursorIndexOfWordsLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "wordsLearned");
          final int _cursorIndexOfStoriesCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "storiesCompleted");
          final int _cursorIndexOfGamesPlayed = CursorUtil.getColumnIndexOrThrow(_cursor, "gamesPlayed");
          final int _cursorIndexOfTotalCorrectAnswers = CursorUtil.getColumnIndexOrThrow(_cursor, "totalCorrectAnswers");
          final int _cursorIndexOfTotalQuestionsAnswered = CursorUtil.getColumnIndexOrThrow(_cursor, "totalQuestionsAnswered");
          final int _cursorIndexOfLessonsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "lessonsCompleted");
          final UserProgressEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final int _tmpTotalXp;
            _tmpTotalXp = _cursor.getInt(_cursorIndexOfTotalXp);
            final int _tmpLevel;
            _tmpLevel = _cursor.getInt(_cursorIndexOfLevel);
            final int _tmpCurrentStreak;
            _tmpCurrentStreak = _cursor.getInt(_cursorIndexOfCurrentStreak);
            final int _tmpLongestStreak;
            _tmpLongestStreak = _cursor.getInt(_cursorIndexOfLongestStreak);
            final String _tmpLastActiveDate;
            _tmpLastActiveDate = _cursor.getString(_cursorIndexOfLastActiveDate);
            final int _tmpWordsLearned;
            _tmpWordsLearned = _cursor.getInt(_cursorIndexOfWordsLearned);
            final int _tmpStoriesCompleted;
            _tmpStoriesCompleted = _cursor.getInt(_cursorIndexOfStoriesCompleted);
            final int _tmpGamesPlayed;
            _tmpGamesPlayed = _cursor.getInt(_cursorIndexOfGamesPlayed);
            final int _tmpTotalCorrectAnswers;
            _tmpTotalCorrectAnswers = _cursor.getInt(_cursorIndexOfTotalCorrectAnswers);
            final int _tmpTotalQuestionsAnswered;
            _tmpTotalQuestionsAnswered = _cursor.getInt(_cursorIndexOfTotalQuestionsAnswered);
            final int _tmpLessonsCompleted;
            _tmpLessonsCompleted = _cursor.getInt(_cursorIndexOfLessonsCompleted);
            _result = new UserProgressEntity(_tmpId,_tmpUserName,_tmpTotalXp,_tmpLevel,_tmpCurrentStreak,_tmpLongestStreak,_tmpLastActiveDate,_tmpWordsLearned,_tmpStoriesCompleted,_tmpGamesPlayed,_tmpTotalCorrectAnswers,_tmpTotalQuestionsAnswered,_tmpLessonsCompleted);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
