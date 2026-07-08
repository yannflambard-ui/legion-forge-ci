package com.legionforge.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LegionForgeDatabase_Impl extends LegionForgeDatabase {
  private volatile GameDataDao _gameDataDao;

  private volatile ArmyListDao _armyListDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `factions` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, `imageUrl` TEXT, `expansionId` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `units` (`id` TEXT NOT NULL, `factionId` TEXT NOT NULL, `name` TEXT NOT NULL, `points` INTEGER NOT NULL, `rank` TEXT NOT NULL, `minInArmy` INTEGER NOT NULL, `maxInArmy` INTEGER NOT NULL, `imageUrl` TEXT, `expansionId` TEXT, `unique` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `upgrade_slots` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `unitId` TEXT NOT NULL, `slotType` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `keywords` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `description` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `unit_keywords` (`unitId` TEXT NOT NULL, `keywordId` TEXT NOT NULL, PRIMARY KEY(`unitId`, `keywordId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `army_lists` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `factionId` TEXT NOT NULL, `battleForceId` TEXT, `pointsLimit` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `army_units` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `armyListId` TEXT NOT NULL, `unitId` TEXT NOT NULL, `upgradeIds` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `owned_units` (`unitId` TEXT NOT NULL, `quantity` INTEGER NOT NULL, PRIMARY KEY(`unitId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '372b9bbedf655355fc211003eb0038d2')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `factions`");
        db.execSQL("DROP TABLE IF EXISTS `units`");
        db.execSQL("DROP TABLE IF EXISTS `upgrade_slots`");
        db.execSQL("DROP TABLE IF EXISTS `keywords`");
        db.execSQL("DROP TABLE IF EXISTS `unit_keywords`");
        db.execSQL("DROP TABLE IF EXISTS `army_lists`");
        db.execSQL("DROP TABLE IF EXISTS `army_units`");
        db.execSQL("DROP TABLE IF EXISTS `owned_units`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsFactions = new HashMap<String, TableInfo.Column>(5);
        _columnsFactions.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFactions.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFactions.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFactions.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFactions.put("expansionId", new TableInfo.Column("expansionId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFactions = new TableInfo("factions", _columnsFactions, _foreignKeysFactions, _indicesFactions);
        final TableInfo _existingFactions = TableInfo.read(db, "factions");
        if (!_infoFactions.equals(_existingFactions)) {
          return new RoomOpenHelper.ValidationResult(false, "factions(com.legionforge.app.data.model.Faction).\n"
                  + " Expected:\n" + _infoFactions + "\n"
                  + " Found:\n" + _existingFactions);
        }
        final HashMap<String, TableInfo.Column> _columnsUnits = new HashMap<String, TableInfo.Column>(10);
        _columnsUnits.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("factionId", new TableInfo.Column("factionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("points", new TableInfo.Column("points", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("rank", new TableInfo.Column("rank", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("minInArmy", new TableInfo.Column("minInArmy", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("maxInArmy", new TableInfo.Column("maxInArmy", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("imageUrl", new TableInfo.Column("imageUrl", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("expansionId", new TableInfo.Column("expansionId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnits.put("unique", new TableInfo.Column("unique", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUnits = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUnits = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUnits = new TableInfo("units", _columnsUnits, _foreignKeysUnits, _indicesUnits);
        final TableInfo _existingUnits = TableInfo.read(db, "units");
        if (!_infoUnits.equals(_existingUnits)) {
          return new RoomOpenHelper.ValidationResult(false, "units(com.legionforge.app.data.model.UnitEntity).\n"
                  + " Expected:\n" + _infoUnits + "\n"
                  + " Found:\n" + _existingUnits);
        }
        final HashMap<String, TableInfo.Column> _columnsUpgradeSlots = new HashMap<String, TableInfo.Column>(3);
        _columnsUpgradeSlots.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUpgradeSlots.put("unitId", new TableInfo.Column("unitId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUpgradeSlots.put("slotType", new TableInfo.Column("slotType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUpgradeSlots = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUpgradeSlots = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUpgradeSlots = new TableInfo("upgrade_slots", _columnsUpgradeSlots, _foreignKeysUpgradeSlots, _indicesUpgradeSlots);
        final TableInfo _existingUpgradeSlots = TableInfo.read(db, "upgrade_slots");
        if (!_infoUpgradeSlots.equals(_existingUpgradeSlots)) {
          return new RoomOpenHelper.ValidationResult(false, "upgrade_slots(com.legionforge.app.data.model.UpgradeSlot).\n"
                  + " Expected:\n" + _infoUpgradeSlots + "\n"
                  + " Found:\n" + _existingUpgradeSlots);
        }
        final HashMap<String, TableInfo.Column> _columnsKeywords = new HashMap<String, TableInfo.Column>(3);
        _columnsKeywords.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeywords.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsKeywords.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysKeywords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesKeywords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoKeywords = new TableInfo("keywords", _columnsKeywords, _foreignKeysKeywords, _indicesKeywords);
        final TableInfo _existingKeywords = TableInfo.read(db, "keywords");
        if (!_infoKeywords.equals(_existingKeywords)) {
          return new RoomOpenHelper.ValidationResult(false, "keywords(com.legionforge.app.data.model.Keyword).\n"
                  + " Expected:\n" + _infoKeywords + "\n"
                  + " Found:\n" + _existingKeywords);
        }
        final HashMap<String, TableInfo.Column> _columnsUnitKeywords = new HashMap<String, TableInfo.Column>(2);
        _columnsUnitKeywords.put("unitId", new TableInfo.Column("unitId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsUnitKeywords.put("keywordId", new TableInfo.Column("keywordId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysUnitKeywords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesUnitKeywords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoUnitKeywords = new TableInfo("unit_keywords", _columnsUnitKeywords, _foreignKeysUnitKeywords, _indicesUnitKeywords);
        final TableInfo _existingUnitKeywords = TableInfo.read(db, "unit_keywords");
        if (!_infoUnitKeywords.equals(_existingUnitKeywords)) {
          return new RoomOpenHelper.ValidationResult(false, "unit_keywords(com.legionforge.app.data.model.UnitKeywordCrossRef).\n"
                  + " Expected:\n" + _infoUnitKeywords + "\n"
                  + " Found:\n" + _existingUnitKeywords);
        }
        final HashMap<String, TableInfo.Column> _columnsArmyLists = new HashMap<String, TableInfo.Column>(7);
        _columnsArmyLists.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyLists.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyLists.put("factionId", new TableInfo.Column("factionId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyLists.put("battleForceId", new TableInfo.Column("battleForceId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyLists.put("pointsLimit", new TableInfo.Column("pointsLimit", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyLists.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyLists.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysArmyLists = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesArmyLists = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoArmyLists = new TableInfo("army_lists", _columnsArmyLists, _foreignKeysArmyLists, _indicesArmyLists);
        final TableInfo _existingArmyLists = TableInfo.read(db, "army_lists");
        if (!_infoArmyLists.equals(_existingArmyLists)) {
          return new RoomOpenHelper.ValidationResult(false, "army_lists(com.legionforge.app.data.model.ArmyList).\n"
                  + " Expected:\n" + _infoArmyLists + "\n"
                  + " Found:\n" + _existingArmyLists);
        }
        final HashMap<String, TableInfo.Column> _columnsArmyUnits = new HashMap<String, TableInfo.Column>(4);
        _columnsArmyUnits.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyUnits.put("armyListId", new TableInfo.Column("armyListId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyUnits.put("unitId", new TableInfo.Column("unitId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsArmyUnits.put("upgradeIds", new TableInfo.Column("upgradeIds", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysArmyUnits = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesArmyUnits = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoArmyUnits = new TableInfo("army_units", _columnsArmyUnits, _foreignKeysArmyUnits, _indicesArmyUnits);
        final TableInfo _existingArmyUnits = TableInfo.read(db, "army_units");
        if (!_infoArmyUnits.equals(_existingArmyUnits)) {
          return new RoomOpenHelper.ValidationResult(false, "army_units(com.legionforge.app.data.model.ArmyUnit).\n"
                  + " Expected:\n" + _infoArmyUnits + "\n"
                  + " Found:\n" + _existingArmyUnits);
        }
        final HashMap<String, TableInfo.Column> _columnsOwnedUnits = new HashMap<String, TableInfo.Column>(2);
        _columnsOwnedUnits.put("unitId", new TableInfo.Column("unitId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsOwnedUnits.put("quantity", new TableInfo.Column("quantity", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysOwnedUnits = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesOwnedUnits = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoOwnedUnits = new TableInfo("owned_units", _columnsOwnedUnits, _foreignKeysOwnedUnits, _indicesOwnedUnits);
        final TableInfo _existingOwnedUnits = TableInfo.read(db, "owned_units");
        if (!_infoOwnedUnits.equals(_existingOwnedUnits)) {
          return new RoomOpenHelper.ValidationResult(false, "owned_units(com.legionforge.app.data.model.OwnedUnit).\n"
                  + " Expected:\n" + _infoOwnedUnits + "\n"
                  + " Found:\n" + _existingOwnedUnits);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "372b9bbedf655355fc211003eb0038d2", "08ef3825a9571854013701ddc117f2cf");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "factions","units","upgrade_slots","keywords","unit_keywords","army_lists","army_units","owned_units");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `factions`");
      _db.execSQL("DELETE FROM `units`");
      _db.execSQL("DELETE FROM `upgrade_slots`");
      _db.execSQL("DELETE FROM `keywords`");
      _db.execSQL("DELETE FROM `unit_keywords`");
      _db.execSQL("DELETE FROM `army_lists`");
      _db.execSQL("DELETE FROM `army_units`");
      _db.execSQL("DELETE FROM `owned_units`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(GameDataDao.class, GameDataDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ArmyListDao.class, ArmyListDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public GameDataDao gameDataDao() {
    if (_gameDataDao != null) {
      return _gameDataDao;
    } else {
      synchronized(this) {
        if(_gameDataDao == null) {
          _gameDataDao = new GameDataDao_Impl(this);
        }
        return _gameDataDao;
      }
    }
  }

  @Override
  public ArmyListDao armyListDao() {
    if (_armyListDao != null) {
      return _armyListDao;
    } else {
      synchronized(this) {
        if(_armyListDao == null) {
          _armyListDao = new ArmyListDao_Impl(this);
        }
        return _armyListDao;
      }
    }
  }
}
