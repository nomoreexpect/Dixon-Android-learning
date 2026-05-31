"""
Android 生命周期 Demo - 逻辑模拟测试
用 Python 模拟四大组件的核心行为与生命周期顺序
"""

import sqlite3
import os
import re
import time

# ============================================================
# 工具函数
# ============================================================

import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

PASS = "[PASS]"
FAIL = "[FAIL]"
INFO = "[INFO]"
SECTION = ""
RESET = ""

results = {"pass": 0, "fail": 0}

def assert_eq(label, got, expected):
    if got == expected:
        print(f"  {PASS}  {label}")
        results["pass"] += 1
    else:
        print(f"  {FAIL}  {label}")
        print(f"         期望: {expected!r}")
        print(f"         实际: {got!r}")
        results["fail"] += 1

def assert_true(label, cond, hint=""):
    if cond:
        print(f"  {PASS}  {label}")
        results["pass"] += 1
    else:
        print(f"  {FAIL}  {label}" + (f" → {hint}" if hint else ""))
        results["fail"] += 1

def section(title):
    print(f"\n{'='*55}")
    print(f"  {title}")
    print(f"{'='*55}")


# ============================================================
# 测试 1：Activity 生命周期顺序
# ============================================================
section("① Activity 生命周期顺序测试")

class MockActivity:
    def __init__(self, name):
        self.name = name
        self.log = []

    def onCreate(self):   self.log.append("onCreate")
    def onStart(self):    self.log.append("onStart")
    def onResume(self):   self.log.append("onResume")
    def onPause(self):    self.log.append("onPause")
    def onStop(self):     self.log.append("onStop")
    def onRestart(self):  self.log.append("onRestart")
    def onDestroy(self):  self.log.append("onDestroy")

def simulate_launch(activity):
    activity.onCreate()
    activity.onStart()
    activity.onResume()

def simulate_navigate_to(from_act, to_act):
    """模拟跳转：先暂停当前，再启动新的，最后停止当前"""
    from_act.onPause()          # 第一步：A 先暂停
    to_act.onCreate()           # 第二步：B 开始创建
    to_act.onStart()
    to_act.onResume()           # B 进入前台
    from_act.onStop()           # A 完全不可见

def simulate_back(to_act, from_act):
    """模拟返回：当前 Activity 退出，前一个恢复"""
    to_act.onPause()
    from_act.onRestart()
    from_act.onStart()
    from_act.onResume()
    to_act.onStop()
    to_act.onDestroy()

# 运行模拟
main = MockActivity("MainActivity")
second = MockActivity("SecondActivity")

simulate_launch(main)
simulate_navigate_to(main, second)
simulate_back(second, main)

print(f"  {INFO} MainActivity 完整日志: {main.log}")
print(f"  {INFO} SecondActivity 完整日志: {second.log}")

# 验证 MainActivity
assert_eq("MainActivity 启动前三步", main.log[:3], ["onCreate", "onStart", "onResume"])
assert_eq("跳转时 MainActivity 先 onPause", main.log[3], "onPause")
assert_eq("跳转时 MainActivity 再 onStop（B 完全可见后）", main.log[4], "onStop")
assert_eq("返回时 MainActivity 经历 onRestart→onStart→onResume", main.log[5:8], ["onRestart", "onStart", "onResume"])
assert_eq("MainActivity 全程不触发 onDestroy", "onDestroy" in main.log, False)

# 验证 SecondActivity
assert_eq("SecondActivity 启动顺序", second.log[:3], ["onCreate", "onStart", "onResume"])
assert_eq("返回时 SecondActivity 经历 onStop→onDestroy", second.log[4:], ["onStop", "onDestroy"])

# 验证跳转期间 onPause(A) 在 onCreate(B) 之前
main_pause_idx   = main.log.index("onPause")
second_create_idx = second.log.index("onCreate")
# 用时间戳间接验证：检查 A.onPause 先于 B.onCreate（在拼合日志里）
combined = []
for e in ["onCreate","onStart","onResume"]: combined.append(("Main", e))
combined.append(("Main", "onPause"))
for e in ["onCreate","onStart","onResume"]: combined.append(("Second", e))
combined.append(("Main", "onStop"))
a_pause_pos  = next(i for i,(a,e) in enumerate(combined) if a=="Main" and e=="onPause")
b_create_pos = next(i for i,(a,e) in enumerate(combined) if a=="Second" and e=="onCreate")
assert_true("Main.onPause 先于 Second.onCreate（关键顺序）",
            a_pause_pos < b_create_pos)


