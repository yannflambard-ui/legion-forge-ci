package com.legionforge.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.legionforge.app.data.model.Faction;
import com.legionforge.app.data.model.Keyword;
import com.legionforge.app.data.model.UnitEntity;
import com.legionforge.app.data.model.UnitKeywordCrossRef;
import com.legionforge.app.data.model.UnitRank;
import com.legionforge.app.data.model.UpgradeSlot;
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
public final class GameDataDao_Impl implements GameDataDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Faction> __insertionAdapterOfFaction;

  private final EntityInsertionAdapter<UnitEntity> __insertionAdapterOfUnitEntity;

  private final Converters __converters = new Converters();

  private final EntityInsertionAdapter<Keyword> __insertionAdapterOfKeyword;

  private final EntityInsertionAdapter<UpgradeSlot> __insertionAdapterOfUpgradeSlot;

  private final EntityInsertionAdapter<UnitKeywordCrossRef> __insertionAdapterOfUnitKeywordCrossRef;

  public GameDataDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfFaction = new EntityInsertionAdapter<Faction>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `factions` (`id`,`name`,`description`,`imageUrl`,`expansionId`) VALUES (?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Faction entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDescription());
        if (entity.getImageUrl() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getImageUrl());
        }
        if (entity.getExpansionId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getExpansionId());
        }
      }
    };
    this.__insertionAdapterOfUnitEntity = new EntityInsertionAdapter<UnitEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `units` (`id`,`factionId`,`name`,`points`,`rank`,`minInArmy`,`maxInArmy`,`imageUrl`,`expansionId`,`unique`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UnitEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getFactionId());
        statement.bindString(3, entity.getName());
        statement.bindLong(4, entity.getPoints());
        final String _tmp = __converters.fromRank(entity.getRank());
        statement.bindString(5, _tmp);
        statement.bindLong(6, entity.getMinInArmy());
        statement.bindLong(7, entity.getMaxInArmy());
        if (entity.getImageUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getImageUrl());
        }
        if (entity.getExpansionId() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getExpansionId());
        }
        final int _tmp_1 = entity.getUnique() ? 1 : 0;
        statement.bindLong(10, _tmp_1);
      }
    };
    this.__insertionAdapterOfKeyword = new EntityInsertionAdapter<Keyword>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `keywords` (`id`,`name`,`description`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Keyword entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDescription());
      }
    };
    this.__insertionAdapterOfUpgradeSlot = new EntityInsertionAdapter<UpgradeSlot>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `upgrade_slots` (`id`,`unitId`,`slotType`) VALUES (nullif(?, 0),?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UpgradeSlot entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUnitId());
        statement.bindString(3, entity.getSlotType());
      }
    };
    this.__insertionAdapterOfUnitKeywordCrossRef = new EntityInsertionAdapter<UnitKeywordCrossRef>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `unit_keywords` (`unitId`,`keywordId`) VALUES (?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final UnitKeywordCrossRef entity) {
        statement.bindString(1, entity.getUnitId());
        statement.bindString(2, entity.getKeywordId());
      }
    };
  }

  @Override
  public Object insertFactions(final List<Faction> factions,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfFaction.insert(factions);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertUnits(final List<UnitEntity> units,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUnitEntity.insert(units);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertKeywords(final List<Keyword> keywords,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfKeyword.insert(keywords);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertUpgradeSlots(final List<UpgradeSlot> slots,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUpgradeSlot.insert(slots);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertUnitKeywords(final List<UnitKeywordCrossRef> crossRefs,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfUnitKeywordCrossRef.insert(crossRefs);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Faction>> getAllFactions() {
    final String _sql = "SELECT * FROM factions";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"factions"}, new Callable<List<Faction>>() {
      @Override
      @NonNull
      public List<Faction> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfExpansionId = CursorUtil.getColumnIndexOrThrow(_cursor, "expansionId");
          final List<Faction> _result = new ArrayList<Faction>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Faction _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpExpansionId;
            if (_cursor.isNull(_cursorIndexOfExpansionId)) {
              _tmpExpansionId = null;
            } else {
              _tmpExpansionId = _cursor.getString(_cursorIndexOfExpansionId);
            }
            _item = new Faction(_tmpId,_tmpName,_tmpDescription,_tmpImageUrl,_tmpExpansionId);
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
  public Flow<List<UnitEntity>> getUnitsForFaction(final String factionId) {
    final String _sql = "SELECT * FROM units WHERE factionId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, factionId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"units"}, new Callable<List<UnitEntity>>() {
      @Override
      @NonNull
      public List<UnitEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "factionId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfRank = CursorUtil.getColumnIndexOrThrow(_cursor, "rank");
          final int _cursorIndexOfMinInArmy = CursorUtil.getColumnIndexOrThrow(_cursor, "minInArmy");
          final int _cursorIndexOfMaxInArmy = CursorUtil.getColumnIndexOrThrow(_cursor, "maxInArmy");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfExpansionId = CursorUtil.getColumnIndexOrThrow(_cursor, "expansionId");
          final int _cursorIndexOfUnique = CursorUtil.getColumnIndexOrThrow(_cursor, "unique");
          final List<UnitEntity> _result = new ArrayList<UnitEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UnitEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpFactionId;
            _tmpFactionId = _cursor.getString(_cursorIndexOfFactionId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            final UnitRank _tmpRank;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfRank);
            _tmpRank = __converters.toRank(_tmp);
            final int _tmpMinInArmy;
            _tmpMinInArmy = _cursor.getInt(_cursorIndexOfMinInArmy);
            final int _tmpMaxInArmy;
            _tmpMaxInArmy = _cursor.getInt(_cursorIndexOfMaxInArmy);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpExpansionId;
            if (_cursor.isNull(_cursorIndexOfExpansionId)) {
              _tmpExpansionId = null;
            } else {
              _tmpExpansionId = _cursor.getString(_cursorIndexOfExpansionId);
            }
            final boolean _tmpUnique;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfUnique);
            _tmpUnique = _tmp_1 != 0;
            _item = new UnitEntity(_tmpId,_tmpFactionId,_tmpName,_tmpPoints,_tmpRank,_tmpMinInArmy,_tmpMaxInArmy,_tmpImageUrl,_tmpExpansionId,_tmpUnique);
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
  public Object getUnit(final String unitId, final Continuation<? super UnitEntity> $completion) {
    final String _sql = "SELECT * FROM units WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, unitId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<UnitEntity>() {
      @Override
      @Nullable
      public UnitEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfFactionId = CursorUtil.getColumnIndexOrThrow(_cursor, "factionId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPoints = CursorUtil.getColumnIndexOrThrow(_cursor, "points");
          final int _cursorIndexOfRank = CursorUtil.getColumnIndexOrThrow(_cursor, "rank");
          final int _cursorIndexOfMinInArmy = CursorUtil.getColumnIndexOrThrow(_cursor, "minInArmy");
          final int _cursorIndexOfMaxInArmy = CursorUtil.getColumnIndexOrThrow(_cursor, "maxInArmy");
          final int _cursorIndexOfImageUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "imageUrl");
          final int _cursorIndexOfExpansionId = CursorUtil.getColumnIndexOrThrow(_cursor, "expansionId");
          final int _cursorIndexOfUnique = CursorUtil.getColumnIndexOrThrow(_cursor, "unique");
          final UnitEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpFactionId;
            _tmpFactionId = _cursor.getString(_cursorIndexOfFactionId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final int _tmpPoints;
            _tmpPoints = _cursor.getInt(_cursorIndexOfPoints);
            final UnitRank _tmpRank;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfRank);
            _tmpRank = __converters.toRank(_tmp);
            final int _tmpMinInArmy;
            _tmpMinInArmy = _cursor.getInt(_cursorIndexOfMinInArmy);
            final int _tmpMaxInArmy;
            _tmpMaxInArmy = _cursor.getInt(_cursorIndexOfMaxInArmy);
            final String _tmpImageUrl;
            if (_cursor.isNull(_cursorIndexOfImageUrl)) {
              _tmpImageUrl = null;
            } else {
              _tmpImageUrl = _cursor.getString(_cursorIndexOfImageUrl);
            }
            final String _tmpExpansionId;
            if (_cursor.isNull(_cursorIndexOfExpansionId)) {
              _tmpExpansionId = null;
            } else {
              _tmpExpansionId = _cursor.getString(_cursorIndexOfExpansionId);
            }
            final boolean _tmpUnique;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfUnique);
            _tmpUnique = _tmp_1 != 0;
            _result = new UnitEntity(_tmpId,_tmpFactionId,_tmpName,_tmpPoints,_tmpRank,_tmpMinInArmy,_tmpMaxInArmy,_tmpImageUrl,_tmpExpansionId,_tmpUnique);
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
  public Object getUpgradeSlots(final String unitId,
      final Continuation<? super List<UpgradeSlot>> $completion) {
    final String _sql = "SELECT * FROM upgrade_slots WHERE unitId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, unitId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<UpgradeSlot>>() {
      @Override
      @NonNull
      public List<UpgradeSlot> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUnitId = CursorUtil.getColumnIndexOrThrow(_cursor, "unitId");
          final int _cursorIndexOfSlotType = CursorUtil.getColumnIndexOrThrow(_cursor, "slotType");
          final List<UpgradeSlot> _result = new ArrayList<UpgradeSlot>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UpgradeSlot _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUnitId;
            _tmpUnitId = _cursor.getString(_cursorIndexOfUnitId);
            final String _tmpSlotType;
            _tmpSlotType = _cursor.getString(_cursorIndexOfSlotType);
            _item = new UpgradeSlot(_tmpId,_tmpUnitId,_tmpSlotType);
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
  public Object getKeywordsForUnit(final String unitId,
      final Continuation<? super List<Keyword>> $completion) {
    final String _sql = "SELECT k.* FROM keywords k INNER JOIN unit_keywords uk ON k.id = uk.keywordId WHERE uk.unitId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, unitId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Keyword>>() {
      @Override
      @NonNull
      public List<Keyword> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final List<Keyword> _result = new ArrayList<Keyword>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Keyword _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            _item = new Keyword(_tmpId,_tmpName,_tmpDescription);
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
  public Object factionCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM factions";
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
