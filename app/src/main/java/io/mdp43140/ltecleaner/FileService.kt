/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.mdp43140.ltecleaner
import java.io.File
import kotlin.system.exitProcess

/** Runs in the Shizuku process (shell/root uid), which can access Android/data and Android/obb. */
class FileService() : IFileService.Stub() {
	override fun destroy() {
		exitProcess(0)
	}
	override fun listDetailed(path: String): Array<String> {
		val files = File(path).listFiles() ?: return emptyArray()
		return Array(files.size) { i ->
			val f = files[i]
			(if (f.isDirectory) "d" else "f") + "|" + f.length() + "|" + f.name
		}
	}
	override fun deleteRecursive(path: String): Boolean {
		val f = File(path)
		return !f.exists() || f.deleteRecursively()
	}
}