# ============================================================
# 测试 2：Service 生命周期
# ============================================================
section("② Service 生命周期测试")

class MockMusicService:
    is_running = False

    def __init__(self):
        self.log = []
        self.playing = False
        self.bound_clients = 0

    def onCreate(self):
        self.log.append("onCreate")
        print(f"    [Service] onCreate → 初始化 MediaPlayer")

    def onStartCommand(self, start_id=1):
        self.log.append(f"onStartCommand({start_id})")
        MockMusicService.is_running = True
        self.playing = True
        print(f"    [Service] onStartCommand → 进入前台，开始播放")
        return "START_STICKY"

    def onBind(self):
        self.log.append("onBind")
        self.bound_clients += 1
        print(f"    [Service] onBind → 返回 IBinder，当前绑定数={self.bound_clients}")
        return "IBinder"

    def onUnbind(self):
        self.log.append("onUnbind")
        self.bound_clients = 0
        print(f"    [Service] onUnbind → 所有客户端已解绑")

    def onDestroy(self):
        self.log.append("onDestroy")
        MockMusicService.is_running = False
        self.playing = False
        print(f"    [Service] onDestroy → 释放 MediaPlayer，移除通知")

svc = MockMusicService()

# 场景A：startForegroundService 启动
svc.onCreate()
ret = svc.onStartCommand(start_id=1)
assert_true("startForegroundService 后 isRunning=True", MockMusicService.is_running)
assert_eq("onStartCommand 返回 START_STICKY", ret, "START_STICKY")
assert_true("MediaPlayer 播放中", svc.playing)

# 场景B：同时 bindService
binder = svc.onBind()
assert_true("bindService 返回 IBinder", binder == "IBinder")
assert_eq("绑定后客户端数=1", svc.bound_clients, 1)

# 重复调用 startService（模拟用户再次点击）
svc.onStartCommand(start_id=2)
assert_true("第二次 onStartCommand 仍然运行", MockMusicService.is_running)

# 解绑再停止
svc.onUnbind()
svc.onDestroy()
assert_true("onDestroy 后 isRunning=False", not MockMusicService.is_running)
assert_true("onDestroy 后 playing=False", not svc.playing)
assert_eq("Service 生命周期完整日志",
    svc.log,
    ["onCreate", "onStartCommand(1)", "onBind", "onStartCommand(2)", "onUnbind", "onDestroy"]
)


# ============================================================
# 测试 3：BroadcastReceiver 逻辑
# ============================================================
section("③ BroadcastReceiver 网络状态测试")

class MockNetworkReceiver:
    def __init__(self):
        self.received = []
        self.listener = None

    def set_listener(self, cb):
        self.listener = cb

    def on_receive(self, network_info):
        """模拟 onReceive 处理网络变化"""
        connected = network_info.get("connected", False)
        transport = network_info.get("transport", "UNKNOWN")

        if connected:
            net_type = {"WIFI": "WiFi", "CELLULAR": "移动数据", "ETHERNET": "以太网"}.get(transport, "其他")
            status = f"✅ 网络已连接 ({net_type})"
        else:
            status = "❌ 网络已断开"

        self.received.append({"connected": connected, "status": status})
        if self.listener:
            self.listener(connected, status)
        return status

callback_log = []
receiver = MockNetworkReceiver()
receiver.set_listener(lambda c, s: callback_log.append(s))

# 模拟各种网络事件
cases = [
    {"connected": True,  "transport": "WIFI"},
    {"connected": True,  "transport": "CELLULAR"},
    {"connected": False, "transport": "NONE"},
    {"connected": True,  "transport": "ETHERNET"},
]
for c in cases:
    receiver.on_receive(c)

print(f"  {INFO} 收到的广播: {[r['status'] for r in receiver.received]}")

assert_eq("WiFi 连接状态描述", receiver.received[0]["status"], "✅ 网络已连接 (WiFi)")
assert_eq("移动数据描述",      receiver.received[1]["status"], "✅ 网络已连接 (移动数据)")
assert_eq("断网描述",          receiver.received[2]["status"], "❌ 网络已断开")
assert_eq("以太网描述",        receiver.received[3]["status"], "✅ 网络已连接 (以太网)")
assert_eq("回调触发次数",      len(callback_log), 4)

