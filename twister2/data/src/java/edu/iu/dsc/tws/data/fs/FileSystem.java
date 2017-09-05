//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
package edu.iu.dsc.tws.data.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import edu.iu.dsc.tws.data.fs.local.LocalFileSystem;

/**
 * This class is an abstract of the generic file system that will be used in the system
 * This can be extend to support distributed file system or a local file system. This defines
 * the basic set of operations
 * that need to be supported by the concrete implementation
 */
public abstract class FileSystem {

  private static final Logger LOG = Logger.getLogger(FileSystem.class.getName());

  /** Object used to protect calls to specific methods.*/
  private static final Object SYNCHRONIZATION_OBJECT = new Object();

  /**
   * Data structure holding supported FileSystem Information
   */
  private static final Map<String, String> SUPPORTEDFS = new HashMap<String, String>();

  static {
    SUPPORTEDFS.put("file", LocalFileSystem.class.getName());
  }

  /**
   * Check if the given path exsits
   * @param path
   * @return
   */
  public boolean exists(Path path){
    return true;
  }

  /**
   * Check if file
   * @param path
   * @return
   */
  public boolean isFile(Path path){
    return true;
  }

  /**
   * Check if directory
   * @param path
   * @return
   */
  public boolean isDirectory(Path path){
    return true;
  }

  /**
   * check if isSymlink
   * @param path
   * @return
   */
  public boolean isSymlink(Path path){
    return true;
  }

  /**
   * Set the working Directory
   * @param path
   */
  public abstract void setWorkingDirectory(Path path);

  /**
   * Get the working Directory
   * @return
   */
  public abstract Path getWorkingDirectory();

  /**
   * Returns a URI whose scheme and authority identify this file system.
   *
   * @return a URI whose scheme and authority identify this file system
   */
  public abstract URI getUri();

  /**
   * Called after a new FileSystem instance is constructed.
   *
   * @param name
   *        a {@link URI} whose authority section names the host, port, etc. for this file system
   */
  public abstract void initialize(URI name) throws IOException;

  /**
   * Returns a FileSystem for the given uri
   * TODO: need to think about security (Flink adds a safety net here, that is skipped for now)
   * @param uri
   * @return
   */
  public static FileSystem get(URI uri) throws IOException {
    return getFileSystem(uri);
  }

  /**
   * Return a file status object that represents the path.
   *
   * @param f
   *        The path we want information from
   * @return a FileStatus object
   * @throws FileNotFoundException
   *         when the path does not exist;
   *         IOException see specific implementation
   */
  public abstract FileStatus getFileStatus(Path f) throws IOException;

  /**
   * List the statuses of the files/directories in the given path if the path is
   * a directory.
   *
   * @param f
   *        given path
   * @return the statuses of the files/directories in the given patch
   * @throws IOException
   */
  public abstract FileStatus[] listFiles(Path f) throws IOException;

  /**
   * Returns a unsafe filesystem for the given uri
   * @param uri
   * @return
   */
  private static FileSystem getFileSystem(URI uri) throws IOException {
    FileSystem fs = null;

    if(uri == null || uri.getScheme() == null){
      //TODO: try to fix missing scheme using a default scheme
      throw new IOException("The URI " + uri.toString() + " is not a vaild URI");
    }
    //TODO: check if the sycn is actually needed or can be scoped down
    synchronized (SYNCHRONIZATION_OBJECT){
      if (uri.getScheme().equals("file") && uri.getAuthority() != null && !uri.getAuthority().isEmpty()) {
        String supposedUri = "file:///" + uri.getAuthority() + uri.getPath();

        throw new IOException("Found local file path with authority '" + uri.getAuthority() + "' in path '"
            + uri.toString() + "'. Hint: Did you forget a slash? (correct path would be '" + supposedUri + "')");
      }

      //TODO : need to add cache that can save FileSystem Objects and return from cache if available
      if(!isSupportedScheme(uri.getScheme())){
        //TODO: handle when the system is not supported
      }else{
        String fsClass = SUPPORTEDFS.get(uri.getScheme());
        fs = instantiateFileSystem(fsClass);
        fs.initialize(uri);
      }

    }
    return fs;
  }

  /**
   * Checks if the given FileSystem scheme is currently supported
   * @param scheme
   * @return true if supported, false otherwise
   */
  private static boolean isSupportedScheme(String scheme){
    return SUPPORTEDFS.containsKey(scheme);
  }

  private static FileSystem instantiateFileSystem(String className) throws IOException {
    try {
      Class<? extends FileSystem> fsClass = getFileSystemByName(className);
      return fsClass.newInstance();
    }
    catch (ClassNotFoundException e) {
      throw new IOException("Could not load file system class '" + className + '\'', e);
    }
    catch (InstantiationException | IllegalAccessException e) {
      throw new IOException("Could not instantiate file system class: " + e.getMessage(), e);
    }
  }

  private static Class<? extends FileSystem> getFileSystemByName(String className) throws ClassNotFoundException {
    return Class.forName(className, true, FileSystem.class.getClassLoader()).asSubclass(FileSystem.class);
  }

  /**
   * Return an array containing hostnames, offset and size of
   * portions of the given file. For a nonexistent
   * file or regions, null will be returned.
   * This call is most helpful with DFS, where it returns
   * hostnames of machines that contain the given file.
   * The FileSystem will simply return an elt containing 'localhost'.
   */
  public abstract BlockLocation[] getFileBlockLocations(FileStatus file, long start, long len) throws IOException;
}
