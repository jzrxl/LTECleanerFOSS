package io.mdp43140.ltecleaner;

interface IFileService {
	// Destroy method defined by Shizuku server (must keep this exact transaction id)
	void destroy() = 16777114;

	// Each entry is "d|size|name" or "f|size|name"
	String[] listDetailed(String path) = 1;
	boolean deleteRecursive(String path) = 2;
}