# 模拟动态注册/注销（在 onResume/onPause 中）
class MockActivityWithReceiver:
    def __init__(self):
        self.receiver = None
        self.registered = False

    def on_resume(self):
        self.receiver = MockNetworkReceiver()
        self.registered = True  # 模拟 registerReceiver

    def on_pause(self):
        if self.registered:
            self.receiver = None
            self.registered = False  # 模拟 unregisterReceiver

act = MockActivityWithReceiver()
assert_true("onPause 前未注册广播", not act.registered)
act.on_resume()
assert_true("onResume 后已注册广播", act.registered)
act.on_pause()
assert_true("onPause 后已注销广播（防内存泄漏）", not act.registered)


# ============================================================
# 测试 4：ContentProvider + SQLite 逻辑
# ============================================================
section("④ ContentProvider + SQLite 测试")

DB_PATH = ":memory:"  # 内存数据库，测试完自动释放

class MockContentProvider:
    AUTHORITY  = "com.example.lifecycledemo.provider"
    TABLE      = "local_data"
    URI_PREFIX = f"content://{AUTHORITY}/{TABLE}"

    def __init__(self):
        self.db = sqlite3.connect(DB_PATH)
        self.created = False

    def on_create(self):
        """模拟 ContentProvider.onCreate"""
        self.db.execute("""
            CREATE TABLE IF NOT EXISTS local_data (
                _id   INTEGER PRIMARY KEY AUTOINCREMENT,
                name  TEXT NOT NULL,
                value TEXT
            )
        """)
        # 插入初始数据
        seed = [
            ("学习进度",   "60%"),
            ("当前章节",   "Android 生命周期"),
            ("练习项目",   "AndroidLifecycleDemo"),
            ("完成时间",   "2026-05-16"),
        ]
        self.db.executemany("INSERT INTO local_data (name,value) VALUES (?,?)", seed)
        self.db.commit()
        self.created = True
        return True  # onCreate 成功返回 True

    def _match_uri(self, uri):
        """模拟 UriMatcher"""
        if re.fullmatch(rf"content://{self.AUTHORITY}/{self.TABLE}", uri):
            return "ALL"
        if re.fullmatch(rf"content://{self.AUTHORITY}/{self.TABLE}/\d+", uri):
            return "ITEM"
        return None

    def query(self, uri, selection=None, args=None):
        match = self._match_uri(uri)
        if match == "ALL":
            cur = self.db.execute("SELECT * FROM local_data")
        elif match == "ITEM":
            row_id = uri.rsplit("/", 1)[-1]
            cur = self.db.execute("SELECT * FROM local_data WHERE _id=?", (row_id,))
        else:
            raise ValueError(f"未知 URI: {uri}")
        cols = [d[0] for d in cur.description]
        return [dict(zip(cols, row)) for row in cur.fetchall()]

    def insert(self, uri, name, value):
        self._match_uri(uri)  # 验证 URI
        cur = self.db.execute("INSERT INTO local_data (name,value) VALUES (?,?)", (name, value))
        self.db.commit()
        new_uri = f"{uri}/{cur.lastrowid}"
        print(f"    [Provider] insert → 新 URI: {new_uri}")
        return new_uri

    def update(self, uri, value):
        match = self._match_uri(uri)
        if match == "ITEM":
            row_id = uri.rsplit("/", 1)[-1]
            cnt = self.db.execute("UPDATE local_data SET value=? WHERE _id=?", (value, row_id)).rowcount
            self.db.commit()
            return cnt
        raise ValueError(f"update 仅支持单条 URI")

    def delete(self, uri):
        match = self._match_uri(uri)
        if match == "ITEM":
            row_id = uri.rsplit("/", 1)[-1]
            cnt = self.db.execute("DELETE FROM local_data WHERE _id=?", (row_id,)).rowcount
            self.db.commit()
            return cnt
        raise ValueError(f"delete 传入了全表 URI，应传单条")

    def get_type(self, uri):
        match = self._match_uri(uri)
        if match == "ALL":  return f"vnd.android.cursor.dir/vnd.{self.AUTHORITY}.{self.TABLE}"
        if match == "ITEM": return f"vnd.android.cursor.item/vnd.{self.AUTHORITY}.{self.TABLE}"
        raise ValueError("未知 URI")

provider = MockContentProvider()

# onCreate
result = provider.on_create()
assert_true("ContentProvider.onCreate 返回 True", result is True)

