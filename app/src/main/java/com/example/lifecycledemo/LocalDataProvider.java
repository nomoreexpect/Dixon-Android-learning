package com.example.lifecycledemo;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * LocalDataProvider - 本地数据 ContentProvider
 *
 * 【学习重点：ContentProvider 的作用与机制】
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │ ContentProvider 的价值                                       │
 * │   - 统一数据访问接口（URI 标准化），与存储实现解耦           │
 * │   - 跨进程数据共享（其他 App 通过 URI 访问你的数据）         │
 * │   - Android 系统本身大量使用：联系人、短信、媒体库...        │
 * │                                                              │
 * │ URI 格式                                                     │
 * │   content://<authority>/<path>                               │
 * │   content://<authority>/<path>/<id>  （单条记录）            │
 * │                                                              │
 * │ 六个必须实现的方法（CRUD + 初始化 + 类型查询）               │
 * │   onCreate / query / insert / update / delete / getType      │
 * └──────────────────────────────────────────────────────────────┘
 *
 * 【ContentProvider 生命周期】
 * - 比 Application.onCreate 更早调用（系统启动时初始化）
 * - onCreate 运行在主线程，不能做耗时操作
 * - query/insert/update/delete 可能在 Binder 线程池中调用，需要线程安全
 */
public class LocalDataProvider extends ContentProvider {

    private static final String TAG = "Lifecycle-Provider";

    // ── URI 常量 ──────────────────────────────────────────────
    public static final String AUTHORITY    = "com.example.lifecycledemo.provider";
    public static final String TABLE_NAME   = "local_data";
    public static final Uri    CONTENT_URI  = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

    // 列名
    public static final String COLUMN_ID    = "_id";
    public static final String COLUMN_NAME  = "name";
    public static final String COLUMN_VALUE = "value";

    // URI 匹配码
    private static final int CODE_ALL  = 1; // content://.../<table>
    private static final int CODE_ITEM = 2; // content://.../<table>/<id>

    /**
     * UriMatcher 用于解析 URI，判断是查询全部还是查询单条
     */
    private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        URI_MATCHER.addURI(AUTHORITY, TABLE_NAME,      CODE_ALL);
        URI_MATCHER.addURI(AUTHORITY, TABLE_NAME + "/#", CODE_ITEM);
    }

    private DatabaseHelper dbHelper;

    // ================================================================
    //  ContentProvider 生命周期
    // ================================================================

    @Override
    public boolean onCreate() {
        // 【生命周期】onCreate: ContentProvider 创建时调用，早于 Application.onCreate
        // 注意：这里运行在主线程，不能做耗时操作
        Log.d(TAG, ">>> onCreate  (比 Application 更早启动)");
        dbHelper = new DatabaseHelper(getContext());
        return true; // 返回 true 表示初始化成功
    }

    // ================================================================
    //  CRUD 实现
    // ================================================================

    /**
     * 查询数据
     * 【知识点】URI 匹配决定查询范围：
     *   CODE_ALL  → 查全表（SELECT * FROM table）
     *   CODE_ITEM → 查单条（SELECT * FROM table WHERE _id = ?）
     */
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        Log.d(TAG, ">>> query  uri=" + uri);

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        switch (URI_MATCHER.match(uri)) {
            case CODE_ALL:
                return db.query(TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
            case CODE_ITEM:
                // 从 URI 末尾提取 ID（如 content://.../local_data/3 → id=3）
                long id = ContentUris.parseId(uri);
                String where = COLUMN_ID + " = " + id;
                if (selection != null) where += " AND (" + selection + ")";
                return db.query(TABLE_NAME, projection, where, selectionArgs,
                        null, null, sortOrder);
            default:
                throw new IllegalArgumentException("未知 URI: " + uri);
        }
    }

    /**
     * 插入数据
     * 插入成功后必须调用 notifyChange，通知所有监听该 URI 的观察者数据已变化
     */
    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        Log.d(TAG, ">>> insert  uri=" + uri);

        if (URI_MATCHER.match(uri) != CODE_ALL) {
            throw new IllegalArgumentException("insert 只支持全表 URI");
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(TABLE_NAME, null, values);

        if (rowId > 0) {
            Uri newUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
            // 通知数据变化（观察者模式）
            if (getContext() != null) {
                getContext().getContentResolver().notifyChange(newUri, null);
            }
            return newUri;
        }
        return null;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        Log.d(TAG, ">>> update  uri=" + uri);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count;
        switch (URI_MATCHER.match(uri)) {
            case CODE_ALL:
                count = db.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            case CODE_ITEM:
                long id = ContentUris.parseId(uri);
                String where = COLUMN_ID + " = " + id;
                if (selection != null) where += " AND (" + selection + ")";
                count = db.update(TABLE_NAME, values, where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("未知 URI: " + uri);
        }

        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        Log.d(TAG, ">>> delete  uri=" + uri);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int count;
        switch (URI_MATCHER.match(uri)) {
            case CODE_ALL:
                count = db.delete(TABLE_NAME, selection, selectionArgs);
                break;
            case CODE_ITEM:
                long id = ContentUris.parseId(uri);
                String where = COLUMN_ID + " = " + id;
                if (selection != null) where += " AND (" + selection + ")";
                count = db.delete(TABLE_NAME, where, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("未知 URI: " + uri);
        }

        if (count > 0 && getContext() != null) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    /**
     * 返回指定 URI 的 MIME 类型
     * 规范：多条记录 "vnd.android.cursor.dir/..."；单条记录 "vnd.android.cursor.item/..."
     */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case CODE_ALL:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + "." + TABLE_NAME;
            case CODE_ITEM:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + "." + TABLE_NAME;
            default:
                throw new IllegalArgumentException("未知 URI: " + uri);
        }
    }

    // ================================================================
    //  内部 SQLite 数据库辅助类
    // ================================================================

    /**
     * SQLiteOpenHelper 管理数据库的创建与升级
     * 【知识点】Android 内置 SQLite，无需额外依赖
     */
    static class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DB_NAME    = "lifecycle_demo.db";
        private static final int    DB_VERSION = 1;

        DatabaseHelper(android.content.Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // 创建表
            db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID    + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_NAME  + " TEXT NOT NULL, " +
                    COLUMN_VALUE + " TEXT" +
                    ")");

            // 插入示例数据
            db.execSQL("INSERT INTO " + TABLE_NAME + " (name, value) VALUES ('学习进度', '60%')");
            db.execSQL("INSERT INTO " + TABLE_NAME + " (name, value) VALUES ('当前章节', 'Android 生命周期')");
            db.execSQL("INSERT INTO " + TABLE_NAME + " (name, value) VALUES ('练习项目', 'AndroidLifecycleDemo')");
            db.execSQL("INSERT INTO " + TABLE_NAME + " (name, value) VALUES ('完成时间', '2026-05-16')");

            Log.d(TAG, "数据库创建完成，已插入示例数据");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 版本升级时处理数据迁移
            Log.d(TAG, "数据库升级: " + oldVersion + " → " + newVersion);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
            onCreate(db);
        }
    }
}
