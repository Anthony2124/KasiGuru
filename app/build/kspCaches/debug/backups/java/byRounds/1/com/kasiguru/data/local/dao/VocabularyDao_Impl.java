package com.kasiguru.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.kasiguru.data.local.entity.VocabularyEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class VocabularyDao_Impl implements VocabularyDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<VocabularyEntity> __insertionAdapterOfVocabularyEntity;

  private final EntityDeletionOrUpdateAdapter<VocabularyEntity> __updateAdapterOfVocabularyEntity;

  private final SharedSQLiteStatement __preparedStmtOfMarkAsLearned;

  private final SharedSQLiteStatement __preparedStmtOfIncrementReviewCount;

  public VocabularyDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfVocabularyEntity = new EntityInsertionAdapter<VocabularyEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `vocabulary` (`id`,`kasiguranin`,`tagalog`,`english`,`rootForm`,`neutralForm`,`imperfectiveForm`,`perfectiveForm`,`contemplativeForm`,`category`,`audioFileName`,`exampleSentence`,`exampleTranslation`,`phoneticGlottal`,`phoneticVowelLength`,`ipaNotation`,`isLearned`,`timesReviewed`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VocabularyEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getKasiguranin());
        statement.bindString(3, entity.getTagalog());
        statement.bindString(4, entity.getEnglish());
        statement.bindString(5, entity.getRootForm());
        statement.bindString(6, entity.getNeutralForm());
        statement.bindString(7, entity.getImperfectiveForm());
        statement.bindString(8, entity.getPerfectiveForm());
        statement.bindString(9, entity.getContemplativeForm());
        statement.bindString(10, entity.getCategory());
        statement.bindString(11, entity.getAudioFileName());
        statement.bindString(12, entity.getExampleSentence());
        statement.bindString(13, entity.getExampleTranslation());
        final int _tmp = entity.getPhoneticGlottal() ? 1 : 0;
        statement.bindLong(14, _tmp);
        final int _tmp_1 = entity.getPhoneticVowelLength() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        statement.bindString(16, entity.getIpaNotation());
        final int _tmp_2 = entity.isLearned() ? 1 : 0;
        statement.bindLong(17, _tmp_2);
        statement.bindLong(18, entity.getTimesReviewed());
      }
    };
    this.__updateAdapterOfVocabularyEntity = new EntityDeletionOrUpdateAdapter<VocabularyEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `vocabulary` SET `id` = ?,`kasiguranin` = ?,`tagalog` = ?,`english` = ?,`rootForm` = ?,`neutralForm` = ?,`imperfectiveForm` = ?,`perfectiveForm` = ?,`contemplativeForm` = ?,`category` = ?,`audioFileName` = ?,`exampleSentence` = ?,`exampleTranslation` = ?,`phoneticGlottal` = ?,`phoneticVowelLength` = ?,`ipaNotation` = ?,`isLearned` = ?,`timesReviewed` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final VocabularyEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getKasiguranin());
        statement.bindString(3, entity.getTagalog());
        statement.bindString(4, entity.getEnglish());
        statement.bindString(5, entity.getRootForm());
        statement.bindString(6, entity.getNeutralForm());
        statement.bindString(7, entity.getImperfectiveForm());
        statement.bindString(8, entity.getPerfectiveForm());
        statement.bindString(9, entity.getContemplativeForm());
        statement.bindString(10, entity.getCategory());
        statement.bindString(11, entity.getAudioFileName());
        statement.bindString(12, entity.getExampleSentence());
        statement.bindString(13, entity.getExampleTranslation());
        final int _tmp = entity.getPhoneticGlottal() ? 1 : 0;
        statement.bindLong(14, _tmp);
        final int _tmp_1 = entity.getPhoneticVowelLength() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        statement.bindString(16, entity.getIpaNotation());
        final int _tmp_2 = entity.isLearned() ? 1 : 0;
        statement.bindLong(17, _tmp_2);
        statement.bindLong(18, entity.getTimesReviewed());
        statement.bindLong(19, entity.getId());
      }
    };
    this.__preparedStmtOfMarkAsLearned = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE vocabulary SET isLearned = 1, timesReviewed = timesReviewed + 1 WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfIncrementReviewCount = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE vocabulary SET timesReviewed = timesReviewed + 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<VocabularyEntity> vocabulary,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVocabularyEntity.insert(vocabulary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insert(final VocabularyEntity vocabulary,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfVocabularyEntity.insert(vocabulary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateVocabulary(final VocabularyEntity vocabulary,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfVocabularyEntity.handle(vocabulary);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markAsLearned(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkAsLearned.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfMarkAsLearned.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object incrementReviewCount(final int id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfIncrementReviewCount.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfIncrementReviewCount.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<VocabularyEntity>> getAllVocabulary() {
    final String _sql = "SELECT * FROM vocabulary ORDER BY category, kasiguranin";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vocabulary"}, new Callable<List<VocabularyEntity>>() {
      @Override
      @NonNull
      public List<VocabularyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKasiguranin = CursorUtil.getColumnIndexOrThrow(_cursor, "kasiguranin");
          final int _cursorIndexOfTagalog = CursorUtil.getColumnIndexOrThrow(_cursor, "tagalog");
          final int _cursorIndexOfEnglish = CursorUtil.getColumnIndexOrThrow(_cursor, "english");
          final int _cursorIndexOfRootForm = CursorUtil.getColumnIndexOrThrow(_cursor, "rootForm");
          final int _cursorIndexOfNeutralForm = CursorUtil.getColumnIndexOrThrow(_cursor, "neutralForm");
          final int _cursorIndexOfImperfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "imperfectiveForm");
          final int _cursorIndexOfPerfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "perfectiveForm");
          final int _cursorIndexOfContemplativeForm = CursorUtil.getColumnIndexOrThrow(_cursor, "contemplativeForm");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudioFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFileName");
          final int _cursorIndexOfExampleSentence = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleSentence");
          final int _cursorIndexOfExampleTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleTranslation");
          final int _cursorIndexOfPhoneticGlottal = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticGlottal");
          final int _cursorIndexOfPhoneticVowelLength = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticVowelLength");
          final int _cursorIndexOfIpaNotation = CursorUtil.getColumnIndexOrThrow(_cursor, "ipaNotation");
          final int _cursorIndexOfIsLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "isLearned");
          final int _cursorIndexOfTimesReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "timesReviewed");
          final List<VocabularyEntity> _result = new ArrayList<VocabularyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VocabularyEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpKasiguranin;
            _tmpKasiguranin = _cursor.getString(_cursorIndexOfKasiguranin);
            final String _tmpTagalog;
            _tmpTagalog = _cursor.getString(_cursorIndexOfTagalog);
            final String _tmpEnglish;
            _tmpEnglish = _cursor.getString(_cursorIndexOfEnglish);
            final String _tmpRootForm;
            _tmpRootForm = _cursor.getString(_cursorIndexOfRootForm);
            final String _tmpNeutralForm;
            _tmpNeutralForm = _cursor.getString(_cursorIndexOfNeutralForm);
            final String _tmpImperfectiveForm;
            _tmpImperfectiveForm = _cursor.getString(_cursorIndexOfImperfectiveForm);
            final String _tmpPerfectiveForm;
            _tmpPerfectiveForm = _cursor.getString(_cursorIndexOfPerfectiveForm);
            final String _tmpContemplativeForm;
            _tmpContemplativeForm = _cursor.getString(_cursorIndexOfContemplativeForm);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudioFileName;
            _tmpAudioFileName = _cursor.getString(_cursorIndexOfAudioFileName);
            final String _tmpExampleSentence;
            _tmpExampleSentence = _cursor.getString(_cursorIndexOfExampleSentence);
            final String _tmpExampleTranslation;
            _tmpExampleTranslation = _cursor.getString(_cursorIndexOfExampleTranslation);
            final boolean _tmpPhoneticGlottal;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPhoneticGlottal);
            _tmpPhoneticGlottal = _tmp != 0;
            final boolean _tmpPhoneticVowelLength;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPhoneticVowelLength);
            _tmpPhoneticVowelLength = _tmp_1 != 0;
            final String _tmpIpaNotation;
            _tmpIpaNotation = _cursor.getString(_cursorIndexOfIpaNotation);
            final boolean _tmpIsLearned;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLearned);
            _tmpIsLearned = _tmp_2 != 0;
            final int _tmpTimesReviewed;
            _tmpTimesReviewed = _cursor.getInt(_cursorIndexOfTimesReviewed);
            _item = new VocabularyEntity(_tmpId,_tmpKasiguranin,_tmpTagalog,_tmpEnglish,_tmpRootForm,_tmpNeutralForm,_tmpImperfectiveForm,_tmpPerfectiveForm,_tmpContemplativeForm,_tmpCategory,_tmpAudioFileName,_tmpExampleSentence,_tmpExampleTranslation,_tmpPhoneticGlottal,_tmpPhoneticVowelLength,_tmpIpaNotation,_tmpIsLearned,_tmpTimesReviewed);
            _result.add(_item);
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
  public Flow<List<VocabularyEntity>> getVocabularyByCategory(final String category) {
    final String _sql = "SELECT * FROM vocabulary WHERE category = ? ORDER BY kasiguranin";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, category);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vocabulary"}, new Callable<List<VocabularyEntity>>() {
      @Override
      @NonNull
      public List<VocabularyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKasiguranin = CursorUtil.getColumnIndexOrThrow(_cursor, "kasiguranin");
          final int _cursorIndexOfTagalog = CursorUtil.getColumnIndexOrThrow(_cursor, "tagalog");
          final int _cursorIndexOfEnglish = CursorUtil.getColumnIndexOrThrow(_cursor, "english");
          final int _cursorIndexOfRootForm = CursorUtil.getColumnIndexOrThrow(_cursor, "rootForm");
          final int _cursorIndexOfNeutralForm = CursorUtil.getColumnIndexOrThrow(_cursor, "neutralForm");
          final int _cursorIndexOfImperfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "imperfectiveForm");
          final int _cursorIndexOfPerfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "perfectiveForm");
          final int _cursorIndexOfContemplativeForm = CursorUtil.getColumnIndexOrThrow(_cursor, "contemplativeForm");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudioFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFileName");
          final int _cursorIndexOfExampleSentence = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleSentence");
          final int _cursorIndexOfExampleTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleTranslation");
          final int _cursorIndexOfPhoneticGlottal = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticGlottal");
          final int _cursorIndexOfPhoneticVowelLength = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticVowelLength");
          final int _cursorIndexOfIpaNotation = CursorUtil.getColumnIndexOrThrow(_cursor, "ipaNotation");
          final int _cursorIndexOfIsLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "isLearned");
          final int _cursorIndexOfTimesReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "timesReviewed");
          final List<VocabularyEntity> _result = new ArrayList<VocabularyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VocabularyEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpKasiguranin;
            _tmpKasiguranin = _cursor.getString(_cursorIndexOfKasiguranin);
            final String _tmpTagalog;
            _tmpTagalog = _cursor.getString(_cursorIndexOfTagalog);
            final String _tmpEnglish;
            _tmpEnglish = _cursor.getString(_cursorIndexOfEnglish);
            final String _tmpRootForm;
            _tmpRootForm = _cursor.getString(_cursorIndexOfRootForm);
            final String _tmpNeutralForm;
            _tmpNeutralForm = _cursor.getString(_cursorIndexOfNeutralForm);
            final String _tmpImperfectiveForm;
            _tmpImperfectiveForm = _cursor.getString(_cursorIndexOfImperfectiveForm);
            final String _tmpPerfectiveForm;
            _tmpPerfectiveForm = _cursor.getString(_cursorIndexOfPerfectiveForm);
            final String _tmpContemplativeForm;
            _tmpContemplativeForm = _cursor.getString(_cursorIndexOfContemplativeForm);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudioFileName;
            _tmpAudioFileName = _cursor.getString(_cursorIndexOfAudioFileName);
            final String _tmpExampleSentence;
            _tmpExampleSentence = _cursor.getString(_cursorIndexOfExampleSentence);
            final String _tmpExampleTranslation;
            _tmpExampleTranslation = _cursor.getString(_cursorIndexOfExampleTranslation);
            final boolean _tmpPhoneticGlottal;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPhoneticGlottal);
            _tmpPhoneticGlottal = _tmp != 0;
            final boolean _tmpPhoneticVowelLength;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPhoneticVowelLength);
            _tmpPhoneticVowelLength = _tmp_1 != 0;
            final String _tmpIpaNotation;
            _tmpIpaNotation = _cursor.getString(_cursorIndexOfIpaNotation);
            final boolean _tmpIsLearned;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLearned);
            _tmpIsLearned = _tmp_2 != 0;
            final int _tmpTimesReviewed;
            _tmpTimesReviewed = _cursor.getInt(_cursorIndexOfTimesReviewed);
            _item = new VocabularyEntity(_tmpId,_tmpKasiguranin,_tmpTagalog,_tmpEnglish,_tmpRootForm,_tmpNeutralForm,_tmpImperfectiveForm,_tmpPerfectiveForm,_tmpContemplativeForm,_tmpCategory,_tmpAudioFileName,_tmpExampleSentence,_tmpExampleTranslation,_tmpPhoneticGlottal,_tmpPhoneticVowelLength,_tmpIpaNotation,_tmpIsLearned,_tmpTimesReviewed);
            _result.add(_item);
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
  public Object getVocabularyById(final int id,
      final Continuation<? super VocabularyEntity> $completion) {
    final String _sql = "SELECT * FROM vocabulary WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<VocabularyEntity>() {
      @Override
      @Nullable
      public VocabularyEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKasiguranin = CursorUtil.getColumnIndexOrThrow(_cursor, "kasiguranin");
          final int _cursorIndexOfTagalog = CursorUtil.getColumnIndexOrThrow(_cursor, "tagalog");
          final int _cursorIndexOfEnglish = CursorUtil.getColumnIndexOrThrow(_cursor, "english");
          final int _cursorIndexOfRootForm = CursorUtil.getColumnIndexOrThrow(_cursor, "rootForm");
          final int _cursorIndexOfNeutralForm = CursorUtil.getColumnIndexOrThrow(_cursor, "neutralForm");
          final int _cursorIndexOfImperfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "imperfectiveForm");
          final int _cursorIndexOfPerfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "perfectiveForm");
          final int _cursorIndexOfContemplativeForm = CursorUtil.getColumnIndexOrThrow(_cursor, "contemplativeForm");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudioFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFileName");
          final int _cursorIndexOfExampleSentence = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleSentence");
          final int _cursorIndexOfExampleTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleTranslation");
          final int _cursorIndexOfPhoneticGlottal = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticGlottal");
          final int _cursorIndexOfPhoneticVowelLength = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticVowelLength");
          final int _cursorIndexOfIpaNotation = CursorUtil.getColumnIndexOrThrow(_cursor, "ipaNotation");
          final int _cursorIndexOfIsLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "isLearned");
          final int _cursorIndexOfTimesReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "timesReviewed");
          final VocabularyEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpKasiguranin;
            _tmpKasiguranin = _cursor.getString(_cursorIndexOfKasiguranin);
            final String _tmpTagalog;
            _tmpTagalog = _cursor.getString(_cursorIndexOfTagalog);
            final String _tmpEnglish;
            _tmpEnglish = _cursor.getString(_cursorIndexOfEnglish);
            final String _tmpRootForm;
            _tmpRootForm = _cursor.getString(_cursorIndexOfRootForm);
            final String _tmpNeutralForm;
            _tmpNeutralForm = _cursor.getString(_cursorIndexOfNeutralForm);
            final String _tmpImperfectiveForm;
            _tmpImperfectiveForm = _cursor.getString(_cursorIndexOfImperfectiveForm);
            final String _tmpPerfectiveForm;
            _tmpPerfectiveForm = _cursor.getString(_cursorIndexOfPerfectiveForm);
            final String _tmpContemplativeForm;
            _tmpContemplativeForm = _cursor.getString(_cursorIndexOfContemplativeForm);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudioFileName;
            _tmpAudioFileName = _cursor.getString(_cursorIndexOfAudioFileName);
            final String _tmpExampleSentence;
            _tmpExampleSentence = _cursor.getString(_cursorIndexOfExampleSentence);
            final String _tmpExampleTranslation;
            _tmpExampleTranslation = _cursor.getString(_cursorIndexOfExampleTranslation);
            final boolean _tmpPhoneticGlottal;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPhoneticGlottal);
            _tmpPhoneticGlottal = _tmp != 0;
            final boolean _tmpPhoneticVowelLength;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPhoneticVowelLength);
            _tmpPhoneticVowelLength = _tmp_1 != 0;
            final String _tmpIpaNotation;
            _tmpIpaNotation = _cursor.getString(_cursorIndexOfIpaNotation);
            final boolean _tmpIsLearned;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLearned);
            _tmpIsLearned = _tmp_2 != 0;
            final int _tmpTimesReviewed;
            _tmpTimesReviewed = _cursor.getInt(_cursorIndexOfTimesReviewed);
            _result = new VocabularyEntity(_tmpId,_tmpKasiguranin,_tmpTagalog,_tmpEnglish,_tmpRootForm,_tmpNeutralForm,_tmpImperfectiveForm,_tmpPerfectiveForm,_tmpContemplativeForm,_tmpCategory,_tmpAudioFileName,_tmpExampleSentence,_tmpExampleTranslation,_tmpPhoneticGlottal,_tmpPhoneticVowelLength,_tmpIpaNotation,_tmpIsLearned,_tmpTimesReviewed);
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

  @Override
  public Flow<List<VocabularyEntity>> getLearnedVocabulary() {
    final String _sql = "SELECT * FROM vocabulary WHERE isLearned = 1 ORDER BY kasiguranin";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vocabulary"}, new Callable<List<VocabularyEntity>>() {
      @Override
      @NonNull
      public List<VocabularyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKasiguranin = CursorUtil.getColumnIndexOrThrow(_cursor, "kasiguranin");
          final int _cursorIndexOfTagalog = CursorUtil.getColumnIndexOrThrow(_cursor, "tagalog");
          final int _cursorIndexOfEnglish = CursorUtil.getColumnIndexOrThrow(_cursor, "english");
          final int _cursorIndexOfRootForm = CursorUtil.getColumnIndexOrThrow(_cursor, "rootForm");
          final int _cursorIndexOfNeutralForm = CursorUtil.getColumnIndexOrThrow(_cursor, "neutralForm");
          final int _cursorIndexOfImperfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "imperfectiveForm");
          final int _cursorIndexOfPerfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "perfectiveForm");
          final int _cursorIndexOfContemplativeForm = CursorUtil.getColumnIndexOrThrow(_cursor, "contemplativeForm");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudioFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFileName");
          final int _cursorIndexOfExampleSentence = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleSentence");
          final int _cursorIndexOfExampleTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleTranslation");
          final int _cursorIndexOfPhoneticGlottal = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticGlottal");
          final int _cursorIndexOfPhoneticVowelLength = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticVowelLength");
          final int _cursorIndexOfIpaNotation = CursorUtil.getColumnIndexOrThrow(_cursor, "ipaNotation");
          final int _cursorIndexOfIsLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "isLearned");
          final int _cursorIndexOfTimesReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "timesReviewed");
          final List<VocabularyEntity> _result = new ArrayList<VocabularyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VocabularyEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpKasiguranin;
            _tmpKasiguranin = _cursor.getString(_cursorIndexOfKasiguranin);
            final String _tmpTagalog;
            _tmpTagalog = _cursor.getString(_cursorIndexOfTagalog);
            final String _tmpEnglish;
            _tmpEnglish = _cursor.getString(_cursorIndexOfEnglish);
            final String _tmpRootForm;
            _tmpRootForm = _cursor.getString(_cursorIndexOfRootForm);
            final String _tmpNeutralForm;
            _tmpNeutralForm = _cursor.getString(_cursorIndexOfNeutralForm);
            final String _tmpImperfectiveForm;
            _tmpImperfectiveForm = _cursor.getString(_cursorIndexOfImperfectiveForm);
            final String _tmpPerfectiveForm;
            _tmpPerfectiveForm = _cursor.getString(_cursorIndexOfPerfectiveForm);
            final String _tmpContemplativeForm;
            _tmpContemplativeForm = _cursor.getString(_cursorIndexOfContemplativeForm);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudioFileName;
            _tmpAudioFileName = _cursor.getString(_cursorIndexOfAudioFileName);
            final String _tmpExampleSentence;
            _tmpExampleSentence = _cursor.getString(_cursorIndexOfExampleSentence);
            final String _tmpExampleTranslation;
            _tmpExampleTranslation = _cursor.getString(_cursorIndexOfExampleTranslation);
            final boolean _tmpPhoneticGlottal;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPhoneticGlottal);
            _tmpPhoneticGlottal = _tmp != 0;
            final boolean _tmpPhoneticVowelLength;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPhoneticVowelLength);
            _tmpPhoneticVowelLength = _tmp_1 != 0;
            final String _tmpIpaNotation;
            _tmpIpaNotation = _cursor.getString(_cursorIndexOfIpaNotation);
            final boolean _tmpIsLearned;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLearned);
            _tmpIsLearned = _tmp_2 != 0;
            final int _tmpTimesReviewed;
            _tmpTimesReviewed = _cursor.getInt(_cursorIndexOfTimesReviewed);
            _item = new VocabularyEntity(_tmpId,_tmpKasiguranin,_tmpTagalog,_tmpEnglish,_tmpRootForm,_tmpNeutralForm,_tmpImperfectiveForm,_tmpPerfectiveForm,_tmpContemplativeForm,_tmpCategory,_tmpAudioFileName,_tmpExampleSentence,_tmpExampleTranslation,_tmpPhoneticGlottal,_tmpPhoneticVowelLength,_tmpIpaNotation,_tmpIsLearned,_tmpTimesReviewed);
            _result.add(_item);
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
  public Object getUnlearnedWords(final int count,
      final Continuation<? super List<VocabularyEntity>> $completion) {
    final String _sql = "SELECT * FROM vocabulary WHERE isLearned = 0 ORDER BY RANDOM() LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, count);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VocabularyEntity>>() {
      @Override
      @NonNull
      public List<VocabularyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKasiguranin = CursorUtil.getColumnIndexOrThrow(_cursor, "kasiguranin");
          final int _cursorIndexOfTagalog = CursorUtil.getColumnIndexOrThrow(_cursor, "tagalog");
          final int _cursorIndexOfEnglish = CursorUtil.getColumnIndexOrThrow(_cursor, "english");
          final int _cursorIndexOfRootForm = CursorUtil.getColumnIndexOrThrow(_cursor, "rootForm");
          final int _cursorIndexOfNeutralForm = CursorUtil.getColumnIndexOrThrow(_cursor, "neutralForm");
          final int _cursorIndexOfImperfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "imperfectiveForm");
          final int _cursorIndexOfPerfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "perfectiveForm");
          final int _cursorIndexOfContemplativeForm = CursorUtil.getColumnIndexOrThrow(_cursor, "contemplativeForm");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudioFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFileName");
          final int _cursorIndexOfExampleSentence = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleSentence");
          final int _cursorIndexOfExampleTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleTranslation");
          final int _cursorIndexOfPhoneticGlottal = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticGlottal");
          final int _cursorIndexOfPhoneticVowelLength = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticVowelLength");
          final int _cursorIndexOfIpaNotation = CursorUtil.getColumnIndexOrThrow(_cursor, "ipaNotation");
          final int _cursorIndexOfIsLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "isLearned");
          final int _cursorIndexOfTimesReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "timesReviewed");
          final List<VocabularyEntity> _result = new ArrayList<VocabularyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VocabularyEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpKasiguranin;
            _tmpKasiguranin = _cursor.getString(_cursorIndexOfKasiguranin);
            final String _tmpTagalog;
            _tmpTagalog = _cursor.getString(_cursorIndexOfTagalog);
            final String _tmpEnglish;
            _tmpEnglish = _cursor.getString(_cursorIndexOfEnglish);
            final String _tmpRootForm;
            _tmpRootForm = _cursor.getString(_cursorIndexOfRootForm);
            final String _tmpNeutralForm;
            _tmpNeutralForm = _cursor.getString(_cursorIndexOfNeutralForm);
            final String _tmpImperfectiveForm;
            _tmpImperfectiveForm = _cursor.getString(_cursorIndexOfImperfectiveForm);
            final String _tmpPerfectiveForm;
            _tmpPerfectiveForm = _cursor.getString(_cursorIndexOfPerfectiveForm);
            final String _tmpContemplativeForm;
            _tmpContemplativeForm = _cursor.getString(_cursorIndexOfContemplativeForm);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudioFileName;
            _tmpAudioFileName = _cursor.getString(_cursorIndexOfAudioFileName);
            final String _tmpExampleSentence;
            _tmpExampleSentence = _cursor.getString(_cursorIndexOfExampleSentence);
            final String _tmpExampleTranslation;
            _tmpExampleTranslation = _cursor.getString(_cursorIndexOfExampleTranslation);
            final boolean _tmpPhoneticGlottal;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPhoneticGlottal);
            _tmpPhoneticGlottal = _tmp != 0;
            final boolean _tmpPhoneticVowelLength;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPhoneticVowelLength);
            _tmpPhoneticVowelLength = _tmp_1 != 0;
            final String _tmpIpaNotation;
            _tmpIpaNotation = _cursor.getString(_cursorIndexOfIpaNotation);
            final boolean _tmpIsLearned;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLearned);
            _tmpIsLearned = _tmp_2 != 0;
            final int _tmpTimesReviewed;
            _tmpTimesReviewed = _cursor.getInt(_cursorIndexOfTimesReviewed);
            _item = new VocabularyEntity(_tmpId,_tmpKasiguranin,_tmpTagalog,_tmpEnglish,_tmpRootForm,_tmpNeutralForm,_tmpImperfectiveForm,_tmpPerfectiveForm,_tmpContemplativeForm,_tmpCategory,_tmpAudioFileName,_tmpExampleSentence,_tmpExampleTranslation,_tmpPhoneticGlottal,_tmpPhoneticVowelLength,_tmpIpaNotation,_tmpIsLearned,_tmpTimesReviewed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRandomWords(final int count,
      final Continuation<? super List<VocabularyEntity>> $completion) {
    final String _sql = "SELECT * FROM vocabulary ORDER BY RANDOM() LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, count);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<VocabularyEntity>>() {
      @Override
      @NonNull
      public List<VocabularyEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfKasiguranin = CursorUtil.getColumnIndexOrThrow(_cursor, "kasiguranin");
          final int _cursorIndexOfTagalog = CursorUtil.getColumnIndexOrThrow(_cursor, "tagalog");
          final int _cursorIndexOfEnglish = CursorUtil.getColumnIndexOrThrow(_cursor, "english");
          final int _cursorIndexOfRootForm = CursorUtil.getColumnIndexOrThrow(_cursor, "rootForm");
          final int _cursorIndexOfNeutralForm = CursorUtil.getColumnIndexOrThrow(_cursor, "neutralForm");
          final int _cursorIndexOfImperfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "imperfectiveForm");
          final int _cursorIndexOfPerfectiveForm = CursorUtil.getColumnIndexOrThrow(_cursor, "perfectiveForm");
          final int _cursorIndexOfContemplativeForm = CursorUtil.getColumnIndexOrThrow(_cursor, "contemplativeForm");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfAudioFileName = CursorUtil.getColumnIndexOrThrow(_cursor, "audioFileName");
          final int _cursorIndexOfExampleSentence = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleSentence");
          final int _cursorIndexOfExampleTranslation = CursorUtil.getColumnIndexOrThrow(_cursor, "exampleTranslation");
          final int _cursorIndexOfPhoneticGlottal = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticGlottal");
          final int _cursorIndexOfPhoneticVowelLength = CursorUtil.getColumnIndexOrThrow(_cursor, "phoneticVowelLength");
          final int _cursorIndexOfIpaNotation = CursorUtil.getColumnIndexOrThrow(_cursor, "ipaNotation");
          final int _cursorIndexOfIsLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "isLearned");
          final int _cursorIndexOfTimesReviewed = CursorUtil.getColumnIndexOrThrow(_cursor, "timesReviewed");
          final List<VocabularyEntity> _result = new ArrayList<VocabularyEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final VocabularyEntity _item;
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final String _tmpKasiguranin;
            _tmpKasiguranin = _cursor.getString(_cursorIndexOfKasiguranin);
            final String _tmpTagalog;
            _tmpTagalog = _cursor.getString(_cursorIndexOfTagalog);
            final String _tmpEnglish;
            _tmpEnglish = _cursor.getString(_cursorIndexOfEnglish);
            final String _tmpRootForm;
            _tmpRootForm = _cursor.getString(_cursorIndexOfRootForm);
            final String _tmpNeutralForm;
            _tmpNeutralForm = _cursor.getString(_cursorIndexOfNeutralForm);
            final String _tmpImperfectiveForm;
            _tmpImperfectiveForm = _cursor.getString(_cursorIndexOfImperfectiveForm);
            final String _tmpPerfectiveForm;
            _tmpPerfectiveForm = _cursor.getString(_cursorIndexOfPerfectiveForm);
            final String _tmpContemplativeForm;
            _tmpContemplativeForm = _cursor.getString(_cursorIndexOfContemplativeForm);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final String _tmpAudioFileName;
            _tmpAudioFileName = _cursor.getString(_cursorIndexOfAudioFileName);
            final String _tmpExampleSentence;
            _tmpExampleSentence = _cursor.getString(_cursorIndexOfExampleSentence);
            final String _tmpExampleTranslation;
            _tmpExampleTranslation = _cursor.getString(_cursorIndexOfExampleTranslation);
            final boolean _tmpPhoneticGlottal;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfPhoneticGlottal);
            _tmpPhoneticGlottal = _tmp != 0;
            final boolean _tmpPhoneticVowelLength;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfPhoneticVowelLength);
            _tmpPhoneticVowelLength = _tmp_1 != 0;
            final String _tmpIpaNotation;
            _tmpIpaNotation = _cursor.getString(_cursorIndexOfIpaNotation);
            final boolean _tmpIsLearned;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsLearned);
            _tmpIsLearned = _tmp_2 != 0;
            final int _tmpTimesReviewed;
            _tmpTimesReviewed = _cursor.getInt(_cursorIndexOfTimesReviewed);
            _item = new VocabularyEntity(_tmpId,_tmpKasiguranin,_tmpTagalog,_tmpEnglish,_tmpRootForm,_tmpNeutralForm,_tmpImperfectiveForm,_tmpPerfectiveForm,_tmpContemplativeForm,_tmpCategory,_tmpAudioFileName,_tmpExampleSentence,_tmpExampleTranslation,_tmpPhoneticGlottal,_tmpPhoneticVowelLength,_tmpIpaNotation,_tmpIsLearned,_tmpTimesReviewed);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> getLearnedCount() {
    final String _sql = "SELECT COUNT(*) FROM vocabulary WHERE isLearned = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vocabulary"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Flow<Integer> getTotalCount() {
    final String _sql = "SELECT COUNT(*) FROM vocabulary";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vocabulary"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object getTotalCountDirect(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM vocabulary";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<String>> getCategories() {
    final String _sql = "SELECT DISTINCT category FROM vocabulary ORDER BY category";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"vocabulary"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