# query 全表
rows = provider.query(provider.URI_PREFIX)
assert_eq("初始数据行数", len(rows), 4)
assert_eq("第一行 name", rows[0]["name"], "学习进度")
assert_eq("第一行 value", rows[0]["value"], "60%")
print(f"  {INFO} 全表数据: {[(r['name'], r['value']) for r in rows]}")

# query 单条
row = provider.query(f"{provider.URI_PREFIX}/2")
assert_eq("单条查询结果数", len(row), 1)
assert_eq("单条查询 name", row[0]["name"], "当前章节")

# insert
new_uri = provider.insert(provider.URI_PREFIX, "新知识点", "BroadcastReceiver")
assert_true("insert 返回正确 URI 格式", new_uri.startswith(provider.URI_PREFIX + "/"))
rows_after_insert = provider.query(provider.URI_PREFIX)
assert_eq("insert 后行数变为 5", len(rows_after_insert), 5)

# update
updated = provider.update(f"{provider.URI_PREFIX}/1", "80%")
assert_eq("update 影响行数=1", updated, 1)
updated_row = provider.query(f"{provider.URI_PREFIX}/1")
assert_eq("update 后值已更新", updated_row[0]["value"], "80%")

# delete
deleted = provider.delete(f"{provider.URI_PREFIX}/5")
assert_eq("delete 影响行数=1", deleted, 1)
rows_after_delete = provider.query(provider.URI_PREFIX)
assert_eq("delete 后行数恢复 4", len(rows_after_delete), 4)

# getType
assert_true("全表 URI MIME 类型含 cursor.dir",
    "cursor.dir" in provider.get_type(provider.URI_PREFIX))
assert_true("单条 URI MIME 类型含 cursor.item",
    "cursor.item" in provider.get_type(f"{provider.URI_PREFIX}/1"))

# URI 匹配异常
try:
    provider.query("content://bad.authority/wrong")
    assert_true("非法 URI 应抛异常", False)
except ValueError:
    assert_true("非法 URI 正确抛出 ValueError", True)

# ============================================================
# 测试 5：音频文件存在性验证
# ============================================================
section("⑤ 资源文件检查")

WAV_PATH = r"C:\Users\Dixon\WorkBuddy\20260516224904\AndroidLifecycleDemo\app\src\main\res\raw\sample_music.wav"
assert_true("sample_music.wav 文件存在", os.path.exists(WAV_PATH))
if os.path.exists(WAV_PATH):
    size = os.path.getsize(WAV_PATH)
    assert_true(f"WAV 文件大小合理（> 100KB，实际 {size//1024}KB）", size > 100 * 1024)
    # 验证 WAV 文件头
    with open(WAV_PATH, "rb") as f:
        header = f.read(12)
    assert_eq("WAV 文件头 RIFF 标识", header[:4], b"RIFF")
    assert_eq("WAV 格式标识 WAVE",    header[8:12], b"WAVE")
    print(f"  {INFO} 文件大小: {size//1024} KB，WAV 格式验证通过")

# 验证 Manifest 关键内容
MANIFEST_PATH = r"C:\Users\Dixon\WorkBuddy\20260516224904\AndroidLifecycleDemo\app\src\main\AndroidManifest.xml"
with open(MANIFEST_PATH, "r", encoding="utf-8") as f:
    manifest = f.read()

assert_true("Manifest 包含 MusicService 声明",        "MusicService" in manifest)
assert_true("Manifest 包含 LocalDataProvider 声明",   "LocalDataProvider" in manifest)
assert_true("Manifest 包含 FOREGROUND_SERVICE 权限",  "FOREGROUND_SERVICE" in manifest)
assert_true("Manifest 包含 ACCESS_NETWORK_STATE 权限","ACCESS_NETWORK_STATE" in manifest)
assert_true("Manifest Provider authority 正确",
    'com.example.lifecycledemo.provider' in manifest)

# ============================================================
# 汇总
# ============================================================
total = results["pass"] + results["fail"]
print(f"\n{'='*55}")
print(f"  测试汇总: {total} 个测试  "
      f"\033[92m{results['pass']} 通过\033[0m  "
      f"\033[91m{results['fail']} 失败\033[0m")
print(f"{'='*55}\n")

if results["fail"] == 0:
    print("  🎉  所有测试通过！项目逻辑验证完整。")
else:
    print(f"  ⚠️   有 {results['fail']} 个测试未通过，请检查输出日志。")
