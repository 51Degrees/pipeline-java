/* *********************************************************************
 * This Original Work is copyright of 51 Degrees Mobile Experts Limited.
 * Copyright 2023 51 Degrees Mobile Experts Limited, Davidson House,
 * Forbury Square, Reading, Berkshire, United Kingdom RG1 3EU.
 *
 * This Original Work is licensed under the European Union Public Licence
 * (EUPL) v.1.2 and is subject to its terms as set out below.
 *
 * If a copy of the EUPL was not distributed with this file, You can obtain
 * one at https://opensource.org/licenses/EUPL-1.2.
 *
 * The 'Compatible Licences' set out in the Appendix to the EUPL (as may be
 * amended by the European Commission) shall be deemed incompatible for
 * the purposes of the Work and the provisions of the compatibility
 * clause in Article 5 of the EUPL shall not apply.
 *
 * If using the Work as, or as part of, a network application, by
 * including the attribution notice(s) required under Article 5 of the EUPL
 * in the end user terms of the application under an appropriate heading,
 * such notice(s) shall fulfill the requirements of that article.
 * ********************************************************************* */

package fiftyone.common.wrappers.io;

/**
 * Factory interface for {@link FileWrapper}s. By default this is a simple wrapper
 * for basic file operations used by the default {@link FileWrapper} implementation.
 * For unit tests, this can be implemented to build instances of {@link FileWrapper}
 * which do not rely on the file system.
 */
public interface FileWrapperFactory {

    /**
     * Return a new {@link FileWrapper} instance using the file located at the
     * specified path
     *
     * @param path path to the data file
     * @return new {@link FileWrapper} instance, or null if the path is null or
     * empty
     */
    FileWrapper build(String path);

    boolean exists(String path);

    long getLastModified(String path);

    void delete(String path);

}
