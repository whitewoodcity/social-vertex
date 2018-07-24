package cn.net.polyglot.utils

import cn.net.polyglot.config.FileSystemConstants.FRIENDS
import cn.net.polyglot.config.FileSystemConstants.USER_DIR
import cn.net.polyglot.config.FileSystemConstants.USER_FILE
import java.io.File.separator

/**
 * @author zxj5470
 * @date 2018/7/21
 */
/**
 * return "social-vertex/user/yourID"
 * @param id String?
 * @return  first: userDir "social-vertex/user/$id"
 *          second: userFile "social-vertex/user/$id/user.json"
 */
fun getUserDirAndFile(id: String?): Pair<String, String> {
  val userDir = "$USER_DIR$separator$id"
  val userFile = "$USER_DIR$separator$id$separator$USER_FILE"
  return Pair(userDir, userFile)
}

/**
 * @param from String
 * @param to String
 * @return String "social-vertex/user/$from/friends/$to"
 */
fun getDirFromUserToFriendDIr(from: String, to: String): String {
  return USER_DIR + separator + from + separator + FRIENDS + separator + to
}
