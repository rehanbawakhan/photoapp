package com.photoapp.data.local;

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
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.photoapp.data.local.entities.AlbumEntity;
import com.photoapp.data.local.entities.PhotoEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class PhotoDao_Impl implements PhotoDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PhotoEntity> __insertionAdapterOfPhotoEntity;

  private final EntityInsertionAdapter<AlbumEntity> __insertionAdapterOfAlbumEntity;

  private final EntityDeletionOrUpdateAdapter<PhotoEntity> __deletionAdapterOfPhotoEntity;

  private final EntityDeletionOrUpdateAdapter<AlbumEntity> __deletionAdapterOfAlbumEntity;

  private final EntityDeletionOrUpdateAdapter<PhotoEntity> __updateAdapterOfPhotoEntity;

  private final EntityDeletionOrUpdateAdapter<AlbumEntity> __updateAdapterOfAlbumEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllPhotos;

  private final SharedSQLiteStatement __preparedStmtOfSetFavorite;

  private final SharedSQLiteStatement __preparedStmtOfMoveToTrash;

  private final SharedSQLiteStatement __preparedStmtOfRestoreFromTrash;

  private final SharedSQLiteStatement __preparedStmtOfEmptyTrash;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAutoAlbums;

  public PhotoDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPhotoEntity = new EntityInsertionAdapter<PhotoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `photos` (`id`,`uri`,`name`,`path`,`dateAdded`,`dateTaken`,`dateModified`,`size`,`width`,`height`,`mimeType`,`bucketId`,`bucketName`,`isFavorite`,`isDeleted`,`dateDeleted`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PhotoEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUri());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getPath());
        statement.bindLong(5, entity.getDateAdded());
        statement.bindLong(6, entity.getDateTaken());
        statement.bindLong(7, entity.getDateModified());
        statement.bindLong(8, entity.getSize());
        statement.bindLong(9, entity.getWidth());
        statement.bindLong(10, entity.getHeight());
        statement.bindString(11, entity.getMimeType());
        if (entity.getBucketId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getBucketId());
        }
        if (entity.getBucketName() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getBucketName());
        }
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(14, _tmp);
        final int _tmp_1 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        if (entity.getDateDeleted() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getDateDeleted());
        }
      }
    };
    this.__insertionAdapterOfAlbumEntity = new EntityInsertionAdapter<AlbumEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `albums` (`id`,`name`,`coverPhotoUri`,`photoCount`,`isCustom`,`dateCreated`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlbumEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getCoverPhotoUri() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCoverPhotoUri());
        }
        statement.bindLong(4, entity.getPhotoCount());
        final int _tmp = entity.isCustom() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getDateCreated());
      }
    };
    this.__deletionAdapterOfPhotoEntity = new EntityDeletionOrUpdateAdapter<PhotoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `photos` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PhotoEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__deletionAdapterOfAlbumEntity = new EntityDeletionOrUpdateAdapter<AlbumEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `albums` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlbumEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfPhotoEntity = new EntityDeletionOrUpdateAdapter<PhotoEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `photos` SET `id` = ?,`uri` = ?,`name` = ?,`path` = ?,`dateAdded` = ?,`dateTaken` = ?,`dateModified` = ?,`size` = ?,`width` = ?,`height` = ?,`mimeType` = ?,`bucketId` = ?,`bucketName` = ?,`isFavorite` = ?,`isDeleted` = ?,`dateDeleted` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PhotoEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getUri());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getPath());
        statement.bindLong(5, entity.getDateAdded());
        statement.bindLong(6, entity.getDateTaken());
        statement.bindLong(7, entity.getDateModified());
        statement.bindLong(8, entity.getSize());
        statement.bindLong(9, entity.getWidth());
        statement.bindLong(10, entity.getHeight());
        statement.bindString(11, entity.getMimeType());
        if (entity.getBucketId() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getBucketId());
        }
        if (entity.getBucketName() == null) {
          statement.bindNull(13);
        } else {
          statement.bindString(13, entity.getBucketName());
        }
        final int _tmp = entity.isFavorite() ? 1 : 0;
        statement.bindLong(14, _tmp);
        final int _tmp_1 = entity.isDeleted() ? 1 : 0;
        statement.bindLong(15, _tmp_1);
        if (entity.getDateDeleted() == null) {
          statement.bindNull(16);
        } else {
          statement.bindLong(16, entity.getDateDeleted());
        }
        statement.bindLong(17, entity.getId());
      }
    };
    this.__updateAdapterOfAlbumEntity = new EntityDeletionOrUpdateAdapter<AlbumEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `albums` SET `id` = ?,`name` = ?,`coverPhotoUri` = ?,`photoCount` = ?,`isCustom` = ?,`dateCreated` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AlbumEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getCoverPhotoUri() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCoverPhotoUri());
        }
        statement.bindLong(4, entity.getPhotoCount());
        final int _tmp = entity.isCustom() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getDateCreated());
        statement.bindString(7, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAllPhotos = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM photos";
        return _query;
      }
    };
    this.__preparedStmtOfSetFavorite = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE photos SET isFavorite = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfMoveToTrash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE photos SET isDeleted = 1, dateDeleted = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfRestoreFromTrash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE photos SET isDeleted = 0, dateDeleted = null WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfEmptyTrash = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM photos WHERE isDeleted = 1";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAutoAlbums = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM albums WHERE isCustom = 0";
        return _query;
      }
    };
  }

  @Override
  public Object insertPhotos(final List<PhotoEntity> photos,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPhotoEntity.insert(photos);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPhoto(final PhotoEntity photo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPhotoEntity.insert(photo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAlbum(final AlbumEntity album, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAlbumEntity.insert(album);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAlbums(final List<AlbumEntity> albums,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAlbumEntity.insert(albums);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePhoto(final PhotoEntity photo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPhotoEntity.handle(photo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAlbum(final AlbumEntity album, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfAlbumEntity.handle(album);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePhoto(final PhotoEntity photo, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPhotoEntity.handle(photo);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateAlbum(final AlbumEntity album, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAlbumEntity.handle(album);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllPhotos(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllPhotos.acquire();
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
          __preparedStmtOfDeleteAllPhotos.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setFavorite(final long id, final boolean isFavorite,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetFavorite.acquire();
        int _argIndex = 1;
        final int _tmp = isFavorite ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
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
          __preparedStmtOfSetFavorite.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object moveToTrash(final long id, final long dateDeleted,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMoveToTrash.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, dateDeleted);
        _argIndex = 2;
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
          __preparedStmtOfMoveToTrash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object restoreFromTrash(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfRestoreFromTrash.acquire();
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
          __preparedStmtOfRestoreFromTrash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object emptyTrash(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfEmptyTrash.acquire();
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
          __preparedStmtOfEmptyTrash.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAutoAlbums(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAutoAlbums.acquire();
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
          __preparedStmtOfDeleteAutoAlbums.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PhotoEntity>> getAllPhotos() {
    final String _sql = "SELECT * FROM photos WHERE isDeleted = 0 ORDER BY dateTaken DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<List<PhotoEntity>>() {
      @Override
      @NonNull
      public List<PhotoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final List<PhotoEntity> _result = new ArrayList<PhotoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _item = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Object getAllPhotosList(final Continuation<? super List<PhotoEntity>> $completion) {
    final String _sql = "SELECT * FROM photos WHERE isDeleted = 0 ORDER BY dateTaken DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PhotoEntity>>() {
      @Override
      @NonNull
      public List<PhotoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final List<PhotoEntity> _result = new ArrayList<PhotoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _item = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Object getPhotoById(final long id, final Continuation<? super PhotoEntity> $completion) {
    final String _sql = "SELECT * FROM photos WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PhotoEntity>() {
      @Override
      @Nullable
      public PhotoEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final PhotoEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _result = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Flow<PhotoEntity> observePhotoById(final long id) {
    final String _sql = "SELECT * FROM photos WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<PhotoEntity>() {
      @Override
      @Nullable
      public PhotoEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final PhotoEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _result = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Flow<List<PhotoEntity>> getFavoritePhotos() {
    final String _sql = "SELECT * FROM photos WHERE isFavorite = 1 AND isDeleted = 0 ORDER BY dateTaken DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<List<PhotoEntity>>() {
      @Override
      @NonNull
      public List<PhotoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final List<PhotoEntity> _result = new ArrayList<PhotoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _item = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Flow<List<PhotoEntity>> getTrashPhotos() {
    final String _sql = "SELECT * FROM photos WHERE isDeleted = 1 ORDER BY dateDeleted DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<List<PhotoEntity>>() {
      @Override
      @NonNull
      public List<PhotoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final List<PhotoEntity> _result = new ArrayList<PhotoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _item = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Object getExpiredTrashPhotos(final long cutoffTime,
      final Continuation<? super List<PhotoEntity>> $completion) {
    final String _sql = "SELECT * FROM photos WHERE isDeleted = 1 AND dateDeleted < ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, cutoffTime);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PhotoEntity>>() {
      @Override
      @NonNull
      public List<PhotoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final List<PhotoEntity> _result = new ArrayList<PhotoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _item = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Flow<List<AlbumEntity>> getAllAlbums() {
    final String _sql = "SELECT * FROM albums ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"albums"}, new Callable<List<AlbumEntity>>() {
      @Override
      @NonNull
      public List<AlbumEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCoverPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPhotoUri");
          final int _cursorIndexOfPhotoCount = CursorUtil.getColumnIndexOrThrow(_cursor, "photoCount");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "isCustom");
          final int _cursorIndexOfDateCreated = CursorUtil.getColumnIndexOrThrow(_cursor, "dateCreated");
          final List<AlbumEntity> _result = new ArrayList<AlbumEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AlbumEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCoverPhotoUri;
            if (_cursor.isNull(_cursorIndexOfCoverPhotoUri)) {
              _tmpCoverPhotoUri = null;
            } else {
              _tmpCoverPhotoUri = _cursor.getString(_cursorIndexOfCoverPhotoUri);
            }
            final int _tmpPhotoCount;
            _tmpPhotoCount = _cursor.getInt(_cursorIndexOfPhotoCount);
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final long _tmpDateCreated;
            _tmpDateCreated = _cursor.getLong(_cursorIndexOfDateCreated);
            _item = new AlbumEntity(_tmpId,_tmpName,_tmpCoverPhotoUri,_tmpPhotoCount,_tmpIsCustom,_tmpDateCreated);
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
  public Object getAlbumById(final String id, final Continuation<? super AlbumEntity> $completion) {
    final String _sql = "SELECT * FROM albums WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AlbumEntity>() {
      @Override
      @Nullable
      public AlbumEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCoverPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPhotoUri");
          final int _cursorIndexOfPhotoCount = CursorUtil.getColumnIndexOrThrow(_cursor, "photoCount");
          final int _cursorIndexOfIsCustom = CursorUtil.getColumnIndexOrThrow(_cursor, "isCustom");
          final int _cursorIndexOfDateCreated = CursorUtil.getColumnIndexOrThrow(_cursor, "dateCreated");
          final AlbumEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpCoverPhotoUri;
            if (_cursor.isNull(_cursorIndexOfCoverPhotoUri)) {
              _tmpCoverPhotoUri = null;
            } else {
              _tmpCoverPhotoUri = _cursor.getString(_cursorIndexOfCoverPhotoUri);
            }
            final int _tmpPhotoCount;
            _tmpPhotoCount = _cursor.getInt(_cursorIndexOfPhotoCount);
            final boolean _tmpIsCustom;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCustom);
            _tmpIsCustom = _tmp != 0;
            final long _tmpDateCreated;
            _tmpDateCreated = _cursor.getLong(_cursorIndexOfDateCreated);
            _result = new AlbumEntity(_tmpId,_tmpName,_tmpCoverPhotoUri,_tmpPhotoCount,_tmpIsCustom,_tmpDateCreated);
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
  public Flow<List<PhotoEntity>> getPhotosByBucket(final String bucketId) {
    final String _sql = "\n"
            + "        SELECT * FROM photos \n"
            + "        WHERE bucketId = ? AND isDeleted = 0 \n"
            + "        ORDER BY dateTaken DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, bucketId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<List<PhotoEntity>>() {
      @Override
      @NonNull
      public List<PhotoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final List<PhotoEntity> _result = new ArrayList<PhotoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _item = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Object getPhotosByBucketList(final String bucketId,
      final Continuation<? super List<PhotoEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM photos \n"
            + "        WHERE bucketId = ? AND isDeleted = 0 \n"
            + "        ORDER BY dateTaken DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, bucketId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PhotoEntity>>() {
      @Override
      @NonNull
      public List<PhotoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final List<PhotoEntity> _result = new ArrayList<PhotoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _item = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Flow<List<PhotoEntity>> searchPhotos(final String query) {
    final String _sql = "\n"
            + "        SELECT * FROM photos \n"
            + "        WHERE isDeleted = 0 AND (name LIKE '%' || ? || '%' OR path LIKE '%' || ? || '%')\n"
            + "        ORDER BY dateTaken DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, query);
    _argIndex = 2;
    _statement.bindString(_argIndex, query);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<List<PhotoEntity>>() {
      @Override
      @NonNull
      public List<PhotoEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUri = CursorUtil.getColumnIndexOrThrow(_cursor, "uri");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfPath = CursorUtil.getColumnIndexOrThrow(_cursor, "path");
          final int _cursorIndexOfDateAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "dateAdded");
          final int _cursorIndexOfDateTaken = CursorUtil.getColumnIndexOrThrow(_cursor, "dateTaken");
          final int _cursorIndexOfDateModified = CursorUtil.getColumnIndexOrThrow(_cursor, "dateModified");
          final int _cursorIndexOfSize = CursorUtil.getColumnIndexOrThrow(_cursor, "size");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfMimeType = CursorUtil.getColumnIndexOrThrow(_cursor, "mimeType");
          final int _cursorIndexOfBucketId = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketId");
          final int _cursorIndexOfBucketName = CursorUtil.getColumnIndexOrThrow(_cursor, "bucketName");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfIsDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isDeleted");
          final int _cursorIndexOfDateDeleted = CursorUtil.getColumnIndexOrThrow(_cursor, "dateDeleted");
          final List<PhotoEntity> _result = new ArrayList<PhotoEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhotoEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpUri;
            _tmpUri = _cursor.getString(_cursorIndexOfUri);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpPath;
            _tmpPath = _cursor.getString(_cursorIndexOfPath);
            final long _tmpDateAdded;
            _tmpDateAdded = _cursor.getLong(_cursorIndexOfDateAdded);
            final long _tmpDateTaken;
            _tmpDateTaken = _cursor.getLong(_cursorIndexOfDateTaken);
            final long _tmpDateModified;
            _tmpDateModified = _cursor.getLong(_cursorIndexOfDateModified);
            final long _tmpSize;
            _tmpSize = _cursor.getLong(_cursorIndexOfSize);
            final int _tmpWidth;
            _tmpWidth = _cursor.getInt(_cursorIndexOfWidth);
            final int _tmpHeight;
            _tmpHeight = _cursor.getInt(_cursorIndexOfHeight);
            final String _tmpMimeType;
            _tmpMimeType = _cursor.getString(_cursorIndexOfMimeType);
            final String _tmpBucketId;
            if (_cursor.isNull(_cursorIndexOfBucketId)) {
              _tmpBucketId = null;
            } else {
              _tmpBucketId = _cursor.getString(_cursorIndexOfBucketId);
            }
            final String _tmpBucketName;
            if (_cursor.isNull(_cursorIndexOfBucketName)) {
              _tmpBucketName = null;
            } else {
              _tmpBucketName = _cursor.getString(_cursorIndexOfBucketName);
            }
            final boolean _tmpIsFavorite;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp != 0;
            final boolean _tmpIsDeleted;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsDeleted);
            _tmpIsDeleted = _tmp_1 != 0;
            final Long _tmpDateDeleted;
            if (_cursor.isNull(_cursorIndexOfDateDeleted)) {
              _tmpDateDeleted = null;
            } else {
              _tmpDateDeleted = _cursor.getLong(_cursorIndexOfDateDeleted);
            }
            _item = new PhotoEntity(_tmpId,_tmpUri,_tmpName,_tmpPath,_tmpDateAdded,_tmpDateTaken,_tmpDateModified,_tmpSize,_tmpWidth,_tmpHeight,_tmpMimeType,_tmpBucketId,_tmpBucketName,_tmpIsFavorite,_tmpIsDeleted,_tmpDateDeleted);
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
  public Flow<Integer> getPhotoCount() {
    final String _sql = "SELECT COUNT(*) FROM photos WHERE isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<Integer>() {
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
  public Flow<Integer> getTrashCount() {
    final String _sql = "SELECT COUNT(*) FROM photos WHERE isDeleted = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<Integer>() {
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
  public Flow<Integer> getFavoriteCount() {
    final String _sql = "SELECT COUNT(*) FROM photos WHERE isFavorite = 1 AND isDeleted = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"photos"}, new Callable<Integer>() {
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
  public Object deletePhotosByIds(final List<Long> ids,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM photos WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setFavoriteMultiple(final List<Long> ids,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE photos SET isFavorite = 1 WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object moveToTrashMultiple(final List<Long> ids, final long dateDeleted,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE photos SET isDeleted = 1, dateDeleted = ");
        _stringBuilder.append("?");
        _stringBuilder.append(" WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, dateDeleted);
        _argIndex = 2;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object restoreAllFromTrash(final List<Long> ids,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE photos SET isDeleted = 0, dateDeleted = null WHERE id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (long _item : ids) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
