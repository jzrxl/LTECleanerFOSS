/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.mdp43140.ltecleaner
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import rikka.shizuku.Shizuku

object ShizukuHelper {
	private const val REQUEST_CODE = 4301
	private val restrictedRegex = Regex("^/storage/emulated/\\d+/Android/(data|obb)(/.*)?$")
	@Volatile var service: IFileService? = null
		private set
	private var args: Shizuku.UserServiceArgs? = null
	private var conn: ServiceConnection? = null

	enum class State { NOT_RUNNING, NO_PERMISSION, DENIED, READY }

	/** Used by the settings screen to show what's going on */
	fun state(): State = try {
		when {
			!Shizuku.pingBinder() || Shizuku.isPreV11() -> State.NOT_RUNNING
			Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> State.READY
			Shizuku.shouldShowRequestPermissionRationale() -> State.DENIED
			else -> State.NO_PERMISSION
		}
	} catch (_: Throwable) { State.NOT_RUNNING }

	/** True if Shizuku is running and this app has been granted permission */
	fun isReady(): Boolean = try {
		Shizuku.pingBinder() && !Shizuku.isPreV11() &&
			Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
	} catch (_: Throwable) { false }

	/** Shows Shizuku's permission prompt if Shizuku is running and permission isn't granted yet */
	fun requestPermissionIfNeeded(force: Boolean = false) {
		if (!force && App.prefs?.useShizuku == false) return
		try {
			if (Shizuku.pingBinder() && !Shizuku.isPreV11() &&
				Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED &&
				!Shizuku.shouldShowRequestPermissionRationale()
			) Shizuku.requestPermission(REQUEST_CODE)
		} catch (_: Throwable) {}
	}

	/** Opens the Shizuku app so the user can start it / allow this app, or its download page if it isn't installed */
	fun openShizukuOrDownload(context: Context) {
		val launch = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
		val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
		try {
			context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
		} catch (_: Throwable) {}
	}

	/** Does whatever the next setup step is: allow this app if Shizuku is running, otherwise open Shizuku */
	fun startSetup(context: Context) {
		when (state()) {
			State.READY -> {}
			State.NO_PERMISSION -> requestPermissionIfNeeded(true)
			else -> openShizukuOrDownload(context)
		}
	}

	/** Android/data and Android/obb (Android 11+ blocks these for normal apps, even with all-files access) */
	fun isRestricted(file: File): Boolean = restrictedRegex.matches(file.absolutePath)

	/** Binds the file service. Blocks (max 5s), so never call from the main thread. Returns null if unavailable. */
	@Synchronized
	fun get(context: Context): IFileService? {
		service?.let { if (it.asBinder().pingBinder()) return it }
		if (!isReady()) return null
		val latch = CountDownLatch(1)
		val c = object : ServiceConnection {
			override fun onServiceConnected(name: ComponentName, binder: IBinder?) {
				service = if (binder != null && binder.pingBinder()) IFileService.Stub.asInterface(binder) else null
				latch.countDown()
			}
			override fun onServiceDisconnected(name: ComponentName) {
				service = null
			}
		}
		val a = Shizuku.UserServiceArgs(ComponentName(context.packageName, FileService::class.java.name))
			.processNameSuffix("file")
			.daemon(false)
			.version(1)
		try {
			Shizuku.bindUserService(a, c)
			latch.await(5, TimeUnit.SECONDS)
		} catch (_: Throwable) {
			return null
		}
		args = a
		conn = c
		return service
	}
}
