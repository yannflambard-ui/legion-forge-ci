package com.legionforge.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.legionforge.app.data.model.ArmyList;
import com.legionforge.app.data.model.ArmyUnit;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
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
public final class ArmyListDao_Impl implements ArmyListDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ArmyList> __insertionAdapterOfArmyList;

  private final EntityInsertionAdapter<ArmyUnit> __insertionAdapterOfArmyUnit;

  private final EntityDeletionOrUpdateAdapter<ArmyUnit> __deletionAdapterOfArmyUnit;

  private final EntityDeletionOrUpdateAdapter<ArmyList> __deletionAdapterOfArmyList;

  private final EntityDeletionOrUpdateAdapter<ArmyUnit> __updateAdapterOfArmyUnit;

  public ArmyListDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfArmyList = new EntityInsertionAdapter<ArmyList>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `army_lists` (`id`,`name`,`factionId`,`battleForceId`,`pointsLimit`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArmyList entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getFactionId());
        if (entity.getBattleForceId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getBattleForceId());
        }
        statement.bindLong(5, entity.getPointsLimit());
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getUpdatedAt());
      }
    };
    this.__insertionAdapterOfArmyUnit = new EntityInsertionAdapter<ArmyUnit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `army_units` (`id`,`armyListId`,`unitId`,`upgradeIds`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArmyUnit entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getArmyListId());
        statement.bindString(3, entity.getUnitId());
        statement.bindString(4, entity.getUpgradeIds());
      }
    };
    this.__deletionAdapterOfArmyUnit = new EntityDeletionOrUpdateAdapter<ArmyUnit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `army_units` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArmyUnit entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfArmyList = new EntityDeletionOrUpdateAdapter<ArmyList>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `army_lists` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArmyList entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfArmyUnit = new EntityDeletionOrUpdateAdapter<ArmyUnit>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `army_units` SET `id` = ?,`armyListId` = ?,`unitId` = ?,`upgradeIds` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ArmyUnit entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getArmyListId());
        statement.bindString(3, entity.getUnitId());
        statement.bindString(4, entity.getUpgradeIds());
        statement.bindLong(5, entity.getId());
      }
    };
  }

  @Override
  public Object upsertList(final ArmyList list, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfArmyList.insert(list);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object addUnitToList(final ArmyUnit unit, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfArmyUnit.insertAndReturnId(unit);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object removeUnitFromList(final ArmyUnit unit,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfArmyUnit.handle(unit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteList(final ArmyList list, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfArmyList.handle(list);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateUnitInList(final ArmyUnit unit,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfArmyUnit.handle(unit);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ArmyList>> getAllLists() {
    final String _sql = "SELECT * FROM army_lists ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"army_lists"}, new Callable<List<ArmyList>>() {
      @Override
      @NonNull
      public List<ArmyList> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "factionId");
          final int _cursorIndexOfBattleForceId = CursorUtil.getColumnIndexOrThrow(_cursor, "battleForceId");
          final int _cursorIndexOfPointsLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsLimit");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ArmyList> _result = new ArrayList<ArmyList>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ArmyList _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFactionId;
            _tmpFactionId = _cursor.getString(_cursorIndexOfFactionId);
            final String _tmpBattleForceId;
            if (_cursor.isNull(_cursorIndexOfBattleForceId)) {
              _tmpBattleForceId = null;
            } else {
              _tmpBattleForceId = _cursor.getString(_cursorIndexOfBattleForceId);
            }
            final int _tmpPointsLimit;
            _tmpPointsLimit = _cursor.getInt(_cursorIndexOfPointsLimit);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new ArmyList(_tmpId,_tmpName,_tmpFactionId,_tmpBattleForceId,_tmpPointsLimit,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getList(final String id, final Continuation<? super ArmyList> $completion) {
    final String _sql = "SELECT * FROM army_lists WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ArmyList>() {
      @Override
      @Nullable
      public ArmyList call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfFactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "factionId");
          final int _cursorIndexOfBattleForceId = CursorUtil.getColumnIndexOrThrow(_cursor, "battleForceId");
          final int _cursorIndexOfPointsLimit = CursorUtil.getColumnIndexOrThrow(_cursor, "pointsLimit");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final ArmyList _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpFactionId;
            _tmpFactionId = _cursor.getString(_cursorIndexOfFactionId);
            final String _tmpBattleForceId;
            if (_cursor.isNull(_cursorIndexOfBattleForceId)) {
              _tmpBattleForceId = null;
            } else {
              _tmpBattleForceId = _cursor.getString(_cursorIndexOfBattleForceId);
            }
            final int _tmpPointsLimit;
            _tmpPointsLimit = _cursor.getInt(_cursorIndexOfPointsLimit);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new ArmyList(_tmpId,_tmpName,_tmpFactionId,_tmpBattleForceId,_tmpPointsLimit,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<ArmyUnit>> getUnitsInList(final String armyListId) {
    final String _sql = "SELECT * FROM army_units WHERE armyListId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, armyListId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"army_units"}, new Callable<List<ArmyUnit>>() {
      @Override
      @NonNull
      public List<ArmyUnit> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfArmyListId = CursorUtil.getColumnIndexOrThrow(_cursor, "armyListId");
          final int _cursorIndexOfUnitId = CursorUtil.getColumnIndexOrThrow(_cursor, "unitId");
          final int _cursorIndexOfUpgradeIds = CursorUtil.getColumnIndexOrThrow(_cursor, "upgradeIds");
          final List<ArmyUnit> _result = new ArrayList<ArmyUnit>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ArmyUnit _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpArmyListId;
            _tmpArmyListId = _cursor.getString(_cursorIndexOfArmyListId);
            final String _tmpUnitId;
            _tmpUnitId = _cursor.getString(_cursorIndexOfUnitId);
            final String _tmpUpgradeIds;
            _tmpUpgradeIds = _cursor.getString(_cursorIndexOfUpgradeIds);
            _item = new ArmyUnit(_tmpId,_tmpArmyListId,_tmpUnitId,_tmpUpgradeIds);
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
