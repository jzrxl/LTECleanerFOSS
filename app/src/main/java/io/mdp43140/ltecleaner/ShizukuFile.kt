/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.mdp43140.ltecleaner
import java.io.File

/**
 * A File whose listing/deleting goes through the Shizuku service, so FileScanner can treat
 * Android/data and Android/obb like any other folder. Type and size come from the listing.
 */
class ShizukuFile(path: String, private val dir: Boolean, private val size: Long) : File(path) {
	override fun isDirectory(): Boolean = dir
	override fun isFile(): Boolean = !dir
	override fun exists(): Boolean = true
	override fun length(): Long = size

	override fun list(): Array<String>? = listFiles()?.map { it.name }?.toTypedArray()

	override fun listFiles(): Array<File>? {
		if (!dir) return null
		val svc = ShizukuHelper.service ?: return null
		val lines = try { svc.listDetailed(absolutePath) } catch (_: Throwable) { return null }
		val out = ArrayList<File>(lines.size)
		for (line in lines) {
			val p = line.split('|', limit = 3)
			if (p.size < 3) continue
			out.add(ShizukuFile(File(absolutePath, p[2]).path, p[0] == "d", p[1].toLongOrNull() ?: 0L))
		}
		return out.toTypedArray()
	}

	override fun delete(): Boolean = deleteRemote()

	fun deleteRemote(): Boolean {
		val svc = ShizukuHelper.service ?: return false
		return try { svc.deleteRecursive(absolutePath) } catch (_: Throwable) { false }
	}
}
